package ai.openclaw.app

import android.view.InputDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudExternalTouchRouterTest {
    @Test
    fun forwardsExternalTouchscreenMappedToAnotherDisplay() {
        assertTrue(
            shouldForwardExternalHudTouch(
                deviceIsExternal = true,
                eventSource = InputDevice.SOURCE_TOUCHSCREEN,
                eventDisplayId = 0,
                hudDisplayId = 7,
            ),
        )
    }

    @Test
    fun forwardsExternalJoystickAndTouchpadMappings() {
        assertTrue(
            shouldForwardExternalHudTouch(
                deviceIsExternal = true,
                eventSource = InputDevice.SOURCE_JOYSTICK,
                eventDisplayId = 0,
                hudDisplayId = 7,
            ),
        )
        assertTrue(
            shouldForwardExternalHudTouch(
                deviceIsExternal = true,
                eventSource = InputDevice.SOURCE_TOUCHPAD,
                eventDisplayId = 0,
                hudDisplayId = 7,
            ),
        )
    }

    @Test
    fun forwardsExternalMouseStylusAndTrackballInput() {
        listOf(
            InputDevice.SOURCE_MOUSE,
            InputDevice.SOURCE_MOUSE_RELATIVE,
            InputDevice.SOURCE_STYLUS,
            InputDevice.SOURCE_TRACKBALL,
        ).forEach { source ->
            assertTrue(
                shouldForwardExternalHudTouch(
                    deviceIsExternal = true,
                    eventSource = source,
                    eventDisplayId = 0,
                    hudDisplayId = 7,
                ),
            )
        }
    }

    @Test
    fun leavesInternalAndAlreadyRoutedTouchAlone() {
        assertFalse(
            shouldForwardExternalHudTouch(
                deviceIsExternal = false,
                eventSource = InputDevice.SOURCE_TOUCHSCREEN,
                eventDisplayId = 0,
                hudDisplayId = 7,
            ),
        )
        assertFalse(
            shouldForwardExternalHudTouch(
                deviceIsExternal = true,
                eventSource = InputDevice.SOURCE_TOUCHSCREEN,
                eventDisplayId = 7,
                hudDisplayId = 7,
            ),
        )
    }
}
