package ai.openclaw.app.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

internal enum class HudGestureMotionResult {
    Tap,
    Swipe,
    Ignore,
}

internal class HudGestureMotionTracker(
    private val touchSlop: Float,
) {
    private var total = Offset.Zero
    private var verticalSwipe = false
    private var canceled = false

    fun add(
        delta: Offset,
        consumed: Boolean,
    ): Float? {
        if (consumed || canceled) {
            canceled = true
            return null
        }

        total += delta
        if (!verticalSwipe && total.getDistance() > touchSlop) {
            if (abs(total.y) <= abs(total.x)) {
                canceled = true
                return null
            }
            verticalSwipe = true
            return -total.y
        }
        return if (verticalSwipe) -delta.y else null
    }

    fun cancel() {
        canceled = true
    }

    fun finish(consumed: Boolean): HudGestureMotionResult =
        when {
            consumed || canceled -> HudGestureMotionResult.Ignore
            verticalSwipe -> HudGestureMotionResult.Swipe
            total.getDistance() <= touchSlop -> HudGestureMotionResult.Tap
            else -> HudGestureMotionResult.Ignore
        }
}

internal fun Modifier.hudTouchGestures(
    singleTapKey: Any?,
    doubleTapKey: Any?,
    swipeKey: Any?,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeStarted: () -> Unit,
    onVerticalSwipe: (Float) -> Unit,
): Modifier =
    pointerInput(singleTapKey, doubleTapKey, swipeKey) {
        var lastTapUptimeMs = 0L
        var pendingSingleTap: Runnable? = null
        val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis
        val mainHandler = Handler(Looper.getMainLooper())

        fun armSingleTap(uptimeMs: Long) {
            lastTapUptimeMs = uptimeMs
            pendingSingleTap =
                Runnable {
                    if (lastTapUptimeMs == uptimeMs) {
                        lastTapUptimeMs = 0L
                        onSingleTap()
                    }
                }
            mainHandler.postDelayed(pendingSingleTap!!, doubleTapTimeoutMs)
        }

        try {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                val tracker = HudGestureMotionTracker(viewConfiguration.touchSlop)
                if (down.isConsumed) tracker.cancel()
                var eventUptimeMs = down.uptimeMillis
                var swipeStarted = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        tracker.cancel()
                        break
                    }

                    eventUptimeMs = change.uptimeMillis
                    val delta = change.position - change.previousPosition
                    tracker.add(delta = delta, consumed = change.isConsumed)?.let { scrollDelta ->
                        if (!swipeStarted) {
                            swipeStarted = true
                            onSwipeStarted()
                        }
                        onVerticalSwipe(scrollDelta)
                    }

                    if (!change.pressed) {
                        if (tracker.finish(consumed = change.isConsumed) == HudGestureMotionResult.Tap) {
                            val elapsedMs = eventUptimeMs - lastTapUptimeMs
                            if (lastTapUptimeMs > 0L && elapsedMs in 1..doubleTapTimeoutMs) {
                                pendingSingleTap?.let(mainHandler::removeCallbacks)
                                pendingSingleTap = null
                                lastTapUptimeMs = 0L
                                onDoubleTap()
                            } else {
                                if (lastTapUptimeMs > 0L) {
                                    pendingSingleTap?.let(mainHandler::removeCallbacks)
                                    onSingleTap()
                                }
                                armSingleTap(eventUptimeMs)
                            }
                        }
                        break
                    }
                }
            }
        } finally {
            pendingSingleTap?.let(mainHandler::removeCallbacks)
        }
    }
