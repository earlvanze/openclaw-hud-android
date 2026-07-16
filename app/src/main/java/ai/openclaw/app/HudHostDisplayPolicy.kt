package ai.openclaw.app

internal fun shouldRelaunchHudHostOnDefaultDisplay(
    openClawDefaultHud: Boolean,
    activityOnExternalDisplay: Boolean,
    relaunchAlreadyAttempted: Boolean,
): Boolean = openClawDefaultHud && activityOnExternalDisplay && !relaunchAlreadyAttempted
