package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionHudFrameShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HudFrameSpecTest {
    @Test
    fun frameShapesMorphFromFullCanvasToPanoramicStrip() {
        val full = hudFrameSpec(AirVisionHudFrameShape.Full)
        val wide = hudFrameSpec(AirVisionHudFrameShape.Wide)
        val compact = hudFrameSpec(AirVisionHudFrameShape.Compact)
        val panoramic = hudFrameSpec(AirVisionHudFrameShape.Panoramic)

        assertEquals(1f, full.heightFraction, 0.001f)
        assertTrue(wide.heightFraction < full.heightFraction)
        assertTrue(compact.widthMultiplier < wide.widthMultiplier)
        assertTrue(panoramic.widthMultiplier > wide.widthMultiplier)
        assertTrue(panoramic.heightFraction < compact.heightFraction)
        assertTrue(panoramic.paddingScale < compact.paddingScale)
    }

    @Test
    fun everyFrameShapeProducesValidFractions() {
        AirVisionHudFrameShape.entries.forEach { shape ->
            val spec = hudFrameSpec(shape)
            assertTrue(spec.widthMultiplier > 0f)
            assertTrue(spec.heightFraction in 0.1f..1f)
            assertTrue(spec.paddingScale in 0f..1f)
        }
    }

    @Test
    fun unresolvedAdaptiveFrameUsesWideFallback() {
        assertEquals(
            hudFrameSpec(AirVisionHudFrameShape.Wide),
            hudFrameSpec(AirVisionHudFrameShape.Adaptive),
        )
    }
}
