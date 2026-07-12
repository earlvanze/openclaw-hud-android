package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AirVisionShortcutMenuStatusTest {
    @Test
    fun from_mapsBrightnessVolumeAndDistanceStatus() {
        val status =
            AirVisionShortcutMenuStatus.from(
                controls =
                    AirVisionHudControls(
                        brightnessKeyAction = AirVisionHudKeyAction.AdjustDistance,
                    ),
                speakerEnabled = true,
            )

        assertEquals("brightness keys adjust virtual distance; panel brightness remains firmware/system owned", status.brightness)
        assertEquals("OpenClaw speaker/TTS output enabled; hardware volume route remains Android system or M1 firmware owned", status.volume)
        assertEquals("Android virtual projection distance, 5 cm per brightness-key press", status.distance)
        assertEquals(2, status.androidMappedCount)
        assertEquals(1, status.firmwareOrSystemOwnedCount)
        assertEquals(
            "Shortcut menu parity: brightness Adjust distance, volume speaker on, distance mapped; 2 Android-mapped, 1 firmware/system-owned.",
            status.summary,
        )
    }
}
