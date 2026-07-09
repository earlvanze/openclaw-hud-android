package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionDisplaySettingsTest {
    @Test
    fun normalized_clampsUnsafeValues() {
        val settings =
            AirVisionDisplaySettings(
                brightnessPercent = 2,
                blueLightFilterPercent = 140,
                distanceCm = 12,
                ipdMm = 120,
                safeAreaPercent = 99,
            ).normalized

        assertEquals(AirVisionDisplaySettings.MIN_BRIGHTNESS_PERCENT, settings.brightnessPercent)
        assertEquals(AirVisionDisplaySettings.MAX_BLUE_LIGHT_FILTER_PERCENT, settings.blueLightFilterPercent)
        assertEquals(AirVisionDisplaySettings.MIN_DISTANCE_CM, settings.distanceCm)
        assertEquals(AirVisionDisplaySettings.MAX_IPD_MM, settings.ipdMm)
        assertEquals(AirVisionDisplaySettings.MAX_SAFE_AREA_PERCENT, settings.safeAreaPercent)
    }

    @Test
    fun hudScaleForDistanceCm_getsSmallerAsDistanceIncreases() {
        val close = AirVisionDisplaySettings.hudScaleForDistanceCm(50)
        val far = AirVisionDisplaySettings.hudScaleForDistanceCm(120)

        assertTrue(close > far)
        assertEquals(1.0f, AirVisionDisplaySettings.hudScaleForDistanceCm(AirVisionDisplaySettings.DEFAULT_DISTANCE_CM), 0.001f)
    }

    @Test
    fun hudOverlays_convertBrightnessAndEyeCareToAlpha() {
        assertEquals(0f, AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(100), 0.001f)
        assertTrue(AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(25) > 0.4f)
        assertEquals(
            0f,
            AirVisionDisplaySettings.hudWarmOverlayAlpha(
                splendidMode = AirVisionSplendidMode.Standard,
                blueLightFilterPercent = 0,
            ),
            0.001f,
        )
        assertTrue(
            AirVisionDisplaySettings.hudWarmOverlayAlpha(
                splendidMode = AirVisionSplendidMode.EyeCare,
                blueLightFilterPercent = 60,
            ) > 0.2f,
        )
    }

    @Test
    fun defaultsForViewMode_createDistinctProfileSlots() {
        val working = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working)
        val gaming = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Gaming)
        val infinity = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Infinity)

        assertEquals(AirVisionViewMode.Working, working.viewMode)
        assertEquals(AirVisionSplendidMode.Standard, working.splendidMode)
        assertEquals(AirVisionHudPlacement.UpperLeft, working.hudPlacement)
        assertEquals(AirVisionViewMode.Gaming, gaming.viewMode)
        assertEquals(AirVisionSplendidMode.Game, gaming.splendidMode)
        assertEquals(AirVisionHudPlacement.Center, gaming.hudPlacement)
        assertEquals(100, gaming.brightnessPercent)
        assertEquals(AirVisionViewMode.Infinity, infinity.viewMode)
        assertEquals(AirVisionHudPlacement.UpperCenter, infinity.hudPlacement)
        assertTrue(infinity.distanceCm > working.distanceCm)
        assertTrue(infinity.lightLoadModeEnabled)
    }
}
