package ai.openclaw.app

import android.view.KeyEvent

// M1 firmware can deliver deliberate tap pairs roughly 1.5 seconds apart.
internal const val HUD_MEDIA_KEY_DOUBLE_TAP_TIMEOUT_MS = 2_000L

internal sealed interface AirVisionHudKeyCommand {
    data class ScrollChat(
        val deltaPx: Float,
    ) : AirVisionHudKeyCommand

    data class AdjustBrightness(
        val deltaPercent: Int,
    ) : AirVisionHudKeyCommand

    data class AdjustDistance(
        val deltaCm: Int,
    ) : AirVisionHudKeyCommand

    data class BrowseNotifications(
        val offset: Int,
    ) : AirVisionHudKeyCommand

    data object StartNotificationReply : AirVisionHudKeyCommand

    data object AbortActiveRun : AirVisionHudKeyCommand

    data object AllowPendingExecOnce : AirVisionHudKeyCommand

    data object DenyPendingExecApproval : AirVisionHudKeyCommand

    data object ToggleMic : AirVisionHudKeyCommand

    data object ArmMicDoubleTap : AirVisionHudKeyCommand

    data object BeginMicHold : AirVisionHudKeyCommand

    data object EndMicHold : AirVisionHudKeyCommand

    data object LogUnhandledHudAccessoryKey : AirVisionHudKeyCommand
}

internal data class AirVisionHudKeyDecision(
    val consume: Boolean,
    val command: AirVisionHudKeyCommand? = null,
)

internal fun hudKeyCommandFeedback(command: AirVisionHudKeyCommand?): String? =
    when (command) {
        AirVisionHudKeyCommand.ArmMicDoubleTap -> "Tap again within 2 seconds for mic"
        else -> null
    }

