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
        assertEquals(
            "Android maps virtual-distance adjustment to M1 brightness key events; Windows cursor-follow, center-cursor, Unity mirror window, and 3DoF remain unavailable on Android.",
            fallback.summary,
        )
    }
}
