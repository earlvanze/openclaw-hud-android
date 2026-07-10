package ai.openclaw.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AirVisionUsbEndpointInfo(
    val address: Int,
    val direction: Int,
    val type: Int,
    val maxPacketSize: Int,
    val interval: Int,
) {
    val directionLabel: String
        get() =
            when (direction) {
                UsbConstants.USB_DIR_IN -> "in"
                UsbConstants.USB_DIR_OUT -> "out"
                else -> "0x${direction.toString(16)}"
            }

    val typeLabel: String
        get() =
            when (type) {
                UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "control"
                UsbConstants.USB_ENDPOINT_XFER_ISOC -> "isoc"
                UsbConstants.USB_ENDPOINT_XFER_BULK -> "bulk"
                UsbConstants.USB_ENDPOINT_XFER_INT -> "interrupt"
                else -> "type-$type"
            }

    val summary: String
        get() = "$directionLabel/$typeLabel addr=0x${address.toString(16)} max=$maxPacketSize int=$interval"
}

data class AirVisionUsbInterfaceInfo(
    val id: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpoints: List<AirVisionUsbEndpointInfo>,
) {
    val classLabel: String
        get() = usbClassLabel(interfaceClass)

    val summary: String
        get() {
            val endpointText =
                endpoints
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("; ") { it.summary }
                    ?: "no endpoints"
            return "if$id $classLabel sub=$interfaceSubclass proto=$interfaceProtocol: $endpointText"
        }
}

data class AirVisionFirmwareReportPath(
    val interfaceId: Int,
    val endpointAddress: Int,
    val direction: Int,
    val type: Int,
    val maxPacketSize: Int,
    val interval: Int,
) {
    val directionLabel: String
        get() =
            when (direction) {
                UsbConstants.USB_DIR_IN -> "in"
                UsbConstants.USB_DIR_OUT -> "out"
                else -> "0x${direction.toString(16)}"
            }

    val typeLabel: String
        get() =
            when (type) {
                UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "control"
                UsbConstants.USB_ENDPOINT_XFER_ISOC -> "isoc"
                UsbConstants.USB_ENDPOINT_XFER_BULK -> "bulk"
                UsbConstants.USB_ENDPOINT_XFER_INT -> "interrupt"
                else -> "type-$type"
            }

    val summary: String
        get() = "$directionLabel if=$interfaceId $typeLabel addr=0x${endpointAddress.toString(16)} max=$maxPacketSize int=$interval"
}

enum class AirVisionFirmwareFeature(
    val rawValue: String,
    val label: String,
    val androidStatus: String,
    val captureProbeValues: List<String>,
    val requiresWritableHid: Boolean = true,
) {
    Brightness(
        rawValue = "brightness",
        label = "Brightness",
        androidStatus = "software HUD dimming active",
        captureProbeValues = listOf("20%", "50%", "80%"),
    ),
    ScreenDistance(
        rawValue = "screen_distance",
        label = "Screen distance",
        androidStatus = "virtual HUD distance scaling active",
        captureProbeValues = listOf("50 cm", "100 cm", "150 cm"),
    ),
    Ipd(
        rawValue = "ipd",
        label = "IPD",
        androidStatus = "profile calibration stored",
        captureProbeValues = listOf("60 mm", "67 mm", "72 mm"),
    ),
    Splendid(
        rawValue = "splendid",
        label = "Splendid",
        androidStatus = "HUD color preview active",
        captureProbeValues = listOf("standard", "theater", "eye_care"),
    ),
    BlueLightFilter(
        rawValue = "blue_light_filter",
        label = "Blue Light Filter",
        androidStatus = "Eye Care warm overlay active",
        captureProbeValues = listOf("0%", "50%", "100%"),
    ),
    MotionSync(
        rawValue = "motion_sync",
        label = "Motion Sync",
        androidStatus = "profile preference stored",
        captureProbeValues = listOf("off", "on"),
    ),
    ThreeDMode(
        rawValue = "3d_mode",
        label = "3D Mode",
        androidStatus = "profile preference stored",
        captureProbeValues = listOf("off", "on"),
    ),
}

data class AirVisionFirmwareFeatureReadiness(
    val feature: AirVisionFirmwareFeature,
    val androidStatus: String,
    val firmwareApplyReady: Boolean,
    val firmwareApplyStatus: String,
    val detail: String,
) {
    val summary: String
        get() = "${feature.label}: $firmwareApplyStatus"
}

