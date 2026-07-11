package ai.openclaw.app

import android.hardware.usb.UsbConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionFirmwareUpdateHandoffTest {
    @Test
    fun renderMarkdown_exportsWindowsUpdateWorkflowWithoutRawSerial() {
        val markdown =
            AirVisionFirmwareUpdateHandoffs.renderMarkdown(
                AirVisionUsbState(
                    connected = true,
                    permissionGranted = true,
                    deviceLabel = "ASUS AirVision M1",
                    vendorProduct = "0x0b05:0x1b3c",
                    deviceInfo =
                        AirVisionUsbDeviceInfo(
                            manufacturerName = "ASUS",
                            productName = "AirVision M1",
                            deviceName = "/dev/bus/usb/001/002",
                            vendorProduct = "0x0b05:0x1b3c",
                            interfaceCount = 4,
                            serialNumber = "private-device-serial",
                            firmwareVersion = "USB descriptor 1.02",
                        ),
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
                                    ),
                            ),
                        ),
                ),
            )

        assertTrue(markdown.startsWith("# AirVision M1 Firmware Update Handoff"))
        assertTrue(markdown.contains("ASUS AirVision app firmware-update session"))
        assertTrue(markdown.contains("- Connected: yes"))
        assertTrue(markdown.contains("- USB ID: 0x0b05:0x1b3c"))
        assertTrue(markdown.contains("- Manufacturer: ASUS"))
        assertTrue(markdown.contains("- Interface count: 4"))
        assertTrue(markdown.contains("- Serial status: available"))
        assertTrue(markdown.contains("- Android-visible firmware/version context: USB descriptor 1.02"))
        assertTrue(markdown.contains("- HID control interface: yes"))
        assertTrue(markdown.contains("- Audio interface: yes"))
        assertTrue(markdown.contains("- Input interface: yes"))
        assertTrue(markdown.contains("writable: out if=2 interrupt addr=0x2 max=64 int=1"))
        assertTrue(markdown.contains("Connect the AirVision M1 directly to the Windows/Cyber machine"))
        assertTrue(markdown.contains("Use the ASUS AirVision Windows app to check for and install firmware updates."))
        assertTrue(markdown.contains("Compare the pre/post Android-visible USB descriptor"))
        assertTrue(markdown.contains("https://www.asus.com/support/faq/1054069/"))
        assertTrue(markdown.contains("https://www.asus.com/displays-desktops/glasses/airvision/asus-airvision-m1"))
        assertTrue(markdown.contains("intentionally omits raw USB serial numbers"))
        assertFalse(markdown.contains("private-device-serial"))
    }

    @Test
    fun renderMarkdown_marksMissingVersionAsPending() {
        val markdown = AirVisionFirmwareUpdateHandoffs.renderMarkdown(AirVisionUsbState())

        assertTrue(markdown.contains("- Connected: no"))
        assertTrue(markdown.contains("- Device: not detected"))
        assertTrue(markdown.contains("- USB ID: not detected"))
        assertTrue(markdown.contains("- Serial status: not captured"))
        assertTrue(markdown.contains("- Android-visible firmware/version context: pending ASUS HID protocol"))
        assertTrue(markdown.contains("- Status: M1 USB device not detected."))
    }
}
