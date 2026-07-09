package ai.openclaw.app

import android.hardware.usb.UsbConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionFirmwareCapturePlanTest {
    @Test
    fun renderMarkdown_exportsProbeValuesAndCurrentReportPaths() {
        val markdown =
            AirVisionFirmwareCapturePlans.renderMarkdown(
                AirVisionUsbState(
                    connected = true,
                    permissionGranted = true,
                    deviceLabel = "ASUS AirVision M1",
                    vendorProduct = "0x0b05:0x1b3c",
                    hidControlInterface = true,
                    audioInterface = true,
                    inputInterface = true,
                    interfaces =
                        listOf(
                            AirVisionUsbInterfaceInfo(
                                id = 2,
                                interfaceClass = UsbConstants.USB_CLASS_HID,
                                interfaceSubclass = 0,
                                interfaceProtocol = 0,
                                endpoints =
                                    listOf(
                                        AirVisionUsbEndpointInfo(
                                            address = 0x02,
                                            direction = UsbConstants.USB_DIR_OUT,
                                            type = UsbConstants.USB_ENDPOINT_XFER_INT,
                                            maxPacketSize = 64,
                                            interval = 1,
                                        ),
                                        AirVisionUsbEndpointInfo(
                                            address = 0x81,
                                            direction = UsbConstants.USB_DIR_IN,
                                            type = UsbConstants.USB_ENDPOINT_XFER_INT,
                                            maxPacketSize = 32,
                                            interval = 4,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertTrue(markdown.startsWith("# AirVision M1 Firmware Protocol Capture Plan"))
        assertTrue(markdown.contains("- Capture readiness: writable HID report path detected"))
        assertTrue(markdown.contains("| IPD | `ipd` | profile calibration stored | 60 mm -> 67 mm -> 72 mm |"))
        assertTrue(markdown.contains("out if=2 interrupt addr=0x2 max=64 int=1"))
        assertTrue(markdown.contains("in if=2 interrupt addr=0x81 max=32 int=4"))
        assertTrue(markdown.contains("- Feature count: ${AirVisionFirmwareFeature.entries.size}"))
    }

    @Test
    fun renderMarkdown_omitsRawUsbSerials() {
        val markdown =
            AirVisionFirmwareCapturePlans.renderMarkdown(
                AirVisionUsbState(
                    connected = true,
                    permissionGranted = true,
                    deviceLabel = "ASUS AirVision M1",
                    vendorProduct = "0x0b05:0x1b3c",
                    deviceInfo =
                        AirVisionUsbDeviceInfo(
                            manufacturerName = "ASUS",
                            productName = "AirVision M1",
                            serialNumber = "private-device-serial",
                        ),
                ),
            )

        assertFalse(markdown.contains("private-device-serial"))
        assertTrue(markdown.contains("- Device: ASUS AirVision M1"))
        assertTrue(markdown.contains("- USB ID: 0x0b05:0x1b3c"))
    }
}
