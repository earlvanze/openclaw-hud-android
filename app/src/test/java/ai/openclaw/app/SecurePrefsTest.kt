package ai.openclaw.app

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SecurePrefsTest {
    @Test
    fun loadLocationMode_migratesLegacyAlwaysValue() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs
            .edit()
            .clear()
            .putString("location.enabledMode", "always")
            .commit()

        val prefs = SecurePrefs(context)

        assertEquals(LocationMode.WhileUsing, prefs.locationMode.value)
        assertEquals("whileUsing", plainPrefs.getString("location.enabledMode", null))
    }

    @Test
    fun saveGatewayBootstrapToken_persistsSeparatelyFromSharedToken() {
        val context = RuntimeEnvironment.getApplication()
        val securePrefs = context.getSharedPreferences("openclaw.node.secure.test", Context.MODE_PRIVATE)
        securePrefs.edit().clear().commit()
        val prefs = SecurePrefs(context, securePrefsOverride = securePrefs)

        prefs.setGatewayToken("shared-token")
        prefs.setGatewayBootstrapToken("bootstrap-token")

        assertEquals("shared-token", prefs.loadGatewayToken())
        assertEquals("bootstrap-token", prefs.loadGatewayBootstrapToken())
        assertEquals("bootstrap-token", prefs.gatewayBootstrapToken.value)
    }

    @Test
    fun clearGatewaySetupAuth_removesStoredGatewayAuth() {
        val context = RuntimeEnvironment.getApplication()
        val securePrefs = context.getSharedPreferences("openclaw.node.secure.test.clear", Context.MODE_PRIVATE)
        securePrefs.edit().clear().commit()
        val prefs = SecurePrefs(context, securePrefsOverride = securePrefs)

        prefs.setGatewayToken("shared-token")
        prefs.setGatewayBootstrapToken("bootstrap-token")
        prefs.setGatewayPassword("password-token")

        prefs.clearGatewaySetupAuth()

        assertEquals("", prefs.gatewayToken.value)
        assertEquals("", prefs.gatewayBootstrapToken.value)
        assertNull(prefs.loadGatewayToken())
        assertNull(prefs.loadGatewayBootstrapToken())
        assertNull(prefs.loadGatewayPassword())
    }

    @Test
    fun airVisionDisplaySettings_persistAndClamp() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Infinity)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.EyeCare)
        prefs.setAirVisionBrightnessPercent(3)
        prefs.setAirVisionBlueLightFilterPercent(125)
        prefs.setAirVisionDistanceCm(120)
        prefs.setAirVisionIpdMm(67)
        prefs.setAirVisionMotionSyncEnabled(false)
        prefs.setAirVisionThreeDModeEnabled(true)
        prefs.setAirVisionLightLoadModeEnabled(true)

        val reloaded = SecurePrefs(context)
        val settings = reloaded.airVisionDisplaySettings.value

        assertEquals(AirVisionViewMode.Infinity, settings.viewMode)
        assertEquals(AirVisionSplendidMode.EyeCare, settings.splendidMode)
        assertEquals(AirVisionDisplaySettings.MIN_BRIGHTNESS_PERCENT, settings.brightnessPercent)
        assertEquals(AirVisionDisplaySettings.MAX_BLUE_LIGHT_FILTER_PERCENT, settings.blueLightFilterPercent)
        assertEquals(120, settings.distanceCm)
        assertEquals(67, settings.ipdMm)
        assertEquals(false, settings.motionSyncEnabled)
        assertEquals(false, settings.threeDModeEnabled)
        assertEquals(true, settings.lightLoadModeEnabled)
    }

    @Test
    fun airVisionDisplaySettings_persistPerViewModeProfile() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionBrightnessPercent(42)
        prefs.setAirVisionDistanceCm(88)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.UpperRight)
        prefs.setAirVisionSafeAreaPercent(12)
        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)

        assertEquals(AirVisionViewMode.Gaming, prefs.airVisionDisplaySettings.value.viewMode)
        assertEquals(AirVisionSplendidMode.Game, prefs.airVisionDisplaySettings.value.splendidMode)
        assertEquals(AirVisionHudPlacement.Center, prefs.airVisionDisplaySettings.value.hudPlacement)
        assertEquals(100, prefs.airVisionDisplaySettings.value.brightnessPercent)

        prefs.setAirVisionBrightnessPercent(55)
        prefs.setAirVisionDistanceCm(66)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.Theater)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.LowerCenter)
        prefs.setAirVisionSafeAreaPercent(7)
        prefs.setAirVisionPhysicalMainScreenVisible(false)
        prefs.setAirVisionLightLoadModeEnabled(false)
        prefs.setAirVisionThreeDModeEnabled(true)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)

        assertEquals(42, prefs.airVisionDisplaySettings.value.brightnessPercent)
        assertEquals(88, prefs.airVisionDisplaySettings.value.distanceCm)
        assertEquals(AirVisionHudPlacement.UpperRight, prefs.airVisionDisplaySettings.value.hudPlacement)
        assertEquals(12, prefs.airVisionDisplaySettings.value.safeAreaPercent)
        assertEquals(true, prefs.airVisionDisplaySettings.value.physicalMainScreenVisible)
        assertEquals(true, prefs.airVisionPhysicalMainScreenVisible.value)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)

        val reloaded = SecurePrefs(context).airVisionDisplaySettings.value
        assertEquals(AirVisionViewMode.Gaming, reloaded.viewMode)
        assertEquals(AirVisionSplendidMode.Theater, reloaded.splendidMode)
        assertEquals(AirVisionHudPlacement.LowerCenter, reloaded.hudPlacement)
        assertEquals(55, reloaded.brightnessPercent)
        assertEquals(66, reloaded.distanceCm)
        assertEquals(7, reloaded.safeAreaPercent)
        assertEquals(false, reloaded.physicalMainScreenVisible)
        assertEquals(false, reloaded.lightLoadModeEnabled)
        assertEquals(true, reloaded.threeDModeEnabled)
    }

    @Test
    fun resetActiveAirVisionProfile_restoresCurrentModeDefaults() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Infinity)
        prefs.setAirVisionBrightnessPercent(33)
        prefs.setAirVisionDistanceCm(44)
        prefs.setAirVisionIpdMm(61)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.LowerCenter)
        prefs.setAirVisionSafeAreaPercent(1)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.Theater)
        prefs.setAirVisionLightLoadModeEnabled(false)

        prefs.resetActiveAirVisionProfile()

        val reloaded = SecurePrefs(context).airVisionDisplaySettings.value
        val defaults = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Infinity)
        assertEquals(defaults, reloaded)
    }

    @Test
    fun copyActiveAirVisionProfileTo_persistsSettingsIntoCustomSlot() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.EyeCare)
        prefs.setAirVisionBlueLightFilterPercent(44)
        prefs.setAirVisionBrightnessPercent(55)
        prefs.setAirVisionDistanceCm(66)
        prefs.setAirVisionIpdMm(68)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.LowerCenter)
        prefs.setAirVisionSafeAreaPercent(9)
        prefs.setAirVisionPhysicalMainScreenVisible(false)
        prefs.setAirVisionMotionSyncEnabled(false)
        prefs.setAirVisionThreeDModeEnabled(true)
        prefs.setAirVisionLightLoadModeEnabled(false)

        assertEquals(true, prefs.copyActiveAirVisionProfileTo(AirVisionViewMode.Custom1))
        assertEquals(false, prefs.copyActiveAirVisionProfileTo(AirVisionViewMode.Gaming))

        val reloaded = SecurePrefs(context)
        reloaded.setAirVisionViewMode(AirVisionViewMode.Custom1)
        val copied = reloaded.airVisionDisplaySettings.value

        assertEquals(AirVisionViewMode.Custom1, copied.viewMode)
        assertEquals(AirVisionSplendidMode.EyeCare, copied.splendidMode)
        assertEquals(44, copied.blueLightFilterPercent)
        assertEquals(55, copied.brightnessPercent)
        assertEquals(66, copied.distanceCm)
        assertEquals(68, copied.ipdMm)
        assertEquals(AirVisionHudPlacement.LowerCenter, copied.hudPlacement)
        assertEquals(9, copied.safeAreaPercent)
        assertEquals(false, copied.physicalMainScreenVisible)
        assertEquals(false, copied.motionSyncEnabled)
        assertEquals(true, copied.threeDModeEnabled)
        assertEquals(false, copied.lightLoadModeEnabled)
    }

    @Test
    fun airVisionProfileBackup_exportsAndImportsTuningState() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom1, "Walk HUD")
        prefs.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom2, "Desk HUD")
        prefs.setAirVisionHudSingleTapAction(AirVisionHudTouchAction.ToggleMic)
        prefs.setAirVisionHudDoubleTapAction(AirVisionHudDoubleTapAction.DismissNotification)
        prefs.setAirVisionHudSwipeAction(AirVisionHudSwipeAction.None)
        prefs.setAirVisionHudBrightnessKeyAction(AirVisionHudKeyAction.AdjustDistance)
        prefs.setAirVisionHudMediaKeyAction(AirVisionHudMediaKeyAction.None)
        prefs.setAirVisionAppLanguage(AirVisionAppLanguage.Spanish)
        prefs.setAirVisionStartupDestination(AirVisionStartupDestination.Voice)
        prefs.setAirVisionRememberedDisplay(AirVisionHudDisplayFingerprint("Samsung DeX Monitor", 2560, 1440))
        prefs.setAirVisionHudDisplayTarget(AirVisionHudDisplayTarget.RememberedExternal)
        prefs.setAirVisionDemoModeEnabled(true)
        prefs.setSpeakerEnabled(false)
        prefs.setNativeCaptionsEnabled(true)
        prefs.setTranslationCaptionSourceLanguage("pt-BR")
        prefs.setTranslationCaptionTargetLanguage("ja")

        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        prefs.setAirVisionBrightnessPercent(51)
        prefs.setAirVisionDistanceCm(111)
        prefs.setAirVisionHudScalePercent(88)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.UpperRight)
        prefs.setAirVisionPhysicalMainScreenVisible(false)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)
        prefs.setAirVisionLightLoadModeEnabled(false)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.EyeCare)
        prefs.setAirVisionBlueLightFilterPercent(49)
        prefs.setAirVisionBrightnessPercent(96)
        prefs.setAirVisionDistanceCm(61)
        prefs.setAirVisionHudScalePercent(123)
        prefs.setAirVisionIpdMm(69)
        prefs.setAirVisionHudPlacement(AirVisionHudPlacement.Center)
        prefs.setAirVisionSafeAreaPercent(4)
        prefs.setAirVisionThreeDModeEnabled(true)

        val backup = prefs.exportAirVisionProfileBackup()
        val backupRoot = Json.parseToJsonElement(backup).jsonObject
        val runtimeProfiles = backupRoot.getValue("runtimeProfiles").jsonArray
        val appPreferences = backupRoot.getValue("appPreferences").jsonObject
        val infinityRuntime =
            runtimeProfiles
                .first { it.jsonObject.getValue("viewMode").jsonPrimitive.content == AirVisionViewMode.Infinity.rawValue }
                .jsonObject

        assertEquals("5", backupRoot.getValue("version").jsonPrimitive.content)
        assertEquals(AirVisionViewMode.entries.size, runtimeProfiles.size)
        assertEquals(false, appPreferences.getValue("speakerEnabled").jsonPrimitive.boolean)
        assertEquals(true, appPreferences.getValue("nativeCaptionsEnabled").jsonPrimitive.boolean)
        assertEquals("pt", appPreferences.getValue("translationCaptionSourceLanguage").jsonPrimitive.content)
        assertEquals("ja", appPreferences.getValue("translationCaptionTargetLanguage").jsonPrimitive.content)
        assertEquals(
            "Samsung DeX Monitor",
            appPreferences
                .getValue("rememberedHudDisplay")
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        assertEquals(
            AirVisionDisplaySettings.LIGHT_LOAD_HUD_TRANSCRIPT_ENTRY_COUNT,
            infinityRuntime.getValue("hudTranscriptEntryCount").jsonPrimitive.int,
        )
        assertEquals(
            AirVisionDisplaySettings.LIGHT_LOAD_HUD_CAPTION_ENTRY_COUNT,
            infinityRuntime.getValue("hudCaptionEntryCount").jsonPrimitive.int,
        )
        assertEquals(false, infinityRuntime.getValue("ipdAdjustmentEnabled").jsonPrimitive.boolean)
        assertEquals(false, infinityRuntime.getValue("threeDModeAvailable").jsonPrimitive.boolean)
        assertEquals(false, infinityRuntime.getValue("colorPreviewOverlaysEnabled").jsonPrimitive.boolean)

        plainPrefs.edit().clear().commit()
        val importedPrefs = SecurePrefs(context)
        importedPrefs.importAirVisionProfileBackup(backup)

        assertEquals(AirVisionViewMode.Gaming, importedPrefs.airVisionDisplaySettings.value.viewMode)
        assertEquals(AirVisionSplendidMode.EyeCare, importedPrefs.airVisionDisplaySettings.value.splendidMode)
        assertEquals(49, importedPrefs.airVisionDisplaySettings.value.blueLightFilterPercent)
        assertEquals(96, importedPrefs.airVisionDisplaySettings.value.brightnessPercent)
        assertEquals(61, importedPrefs.airVisionDisplaySettings.value.distanceCm)
        assertEquals(123, importedPrefs.airVisionDisplaySettings.value.hudScalePercent)
        assertEquals(69, importedPrefs.airVisionDisplaySettings.value.ipdMm)
        assertEquals(AirVisionHudPlacement.Center, importedPrefs.airVisionDisplaySettings.value.hudPlacement)
        assertEquals(4, importedPrefs.airVisionDisplaySettings.value.safeAreaPercent)
        assertEquals(true, importedPrefs.airVisionDisplaySettings.value.threeDModeEnabled)
        assertEquals("Walk HUD", importedPrefs.airVisionCustomProfileLabels.value.custom1)
        assertEquals("Desk HUD", importedPrefs.airVisionCustomProfileLabels.value.custom2)
        assertEquals(AirVisionHudTouchAction.ToggleMic, importedPrefs.airVisionHudControls.value.singleTapAction)
        assertEquals(
            AirVisionHudDoubleTapAction.DismissNotification,
            importedPrefs.airVisionHudControls.value.doubleTapAction,
        )
        assertEquals(AirVisionHudSwipeAction.None, importedPrefs.airVisionHudControls.value.swipeAction)
        assertEquals(AirVisionHudKeyAction.AdjustDistance, importedPrefs.airVisionHudControls.value.brightnessKeyAction)
        assertEquals(AirVisionHudMediaKeyAction.None, importedPrefs.airVisionHudControls.value.mediaKeyAction)
        assertEquals(AirVisionAppLanguage.Spanish, importedPrefs.airVisionAppLanguage.value)
        assertEquals(AirVisionStartupDestination.Voice, importedPrefs.airVisionStartupDestination.value)
        assertEquals(AirVisionHudDisplayTarget.RememberedExternal, importedPrefs.airVisionHudDisplayTarget.value)
        assertEquals(
            AirVisionHudDisplayFingerprint("Samsung DeX Monitor", 2560, 1440),
            importedPrefs.airVisionRememberedDisplay.value,
        )
        assertEquals(true, importedPrefs.airVisionDemoModeEnabled.value)
        assertEquals(false, importedPrefs.speakerEnabled.value)
        assertEquals(true, importedPrefs.nativeCaptionsEnabled.value)
        assertEquals("pt", importedPrefs.translationCaptionSourceLanguage.value)
        assertEquals("ja", importedPrefs.translationCaptionTargetLanguage.value)

        importedPrefs.setAirVisionViewMode(AirVisionViewMode.Working)
        assertEquals(51, importedPrefs.airVisionDisplaySettings.value.brightnessPercent)
        assertEquals(111, importedPrefs.airVisionDisplaySettings.value.distanceCm)
        assertEquals(88, importedPrefs.airVisionDisplaySettings.value.hudScalePercent)
        assertEquals(AirVisionHudPlacement.UpperRight, importedPrefs.airVisionDisplaySettings.value.hudPlacement)
        assertEquals(false, importedPrefs.airVisionDisplaySettings.value.physicalMainScreenVisible)
    }

    @Test
    fun airVisionRememberedDisplay_persistsAndCanBeCleared() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()
        val prefs = SecurePrefs(context)

        prefs.setAirVisionRememberedDisplay(AirVisionHudDisplayFingerprint("USB-C projector", 1920, 1200))

        assertEquals(
            AirVisionHudDisplayFingerprint("USB-C projector", 1920, 1200),
            SecurePrefs(context).airVisionRememberedDisplay.value,
        )

        prefs.setAirVisionRememberedDisplay(null)

        assertNull(SecurePrefs(context).airVisionRememberedDisplay.value)
    }

    @Test
    fun importAirVisionProfileBackup_acceptsVersionOneBackupsWithoutRuntimeProfiles() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        val legacyBackup =
            """
            {
              "schema": "openclaw.airvision.m1.profile-backup",
              "version": 1,
              "activeViewMode": "custom1",
              "customLabels": { "custom1": "Walk HUD", "custom2": "Desk HUD" },
              "hudControls": {
                "singleTapAction": "dismiss_notification",
                "doubleTapAction": "toggle_mic",
                "swipeAction": "scroll_chat",
                "brightnessKeyAction": "scroll_chat",
                "mediaKeyAction": "double_tap_toggle_mic"
              },
              "appPreferences": {
                "language": "system",
                "startupDestination": "hud",
                "hudDisplayTarget": "airvision_preferred",
                "demoModeEnabled": false
              },
              "profiles": [
                ${backupProfileJson("working")},
                ${backupProfileJson("gaming")},
                ${backupProfileJson("infinity")},
                ${backupProfileJson("custom1", brightnessPercent = 44, distanceCm = 92)},
                ${backupProfileJson("custom2")}
              ]
            }
            """.trimIndent()

        prefs.importAirVisionProfileBackup(legacyBackup)

        assertEquals(AirVisionViewMode.Custom1, prefs.airVisionDisplaySettings.value.viewMode)
        assertEquals(44, prefs.airVisionDisplaySettings.value.brightnessPercent)
        assertEquals(92, prefs.airVisionDisplaySettings.value.distanceCm)
        assertEquals("Walk HUD", prefs.airVisionCustomProfileLabels.value.custom1)
        assertEquals(true, prefs.speakerEnabled.value)
        assertEquals(false, prefs.nativeCaptionsEnabled.value)
        assertEquals(TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE, prefs.translationCaptionSourceLanguage.value)
        assertEquals(TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE, prefs.translationCaptionTargetLanguage.value)
    }

    @Test
    fun importAirVisionProfileBackup_rejectsMalformedBackup() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)

        assertThrows(IllegalArgumentException::class.java) {
            prefs.importAirVisionProfileBackup(
                """
                {
                  "schema": "openclaw.airvision.m1.profile-backup",
                  "version": 1,
                  "activeViewMode": "working",
                  "customLabels": { "custom1": "A", "custom2": "B" },
                  "hudControls": {
                    "singleTapAction": "dismiss_notification",
                    "doubleTapAction": "toggle_mic",
                    "swipeAction": "scroll_chat",
                    "brightnessKeyAction": "scroll_chat",
                    "mediaKeyAction": "double_tap_toggle_mic"
                  },
                  "appPreferences": {
                    "language": "system",
                    "startupDestination": "hud",
                    "hudDisplayTarget": "airvision_preferred",
                    "demoModeEnabled": false
                  },
                  "profiles": []
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun importAirVisionProfileBackup_rejectsDuplicateProfileSlots() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        val duplicateBackup =
            """
            {
              "schema": "openclaw.airvision.m1.profile-backup",
              "version": 1,
              "activeViewMode": "working",
              "customLabels": { "custom1": "A", "custom2": "B" },
              "hudControls": {
                "singleTapAction": "dismiss_notification",
                "doubleTapAction": "toggle_mic",
                "swipeAction": "scroll_chat",
                "brightnessKeyAction": "scroll_chat",
                "mediaKeyAction": "double_tap_toggle_mic"
              },
              "appPreferences": {
                "language": "system",
                "startupDestination": "hud",
                "hudDisplayTarget": "airvision_preferred",
                "demoModeEnabled": false
              },
              "profiles": [
                ${backupProfileJson("working")},
                ${backupProfileJson("gaming")},
                ${backupProfileJson("infinity")},
                ${backupProfileJson("custom1")},
                ${backupProfileJson("custom2")},
                ${backupProfileJson("working")}
              ]
            }
            """.trimIndent()

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                prefs.importAirVisionProfileBackup(duplicateBackup)
            }

        assertEquals("Profile backup includes duplicate profiles: Working.", error.message)
    }

    @Test
    fun importAirVisionProfileBackup_rejectsMissingRequiredProfileSlots() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        val missingBackup =
            """
            {
              "schema": "openclaw.airvision.m1.profile-backup",
              "version": 1,
              "activeViewMode": "working",
              "customLabels": { "custom1": "A", "custom2": "B" },
              "hudControls": {
                "singleTapAction": "dismiss_notification",
                "doubleTapAction": "toggle_mic",
                "swipeAction": "scroll_chat",
                "brightnessKeyAction": "scroll_chat",
                "mediaKeyAction": "double_tap_toggle_mic"
              },
              "appPreferences": {
                "language": "system",
                "startupDestination": "hud",
                "hudDisplayTarget": "airvision_preferred",
                "demoModeEnabled": false
              },
              "profiles": [
                ${backupProfileJson("working")},
                ${backupProfileJson("gaming")},
                ${backupProfileJson("infinity")},
                ${backupProfileJson("custom1")}
              ]
            }
            """.trimIndent()

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                prefs.importAirVisionProfileBackup(missingBackup)
            }

        assertEquals("Profile backup is missing: Custom 2.", error.message)
    }

    @Test
    fun airVisionIpdChange_isLockedWhileLightLoadModeIsEnabled() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        prefs.setAirVisionIpdMm(64)
        prefs.setAirVisionLightLoadModeEnabled(true)
        prefs.setAirVisionIpdMm(70)

        assertEquals(64, prefs.airVisionDisplaySettings.value.ipdMm)

        prefs.setAirVisionLightLoadModeEnabled(false)
        prefs.setAirVisionIpdMm(70)

        assertEquals(70, SecurePrefs(context).airVisionDisplaySettings.value.ipdMm)
    }

    @Test
    fun airVisionIpdChange_clampsToAsusCompanionRange() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        prefs.setAirVisionIpdMm(40)

        assertEquals(AirVisionDisplaySettings.MIN_ASUS_IPD_MM, prefs.airVisionDisplaySettings.value.ipdMm)

        prefs.setAirVisionIpdMm(90)

        assertEquals(AirVisionDisplaySettings.MAX_ASUS_IPD_MM, SecurePrefs(context).airVisionDisplaySettings.value.ipdMm)
    }

    @Test
    fun airVisionBlueLightFilterChange_isLockedOutsideEyeCareMode() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.Standard)
        prefs.setAirVisionBlueLightFilterPercent(50)

        assertEquals(0, prefs.airVisionDisplaySettings.value.blueLightFilterPercent)

        prefs.setAirVisionSplendidMode(AirVisionSplendidMode.EyeCare)
        prefs.setAirVisionBlueLightFilterPercent(50)

        assertEquals(50, SecurePrefs(context).airVisionDisplaySettings.value.blueLightFilterPercent)
    }

    @Test
    fun airVisionThreeDMode_isLockedWhileLightLoadModeIsEnabled() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        prefs.setAirVisionThreeDModeEnabled(true)

        assertEquals(true, prefs.airVisionDisplaySettings.value.threeDModeEnabled)

        prefs.setAirVisionLightLoadModeEnabled(true)
        prefs.setAirVisionThreeDModeEnabled(true)

        assertEquals(false, prefs.airVisionDisplaySettings.value.threeDModeEnabled)

        prefs.setAirVisionLightLoadModeEnabled(false)
        prefs.setAirVisionThreeDModeEnabled(true)

        assertEquals(true, SecurePrefs(context).airVisionDisplaySettings.value.threeDModeEnabled)
    }

    @Test
    fun adjustAirVisionDistanceCm_clampsAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionDistanceCm(AirVisionDisplaySettings.MIN_DISTANCE_CM)
        prefs.adjustAirVisionDistanceCm(-5)
        assertEquals(AirVisionDisplaySettings.MIN_DISTANCE_CM, prefs.airVisionDisplaySettings.value.distanceCm)

        prefs.setAirVisionDistanceCm(AirVisionDisplaySettings.MAX_DISTANCE_CM)
        prefs.adjustAirVisionDistanceCm(5)
        assertEquals(AirVisionDisplaySettings.MAX_DISTANCE_CM, SecurePrefs(context).airVisionDisplaySettings.value.distanceCm)
    }

    @Test
    fun adjustAirVisionBrightnessPercent_clampsAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionBrightnessPercent(AirVisionDisplaySettings.MIN_BRIGHTNESS_PERCENT)
        prefs.adjustAirVisionBrightnessPercent(-5)
        assertEquals(AirVisionDisplaySettings.MIN_BRIGHTNESS_PERCENT, prefs.airVisionDisplaySettings.value.brightnessPercent)

        prefs.setAirVisionBrightnessPercent(AirVisionDisplaySettings.MAX_BRIGHTNESS_PERCENT)
        prefs.adjustAirVisionBrightnessPercent(5)
        assertEquals(
            AirVisionDisplaySettings.MAX_BRIGHTNESS_PERCENT,
            SecurePrefs(context).airVisionDisplaySettings.value.brightnessPercent,
        )
    }

    @Test
    fun airVisionHudScalePercent_clampsAndPersistsPerProfile() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionHudScalePercent(25)
        assertEquals(AirVisionDisplaySettings.MIN_HUD_SCALE_PERCENT, prefs.airVisionDisplaySettings.value.hudScalePercent)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)
        prefs.setAirVisionHudScalePercent(180)
        assertEquals(AirVisionDisplaySettings.MAX_HUD_SCALE_PERCENT, prefs.airVisionDisplaySettings.value.hudScalePercent)

        val reloaded = SecurePrefs(context)
        assertEquals(AirVisionDisplaySettings.MAX_HUD_SCALE_PERCENT, reloaded.airVisionDisplaySettings.value.hudScalePercent)
        reloaded.setAirVisionViewMode(AirVisionViewMode.Working)
        assertEquals(AirVisionDisplaySettings.MIN_HUD_SCALE_PERCENT, reloaded.airVisionDisplaySettings.value.hudScalePercent)
    }

    @Test
    fun airVisionHudControls_persist() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.setAirVisionHudSingleTapAction(AirVisionHudTouchAction.ToggleMic)
        prefs.setAirVisionHudDoubleTapAction(AirVisionHudDoubleTapAction.DismissNotification)
        prefs.setAirVisionHudSwipeAction(AirVisionHudSwipeAction.None)
        prefs.setAirVisionHudBrightnessKeyAction(AirVisionHudKeyAction.AdjustDistance)
        prefs.setAirVisionHudMediaKeyAction(AirVisionHudMediaKeyAction.None)

        val reloaded = SecurePrefs(context)
        val controls = reloaded.airVisionHudControls.value

        assertEquals(AirVisionHudTouchAction.ToggleMic, controls.singleTapAction)
        assertEquals(AirVisionHudDoubleTapAction.DismissNotification, controls.doubleTapAction)
        assertEquals(AirVisionHudSwipeAction.None, controls.swipeAction)
        assertEquals(AirVisionHudKeyAction.AdjustDistance, controls.brightnessKeyAction)
        assertEquals(AirVisionHudMediaKeyAction.None, controls.mediaKeyAction)
    }

    @Test
    fun airVisionAppLanguage_persists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(AirVisionAppLanguage.System, prefs.airVisionAppLanguage.value)

        prefs.setAirVisionAppLanguage(AirVisionAppLanguage.Spanish)

        assertEquals(AirVisionAppLanguage.Spanish, SecurePrefs(context).airVisionAppLanguage.value)
    }

    @Test
    fun airVisionStartupDestination_persists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(AirVisionStartupDestination.Hud, prefs.airVisionStartupDestination.value)

        prefs.setAirVisionStartupDestination(AirVisionStartupDestination.Agents)

        assertEquals(AirVisionStartupDestination.Agents, SecurePrefs(context).airVisionStartupDestination.value)
    }

    @Test
    fun airVisionHudDisplayTarget_persists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(AirVisionHudDisplayTarget.Automatic, prefs.airVisionHudDisplayTarget.value)

        prefs.setAirVisionHudDisplayTarget(AirVisionHudDisplayTarget.LargestExternal)

        assertEquals(AirVisionHudDisplayTarget.LargestExternal, SecurePrefs(context).airVisionHudDisplayTarget.value)
    }

    @Test
    fun airVisionCustomProfileLabels_persistAndNormalize() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(AirVisionViewMode.Custom1.label, prefs.airVisionCustomProfileLabels.value.custom1)
        assertEquals(AirVisionViewMode.Custom2.label, prefs.airVisionCustomProfileLabels.value.custom2)

        prefs.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom1, "  walking   hud  ")
        prefs.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom2, "")
        prefs.setAirVisionCustomProfileLabel(AirVisionViewMode.Working, "Ignored")

        val reloaded = SecurePrefs(context).airVisionCustomProfileLabels.value
        assertEquals("walking hud", reloaded.custom1)
        assertEquals(AirVisionViewMode.Custom2.label, reloaded.custom2)
    }

    @Test
    fun airVisionDemoModeEnabled_persists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(false, prefs.airVisionDemoModeEnabled.value)

        prefs.setAirVisionDemoModeEnabled(true)

        assertEquals(true, SecurePrefs(context).airVisionDemoModeEnabled.value)
    }

    @Test
    fun airVisionPhysicalMainScreenVisible_persists() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        assertEquals(true, prefs.airVisionPhysicalMainScreenVisible.value)
        assertEquals(true, prefs.airVisionDisplaySettings.value.physicalMainScreenVisible)

        prefs.setAirVisionPhysicalMainScreenVisible(false)

        assertEquals(false, SecurePrefs(context).airVisionPhysicalMainScreenVisible.value)
        assertEquals(false, SecurePrefs(context).airVisionDisplaySettings.value.physicalMainScreenVisible)
    }

    @Test
    fun airVisionPhysicalMainScreenVisible_migratesLegacyGlobalValueAcrossProfiles() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs
            .edit()
            .clear()
            .putString("airVision.viewMode", AirVisionViewMode.Working.rawValue)
            .putString("airVision.hudPlacement.working", AirVisionHudPlacement.UpperCenter.rawValue)
            .putBoolean("airVision.physicalMainScreenVisible", false)
            .commit()

        val prefs = SecurePrefs(context)
        assertEquals(false, prefs.airVisionPhysicalMainScreenVisible.value)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)
        assertEquals(false, prefs.airVisionPhysicalMainScreenVisible.value)

        prefs.setAirVisionPhysicalMainScreenVisible(true)
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)
        assertEquals(false, prefs.airVisionPhysicalMainScreenVisible.value)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)
        assertEquals(true, SecurePrefs(context).airVisionPhysicalMainScreenVisible.value)
    }

    @Test
    fun airVisionFirmwareCaptureResults_persistSanitizedSummaryOnlyOutsideProfileBackup() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        val summary = prefs.importAirVisionFirmwareCaptureResults(airVisionCaptureResultsJson())

        assertEquals(
            "capture results: 0 validated, 0 captured-review, 9 pending, 0 protocol-ready, 0 validated-blocked, 9 blocked",
            summary.summary,
        )
        assertEquals(
            "capture results: 0 validated, 0 captured-review, 9 pending, 0 protocol-ready, " +
                "0 validated-blocked, 9 blocked; host=Cyber, tool=USBPcap/Wireshark",
            prefs.airVisionFirmwareCaptureResultsSummary.value,
        )

        val reloaded = SecurePrefs(context)
        assertEquals(
            "capture results: 0 validated, 0 captured-review, 9 pending, 0 protocol-ready, " +
                "0 validated-blocked, 9 blocked; host=Cyber, tool=USBPcap/Wireshark",
            reloaded.airVisionFirmwareCaptureResultsSummary.value,
        )
        assertEquals(AirVisionFirmwareFeature.entries.size, reloaded.airVisionFirmwareCaptureResults.value?.features?.size)
        assertFalse(reloaded.exportAirVisionProfileBackup().contains("firmwareCaptureResults"))
    }

    @Test
    fun airVisionFirmwareCaptureResults_rejectsInvalidImportWithoutReplacingExistingSummary() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.importAirVisionFirmwareCaptureResults(airVisionCaptureResultsJson())

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                prefs.importAirVisionFirmwareCaptureResults(
                    airVisionCaptureResultsJson(
                        overridesByRawKey =
                            mapOf(
                                "brightness" to
                                    """
                                    "writePayloadSummary": "token=do-not-store",
                                    "blockerReason": "Windows ASUS HID protocol capture has not been validated."
                                    """.trimIndent(),
                            ),
                    ),
                )
            }

        assertEquals(true, error.message.orEmpty().contains("secret or raw-serial-shaped"))
        assertEquals(
            "capture results: 0 validated, 0 captured-review, 9 pending, 0 protocol-ready, " +
                "0 validated-blocked, 9 blocked; host=Cyber, tool=USBPcap/Wireshark",
            prefs.airVisionFirmwareCaptureResultsSummary.value,
        )
    }

    @Test
    fun airVisionFirmwareCaptureResults_clearRemovesPersistedEvidence() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().commit()

        val prefs = SecurePrefs(context)
        prefs.importAirVisionFirmwareCaptureResults(airVisionCaptureResultsJson())
        assertEquals(AirVisionFirmwareFeature.entries.size, prefs.airVisionFirmwareCaptureResults.value?.features?.size)

        prefs.clearAirVisionFirmwareCaptureResults()

        assertNull(prefs.airVisionFirmwareCaptureResults.value)
        assertNull(prefs.airVisionFirmwareCaptureResultsSummary.value)
        val reloaded = SecurePrefs(context)
        assertNull(reloaded.airVisionFirmwareCaptureResults.value)
        assertNull(reloaded.airVisionFirmwareCaptureResultsSummary.value)
    }
}

private fun backupProfileJson(
    viewMode: String,
    brightnessPercent: Int = 80,
    distanceCm: Int = 75,
): String =
    """
    {
      "viewMode": "$viewMode",
      "splendidMode": "standard",
      "hudPlacement": "upper_left",
      "brightnessPercent": $brightnessPercent,
      "blueLightFilterPercent": 0,
      "distanceCm": $distanceCm,
      "ipdMm": 67,
      "safeAreaPercent": 5,
      "physicalMainScreenVisible": true,
      "motionSyncEnabled": true,
      "threeDModeEnabled": false,
      "lightLoadModeEnabled": false
    }
    """.trimIndent()

private fun airVisionCaptureResultsJson(
    overridesByRawKey: Map<String, String> = emptyMap(),
): String =
    """
    {
      "schema": "openclaw.airvision.firmwareCaptureResults",
      "version": 1,
      "payloadPolicy": "Sanitized summaries only.",
      "source": {
        "windowsHost": "Cyber",
        "captureTool": "USBPcap/Wireshark",
        "asusAirVisionAppVersion": null,
        "androidDiagnosticsExportSha256": null,
        "notes": "test"
      },
      "features": [
        ${AirVisionFirmwareFeature.entries.joinToString(",\n") { feature ->
        airVisionCaptureResultFeatureJson(feature, overridesByRawKey[feature.rawValue])
    }}
      ]
    }
    """.trimIndent()

private fun airVisionCaptureResultFeatureJson(
    feature: AirVisionFirmwareFeature,
    overrideFields: String?,
): String {
    val overrideKeys =
        overrideFields
            ?.lineSequence()
            ?.mapNotNull { line ->
                line
                    .trim()
                    .substringBefore(":", missingDelimiterValue = "")
                    .trim('"')
                    .takeIf(String::isNotBlank)
            }?.toSet()
            .orEmpty()
    val defaults =
        linkedMapOf(
            "status" to "\"pending\"",
            "writeReportId" to "null",
            "writeEndpoint" to "null",
            "writePayloadSummary" to "null",
            "readbackReportId" to "null",
            "readbackEndpoint" to "null",
            "readbackPayloadSummary" to "null",
            "checksumFramingNotes" to "null",
            "visibleStateConfirmed" to "false",
            "captureReferences" to "[]",
            "androidEnablementDecision" to "\"blocked\"",
            "blockerReason" to "\"Windows ASUS HID protocol capture has not been validated.\"",
        ).filterKeys { it !in overrideKeys }
    val fields =
        listOf(
            "\"rawKey\": \"${feature.rawValue}\"",
            "\"label\": \"${feature.label}\"",
            "\"probeValues\": [${feature.captureProbeValues.joinToString(", ") { "\"$it\"" }}]",
        ) +
            defaults.map { (key, value) -> "\"$key\": $value" } +
            listOfNotNull(overrideFields)
    return "{\n${fields.joinToString(",\n")}\n}"
}
