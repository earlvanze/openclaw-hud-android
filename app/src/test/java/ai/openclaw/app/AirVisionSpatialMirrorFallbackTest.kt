package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionSpatialMirrorFallbackTest {
    @Test
    fun from_reportsWindowsOnlyControlsAndAndroidFallbackActions() {
        val fallback =
            AirVisionSpatialMirrorFallback.from(
                AirVisionHudControls(brightnessKeyAction = AirVisionHudKeyAction.AdjustDistance),
            )

        assertFalse(fallback.cursorFollowAvailable)
        assertFalse(fallback.centerCursorAvailable)
        assertFalse(fallback.threeDofAvailable)
        assertFalse(fallback.unityMirrorWindowAvailable)
        assertTrue(fallback.distanceHotkeyMapped)
        assertTrue(fallback.hardwareTouchpadPassthrough)
        assertEquals(
            listOf(
                "Open Android Cast settings from AirVision M1 settings.",
                "Open Android Display settings from AirVision M1 settings.",
                "Use Samsung DeX or Android screen sharing outside OpenClaw HUD when a projected-glasses-view mirror is needed.",
            ),
            fallback.androidMirrorFallbackActions,
        )
        assertEquals(2, fallback.androidMirrorFallbackLaunchActions.size)
        assertEquals("android_cast_settings", fallback.androidMirrorFallbackLaunchActions[0].id)
        assertEquals("Cast", fallback.androidMirrorFallbackLaunchActions[0].label)
        assertEquals("android.settings.CAST_SETTINGS", fallback.androidMirrorFallbackLaunchActions[0].androidIntentAction)
        assertEquals("android.settings.DISPLAY_SETTINGS", fallback.androidMirrorFallbackLaunchActions[0].fallbackIntentAction)
        assertTrue(fallback.androidMirrorFallbackLaunchActions[0].summary.contains("falling back to Display settings"))
        assertEquals("android_display_settings", fallback.androidMirrorFallbackLaunchActions[1].id)
        assertEquals("Display", fallback.androidMirrorFallbackLaunchActions[1].label)
        assertEquals("android.settings.DISPLAY_SETTINGS", fallback.androidMirrorFallbackLaunchActions[1].androidIntentAction)
        assertEquals(null, fallback.androidMirrorFallbackLaunchActions[1].fallbackIntentAction)
        assertEquals(
            "Android maps virtual-distance adjustment to M1 brightness key events; Windows cursor-follow, center-cursor, Unity mirror window, and 3DoF remain unavailable on Android.",
            fallback.summary,
        )
    }
}
