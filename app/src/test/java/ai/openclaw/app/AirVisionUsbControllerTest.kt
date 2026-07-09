package ai.openclaw.app

import android.hardware.usb.UsbConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionUsbControllerTest {
    @Test
    fun isAirVisionM1Device_matchesKnownVendorAndProduct() {
        assertTrue(
            AirVisionUsbController.isAirVisionM1Device(
                vendorId = 0x0B05,
                productId = 0x1B3C,
                manufacturerName = "STMicroelectronics",
                productName = "ASUS AirVision M1",
                deviceName = "/dev/bus/usb/001/002",
            ),
        )
    }

    @Test
    fun isAirVisionM1Device_matchesHumanReadableNameFallback() {
        assertTrue(
            AirVisionUsbController.isAirVisionM1Device(
                vendorId = 1234,
                productId = 5678,
                manufacturerName = "ASUS",
                productName = "AirVision M1",
                deviceName = null,
            ),
        )
    }

    @Test
    fun isAirVisionM1Device_rejectsUnrelatedUsbDevice() {
        assertFalse(
            AirVisionUsbController.isAirVisionM1Device(
                vendorId = 0x18D1,
                productId = 0x4EE7,
                manufacturerName = "Google",
                productName = "Pixel",
                deviceName = "/dev/bus/usb/001/003",
            ),
        )
    }

    @Test
    fun statusText_reportsFirmwareReadinessSteps() {
        assertEquals("M1 USB device not detected.", AirVisionUsbState().statusText)
        assertEquals(
            "M1 detected; grant USB access to inspect firmware controls.",
            AirVisionUsbState(connected = true).statusText,
        )
        assertEquals(
            "M1 HID control interface detected. ASUS report protocol still pending.",
            AirVisionUsbState(
                connected = true,
                permissionGranted = true,
                hidControlInterface = true,
            ).statusText,
        )
        assertEquals(
            "M1 HID input reports detected. Writable firmware controls still need ASUS protocol support.",
            AirVisionUsbState(
                connected = true,
                permissionGranted = true,
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
                                        address = 0x81,
                                        direction = UsbConstants.USB_DIR_IN,
                                        type = UsbConstants.USB_ENDPOINT_XFER_INT,
                                        maxPacketSize = 32,
                                        interval = 8,
                                    ),
                                ),
                        ),
                    ),
            ).statusText,
        )
    }

    @Test
    fun diagnosticsText_summarizesInterfacesAndEndpoints() {
        val state =
            AirVisionUsbState(
                interfaces =
                    listOf(
                        AirVisionUsbInterfaceInfo(
                            id = 1,
                            interfaceClass = 3,
                            interfaceSubclass = 0,
                            interfaceProtocol = 0,
                            endpoints =
                                listOf(
                                    AirVisionUsbEndpointInfo(
                                        address = 1,
                                        direction = 0,
                                        type = 3,
                                        maxPacketSize = 64,
                                        interval = 1,
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(
            "if1 hid sub=0 proto=0: out/interrupt addr=0x1 max=64 int=1",
            state.diagnosticsText,
        )
    }

    @Test
    fun deviceInfoText_summarizesUsbIdentityAndFirmwareProtocolStatus() {
        val state =
            AirVisionUsbState(
                deviceInfo =
                    AirVisionUsbDeviceInfo(
                        manufacturerName = "ASUS",
                        productName = "AirVision M1",
                        deviceName = "/dev/bus/usb/001/002",
                        vendorProduct = "0x0b05:0x1b3c",
                        serialStatus = "grant USB access",
                    ),
            )

        assertEquals(
            """
            manufacturer: ASUS
            product: AirVision M1
            usb id: 0x0b05:0x1b3c
            device path: /dev/bus/usb/001/002
            serial: grant USB access
            firmware: pending ASUS HID protocol
            """.trimIndent(),
            state.deviceInfoText,
        )
    }

    @Test
    fun firmwareCapabilities_summarizeReadableAndWritableHidReportPaths() {
        val state =
            AirVisionUsbState(
                interfaces =
                    listOf(
                        AirVisionUsbInterfaceInfo(
                            id = 1,
                            interfaceClass = UsbConstants.USB_CLASS_HID,
                            interfaceSubclass = 0,
                            interfaceProtocol = 0,
                            endpoints =
                                listOf(
                                    AirVisionUsbEndpointInfo(
                                        address = 0x81,
                                        direction = UsbConstants.USB_DIR_IN,
                                        type = UsbConstants.USB_ENDPOINT_XFER_INT,
                                        maxPacketSize = 32,
                                        interval = 4,
                                    ),
                                ),
                        ),
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
            )

        val capabilities = state.firmwareCapabilities

        assertTrue(capabilities.hasReadableHidReports)
        assertTrue(capabilities.hasWritableHidReports)
        assertTrue(capabilities.hasInterruptReportPath)
        assertTrue(capabilities.protocolCaptureReady)
        assertEquals(listOf(1), capabilities.hidInputInterfaceIds)
        assertEquals(listOf(2), capabilities.hidOutputInterfaceIds)
        assertEquals(32, capabilities.maxInputPacketSize)
        assertEquals(64, capabilities.maxOutputPacketSize)
        assertEquals(
            listOf("in if=1 interrupt addr=0x81 max=32 int=4"),
            capabilities.readableReportPaths.map { it.summary },
        )
        assertEquals(
            listOf("out if=2 interrupt addr=0x2 max=64 int=1"),
            capabilities.writableReportPaths.map { it.summary },
        )
        assertEquals(
            """
            firmware reports: readable: in if=1 interrupt addr=0x81 max=32 int=4, writable: out if=2 interrupt addr=0x2 max=64 int=1, interrupt out=1, interrupt in=1, max out=64, max in=32
            """.trimIndent(),
            capabilities.summary,
        )
        assertEquals(AirVisionFirmwareFeature.entries.size, capabilities.featureReadiness.size)
        assertEquals(
            "Brightness: ASUS HID protocol capture pending",
            capabilities.featureReadiness.first().summary,
        )
        assertTrue(capabilities.featureReadiness.all { !it.firmwareApplyReady })
        assertTrue(capabilities.featureReadiness.all { it.detail.contains("writable HID path detected") })
        assertEquals(
            "firmware apply: Brightness: ASUS HID protocol capture pending; " +
                "Screen distance: ASUS HID protocol capture pending; " +
                "IPD: ASUS HID protocol capture pending; " +
                "Splendid: ASUS HID protocol capture pending; " +
                "Blue Light Filter: ASUS HID protocol capture pending; " +
                "Motion Sync: ASUS HID protocol capture pending; " +
                "3D Mode: ASUS HID protocol capture pending",
            capabilities.featureReadinessSummary,
        )
    }

    @Test
    fun firmwareCapabilities_reportNoHidReportEndpoints() {
        val state =
            AirVisionUsbState(
                interfaces =
                    listOf(
                        AirVisionUsbInterfaceInfo(
                            id = 3,
                            interfaceClass = UsbConstants.USB_CLASS_AUDIO,
                            interfaceSubclass = 1,
                            interfaceProtocol = 0,
                            endpoints = emptyList(),
                        ),
                    ),
            )

        assertFalse(state.firmwareCapabilities.protocolCaptureReady)
        assertEquals("firmware reports: no HID report endpoints exposed", state.firmwareCapabilities.summary)
        assertTrue(
            state.firmwareCapabilities.featureReadiness.all {
                it.firmwareApplyStatus == "waiting for writable HID report path"
            },
        )
    }
}
