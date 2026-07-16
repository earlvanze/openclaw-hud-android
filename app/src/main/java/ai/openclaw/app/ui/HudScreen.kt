@file:Suppress("FunctionName")

package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionDisplaySettings
import ai.openclaw.app.AirVisionHudDoubleTapAction
import ai.openclaw.app.AirVisionHudFrameShape
import ai.openclaw.app.AirVisionHudHorizontalSwipeAction
import ai.openclaw.app.AirVisionHudPlacement
import ai.openclaw.app.AirVisionHudSwipeAction
import ai.openclaw.app.AirVisionHudTouchAction
import ai.openclaw.app.AirVisionHudTouchCommand
import ai.openclaw.app.AirVisionSplendidMode
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.TranslationCaptionMode
import ai.openclaw.app.airVisionHudDoubleTapCommand
import ai.openclaw.app.airVisionHudSingleTapCommand
import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.hudNotificationOpenResultMessage
import ai.openclaw.app.openNativeCaptionSettings
import ai.openclaw.app.voice.VoiceConversationEntry
import ai.openclaw.app.voice.VoiceConversationRole
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val hudBackground = Color.Black
private val hudText = Color(0xFFB8FFB0)
private val hudSecondary = Color(0xFF66FF66)
private val hudMuted = Color(0xFF1F6B2B)
private val hudAccent = Color(0xFF00FF66)
private val hudWarn = Color(0xFFA7FF4A)
private val hudDanger = Color(0xFF7CFF7C)
private val hudMicOn = Color(0xFFFF2D2D)
private val hudMicOff = Color(0xFF3A0505)
private val hudConnectedOff = Color(0xFF07210D)
private const val HUD_FRAME_MORPH_DURATION_MS = 320

