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
    val windowsAppEvidence: AirVisionDiagnosticsWindowsAppEvidence,
    val windowsApplyMatrix: AirVisionDiagnosticsWindowsApplyMatrix,
    val companionParity: AirVisionDiagnosticsCompanionParity,
    val hardwareKeyMapping: AirVisionDiagnosticsHardwareKeyMapping,
    val windowsGestureCatalog: AirVisionDiagnosticsWindowsGestureCatalog,
    val shortcutMenu: AirVisionDiagnosticsShortcutMenu,
    val hudControls: AirVisionBackupHudControls,
    val appPreferences: AirVisionBackupAppPreferences,
    val supportMetadata: AirVisionSupportMetadata,
    val captionMode: AirVisionCaptionModeStatus,
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
    val applyPreview: AirVisionDiagnosticsFirmwareApplyPreview,
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
data class AirVisionDiagnosticsFirmwareApplyPreview(
    val status: String,
    val commandCount: Int,
    val readyCommandCount: Int,
    val blockedFeatureLabels: List<String>,
    val summary: String,
    val commands: List<AirVisionDiagnosticsFirmwareApplyCommand>,
)

@Serializable
data class AirVisionDiagnosticsFirmwareApplyCommand(
    val feature: String,
    val label: String,
    val desiredValue: String,
    val writeReportId: String,
    val writeEndpoint: String,
    val writePayloadSummary: String,
    val readbackReportId: String,
    val readbackEndpoint: String,
    val readbackPayloadSummary: String,
    val checksumFramingNotes: String,
    val protocolReady: Boolean,
    val blockedReason: String,
    val summary: String,
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
    val capturedFeatureCount: Int,
    val pendingFeatureCount: Int,
    val writeEnabledFeatureCount: Int,
    val validatedBlockedFeatureCount: Int,
    val blockedFeatureCount: Int,
    val writeEnabledFeatureLabels: List<String>,
    val validatedBlockedFeatureLabels: List<String>,
    val reviewRequiredFeatureLabels: List<String>,
    val pendingFeatureLabels: List<String>,
    val blockedFeatureLabels: List<String>,
    val sourceCompletenessWarnings: List<String>,
    val sourceCompletenessSummary: String,
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
    val rememberedDisplayName: String?,
    val rememberedDisplayWidthPx: Int?,
    val rememberedDisplayHeightPx: Int?,
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
    val androidMirrorFallbackLaunchActions: List<AirVisionDiagnosticsMirrorLaunchAction>,
    val distanceHotkeyMapped: Boolean,
    val hardwareTouchpadPassthrough: Boolean,
    val summary: String,
    val limitations: List<String>,
)

