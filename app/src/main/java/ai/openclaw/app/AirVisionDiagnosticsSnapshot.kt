package ai.openclaw.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AirVisionDiagnosticsSnapshot(
    val schema: String = AirVisionDiagnosticsSnapshots.SCHEMA,
    val version: Int = AirVisionDiagnosticsSnapshots.VERSION,
    val usb: AirVisionDiagnosticsUsb,
    val activeProfile: AirVisionBackupDisplayProfile,
    val firmwareSync: AirVisionDiagnosticsFirmwareSyncPlan,
    val firmwareCaptureResults: AirVisionDiagnosticsFirmwareCaptureResults,
    val firmwareUpdate: AirVisionDiagnosticsFirmwareUpdate,
    val hudRuntime: AirVisionDiagnosticsHudRuntime,
    val profileBackup: AirVisionDiagnosticsProfileBackup,
    val fitAndClarity: AirVisionDiagnosticsFitAndClarity,
    val demoExperience: AirVisionDiagnosticsDemoExperience,
    val windowsCompatibility: AirVisionDiagnosticsWindowsCompatibility,
    val hudControls: AirVisionBackupHudControls,
    val appPreferences: AirVisionBackupAppPreferences,
)

@Serializable
data class AirVisionDiagnosticsUsb(
    val connected: Boolean,
    val permissionGranted: Boolean,
    val deviceLabel: String?,
    val vendorProduct: String?,
    val firmwareControlReady: Boolean,
    val hidControlInterface: Boolean,
    val audioInterface: Boolean,
    val inputInterface: Boolean,
    val statusText: String,
    val firmwareCapabilities: AirVisionDiagnosticsFirmwareCapabilities,
    val deviceInfo: AirVisionDiagnosticsDeviceInfo,
    val interfaces: List<AirVisionDiagnosticsInterface>,
)

