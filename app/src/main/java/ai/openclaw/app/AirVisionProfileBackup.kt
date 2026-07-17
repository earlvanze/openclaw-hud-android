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
    val horizontalSwipeAction: String = AirVisionHudHorizontalSwipeAction.BrowseNotifications.rawValue,
    val brightnessKeyAction: String,
    val mediaKeyAction: String,
    val mediaDoubleTapWindow: String = ExternalHudDoubleTapWindow.Extended.rawValue,
    val customMediaKeyCode: Int? = null,
)

@Serializable
data class AirVisionBackupAppPreferences(
    val language: String,
    val startupDestination: String,
    val hudDisplayTarget: String,
    val rememberedHudDisplay: AirVisionBackupDisplayFingerprint? = null,
    val demoModeEnabled: Boolean,
    val speakerEnabled: Boolean = true,
    val nativeCaptionsEnabled: Boolean = false,
    val translationCaptionSourceLanguage: String = TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
    val translationCaptionTargetLanguage: String = TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
)

@Serializable
data class AirVisionBackupDisplayFingerprint(
    val name: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
)

@Serializable
data class AirVisionBackupDisplayProfile(
    val viewMode: String,
    val splendidMode: String,
    val hudPlacement: String,
    val hudFrameShape: String = "",
    val brightnessPercent: Int,
    val blueLightFilterPercent: Int,
    val distanceCm: Int,
    val hudScalePercent: Int = AirVisionDisplaySettings.DEFAULT_HUD_SCALE_PERCENT,
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
    val hudFrameShape: String = "",
    val ipdAdjustmentEnabled: Boolean,
    val threeDModeAvailable: Boolean,
    val blueLightFilterAvailable: Boolean,
    val hudTranscriptEntryCount: Int,
    val hudCaptionEntryCount: Int,
    val effectiveHudScalePercent: Int = AirVisionDisplaySettings.DEFAULT_HUD_SCALE_PERCENT,
    val colorPreviewOverlaysEnabled: Boolean,
    val brightnessDimmingEnabled: Boolean,
)

object AirVisionProfileBackups {
    const val SCHEMA = "openclaw.airvision.m1.profile-backup"
    const val VERSION = 8
    private val SUPPORTED_VERSIONS = setOf(1, 2, 3, 4, 5, 6, 7, VERSION)

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

    fun resolve(raw: String): AirVisionResolvedProfileBackup {
        val backup = decode(raw)
        val activeViewMode = requireViewMode(backup.activeViewMode)
        val resolvedProfiles = backup.profiles.map(::settingsFromProfile)
        val duplicatedModes =
            resolvedProfiles
                .groupingBy { it.viewMode }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicatedModes.isEmpty()) {
            "Profile backup includes duplicate profiles: ${duplicatedModes.joinToString { it.label }}."
        }
        val profileByMode = resolvedProfiles.associateBy { it.viewMode }
        val missingModes = AirVisionViewMode.entries.filterNot { profileByMode.containsKey(it) }
        require(missingModes.isEmpty()) {
            "Profile backup is missing: ${missingModes.joinToString { it.label }}."
        }
        val activeSettings = checkNotNull(profileByMode[activeViewMode])

