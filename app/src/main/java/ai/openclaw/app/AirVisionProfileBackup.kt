package ai.openclaw.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AirVisionProfileBackup(
    val schema: String = AirVisionProfileBackups.SCHEMA,
    val version: Int = AirVisionProfileBackups.VERSION,
    val activeViewMode: String,
    val customLabels: AirVisionBackupCustomLabels,
    val hudControls: AirVisionBackupHudControls,
    val appPreferences: AirVisionBackupAppPreferences,
    val runtimeProfiles: List<AirVisionBackupRuntimeProfile> = emptyList(),
    val profiles: List<AirVisionBackupDisplayProfile>,
)

@Serializable
data class AirVisionBackupCustomLabels(
    val custom1: String,
    val custom2: String,
)

@Serializable
data class AirVisionBackupHudControls(
    val singleTapAction: String,
    val doubleTapAction: String,
    val swipeAction: String,
    val brightnessKeyAction: String,
    val mediaKeyAction: String,
)

@Serializable
data class AirVisionBackupAppPreferences(
    val language: String,
    val startupDestination: String,
    val hudDisplayTarget: String,
    val demoModeEnabled: Boolean,
    val speakerEnabled: Boolean = true,
    val nativeCaptionsEnabled: Boolean = false,
    val translationCaptionSourceLanguage: String = TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
    val translationCaptionTargetLanguage: String = TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
)

@Serializable
data class AirVisionBackupDisplayProfile(
    val viewMode: String,
    val splendidMode: String,
    val hudPlacement: String,
    val brightnessPercent: Int,
    val blueLightFilterPercent: Int,
    val distanceCm: Int,
    val ipdMm: Int,
    val safeAreaPercent: Int,
    val physicalMainScreenVisible: Boolean,
    val motionSyncEnabled: Boolean,
    val threeDModeEnabled: Boolean,
    val lightLoadModeEnabled: Boolean,
)

@Serializable
data class AirVisionBackupRuntimeProfile(
    val viewMode: String,
    val ipdAdjustmentEnabled: Boolean,
    val threeDModeAvailable: Boolean,
    val blueLightFilterAvailable: Boolean,
    val hudTranscriptEntryCount: Int,
    val hudCaptionEntryCount: Int,
    val colorPreviewOverlaysEnabled: Boolean,
    val brightnessDimmingEnabled: Boolean,
)

object AirVisionProfileBackups {
    const val SCHEMA = "openclaw.airvision.m1.profile-backup"
    const val VERSION = 3
    private val SUPPORTED_VERSIONS = setOf(1, 2, VERSION)

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    fun encode(backup: AirVisionProfileBackup): String = json.encodeToString(backup)

    fun decode(raw: String): AirVisionProfileBackup {
        val backup =
            try {
                json.decodeFromString<AirVisionProfileBackup>(raw)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Profile backup is not valid JSON.", error)
        }

        require(backup.schema == SCHEMA) { "Profile backup schema is not supported." }
        require(backup.version in SUPPORTED_VERSIONS) { "Profile backup version is not supported." }
        require(backup.profiles.isNotEmpty()) { "Profile backup does not include any display profiles." }
        return backup
    }

    fun profileFromSettings(settings: AirVisionDisplaySettings): AirVisionBackupDisplayProfile =
        AirVisionBackupDisplayProfile(
            viewMode = settings.viewMode.rawValue,
            splendidMode = settings.splendidMode.rawValue,
            hudPlacement = settings.hudPlacement.rawValue,
            brightnessPercent = settings.brightnessPercent,
            blueLightFilterPercent = settings.blueLightFilterPercent,
            distanceCm = settings.distanceCm,
            ipdMm = settings.ipdMm,
            safeAreaPercent = settings.safeAreaPercent,
            physicalMainScreenVisible = settings.physicalMainScreenVisible,
            motionSyncEnabled = settings.motionSyncEnabled,
            threeDModeEnabled = settings.threeDModeEnabled,
            lightLoadModeEnabled = settings.lightLoadModeEnabled,
        )

    fun runtimeProfileFromSettings(settings: AirVisionDisplaySettings): AirVisionBackupRuntimeProfile =
        AirVisionBackupRuntimeProfile(
            viewMode = settings.viewMode.rawValue,
            ipdAdjustmentEnabled = settings.ipdAdjustmentEnabled,
            threeDModeAvailable = settings.threeDModeAvailable,
            blueLightFilterAvailable = settings.blueLightFilterAvailable,
            hudTranscriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(settings.lightLoadModeEnabled),
            hudCaptionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(settings.lightLoadModeEnabled),
            colorPreviewOverlaysEnabled =
                AirVisionDisplaySettings.hudColorPreviewAlpha(
                    alpha = 1f,
                    lightLoadModeEnabled = settings.lightLoadModeEnabled,
                ) > 0f,
            brightnessDimmingEnabled =
                AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(settings.brightnessPercent) > 0f,
        )

