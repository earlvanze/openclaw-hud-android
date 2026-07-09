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
            AirVisionHudKeyAction.None,
            AirVisionHudKeyAction.fromRawValue(" none "),
        )
        assertEquals(
            AirVisionHudMediaKeyAction.None,
            AirVisionHudMediaKeyAction.fromRawValue(" NONE "),
        )
    }
}
