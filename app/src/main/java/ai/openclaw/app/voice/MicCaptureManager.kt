package ai.openclaw.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

enum class VoiceConversationRole {
    User,
    Assistant,
}

/**
 * Returns the part of an updated recognizer result that was not already sent as
 * an idle partial. Android often extends a partial with more words before its
 * next partial or final callback; replaying the whole result would produce two
 * caption turns for the same spoken phrase.
 */
internal fun unsentFinalTranscript(
    finalTranscript: String,
    flushedPartialTranscript: String?,
): String? {
    val final = finalTranscript.trim()
    val flushed = flushedPartialTranscript?.trim().orEmpty()
    if (final.isEmpty()) return null
    if (flushed.isEmpty()) return final
    if (final == flushed) return null

    val nextCharacter = final.getOrNull(flushed.length)
    val extendsFlushedPartial =
        final.startsWith(flushed) &&
            (nextCharacter == null ||
                nextCharacter.isWhitespace() ||
                nextCharacter in ",.!?;:)".toCharArray())
    return if (extendsFlushedPartial) final.substring(flushed.length).trim().takeIf { it.isNotEmpty() } else final
}

/**
 * Whether [candidate] is an earlier, whole-word version of [alreadyFlushed].
 *
 * Speech recognizers sometimes retract the tail of an interim result while
 * settling on a final phrase. That is not a new caption turn; emitting it
 * would duplicate words that the idle flush already sent to the gateway.
 */
private fun isTranscriptRegression(candidate: String, alreadyFlushed: String): Boolean {
    if (candidate.length >= alreadyFlushed.length || !alreadyFlushed.startsWith(candidate)) return false
    val nextCharacter = alreadyFlushed.getOrNull(candidate.length)
    return nextCharacter == null ||
        nextCharacter.isWhitespace() ||
        nextCharacter in ",.!?;:)".toCharArray()
}

/**
 * Keeps partial-caption de-duplication scoped to one recognizer session.
 *
 * Android can deliver an error, or the user can pause/stop the microphone,
 * after an idle partial was already sent to the gateway but before a final
 * result arrives. Carrying that partial into the next recognizer session can
 * incorrectly suppress the next spoken phrase when it happens to be the same
 * text. Every recognizer-session boundary must therefore call [reset].
 */
internal class CaptionTranscriptAccumulator {
    private var flushedPartialTranscript: String? = null

    fun flushPartial(partialTranscript: String): String? {
        val partial = partialTranscript.trim()
        val flushed = flushedPartialTranscript?.trim().orEmpty()
        // Keep the longer sent prefix when Android temporarily retracts words.
        // Replacing it with the shorter partial would make a later final result
        // resend the removed suffix as though it had never been delivered.
        if (partial.isNotEmpty() && flushed.isNotEmpty() && isTranscriptRegression(partial, flushed)) {
            return null
        }
        val unsent = unsentFinalTranscript(partial, flushedPartialTranscript)
        flushedPartialTranscript = partial.takeIf { it.isNotEmpty() }
        return unsent
    }

    fun finish(finalTranscript: String): String? {
        val unsent = unsentFinalTranscript(finalTranscript, flushedPartialTranscript)
        reset()
        return unsent
    }

    fun reset() {
        flushedPartialTranscript = null
    }
}

data class VoiceConversationEntry(
    val id: String,
    val role: VoiceConversationRole,
    val text: String,
    val isStreaming: Boolean = false,
)

