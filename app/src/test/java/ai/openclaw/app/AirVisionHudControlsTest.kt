package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AirVisionHudControlsTest {
    @Test
    fun fromRawValue_defaultsToHudProfile() {
        assertEquals(
            AirVisionHudTouchAction.DismissNotification,
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
    fun singleTapCommandDefaultsToClearableNotificationDismiss() {
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
}