        return AirVisionResolvedProfileBackup(
            backup = backup,
            activeViewMode = activeViewMode,
            activeSettings = activeSettings,
            profileByMode = profileByMode,
            labels = labelsFromBackup(backup.customLabels),
            controls = controlsFromBackup(backup.hudControls),
            appPreferences = appPreferencesFromBackup(backup.appPreferences),
        )
    }

    fun preview(raw: String): AirVisionProfileBackupPreview {
        val resolved = resolve(raw)
        val active = resolved.activeSettings
        val preferences = resolved.appPreferences
        val labels = resolved.labels
        val controls = resolved.controls
        val runtime = runtimeProfileFromSettings(active)
        val runtimeByMode = resolved.profileByMode.mapValues { (_, settings) -> runtimeProfileFromSettings(settings) }
        val importedRuntimeByMode =
            resolved.backup.runtimeProfiles.associateBy { it.viewMode.trim().lowercase() }
        val sourceLanguage =
            TranslationCaptionMode.languageFor(preferences.translationCaptionSourceLanguage)
        val targetLanguage =
            TranslationCaptionMode.languageFor(preferences.translationCaptionTargetLanguage)
        val profileLabels =
            AirVisionViewMode.entries.joinToString {
                labels.labelFor(it)
            }
        val details =
            listOf(
                "Version ${resolved.backup.version}; ${resolved.profileByMode.size} profiles: $profileLabels",
                "Active: ${labels.labelFor(resolved.activeViewMode)}",
                "Brightness ${active.brightnessPercent}%, distance ${active.distanceCm} cm, HUD scale ${active.hudScalePercent}%, IPD ${active.ipdMm} mm",
                "Splendid ${active.splendidMode.label}, Eye Care ${active.blueLightFilterPercent}%, " +
                    "${active.hudPlacement.label}, ${active.hudFrameShape.label} frame",
                "Single tap ${controls.singleTapAction.label}; double tap ${controls.doubleTapAction.label}; " +
                    "vertical swipe ${controls.swipeAction.label}; horizontal swipe ${controls.horizontalSwipeAction.label}",
                "Brightness key ${controls.brightnessKeyAction.label}; media key ${controls.mediaKeyAction.label}; " +
                    "double-tap window ${controls.mediaDoubleTapWindow.label}; " +
                    "custom mic key ${externalHudKeyLabel(controls.customMediaKeyCode)}",
                "Runtime effective HUD scale ${runtime.effectiveHudScalePercent}%, " +
                    "transcript ${runtime.hudTranscriptEntryCount}, captions ${runtime.hudCaptionEntryCount}",
                "Runtime overlays ${enabledDisabled(runtime.colorPreviewOverlaysEnabled)}; " +
                    "brightness dimming ${enabledDisabled(runtime.brightnessDimmingEnabled)}",
                *runtimeSummaryDetails(
                    modes = AirVisionViewMode.entries,
                    labels = labels,
                    runtimeByMode = runtimeByMode,
                ).toTypedArray(),
                "Startup ${preferences.startupDestination.label}; display target ${preferences.hudDisplayTarget.label}; " +
                    "remembered display ${preferences.rememberedHudDisplay?.label() ?: "none"}; " +
                    "language ${preferences.language.label}",
                "Speaker ${enabledDisabled(preferences.speakerEnabled)}; " +
                    "Samsung/native captions ${enabledDisabled(preferences.nativeCaptionsEnabled)}; " +
                    "translation captions ${sourceLanguage.label} -> ${targetLanguage.label}",
            )
        val warnings =
            buildList {
                if (active.lightLoadModeEnabled) {
                    add("Light Load is enabled; IPD and 3D controls stay locked for this active profile.")
                }
                if (resolved.backup.runtimeProfiles.isEmpty()) {
                    add(
                        "Runtime metadata is missing; HUD runtime behavior will be recalculated from " +
                            "profile values.",
                    )
                } else {
                    val missingRuntimeModes =
                        AirVisionViewMode.entries.filter { mode ->
                            importedRuntimeByMode[mode.rawValue] == null
                        }
                    val staleRuntimeModes =
                        AirVisionViewMode.entries.filter { mode ->
                            val importedRuntime = importedRuntimeByMode[mode.rawValue]
                            val derivedRuntime = runtimeByMode[mode]
                            importedRuntime != null && derivedRuntime != null && !runtimeMatches(importedRuntime, derivedRuntime)
                        }
                    if (missingRuntimeModes.isNotEmpty()) {
                        add(
                            "Runtime metadata is missing for " +
                                missingRuntimeModes.joinToString { labels.labelFor(it) } +
                                "; HUD runtime behavior will be recalculated from profile values.",
                        )
                    }
                    if (staleRuntimeModes.isNotEmpty()) {
                        add(
                            "Runtime metadata is stale for " +
                                staleRuntimeModes.joinToString { labels.labelFor(it) } +
                                "; HUD runtime behavior will be recalculated from profile values.",
                        )
                    }
                }
                if (!preferences.speakerEnabled) {
                    add("Speaker is disabled in this backup.")
                }
                if (preferences.demoModeEnabled) {
                    add("Demo Mode is enabled in this backup.")
                }
            }
        return AirVisionProfileBackupPreview(
            title = "Apply ${labels.labelFor(resolved.activeViewMode)} AirVision profile backup?",
            details = details,
            warnings = warnings,
        )
    }

    fun profileFromSettings(settings: AirVisionDisplaySettings): AirVisionBackupDisplayProfile =
        AirVisionBackupDisplayProfile(
            viewMode = settings.viewMode.rawValue,
            splendidMode = settings.splendidMode.rawValue,
            hudPlacement = settings.hudPlacement.rawValue,
            hudFrameShape = settings.hudFrameShape.rawValue,
            brightnessPercent = settings.brightnessPercent,
            blueLightFilterPercent = settings.blueLightFilterPercent,
            distanceCm = settings.distanceCm,
            hudScalePercent = settings.hudScalePercent,
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
            hudFrameShape = settings.hudFrameShape.rawValue,
            ipdAdjustmentEnabled = settings.ipdAdjustmentEnabled,
            threeDModeAvailable = settings.threeDModeAvailable,
            blueLightFilterAvailable = settings.blueLightFilterAvailable,
            hudTranscriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(settings.lightLoadModeEnabled),
            hudCaptionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(settings.lightLoadModeEnabled),
            effectiveHudScalePercent =
                (
                    AirVisionDisplaySettings.hudScaleForDistanceCm(settings.distanceCm) *
                        AirVisionDisplaySettings.hudScaleMultiplierForViewMode(settings.viewMode) *
                        AirVisionDisplaySettings.hudScaleMultiplierForPercent(settings.hudScalePercent) *
                        100f
                ).toInt(),
            colorPreviewOverlaysEnabled =
                AirVisionDisplaySettings.hudColorPreviewAlpha(
                    alpha = 1f,
                    lightLoadModeEnabled = settings.lightLoadModeEnabled,
                ) > 0f,
            brightnessDimmingEnabled =
                AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(settings.brightnessPercent) > 0f,
        )

    fun settingsFromProfile(profile: AirVisionBackupDisplayProfile): AirVisionDisplaySettings {
        val viewMode = requireViewMode(profile.viewMode)
        val defaultFrameShape = AirVisionDisplaySettings.defaultsForViewMode(viewMode).hudFrameShape
        return AirVisionDisplaySettings(
            viewMode = viewMode,
            splendidMode = requireSplendidMode(profile.splendidMode),
            hudPlacement = requireHudPlacement(profile.hudPlacement),
            hudFrameShape = requireHudFrameShape(profile.hudFrameShape, defaultFrameShape),
            brightnessPercent = profile.brightnessPercent,
            blueLightFilterPercent = profile.blueLightFilterPercent,
            distanceCm = profile.distanceCm,
            hudScalePercent = profile.hudScalePercent,
            ipdMm = profile.ipdMm,
            safeAreaPercent = profile.safeAreaPercent,
            physicalMainScreenVisible = profile.physicalMainScreenVisible,
            motionSyncEnabled = profile.motionSyncEnabled,
            threeDModeEnabled = profile.threeDModeEnabled,
            lightLoadModeEnabled = profile.lightLoadModeEnabled,
        ).normalized
    }

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
            horizontalSwipeAction = requireHorizontalSwipeAction(controls.horizontalSwipeAction),
            brightnessKeyAction = requireBrightnessKeyAction(controls.brightnessKeyAction),
            mediaKeyAction = requireMediaKeyAction(controls.mediaKeyAction),
            mediaDoubleTapWindow = requireMediaDoubleTapWindow(controls.mediaDoubleTapWindow),
            customMediaKeyCode = requireExternalHudKeyCode(controls.customMediaKeyCode),
        )

    fun appPreferencesFromBackup(preferences: AirVisionBackupAppPreferences): AirVisionBackupResolvedAppPreferences {
        val displayTarget = requireHudDisplayTarget(preferences.hudDisplayTarget)
        val rememberedDisplay =
            preferences.rememberedHudDisplay?.let {
                AirVisionHudDisplayFingerprint(
                    name = it.name.trim(),
                    widthPx = it.widthPx.coerceAtLeast(0),
                    heightPx = it.heightPx.coerceAtLeast(0),
                ).takeIf { display -> display.isConfigured }
            }
        require(displayTarget != AirVisionHudDisplayTarget.RememberedExternal || rememberedDisplay != null) {
            "Remembered HUD display target requires a display fingerprint."
        }
        return AirVisionBackupResolvedAppPreferences(
            language = requireAppLanguage(preferences.language),
            startupDestination = requireStartupDestination(preferences.startupDestination),
            hudDisplayTarget = displayTarget,
            rememberedHudDisplay = rememberedDisplay,
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
    }

    fun requireViewMode(rawValue: String): AirVisionViewMode =
        AirVisionViewMode.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision view mode: $rawValue")

    private fun requireSplendidMode(rawValue: String): AirVisionSplendidMode =
        AirVisionSplendidMode.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision Splendid mode: $rawValue")

    private fun requireHudPlacement(rawValue: String): AirVisionHudPlacement =
        AirVisionHudPlacement.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision HUD placement: $rawValue")

    private fun requireHudFrameShape(
        rawValue: String,
        fallback: AirVisionHudFrameShape,
    ): AirVisionHudFrameShape {
        if (rawValue.isBlank()) return fallback
        return AirVisionHudFrameShape.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported HUD frame shape: $rawValue")
    }

    private fun requireSingleTapAction(rawValue: String): AirVisionHudTouchAction =
        AirVisionHudTouchAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision single-tap action: $rawValue")

    private fun requireDoubleTapAction(rawValue: String): AirVisionHudDoubleTapAction =
        AirVisionHudDoubleTapAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision double-tap action: $rawValue")

    private fun requireSwipeAction(rawValue: String): AirVisionHudSwipeAction =
        AirVisionHudSwipeAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision swipe action: $rawValue")

    private fun requireHorizontalSwipeAction(rawValue: String): AirVisionHudHorizontalSwipeAction =
        AirVisionHudHorizontalSwipeAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision horizontal swipe action: $rawValue")

    private fun requireBrightnessKeyAction(rawValue: String): AirVisionHudKeyAction =
        AirVisionHudKeyAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision brightness-key action: $rawValue")

    private fun requireMediaKeyAction(rawValue: String): AirVisionHudMediaKeyAction =
        AirVisionHudMediaKeyAction.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision media-key action: $rawValue")

    private fun requireMediaDoubleTapWindow(rawValue: String): ExternalHudDoubleTapWindow =
        ExternalHudDoubleTapWindow.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported external HUD double-tap window: $rawValue")

    private fun requireExternalHudKeyCode(keyCode: Int?): Int? {
        require(keyCode == null || normalizeExternalHudKeyCode(keyCode) == keyCode) {
            "Unsupported external HUD custom media key code: $keyCode"
        }
        return keyCode
    }

    private fun requireAppLanguage(rawValue: String): AirVisionAppLanguage =
        AirVisionAppLanguage.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision language: $rawValue")

    private fun requireStartupDestination(rawValue: String): AirVisionStartupDestination =
        AirVisionStartupDestination.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision startup view: $rawValue")

    private fun requireHudDisplayTarget(rawValue: String): AirVisionHudDisplayTarget =
        AirVisionHudDisplayTarget.entries.firstOrNull { it.rawValue == rawValue.trim().lowercase() }
            ?: throw IllegalArgumentException("Unsupported AirVision HUD display target: $rawValue")

    private fun enabledDisabled(value: Boolean): String = if (value) "enabled" else "disabled"

    private fun runtimeSummaryDetails(
        modes: List<AirVisionViewMode>,
        labels: AirVisionCustomProfileLabels,
        runtimeByMode: Map<AirVisionViewMode, AirVisionBackupRuntimeProfile>,
    ): List<String> =
        modes.mapNotNull { mode ->
            val runtime = runtimeByMode[mode] ?: return@mapNotNull null
            "Runtime ${labels.labelFor(mode)}: effective HUD scale ${runtime.effectiveHudScalePercent}%, " +
                "${runtime.hudFrameShape.ifBlank { "mode default" }} frame, " +
                "transcript ${runtime.hudTranscriptEntryCount}, captions ${runtime.hudCaptionEntryCount}, " +
                "overlays ${enabledDisabled(runtime.colorPreviewOverlaysEnabled)}, " +
                "dimming ${enabledDisabled(runtime.brightnessDimmingEnabled)}"
        }

    private fun runtimeMatches(
        imported: AirVisionBackupRuntimeProfile,
        derived: AirVisionBackupRuntimeProfile,
    ): Boolean =
        imported.viewMode.trim().lowercase() == derived.viewMode &&
            (imported.hudFrameShape.isBlank() || imported.hudFrameShape == derived.hudFrameShape) &&
            imported.ipdAdjustmentEnabled == derived.ipdAdjustmentEnabled &&
            imported.threeDModeAvailable == derived.threeDModeAvailable &&
            imported.blueLightFilterAvailable == derived.blueLightFilterAvailable &&
            imported.hudTranscriptEntryCount == derived.hudTranscriptEntryCount &&
            imported.hudCaptionEntryCount == derived.hudCaptionEntryCount &&
            imported.effectiveHudScalePercent == derived.effectiveHudScalePercent &&
            imported.colorPreviewOverlaysEnabled == derived.colorPreviewOverlaysEnabled &&
            imported.brightnessDimmingEnabled == derived.brightnessDimmingEnabled
}

data class AirVisionBackupResolvedAppPreferences(
    val language: AirVisionAppLanguage,
    val startupDestination: AirVisionStartupDestination,
    val hudDisplayTarget: AirVisionHudDisplayTarget,
    val rememberedHudDisplay: AirVisionHudDisplayFingerprint?,
    val demoModeEnabled: Boolean,
    val speakerEnabled: Boolean,
    val nativeCaptionsEnabled: Boolean,
    val translationCaptionSourceLanguage: String,
    val translationCaptionTargetLanguage: String,
)

data class AirVisionResolvedProfileBackup(
    val backup: AirVisionProfileBackup,
    val activeViewMode: AirVisionViewMode,
    val activeSettings: AirVisionDisplaySettings,
    val profileByMode: Map<AirVisionViewMode, AirVisionDisplaySettings>,
    val labels: AirVisionCustomProfileLabels,
    val controls: AirVisionHudControls,
    val appPreferences: AirVisionBackupResolvedAppPreferences,
)

data class AirVisionProfileBackupPreview(
    val title: String,
    val details: List<String>,
    val warnings: List<String>,
)
