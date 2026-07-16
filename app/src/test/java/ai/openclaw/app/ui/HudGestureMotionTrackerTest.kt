package ai.openclaw.app.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HudGestureMotionTrackerTest {
    @Test
    fun shortUnconsumedMovementIsTap() {
        val tracker = HudGestureMotionTracker(touchSlop = 10f)

        assertNull(tracker.add(Offset(2f, 3f), consumed = false))
        assertEquals(HudGestureMotionResult.Tap, tracker.finish(consumed = false))
    }

    @Test
    fun verticalMovementBecomesSwipeAndIncludesInitialDistance() {
        val tracker = HudGestureMotionTracker(touchSlop = 10f)

        assertNull(tracker.add(Offset(1f, -4f), consumed = false))
        assertEquals(12f, tracker.add(Offset(1f, -8f), consumed = false))
        assertEquals(5f, tracker.add(Offset.Zero.copy(y = -5f), consumed = false))
        assertEquals(HudGestureMotionResult.VerticalSwipe, tracker.finish(consumed = false))
    }

    @Test
    fun horizontalMovementReportsPhysicalDirection() {
        val left = HudGestureMotionTracker(touchSlop = 10f)
        assertNull(left.add(Offset(-12f, 3f), consumed = false))
        assertEquals(HudGestureMotionResult.SwipeLeft, left.finish(consumed = false))

        val right = HudGestureMotionTracker(touchSlop = 10f)
        assertNull(right.add(Offset(12f, -3f), consumed = false))
        assertEquals(HudGestureMotionResult.SwipeRight, right.finish(consumed = false))
    }

    @Test
    fun consumedMovementIsIgnored() {
        val consumed = HudGestureMotionTracker(touchSlop = 10f)
        assertNull(consumed.add(Offset(0f, 12f), consumed = true))
        assertEquals(HudGestureMotionResult.Ignore, consumed.finish(consumed = false))
    }
}
