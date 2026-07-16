package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AirVisionHudControlsTest {
    @Test
    fun fromRawValue_defaultsToHudProfile() {
        assertEquals(
            AirVisionHudTouchAction.OpenNotification,
            AirVisionHudTouchAction.fromRawValue(null),
        )
        assertEquals(
            AirVisionHudDoubleTapAction.ToggleMic,
            AirVisionHudDoubleTapAction.fromRawValue(null),
        )
        assertEquals(
            AirVisionHudSwipeAction.ScrollChat,
            AirVisionHudSwipeAction.fromRawValue(null),
        )
        assertEquals(
            AirVisionHudHorizontalSwipeAction.BrowseNotifications,
            AirVisionHudHorizontalSwipeAction.fromRawValue(null),
        )
        assertEquals(
            AirVisionHudKeyAction.ScrollChat,
            AirVisionHudKeyAction.fromRawValue(null),
        )
        assertEquals(
            AirVisionHudMediaKeyAction.DoubleTapToggleMic,
            AirVisionHudMediaKeyAction.fromRawValue(null),
        )
    }

    @Test
    fun fromRawValue_trimsAndNormalizes() {
        assertEquals(
            AirVisionHudTouchAction.ToggleMic,
            AirVisionHudTouchAction.fromRawValue(" TOGGLE_MIC "),
        )
        assertEquals(
            AirVisionHudDoubleTapAction.DismissNotification,
            AirVisionHudDoubleTapAction.fromRawValue(" dismiss_notification "),
        )
        assertEquals(
            AirVisionHudSwipeAction.None,
            AirVisionHudSwipeAction.fromRawValue(" NONE "),
        )
        assertEquals(
            AirVisionHudHorizontalSwipeAction.None,
            AirVisionHudHorizontalSwipeAction.fromRawValue(" NONE "),
        )
        assertEquals(
            AirVisionHudKeyAction.None,
            AirVisionHudKeyAction.fromRawValue(" none "),
        )
        assertEquals(
            AirVisionHudMediaKeyAction.None,
            AirVisionHudMediaKeyAction.fromRawValue(" NONE "),
        )
    }

    @Test
    fun singleTapCommandCanOpenOrDismissNotification() {
        assertEquals(
            AirVisionHudTouchCommand.OpenNotification("maps:1"),
            airVisionHudSingleTapCommand(
                action = AirVisionHudTouchAction.OpenNotification,
                notificationKey = "maps:1",
                notificationClearable = false,
            ),
        )
        assertEquals(
            null,
            airVisionHudSingleTapCommand(
                action = AirVisionHudTouchAction.OpenNotification,
                notificationKey = null,
                notificationClearable = false,
            ),
        )
        assertEquals(
            AirVisionHudTouchCommand.DismissNotification("maps:1"),
            airVisionHudSingleTapCommand(
                action = AirVisionHudTouchAction.DismissNotification,
                notificationKey = "maps:1",
                notificationClearable = true,
            ),
        )
        assertEquals(
            null,
            airVisionHudSingleTapCommand(
                action = AirVisionHudTouchAction.DismissNotification,
                notificationKey = "maps:1",
                notificationClearable = false,
            ),
        )
        assertEquals(
            AirVisionHudTouchCommand.ToggleMic,
            airVisionHudSingleTapCommand(
                action = AirVisionHudTouchAction.ToggleMic,
                notificationKey = "maps:1",
                notificationClearable = true,
            ),
        )
    }

    @Test
    fun doubleTapCommandDefaultsToMicAndCanClearNotifications() {
        assertEquals(
            AirVisionHudTouchCommand.ToggleMic,
            airVisionHudDoubleTapCommand(
                action = AirVisionHudDoubleTapAction.ToggleMic,
                notificationKey = "maps:1",
                notificationClearable = true,
            ),
        )
        assertEquals(
            AirVisionHudTouchCommand.OpenNotification("maps:1"),
            airVisionHudDoubleTapCommand(
                action = AirVisionHudDoubleTapAction.OpenNotification,
                notificationKey = "maps:1",
                notificationClearable = false,
            ),
        )
        assertEquals(
            null,
            airVisionHudDoubleTapCommand(
                action = AirVisionHudDoubleTapAction.OpenNotification,
                notificationKey = " ",
                notificationClearable = false,
            ),
        )
        assertEquals(
            AirVisionHudTouchCommand.DismissNotification("maps:1"),
            airVisionHudDoubleTapCommand(
                action = AirVisionHudDoubleTapAction.DismissNotification,
                notificationKey = "maps:1",
                notificationClearable = true,
            ),
        )
        assertEquals(
            null,
            airVisionHudDoubleTapCommand(
                action = AirVisionHudDoubleTapAction.DismissNotification,
                notificationKey = "maps:1",
                notificationClearable = false,
            ),
        )
    }

    @Test
    fun notificationOpenResultMessageExplainsActionState() {
        assertEquals("Opened on phone", hudNotificationOpenResultMessage(ok = true, code = null))
        assertEquals(
            "Enable notification access",
            hudNotificationOpenResultMessage(ok = false, code = "NOTIFICATIONS_DISABLED"),
        )
        assertEquals(
            "Notification no longer available",
            hudNotificationOpenResultMessage(ok = false, code = "NOTIFICATION_NOT_FOUND"),
        )
        assertEquals(
            "Notification cannot be opened",
            hudNotificationOpenResultMessage(ok = false, code = "ACTION_UNAVAILABLE"),
        )
        assertEquals("Could not open notification", hudNotificationOpenResultMessage(ok = false, code = "ACTION_FAILED"))
    }

    @Test
    fun notificationReplyResultMessageExplainsActionState() {
        assertEquals("Reply sent", hudNotificationReplyResultMessage(ok = true, code = null))
        assertEquals(
            "Enable notification access",
            hudNotificationReplyResultMessage(ok = false, code = "NOTIFICATIONS_DISABLED"),
        )
        assertEquals("Reply unavailable", hudNotificationReplyResultMessage(ok = false, code = "ACTION_UNAVAILABLE"))
        assertEquals("Enter a reply", hudNotificationReplyResultMessage(ok = false, code = "INVALID_REQUEST"))
        assertEquals("Could not send reply", hudNotificationReplyResultMessage(ok = false, code = "ACTION_FAILED"))
    }
}
