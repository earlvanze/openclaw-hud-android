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
        assertTrue(preview.details.any { it.contains("Runtime effective HUD scale") })
        assertTrue(preview.details.any { it.contains("transcript 3, captions 2") })
        assertTrue(preview.details.any { it.contains("Runtime overlays disabled") })
        assertTrue(preview.details.any { it.contains("brightness dimming enabled") })
        assertTrue(preview.details.any { it.contains("Runtime Working: effective HUD scale 110%") })
        assertTrue(preview.details.any { it.contains("Runtime Walk HUD: effective HUD scale 84%") })
        assertTrue(preview.details.any { it.contains("Runtime Walk HUD") && it.contains("transcript 3, captions 2") })
        assertTrue(preview.details.any { it.contains("Runtime Desk HUD: effective HUD scale 125%") })
        assertTrue(preview.details.any { it.contains("Startup HUD") })
        assertTrue(preview.details.any { it.contains("display target AirVision Preferred") })
        assertTrue(preview.details.any { it.contains("Speaker disabled") })
        assertTrue(preview.details.any { it.contains("Samsung/native captions enabled") })
        assertTrue(preview.details.any { it.contains("translation captions Auto -> Spanish") })
        assertTrue(preview.warnings.any { it.contains("Light Load is enabled") })
        assertTrue(preview.warnings.any { it.contains("Runtime metadata is missing") })
        assertTrue(preview.warnings.any { it.contains("Speaker is disabled") })
        assertTrue(preview.warnings.any { it.contains("Demo Mode is enabled") })
    }

    @Test
    fun preview_warnsWhenRuntimeMetadataIsStale() {
        val preview =
            AirVisionProfileBackups.preview(
                profileBackupJson(
                    runtimeProfiles =
                        listOf(
                            runtimeProfileJson(
                                viewMode = "custom1",
                                effectiveHudScalePercent = 999,
                                hudTranscriptEntryCount = 99,
                                hudCaptionEntryCount = 99,
                            ),
                        ),
                ),
            )

        assertTrue(preview.details.any { it.contains("Runtime effective HUD scale") })
        assertTrue(preview.details.any { it.contains("transcript 3, captions 2") })
        assertTrue(preview.warnings.any { it.contains("Runtime metadata is missing for Working") })
        assertTrue(preview.warnings.any { it.contains("Runtime metadata is stale for Walk HUD") })
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

    @Test
    fun resolve_restoresRememberedDisplayFromVersionFiveBackup() {
        val raw =
            profileBackupJson()
                .replace("\"version\": 4", "\"version\": 5")
                .replace(
                    "\"hudDisplayTarget\": \"airvision_preferred\",",
                    """"hudDisplayTarget": "remembered_external",
        "rememberedHudDisplay": { "name": "USB-C projector", "widthPx": 1920, "heightPx": 1200 },""",
                )

        val preferences = AirVisionProfileBackups.resolve(raw).appPreferences

        assertEquals(AirVisionHudDisplayTarget.RememberedExternal, preferences.hudDisplayTarget)
        assertEquals(
            AirVisionHudDisplayFingerprint("USB-C projector", 1920, 1200),
            preferences.rememberedHudDisplay,
        )
    }

    @Test
    fun resolve_rejectsRememberedTargetWithoutFingerprint() {
        val raw =
            profileBackupJson()
                .replace("\"version\": 4", "\"version\": 5")
                .replace("\"airvision_preferred\"", "\"remembered_external\"")

        val error =
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                AirVisionProfileBackups.resolve(raw)
            }

        assertEquals("Remembered HUD display target requires a display fingerprint.", error.message)
    }

    @Test
    fun resolve_rejectsInvalidCustomExternalHudKeyCode() {
        val raw =
            profileBackupJson()
                .replace(
                    "\"mediaKeyAction\": \"double_tap_toggle_mic\"",
                    "\"mediaKeyAction\": \"double_tap_toggle_mic\", \"customMediaKeyCode\": 99999",
                )

        val error =
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                AirVisionProfileBackups.resolve(raw)
            }

        assertEquals("Unsupported external HUD custom media key code: 99999", error.message)
    }
}

private fun profileBackupJson(
    activeViewMode: String = "custom1",
    runtimeProfiles: List<String> = emptyList(),
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
      "runtimeProfiles": [
        ${runtimeProfiles.joinToString(",\n")}
      ],
      "profiles": [
        ${profiles.joinToString(",\n")}
      ]
    }
    """.trimIndent()

private fun runtimeProfileJson(
    viewMode: String,
    effectiveHudScalePercent: Int,
    hudTranscriptEntryCount: Int,
    hudCaptionEntryCount: Int,
): String =
    """
    {
      "viewMode": "$viewMode",
      "ipdAdjustmentEnabled": false,
      "threeDModeAvailable": false,
      "blueLightFilterAvailable": true,
      "hudTranscriptEntryCount": $hudTranscriptEntryCount,
      "hudCaptionEntryCount": $hudCaptionEntryCount,
      "effectiveHudScalePercent": $effectiveHudScalePercent,
      "colorPreviewOverlaysEnabled": false,
      "brightnessDimmingEnabled": true
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
