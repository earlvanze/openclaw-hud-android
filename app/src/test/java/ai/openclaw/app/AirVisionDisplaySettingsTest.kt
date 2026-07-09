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
                blueLightFilterPercent = 80,
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
    fun splendidOverlayAlpha_previewsNonStandardSplendidModes() {
        assertEquals(
            0f,
            AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                splendidMode = AirVisionSplendidMode.Standard,
                blueLightFilterPercent = 100,
            ),
            0.001f,
        )
        assertTrue(
            AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                splendidMode = AirVisionSplendidMode.Theater,
                blueLightFilterPercent = 0,
            ) > 0f,
        )
        assertTrue(
            AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                splendidMode = AirVisionSplendidMode.Office,
                blueLightFilterPercent = 0,
            ) > 0f,
        )
        assertTrue(
            AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                splendidMode = AirVisionSplendidMode.Game,
                blueLightFilterPercent = 0,
            ) > 0f,
        )
        assertTrue(
            AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                splendidMode = AirVisionSplendidMode.EyeCare,
                blueLightFilterPercent = 100,
            ) >
                AirVisionDisplaySettings.hudSplendidOverlayAlpha(
                    splendidMode = AirVisionSplendidMode.EyeCare,
                    blueLightFilterPercent = 0,
                ),
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

    @Test
    fun customProfileLabels_normalizeAndLabelCustomModesOnly() {
        val labels =
            AirVisionCustomProfileLabels(
                custom1 = AirVisionCustomProfileLabels.normalizeLabel("  Walking    HUD  ", AirVisionViewMode.Custom1.label),
                custom2 =
                    AirVisionCustomProfileLabels.normalizeLabel(
                        "This profile name is intentionally much too long",
                        AirVisionViewMode.Custom2.label,
                    ),
            )

        assertEquals("Walking HUD", labels.custom1)
        assertEquals(AirVisionCustomProfileLabels.MAX_LABEL_LENGTH, labels.custom2.length)
        assertEquals("Walking HUD", labels.labelFor(AirVisionViewMode.Custom1))
        assertEquals(labels.custom2, labels.labelFor(AirVisionViewMode.Custom2))
        assertEquals("Working", labels.labelFor(AirVisionViewMode.Working))
    }

    @Test
    fun ipdAdjustmentEnabled_followsLightLoadMode() {
        assertTrue(AirVisionDisplaySettings(lightLoadModeEnabled = false).ipdAdjustmentEnabled)
        assertEquals(false, AirVisionDisplaySettings(lightLoadModeEnabled = true).ipdAdjustmentEnabled)
    }

    @Test
    fun normalized_disablesThreeDModeWhenLightLoadModeIsEnabled() {
        val settings =
            AirVisionDisplaySettings(
                threeDModeEnabled = true,
                lightLoadModeEnabled = true,
            ).normalized

        assertEquals(false, settings.threeDModeEnabled)
        assertEquals(false, settings.threeDModeAvailable)
    }

    @Test
    fun blueLightFilterAvailability_followsEyeCareMode() {
        assertEquals(false, AirVisionDisplaySettings(splendidMode = AirVisionSplendidMode.Standard).blueLightFilterAvailable)
        assertTrue(AirVisionDisplaySettings(splendidMode = AirVisionSplendidMode.EyeCare).blueLightFilterAvailable)
    }
}