internal class AirVisionHudKeyInputController(
    private val doubleTapTimeoutMs: Long = HUD_MEDIA_KEY_DOUBLE_TAP_TIMEOUT_MS,
) {
    private var lastMicTapUptimeMs = 0L
    private var micHoldKeyCode: Int? = null

    fun handleKeyEvent(
        keyCode: Int,
        action: Int,
        eventTimeMs: Long,
        isHudAccessoryEvent: Boolean,
        controls: AirVisionHudControls,
        hasActiveRun: Boolean = false,
        hasPendingExecApproval: Boolean = false,
        canAllowPendingExecOnce: Boolean = false,
        canDenyPendingExec: Boolean = false,
    ): AirVisionHudKeyDecision {
        if (isHudAccessoryEvent && controls.customMediaKeyCode == keyCode) {
            return handleMediaKeyEvent(
                keyCode = keyCode,
                action = action,
                eventTimeMs = eventTimeMs,
                mediaKeyAction = controls.mediaKeyAction,
            )
        }

        if (isHudAccessoryEvent && hasPendingExecApproval && keyCode in hudApprovalActionKeys) {
            val command =
                if (action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BUTTON_Y ->
                            AirVisionHudKeyCommand.AllowPendingExecOnce.takeIf { canAllowPendingExecOnce }
                        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE ->
                            AirVisionHudKeyCommand.DenyPendingExecApproval.takeIf { canDenyPendingExec }
                        else -> null
                    }
                } else {
                    null
                }
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command = command,
            )
        }

        if (keyCode in hudAbortRunKeys && isHudAccessoryEvent && hasActiveRun) {
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command =
                    if (action == KeyEvent.ACTION_DOWN) {
                        AirVisionHudKeyCommand.AbortActiveRun
                    } else {
                        null
                    },
            )
        }

        if (keyCode in hudNotificationReplyKeys && isHudAccessoryEvent) {
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command =
                    if (action == KeyEvent.ACTION_DOWN) {
                        AirVisionHudKeyCommand.StartNotificationReply
                    } else {
                        null
                    },
            )
        }

        val notificationOffset = hudNotificationBrowseKeyOffsets[keyCode]
        if (notificationOffset != null && isHudAccessoryEvent) {
            if (controls.horizontalSwipeAction != AirVisionHudHorizontalSwipeAction.BrowseNotifications) {
                return AirVisionHudKeyDecision(consume = false)
            }
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command =
                    if (action == KeyEvent.ACTION_DOWN) {
                        AirVisionHudKeyCommand.BrowseNotifications(notificationOffset)
                    } else {
                        null
                    },
            )
        }

        val navigationScrollDelta = hudNavigationScrollKeyDeltas[keyCode]
        if (navigationScrollDelta != null && isHudAccessoryEvent) {
            if (controls.swipeAction != AirVisionHudSwipeAction.ScrollChat) {
                return AirVisionHudKeyDecision(consume = false)
            }
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command =
                    if (action == KeyEvent.ACTION_DOWN) {
                        AirVisionHudKeyCommand.ScrollChat(navigationScrollDelta)
                    } else {
                        null
                    },
            )
        }

        val scrollDelta = hudScrollKeyDeltas[keyCode]
        if (scrollDelta != null) {
            val command =
                when (controls.brightnessKeyAction) {
                    AirVisionHudKeyAction.None -> return AirVisionHudKeyDecision(consume = false)
                    AirVisionHudKeyAction.ScrollChat ->
                        if (action == KeyEvent.ACTION_DOWN) {
                            AirVisionHudKeyCommand.ScrollChat(scrollDelta)
                        } else {
                            null
                        }
                    AirVisionHudKeyAction.AdjustBrightness -> {
                        val brightnessDelta = hudBrightnessKeyDeltas[keyCode] ?: 0
                        if (action == KeyEvent.ACTION_DOWN && brightnessDelta != 0) {
                            AirVisionHudKeyCommand.AdjustBrightness(brightnessDelta)
                        } else {
                            null
                        }
                    }
                    AirVisionHudKeyAction.AdjustDistance -> {
                        val distanceDelta = hudDistanceKeyDeltas[keyCode] ?: 0
                        if (action == KeyEvent.ACTION_DOWN && distanceDelta != 0) {
                            AirVisionHudKeyCommand.AdjustDistance(distanceDelta)
                        } else {
                            null
                        }
                    }
                }
            return AirVisionHudKeyDecision(
                consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                command = command,
            )
        }

        val isHudMediaKey = hudMicToggleKeys.contains(keyCode) || controls.customMediaKeyCode == keyCode
        if (!isHudMediaKey || (!isHudAccessoryEvent && keyCode !in hudGlobalMicToggleKeys)) {
            return AirVisionHudKeyDecision(
                consume = false,
                command =
                    if (action == KeyEvent.ACTION_UP && isHudAccessoryEvent) {
                        AirVisionHudKeyCommand.LogUnhandledHudAccessoryKey
                    } else {
                        null
                    },
            )
        }

        return handleMediaKeyEvent(
            keyCode = keyCode,
            action = action,
            eventTimeMs = eventTimeMs,
            mediaKeyAction = controls.mediaKeyAction,
        )
    }

    private fun handleMediaKeyEvent(
        keyCode: Int,
        action: Int,
        eventTimeMs: Long,
        mediaKeyAction: AirVisionHudMediaKeyAction,
    ): AirVisionHudKeyDecision {
        if (micHoldKeyCode == keyCode && action == KeyEvent.ACTION_UP) {
            micHoldKeyCode = null
            lastMicTapUptimeMs = 0L
            return AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.EndMicHold,
            )
        }

        return when (mediaKeyAction) {
            AirVisionHudMediaKeyAction.None -> AirVisionHudKeyDecision(consume = false)
            AirVisionHudMediaKeyAction.SingleTapToggleMic -> {
                lastMicTapUptimeMs = 0L
                AirVisionHudKeyDecision(
                    consume = action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP,
                    command = AirVisionHudKeyCommand.ToggleMic.takeIf { action == KeyEvent.ACTION_UP },
                )
            }
            AirVisionHudMediaKeyAction.DoubleTapToggleMic -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    AirVisionHudKeyDecision(consume = true)
                } else if (action == KeyEvent.ACTION_UP) {
                    AirVisionHudKeyDecision(
                        consume = true,
                        command = handleMicTap(eventTimeMs),
                    )
                } else {
                    AirVisionHudKeyDecision(consume = false)
                }
            }
            AirVisionHudMediaKeyAction.HoldToTalk -> {
                lastMicTapUptimeMs = 0L
                if (action == KeyEvent.ACTION_DOWN) {
                    val begin = micHoldKeyCode == null
                    if (begin) micHoldKeyCode = keyCode
                    AirVisionHudKeyDecision(
                        consume = true,
                        command = AirVisionHudKeyCommand.BeginMicHold.takeIf { begin },
                    )
                } else {
                    AirVisionHudKeyDecision(consume = action == KeyEvent.ACTION_UP)
                }
            }
        }
    }

    fun handleMicTap(eventTimeMs: Long): AirVisionHudKeyCommand {
        val elapsedMs = eventTimeMs - lastMicTapUptimeMs
        if (lastMicTapUptimeMs > 0L && elapsedMs in 1..doubleTapTimeoutMs) {
            lastMicTapUptimeMs = 0L
            return AirVisionHudKeyCommand.ToggleMic
        }

        lastMicTapUptimeMs = eventTimeMs
        return AirVisionHudKeyCommand.ArmMicDoubleTap
    }

    fun cancelMicHold() {
        micHoldKeyCode = null
    }

    private companion object {
        private val hudMicToggleKeys =
            setOf(
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_ASSIST,
                KeyEvent.KEYCODE_VOICE_ASSIST,
            )
        private val hudGlobalMicToggleKeys =
            setOf(
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK,
            )
        private val hudScrollKeyDeltas =
            mapOf(
                KeyEvent.KEYCODE_BRIGHTNESS_DOWN to -HUD_KEY_SCROLL_PIXELS,
                KeyEvent.KEYCODE_BRIGHTNESS_UP to HUD_KEY_SCROLL_PIXELS,
            )
        private val hudNavigationScrollKeyDeltas =
            mapOf(
                KeyEvent.KEYCODE_DPAD_UP to HUD_KEY_SCROLL_PIXELS,
                KeyEvent.KEYCODE_DPAD_DOWN to -HUD_KEY_SCROLL_PIXELS,
                KeyEvent.KEYCODE_PAGE_UP to HUD_KEY_PAGE_SCROLL_PIXELS,
                KeyEvent.KEYCODE_PAGE_DOWN to -HUD_KEY_PAGE_SCROLL_PIXELS,
            )
        private val hudNotificationBrowseKeyOffsets =
            mapOf(
                KeyEvent.KEYCODE_DPAD_LEFT to -1,
                KeyEvent.KEYCODE_BUTTON_L1 to -1,
                KeyEvent.KEYCODE_DPAD_RIGHT to 1,
                KeyEvent.KEYCODE_BUTTON_R1 to 1,
            )
        private val hudNotificationReplyKeys = setOf(KeyEvent.KEYCODE_BUTTON_X)
        private val hudAbortRunKeys = setOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE)
        private val hudApprovalActionKeys =
            setOf(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE)
        private val hudBrightnessKeyDeltas =
            mapOf(
                KeyEvent.KEYCODE_BRIGHTNESS_DOWN to -HUD_KEY_BRIGHTNESS_STEP_PERCENT,
                KeyEvent.KEYCODE_BRIGHTNESS_UP to HUD_KEY_BRIGHTNESS_STEP_PERCENT,
            )
        private val hudDistanceKeyDeltas =
            mapOf(
                KeyEvent.KEYCODE_BRIGHTNESS_DOWN to -HUD_KEY_DISTANCE_STEP_CM,
                KeyEvent.KEYCODE_BRIGHTNESS_UP to HUD_KEY_DISTANCE_STEP_CM,
            )
        private const val HUD_KEY_SCROLL_PIXELS = 160f
        private const val HUD_KEY_PAGE_SCROLL_PIXELS = 480f
        private const val HUD_KEY_BRIGHTNESS_STEP_PERCENT = 5
        private const val HUD_KEY_DISTANCE_STEP_CM = 5
    }
}

internal data class AirVisionHudMicHoldStart(
    val generation: Long,
    val shouldEnableMic: Boolean,
)

internal data class AirVisionHudMicHoldEnd(
    val shouldDisableMic: Boolean,
)

internal class AirVisionHudMicHoldController {
    private var active = false
    private var restoreMicEnabled = false
    private var generation = 0L

    fun begin(micEnabled: Boolean): AirVisionHudMicHoldStart? {
        if (active) return null
        active = true
        restoreMicEnabled = micEnabled
        generation += 1L
        return AirVisionHudMicHoldStart(
            generation = generation,
            shouldEnableMic = !micEnabled,
        )
    }

    fun end(): AirVisionHudMicHoldEnd? {
        if (!active) return null
        active = false
        generation += 1L
        return AirVisionHudMicHoldEnd(shouldDisableMic = !restoreMicEnabled)
            .also { restoreMicEnabled = false }
    }

    fun isEnableRequestCurrent(requestGeneration: Long): Boolean = active && generation == requestGeneration
}