@Serializable
data class AirVisionDiagnosticsMirrorLaunchAction(
    val id: String,
    val label: String,
    val androidIntentAction: String,
    val fallbackIntentAction: String?,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsWindowsApplyMatrix(
    val itemCount: Int,
    val liveM1RequiredCount: Int,
    val firmwareGatedCount: Int,
    val windowsOnlyCount: Int,
    val summary: String,
    val items: List<AirVisionDiagnosticsWindowsApplyMatrixItem>,
)

@Serializable
data class AirVisionDiagnosticsWindowsApplyMatrixItem(
    val feature: String,
    val windowsAppTarget: String,
    val windowsSurface: String,
    val observedSettingKeys: List<String>,
    val observedDefault: String,
    val androidEffect: String,
    val captureImplication: String,
    val liveM1ProofRequired: Boolean,
    val firmwareGate: String,
    val windowsOnly: Boolean,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsHardwareKeyMapping(
    val brightnessKeyAction: String,
    val brightnessKeyLabel: String,
    val brightnessKeyConsumedByAndroid: Boolean,
    val brightnessStepPercent: Int?,
    val distanceStepCm: Int?,
    val scrollStepPx: Float?,
    val mediaKeyAction: String,
    val mediaKeyLabel: String,
    val mediaKeyDoubleTapTimeoutMs: Long,
    val customMediaKeyCode: Int?,
    val customMediaKeyLabel: String,
    val firmwareBrightnessPassthroughPossible: Boolean,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsWindowsGestureCatalog(
    val itemCount: Int,
    val androidImplementedCount: Int,
    val firmwarePassthroughCount: Int,
    val windowsOnlyCount: Int,
    val items: List<AirVisionDiagnosticsWindowsGestureCatalogItem>,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsWindowsGestureCatalogItem(
    val gesture: String,
    val windowsAction: String,
    val androidStatus: String,
    val androidMapping: String,
    val firmwarePassthroughRequired: Boolean,
    val windowsOnly: Boolean,
    val notes: String,
)

@Serializable
data class AirVisionDiagnosticsShortcutMenu(
    val brightness: String,
    val volume: String,
    val distance: String,
    val androidMappedCount: Int,
    val firmwareOrSystemOwnedCount: Int,
    val summary: String,
)

object AirVisionDiagnosticsSnapshots {
    const val SCHEMA = "openclaw.airvision.m1.diagnostics"
    const val VERSION = 38
    private const val ASUS_MIN_IPD_MM = 53.5
    private const val ASUS_MAX_IPD_MM = 74.5
    private val SUPPORTED_PROFILE_BACKUP_VERSIONS = (1..AirVisionProfileBackups.VERSION).toList()

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
        val spatialMirrorFallback = AirVisionSpatialMirrorFallback.from(hudControls)
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
                    rememberedDisplayName = hudDisplayRoute.rememberedDisplay?.name,
                    rememberedDisplayWidthPx = hudDisplayRoute.rememberedDisplay?.widthPx,
                    rememberedDisplayHeightPx = hudDisplayRoute.rememberedDisplay?.heightPx,
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
                            "startup view, display target, and remembered display identity",
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
                    cursorFollowAvailable = spatialMirrorFallback.cursorFollowAvailable,
                    centerCursorAvailable = spatialMirrorFallback.centerCursorAvailable,
                    threeDofAvailable = spatialMirrorFallback.threeDofAvailable,
                    unityMirrorWindowAvailable = spatialMirrorFallback.unityMirrorWindowAvailable,
                    androidMirrorFallback = spatialMirrorFallback.androidMirrorFallback,
                    androidMirrorFallbackActions = spatialMirrorFallback.androidMirrorFallbackActions,
                    androidMirrorFallbackLaunchActions =
                        spatialMirrorFallback.androidMirrorFallbackLaunchActions.map {
                            AirVisionDiagnosticsMirrorLaunchAction(
                                id = it.id,
                                label = it.label,
                                androidIntentAction = it.androidIntentAction,
                                fallbackIntentAction = it.fallbackIntentAction,
                                summary = it.summary,
                            )
                        },
                    distanceHotkeyMapped = spatialMirrorFallback.distanceHotkeyMapped,
                    hardwareTouchpadPassthrough = spatialMirrorFallback.hardwareTouchpadPassthrough,
                    summary = spatialMirrorFallback.summary,
                    limitations = spatialMirrorFallback.limitations,
                ),
            windowsAppEvidence = AirVisionWindowsAppEvidence.diagnostics,
            windowsApplyMatrix =
                windowsApplyMatrix(
                    profile = fallbackProfile,
                    labels = backupCustomLabels,
                    controls = hudControls,
                    displayTarget = hudDisplayTarget,
                ),
            companionParity =
                AirVisionCompanionParity.fromState(
                    hudControls = hudControls,
                    nativeCaptionsEnabled = nativeCaptionsEnabled,
                    translationCaptionSourceLanguage = translationCaptionSourceLanguage,
                    translationCaptionTargetLanguage = translationCaptionTargetLanguage,
                ),
            hardwareKeyMapping = hardwareKeyMapping(hudControls),
            windowsGestureCatalog = windowsGestureCatalog(hudControls),
            shortcutMenu = shortcutMenu(hudControls, speakerEnabled),
            hudControls =
                AirVisionBackupHudControls(
                    singleTapAction = hudControls.singleTapAction.rawValue,
                    doubleTapAction = hudControls.doubleTapAction.rawValue,
                    swipeAction = hudControls.swipeAction.rawValue,
                    horizontalSwipeAction = hudControls.horizontalSwipeAction.rawValue,
                    brightnessKeyAction = hudControls.brightnessKeyAction.rawValue,
                    mediaKeyAction = hudControls.mediaKeyAction.rawValue,
                ),
            appPreferences =
                AirVisionBackupAppPreferences(
                    language = appLanguage.rawValue,
                    startupDestination = startupDestination.rawValue,
                    hudDisplayTarget = hudDisplayTarget.rawValue,
                    rememberedHudDisplay =
                        hudDisplayRoute.rememberedDisplay?.let {
                            AirVisionBackupDisplayFingerprint(it.name, it.widthPx, it.heightPx)
                        },
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
            supportMetadata = AirVisionSupportMetadata.default,
            captionMode =
                AirVisionCaptionModeStatus.from(
                    nativeCaptionsEnabled = nativeCaptionsEnabled,
                    sourceLanguageCode = translationCaptionSourceLanguage,
                    targetLanguageCode = translationCaptionTargetLanguage,
                ),
        )
    }

    private fun windowsGestureCatalog(controls: AirVisionHudControls): AirVisionDiagnosticsWindowsGestureCatalog {
        val items =
            listOf(
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "One-finger swipe forward/backward",
                    windowsAction = "Quick brightness adjustment",
                    androidStatus =
                        if (controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustBrightness) {
                            "implemented when Android receives brightness key events"
                        } else {
                            "firmware passthrough or alternate Android mapping"
                        },
                    androidMapping = controls.brightnessKeyAction.label,
                    firmwarePassthroughRequired = controls.brightnessKeyAction == AirVisionHudKeyAction.None,
                    windowsOnly = false,
                    notes = "ASUS documents hardware touchpad brightness; Android can also map delivered brightness keys to chat scroll, HUD dimming, or virtual distance.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "One-finger tap",
                    windowsAction = "Audio/video play/pause in floating mode",
                    androidStatus =
                        when (controls.mediaKeyAction) {
                            AirVisionHudMediaKeyAction.None -> "left to Android or firmware media handling"
                            AirVisionHudMediaKeyAction.SingleTapToggleMic ->
                                "mapped to HUD media-key single-tap mic behavior"
                            AirVisionHudMediaKeyAction.DoubleTapToggleMic ->
                                "mapped to HUD media-key double-tap mic behavior"
                            AirVisionHudMediaKeyAction.HoldToTalk ->
                                "mapped to focused HUD accessory hold-to-talk behavior"
                        },
                    androidMapping = controls.mediaKeyAction.label,
                    firmwarePassthroughRequired = controls.mediaKeyAction == AirVisionHudMediaKeyAction.None,
                    windowsOnly = false,
                    notes = "Android HUD tap actions are configured separately for on-screen touch input.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "Two-finger tap",
                    windowsAction = "Instant transparent / lowest brightness toggle",
                    androidStatus = "firmware passthrough",
                    androidMapping = "M1 firmware handles transparent/lowest-brightness behavior before Android receives a gesture event.",
                    firmwarePassthroughRequired = true,
                    windowsOnly = false,
                    notes = "Android does not synthesize panel transparent mode; use brightness passthrough or HUD dimming for software-only review.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "One-finger tap in positioning mode",
                    windowsAction = "Center virtual screens",
                    androidStatus = "Windows-only virtual-screen action",
                    androidMapping = "Android reports Center Cursor/virtual-screen centering as Windows-only and offers Cast/Display fallback guidance.",
                    firmwarePassthroughRequired = false,
                    windowsOnly = true,
                    notes = "Centering Windows virtual screens requires the ASUS AirVision Windows compositor.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "One-finger double tap",
                    windowsAction = "Switch viewing mode between positioning and floating",
                    androidStatus = "Android HUD double tap configurable",
                    androidMapping = controls.doubleTapAction.label,
                    firmwarePassthroughRequired = false,
                    windowsOnly = false,
                    notes = "Android does not switch ASUS positioning/floating compositor modes; the HUD maps double tap to microphone or notification actions.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "Two-finger press and hold for 1.5 seconds",
                    windowsAction = "Toggle 3D mode",
                    androidStatus = "profile preference stored; panel write firmware-gated",
                    androidMapping = "3D Mode profile toggle is available in Settings and locked while Light Load Mode is enabled.",
                    firmwarePassthroughRequired = true,
                    windowsOnly = false,
                    notes = "ASUS documents 3D for side-by-side full-screen content and not while Light Load Mode is active.",
                ),
                AirVisionDiagnosticsWindowsGestureCatalogItem(
                    gesture = "One-finger press and hold 1.5 seconds, then slide",
                    windowsAction = "Shortcut menu for brightness, volume, and distance",
                    androidStatus = "partially mapped through HUD controls",
                    androidMapping = "Brightness keys can adjust HUD brightness or virtual distance; audio routing is controlled by the AirVision speaker preference.",
                    firmwarePassthroughRequired = true,
                    windowsOnly = false,
                    notes = "Android does not display the ASUS shortcut menu; equivalent companion controls live in AirVision M1 settings.",
                ),
            )
        val androidImplementedCount =
            items.count { !it.windowsOnly && !it.androidStatus.contains("firmware passthrough", ignoreCase = true) }
        val firmwarePassthroughCount = items.count { it.firmwarePassthroughRequired }
        val windowsOnlyCount = items.count { it.windowsOnly }
        return AirVisionDiagnosticsWindowsGestureCatalog(
            itemCount = items.size,
            androidImplementedCount = androidImplementedCount,
            firmwarePassthroughCount = firmwarePassthroughCount,
            windowsOnlyCount = windowsOnlyCount,
            items = items,
            summary =
                "Windows gesture catalog: ${items.size} gestures, $androidImplementedCount Android-mapped, " +
                    "$firmwarePassthroughCount firmware-passthrough, $windowsOnlyCount Windows-only.",
        )
    }

    private fun hardwareKeyMapping(controls: AirVisionHudControls): AirVisionDiagnosticsHardwareKeyMapping {
        val brightnessConsumed = controls.brightnessKeyAction != AirVisionHudKeyAction.None
        val firmwarePassthrough = controls.brightnessKeyAction == AirVisionHudKeyAction.None
        val effect =
            when (controls.brightnessKeyAction) {
                AirVisionHudKeyAction.None -> "M1 firmware or Android system handles brightness keys"
                AirVisionHudKeyAction.ScrollChat -> "brightness keys scroll the HUD chat transcript"
                AirVisionHudKeyAction.AdjustBrightness -> "brightness keys step Android HUD dimming by 5%"
                AirVisionHudKeyAction.AdjustDistance -> "brightness keys step virtual projection distance by 5 cm"
            }
        return AirVisionDiagnosticsHardwareKeyMapping(
            brightnessKeyAction = controls.brightnessKeyAction.rawValue,
            brightnessKeyLabel = controls.brightnessKeyAction.label,
            brightnessKeyConsumedByAndroid = brightnessConsumed,
            brightnessStepPercent =
                if (controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustBrightness) {
                    5
                } else {
                    null
                },
            distanceStepCm =
                if (controls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) {
                    5
                } else {
                    null
                },
            scrollStepPx =
                if (controls.brightnessKeyAction == AirVisionHudKeyAction.ScrollChat) {
                    160f
                } else {
                    null
                },
            mediaKeyAction = controls.mediaKeyAction.rawValue,
            mediaKeyLabel = controls.mediaKeyAction.label,
            mediaKeyDoubleTapTimeoutMs =
                if (controls.mediaKeyAction == AirVisionHudMediaKeyAction.DoubleTapToggleMic) {
                    HUD_MEDIA_KEY_DOUBLE_TAP_TIMEOUT_MS
                } else {
                    0L
                },
            customMediaKeyCode = controls.customMediaKeyCode,
            customMediaKeyLabel = externalHudKeyLabel(controls.customMediaKeyCode),
            firmwareBrightnessPassthroughPossible = firmwarePassthrough,
            summary =
                "M1 brightness keys: ${controls.brightnessKeyAction.label}; $effect; " +
                    "media key ${controls.mediaKeyAction.label}; " +
                    "custom mic key ${externalHudKeyLabel(controls.customMediaKeyCode)}.",
        )
    }

    private fun shortcutMenu(
        controls: AirVisionHudControls,
        speakerEnabled: Boolean,
    ): AirVisionDiagnosticsShortcutMenu {
        val status = AirVisionShortcutMenuStatus.from(controls, speakerEnabled)
        return AirVisionDiagnosticsShortcutMenu(
            brightness = status.brightness,
            volume = status.volume,
            distance = status.distance,
            androidMappedCount = status.androidMappedCount,
            firmwareOrSystemOwnedCount = status.firmwareOrSystemOwnedCount,
            summary = status.summary,
        )
    }

    fun windowsApplyMatrix(
        profile: AirVisionBackupDisplayProfile,
        labels: AirVisionBackupCustomLabels,
        controls: AirVisionHudControls,
        displayTarget: AirVisionHudDisplayTarget,
    ): AirVisionDiagnosticsWindowsApplyMatrix {
        val mode = AirVisionViewMode.fromRawValue(profile.viewMode)
        val splendidMode = AirVisionSplendidMode.fromRawValue(profile.splendidMode)
        val placement = AirVisionHudPlacement.fromRawValue(profile.hudPlacement)
        val items =
            listOf(
                windowsApplyItem(
                    feature = "View Mode",
                    windowsAppTarget = mode.label,
                    androidEffect = "Android profile slot ${profileDisplayLabel(mode, labels)}",
                    liveM1ProofRequired = false,
                    firmwareGate = "none",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Brightness",
                    windowsAppTarget = "${profile.brightnessPercent}%",
                    androidEffect = "software HUD dimming",
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Screen distance",
                    windowsAppTarget = "${profile.distanceCm} cm",
                    androidEffect = "virtual HUD distance scale",
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "IPD",
                    windowsAppTarget = ipdValue(profile),
                    androidEffect = "stored calibration and fit guidance",
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Splendid / Eye Care",
                    windowsAppTarget = "${splendidMode.label}, blue-light ${blueLightFilterValue(profile)}",
                    androidEffect = "HUD color/warmth preview",
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Motion Sync",
                    windowsAppTarget = onOff(profile.motionSyncEnabled),
                    androidEffect = "stored desired state only",
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Light Load Mode",
                    windowsAppTarget = onOff(profile.lightLoadModeEnabled),
                    androidEffect = lightLoadAndroidEffect(profile),
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "3D Mode",
                    windowsAppTarget = threeDValue(profile),
                    androidEffect = threeDAndroidEffect(profile),
                    liveM1ProofRequired = true,
                    firmwareGate = "HID capture pending",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Android HUD layout",
                    windowsAppTarget = "none",
                    androidEffect =
                        "HUD scale ${profile.hudScalePercent}%, ${placement.label}, safe area ${profile.safeAreaPercent}%, " +
                            "physical main screen ${yesNo(profile.physicalMainScreenVisible)}",
                    liveM1ProofRequired = false,
                    firmwareGate = "none",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Display routing",
                    windowsAppTarget = "none",
                    androidEffect = displayTarget.label,
                    liveM1ProofRequired = true,
                    firmwareGate = "none",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Gesture and hotkey settings",
                    windowsAppTarget = "none",
                    androidEffect =
                        "single tap ${controls.singleTapAction.label}, double tap ${controls.doubleTapAction.label}, " +
                            "vertical swipe ${controls.swipeAction.label}, horizontal swipe ${controls.horizontalSwipeAction.label}, " +
                            "brightness key ${controls.brightnessKeyAction.label}, " +
                            "media key ${controls.mediaKeyAction.label}, " +
                            "custom mic key ${externalHudKeyLabel(controls.customMediaKeyCode)}",
                    liveM1ProofRequired = true,
                    firmwareGate = "none",
                    windowsOnly = false,
                ),
                windowsApplyItem(
                    feature = "Windows spatial/mirror features",
                    windowsAppTarget = "Cursor Follow, Center Cursor, 3DoF, or Unity mirror when needed",
                    androidEffect = "reports Windows-only state and offers Cast/Display fallback",
                    liveM1ProofRequired = false,
                    firmwareGate = "Windows-only",
                    windowsOnly = true,
                ),
            )
        val liveM1RequiredCount = items.count { it.liveM1ProofRequired }
        val firmwareGatedCount = items.count { it.firmwareGate == "HID capture pending" }
        val windowsOnlyCount = items.count { it.windowsOnly }
        return AirVisionDiagnosticsWindowsApplyMatrix(
            itemCount = items.size,
            liveM1RequiredCount = liveM1RequiredCount,
            firmwareGatedCount = firmwareGatedCount,
            windowsOnlyCount = windowsOnlyCount,
            summary =
                "Windows app apply matrix: ${items.size} targets, $liveM1RequiredCount live-M1 proof, " +
                    "$firmwareGatedCount firmware-gated, $windowsOnlyCount Windows-only",
            items = items,
        )
    }

    private fun windowsApplyItem(
        feature: String,
        windowsAppTarget: String,
        androidEffect: String,
        liveM1ProofRequired: Boolean,
        firmwareGate: String,
        windowsOnly: Boolean,
    ): AirVisionDiagnosticsWindowsApplyMatrixItem {
        val evidence = AirVisionWindowsAppEvidence.mappingFor(feature)
        return AirVisionDiagnosticsWindowsApplyMatrixItem(
            feature = feature,
            windowsAppTarget = windowsAppTarget,
            windowsSurface = evidence?.windowsSurface ?: "not mapped in observed ASUS AirVision evidence",
            observedSettingKeys = evidence?.observedSettingKeys.orEmpty(),
            observedDefault = evidence?.observedDefault ?: "not observed",
            androidEffect = androidEffect,
            captureImplication = evidence?.captureImplication ?: "No ASUS AirVision capture implication documented.",
            liveM1ProofRequired = liveM1ProofRequired,
            firmwareGate = firmwareGate,
            windowsOnly = windowsOnly,
            summary =
                "$feature: Windows target $windowsAppTarget; Android effect $androidEffect; " +
                    "live M1 proof ${yesNo(liveM1ProofRequired)}; firmware gate $firmwareGate",
        )
    }

    private fun profileDisplayLabel(
        mode: AirVisionViewMode,
        labels: AirVisionBackupCustomLabels,
    ): String =
        when (mode) {
            AirVisionViewMode.Custom1 -> labels.custom1
            AirVisionViewMode.Custom2 -> labels.custom2
            else -> mode.label
        }

    private fun blueLightFilterValue(profile: AirVisionBackupDisplayProfile): String =
        if (AirVisionSplendidMode.fromRawValue(profile.splendidMode) == AirVisionSplendidMode.EyeCare) {
            "${profile.blueLightFilterPercent}%"
        } else {
            "off (requires Eye Care)"
        }

    private fun ipdValue(profile: AirVisionBackupDisplayProfile): String =
        if (profile.lightLoadModeEnabled) {
            "${profile.ipdMm} mm (locked by Light Load Mode)"
        } else {
            "${profile.ipdMm} mm"
        }

    private fun threeDValue(profile: AirVisionBackupDisplayProfile): String =
        if (profile.lightLoadModeEnabled) {
            "off (locked by Light Load Mode)"
        } else {
            onOff(profile.threeDModeEnabled)
        }

    private fun lightLoadAndroidEffect(profile: AirVisionBackupDisplayProfile): String =
        if (profile.lightLoadModeEnabled) {
            "trimmed transcript/caption history and locked IPD/3D controls"
        } else {
            "full HUD history and unlocked IPD/3D controls"
        }

    private fun threeDAndroidEffect(profile: AirVisionBackupDisplayProfile): String =
        if (profile.lightLoadModeEnabled) {
            "locked off by Light Load Mode"
        } else {
            "stored desired state only"
        }

    private fun onOff(value: Boolean): String = if (value) "on" else "off"

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

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
            applyPreview = applyPreview.toDiagnostics(),
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

    private fun AirVisionFirmwareApplyPreview.toDiagnostics(): AirVisionDiagnosticsFirmwareApplyPreview =
        AirVisionDiagnosticsFirmwareApplyPreview(
            status = status,
            commandCount = commandCount,
            readyCommandCount = readyCommandCount,
            blockedFeatureLabels = blockedFeatureLabels,
            summary = summary,
            commands = commands.map { it.toDiagnostics() },
        )

    private fun AirVisionFirmwareApplyCommand.toDiagnostics(): AirVisionDiagnosticsFirmwareApplyCommand =
        AirVisionDiagnosticsFirmwareApplyCommand(
            feature = feature.rawValue,
            label = feature.label,
            desiredValue = desiredValue,
            writeReportId = writeReportId,
            writeEndpoint = writeEndpoint,
            writePayloadSummary = writePayloadSummary,
            readbackReportId = readbackReportId,
            readbackEndpoint = readbackEndpoint,
            readbackPayloadSummary = readbackPayloadSummary,
            checksumFramingNotes = checksumFramingNotes,
            protocolReady = protocolReady,
            blockedReason = blockedReason,
            summary = summary,
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
                capturedFeatureCount = 0,
                pendingFeatureCount = 0,
                writeEnabledFeatureCount = 0,
                validatedBlockedFeatureCount = 0,
                blockedFeatureCount = 0,
                writeEnabledFeatureLabels = emptyList(),
                validatedBlockedFeatureLabels = emptyList(),
                reviewRequiredFeatureLabels = emptyList(),
                pendingFeatureLabels = emptyList(),
                blockedFeatureLabels = emptyList(),
                sourceCompletenessWarnings = listOf("capture results not imported"),
                sourceCompletenessSummary = "capture results not imported",
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
            capturedFeatureCount = summary.capturedFeatureCount,
            pendingFeatureCount = summary.pendingFeatureCount,
            writeEnabledFeatureCount = summary.writeEnabledFeatureCount,
            validatedBlockedFeatureCount = summary.validatedBlockedFeatureCount,
            blockedFeatureCount = summary.blockedFeatureCount,
            writeEnabledFeatureLabels = summary.writeEnabledFeatureLabels,
            validatedBlockedFeatureLabels = summary.validatedBlockedFeatureLabels,
            reviewRequiredFeatureLabels = summary.reviewRequiredFeatureLabels,
            pendingFeatureLabels = summary.pendingFeatureLabels,
            blockedFeatureLabels = summary.blockedFeatureLabels,
            sourceCompletenessWarnings = summary.sourceCompletenessWarnings,
            sourceCompletenessSummary = summary.sourceCompletenessSummary,
            safetyPreviewText = summary.safetyPreviewText,
            sourceSummary = summary.sourceSummary,
            summary = summary.summary,
            displayText = summary.displayText,
        )
    }
}