data class AirVisionFirmwareCaptureTarget(
    val feature: AirVisionFirmwareFeature,
    val captureReady: Boolean,
    val writeReportPathSummaries: List<String>,
    val readReportPathSummaries: List<String>,
    val suggestedProbeValues: List<String>,
    val instruction: String,
) {
    val summary: String
        get() =
            if (captureReady) {
                "${feature.label}: capture ${suggestedProbeValues.joinToString(" -> ")} on ${writeReportPathSummaries.joinToString()}"
            } else {
                "${feature.label}: waiting for writable HID report path"
            }
}

data class AirVisionFirmwareCapabilities(
    val hidInputInterfaceIds: List<Int> = emptyList(),
    val hidOutputInterfaceIds: List<Int> = emptyList(),
    val interruptInputEndpoints: Int = 0,
    val interruptOutputEndpoints: Int = 0,
    val maxInputPacketSize: Int? = null,
    val maxOutputPacketSize: Int? = null,
    val readableReportPaths: List<AirVisionFirmwareReportPath> = emptyList(),
    val writableReportPaths: List<AirVisionFirmwareReportPath> = emptyList(),
) {
    val hasReadableHidReports: Boolean
        get() = readableReportPaths.isNotEmpty()

    val hasWritableHidReports: Boolean
        get() = writableReportPaths.isNotEmpty()

    val hasInterruptReportPath: Boolean
        get() = interruptInputEndpoints > 0 || interruptOutputEndpoints > 0

    val protocolCaptureReady: Boolean
        get() = hasReadableHidReports || hasWritableHidReports

    val featureReadiness: List<AirVisionFirmwareFeatureReadiness>
        get() = AirVisionFirmwareFeature.entries.map { it.readinessFor(this) }

    val featureReadinessSummary: String
        get() = "firmware apply: ${featureReadiness.joinToString("; ") { it.summary }}"

    val captureTargets: List<AirVisionFirmwareCaptureTarget>
        get() = AirVisionFirmwareFeature.entries.map { it.captureTargetFor(this) }

    val capturePlanSummary: String
        get() = "firmware capture: ${captureTargets.joinToString("; ") { it.summary }}"

    val summary: String
        get() {
            if (!protocolCaptureReady) return "firmware reports: no HID report endpoints exposed"
            val parts =
                listOfNotNull(
                    readableReportPaths.takeIf { it.isNotEmpty() }?.joinToString(prefix = "readable: ") { it.summary },
                    writableReportPaths.takeIf { it.isNotEmpty() }?.joinToString(prefix = "writable: ") { it.summary },
                    interruptOutputEndpoints.takeIf { it > 0 }?.let { "interrupt out=$it" },
                    interruptInputEndpoints.takeIf { it > 0 }?.let { "interrupt in=$it" },
                    maxOutputPacketSize?.let { "max out=$it" },
                    maxInputPacketSize?.let { "max in=$it" },
                )
            return "firmware reports: ${parts.joinToString(", ")}"
        }
}

private fun AirVisionFirmwareFeature.captureTargetFor(capabilities: AirVisionFirmwareCapabilities): AirVisionFirmwareCaptureTarget {
    val writePaths = capabilities.writableReportPaths.map { it.summary }
    val readPaths = capabilities.readableReportPaths.map { it.summary }
    val captureReady = capabilities.hasWritableHidReports
    val instruction =
        if (captureReady) {
            "Capture Windows AirVision USB traffic while changing $label through ${captureProbeValues.joinToString(" -> ")}. " +
                "Keep Android read-only until the vendor report payload and checksum behavior are validated."
        } else {
            "Reconnect the AirVision M1 over USB and grant permission until Android exposes a writable HID report path."
        }
    return AirVisionFirmwareCaptureTarget(
        feature = this,
        captureReady = captureReady,
        writeReportPathSummaries = writePaths,
        readReportPathSummaries = readPaths,
        suggestedProbeValues = captureProbeValues,
        instruction = instruction,
    )
}

