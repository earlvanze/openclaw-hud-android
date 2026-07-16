package ai.openclaw.app

data class AirVisionSpatialMirrorFallback(
    val cursorFollowAvailable: Boolean,
    val centerCursorAvailable: Boolean,
    val threeDofAvailable: Boolean,
    val unityMirrorWindowAvailable: Boolean,
    val androidMirrorFallback: String,
    val androidMirrorFallbackActions: List<String>,
    val androidMirrorFallbackLaunchActions: List<AirVisionSpatialMirrorLaunchAction>,
    val distanceHotkeyMapped: Boolean,
    val hardwareTouchpadPassthrough: Boolean,
    val summary: String,
    val limitations: List<String>,
) {
    companion object {
        fun from(controls: AirVisionHudControls): AirVisionSpatialMirrorFallback {
            val distanceHotkeyMapped = controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance
            return AirVisionSpatialMirrorFallback(
                cursorFollowAvailable = false,
                centerCursorAvailable = false,
                threeDofAvailable = false,
                unityMirrorWindowAvailable = false,
                androidMirrorFallback = "Use Android/DeX screen sharing outside OpenClaw HUD; the ASUS Unity mirror window is Windows-only.",
                androidMirrorFallbackActions =
                    listOf(
                        "Open Android Cast settings from AirVision M1 settings.",
                        "Open Android Display settings from AirVision M1 settings.",
                        "Use Samsung DeX or Android screen sharing outside OpenClaw HUD when a projected-glasses-view mirror is needed.",
                    ),
                androidMirrorFallbackLaunchActions =
                    listOf(
                        AirVisionSpatialMirrorLaunchAction(
                            id = "android_cast_settings",
                            label = "Cast",
                            androidIntentAction = "android.settings.CAST_SETTINGS",
                            fallbackIntentAction = "android.settings.DISPLAY_SETTINGS",
                            summary = "Opens Android Cast settings, falling back to Display settings when Cast settings are unavailable.",
                        ),
                        AirVisionSpatialMirrorLaunchAction(
                            id = "android_display_settings",
                            label = "Display",
                            androidIntentAction = "android.settings.DISPLAY_SETTINGS",
                            fallbackIntentAction = null,
                            summary = "Opens Android Display settings for DeX/external-display and screen-sharing fallback setup.",
                        ),
                    ),
                distanceHotkeyMapped = distanceHotkeyMapped,
                hardwareTouchpadPassthrough = true,
                summary =
                    if (distanceHotkeyMapped) {
                        "Android maps virtual-distance adjustment to M1 brightness key events; Windows cursor-follow, center-cursor, Unity mirror window, and 3DoF remain unavailable on Android."
                    } else {
                        "Windows cursor-follow, center-cursor, Unity mirror window, and 3DoF remain unavailable on Android; M1 touchpad brightness/media behavior can still pass through firmware."
                    },
                limitations =
                    listOf(
                        "Cursor Follow requires Windows AirVision line-of-sight cursor control.",
                        "Center Cursor requires Windows virtual-screen cursor ownership.",
                        "The ASUS Unity mirror window requires the Windows AirVision app shortcut.",
                        "ASUS documents 3DoF support as Windows laptop only; phones do not support it.",
                        "M1 firmware can keep touchpad brightness swipe before Android receives a gesture event.",
                    ),
            )
        }

        fun from(controls: AirVisionBackupHudControls): AirVisionSpatialMirrorFallback =
            from(
                AirVisionHudControls(
                    singleTapAction = AirVisionHudTouchAction.fromRawValue(controls.singleTapAction),
                    doubleTapAction = AirVisionHudDoubleTapAction.fromRawValue(controls.doubleTapAction),
                    swipeAction = AirVisionHudSwipeAction.fromRawValue(controls.swipeAction),
                    horizontalSwipeAction =
                        AirVisionHudHorizontalSwipeAction.fromRawValue(controls.horizontalSwipeAction),
                    brightnessKeyAction = AirVisionHudKeyAction.fromRawValue(controls.brightnessKeyAction),
                    mediaKeyAction = AirVisionHudMediaKeyAction.fromRawValue(controls.mediaKeyAction),
                ),
            )
    }
}

data class AirVisionSpatialMirrorLaunchAction(
    val id: String,
    val label: String,
    val androidIntentAction: String,
    val fallbackIntentAction: String?,
    val summary: String,
)
