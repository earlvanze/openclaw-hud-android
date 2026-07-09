package ai.openclaw.app

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
}
