package ai.openclaw.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process

internal enum class ExternalHudCompanionAction(
    val label: String,
) {
    SingleTap("Single tap"),
    DoubleTap("Double tap"),
    SwipeForward("Swipe forward"),
    SwipeBackward("Swipe backward"),
    ObserveOnly("Observe only"),
}

internal data class ExternalHudCompanionInput(
    val event: String,
    val phase: String,
    val rawTouch: Int,
    val slide: Int,
    val action: ExternalHudCompanionAction,
)

internal fun parseExternalHudCompanionInput(
    event: String?,
    phase: String?,
    rawTouch: Int,
    slide: Int,
): ExternalHudCompanionInput? {
    val normalizedEvent = event?.trim()?.lowercase()?.takeIf { it in EXTERNAL_HUD_COMPANION_EVENTS } ?: return null
    val normalizedPhase = phase?.trim()?.lowercase()?.takeIf { it in EXTERNAL_HUD_COMPANION_PHASES } ?: return null
    if (rawTouch !in 0..255 || slide !in 0..255) return null
    if (normalizedPhase == "release" && (rawTouch != 6 || normalizedEvent !in EXTERNAL_HUD_RELEASE_SLIDE_EVENTS)) {
        return null
    }

    val action =
        when (normalizedEvent) {
            "one_click", "two_finger_click" -> ExternalHudCompanionAction.SingleTap
            "double_click" -> ExternalHudCompanionAction.DoubleTap
            "slide_forward", "long_press_slide_forward" -> ExternalHudCompanionAction.SwipeForward
            "slide_backward", "long_press_slide_backward" -> ExternalHudCompanionAction.SwipeBackward
            else -> ExternalHudCompanionAction.ObserveOnly
        }
    return ExternalHudCompanionInput(
        event = normalizedEvent,
        phase = normalizedPhase,
        rawTouch = rawTouch,
        slide = slide,
        action = action,
    )
}

internal fun parseExternalHudCompanionInput(intent: Intent): ExternalHudCompanionInput? {
    if (intent.action != EXTERNAL_HUD_COMPANION_ACTION) return null
    return parseExternalHudCompanionInput(
        event = intent.getStringExtra(EXTERNAL_HUD_COMPANION_EVENT),
        phase = intent.getStringExtra(EXTERNAL_HUD_COMPANION_PHASE),
        rawTouch = intent.getIntExtra(EXTERNAL_HUD_COMPANION_RAW_TOUCH, -1),
        slide = intent.getIntExtra(EXTERNAL_HUD_COMPANION_SLIDE, -1),
    )
}

internal fun isTrustedExternalHudCompanionSender(
    context: Context,
    receiver: BroadcastReceiver,
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    val senderPackage = receiver.sentFromPackage ?: return false
    val senderUid = receiver.sentFromUid
    if (senderPackage != EXTERNAL_HUD_COMPANION_PACKAGE || senderUid == Process.INVALID_UID) return false
    return context.packageManager.getPackagesForUid(senderUid)?.contains(EXTERNAL_HUD_COMPANION_PACKAGE) == true
}

internal const val EXTERNAL_HUD_COMPANION_PACKAGE = "com.ecosystems.airvision"
internal const val EXTERNAL_HUD_COMPANION_ACTION = "ai.openclaw.app.hud.action.COMPANION_INPUT"
private const val EXTERNAL_HUD_COMPANION_EVENT = "event"
private const val EXTERNAL_HUD_COMPANION_PHASE = "phase"
private const val EXTERNAL_HUD_COMPANION_RAW_TOUCH = "raw_touch"
private const val EXTERNAL_HUD_COMPANION_SLIDE = "slide"
private val EXTERNAL_HUD_COMPANION_PHASES = setOf("press", "release", "update")
private val EXTERNAL_HUD_RELEASE_SLIDE_EVENTS = setOf("slide_forward", "slide_backward")
private val EXTERNAL_HUD_COMPANION_EVENTS =
    setOf(
        "one_click",
        "double_click",
        "long_click",
        "two_finger_click",
        "two_finger_long_click",
        "slide_forward",
        "slide_backward",
        "long_press_slide_forward",
        "long_press_slide_backward",
        "long_press_slide_release",
    )