class MicCaptureManager(
    private val context: Context,
    private val scope: CoroutineScope,
    /**
     * The language to request from Android speech recognition, or null to let
     * the recognizer auto-detect. Caption translation supplies its selected
     * source language here so the transcript matches the translation prompt.
     */
    private val recognitionLanguage: () -> String? = { null },
    /**
     * Send [message] to the gateway and return the run ID.
     * [onRunIdKnown] is called with the idempotency key *before* the network
     * round-trip so [pendingRunId] is set before any chat events can arrive.
     */
    private val sendToGateway: suspend (message: String, onRunIdKnown: (String) -> Unit) -> String?,
    private val speakAssistantReply: suspend (String) -> Unit = {},
) {
    companion object {
        private const val tag = "MicCapture"
        private const val speechMinSessionMs = 30_000L
        // Caption mode favors short turns, while partials make ongoing speech
        // visible before Android has declared a final recognition result.
        private const val speechCompleteSilenceMs = 550L
        private const val speechPossibleSilenceMs = 250L
        private const val transcriptIdleFlushMs = 350L
        private const val maxConversationEntries = 40
        private const val pendingRunTimeoutMs = 45_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val json = Json { ignoreUnknownKeys = true }

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled: StateFlow<Boolean> = _micEnabled

    private val _micCooldown = MutableStateFlow(false)
    val micCooldown: StateFlow<Boolean> = _micCooldown

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _statusText = MutableStateFlow("Mic off")
    val statusText: StateFlow<String> = _statusText

    private val _liveTranscript = MutableStateFlow<String?>(null)
    val liveTranscript: StateFlow<String?> = _liveTranscript

    private val _queuedMessages = MutableStateFlow<List<String>>(emptyList())
    val queuedMessages: StateFlow<List<String>> = _queuedMessages

    private val _conversation = MutableStateFlow<List<VoiceConversationEntry>>(emptyList())
    val conversation: StateFlow<List<VoiceConversationEntry>> = _conversation

    private val _inputLevel = MutableStateFlow(0f)
    val inputLevel: StateFlow<Float> = _inputLevel

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val messageQueue = ArrayDeque<String>()
    private val messageQueueLock = Any()
    private val captionTranscriptAccumulator = CaptionTranscriptAccumulator()
    private var pendingRunId: String? = null
    private var pendingAssistantEntryId: String? = null
    private var gatewayConnected = false

    private var recognizer: SpeechRecognizer? = null
    private var restartJob: Job? = null
    private var drainJob: Job? = null
    private var transcriptFlushJob: Job? = null
    private var pendingRunTimeoutJob: Job? = null
    private var stopRequested = false
    private val ttsPauseLock = Any()
    private var ttsPauseDepth = 0
    private var resumeMicAfterTts = false

    private fun enqueueMessage(message: String) {
        synchronized(messageQueueLock) {
            messageQueue.addLast(message)
        }
    }

    private fun snapshotMessageQueue(): List<String> =
        synchronized(messageQueueLock) {
            messageQueue.toList()
        }

    private fun hasQueuedMessages(): Boolean =
        synchronized(messageQueueLock) {
            messageQueue.isNotEmpty()
        }

    private fun firstQueuedMessage(): String? =
        synchronized(messageQueueLock) {
            messageQueue.firstOrNull()
        }

    private fun removeFirstQueuedMessage(): String? =
        synchronized(messageQueueLock) {
            if (messageQueue.isEmpty()) null else messageQueue.removeFirst()
        }

    private fun queuedMessageCount(): Int =
        synchronized(messageQueueLock) {
            messageQueue.size
        }

    fun setMicEnabled(enabled: Boolean) {
        if (_micEnabled.value == enabled) return
        _micEnabled.value = enabled
        if (enabled) {
            val pausedForTts =
                synchronized(ttsPauseLock) {
                    if (ttsPauseDepth > 0) {
                        resumeMicAfterTts = true
                        true
                    } else {
                        false
                    }
                }
            if (pausedForTts) {
                _statusText.value = if (_isSending.value) "Speaking · waiting for reply" else "Speaking…"
                return
            }
            start()
            sendQueuedIfIdle()
        } else {
            // Give the recognizer time to finish processing buffered audio.
            // Cancel any prior drain to prevent duplicate sends on rapid toggle.
            drainJob?.cancel()
            _micCooldown.value = true
            drainJob =
                scope.launch {
                    delay(2000L)
                    // Finish through the same accumulator used by the idle flush.  Sending the raw
                    // live value here used to duplicate a partial that had already been sent while
                    // the mic was draining.
                    val partial = _liveTranscript.value?.trim().orEmpty()
                    val unsent =
                        partial
                            .takeIf { it.isNotEmpty() }
                            ?.let(captionTranscriptAccumulator::finish)
                    stop()
                    if (unsent != null) {
                        queueRecognizedMessage(unsent)
                    }
                    drainJob = null
                    _micCooldown.value = false
                    sendQueuedIfIdle()
                }
        }
    }

    suspend fun pauseForTts() {
        val shouldPause =
            synchronized(ttsPauseLock) {
                ttsPauseDepth += 1
                if (ttsPauseDepth > 1) return@synchronized false
                resumeMicAfterTts = _micEnabled.value
                val active = resumeMicAfterTts || recognizer != null || _isListening.value
                if (!active) return@synchronized false
                stopRequested = true
                restartJob?.cancel()
                restartJob = null
                transcriptFlushJob?.cancel()
                transcriptFlushJob = null
                captionTranscriptAccumulator.reset()
                _isListening.value = false
                _inputLevel.value = 0f
                _liveTranscript.value = null
                _statusText.value = if (_isSending.value) "Speaking · waiting for reply" else "Speaking…"
                true
            }
        if (!shouldPause) return
        withContext(Dispatchers.Main) {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    suspend fun resumeAfterTts() {
        val shouldResume =
            synchronized(ttsPauseLock) {
                if (ttsPauseDepth == 0) return@synchronized false
                ttsPauseDepth -= 1
                if (ttsPauseDepth > 0) return@synchronized false
                val resume = resumeMicAfterTts && _micEnabled.value
                resumeMicAfterTts = false
                if (!resume) {
                    _statusText.value =
                        when {
                            _micEnabled.value && _isSending.value -> "Listening · sending queued voice"
                            _micEnabled.value -> "Listening"
                            _isSending.value -> "Mic off · sending…"
                            else -> "Mic off"
                        }
                }
                resume
            }
        if (!shouldResume) return
        stopRequested = false
        start()
        sendQueuedIfIdle()
    }

    fun onGatewayConnectionChanged(connected: Boolean) {
        gatewayConnected = connected
        if (connected) {
            sendQueuedIfIdle()
            return
        }
        pendingRunTimeoutJob?.cancel()
        pendingRunTimeoutJob = null
        pendingRunId = null
        pendingAssistantEntryId = null
        _isSending.value = false
        if (hasQueuedMessages()) {
            _statusText.value = queuedWaitingStatus()
        }
    }

    fun handleGatewayEvent(
        event: String,
        payloadJson: String?,
    ) {
        if (event != "chat") return
        if (payloadJson.isNullOrBlank()) return
        val payload =
            try {
                json.parseToJsonElement(payloadJson).asObjectOrNull()
            } catch (_: Throwable) {
                null
            } ?: return

        val runId =
            pendingRunId ?: run {
                Log.d("MicCapture", "no pendingRunId — drop")
                return
            }
        val eventRunId = payload["runId"].asStringOrNull() ?: return
        if (eventRunId != runId) {
            Log.d("MicCapture", "runId mismatch: event=$eventRunId pending=$runId")
            return
        }

        when (payload["state"].asStringOrNull()) {
            "delta" -> {
                val deltaText = parseAssistantText(payload)
                if (!deltaText.isNullOrBlank()) {
                    Log.d(tag, "caption translation delta chars=${deltaText.length}")
                    upsertPendingAssistant(text = deltaText.trim(), isStreaming = true)
                }
            }
            "final" -> {
                val finalText = parseAssistantText(payload)?.trim().orEmpty()
                if (finalText.isNotEmpty()) {
                    Log.d(tag, "caption translation final chars=${finalText.length}")
                    upsertPendingAssistant(text = finalText, isStreaming = false)
                    playAssistantReplyAsync(finalText)
                } else if (pendingAssistantEntryId != null) {
                    updateConversationEntry(pendingAssistantEntryId!!, text = null, isStreaming = false)
                }
                completePendingTurn()
            }
            "error" -> {
                val errorMessage =
                    payload["errorMessage"]
                        .asStringOrNull()
                        ?.trim()
                        .orEmpty()
                        .ifEmpty { "Voice request failed" }
                upsertPendingAssistant(text = errorMessage, isStreaming = false)
                completePendingTurn()
            }
            "aborted" -> {
                upsertPendingAssistant(text = "Response aborted", isStreaming = false)
                completePendingTurn()
            }
        }
    }

    private fun start() {
        stopRequested = false
        // setMicEnabled(true) starts a fresh recognizer session.
        captionTranscriptAccumulator.reset()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _statusText.value = "Speech recognizer unavailable"
            _micEnabled.value = false
            return
        }
        if (!hasMicPermission()) {
            _statusText.value = "Microphone permission required"
            _micEnabled.value = false
            return
        }

        mainHandler.post {
            try {
                if (recognizer == null) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(listener) }
                }
                startListeningSession()
            } catch (err: Throwable) {
                _statusText.value = "Start failed: ${err.message ?: err::class.simpleName}"
                _micEnabled.value = false
            }
        }
    }

    private fun stop() {
        stopRequested = true
        restartJob?.cancel()
        restartJob = null
        transcriptFlushJob?.cancel()
        transcriptFlushJob = null
        captionTranscriptAccumulator.reset()
        _isListening.value = false
        _statusText.value = if (_isSending.value) "Mic off · sending…" else "Mic off"
        _inputLevel.value = 0f
        mainHandler.post {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun startListeningSession() {
        val recognizerInstance = recognizer ?: return
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                recognitionLanguage()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, speechMinSessionMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, speechCompleteSilenceMs)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    speechPossibleSilenceMs,
                )
            }
        _statusText.value =
            when {
                _isSending.value -> "Listening · sending queued voice"
                hasQueuedMessages() -> "Listening · ${queuedMessageCount()} queued"
                else -> "Listening"
            }
        _isListening.value = true
        recognizerInstance.startListening(intent)
    }

    private fun scheduleRestart(delayMs: Long = 300L) {
        if (stopRequested) return
        if (!_micEnabled.value) return
        restartJob?.cancel()
        restartJob =
            scope.launch {
                delay(delayMs)
                mainHandler.post {
                    if (stopRequested || !_micEnabled.value) return@post
                    try {
                        startListeningSession()
                    } catch (_: Throwable) {
                        // retry through onError
                    }
                }
            }
    }

    private fun queueRecognizedMessage(text: String) {
        val message = text.trim()
        _liveTranscript.value = null
        if (message.isEmpty()) return
        Log.d(tag, "caption turn queued chars=${message.length}")
        appendConversation(
            role = VoiceConversationRole.User,
            text = message,
        )
        enqueueMessage(message)
        publishQueue()
    }

    private fun scheduleTranscriptFlush(expectedText: String) {
        transcriptFlushJob?.cancel()
        transcriptFlushJob =
            scope.launch {
                delay(transcriptIdleFlushMs)
                if (!_micEnabled.value || _isSending.value) return@launch
                val current = _liveTranscript.value?.trim().orEmpty()
                if (current.isEmpty() || current != expectedText) return@launch
                val unsent = captionTranscriptAccumulator.flushPartial(current)
                if (unsent != null) {
                    queueRecognizedMessage(unsent)
                    sendQueuedIfIdle()
                }
            }
    }

    private fun publishQueue() {
        _queuedMessages.value = snapshotMessageQueue()
    }

    private fun sendQueuedIfIdle() {
        if (_isSending.value) return
        if (!hasQueuedMessages()) {
            if (_micEnabled.value) {
                _statusText.value = "Listening"
            } else {
                _statusText.value = "Mic off"
            }
            return
        }
        if (!gatewayConnected) {
            _statusText.value = queuedWaitingStatus()
            return
        }

        val next = firstQueuedMessage() ?: return
        _isSending.value = true
        pendingRunTimeoutJob?.cancel()
        pendingRunTimeoutJob = null
        _statusText.value = if (_micEnabled.value) "Listening · sending queued voice" else "Sending queued voice"

        scope.launch {
            try {
                Log.d(tag, "caption gateway send chars=${next.length} queued=${queuedMessageCount()}")
                val runId =
                    sendToGateway(next) { earlyRunId ->
                        // Called with the idempotency key before chat.send fires so that
                        // pendingRunId is populated before any chat events can arrive.
                        pendingRunId = earlyRunId
                    }
                // Update to the real runId if the gateway returned a different one.
                if (runId != null && runId != pendingRunId) pendingRunId = runId
                if (runId == null) {
                    pendingRunTimeoutJob?.cancel()
                    pendingRunTimeoutJob = null
                    removeFirstQueuedMessage()
                    publishQueue()
                    _isSending.value = false
                    pendingAssistantEntryId = null
                    sendQueuedIfIdle()
                } else {
                    armPendingRunTimeout(runId)
                }
            } catch (err: Throwable) {
                pendingRunTimeoutJob?.cancel()
                pendingRunTimeoutJob = null
                _isSending.value = false
                pendingRunId = null
                pendingAssistantEntryId = null
                _statusText.value =
                    if (!gatewayConnected) {
                        queuedWaitingStatus()
                    } else {
                        "Send failed: ${err.message ?: err::class.simpleName}"
                    }
            }
        }
    }

    private fun armPendingRunTimeout(runId: String) {
        pendingRunTimeoutJob?.cancel()
        pendingRunTimeoutJob =
            scope.launch {
                delay(pendingRunTimeoutMs)
                if (pendingRunId != runId) return@launch
                pendingRunId = null
                pendingAssistantEntryId = null
                _isSending.value = false
                _statusText.value =
                    if (gatewayConnected) {
                        "Voice reply timed out; retrying queued turn"
                    } else {
                        queuedWaitingStatus()
                    }
                sendQueuedIfIdle()
            }
    }

    private fun completePendingTurn() {
        pendingRunTimeoutJob?.cancel()
        pendingRunTimeoutJob = null
        if (removeFirstQueuedMessage() != null) {
            publishQueue()
        }
        pendingRunId = null
        pendingAssistantEntryId = null
        _isSending.value = false
        sendQueuedIfIdle()
    }

    private fun queuedWaitingStatus(): String = "${queuedMessageCount()} queued · waiting for gateway"

    private fun appendConversation(
        role: VoiceConversationRole,
        text: String,
        isStreaming: Boolean = false,
    ): String {
        val id = UUID.randomUUID().toString()
        _conversation.value =
            (_conversation.value + VoiceConversationEntry(id = id, role = role, text = text, isStreaming = isStreaming))
                .takeLast(maxConversationEntries)
        return id
    }

    private fun updateConversationEntry(
        id: String,
        text: String?,
        isStreaming: Boolean,
    ) {
        val current = _conversation.value
        if (current.isEmpty()) return

        val targetIndex =
            when {
                current[current.lastIndex].id == id -> current.lastIndex
                else -> current.indexOfFirst { it.id == id }
            }
        if (targetIndex < 0) return

        val entry = current[targetIndex]
        val updatedText = text ?: entry.text
        if (updatedText == entry.text && entry.isStreaming == isStreaming) return
        val updated = current.toMutableList()
        updated[targetIndex] = entry.copy(text = updatedText, isStreaming = isStreaming)
        _conversation.value = updated
    }

    private fun upsertPendingAssistant(
        text: String,
        isStreaming: Boolean,
    ) {
        val currentId = pendingAssistantEntryId
        if (currentId == null) {
            pendingAssistantEntryId =
                appendConversation(
                    role = VoiceConversationRole.Assistant,
                    text = text,
                    isStreaming = isStreaming,
                )
            return
        }
        updateConversationEntry(id = currentId, text = text, isStreaming = isStreaming)
    }

    private fun playAssistantReplyAsync(text: String) {
        val spoken = text.trim()
        if (spoken.isEmpty()) return
        scope.launch {
            try {
                speakAssistantReply(spoken)
            } catch (err: Throwable) {
                Log.w(tag, "assistant speech failed: ${err.message ?: err::class.simpleName}")
            }
        }
    }

    private fun disableMic(status: String) {
        stopRequested = true
        restartJob?.cancel()
        restartJob = null
        transcriptFlushJob?.cancel()
        transcriptFlushJob = null
        captionTranscriptAccumulator.reset()
        _micEnabled.value = false
        _isListening.value = false
        _inputLevel.value = 0f
        _statusText.value = status
        mainHandler.post {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun hasMicPermission(): Boolean =
        (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )

    private fun parseAssistantText(payload: JsonObject): String? {
        val message = payload["message"].asObjectOrNull() ?: return null
        if (message["role"].asStringOrNull() != "assistant") return null
        val content = message["content"] as? JsonArray ?: return null

        val parts =
            content.mapNotNull { item ->
                val obj = item.asObjectOrNull() ?: return@mapNotNull null
                if (obj["type"].asStringOrNull() != "text") return@mapNotNull null
                obj["text"].asStringOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            }
        if (parts.isEmpty()) return null
        return parts.joinToString("\n")
    }

    private val listener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _inputLevel.value = level
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _inputLevel.value = 0f
                // Android delivers this before onResults. Restarting here can race
                // the final callback, surface ERROR_RECOGNIZER_BUSY, and add a
                // retry delay to the next caption. onResults/onError own restart.
            }

            override fun onError(error: Int) {
                if (stopRequested) return
                _isListening.value = false
                _inputLevel.value = 0f
                // This recognizer session cannot yield a final result now.
                // Do not let a scheduled partial from the failed session reach
                // the gateway after its retry has begun. In particular, an
                // ERROR_RECOGNIZER_BUSY callback can arrive while the idle
                // flush delay is still pending.
                transcriptFlushJob?.cancel()
                transcriptFlushJob = null
                _liveTranscript.value = null
                captionTranscriptAccumulator.reset()
                val status =
                    when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Listening"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported on this device"
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable on this device"
                        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech service disconnected"
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Speech requests limited; retrying"
                        else -> "Speech error ($error)"
                    }
                _statusText.value = status

                if (
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                ) {
                    disableMic(status)
                    return
                }

                val restartDelayMs =
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        -> 1_200L
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> 2_500L
                        else -> 600L
                    }
                scheduleRestart(delayMs = restartDelayMs)
            }

            override fun onResults(results: Bundle?) {
                transcriptFlushJob?.cancel()
                transcriptFlushJob = null
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().firstOrNull()
                if (!text.isNullOrBlank()) {
                    val trimmed = text.trim()
                    Log.d(tag, "caption recognizer final chars=${trimmed.length}")
                    val unsent = captionTranscriptAccumulator.finish(trimmed)
                    if (unsent != null) {
                        queueRecognizedMessage(unsent)
                        sendQueuedIfIdle()
                    } else {
                        _liveTranscript.value = null
                    }
                } else {
                    // A completed callback without text still ends this
                    // recognizer session, so stale partials cannot suppress
                    // a phrase after the scheduled restart.
                    captionTranscriptAccumulator.reset()
                }
                scheduleRestart()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().firstOrNull()
                if (!text.isNullOrBlank()) {
                    val trimmed = text.trim()
                    Log.d(tag, "caption recognizer partial chars=${trimmed.length}")
                    _liveTranscript.value = trimmed
                    scheduleTranscriptFlush(trimmed)
                }
            }

            override fun onEvent(
                eventType: Int,
                params: Bundle?,
            ) {}
        }
}

private fun kotlinx.serialization.json.JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun kotlinx.serialization.json.JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
