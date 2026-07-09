package ai.openclaw.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AirVisionUsbState(
    val connected: Boolean = false,
    val permissionGranted: Boolean = false,
    val deviceLabel: String? = null,
    val vendorProduct: String? = null,
    val hidControlInterface: Boolean = false,
    val audioInterface: Boolean = false,
    val inputInterface: Boolean = false,
    val lastPermissionGranted: Boolean? = null,
) {
    val firmwareControlReady: Boolean
        get() = connected && permissionGranted && hidControlInterface

    val statusText: String
        get() =
            when {
                !connected -> "M1 USB device not detected."
                !permissionGranted -> "M1 detected; grant USB access to inspect firmware controls."
                hidControlInterface -> "M1 HID control interface detected. ASUS report protocol still pending."
                else -> "M1 detected, but no writable HID control interface was exposed."
            }
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
                hidControlInterface = device.hasHidControlInterface(),
                audioInterface = device.hasInterfaceClass(UsbConstants.USB_CLASS_AUDIO),
                inputInterface = device.hasInputOnlyHidInterface(),
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

private fun UsbDevice.hasInterfaceClass(interfaceClass: Int): Boolean =
    interfaces().any { it.interfaceClass == interfaceClass }

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

private fun UsbDevice.interfaces(): Sequence<UsbInterface> =
    (0 until interfaceCount).asSequence().map { getInterface(it) }

private fun UsbInterface.endpoints() =
    (0 until endpointCount).asSequence().map { getEndpoint(it) }
