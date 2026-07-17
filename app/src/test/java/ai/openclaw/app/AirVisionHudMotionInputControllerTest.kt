package ai.openclaw.app

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionHudMotionInputControllerTest {
    @Test
    fun convertsM1AbsoluteAxisMovementToScroll() {
        val controller = AirVisionHudMotionInputController()

        assertNull(controller.absoluteAxisScrollDelta(deviceId = 19, value = 0.25f, eventTimeMs = 1_000L))
        assertEquals(
            -180f,
            requireNotNull(
                controller.absoluteAxisScrollDelta(deviceId = 19, value = 0.50f, eventTimeMs = 1_040L),
            ),
            0.001f,
        )
    }

    @Test
    fun resetsAbsoluteAxisAfterGestureGapAndIgnoresNoise() {
        val controller = AirVisionHudMotionInputController(absoluteAxisTimeoutMs = 100L)

        assertNull(controller.absoluteAxisScrollDelta(deviceId = 19, value = 0.25f, eventTimeMs = 1_000L))
        assertNull(controller.absoluteAxisScrollDelta(deviceId = 19, value = 0.75f, eventTimeMs = 1_200L))
        assertNull(controller.absoluteAxisScrollDelta(deviceId = 19, value = 0.7505f, eventTimeMs = 1_220L))
    }

    @Test
    fun convertsRelativeWheelMovementToScrollStep() {
        val controller = AirVisionHudMotionInputController()

        assertEquals(320f, requireNotNull(controller.relativeAxisScrollDelta(2f)), 0.001f)
        assertNull(controller.relativeAxisScrollDelta(0f))
    }

    @Test
    fun tracksAbsoluteAxesIndependentlyAndNormalizesTheirRanges() {
        val controller = AirVisionHudMotionInputController()

        assertNull(
            controller.absoluteAxisScrollDelta(
                deviceId = 22,
                axisId = 1,
                value = -1f,
                rangeSpan = 2f,
                eventTimeMs = 1_000L,
            ),
        )
        assertNull(
            controller.absoluteAxisScrollDelta(
                deviceId = 22,
                axisId = 15,
                value = 0f,
                rangeSpan = 1f,
                eventTimeMs = 1_000L,
            ),
        )
        assertEquals(
            -360f,
            requireNotNull(
                controller.absoluteAxisScrollDelta(
                    deviceId = 22,
                    axisId = 1,
                    value = 0f,
                    rangeSpan = 2f,
                    eventTimeMs = 1_040L,
                ),
            ),
            0.001f,
        )
        assertEquals(
            360f,
            requireNotNull(
                controller.absoluteAxisScrollDelta(
                    deviceId = 22,
                    axisId = 15,
                    value = -0.5f,
                    rangeSpan = 1f,
                    eventTimeMs = 1_040L,
                ),
            ),
            0.001f,
        )
    }

    @Test
    fun recognizesAccessoryTapKeys() {
        assertTrue(isHudAccessoryTapKey(KeyEvent.KEYCODE_ENTER))
        assertTrue(isHudAccessoryTapKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertTrue(shouldDispatchHudAccessoryTapToPresentation(KeyEvent.KEYCODE_ENTER))
        assertFalse(shouldDispatchHudAccessoryTapToPresentation(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }
}
