package ai.openclaw.app

enum class AirVisionHudTouchAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    OpenNotification("open_notification", "Open notification"),
    DismissNotification("dismiss_notification", "Dismiss notification"),
    ToggleMic("toggle_mic", "Toggle mic"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudTouchAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: OpenNotification
    }
}

enum class AirVisionHudDoubleTapAction(
    val rawValue: String,
    val label: String,
) {
    None("none", "None"),
    ToggleMic("toggle_mic", "Toggle mic"),
    OpenNotification("open_notification", "Open notification"),
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
    SingleTapToggleMic("single_tap_toggle_mic", "Single-tap mic"),
    DoubleTapToggleMic("double_tap_toggle_mic", "Double-tap mic"),
    HoldToTalk("hold_to_talk", "Hold to talk"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudMediaKeyAction =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: DoubleTapToggleMic
    }
}

enum class ExternalHudDoubleTapWindow(
    val rawValue: String,
    val timeoutMs: Long,
    val label: String,
    val durationLabel: String,
) {
    Quick("quick", 500L, "Quick (0.5 s)", "0.5 seconds"),
    Standard("standard", 1_000L, "Standard (1 s)", "1 second"),
    Relaxed("relaxed", 1_500L, "Relaxed (1.5 s)", "1.5 seconds"),
    Extended("extended", 2_000L, "Extended (2 s)", "2 seconds"),
    Accessibility("accessibility", 2_500L, "Accessibility (2.5 s)", "2.5 seconds"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): ExternalHudDoubleTapWindow =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: Extended
    }
}

data class AirVisionHudControls(
    val singleTapAction: AirVisionHudTouchAction = AirVisionHudTouchAction.OpenNotification,
    val doubleTapAction: AirVisionHudDoubleTapAction = AirVisionHudDoubleTapAction.ToggleMic,
    val swipeAction: AirVisionHudSwipeAction = AirVisionHudSwipeAction.ScrollChat,
    val horizontalSwipeAction: AirVisionHudHorizontalSwipeAction =
        AirVisionHudHorizontalSwipeAction.BrowseNotifications,
    val brightnessKeyAction: AirVisionHudKeyAction = AirVisionHudKeyAction.ScrollChat,
    val mediaKeyAction: AirVisionHudMediaKeyAction = AirVisionHudMediaKeyAction.DoubleTapToggleMic,
    val mediaDoubleTapWindow: ExternalHudDoubleTapWindow = ExternalHudDoubleTapWindow.Extended,
    val customMediaKeyCode: Int? = null,
)

internal sealed interface AirVisionHudTouchCommand {
    data object ToggleMic : AirVisionHudTouchCommand

    data class OpenNotification(
        val key: String,
    ) : AirVisionHudTouchCommand

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
        AirVisionHudTouchAction.OpenNotification -> openNotificationCommand(notificationKey)
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
        AirVisionHudDoubleTapAction.OpenNotification -> openNotificationCommand(notificationKey)
        AirVisionHudDoubleTapAction.DismissNotification -> dismissNotificationCommand(notificationKey, notificationClearable)
    }

private fun openNotificationCommand(notificationKey: String?): AirVisionHudTouchCommand? =
    notificationKey
        ?.takeIf { it.isNotBlank() }
        ?.let { AirVisionHudTouchCommand.OpenNotification(it) }

internal fun hudNotificationOpenResultMessage(
    ok: Boolean,
    code: String?,
): String =
    when {
        ok -> "Opened on phone"
        code == "NOTIFICATIONS_DISABLED" -> "Enable notification access"
        code == "NOTIFICATIONS_UNAVAILABLE" -> "Notification access reconnecting"
        code == "NOTIFICATION_NOT_FOUND" -> "Notification no longer available"
        code == "ACTION_UNAVAILABLE" -> "Notification cannot be opened"
        else -> "Could not open notification"
    }

internal fun hudNotificationReplyResultMessage(
    ok: Boolean,
    code: String?,
): String =
    when {
        ok -> "Reply sent"
        code == "NOTIFICATIONS_DISABLED" -> "Enable notification access"
        code == "NOTIFICATIONS_UNAVAILABLE" -> "Notification access reconnecting"
        code == "NOTIFICATION_NOT_FOUND" -> "Notification no longer available"
        code == "ACTION_UNAVAILABLE" -> "Reply unavailable"
        code == "INVALID_REQUEST" -> "Enter a reply"
        else -> "Could not send reply"
    }

internal fun hudChatAbortRequestMessage(pendingRunCount: Int): String =
    when {
        pendingRunCount <= 0 -> "No active OpenClaw run"
        pendingRunCount == 1 -> "Stopping OpenClaw"
        else -> "Stopping $pendingRunCount OpenClaw runs"
    }

private fun dismissNotificationCommand(
    notificationKey: String?,
    notificationClearable: Boolean,
): AirVisionHudTouchCommand? =
    notificationKey
        ?.takeIf { notificationClearable }
        ?.let { AirVisionHudTouchCommand.DismissNotification(it) }
