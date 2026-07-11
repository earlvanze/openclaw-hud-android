package ai.openclaw.app

import kotlinx.serialization.Serializable

@Serializable
data class AirVisionDiagnosticsCompanionParity(
    val reviewableOfflineCount: Int,
    val m1OptionalCount: Int,
    val firmwareGatedCount: Int,
    val windowsOnlyCount: Int,
    val liveM1RequiredCount: Int,
    val playReviewableOfflineCount: Int,
    val entries: List<AirVisionDiagnosticsCompanionParityEntry>,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsCompanionParityEntry(
    val feature: String,
    val androidState: String,
    val reviewPath: String,
    val evidence: String,
    val liveM1Required: Boolean,
    val firmwareProtocolRequired: Boolean,
    val playReviewableOffline: Boolean,
)

object AirVisionCompanionParity {
    private const val REVIEWABLE_OFFLINE = "reviewable_offline"
    private const val M1_OPTIONAL = "m1_optional"
    private const val FIRMWARE_GATED = "firmware_gated"
    private const val WINDOWS_ONLY = "windows_only"

    fun fromState(
        hudControls: AirVisionHudControls,
        nativeCaptionsEnabled: Boolean,
        translationCaptionSourceLanguage: String,
        translationCaptionTargetLanguage: String,
    ): AirVisionDiagnosticsCompanionParity =
        build(
            distanceHotkeyMapped = hudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance,
            nativeCaptionsEnabled = nativeCaptionsEnabled,
            translationCaptionSourceLanguage = translationCaptionSourceLanguage,
            translationCaptionTargetLanguage = translationCaptionTargetLanguage,
        )

    fun fromBackup(
        hudControls: AirVisionBackupHudControls,
        appPreferences: AirVisionBackupAppPreferences,
    ): AirVisionDiagnosticsCompanionParity =
        build(
            distanceHotkeyMapped =
                AirVisionHudKeyAction.fromRawValue(hudControls.brightnessKeyAction) ==
                    AirVisionHudKeyAction.AdjustDistance,
            nativeCaptionsEnabled = appPreferences.nativeCaptionsEnabled,
            translationCaptionSourceLanguage = appPreferences.translationCaptionSourceLanguage,
            translationCaptionTargetLanguage = appPreferences.translationCaptionTargetLanguage,
        )

    private fun build(
        distanceHotkeyMapped: Boolean,
        nativeCaptionsEnabled: Boolean,
        translationCaptionSourceLanguage: String,
        translationCaptionTargetLanguage: String,
    ): AirVisionDiagnosticsCompanionParity {
        val sourceLanguage = languageLabel(translationCaptionSourceLanguage, TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE)
        val targetLanguage = languageLabel(translationCaptionTargetLanguage, TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE)
        val entries =
            listOf(
                entry(
                    feature = "HUD presentation and DeX display targeting",
                    androidState = REVIEWABLE_OFFLINE,
                    reviewPath = "Enable Demo Mode, open the HUD, and inspect Settings > AirVision M1 > Display Target.",
                    evidence = "Android owns the green-on-black HUD presentation, display fallback, safe area, placement, and DeX-friendly launch state.",
                    liveM1Required = false,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "Windows-like profile controls",
                    androidState = REVIEWABLE_OFFLINE,
                    reviewPath = "Settings > AirVision M1 profile controls, profile backup/import, and Windows App Handoff export.",
                    evidence = "Android stores profile slots for view mode, brightness, distance, IPD, Splendid, Eye Care, Motion Sync, Light Load, 3D, HUD scale, placement, and safe area.",
                    liveM1Required = false,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "Captions and translation",
                    androidState = REVIEWABLE_OFFLINE,
                    reviewPath = "Settings > AirVision M1 > App Preferences > Samsung/native captions and Translation captions.",
                    evidence =
                        "Native captions preference is ${onOff(nativeCaptionsEnabled)}; OpenClaw translation captions are " +
                            "$sourceLanguage -> $targetLanguage.",
                    liveM1Required = false,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "USB firmware-link diagnostics",
                    androidState = M1_OPTIONAL,
                    reviewPath = "Settings > AirVision M1 > Firmware Link > Export diagnostics.",
                    evidence = "Diagnostics are exportable without a live M1; live USB descriptors and report paths populate when the M1 is connected and permission is granted.",
                    liveM1Required = true,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "Firmware apply and update",
                    androidState = FIRMWARE_GATED,
                    reviewPath = "Settings > AirVision M1 > Firmware Updates and Firmware Capture Results.",
                    evidence = "Android firmware writes and ASUS update checks remain blocked until sanitized Windows protocol evidence is validated and live M1 write/readback testing passes.",
                    liveM1Required = true,
                    firmwareProtocolRequired = true,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "Cursor Follow, Center Cursor, and 3DoF",
                    androidState = WINDOWS_ONLY,
                    reviewPath = "Settings > AirVision M1 > Windows-only spatial controls and Windows App Handoff export.",
                    evidence =
                        if (distanceHotkeyMapped) {
                            "Android maps the distance hotkey concept to M1 brightness-key virtual-distance changes, but Windows virtual-cursor and 3DoF control remain unavailable."
                        } else {
                            "Android reports these as Windows-only and leaves M1 brightness/media touch behavior available as hardware passthrough."
                        },
                    liveM1Required = false,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
                entry(
                    feature = "Unity mirror window / projected glasses view",
                    androidState = WINDOWS_ONLY,
                    reviewPath = "Settings > AirVision M1 > Cast, Display, and Windows App Handoff mirror fallback guidance.",
                    evidence = "The ASUS Unity mirror window and Ctrl+Alt+E shortcut are Windows-only; Android offers Cast, Display settings, and DeX screen-sharing fallback guidance outside the HUD.",
                    liveM1Required = false,
                    firmwareProtocolRequired = false,
                    playReviewableOffline = true,
                ),
            )
        val reviewableOfflineCount = entries.count { it.androidState == REVIEWABLE_OFFLINE }
        val m1OptionalCount = entries.count { it.androidState == M1_OPTIONAL }
        val firmwareGatedCount = entries.count { it.androidState == FIRMWARE_GATED }
        val windowsOnlyCount = entries.count { it.androidState == WINDOWS_ONLY }
        return AirVisionDiagnosticsCompanionParity(
            reviewableOfflineCount = reviewableOfflineCount,
            m1OptionalCount = m1OptionalCount,
            firmwareGatedCount = firmwareGatedCount,
            windowsOnlyCount = windowsOnlyCount,
            liveM1RequiredCount = entries.count { it.liveM1Required },
            playReviewableOfflineCount = entries.count { it.playReviewableOffline },
            entries = entries,
            summary =
                "AirVision companion parity: $reviewableOfflineCount offline-reviewable, " +
                    "$m1OptionalCount M1-optional, $firmwareGatedCount firmware-gated, " +
                    "$windowsOnlyCount Windows-only",
        )
    }

    private fun entry(
        feature: String,
        androidState: String,
        reviewPath: String,
        evidence: String,
        liveM1Required: Boolean,
        firmwareProtocolRequired: Boolean,
        playReviewableOffline: Boolean,
    ): AirVisionDiagnosticsCompanionParityEntry =
        AirVisionDiagnosticsCompanionParityEntry(
            feature = feature,
            androidState = androidState,
            reviewPath = reviewPath,
            evidence = evidence,
            liveM1Required = liveM1Required,
            firmwareProtocolRequired = firmwareProtocolRequired,
            playReviewableOffline = playReviewableOffline,
        )

    private fun languageLabel(
        languageCode: String,
        fallbackLanguageCode: String,
    ): String =
        TranslationCaptionMode
            .languageFor(TranslationCaptionMode.normalizeLanguageCode(languageCode, fallbackLanguageCode))
            .label

    private fun onOff(value: Boolean): String = if (value) "on" else "off"
}
