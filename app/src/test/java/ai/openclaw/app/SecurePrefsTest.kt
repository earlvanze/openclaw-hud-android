package ai.openclaw.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
