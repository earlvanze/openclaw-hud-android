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

enum class AirVisionHudHorizontalSwipeAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    BrowseNotifications("browse_notifications", "Browse notifications"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudHorizontalSwipeAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: BrowseNotifications
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
    val horizontalSwipeAction: AirVisionHudHorizontalSwipeAction =
        AirVisionHudHorizontalSwipeAction.BrowseNotifications,
    val brightnessKeyAction: AirVisionHudKeyAction = AirVisionHudKeyAction.ScrollChat,
    val mediaKeyAction: AirVisionHudMediaKeyAction = AirVisionHudMediaKeyAction.DoubleTapToggleMic,
)

internal sealed interface AirVisionHudTouchCommand {
    data object ToggleMic : AirVisionHudTouchCommand

    data class DismissNotification(
        val key: String,
    ) : AirVisionHudTouchCommand
}

internal fun airVisionHudSingleTapCommand(
    action: AirVisionHudTouchAction,
    notificationKey: String?,
    notificationClearable: Boolean,
): AirVisionHudTouchCommand? =
    when (action) {
        AirVisionHudTouchAction.None -> null
        AirVisionHudTouchAction.DismissNotification -> dismissNotificationCommand(notificationKey, notificationClearable)
        AirVisionHudTouchAction.ToggleMic -> AirVisionHudTouchCommand.ToggleMic
    }

internal fun airVisionHudDoubleTapCommand(
    action: AirVisionHudDoubleTapAction,
    notificationKey: String?,
    notificationClearable: Boolean,
): AirVisionHudTouchCommand? =
    when (action) {
        AirVisionHudDoubleTapAction.None -> null
        AirVisionHudDoubleTapAction.ToggleMic -> AirVisionHudTouchCommand.ToggleMic
        AirVisionHudDoubleTapAction.DismissNotification -> dismissNotificationCommand(notificationKey, notificationClearable)
    }

private fun dismissNotificationCommand(
    notificationKey: String?,
    notificationClearable: Boolean,
): AirVisionHudTouchCommand? =
    notificationKey
        ?.takeIf { notificationClearable }
        ?.let { AirVisionHudTouchCommand.DismissNotification(it) }
