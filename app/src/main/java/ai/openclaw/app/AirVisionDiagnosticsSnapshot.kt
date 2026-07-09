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
    val hasReadableHidReports: Boolean,
    val hasWritableHidReports: Boolean,
    val hasInterruptReportPath: Boolean,
    val protocolCaptureReady: Boolean,
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

object AirVisionDiagnosticsSnapshots {
    const val SCHEMA = "openclaw.airvision.m1.diagnostics"
    const val VERSION = 2

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
            hasReadableHidReports = hasReadableHidReports,
            hasWritableHidReports = hasWritableHidReports,
            hasInterruptReportPath = hasInterruptReportPath,
            protocolCaptureReady = protocolCaptureReady,
            summary = summary,
        )
}