@Composable
fun HudScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val statusText by viewModel.statusText.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isNodeConnected by viewModel.isNodeConnected.collectAsState()
    val serverName by viewModel.serverName.collectAsState()
    val remoteAddress by viewModel.remoteAddress.collectAsState()
    val mainSessionKey by viewModel.mainSessionKey.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val healthOk by viewModel.chatHealthOk.collectAsState()
    val thinkingLevel by viewModel.chatThinkingLevel.collectAsState()
    val streamingAssistantText by viewModel.chatStreamingAssistantText.collectAsState()
    val pendingToolCalls by viewModel.chatPendingToolCalls.collectAsState()
    val pendingRunCount by viewModel.pendingRunCount.collectAsState()
    val chatError by viewModel.chatError.collectAsState()
    val micEnabled by viewModel.micEnabled.collectAsState()
    val speakerEnabled by viewModel.speakerEnabled.collectAsState()
    val micStatusText by viewModel.micStatusText.collectAsState()
    val micLiveTranscript by viewModel.micLiveTranscript.collectAsState()
    val micConversation by viewModel.micConversation.collectAsState()
    val micInputLevel by viewModel.micInputLevel.collectAsState()
    val translationCaptionsEnabled by viewModel.translationCaptionsEnabled.collectAsState()
    val translationCaptionTargetLanguage by viewModel.translationCaptionTargetLanguage.collectAsState()
    val nativeCaptionsEnabled by viewModel.nativeCaptionsEnabled.collectAsState()
    val notificationSnapshot by viewModel.notificationSnapshot.collectAsState()
    val pendingTrust by viewModel.pendingGatewayTrust.collectAsState()
    val airVisionSettings by viewModel.airVisionDisplaySettings.collectAsState()
    val airVisionHudControls by viewModel.airVisionHudControls.collectAsState()
    val airVisionDemoModeEnabled by viewModel.airVisionDemoModeEnabled.collectAsState()
    val airVisionIdentifyToken by viewModel.airVisionIdentifyToken.collectAsState()
    val hudDisplayRoute by viewModel.airVisionHudDisplayRoute.collectAsState()
    val prompt by viewModel.hudPromptDraft.collectAsState()
    var identifyVisible by remember { mutableStateOf(false) }
    var transientHudText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mainSessionKey) {
        viewModel.loadChat(mainSessionKey)
        viewModel.refreshChatSessions(limit = 20)
    }

    pendingTrust?.let { promptTrust ->
        AlertDialog(
            onDismissRequest = { viewModel.declineGatewayTrustPrompt() },
            containerColor = hudBackground,
            title = { Text("Trust this gateway?", style = hudReadableTextStyle, color = hudText) },
            text = {
                Text(
                    "Verify SHA-256 fingerprint:\n${promptTrust.fingerprintSha256}",
                    style = hudReadableTextStyle,
                    color = hudSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptGatewayTrustPrompt() }) {
                    Text("Trust", color = hudAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineGatewayTrustPrompt() }) {
                    Text("Cancel", color = hudSecondary)
                }
            },
        )
    }

    val notificationLines =
        if (airVisionDemoModeEnabled) {
            demoHudNotificationLines
        } else {
            selectHudNotifications(notificationSnapshot.notifications)
        }
    val notificationKeys = notificationLines.map { it.key }
    var selectedNotificationKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notificationKeys) {
        if (selectedNotificationKey !in notificationKeys) {
            selectedNotificationKey = notificationKeys.firstOrNull()
        }
    }
    val notificationLine =
        notificationLines.firstOrNull { it.key == selectedNotificationKey }
            ?: notificationLines.firstOrNull()
    val runningLine =
        if (airVisionDemoModeEnabled) {
            null
        } else {
            hudRunningLine(pendingRunCount, pendingToolCalls.map { it.name })
        }
    val latestAssistant = streamingAssistantText?.trim()?.takeIf { it.isNotEmpty() } ?: messages.latestAssistantText()
    val latestUser = messages.latestUserText()
    val chatTranscript =
        remember(messages, streamingAssistantText, airVisionDemoModeEnabled) {
            if (airVisionDemoModeEnabled) {
                demoHudChatTranscript
            } else {
                hudChatTranscript(messages = messages, streamingAssistantText = streamingAssistantText)
            }
        }
    val chatScrollState = rememberScrollState()
    LaunchedEffect(viewModel, chatScrollState) {
        viewModel.hudScrollRequests.collect { deltaPx ->
            chatScrollState.scrollBy(deltaPx)
        }
    }
    LaunchedEffect(airVisionIdentifyToken) {
        if (airVisionIdentifyToken <= 0L) return@LaunchedEffect
        identifyVisible = true
        delay(3500)
        identifyVisible = false
    }
    LaunchedEffect(viewModel) {
        viewModel.hudTransientMessages.collectLatest { message ->
            transientHudText = message
            delay(1800)
            transientHudText = null
        }
    }
    val primaryLine =
        if (airVisionDemoModeEnabled) {
            "Walking HUD ready"
        } else {
            runningLine
                ?: latestAssistant
                ?: micLiveTranscript?.trim()?.takeIf { it.isNotEmpty() }
                ?: latestUser
        }
    val secondaryLine =
        if (airVisionDemoModeEnabled) {
            "Maps, captions, mic, and assistant status stay glanceable."
        } else {
            chatError?.trim()?.takeIf { it.isNotEmpty() }
                ?: micLiveTranscript?.trim()?.takeIf { it.isNotEmpty() }
                ?: latestUser?.takeIf { latestAssistant != null && it != latestAssistant }
                ?: listOfNotNull(serverName, remoteAddress)
                    .joinToString(" / ")
                    .takeIf { !isConnected && it.isNotBlank() }
                ?: statusText.trim().takeIf { !isConnected && it.isNotBlank() }
        }
    val warning = !airVisionDemoModeEnabled && (chatError != null || (!isConnected && !isNodeConnected))
    val sessionLine =
        if (airVisionDemoModeEnabled) {
            "hud demo / session hud / mic listening"
        } else {
            hudSessionText(
                mainSessionKey = mainSessionKey,
                pendingRunCount = pendingRunCount,
                pendingToolCalls = pendingToolCalls.map { it.name },
                healthOk = healthOk,
                micStatusText = micStatusText,
                micInputLevel = micInputLevel,
                micEnabled = micEnabled,
            )
        }
    val hudScale =
        (
            AirVisionDisplaySettings.hudScaleForDistanceCm(airVisionSettings.distanceCm) *
                AirVisionDisplaySettings.hudScaleMultiplierForViewMode(airVisionSettings.viewMode) *
                AirVisionDisplaySettings.hudScaleMultiplierForPercent(airVisionSettings.hudScalePercent)
        ).coerceIn(0.60f, 1.60f)
    val dimAlpha = AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(airVisionSettings.brightnessPercent)
    val splendidOverlayAlpha =
        AirVisionDisplaySettings.hudColorPreviewAlpha(
            alpha =
                AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                    splendidMode = airVisionSettings.splendidMode,
                    blueLightFilterPercent = airVisionSettings.blueLightFilterPercent,
                ),
            lightLoadModeEnabled = airVisionSettings.lightLoadModeEnabled,
        )
    val transcriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(airVisionSettings.lightLoadModeEnabled)
    val captionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(airVisionSettings.lightLoadModeEnabled)
    val safePadding = (22 + airVisionSettings.safeAreaPercent * 3).dp
    val layoutSpec = hudLayoutSpec(airVisionSettings.hudPlacement)
    val frameSpec = hudFrameSpec(airVisionSettings.hudFrameShape)
    val frameWidthFraction by
        animateFloatAsState(
            targetValue = (layoutSpec.widthFraction * frameSpec.widthMultiplier).coerceIn(0.42f, 0.98f),
            animationSpec = tween(durationMillis = HUD_FRAME_MORPH_DURATION_MS),
            label = "HUD frame width",
        )
    val frameHeightFraction by
        animateFloatAsState(
            targetValue = frameSpec.heightFraction,
            animationSpec = tween(durationMillis = HUD_FRAME_MORPH_DURATION_MS),
            label = "HUD frame height",
        )
    val framePaddingScale by
        animateFloatAsState(
            targetValue = frameSpec.paddingScale,
            animationSpec = tween(durationMillis = HUD_FRAME_MORPH_DURATION_MS),
            label = "HUD frame padding",
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(hudBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(safePadding)
                .hudTouchGestures(
                    singleTapKey = notificationLine?.key to airVisionHudControls.singleTapAction,
                    doubleTapKey = notificationLine?.key to airVisionHudControls.doubleTapAction,
                    swipeKey = airVisionHudControls.swipeAction to airVisionHudControls.horizontalSwipeAction,
                    onSingleTap = {
                        performHudSingleTapAction(
                            action = airVisionHudControls.singleTapAction,
                            notificationLine = notificationLine,
                            demoMode = airVisionDemoModeEnabled,
                            viewModel = viewModel,
                        )
                    },
                    onDoubleTap = {
                        performHudDoubleTapAction(
                            action = airVisionHudControls.doubleTapAction,
                            notificationLine = notificationLine,
                            demoMode = airVisionDemoModeEnabled,
                            viewModel = viewModel,
                        )
                    },
                    onSwipeStarted = {
                        if (
                            airVisionHudControls.swipeAction == AirVisionHudSwipeAction.ScrollChat &&
                            chatScrollState.maxValue == 0
                        ) {
                            viewModel.showHudTransientMessage("Nothing to scroll")
                        }
                    },
                    onVerticalSwipe = { deltaPx ->
                        if (airVisionHudControls.swipeAction == AirVisionHudSwipeAction.ScrollChat) {
                            chatScrollState.dispatchRawDelta(deltaPx)
                        }
                    },
                    onHorizontalSwipe = { direction ->
                        if (
                            airVisionHudControls.horizontalSwipeAction ==
                            AirVisionHudHorizontalSwipeAction.BrowseNotifications
                        ) {
                            val next =
                                adjacentHudNotification(
                                    notifications = notificationLines,
                                    currentKey = notificationLine?.key,
                                    direction = direction,
                                )
                            when {
                                next == null -> viewModel.showHudTransientMessage("No notifications")
                                notificationLines.size == 1 -> viewModel.showHudTransientMessage("1 notification")
                                else -> {
                                    selectedNotificationKey = next.key
                                    val position = notificationLines.indexOfFirst { it.key == next.key } + 1
                                    viewModel.showHudTransientMessage("${next.source} $position/${notificationLines.size}")
                                }
                            }
                        }
                    },
                ),
    ) {
        if (splendidOverlayAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                        .background(
                            hudSplendidOverlayColor(airVisionSettings.splendidMode)
                                .copy(alpha = splendidOverlayAlpha),
                        ),
            )
        }
        if (dimAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                        .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }

        HudSignalLights(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f),
            isConnected = if (airVisionDemoModeEnabled) true else isConnected,
            micEnabled = if (airVisionDemoModeEnabled) true else micEnabled,
            micInputLevel = if (airVisionDemoModeEnabled) 0.25f else micInputLevel,
            speakerEnabled = if (airVisionDemoModeEnabled) true else speakerEnabled,
            thinkingLevel = if (airVisionDemoModeEnabled) "off" else thinkingLevel,
            translationCaptionsEnabled = if (airVisionDemoModeEnabled) true else translationCaptionsEnabled,
            translationCaptionTargetLanguage = if (airVisionDemoModeEnabled) "es" else translationCaptionTargetLanguage,
            nativeCaptionsEnabled = if (airVisionDemoModeEnabled) false else nativeCaptionsEnabled,
            onToggleThinking = { viewModel.setChatThinkingLevel(nextHudThinkingLevel(thinkingLevel)) },
            onToggleTranslationCaptions = {
                when (
                    HudCaptionProviders.next(
                        nativeEnabled = nativeCaptionsEnabled,
                        openClawEnabled = translationCaptionsEnabled,
                    )
                ) {
                    HudCaptionProvider.Native -> {
                        viewModel.setTranslationCaptionsEnabled(false)
                        viewModel.setNativeCaptionsEnabled(true)
                        openNativeCaptionSettings(context)
                    }
                    HudCaptionProvider.OpenClaw -> {
                        viewModel.setNativeCaptionsEnabled(false)
                        viewModel.setTranslationCaptionsEnabled(true)
                    }
                    HudCaptionProvider.Off -> {
                        viewModel.setNativeCaptionsEnabled(false)
                        viewModel.setTranslationCaptionsEnabled(false)
                    }
                }
            },
        )

        Column(
            modifier =
                Modifier
                    .align(layoutSpec.alignment)
                    .zIndex(1f)
                    .fillMaxWidth(frameWidthFraction)
                    .fillMaxHeight(frameHeightFraction),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = hudScale
                            scaleY = hudScale
                            transformOrigin = layoutSpec.transformOrigin
                        }.padding(top = layoutSpec.topPadding * framePaddingScale)
                        .padding(
                            end = layoutSpec.trailingPadding * framePaddingScale,
                        ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (transientHudText != null) {
                    HudTransientText(message = transientHudText.orEmpty())
                } else {
                    notificationLine?.let { line ->
                        HudNotificationBlock(
                            line = line,
                            onDismiss = { viewModel.dismissNotification(line.key) },
                        )
                    }
                }

                prompt.trim().takeIf { it.isNotEmpty() && !airVisionDemoModeEnabled }?.let { draft ->
                    HudPromptMirror(draft = draft)
                }

                if (airVisionDemoModeEnabled) {
                    HudChatTranscript(
                        entries = demoHudChatTranscript.takeLast(transcriptEntryCount),
                        scrollState = chatScrollState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                    )
                } else if (translationCaptionsEnabled) {
                    HudTranslationCaptions(
                        conversation = micConversation,
                        liveTranscript = micLiveTranscript,
                        targetLanguageCode = translationCaptionTargetLanguage,
                        entryCount = captionEntryCount,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                    )
                } else if (chatTranscript.isNotEmpty()) {
                    HudChatTranscript(
                        entries = chatTranscript.takeLast(transcriptEntryCount),
                        scrollState = chatScrollState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                    )
                } else {
                    primaryLine?.takeIf { it.isNotBlank() }?.let { line ->
                        Text(
                            line,
                            style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = if (warning) hudWarn else hudText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    secondaryLine?.takeIf { it.isNotBlank() }?.let { line ->
                        Text(
                            line,
                            style = hudReadableTextStyle,
                            color = if (warning) hudDanger else hudSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                sessionLine?.let { line ->
                    Text(
                        line,
                        style = hudReadableTextStyle,
                        color = hudMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HudChatInputBar(
                prompt = prompt,
                enabled = !airVisionDemoModeEnabled && healthOk && pendingRunCount == 0,
                onPromptChange = viewModel::setHudPromptDraft,
                onSend = {
                    val text = prompt.trim()
                    if (text.isNotEmpty()) {
                        viewModel.clearHudPromptDraft()
                        viewModel.sendChat(
                            message = text,
                            thinking = if (translationCaptionsEnabled) "off" else thinkingLevel,
                            attachments = emptyList(),
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(
                            end = layoutSpec.trailingPadding * framePaddingScale,
                            bottom = layoutSpec.bottomPadding * framePaddingScale,
                        ),
            )
        }

        if (identifyVisible) {
            HudDisplayIdentifyOverlay(
                displayLabel = hudDisplayRoute.selectedCandidate?.name?.ifBlank { null } ?: "External display",
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .zIndex(2f),
            )
        }
    }
}

@Composable
private fun HudTransientText(
    message: String,
) {
    Text(
        message,
        style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
        color = hudAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HudDisplayIdentifyOverlay(
    displayLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "HUD 1",
            style =
                TextStyle(
                    fontSize = 72.sp,
                    lineHeight = 78.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = hudAccent,
            maxLines = 1,
        )
        Text(
            displayLabel,
            style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
            color = hudSecondary,
            maxLines = 1,
        )
    }
}

private data class HudLayoutSpec(
    val alignment: Alignment,
    val widthFraction: Float,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val trailingPadding: Dp,
    val transformOrigin: TransformOrigin,
)

internal data class HudFrameSpec(
    val widthMultiplier: Float,
    val heightFraction: Float,
    val paddingScale: Float,
)

internal fun hudFrameSpec(shape: AirVisionHudFrameShape): HudFrameSpec =
    when (shape) {
        AirVisionHudFrameShape.Full ->
            HudFrameSpec(
                widthMultiplier = 1.08f,
                heightFraction = 1.0f,
                paddingScale = 1.0f,
            )
        AirVisionHudFrameShape.Wide ->
            HudFrameSpec(
                widthMultiplier = 1.0f,
                heightFraction = 0.82f,
                paddingScale = 0.76f,
            )
        AirVisionHudFrameShape.Compact ->
            HudFrameSpec(
                widthMultiplier = 0.78f,
                heightFraction = 0.66f,
                paddingScale = 0.46f,
            )
        AirVisionHudFrameShape.Panoramic ->
            HudFrameSpec(
                widthMultiplier = 1.10f,
                heightFraction = 0.46f,
                paddingScale = 0.18f,
            )
    }

private fun hudSplendidOverlayColor(mode: AirVisionSplendidMode): Color =
    when (mode) {
        AirVisionSplendidMode.Standard -> Color.Transparent
        AirVisionSplendidMode.Theater -> Color(0xFFFF9E4A)
        AirVisionSplendidMode.Office -> Color(0xFFEAF7FF)
        AirVisionSplendidMode.Game -> Color(0xFF5CB2FF)
        AirVisionSplendidMode.EyeCare -> Color(0xFFFFB15C)
    }

private fun hudLayoutSpec(placement: AirVisionHudPlacement): HudLayoutSpec =
    when (placement) {
        AirVisionHudPlacement.UpperLeft ->
            HudLayoutSpec(
                alignment = Alignment.TopStart,
                widthFraction = 0.90f,
                topPadding = 34.dp,
                bottomPadding = 0.dp,
                trailingPadding = 48.dp,
                transformOrigin = TransformOrigin(0f, 0f),
            )
        AirVisionHudPlacement.UpperCenter ->
            HudLayoutSpec(
                alignment = Alignment.TopCenter,
                widthFraction = 0.82f,
                topPadding = 34.dp,
                bottomPadding = 0.dp,
                trailingPadding = 0.dp,
                transformOrigin = TransformOrigin(0.5f, 0f),
            )
        AirVisionHudPlacement.UpperRight ->
            HudLayoutSpec(
                alignment = Alignment.TopEnd,
                widthFraction = 0.84f,
                topPadding = 34.dp,
                bottomPadding = 0.dp,
                trailingPadding = 0.dp,
                transformOrigin = TransformOrigin(1f, 0f),
            )
        AirVisionHudPlacement.Center ->
            HudLayoutSpec(
                alignment = Alignment.Center,
                widthFraction = 0.78f,
                topPadding = 116.dp,
                bottomPadding = 42.dp,
                trailingPadding = 0.dp,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
            )
        AirVisionHudPlacement.LowerCenter ->
            HudLayoutSpec(
                alignment = Alignment.BottomCenter,
                widthFraction = 0.82f,
                topPadding = 180.dp,
                bottomPadding = 10.dp,
                trailingPadding = 0.dp,
                transformOrigin = TransformOrigin(0.5f, 1f),
            )
    }

private fun performHudSingleTapAction(
    action: AirVisionHudTouchAction,
    notificationLine: HudNotificationLine?,
    demoMode: Boolean,
    viewModel: MainViewModel,
) {
    val command =
        airVisionHudSingleTapCommand(
            action = action,
            notificationKey = notificationLine?.key,
            notificationClearable = notificationLine?.isClearable == true,
        )
    when {
        command == null && action == AirVisionHudTouchAction.OpenNotification ->
            viewModel.showHudTransientMessage("No notification to open")
        command == null && action == AirVisionHudTouchAction.DismissNotification ->
            viewModel.showHudTransientMessage("No notification to dismiss")
        else ->
            performHudTouchCommand(
                command = command,
                notificationSource = notificationLine?.source,
                demoMode = demoMode,
                viewModel = viewModel,
            )
    }
}

private fun performHudDoubleTapAction(
    action: AirVisionHudDoubleTapAction,
    notificationLine: HudNotificationLine?,
    demoMode: Boolean,
    viewModel: MainViewModel,
) {
    val command =
        airVisionHudDoubleTapCommand(
            action = action,
            notificationKey = notificationLine?.key,
            notificationClearable = notificationLine?.isClearable == true,
        )
    when {
        command == null && action == AirVisionHudDoubleTapAction.OpenNotification ->
            viewModel.showHudTransientMessage("No notification to open")
        command == null && action == AirVisionHudDoubleTapAction.DismissNotification ->
            viewModel.showHudTransientMessage("No notification to dismiss")
        else ->
            performHudTouchCommand(
                command = command,
                notificationSource = notificationLine?.source,
                demoMode = demoMode,
                viewModel = viewModel,
            )
    }
}

private fun performHudTouchCommand(
    command: AirVisionHudTouchCommand?,
    notificationSource: String?,
    demoMode: Boolean,
    viewModel: MainViewModel,
) {
    when (command) {
        null -> Unit
        AirVisionHudTouchCommand.ToggleMic -> {
            viewModel.toggleMicEnabled()
            viewModel.showHudTransientMessage("Mic toggled")
        }
        is AirVisionHudTouchCommand.OpenNotification -> {
            if (demoMode) {
                viewModel.showHudTransientMessage("Demo: would open ${notificationSource ?: "notification"} on phone")
            } else {
                val result = viewModel.openNotification(command.key)
                viewModel.showHudTransientMessage(
                    hudNotificationOpenResultMessage(ok = result.ok, code = result.code),
                )
            }
        }
        is AirVisionHudTouchCommand.DismissNotification -> {
            viewModel.dismissNotification(command.key)
            viewModel.showHudTransientMessage("Notification dismissed")
        }
    }
}

@Composable
private fun HudNotificationBlock(
    line: HudNotificationLine,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                line.primary,
                style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
                color = hudText,
            )
            line.secondary?.takeIf { it.isNotBlank() }?.let { secondary ->
                Text(
                    secondary,
                    style = hudReadableTextStyle,
                    color = hudSecondary,
                )
            }
        }
        if (line.isClearable) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("x", style = hudReadableTextStyle, color = hudMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HudChatTranscript(
    entries: List<HudChatTranscriptEntry>,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            Text(
                entry.text,
                style =
                    if (index == entries.lastIndex) {
                        hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        hudReadableTextStyle
                    },
                color =
                    when (entry.role) {
                        "user" -> hudSecondary
                        "assistant" -> hudText
                        else -> hudWarn
                    },
                maxLines = if (index == entries.lastIndex) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HudTranslationCaptions(
    conversation: List<VoiceConversationEntry>,
    liveTranscript: String?,
    targetLanguageCode: String,
    entryCount: Int,
    modifier: Modifier = Modifier,
) {
    val entries = hudCaptionEntries(conversation, liveTranscript, entryCount)
    val targetLanguageLabel = TranslationCaptionMode.languageFor(targetLanguageCode).label.lowercase()
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "cc $targetLanguageLabel",
            style = hudReadableTextStyle,
            color = hudMuted,
            maxLines = 1,
        )
        if (entries.isEmpty()) {
            Text(
                "listening for captions",
                style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
                color = hudMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            entries.forEachIndexed { index, entry ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.speaker,
                        style = hudReadableTextStyle,
                        color = if (entry.speaker == "S1") hudSecondary else hudAccent,
                        maxLines = 1,
                    )
                    Text(
                        entry.text,
                        style =
                            if (index == entries.lastIndex) {
                                hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold)
                            } else {
                                hudReadableTextStyle
                            },
                        color = if (entry.isLive) hudMuted else hudText,
                        maxLines = if (index == entries.lastIndex) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HudPromptMirror(draft: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "ask",
            style = hudReadableTextStyle,
            color = hudMuted,
            maxLines = 1,
        )
        Text(
            draft,
            style = hudPrimaryTextStyle.copy(fontWeight = FontWeight.SemiBold),
            color = hudText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HudChatInputBar(
    prompt: String,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChange,
            enabled = enabled,
            singleLine = false,
            maxLines = 3,
            textStyle = hudReadableTextStyle.copy(color = hudText),
            modifier =
                Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        val isEnter =
                            event.key == Key.Enter ||
                                event.key == Key.NumPadEnter
                        if (
                            enabled &&
                            isEnter &&
                            event.type == KeyEventType.KeyUp &&
                            !event.isShiftPressed &&
                            prompt.trim().isNotEmpty()
                        ) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    },
            decorationBox = { innerTextField ->
                Box {
                    if (prompt.isBlank()) {
                        Text("ask", style = hudReadableTextStyle, color = hudMuted)
                    }
                    innerTextField()
                }
            },
        )
        Text(
            text = "send",
            modifier =
                Modifier
                    .clickable(enabled = enabled && prompt.trim().isNotEmpty(), onClick = onSend)
                    .padding(vertical = 4.dp),
            style = hudReadableTextStyle,
            color = if (enabled && prompt.trim().isNotEmpty()) hudAccent else hudMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HudSignalLights(
    isConnected: Boolean,
    micEnabled: Boolean,
    micInputLevel: Float,
    speakerEnabled: Boolean,
    thinkingLevel: String,
    translationCaptionsEnabled: Boolean,
    translationCaptionTargetLanguage: String,
    nativeCaptionsEnabled: Boolean,
    onToggleThinking: () -> Unit,
    onToggleTranslationCaptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (translationCaptionsEnabled) "off" else thinkingLevel,
            modifier =
                Modifier
                    .clickable(enabled = !translationCaptionsEnabled, onClick = onToggleThinking)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            style = hudReadableTextStyle,
            color = if (translationCaptionsEnabled || thinkingLevel == "off") hudMuted else hudSecondary,
            maxLines = 1,
        )
        Text(
            text =
                when {
                    nativeCaptionsEnabled -> "cc:native"
                    translationCaptionsEnabled -> "cc:$translationCaptionTargetLanguage"
                    else -> "cc"
                },
            modifier =
                Modifier
                    .clickable(onClick = onToggleTranslationCaptions)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            style = hudReadableTextStyle,
            color = if (nativeCaptionsEnabled || translationCaptionsEnabled) hudAccent else hudMuted,
            maxLines = 1,
        )
        Icon(
            imageVector = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (speakerEnabled) hudAccent else hudMuted,
        )
        HudDot(color = if (isConnected) hudAccent else hudConnectedOff, size = 14.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudDot(
                color =
                    when {
                        micEnabled && micInputLevel > 0.12f -> hudMicOn
                        micEnabled -> hudMicOn.copy(alpha = 0.82f)
                        else -> hudMicOff
                    },
                size = 14.dp,
            )
            if (micEnabled) {
                Text("mic", style = hudReadableTextStyle, color = hudMicOn, maxLines = 1)
            }
        }
    }
}

private fun nextHudThinkingLevel(current: String): String =
    when (current.trim().lowercase()) {
        "off" -> "low"
        "low" -> "medium"
        "medium" -> "high"
        else -> "off"
    }

@Composable
private fun HudDot(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = color,
    ) {}
}

private val hudPrimaryTextStyle: TextStyle =
    mobileDisplay.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    )

private val hudReadableTextStyle: TextStyle =
    mobileHeadline.copy(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    )

private fun hudSessionText(
    mainSessionKey: String,
    pendingRunCount: Int,
    pendingToolCalls: List<String>,
    healthOk: Boolean,
    micStatusText: String,
    micInputLevel: Float,
    micEnabled: Boolean,
): String? {
    if (pendingRunCount == 0 && pendingToolCalls.isEmpty() && healthOk && !micEnabled) return null
    val gateway =
        when {
            pendingRunCount > 0 -> "openclaw running"
            healthOk -> "openclaw ready"
            else -> "openclaw offline"
        }
    val tool = pendingToolCalls.firstOrNull()?.take(24)?.let { "tool $it" }
    val mic = if (micInputLevel > 0.12f) "$micStatusText listening" else micStatusText.takeIf { micEnabled }
    return listOfNotNull(gateway, tool, "session ${mainSessionKey.shortSessionLabel()}", mic).joinToString(" / ")
}

private fun hudRunningLine(
    pendingRunCount: Int,
    pendingToolCalls: List<String>,
): String? =
    when {
        pendingToolCalls.isNotEmpty() -> "Tool: ${pendingToolCalls.first()}"
        pendingRunCount > 1 -> "$pendingRunCount OpenClaw runs active"
        pendingRunCount == 1 -> "OpenClaw is working"
        else -> null
    }

private data class HudChatTranscriptEntry(
    val role: String,
    val text: String,
)

private val demoHudNotificationLines =
    listOf(
        HudNotificationLine(
            key = "airvision-demo-navigation",
            source = "Maps",
            primary = "Turn right on Madison St",
            secondary = "0.2 mi, then continue toward the station",
            kind = HudNotificationKind.Navigation,
            isClearable = false,
        ),
        HudNotificationLine(
            key = "airvision-demo-message",
            source = "Messages",
            primary = "Jordan",
            secondary = "Meeting moved to 3:30 PM",
            kind = HudNotificationKind.Message,
            isClearable = false,
        ),
    )

private val demoHudChatTranscript =
    listOf(
        HudChatTranscriptEntry(role = "assistant", text = "Route clear. Next turn in 0.2 mi."),
        HudChatTranscriptEntry(role = "user", text = "Summarize the last notification."),
        HudChatTranscriptEntry(role = "assistant", text = "Calendar moved to 3:30 PM. No action needed."),
    )

private data class HudCaptionEntry(
    val speaker: String,
    val text: String,
    val isLive: Boolean = false,
)

private fun hudCaptionEntries(
    conversation: List<VoiceConversationEntry>,
    liveTranscript: String?,
    entryCount: Int = AirVisionDisplaySettings.HUD_CAPTION_ENTRY_COUNT,
): List<HudCaptionEntry> {
    val entries = mutableListOf<HudCaptionEntry>()
    var userTurnIndex = 0
    var lastSpeaker = "S1"
    for (entry in conversation) {
        val text = entry.text.trim()
        if (text.isEmpty()) continue
        when (entry.role) {
            VoiceConversationRole.User -> {
                lastSpeaker = TranslationCaptionMode.speakerLabelForTurn(userTurnIndex)
                userTurnIndex += 1
            }
            VoiceConversationRole.Assistant -> {
                val (speaker, caption) = TranslationCaptionMode.stripSpeakerPrefix(text)
                entries += HudCaptionEntry(speaker = speaker ?: lastSpeaker, text = caption)
            }
        }
    }
    val live = liveTranscript?.trim()?.takeIf { it.isNotEmpty() }
    if (live != null) {
        val speaker = TranslationCaptionMode.speakerLabelForTurn(userTurnIndex)
        entries += HudCaptionEntry(speaker = speaker, text = live, isLive = true)
    }
    return entries.takeLast(entryCount)
}

private fun hudChatTranscript(
    messages: List<ChatMessage>,
    streamingAssistantText: String?,
): List<HudChatTranscriptEntry> {
    val entries =
        messages
            .mapNotNull { message ->
                val text = message.plainText(maxChars = HUD_TRANSCRIPT_ENTRY_MAX_CHARS) ?: return@mapNotNull null
                HudChatTranscriptEntry(role = message.role, text = text)
            }.takeLast(AirVisionDisplaySettings.HUD_TRANSCRIPT_ENTRY_COUNT)
            .toMutableList()

    val streaming = streamingAssistantText?.trim()?.takeIf { it.isNotEmpty() }
    if (streaming != null) {
        entries.removeLastOrNull()
        entries += HudChatTranscriptEntry(role = "assistant", text = streaming.take(HUD_TRANSCRIPT_ENTRY_MAX_CHARS))
    }
    return entries
}

private fun List<ChatMessage>.latestAssistantText(): String? =
    asReversed()
        .firstOrNull { it.role == "assistant" }
        ?.plainText(maxChars = 180)

private fun List<ChatMessage>.latestUserText(): String? =
    asReversed()
        .firstOrNull { it.role == "user" }
        ?.plainText(maxChars = 120)

private fun ChatMessage.plainText(maxChars: Int): String? =
    content
        .mapNotNull { it.text?.trim()?.takeIf { text -> text.isNotEmpty() } }
        .joinToString(" ")
        .take(maxChars)
        .trim()
        .takeIf { it.isNotEmpty() }

private fun String.shortSessionLabel(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return "main"
    return trimmed
        .substringAfter("agent:")
        .substringBefore(':')
        .take(18)
}

private const val HUD_TRANSCRIPT_ENTRY_MAX_CHARS = 360