    fun settingsFromProfile(profile: AirVisionBackupDisplayProfile): AirVisionDisplaySettings =
        AirVisionDisplaySettings(
            viewMode = requireViewMode(profile.viewMode),
            splendidMode = requireSplendidMode(profile.splendidMode),
            hudPlacement = requireHudPlacement(profile.hudPlacement),
            brightnessPercent = profile.brightnessPercent,
            blueLightFilterPercent = profile.blueLightFilterPercent,
            distanceCm = profile.distanceCm,
            ipdMm = profile.ipdMm,
            safeAreaPercent = profile.safeAreaPercent,
            physicalMainScreenVisible = profile.physicalMainScreenVisible,
            motionSyncEnabled = profile.motionSyncEnabled,
            threeDModeEnabled = profile.threeDModeEnabled,
            lightLoadModeEnabled = profile.lightLoadModeEnabled,
        ).normalized

    fun labelsFromBackup(labels: AirVisionBackupCustomLabels): AirVisionCustomProfileLabels =
        AirVisionCustomProfileLabels(
            custom1 =
                AirVisionCustomProfileLabels.normalizeLabel(
                    labels.custom1,
                    AirVisionViewMode.Custom1.label,
                ),
            custom2 =
                AirVisionCustomProfileLabels.normalizeLabel(
                    labels.custom2,
                    AirVisionViewMode.Custom2.label,
                ),
        )

    fun controlsFromBackup(controls: AirVisionBackupHudControls): AirVisionHudControls =
        AirVisionHudControls(
            singleTapAction = requireSingleTapAction(controls.singleTapAction),
            doubleTapAction = requireDoubleTapAction(controls.doubleTapAction),
            swipeAction = requireSwipeAction(controls.swipeAction),
            brightnessKeyAction = requireBrightnessKeyAction(controls.brightnessKeyAction),
            mediaKeyAction = requireMediaKeyAction(controls.mediaKeyAction),
        )

    fun appPreferencesFromBackup(preferences: AirVisionBackupAppPreferences): AirVisionBackupResolvedAppPreferences =
        AirVisionBackupResolvedAppPreferences(
            language = requireAppLanguage(preferences.language),
            startupDestination = requireStartupDestination(preferences.startupDestination),
            hudDisplayTarget = requireHudDisplayTarget(preferences.hudDisplayTarget),
            demoModeEnabled = preferences.demoModeEnabled,
            speakerEnabled = preferences.speakerEnabled,
            nativeCaptionsEnabled = preferences.nativeCaptionsEnabled,
            translationCaptionSourceLanguage =
                TranslationCaptionMode.normalizeLanguageCode(
                    preferences.translationCaptionSourceLanguage,
                    TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
                ),
            translationCaptionTargetLanguage =
                TranslationCaptionMode.normalizeLanguageCode(
                    preferences.translationCaptionTargetLanguage,
                    TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
                ),
        )

    fun requireViewMode(rawValue: String): AirVisionViewMode =
        AirVisionViewMode.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision view mode: $rawValue")

    private fun requireSplendidMode(rawValue: String): AirVisionSplendidMode =
        AirVisionSplendidMode.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision Splendid mode: $rawValue")

    private fun requireHudPlacement(rawValue: String): AirVisionHudPlacement =
        AirVisionHudPlacement.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision HUD placement: $rawValue")

    private fun requireSingleTapAction(rawValue: String): AirVisionHudTouchAction =
        AirVisionHudTouchAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision single-tap action: $rawValue")

    private fun requireDoubleTapAction(rawValue: String): AirVisionHudDoubleTapAction =
        AirVisionHudDoubleTapAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision double-tap action: $rawValue")

    private fun requireSwipeAction(rawValue: String): AirVisionHudSwipeAction =
        AirVisionHudSwipeAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision swipe action: $rawValue")

    private fun requireBrightnessKeyAction(rawValue: String): AirVisionHudKeyAction =
        AirVisionHudKeyAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision brightness-key action: $rawValue")

    private fun requireMediaKeyAction(rawValue: String): AirVisionHudMediaKeyAction =
        AirVisionHudMediaKeyAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision media-key action: $rawValue")

    private fun requireAppLanguage(rawValue: String): AirVisionAppLanguage =
        AirVisionAppLanguage.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision language: $rawValue")

    private fun requireStartupDestination(rawValue: String): AirVisionStartupDestination =
        AirVisionStartupDestination.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision startup view: $rawValue")

    private fun requireHudDisplayTarget(rawValue: String): AirVisionHudDisplayTarget =
        AirVisionHudDisplayTarget.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision HUD display target: $rawValue")
}

data class AirVisionBackupResolvedAppPreferences(
    val language: AirVisionAppLanguage,
    val startupDestination: AirVisionStartupDestination,
    val hudDisplayTarget: AirVisionHudDisplayTarget,
    val demoModeEnabled: Boolean,
    val speakerEnabled: Boolean,
    val nativeCaptionsEnabled: Boolean,
    val translationCaptionSourceLanguage: String,
    val translationCaptionTargetLanguage: String,
)
