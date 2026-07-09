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
        prefs.setAirVisionViewMode(AirVisionViewMode.Working)

        assertEquals(42, prefs.airVisionDisplaySettings.value.brightnessPercent)
        assertEquals(88, prefs.airVisionDisplaySettings.value.distanceCm)
        assertEquals(AirVisionHudPlacement.UpperRight, prefs.airVisionDisplaySettings.value.hudPlacement)
        assertEquals(12, prefs.airVisionDisplaySettings.value.safeAreaPercent)

        prefs.setAirVisionViewMode(AirVisionViewMode.Gaming)

        val reloaded = SecurePrefs(context).airVisionDisplaySettings.value
        assertEquals(AirVisionViewMode.Gaming, reloaded.viewMode)
        assertEquals(AirVisionSplendidMode.Theater, reloaded.splendidMode)
        assertEquals(AirVisionHudPlacement.LowerCenter, reloaded.hudPlacement)
        assertEquals(55, reloaded.brightnessPercent)
        assertEquals(66, reloaded.distanceCm)
        assertEquals(7, reloaded.safeAreaPercent)
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
        prefs.setAirVisionHudBrightnessKeyAction(AirVisionHudKeyAction.None)
        prefs.setAirVisionHudMediaKeyAction(AirVisionHudMediaKeyAction.None)

        val reloaded = SecurePrefs(context)
        val controls = reloaded.airVisionHudControls.value

        assertEquals(AirVisionHudTouchAction.ToggleMic, controls.singleTapAction)
        assertEquals(AirVisionHudDoubleTapAction.DismissNotification, controls.doubleTapAction)
        assertEquals(AirVisionHudSwipeAction.None, controls.swipeAction)
        assertEquals(AirVisionHudKeyAction.None, controls.brightnessKeyAction)
        assertEquals(AirVisionHudMediaKeyAction.None, controls.mediaKeyAction)
    }
}
