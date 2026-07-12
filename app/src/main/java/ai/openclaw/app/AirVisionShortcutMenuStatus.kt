package ai.openclaw.app

data class AirVisionShortcutMenuStatus(
    val brightness: String,
    val volume: String,
    val distance: String,
    val androidMappedCount: Int,
    val firmwareOrSystemOwnedCount: Int,
    val summary: String,
) {
    companion object {
        fun from(
            controls: AirVisionHudControls,
            speakerEnabled: Boolean,
        ): AirVisionShortcutMenuStatus {
            val brightness =
                when (controls.brightnessKeyAction) {
                    AirVisionHudKeyAction.AdjustBrightness -> "Android HUD dimming, 5% per brightness-key press"
                    AirVisionHudKeyAction.ScrollChat -> "brightness keys scroll chat; panel brightness remains firmware/system owned"
                    AirVisionHudKeyAction.AdjustDistance -> "brightness keys adjust virtual distance; panel brightness remains firmware/system owned"
                    AirVisionHudKeyAction.None -> "M1 firmware or Android system handles panel brightness"
                }
            val distance =
                if (controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) {
                    "Android virtual projection distance, 5 cm per brightness-key press"
                } else {
                    "Settings slider stores virtual projection distance; ASUS shortcut-menu distance remains firmware/Windows owned"
                }
            val volume =
                if (speakerEnabled) {
                    "OpenClaw speaker/TTS output enabled; hardware volume route remains Android system or M1 firmware owned"
                } else {
                    "OpenClaw speaker/TTS output muted; hardware volume route remains Android system or M1 firmware owned"
                }
            val androidMappedCount =
                listOf(
                    controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustBrightness,
                    controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance,
                    speakerEnabled,
                ).count { it }
            val firmwareOrSystemOwnedCount = 3 - androidMappedCount
            val summary =
                "Shortcut menu parity: brightness ${controls.brightnessKeyAction.label}, " +
                    "volume ${if (speakerEnabled) "speaker on" else "speaker muted"}, " +
                    "distance ${if (controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) "mapped" else "settings-only"}; " +
                    "$androidMappedCount Android-mapped, $firmwareOrSystemOwnedCount firmware/system-owned."
            return AirVisionShortcutMenuStatus(
                brightness = brightness,
                volume = volume,
                distance = distance,
                androidMappedCount = androidMappedCount,
                firmwareOrSystemOwnedCount = firmwareOrSystemOwnedCount,
                summary = summary,
            )
        }

        fun from(
            controls: AirVisionBackupHudControls,
            preferences: AirVisionBackupAppPreferences,
        ): AirVisionShortcutMenuStatus =
            from(
                controls =
                    AirVisionHudControls(
                        singleTapAction = AirVisionHudTouchAction.fromRawValue(controls.singleTapAction),
                        doubleTapAction = AirVisionHudDoubleTapAction.fromRawValue(controls.doubleTapAction),
                        swipeAction = AirVisionHudSwipeAction.fromRawValue(controls.swipeAction),
                        brightnessKeyAction = AirVisionHudKeyAction.fromRawValue(controls.brightnessKeyAction),
                        mediaKeyAction = AirVisionHudMediaKeyAction.fromRawValue(controls.mediaKeyAction),
                    ),
                speakerEnabled = preferences.speakerEnabled,
            )
    }
}
