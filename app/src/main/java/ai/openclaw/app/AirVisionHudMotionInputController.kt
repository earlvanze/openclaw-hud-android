package ai.openclaw.app

import android.view.KeyEvent
import kotlin.math.abs

internal class AirVisionHudMotionInputController(
    private val absoluteAxisTimeoutMs: Long = ABSOLUTE_AXIS_TIMEOUT_MS,
) {
    private data class AxisKey(
        val deviceId: Int,
        val axisId: Int,
    )

    private data class AxisState(
        val value: Float,
        val eventTimeMs: Long,
    )

    private val absoluteAxisState = mutableMapOf<AxisKey, AxisState>()

    fun absoluteAxisScrollDelta(
        deviceId: Int,
        value: Float,
        eventTimeMs: Long,
        axisId: Int = 0,
        rangeSpan: Float? = null,
    ): Float? {
        if (!value.isFinite()) return null

        val key = AxisKey(deviceId = deviceId, axisId = axisId)
        val previous = absoluteAxisState.put(key, AxisState(value, eventTimeMs)) ?: return null
        if (eventTimeMs - previous.eventTimeMs !in 0..absoluteAxisTimeoutMs) return null

        val span = rangeSpan?.takeIf { it.isFinite() && abs(it) > ABSOLUTE_AXIS_DEAD_ZONE }
        val axisDelta = (value - previous.value) / (span ?: 1f)
        if (abs(axisDelta) < ABSOLUTE_AXIS_DEAD_ZONE) return null
        return -axisDelta * ABSOLUTE_AXIS_SCROLL_RANGE_PX
    }

    fun relativeAxisScrollDelta(value: Float): Float? {
        if (!value.isFinite() || abs(value) < RELATIVE_AXIS_DEAD_ZONE) return null
        return value * RELATIVE_AXIS_SCROLL_STEP_PX
    }

    private companion object {
        private const val ABSOLUTE_AXIS_TIMEOUT_MS = 350L
        private const val ABSOLUTE_AXIS_DEAD_ZONE = 0.002f
        private const val ABSOLUTE_AXIS_SCROLL_RANGE_PX = 720f
        private const val RELATIVE_AXIS_DEAD_ZONE = 0.001f
        private const val RELATIVE_AXIS_SCROLL_STEP_PX = 160f
    }
}

internal fun isHudAccessoryTapKey(keyCode: Int): Boolean =
    keyCode in
        setOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        )

internal fun shouldDispatchHudAccessoryTapToPresentation(
    keyCode: Int,
    customMediaKeyCode: Int? = null,
): Boolean =
    isHudAccessoryTapKey(keyCode) &&
        keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
        keyCode != customMediaKeyCode
