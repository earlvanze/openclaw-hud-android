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
    val hudRuntime: AirVisionDiagnosticsHudRuntime,
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
    val hasReadableHidReports: Boolean,
    val hasWritableHidReports: Boolean,
    val hasInterruptReportPath: Boolean,
    val protocolCaptureReady: Boolean,
    val summary: String,
    val featureReadinessSummary: String,
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
data class AirVisionDiagnosticsDeviceInfo(
    val manufacturerName: String?,
    val productName: String?,
    val deviceName: String?,
    val vendorProduct: String?,
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

object AirVisionDiagnosticsSnapshots {
    const val SCHEMA = "openclaw.airvision.m1.diagnostics"
    const val VERSION = 7

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
            hudRuntime =
                AirVisionDiagnosticsHudRuntime(
                    transcriptEntryCount = AirVisionDisplaySettings.hudTranscriptEntryCount(displaySettings.lightLoadModeEnabled),
                    captionEntryCount = AirVisionDisplaySettings.hudCaptionEntryCount(displaySettings.lightLoadModeEnabled),
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
            hasReadableHidReports = hasReadableHidReports,
            hasWritableHidReports = hasWritableHidReports,
            hasInterruptReportPath = hasInterruptReportPath,
            protocolCaptureReady = protocolCaptureReady,
            summary = summary,
            featureReadinessSummary = featureReadinessSummary,
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
}