private fun AirVisionFirmwareFeature.readinessFor(capabilities: AirVisionFirmwareCapabilities): AirVisionFirmwareFeatureReadiness {
    val firmwareApplyStatus =
        when {
            !requiresWritableHid -> "not required"
            capabilities.hasWritableHidReports -> "ASUS HID protocol capture pending"
            else -> "waiting for writable HID report path"
        }
    val detail =
        when {
            !requiresWritableHid -> "$androidStatus; no firmware write path required."
            capabilities.hasWritableHidReports ->
                "$androidStatus; writable HID path detected, but ASUS vendor report payload is not validated yet."
            else ->
                "$androidStatus; no writable HID report path has been exposed to Android yet."
        }
    return AirVisionFirmwareFeatureReadiness(
        feature = this,
        androidStatus = androidStatus,
        firmwareApplyReady = false,
        firmwareApplyStatus = firmwareApplyStatus,
        detail = detail,
    )
}

data class AirVisionUsbDeviceInfo(
    val manufacturerName: String? = null,
    val productName: String? = null,
    val deviceName: String? = null,
    val vendorProduct: String? = null,
    val deviceClass: Int? = null,
    val deviceSubclass: Int? = null,
    val deviceProtocol: Int? = null,
    val interfaceCount: Int? = null,
    val serialNumber: String? = null,
    val serialStatus: String? = null,
    val firmwareVersion: String? = null,
) {
    val deviceClassSummary: String?
        get() =
            deviceClass?.let { classValue ->
                buildList {
                    add("${usbClassLabel(classValue)} class=$classValue")
                    deviceSubclass?.let { add("sub=$it") }
                    deviceProtocol?.let { add("proto=$it") }
                }.joinToString(" ")
            }

    val summary: String
        get() =
            listOfNotNull(
                manufacturerName?.takeIf { it.isNotBlank() }?.let { "manufacturer: $it" },
                productName?.takeIf { it.isNotBlank() }?.let { "product: $it" },
                vendorProduct?.takeIf { it.isNotBlank() }?.let { "usb id: $it" },
                deviceName?.takeIf { it.isNotBlank() }?.let { "device path: $it" },
                deviceClassSummary?.let { "device class: $it" },
                interfaceCount?.let { "interfaces: $it" },
                serialNumber?.takeIf { it.isNotBlank() }?.let { "serial: $it" }
                    ?: serialStatus?.takeIf { it.isNotBlank() }?.let { "serial: $it" },
                "firmware/version: ${firmwareVersion?.takeIf { it.isNotBlank() } ?: "pending ASUS HID protocol"}",
            ).joinToString("\n")
}

data class AirVisionUsbState(
    val connected: Boolean = false,
    val permissionGranted: Boolean = false,
    val deviceLabel: String? = null,
    val vendorProduct: String? = null,
    val deviceInfo: AirVisionUsbDeviceInfo = AirVisionUsbDeviceInfo(),
    val hidControlInterface: Boolean = false,
    val audioInterface: Boolean = false,
    val inputInterface: Boolean = false,
    val interfaces: List<AirVisionUsbInterfaceInfo> = emptyList(),
    val lastPermissionGranted: Boolean? = null,
) {
    val firmwareControlReady: Boolean
        get() = connected && permissionGranted && hidControlInterface

    val firmwareCapabilities: AirVisionFirmwareCapabilities
        get() = interfaces.airVisionFirmwareCapabilities()

    val statusText: String
        get() =
            when {
                !connected -> "M1 USB device not detected."
                !permissionGranted -> "M1 detected; grant USB access to inspect firmware controls."
                hidControlInterface -> "M1 HID control interface detected. ASUS report protocol still pending."
                firmwareCapabilities.hasReadableHidReports ->
                    "M1 HID input reports detected. Writable firmware controls still need ASUS protocol support."
                else -> "M1 detected, but no writable HID control interface was exposed."
            }

    val diagnosticsText: String
        get() =
            interfaces
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { it.summary }
                ?: "No USB interface descriptors captured."

    val deviceInfoText: String
        get() =
            deviceInfo.summary
                .takeIf { it.isNotBlank() }
                ?: "No USB device information captured."
}

