package ai.openclaw.app

import android.view.KeyEvent

internal data class ExternalHudKeyLearningDecision(
    val consume: Boolean,
    val learnedKeyCode: Int? = null,
)

internal class ExternalHudKeyLearningController {
    private var pendingKeyCode: Int? = null

    fun handleKeyEvent(
        keyCode: Int,
        action: Int,
        learningEnabled: Boolean,
        isExternalAccessory: Boolean,
    ): ExternalHudKeyLearningDecision {
        pendingKeyCode?.let { pending ->
            if (keyCode == pending) {
                if (action == KeyEvent.ACTION_UP) {
                    pendingKeyCode = null
                    return ExternalHudKeyLearningDecision(
                        consume = true,
                        learnedKeyCode = pending.takeIf { learningEnabled },
                    )
                }
                return ExternalHudKeyLearningDecision(consume = true)
            }
        }

        if (!learningEnabled || !isExternalAccessory || normalizeExternalHudKeyCode(keyCode) == null) {
            return ExternalHudKeyLearningDecision(consume = false)
        }

        return when (action) {
            KeyEvent.ACTION_DOWN -> {
                pendingKeyCode = keyCode
                ExternalHudKeyLearningDecision(consume = true)
            }
            KeyEvent.ACTION_UP ->
                ExternalHudKeyLearningDecision(
                    consume = true,
                    learnedKeyCode = keyCode,
                )
            else -> ExternalHudKeyLearningDecision(consume = false)
        }
    }

    fun cancel() {
        pendingKeyCode = null
    }
}

internal fun normalizeExternalHudKeyCode(keyCode: Int?): Int? =
    keyCode?.takeIf { it > KeyEvent.KEYCODE_UNKNOWN && it <= MAX_EXTERNAL_HUD_KEY_CODE }

internal fun externalHudKeyLabel(keyCode: Int?): String {
    val normalized = normalizeExternalHudKeyCode(keyCode) ?: return "Built-in accessory keys"
    val readable =
        when (normalized) {
            in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F${normalized - KeyEvent.KEYCODE_F1 + 1}"
            else -> commonExternalHudKeyNames[normalized] ?: "Key"
        }
    return "$readable ($normalized)"
}

private const val MAX_EXTERNAL_HUD_KEY_CODE = 1024

private val commonExternalHudKeyNames =
    mapOf(
        KeyEvent.KEYCODE_ENTER to "Enter",
        KeyEvent.KEYCODE_NUMPAD_ENTER to "Numpad enter",
        KeyEvent.KEYCODE_DPAD_CENTER to "D-pad center",
        KeyEvent.KEYCODE_BUTTON_A to "Button A",
        KeyEvent.KEYCODE_BUTTON_B to "Button B",
        KeyEvent.KEYCODE_BUTTON_X to "Button X",
        KeyEvent.KEYCODE_BUTTON_Y to "Button Y",
        KeyEvent.KEYCODE_BUTTON_MODE to "Button mode",
        KeyEvent.KEYCODE_BUTTON_SELECT to "Button select",
        KeyEvent.KEYCODE_BUTTON_START to "Button start",
        KeyEvent.KEYCODE_HEADSETHOOK to "Headset hook",
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Media play/pause",
        KeyEvent.KEYCODE_ASSIST to "Assist",
        KeyEvent.KEYCODE_VOICE_ASSIST to "Voice assist",
        KeyEvent.KEYCODE_VOLUME_UP to "Volume up",
        KeyEvent.KEYCODE_VOLUME_DOWN to "Volume down",
        KeyEvent.KEYCODE_BRIGHTNESS_UP to "Brightness up",
        KeyEvent.KEYCODE_BRIGHTNESS_DOWN to "Brightness down",
    )
