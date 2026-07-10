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
                usbState =
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
                displaySettings =
                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                        brightnessPercent = 64,
                        distanceCm = 90,
                        ipdMm = 67,
                        splendidMode = AirVisionSplendidMode.EyeCare,
                        blueLightFilterPercent = 35,
                        motionSyncEnabled = false,
                        threeDModeEnabled = true,
                    ),
            )

        assertTrue(markdown.startsWith("# AirVision M1 Firmware Protocol Capture Plan"))
        assertTrue(markdown.contains("- Capture readiness: writable HID report path detected"))
        assertTrue(markdown.contains("## Desired Firmware Sync"))
        assertTrue(markdown.contains("firmware sync: 7 Android-applied, 7 pending ASUS HID sync"))
        assertTrue(
            markdown.contains(
                "| Brightness | 64% | software HUD dimming | capture pending | " +
                    "Writable HID path detected, but ASUS vendor report payloads are not validated. |",
            ),
        )
        assertTrue(markdown.contains("| IPD | 67 mm | profile calibration | capture pending |"))
        assertTrue(markdown.contains("- Desired value: 35%"))
        assertTrue(markdown.contains("- Hardware sync: capture pending"))
        assertTrue(markdown.contains("| IPD | `ipd` | profile calibration stored | 60 mm -> 67 mm -> 72 mm |"))
        assertTrue(markdown.contains("out if=2 interrupt addr=0x2 max=64 int=1"))
        assertTrue(markdown.contains("in if=2 interrupt addr=0x81 max=32 int=4"))
        assertTrue(markdown.contains("- Feature count: ${AirVisionFirmwareFeature.entries.size}"))
        assertTrue(markdown.contains("## Capture Acceptance Criteria"))
        assertTrue(markdown.contains("exact Windows write report ID"))
        assertTrue(markdown.contains("sanitized payload summary"))
        assertTrue(markdown.contains("Keep raw bytes only in private capture files."))
        assertTrue(markdown.contains("checksum/framing"))
        assertTrue(markdown.contains("visible-state evidence"))
        assertTrue(markdown.contains("## Capture Result Template"))
        assertTrue(markdown.contains("Write payload summary"))
        assertTrue(markdown.contains("Readback payload summary"))
        assertTrue(markdown.contains("Android enablement decision"))
        assertFalse(markdown.contains("payload bytes"))
        assertTrue(markdown.contains("| Brightness | pending | pending | pending | pending | pending | pending | blocked |"))
        assertTrue(markdown.contains("| IPD | pending | pending | pending | pending | pending | pending | blocked |"))
    }

    @Test
    fun renderMarkdown_omitsRawUsbSerials() {
        val markdown =
            AirVisionFirmwareCapturePlans.renderMarkdown(
                usbState =
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

    @Test
    fun renderMarkdown_marksLightLoadLockedDesiredValues() {
        val markdown =
            AirVisionFirmwareCapturePlans.renderMarkdown(
                usbState = AirVisionUsbState(),
                displaySettings =
                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                        lightLoadModeEnabled = true,
                        splendidMode = AirVisionSplendidMode.Standard,
                        blueLightFilterPercent = 50,
                        threeDModeEnabled = true,
                    ),
            )

        assertTrue(markdown.contains("| IPD | 67 mm (locked by Light Load Mode) |"))
        assertTrue(markdown.contains("| Blue Light Filter | off (requires Eye Care) |"))
        assertTrue(markdown.contains("| 3D Mode | off (locked by Light Load Mode) |"))
        assertTrue(markdown.contains("- Hardware sync: waiting for writable HID"))
        assertTrue(
            markdown.contains(
                "- Blocked reason: Android has not exposed a writable AirVision HID report path.",
            ),
        )
    }
}