class AirVisionUsbController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val usbManager: UsbManager? = appContext.getSystemService(UsbManager::class.java)
    private val started = AtomicBoolean(false)
    private val permissionAction = "${appContext.packageName}.AIRVISION_USB_PERMISSION"
    private val _state = MutableStateFlow(AirVisionUsbState())
    val state: StateFlow<AirVisionUsbState> = _state

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    permissionAction -> {
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        refresh(lastPermissionGranted = granted)
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    -> refresh()
                }
            }
        }

    fun start() {
        if (!started.compareAndSet(false, true)) {
            refresh()
            return
        }
        val filter =
            IntentFilter().apply {
                addAction(permissionAction)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refresh()
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        runCatching { appContext.unregisterReceiver(receiver) }
    }

    fun refresh() {
        refresh(lastPermissionGranted = _state.value.lastPermissionGranted)
    }

    fun requestPermission() {
        val manager = usbManager ?: return
        val device = findAirVisionDevice(manager) ?: return
        if (manager.hasPermission(device)) {
            refresh(lastPermissionGranted = true)
            return
        }
        manager.requestPermission(device, permissionIntent())
        refresh()
    }

    private fun refresh(lastPermissionGranted: Boolean? = null) {
        val manager = usbManager
        val device = manager?.let(::findAirVisionDevice)
        if (manager == null || device == null) {
            _state.value = AirVisionUsbState(lastPermissionGranted = lastPermissionGranted)
            return
        }

        val permissionGranted = manager.hasPermission(device)
        _state.value =
            AirVisionUsbState(
                connected = true,
                permissionGranted = permissionGranted,
                deviceLabel = device.bestLabel(),
                vendorProduct = device.vendorProductLabel(),
                deviceInfo = device.airVisionDeviceInfo(permissionGranted),
                hidControlInterface = device.hasHidControlInterface(),
                audioInterface = device.hasInterfaceClass(UsbConstants.USB_CLASS_AUDIO),
                inputInterface = device.hasInputOnlyHidInterface(),
                interfaces = device.airVisionInterfaceInfo(),
                lastPermissionGranted = lastPermissionGranted,
            )
    }

    private fun permissionIntent(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(permissionAction).setPackage(appContext.packageName),
            flags,
        )
    }

    private fun findAirVisionDevice(manager: UsbManager): UsbDevice? =
        manager.deviceList.values.firstOrNull { device ->
            isAirVisionM1Device(
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturerName = device.manufacturerName,
                productName = device.productName,
                deviceName = device.deviceName,
            )
        }

    companion object {
        const val ASUS_VENDOR_ID = 0x0B05
        const val AIRVISION_M1_PRODUCT_ID = 0x1B3C

        fun isAirVisionM1Device(
            vendorId: Int,
            productId: Int,
            manufacturerName: String?,
            productName: String?,
            deviceName: String?,
        ): Boolean {
            if (vendorId == ASUS_VENDOR_ID && productId == AIRVISION_M1_PRODUCT_ID) return true
            val label = listOfNotNull(manufacturerName, productName, deviceName).joinToString(" ")
            return label.contains("AirVision", ignoreCase = true) ||
                (label.contains("ASUS", ignoreCase = true) && label.contains("M1", ignoreCase = true))
        }
    }
}

private fun UsbDevice.bestLabel(): String =
    listOfNotNull(manufacturerName, productName)
        .joinToString(" ")
        .trim()
        .ifEmpty { deviceName }

private fun UsbDevice.vendorProductLabel(): String =
    "0x${vendorId.toString(16).padStart(4, '0')}:0x${productId.toString(16).padStart(4, '0')}"

