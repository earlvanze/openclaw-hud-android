package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HudPresentationSessionControllerTest {
    @Test
    fun staleReleaseCannotClearReplacementSession() {
        val controller = HudPresentationSessionController<Any>()
        val first = Any()
        val replacement = Any()

        assertNull(controller.attach(first))
        assertSame(first, controller.attach(replacement))

        assertFalse(controller.release(first))
        assertSame(replacement, controller.current)
        assertTrue(controller.release(replacement))
        assertNull(controller.current)
    }

    @Test
    fun recoveryDelayIsBoundedAtConfiguredMaximum() {
        val controller = HudPresentationSessionController<Any>(listOf(5L, 10L, 20L))

        assertEquals(listOf(5L, 10L, 20L, 20L, 20L), List(5) { controller.nextRecoveryDelayMs() })
    }

    @Test
    fun shownSessionKeepsRecoveryBackoffUntilStable() {
        val controller = HudPresentationSessionController<Any>(listOf(5L, 10L, 20L))
        val session = Any()

        controller.attach(session)
        assertEquals(5L, controller.nextRecoveryDelayMs())
        assertEquals(10L, controller.nextRecoveryDelayMs())
        assertTrue(controller.markShown(session))

        assertEquals(20L, controller.nextRecoveryDelayMs())
        assertTrue(controller.markStable(session))
        assertEquals(5L, controller.nextRecoveryDelayMs())
    }

    @Test
    fun staleShownCallbackCannotResetReplacementRecovery() {
        val controller = HudPresentationSessionController<Any>(listOf(5L, 10L, 20L))
        val first = Any()
        val replacement = Any()

        controller.attach(first)
        controller.nextRecoveryDelayMs()
        controller.attach(replacement)

        assertFalse(controller.markStable(first))
        assertEquals(10L, controller.nextRecoveryDelayMs())
        assertSame(replacement, controller.current)
    }
}
