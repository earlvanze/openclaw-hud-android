package ai.openclaw.app

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalHudKeyLearningControllerTest {
    @Test
    fun learnsExternalKeyOnMatchingDownAndUp() {
        val controller = ExternalHudKeyLearningController()

        assertEquals(
            ExternalHudKeyLearningDecision(consume = true),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_MODE,
                action = KeyEvent.ACTION_DOWN,
                learningEnabled = true,
                isExternalAccessory = true,
            ),
        )
        assertEquals(
            ExternalHudKeyLearningDecision(
                consume = true,
                learnedKeyCode = KeyEvent.KEYCODE_BUTTON_MODE,
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_MODE,
                action = KeyEvent.ACTION_UP,
                learningEnabled = true,
                isExternalAccessory = true,
            ),
        )
    }

    @Test
    fun ignoresInternalAndUnknownKeys() {
        val controller = ExternalHudKeyLearningController()

        assertEquals(
            ExternalHudKeyLearningDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_MODE,
                action = KeyEvent.ACTION_DOWN,
                learningEnabled = true,
                isExternalAccessory = false,
            ),
        )
        assertEquals(
            ExternalHudKeyLearningDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_UNKNOWN,
                action = KeyEvent.ACTION_DOWN,
                learningEnabled = true,
                isExternalAccessory = true,
            ),
        )
    }

    @Test
    fun canceledLearningStillConsumesPendingKeyUpWithoutSaving() {
        val controller = ExternalHudKeyLearningController()
        controller.handleKeyEvent(
            keyCode = KeyEvent.KEYCODE_F12,
            action = KeyEvent.ACTION_DOWN,
            learningEnabled = true,
            isExternalAccessory = true,
        )

        val decision =
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_F12,
                action = KeyEvent.ACTION_UP,
                learningEnabled = false,
                isExternalAccessory = true,
            )

        assertEquals(true, decision.consume)
        assertNull(decision.learnedKeyCode)
    }

    @Test
    fun labelsKeyCodeWithoutAssumingDeviceModel() {
        assertEquals("Button mode (110)", externalHudKeyLabel(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals("Built-in accessory keys", externalHudKeyLabel(null))
        assertNull(normalizeExternalHudKeyCode(KeyEvent.KEYCODE_UNKNOWN))
    }
}
