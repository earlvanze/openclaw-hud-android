package ai.openclaw.app

import android.view.InputDevice
import android.view.KeyEvent

enum class ExternalHudInputKind(
    val label: String,
) {
    Touch("Touch"),
    Gesture("Gesture"),
    Key("Key"),
    Media("Media"),
    Motion("Motion"),
    Relay("Companion relay"),
}

data class ExternalHudInputEvent(
    val sequence: Long,
    val kind: ExternalHudInputKind,
    val input: String,
    val source: String,
    val deviceName: String?,
    val mappedAction: String,
    val handled: Boolean,
) {
    val summary: String
        get() =
            buildString {
                append('#')
                append(sequence)
                append(' ')
                append(input)
                append(" from ")
                append(source)
                deviceName?.let {
                    append(" (")
                    append(it)
                    append(')')
                }
                append(" -> ")
                append(mappedAction)
                append(if (handled) " [handled]" else " [passed through]")
            }

    val hudMessage: String
        get() = "Input: $input -> $mappedAction"
}

internal fun appendExternalHudInputEvent(
    current: List<ExternalHudInputEvent>,
    event: ExternalHudInputEvent,
    limit: Int = MAX_EXTERNAL_HUD_INPUT_EVENTS,
): List<ExternalHudInputEvent> {
    require(limit > 0) { "External HUD input history limit must be positive." }
    return (listOf(event) + current).take(limit)
}

internal fun sanitizeExternalHudInputText(
    value: String?,
    maxLength: Int = MAX_EXTERNAL_HUD_INPUT_TEXT_LENGTH,
): String? {
    require(maxLength > 0) { "External HUD input text limit must be positive." }
    return value
        ?.replace(Regex("[\\r\\n\\t]+"), " ")
        ?.trim()
        ?.replace(Regex(" +"), " ")
        ?.takeIf { it.isNotEmpty() }
        ?.take(maxLength)
}

internal fun externalHudInputSourceLabel(source: Int): String =
    when {
        source and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN -> "Touchscreen"
        source and InputDevice.SOURCE_TOUCHPAD == InputDevice.SOURCE_TOUCHPAD -> "Touchpad"
        source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE -> "Relative mouse"
        source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE -> "Mouse"
        source and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS -> "Stylus"
        source and InputDevice.SOURCE_TRACKBALL == InputDevice.SOURCE_TRACKBALL -> "Trackball"
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD -> "Gamepad"
        source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK -> "Joystick"
        source and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD -> "D-pad"
        source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD -> "Keyboard"
        else -> "Source 0x${source.toString(16)}"
    }

internal fun externalHudKeyInputLabel(
    keyCode: Int,
    action: Int,
): String {
    val actionLabel =
        when (action) {
            KeyEvent.ACTION_DOWN -> "down"
            KeyEvent.ACTION_UP -> "up"
            else -> "action $action"
        }
    return "${externalHudKeyLabel(keyCode)} $actionLabel"
}

internal fun externalHudKeyCommandLabel(
    command: AirVisionHudKeyCommand?,
    consumed: Boolean,
): String =
    when (command) {
        is AirVisionHudKeyCommand.ScrollChat -> "Scroll chat"
        is AirVisionHudKeyCommand.AdjustBrightness -> "Adjust HUD brightness"
        is AirVisionHudKeyCommand.AdjustDistance -> "Adjust virtual distance"
        is AirVisionHudKeyCommand.BrowseNotifications -> "Browse notifications"
        AirVisionHudKeyCommand.StartNotificationReply -> "Reply to notification"
        AirVisionHudKeyCommand.AbortActiveRun -> "Stop active run"
        AirVisionHudKeyCommand.AllowPendingExecOnce -> "Allow execution once"
        AirVisionHudKeyCommand.DenyPendingExecApproval -> "Deny execution"
        AirVisionHudKeyCommand.ToggleMic -> "Toggle mic"
        AirVisionHudKeyCommand.ArmMicDoubleTap -> "Await second mic tap"
        AirVisionHudKeyCommand.BeginMicHold -> "Begin hold-to-talk"
        AirVisionHudKeyCommand.EndMicHold -> "End hold-to-talk"
        AirVisionHudKeyCommand.LogUnhandledHudAccessoryKey -> "Unmapped accessory key"
        null -> if (consumed) "Consumed" else "Android passthrough"
    }

internal const val MAX_EXTERNAL_HUD_INPUT_EVENTS = 8
private const val MAX_EXTERNAL_HUD_INPUT_TEXT_LENGTH = 64
