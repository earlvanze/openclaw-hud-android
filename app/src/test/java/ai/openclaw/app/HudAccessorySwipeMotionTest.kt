package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudAccessorySwipeMotionTest {
    @Test
    fun forwardSwipeMovesUpAndBackwardSwipeMovesDown() {
        val forward = requireNotNull(hudAccessorySwipeMotion(width = 1000, height = 500, forward = true))
        val backward = requireNotNull(hudAccessorySwipeMotion(width = 1000, height = 500, forward = false))

        assertEquals(500f, forward.x)
        assertTrue(forward.startY > forward.endY)
        assertTrue(backward.startY < backward.endY)
    }

    @Test
    fun invalidSurfaceHasNoMotion() {
        assertNull(hudAccessorySwipeMotion(width = 0, height = 500, forward = true))
        assertNull(hudAccessorySwipeMotion(width = 1000, height = 0, forward = true))
    }
}
