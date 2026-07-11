package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionProfileBackupPreviewTest {
    @Test
    fun preview_summarizesImportBeforeApply() {
        val preview = AirVisionProfileBackups.preview(profileBackupJson())

        assertEquals("Apply Walk HUD AirVision profile backup?", preview.title)
        assertTrue(preview.details.any { it.contains("Version 4; 5 profiles") })
        assertTrue(preview.details.any { it.contains("Active: Walk HUD") })
        assertTrue(preview.details.any { it.contains("Brightness 44%") })
        assertTrue(preview.details.any { it.contains("distance 92 cm") })
        assertTrue(preview.details.any { it.contains("HUD scale 110%") })
        assertTrue(preview.details.any { it.contains("IPD 67 mm") })
        assertTrue(preview.details.any { it.contains("Single tap Dismiss notification") })
        assertTrue(preview.details.any { it.contains("double tap Toggle mic") })
        assertTrue(preview.details.any { it.contains("Startup HUD") })
        assertTrue(preview.details.any { it.contains("display target AirVision Preferred") })
        assertTrue(preview.details.any { it.contains("Speaker disabled") })
        assertTrue(preview.details.any { it.contains("Samsung/native captions enabled") })
        assertTrue(preview.details.any { it.contains("translation captions Auto -> Spanish") })
        assertTrue(preview.warnings.any { it.contains("Light Load is enabled") })
        assertTrue(preview.warnings.any { it.contains("Speaker is disabled") })
        assertTrue(preview.warnings.any { it.contains("Demo Mode is enabled") })
    }

    @Test
    fun resolve_rejectsMissingProfilesBeforeImport() {
        val error =
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                AirVisionProfileBackups.preview(
                    profileBackupJson(
                        profiles =
                            listOf(
                                profileJson("working"),
                                profileJson("gaming"),
                                profileJson("infinity"),
                                profileJson("custom1"),
                            ),
                    ),
                )
            }

        assertEquals("Profile backup is missing: Custom 2.", error.message)
    }
}

private fun profileBackupJson(
    activeViewMode: String = "custom1",
    profiles: List<String> =
        listOf(
            profileJson("working"),
            profileJson("gaming"),
            profileJson("infinity"),
            profileJson("custom1", brightnessPercent = 44, distanceCm = 92, lightLoadModeEnabled = true),
            profileJson("custom2"),
        ),
): String =
    """
    {
      "schema": "openclaw.airvision.m1.profile-backup",
      "version": 4,
      "activeViewMode": "$activeViewMode",
      "customLabels": { "custom1": "Walk HUD", "custom2": "Desk HUD" },
      "hudControls": {
        "singleTapAction": "dismiss_notification",
        "doubleTapAction": "toggle_mic",
        "swipeAction": "scroll_chat",
        "brightnessKeyAction": "adjust_distance",
        "mediaKeyAction": "double_tap_toggle_mic"
      },
      "appPreferences": {
        "language": "system",
        "startupDestination": "hud",
        "hudDisplayTarget": "airvision_preferred",
        "demoModeEnabled": true,
        "speakerEnabled": false,
        "nativeCaptionsEnabled": true,
        "translationCaptionSourceLanguage": "auto",
        "translationCaptionTargetLanguage": "es"
      },
      "runtimeProfiles": [],
      "profiles": [
        ${profiles.joinToString(",\n")}
      ]
    }
    """.trimIndent()

private fun profileJson(
    viewMode: String,
    brightnessPercent: Int = 80,
    distanceCm: Int = 75,
    lightLoadModeEnabled: Boolean = false,
): String =
    """
    {
      "viewMode": "$viewMode",
      "splendidMode": "eye_care",
      "hudPlacement": "upper_left",
      "brightnessPercent": $brightnessPercent,
      "blueLightFilterPercent": 30,
      "distanceCm": $distanceCm,
      "hudScalePercent": 110,
      "ipdMm": 67,
      "safeAreaPercent": 5,
      "physicalMainScreenVisible": true,
      "motionSyncEnabled": true,
      "threeDModeEnabled": false,
      "lightLoadModeEnabled": $lightLoadModeEnabled
    }
    """.trimIndent()
