package ai.openclaw.app

import android.view.InputDevice
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ExternalHudInputMonitorTest {
    @Test
    fun append_keepsNewestBoundedEvents() {
        val events =
            (1L..10L).fold(emptyList<ExternalHudInputEvent>()) { current, sequence ->
                appendExternalHudInputEvent(
                    current = current,
                    event = event(sequence),
                )
            }

        assertEquals((10L downTo 3L).toList(), events.map { it.sequence })
    }

    @Test
    fun append_rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            appendExternalHudInputEvent(emptyList(), event(1L), limit = 0)
        }
    }

    @Test
    fun sanitize_removesControlWhitespaceAndBoundsLength() {
        assertEquals("External Display Name", sanitizeExternalHudInputText("  External\nDisplay\tName  "))
        assertEquals("12345", sanitizeExternalHudInputText("123456789", maxLength = 5))
        assertNull(sanitizeExternalHudInputText(" \n\t "))
    }

    @Test
    fun sourceLabel_prefersSpecificExternalInputTypes() {
        assertEquals("Touchscreen", externalHudInputSourceLabel(InputDevice.SOURCE_TOUCHSCREEN))
        assertEquals("Touchpad", externalHudInputSourceLabel(InputDevice.SOURCE_TOUCHPAD))
        assertEquals("Gamepad", externalHudInputSourceLabel(InputDevice.SOURCE_GAMEPAD))
        assertEquals("Joystick", externalHudInputSourceLabel(InputDevice.SOURCE_JOYSTICK))
        assertEquals("Source 0x0", externalHudInputSourceLabel(0))
    }

    @Test
    fun keyInputLabel_includesReadableKeyAndAction() {
        assertEquals(
            "Media play/pause (85) up",
            externalHudKeyInputLabel(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun keyCommandLabel_distinguishesMappedAndPassthroughEvents() {
        assertEquals(
            "Await second mic tap",
            externalHudKeyCommandLabel(AirVisionHudKeyCommand.ArmMicDoubleTap, consumed = true),
        )
        assertEquals("Consumed", externalHudKeyCommandLabel(command = null, consumed = true))
        assertEquals("Android passthrough", externalHudKeyCommandLabel(command = null, consumed = false))
    }

    @Test
    fun summary_isReadableAndDoesNotExposeUnboundedText() {
        val event =
            ExternalHudInputEvent(
                sequence = 4,
                kind = ExternalHudInputKind.Key,
                input = "Media play/pause up",
                source = "Keyboard",
                deviceName = "Wearable Control",
                mappedAction = "Await second mic tap",
                handled = true,
            )

        assertEquals(
            "#4 Media play/pause up from Keyboard (Wearable Control) -> Await second mic tap [handled]",
            event.summary,
        )
        assertEquals("Input: Media play/pause up -> Await second mic tap", event.hudMessage)
    }

    private fun event(sequence: Long): ExternalHudInputEvent =
        ExternalHudInputEvent(
            sequence = sequence,
            kind = ExternalHudInputKind.Touch,
            input = "Touch down",
            source = "Touchscreen",
            deviceName = null,
            mappedAction = "Forward to HUD",
            handled = true,
        )
}
