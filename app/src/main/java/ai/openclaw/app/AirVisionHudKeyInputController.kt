package ai.openclaw.app

import android.view.KeyEvent

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

    data object ToggleMic : AirVisionHudKeyCommand

    data object ArmMicDoubleTap : AirVisionHudKeyCommand

    data object LogUnhandledHudAccessoryKey : AirVisionHudKeyCommand
}

internal data class AirVisionHudKeyDecision(
    val consume: Boolean,
    val command: AirVisionHudKeyCommand? = null,
)

internal class AirVisionHudKeyInputController(
    private val doubleTapTimeoutMs: Long = HUD_MIC_DOUBLE_TAP_TIMEOUT_MS,
) {
    private var lastMicTapUptimeMs = 0L

    fun handleKeyEvent(
        keyCode: Int,
        action: Int,
        eventTimeMs: Long,
        isHudAccessoryEvent: Boolean,
        controls: AirVisionHudControls,
    ): AirVisionHudKeyDecision {
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

        val isHudMediaKey = hudMicToggleKeys.contains(keyCode)
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

        if (controls.mediaKeyAction != AirVisionHudMediaKeyAction.DoubleTapToggleMic) {
            return AirVisionHudKeyDecision(consume = false)
        }

        if (action == KeyEvent.ACTION_DOWN) {
            return AirVisionHudKeyDecision(consume = true)
        }
        if (action != KeyEvent.ACTION_UP) {
            return AirVisionHudKeyDecision(consume = false)
        }

        return AirVisionHudKeyDecision(
            consume = true,
            command = handleMicTap(eventTimeMs),
        )
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
        private const val HUD_MIC_DOUBLE_TAP_TIMEOUT_MS = 500L
    }
}
