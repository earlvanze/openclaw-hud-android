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
    val hudRuntime: AirVisionDiagnosticsHudRuntime,
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
    val summary: String,
    val writeGateSummary: String,
    val detailSummary: String,
    val items: List<AirVisionDiagnosticsFirmwareSyncItem>,
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
data class AirVisionDiagnosticsWindowsCompatibility(
    val cursorFollowAvailable: Boolean,
    val centerCursorAvailable: Boolean,
    val threeDofAvailable: Boolean,
    val distanceHotkeyMapped: Boolean,
    val hardwareTouchpadPassthrough: Boolean,
    val summary: String,
    val limitations: List<String>,
)

object AirVisionDiagnosticsSnapshots {
    const val SCHEMA = "openclaw.airvision.m1.diagnostics"
    const val VERSION = 14

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
    ): AirVisionDiagnosticsSnapshot =
        AirVisionDiagnosticsSnapshot(
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
            hudRuntime =
                AirVisionDiagnosticsHudRuntime(
                    transcriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(displaySettings.lightLoadModeEnabled),
                    captionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(displaySettings.lightLoadModeEnabled),
                    effectiveHudScalePercent =
                        (
                            AirVisionDisplaySettings.hudScaleForDistanceCm(displaySettings.distanceCm) *
                                AirVisionDisplaySettings.hudScaleMultiplierForViewMode(displaySettings.viewMode) *
                                AirVisionDisplaySettings.hudScaleMultiplierForPercent(displaySettings.hudScalePercent) *
                                100f
                        ).toInt(),
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
            windowsCompatibility =
                AirVisionDiagnosticsWindowsCompatibility(
                    cursorFollowAvailable = false,
                    centerCursorAvailable = false,
                    threeDofAvailable = false,
                    distanceHotkeyMapped = hudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance,
                    hardwareTouchpadPassthrough = true,
                    summary =
                        if (hudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) {
                            "Android maps virtual-distance adjustment to M1 brightness key events; Windows cursor-follow, center-cursor, and 3DoF remain unavailable on Android."
                        } else {
                            "Windows cursor-follow, center-cursor, and 3DoF remain unavailable on Android; M1 touchpad brightness/media behavior can still pass through firmware."
                        },
                    limitations =
                        listOf(
                            "Cursor Follow requires Windows AirVision line-of-sight cursor control.",
                            "Center Cursor requires Windows virtual-screen cursor ownership.",
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
            summary = summary,
            writeGateSummary = writeGateSummary,
            detailSummary = detailSummary,
            items = items.map { it.toDiagnostics() },
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
}
