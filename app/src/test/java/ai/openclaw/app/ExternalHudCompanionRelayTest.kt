package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalHudCompanionRelayTest {
    @Test
    fun parser_mapsClicksAndSlidesToExistingHudGestureSurface() {
        assertEquals(
            ExternalHudCompanionAction.SingleTap,
            parseExternalHudCompanionInput("one_click", "press", 4, 0)?.action,
        )
        assertEquals(
            ExternalHudCompanionAction.DoubleTap,
            parseExternalHudCompanionInput("double_click", "press", 5, 0)?.action,
        )
        assertEquals(
            ExternalHudCompanionAction.SwipeForward,
            parseExternalHudCompanionInput("slide_forward", "press", 7, 11)?.action,
        )
        assertEquals(
            ExternalHudCompanionAction.SwipeBackward,
            parseExternalHudCompanionInput("long_press_slide_backward", "update", 3, 2)?.action,
        )
    }

    @Test
    fun parser_retainsUnmappedVendorGesturesForMonitoring() {
        val input = parseExternalHudCompanionInput("long_click", "press", 3, 0)

        assertEquals(ExternalHudCompanionAction.ObserveOnly, input?.action)
        assertEquals("long_click", input?.event)
    }

    @Test
    fun parser_rejectsReleaseUnknownAndOutOfRangePayloads() {
        assertNull(parseExternalHudCompanionInput("double_click", "release", 254, 0))
        assertNull(parseExternalHudCompanionInput("unknown", "press", 5, 0))
        assertNull(parseExternalHudCompanionInput("double_click", "press", -1, 0))
        assertNull(parseExternalHudCompanionInput("double_click", "press", 5, 256))
    }

    @Test
    fun parser_acceptsVendorRawSixSlideResolvedOnRelease() {
        assertEquals(
            ExternalHudCompanionAction.SwipeForward,
            parseExternalHudCompanionInput("slide_forward", "release", 6, 14)?.action,
        )
        assertNull(parseExternalHudCompanionInput("slide_forward", "release", 7, 14))
    }
}