@Serializable
data class AirVisionDiagnosticsFirmwareCapabilities(
    val hidInputInterfaceIds: List<Int>,
    val hidOutputInterfaceIds: List<Int>,
    val interruptInputEndpoints: Int,
    val interruptOutputEndpoints: Int,
    val maxInputPacketSize: Int?,
    val maxOutputPacketSize: Int?,
    val readableReportPaths: List<AirVisionDiagnosticsFirmwareReportPath>,
    val writableReportPaths: List<AirVisionDiagnosticsFirmwareReportPath>,
    val featureReadiness: List<AirVisionDiagnosticsFirmwareFeatureReadiness>,
    val captureTargets: List<AirVisionDiagnosticsFirmwareCaptureTarget>,
    val hasReadableHidReports: Boolean,
    val hasWritableHidReports: Boolean,
    val hasInterruptReportPath: Boolean,
    val protocolCaptureReady: Boolean,
    val summary: String,
    val featureReadinessSummary: String,
    val capturePlanSummary: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareReportPath(
    val interfaceId: Int,
    val endpointAddress: Int,
    val direction: Int,
    val directionLabel: String,
    val type: Int,
    val typeLabel: String,
    val maxPacketSize: Int,
    val interval: Int,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareFeatureReadiness(
    val feature: String,
    val label: String,
    val androidStatus: String,
    val firmwareApplyReady: Boolean,
    val firmwareApplyStatus: String,
    val detail: String,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareCaptureTarget(
    val feature: String,
    val label: String,
    val captureReady: Boolean,
    val writeReportPathSummaries: List<String>,
    val readReportPathSummaries: List<String>,
    val suggestedProbeValues: List<String>,
    val instruction: String,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareSyncPlan(
    val pendingHardwareSyncCount: Int,
    val androidAppliedCount: Int,
    val firmwareWriteAllowedCount: Int,
    val blockedFirmwareWriteCount: Int,
    val writeGate: AirVisionDiagnosticsFirmwareWriteGate,
    val summary: String,
    val writeGateSummary: String,
    val detailSummary: String,
    val items: List<AirVisionDiagnosticsFirmwareSyncItem>,
)

@Serializable
data class AirVisionDiagnosticsFirmwareWriteGate(
    val status: String,
    val firmwareWritesEnabled: Boolean,
    val validatedCaptureCount: Int,
    val writeEnabledCaptureCount: Int,
    val blockedFeatureCount: Int,
    val protocolReadyFeatureLabels: List<String>,
    val blockedFeatureLabels: List<String>,
    val blockedFeatureSummaries: List<String>,
    val liveTestChecklist: List<String>,
    val liveM1Required: Boolean,
    val explicitUserConfirmationRequired: Boolean,
    val summary: String,
    val nextStep: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareSyncItem(
    val feature: String,
    val label: String,
    val desiredValue: String,
    val androidApplied: Boolean,
    val androidEffect: String,
    val hardwareSyncPending: Boolean,
    val hardwareSyncStatus: String,
    val captureResultsSchema: String,
    val captureResultStatus: String,
    val androidEnablementDecision: String,
    val firmwareWriteAllowed: Boolean,
    val requiredEvidence: List<String>,
    val blockedReason: String,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareCaptureResults(
    val imported: Boolean,
    val schema: String,
    val version: Int?,
    val payloadPolicy: String?,
    val source: AirVisionDiagnosticsFirmwareCaptureResultsSource?,
    val featureCount: Int,
    val expectedFeatureCount: Int,
    val completeFeatureSet: Boolean,
    val validatedFeatureCount: Int,
    val writeEnabledFeatureCount: Int,
    val blockedFeatureCount: Int,
    val writeEnabledFeatureLabels: List<String>,
    val blockedFeatureLabels: List<String>,
    val safetyPreviewText: String,
    val sourceSummary: String,
    val summary: String,
    val displayText: String,
)

@Serializable
data class AirVisionDiagnosticsFirmwareCaptureResultsSource(
    val windowsHost: String?,
    val captureTool: String?,
    val asusAirVisionAppVersion: String?,
    val androidDiagnosticsExportSha256: String?,
    val notes: String?,
)

@Serializable
data class AirVisionDiagnosticsFirmwareUpdate(
    val androidFirmwareUpdateSupported: Boolean,
    val windowsFirmwareUpdateRequired: Boolean,
    val androidUpdateCheckAvailable: Boolean,
    val detectedVersionContext: String?,
    val recommendedExternalAdapterFirmware: String,
    val summary: String,
    val limitations: List<String>,
)

@Serializable
data class AirVisionDiagnosticsDeviceInfo(
    val manufacturerName: String?,
    val productName: String?,
    val deviceName: String?,
    val vendorProduct: String?,
    val deviceClass: Int?,
    val deviceSubclass: Int?,
    val deviceProtocol: Int?,
    val interfaceCount: Int?,
    val serialStatus: String?,
    val firmwareVersion: String?,
)

@Serializable
data class AirVisionDiagnosticsInterface(
    val id: Int,
    val interfaceClass: Int,
    val classLabel: String,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpoints: List<AirVisionDiagnosticsEndpoint>,
)

@Serializable
data class AirVisionDiagnosticsEndpoint(
    val address: Int,
    val direction: Int,
    val directionLabel: String,
    val type: Int,
    val typeLabel: String,
    val maxPacketSize: Int,
    val interval: Int,
)

@Serializable
data class AirVisionDiagnosticsHudRuntime(
    val transcriptEntryCount: Int,
    val captionEntryCount: Int,
    val effectiveHudScalePercent: Int,
    val colorPreviewOverlaysEnabled: Boolean,
    val brightnessDimmingEnabled: Boolean,
    val ipdAdjustmentEnabled: Boolean,
    val threeDModeAvailable: Boolean,
    val presentationActive: Boolean,
    val displayTarget: String,
    val presentationDisplayCategoryPreferred: Boolean,
    val nonDefaultDisplayFallbackEnabled: Boolean,
    val displayCandidateCount: Int,
    val presentationDisplayCandidateCount: Int,
    val selectedDisplayId: Int?,
    val selectedDisplayName: String?,
    val selectedDisplayWidthPx: Int?,
    val selectedDisplayHeightPx: Int?,
    val selectedDisplayPresentationEligible: Boolean?,
    val usedNonDefaultDisplayFallback: Boolean,
    val displayRouteReason: String,
)

@Serializable
data class AirVisionDiagnosticsProfileBackup(
    val schema: String,
    val currentVersion: Int,
    val supportedVersions: List<Int>,
    val activeViewMode: String,
    val customLabels: AirVisionBackupCustomLabels,
    val exportedProfileCount: Int,
    val exportedRuntimeProfileCount: Int,
    val expectedProfileCount: Int,
    val completeProfileSet: Boolean,
    val includesHudControls: Boolean,
    val includesAppPreferences: Boolean,
    val includesRuntimeProfiles: Boolean,
    val profiles: List<AirVisionBackupDisplayProfile>,
    val runtimeProfiles: List<AirVisionBackupRuntimeProfile>,
    val runtimeSummaries: List<AirVisionDiagnosticsProfileRuntimeSummary>,
    val restoreScope: List<String>,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsProfileRuntimeSummary(
    val viewMode: String,
    val label: String,
    val effectiveHudScalePercent: Int,
    val hudTranscriptEntryCount: Int,
    val hudCaptionEntryCount: Int,
    val colorPreviewOverlaysEnabled: Boolean,
    val brightnessDimmingEnabled: Boolean,
    val ipdAdjustmentEnabled: Boolean,
    val threeDModeAvailable: Boolean,
    val blueLightFilterAvailable: Boolean,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsFitAndClarity(
    val ipdMm: Int,
    val asusDocumentedMinIpdMm: Double,
    val asusDocumentedMaxIpdMm: Double,
    val currentIpdWithinAsusRange: Boolean,
    val androidCalibrationMinIpdMm: Int,
    val androidCalibrationMaxIpdMm: Int,
    val virtualDistanceCm: Int,
    val hudScalePercent: Int,
    val effectiveHudScalePercent: Int,
    val threeDModeEnabled: Boolean,
    val blurChecks: List<String>,
    val textSizeActions: List<String>,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsDemoExperience(
    val androidDemoModeAvailable: Boolean,
    val androidDemoModeEnabled: Boolean,
    val windowsDemoShortcutAvailable: Boolean,
    val reviewerAccessReady: Boolean,
    val liveGatewayRequiredForReview: Boolean,
    val liveM1RequiredForReview: Boolean,
    val offlineReviewSurfaces: List<String>,
    val summary: String,
    val limitations: List<String>,
)

@Serializable
data class AirVisionDiagnosticsWindowsCompatibility(
    val cursorFollowAvailable: Boolean,
    val centerCursorAvailable: Boolean,
    val threeDofAvailable: Boolean,
    val unityMirrorWindowAvailable: Boolean,
    val androidMirrorFallback: String,
    val androidMirrorFallbackActions: List<String>,
    val distanceHotkeyMapped: Boolean,
    val hardwareTouchpadPassthrough: Boolean,
    val summary: String,
    val limitations: List<String>,
)

object AirVisionDiagnosticsSnapshots {
    const val SCHEMA = "openclaw.airvision.m1.diagnostics"
    const val VERSION = 26
    private const val ASUS_MIN_IPD_MM = 53.5
    private const val ASUS_MAX_IPD_MM = 74.5
    private val SUPPORTED_PROFILE_BACKUP_VERSIONS = listOf(1, 2, 3, AirVisionProfileBackups.VERSION)

    private val json =
        Json {
            encodeDefaults = true
            prettyPrint = true
        }

    fun encode(snapshot: AirVisionDiagnosticsSnapshot): String = json.encodeToString(snapshot)

    fun fromState(
        usbState: AirVisionUsbState,
        displaySettings: AirVisionDisplaySettings,
        hudControls: AirVisionHudControls,
        appLanguage: AirVisionAppLanguage,
        startupDestination: AirVisionStartupDestination,
        hudDisplayTarget: AirVisionHudDisplayTarget,
        hudPresentationActive: Boolean = false,
        hudDisplayRoute: AirVisionHudDisplayRoute = AirVisionHudDisplayRoute(target = hudDisplayTarget),
        demoModeEnabled: Boolean,
        speakerEnabled: Boolean = true,
        nativeCaptionsEnabled: Boolean = false,
        translationCaptionSourceLanguage: String = TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
        translationCaptionTargetLanguage: String = TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
        firmwareCaptureResults: AirVisionFirmwareCaptureResults? = null,
        profileBackup: AirVisionProfileBackup? = null,
    ): AirVisionDiagnosticsSnapshot {
        val effectiveHudScalePercent =
            (
                AirVisionDisplaySettings.hudScaleForDistanceCm(displaySettings.distanceCm) *
                    AirVisionDisplaySettings.hudScaleMultiplierForViewMode(displaySettings.viewMode) *
                    AirVisionDisplaySettings.hudScaleMultiplierForPercent(displaySettings.hudScalePercent) *
                    100f
            ).toInt()
        val currentIpdWithinAsusRange = displaySettings.ipdMm.toDouble() in ASUS_MIN_IPD_MM..ASUS_MAX_IPD_MM
        val expectedProfileCount = AirVisionViewMode.entries.size
        val fallbackProfile = AirVisionProfileBackups.profileFromSettings(displaySettings)
        val backupProfiles = profileBackup?.profiles ?: listOf(fallbackProfile)
        val backupRuntimeProfiles =
            profileBackup?.runtimeProfiles
                ?: listOf(AirVisionProfileBackups.runtimeProfileFromSettings(displaySettings))
        val backupCustomLabels =
            profileBackup?.customLabels
                ?: AirVisionBackupCustomLabels(
                    custom1 = AirVisionViewMode.Custom1.label,
                    custom2 = AirVisionViewMode.Custom2.label,
                )
        val backupRuntimeSummaries =
            backupProfiles.map { profile ->
                profileRuntimeSummary(
                    profile = profile,
                    labels = backupCustomLabels,
                )
            }
        val exportedProfileCount = backupProfiles.size
        val exportedRuntimeProfileCount = backupRuntimeProfiles.size
        val profileModes = backupProfiles.map { it.viewMode }.toSet()
        val completeProfileSet =
            AirVisionViewMode.entries.all { it.rawValue in profileModes } &&
                exportedProfileCount == expectedProfileCount
        val firmwareCaptureResultsDiagnostics = firmwareCaptureResults.toDiagnostics()

        return AirVisionDiagnosticsSnapshot(
            usb =
                AirVisionDiagnosticsUsb(
                    connected = usbState.connected,
                    permissionGranted = usbState.permissionGranted,
                    deviceLabel = usbState.deviceLabel,
                    vendorProduct = usbState.vendorProduct,
                    firmwareControlReady = usbState.firmwareControlReady,
                    hidControlInterface = usbState.hidControlInterface,
                    audioInterface = usbState.audioInterface,
                    inputInterface = usbState.inputInterface,
                    statusText = usbState.statusText,
                    firmwareCapabilities = usbState.firmwareCapabilities.toDiagnostics(),
                    deviceInfo =
                        AirVisionDiagnosticsDeviceInfo(
                            manufacturerName = usbState.deviceInfo.manufacturerName,
                            productName = usbState.deviceInfo.productName,
                            deviceName = usbState.deviceInfo.deviceName,
                            vendorProduct = usbState.deviceInfo.vendorProduct,
                            deviceClass = usbState.deviceInfo.deviceClass,
                            deviceSubclass = usbState.deviceInfo.deviceSubclass,
                            deviceProtocol = usbState.deviceInfo.deviceProtocol,
                            interfaceCount = usbState.deviceInfo.interfaceCount,
                            serialStatus = usbState.deviceInfo.serialStatus ?: usbState.deviceInfo.serialNumber?.let { "available" },
                            firmwareVersion = usbState.deviceInfo.firmwareVersion,
                        ),
                    interfaces =
                        usbState.interfaces.map { usbInterface ->
                            AirVisionDiagnosticsInterface(
                                id = usbInterface.id,
                                interfaceClass = usbInterface.interfaceClass,
                                classLabel = usbInterface.classLabel,
                                interfaceSubclass = usbInterface.interfaceSubclass,
                                interfaceProtocol = usbInterface.interfaceProtocol,
                                endpoints =
                                    usbInterface.endpoints.map { endpoint ->
                                        AirVisionDiagnosticsEndpoint(
                                            address = endpoint.address,
                                            direction = endpoint.direction,
                                            directionLabel = endpoint.directionLabel,
                                            type = endpoint.type,
                                            typeLabel = endpoint.typeLabel,
                                            maxPacketSize = endpoint.maxPacketSize,
                                            interval = endpoint.interval,
                                        )
                                    },
                            )
                        },
                ),
            activeProfile = AirVisionProfileBackups.profileFromSettings(displaySettings),
            firmwareSync =
                AirVisionFirmwareSyncPlans
                    .fromSettings(
                        settings = displaySettings,
                        capabilities = usbState.firmwareCapabilities,
                        captureResults = firmwareCaptureResults,
                    ).toDiagnostics(),
            firmwareCaptureResults = firmwareCaptureResultsDiagnostics,
            firmwareUpdate =
                AirVisionDiagnosticsFirmwareUpdate(
                    androidFirmwareUpdateSupported = false,
                    windowsFirmwareUpdateRequired = true,
                    androidUpdateCheckAvailable = false,
                    detectedVersionContext = usbState.deviceInfo.firmwareVersion,
                    recommendedExternalAdapterFirmware = "1.0.7.1 or later for PS5/HDMI adapter use",
                    summary =
                        usbState.deviceInfo.firmwareVersion
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "Android can report $it, but AirVision firmware update checks and installs require the ASUS Windows app." }
                            ?: "AirVision firmware update checks and installs require the ASUS Windows app; Android stays read-only.",
                    limitations =
                        listOf(
                            "ASUS firmware updates are performed through the AirVision Windows app.",
                            "Phone or tablet firmware update is not supported by the ASUS workflow.",
                            "Android diagnostics can export USB descriptor context but cannot confirm a newer ASUS firmware package.",
                        ),
                ),
            hudRuntime =
                AirVisionDiagnosticsHudRuntime(
                    transcriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(displaySettings.lightLoadModeEnabled),
                    captionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(displaySettings.lightLoadModeEnabled),
                    effectiveHudScalePercent = effectiveHudScalePercent,
                    colorPreviewOverlaysEnabled =
                        AirVisionDisplaySettings.hudColorPreviewAlpha(
                            alpha = 1f,
                            lightLoadModeEnabled = displaySettings.lightLoadModeEnabled,
                        ) > 0f,
                    brightnessDimmingEnabled =
                        AirVisionDisplaySettings.hudDimAlphaForBrightnessPercent(displaySettings.brightnessPercent) > 0f,
                    ipdAdjustmentEnabled = displaySettings.ipdAdjustmentEnabled,
                    threeDModeAvailable = displaySettings.threeDModeAvailable,
                    presentationActive = hudPresentationActive,
                    displayTarget = hudDisplayTarget.rawValue,
                    presentationDisplayCategoryPreferred = true,
                    nonDefaultDisplayFallbackEnabled = true,
                    displayCandidateCount = hudDisplayRoute.candidateCount,
                    presentationDisplayCandidateCount = hudDisplayRoute.presentationCandidateCount,
                    selectedDisplayId = hudDisplayRoute.selectedCandidate?.displayId,
                    selectedDisplayName = hudDisplayRoute.selectedCandidate?.name,
                    selectedDisplayWidthPx = hudDisplayRoute.selectedCandidate?.widthPx,
                    selectedDisplayHeightPx = hudDisplayRoute.selectedCandidate?.heightPx,
                    selectedDisplayPresentationEligible = hudDisplayRoute.selectedCandidate?.isPresentation,
                    usedNonDefaultDisplayFallback = hudDisplayRoute.usedNonDefaultDisplayFallback,
                    displayRouteReason = hudDisplayRoute.reason,
                ),
            profileBackup =
                AirVisionDiagnosticsProfileBackup(
                    schema = AirVisionProfileBackups.SCHEMA,
                    currentVersion = AirVisionProfileBackups.VERSION,
                    supportedVersions = SUPPORTED_PROFILE_BACKUP_VERSIONS,
                    activeViewMode = profileBackup?.activeViewMode ?: displaySettings.viewMode.rawValue,
                    customLabels = backupCustomLabels,
                    exportedProfileCount = exportedProfileCount,
                    exportedRuntimeProfileCount = exportedRuntimeProfileCount,
                    expectedProfileCount = expectedProfileCount,
                    completeProfileSet = completeProfileSet,
                    includesHudControls = true,
                    includesAppPreferences = true,
                    includesRuntimeProfiles = exportedRuntimeProfileCount == expectedProfileCount,
                    profiles = backupProfiles,
                    runtimeProfiles = backupRuntimeProfiles,
                    runtimeSummaries = backupRuntimeSummaries,
                    restoreScope =
                        listOf(
                            "view mode profiles",
                            "custom profile labels",
                            "HUD gesture and hotkey controls",
                            "startup view and display target",
                            "speaker and captions preferences",
                            "translation caption languages",
                            "demo mode preference",
                            "derived runtime summaries",
                        ),
                    summary =
                        "profile backup v${AirVisionProfileBackups.VERSION}: $exportedProfileCount/$expectedProfileCount profiles, " +
                            "$exportedRuntimeProfileCount runtime profiles, HUD controls and app preferences included.",
                ),
            fitAndClarity =
                AirVisionDiagnosticsFitAndClarity(
                    ipdMm = displaySettings.ipdMm,
                    asusDocumentedMinIpdMm = ASUS_MIN_IPD_MM,
                    asusDocumentedMaxIpdMm = ASUS_MAX_IPD_MM,
                    currentIpdWithinAsusRange = currentIpdWithinAsusRange,
                    androidCalibrationMinIpdMm = AirVisionDisplaySettings.MIN_IPD_MM,
                    androidCalibrationMaxIpdMm = AirVisionDisplaySettings.MAX_IPD_MM,
                    virtualDistanceCm = displaySettings.distanceCm,
                    hudScalePercent = displaySettings.hudScalePercent,
                    effectiveHudScalePercent = effectiveHudScalePercent,
                    threeDModeEnabled = displaySettings.threeDModeEnabled,
                    blurChecks =
                        listOf(
                            "Confirm 3D Mode is off unless viewing side-by-side 3D content.",
                            "Confirm IPD is within the ASUS documented 53.5-74.5 mm range.",
                            "Wear the correct prescription lenses or contacts if needed.",
                            "Adjust glasses position before changing firmware-level alignment.",
                        ),
                    textSizeActions =
                        listOf(
                            "Increase Android HUD Scale in this AirVision profile.",
                            "Pull Virtual Distance closer to enlarge HUD text.",
                            "Use Android or DeX display scaling outside the HUD when mirroring other apps.",
                            "Use browser zoom for web content outside the HUD.",
                        ),
                    summary =
                        if (currentIpdWithinAsusRange) {
                            "IPD ${displaySettings.ipdMm} mm is within ASUS documented range; effective HUD scale is $effectiveHudScalePercent%."
                        } else {
                            "IPD ${displaySettings.ipdMm} mm is outside ASUS documented range; verify fit, prescription, and alignment before relying on software scaling."
                        },
                ),
            demoExperience =
                AirVisionDiagnosticsDemoExperience(
                    androidDemoModeAvailable = true,
                    androidDemoModeEnabled = demoModeEnabled,
                    windowsDemoShortcutAvailable = false,
                    reviewerAccessReady = demoModeEnabled,
                    liveGatewayRequiredForReview = false,
                    liveM1RequiredForReview = false,
                    offlineReviewSurfaces =
                        listOf(
                            "minimal green-on-black HUD",
                            "sample chat and assistant status",
                            "sample notification and caption text",
                            "AirVision profile/settings review",
                            "diagnostics export flow",
                        ),
                    summary =
                        if (demoModeEnabled) {
                            "Android Demo Mode is enabled for deterministic HUD review, tutorials, screenshots, and fit checks without a live gateway or live M1."
                        } else {
                            "Android Demo Mode is available for deterministic HUD review, tutorials, screenshots, and fit checks without a live gateway or live M1."
                        },
                    limitations =
                        listOf(
                            "ASUS Windows demo mode uses the Windows AirVision tutorial shortcut flow.",
                            "Android Demo Mode does not toggle ASUS Windows demo state on the glasses.",
                            "Google Play reviewer access still needs Play Console App access instructions before final submission.",
                        ),
                ),
            windowsCompatibility =
                AirVisionDiagnosticsWindowsCompatibility(
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
                    distanceHotkeyMapped = hudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance,
                    hardwareTouchpadPassthrough = true,
                    summary =
                        if (hudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) {
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
                ),
            hudControls =
                AirVisionBackupHudControls(
                    singleTapAction = hudControls.singleTapAction.rawValue,
                    doubleTapAction = hudControls.doubleTapAction.rawValue,
                    swipeAction = hudControls.swipeAction.rawValue,
                    brightnessKeyAction = hudControls.brightnessKeyAction.rawValue,
                    mediaKeyAction = hudControls.mediaKeyAction.rawValue,
                ),
            appPreferences =
                AirVisionBackupAppPreferences(
                    language = appLanguage.rawValue,
                    startupDestination = startupDestination.rawValue,
                    hudDisplayTarget = hudDisplayTarget.rawValue,
                    demoModeEnabled = demoModeEnabled,
                    speakerEnabled = speakerEnabled,
                    nativeCaptionsEnabled = nativeCaptionsEnabled,
                    translationCaptionSourceLanguage =
                        TranslationCaptionMode.normalizeLanguageCode(
                            translationCaptionSourceLanguage,
                            TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
                        ),
                    translationCaptionTargetLanguage =
                        TranslationCaptionMode.normalizeLanguageCode(
                            translationCaptionTargetLanguage,
                            TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
                        ),
                ),
        )
    }

    private fun profileRuntimeSummary(
        profile: AirVisionBackupDisplayProfile,
        labels: AirVisionBackupCustomLabels,
    ): AirVisionDiagnosticsProfileRuntimeSummary {
        val mode = AirVisionViewMode.fromRawValue(profile.viewMode)
        val runtime = AirVisionProfileBackups.runtimeProfileFromSettings(AirVisionProfileBackups.settingsFromProfile(profile))
        val label =
            when (mode) {
                AirVisionViewMode.Custom1 -> labels.custom1
                AirVisionViewMode.Custom2 -> labels.custom2
                else -> mode.label
            }
        return AirVisionDiagnosticsProfileRuntimeSummary(
            viewMode = mode.rawValue,
            label = label,
            effectiveHudScalePercent = runtime.effectiveHudScalePercent,
            hudTranscriptEntryCount = runtime.hudTranscriptEntryCount,
            hudCaptionEntryCount = runtime.hudCaptionEntryCount,
            colorPreviewOverlaysEnabled = runtime.colorPreviewOverlaysEnabled,
            brightnessDimmingEnabled = runtime.brightnessDimmingEnabled,
            ipdAdjustmentEnabled = runtime.ipdAdjustmentEnabled,
            threeDModeAvailable = runtime.threeDModeAvailable,
            blueLightFilterAvailable = runtime.blueLightFilterAvailable,
            summary =
                "$label: effective HUD scale ${runtime.effectiveHudScalePercent}%, " +
                    "transcript ${runtime.hudTranscriptEntryCount}, captions ${runtime.hudCaptionEntryCount}",
        )
    }

    private fun AirVisionFirmwareCapabilities.toDiagnostics(): AirVisionDiagnosticsFirmwareCapabilities =
        AirVisionDiagnosticsFirmwareCapabilities(
            hidInputInterfaceIds = hidInputInterfaceIds,
            hidOutputInterfaceIds = hidOutputInterfaceIds,
            interruptInputEndpoints = interruptInputEndpoints,
            interruptOutputEndpoints = interruptOutputEndpoints,
            maxInputPacketSize = maxInputPacketSize,
            maxOutputPacketSize = maxOutputPacketSize,
            readableReportPaths = readableReportPaths.map { it.toDiagnostics() },
            writableReportPaths = writableReportPaths.map { it.toDiagnostics() },
            featureReadiness = featureReadiness.map { it.toDiagnostics() },
            captureTargets = captureTargets.map { it.toDiagnostics() },
            hasReadableHidReports = hasReadableHidReports,
            hasWritableHidReports = hasWritableHidReports,
            hasInterruptReportPath = hasInterruptReportPath,
            protocolCaptureReady = protocolCaptureReady,
            summary = summary,
            featureReadinessSummary = featureReadinessSummary,
            capturePlanSummary = capturePlanSummary,
        )

    private fun AirVisionFirmwareReportPath.toDiagnostics(): AirVisionDiagnosticsFirmwareReportPath =
        AirVisionDiagnosticsFirmwareReportPath(
            interfaceId = interfaceId,
            endpointAddress = endpointAddress,
            direction = direction,
            directionLabel = directionLabel,
            type = type,
            typeLabel = typeLabel,
            maxPacketSize = maxPacketSize,
            interval = interval,
            summary = summary,
        )

    private fun AirVisionFirmwareFeatureReadiness.toDiagnostics(): AirVisionDiagnosticsFirmwareFeatureReadiness =
        AirVisionDiagnosticsFirmwareFeatureReadiness(
            feature = feature.rawValue,
            label = feature.label,
            androidStatus = androidStatus,
            firmwareApplyReady = firmwareApplyReady,
            firmwareApplyStatus = firmwareApplyStatus,
            detail = detail,
            summary = summary,
        )

    private fun AirVisionFirmwareCaptureTarget.toDiagnostics(): AirVisionDiagnosticsFirmwareCaptureTarget =
        AirVisionDiagnosticsFirmwareCaptureTarget(
            feature = feature.rawValue,
            label = feature.label,
            captureReady = captureReady,
            writeReportPathSummaries = writeReportPathSummaries,
            readReportPathSummaries = readReportPathSummaries,
            suggestedProbeValues = suggestedProbeValues,
            instruction = instruction,
            summary = summary,
        )

    private fun AirVisionFirmwareSyncPlan.toDiagnostics(): AirVisionDiagnosticsFirmwareSyncPlan =
        AirVisionDiagnosticsFirmwareSyncPlan(
            pendingHardwareSyncCount = pendingHardwareSyncCount,
            androidAppliedCount = androidAppliedCount,
            firmwareWriteAllowedCount = firmwareWriteAllowedCount,
            blockedFirmwareWriteCount = blockedFirmwareWriteCount,
            writeGate = writeGate.toDiagnostics(),
            summary = summary,
            writeGateSummary = writeGateSummary,
            detailSummary = detailSummary,
            items = items.map { it.toDiagnostics() },
        )

    private fun AirVisionFirmwareWriteGate.toDiagnostics(): AirVisionDiagnosticsFirmwareWriteGate =
        AirVisionDiagnosticsFirmwareWriteGate(
            status = status,
            firmwareWritesEnabled = firmwareWritesEnabled,
            validatedCaptureCount = validatedCaptureCount,
            writeEnabledCaptureCount = writeEnabledCaptureCount,
            blockedFeatureCount = blockedFeatureCount,
            protocolReadyFeatureLabels = protocolReadyFeatureLabels,
            blockedFeatureLabels = blockedFeatureLabels,
            blockedFeatureSummaries = blockedFeatureSummaries,
            liveTestChecklist = liveTestChecklist,
            liveM1Required = liveM1Required,
            explicitUserConfirmationRequired = explicitUserConfirmationRequired,
            summary = summary,
            nextStep = nextStep,
        )

    private fun AirVisionFirmwareSyncItem.toDiagnostics(): AirVisionDiagnosticsFirmwareSyncItem =
        AirVisionDiagnosticsFirmwareSyncItem(
            feature = feature.rawValue,
            label = feature.label,
            desiredValue = desiredValue,
            androidApplied = androidApplied,
            androidEffect = androidEffect,
            hardwareSyncPending = hardwareSyncPending,
            hardwareSyncStatus = hardwareSyncStatus,
            captureResultsSchema = AirVisionFirmwareSyncPlans.CAPTURE_RESULTS_SCHEMA,
            captureResultStatus = captureResultStatus,
            androidEnablementDecision = androidEnablementDecision,
            firmwareWriteAllowed = firmwareWriteAllowed,
            requiredEvidence = requiredEvidence,
            blockedReason = blockedReason,
            summary = summary,
        )

    private fun AirVisionFirmwareCaptureResults?.toDiagnostics(): AirVisionDiagnosticsFirmwareCaptureResults {
        val expectedFeatureCount = AirVisionFirmwareFeature.entries.size
        if (this == null) {
            return AirVisionDiagnosticsFirmwareCaptureResults(
                imported = false,
                schema = AirVisionFirmwareCaptureResultFiles.SCHEMA,
                version = null,
                payloadPolicy = null,
                source = null,
                featureCount = 0,
                expectedFeatureCount = expectedFeatureCount,
                completeFeatureSet = false,
                validatedFeatureCount = 0,
                writeEnabledFeatureCount = 0,
                blockedFeatureCount = 0,
                writeEnabledFeatureLabels = emptyList(),
                blockedFeatureLabels = emptyList(),
                safetyPreviewText =
                    "No capture results imported; Android firmware writes remain blocked.",
                sourceSummary = "source=pending",
                summary = "capture results: not imported",
                displayText = "capture results: not imported; source=pending",
            )
        }

        val summary = AirVisionFirmwareCaptureResultFiles.summarize(this)
        return AirVisionDiagnosticsFirmwareCaptureResults(
            imported = true,
            schema = schema,
            version = version,
            payloadPolicy = payloadPolicy,
            source =
                AirVisionDiagnosticsFirmwareCaptureResultsSource(
                    windowsHost = source.windowsHost,
                    captureTool = source.captureTool,
                    asusAirVisionAppVersion = source.asusAirVisionAppVersion,
                    androidDiagnosticsExportSha256 = source.androidDiagnosticsExportSha256,
                    notes = source.notes,
                ),
            featureCount = summary.featureCount,
            expectedFeatureCount = expectedFeatureCount,
            completeFeatureSet = summary.featureCount == expectedFeatureCount,
            validatedFeatureCount = summary.validatedFeatureCount,
            writeEnabledFeatureCount = summary.writeEnabledFeatureCount,
            blockedFeatureCount = summary.blockedFeatureCount,
            writeEnabledFeatureLabels = summary.writeEnabledFeatureLabels,
            blockedFeatureLabels = summary.blockedFeatureLabels,
            safetyPreviewText = summary.safetyPreviewText,
            sourceSummary = summary.sourceSummary,
            summary = summary.summary,
            displayText = summary.displayText,
        )
    }
}