private fun UsbDevice.airVisionDeviceInfo(permissionGranted: Boolean): AirVisionUsbDeviceInfo {
    val serialNumber =
        if (permissionGranted) {
            runCatching { serialNumber?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()
        } else {
            null
        }
    val serialStatus =
        when {
            serialNumber != null -> null
            permissionGranted -> "not exposed"
            else -> "grant USB access"
        }
    return AirVisionUsbDeviceInfo(
        manufacturerName = manufacturerName,
        productName = productName,
        deviceName = deviceName,
        vendorProduct = vendorProductLabel(),
        deviceClass = deviceClass,
        deviceSubclass = deviceSubclass,
        deviceProtocol = deviceProtocol,
        interfaceCount = interfaceCount,
        serialNumber = serialNumber,
        serialStatus = serialStatus,
        firmwareVersion = usbDescriptorVersion(),
    )
}

private fun UsbDevice.usbDescriptorVersion(): String? =
    runCatching {
        version
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { "USB descriptor $it" }
    }.getOrNull()

private fun usbClassLabel(interfaceClass: Int): String =
    when (interfaceClass) {
        UsbConstants.USB_CLASS_AUDIO -> "audio"
        UsbConstants.USB_CLASS_HID -> "hid"
        UsbConstants.USB_CLASS_HUB -> "hub"
        UsbConstants.USB_CLASS_MASS_STORAGE -> "storage"
        UsbConstants.USB_CLASS_PER_INTERFACE -> "per-interface"
        UsbConstants.USB_CLASS_VENDOR_SPEC -> "vendor"
        else -> "class-$interfaceClass"
    }

private fun UsbDevice.hasInterfaceClass(interfaceClass: Int): Boolean = interfaces().any { it.interfaceClass == interfaceClass }

private fun UsbDevice.hasHidControlInterface(): Boolean =
    interfaces().any { usbInterface ->
        usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID &&
            usbInterface.endpoints().any { it.direction == UsbConstants.USB_DIR_OUT }
    }

private fun UsbDevice.hasInputOnlyHidInterface(): Boolean =
    interfaces().any { usbInterface ->
        usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID &&
            usbInterface.endpoints().any { it.direction == UsbConstants.USB_DIR_IN } &&
            usbInterface.endpoints().none { it.direction == UsbConstants.USB_DIR_OUT }
    }

private fun UsbDevice.interfaces(): Sequence<UsbInterface> = (0 until interfaceCount).asSequence().map { getInterface(it) }

private fun UsbInterface.endpoints() = (0 until endpointCount).asSequence().map { getEndpoint(it) }

private fun UsbDevice.airVisionInterfaceInfo(): List<AirVisionUsbInterfaceInfo> =
    interfaces()
        .map { usbInterface ->
            AirVisionUsbInterfaceInfo(
                id = usbInterface.id,
                interfaceClass = usbInterface.interfaceClass,
                interfaceSubclass = usbInterface.interfaceSubclass,
                interfaceProtocol = usbInterface.interfaceProtocol,
                endpoints = usbInterface.endpoints().map { it.airVisionEndpointInfo() }.toList(),
            )
        }.toList()

private fun UsbEndpoint.airVisionEndpointInfo(): AirVisionUsbEndpointInfo =
    AirVisionUsbEndpointInfo(
        address = address,
        direction = direction,
        type = type,
        maxPacketSize = maxPacketSize,
        interval = interval,
    )

fun List<AirVisionUsbInterfaceInfo>.airVisionFirmwareCapabilities(): AirVisionFirmwareCapabilities {
    val hidInterfaces = filter { it.interfaceClass == UsbConstants.USB_CLASS_HID }
    val inputPaths = hidInterfaces.reportPathsFor(direction = UsbConstants.USB_DIR_IN)
    val outputPaths = hidInterfaces.reportPathsFor(direction = UsbConstants.USB_DIR_OUT)
    return AirVisionFirmwareCapabilities(
        hidInputInterfaceIds = inputPaths.map { it.interfaceId }.distinct(),
        hidOutputInterfaceIds = outputPaths.map { it.interfaceId }.distinct(),
        interruptInputEndpoints = inputPaths.count { it.type == UsbConstants.USB_ENDPOINT_XFER_INT },
        interruptOutputEndpoints = outputPaths.count { it.type == UsbConstants.USB_ENDPOINT_XFER_INT },
        maxInputPacketSize = inputPaths.maxOfOrNull { it.maxPacketSize },
        maxOutputPacketSize = outputPaths.maxOfOrNull { it.maxPacketSize },
        readableReportPaths = inputPaths,
        writableReportPaths = outputPaths,
    )
}

private fun List<AirVisionUsbInterfaceInfo>.reportPathsFor(direction: Int): List<AirVisionFirmwareReportPath> =
    flatMap { usbInterface ->
        usbInterface.endpoints
            .filter { it.direction == direction }
            .map { endpoint ->
                AirVisionFirmwareReportPath(
                    interfaceId = usbInterface.id,
                    endpointAddress = endpoint.address,
                    direction = endpoint.direction,
                    type = endpoint.type,
                    maxPacketSize = endpoint.maxPacketSize,
                    interval = endpoint.interval,
                )
            }
    }
