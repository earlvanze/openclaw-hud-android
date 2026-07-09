package ai.openclaw.app

enum class AirVisionHudTouchAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    DismissNotification("dismiss_notification", "Dismiss notification"),
    ToggleMic("toggle_mic", "Toggle mic"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudTouchAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: DismissNotification
    }
}

enum class AirVisionHudDoubleTapAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    ToggleMic("toggle_mic", "Toggle mic"),
    DismissNotification("dismiss_notification", "Dismiss notification"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudDoubleTapAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: ToggleMic
    }
}

enum class AirVisionHudSwipeAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    ScrollChat("scroll_chat", "Scroll chat"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudSwipeAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: ScrollChat
    }
}

enum class AirVisionHudKeyAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    ScrollChat("scroll_chat", "Scroll chat"),
    AdjustBrightness("adjust_brightness", "Adjust brightness"),
    AdjustDistance("adjust_distance", "Adjust distance"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudKeyAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: ScrollChat
    }
}

enum class AirVisionHudMediaKeyAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    DoubleTapToggleMic("double_tap_toggle_mic", "Double-tap mic"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudMediaKeyAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: DoubleTapToggleMic
    }
}

data class AirVisionHudControls(
    val singleTapAction: AirVisionHudTouchAction = AirVisionHudTouchAction.DismissNotification,
    val doubleTapAction: AirVisionHudDoubleTapAction = AirVisionHudDoubleTapAction.ToggleMic,
    val swipeAction: AirVisionHudSwipeAction = AirVisionHudSwipeAction.ScrollChat,
    val brightnessKeyAction: AirVisionHudKeyAction = AirVisionHudKeyAction.ScrollChat,
    val mediaKeyAction: AirVisionHudMediaKeyAction = AirVisionHudMediaKeyAction.DoubleTapToggleMic,
)
