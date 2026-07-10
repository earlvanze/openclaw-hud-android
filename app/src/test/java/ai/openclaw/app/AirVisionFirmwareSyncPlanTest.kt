package ai.openclaw.app

import android.hardware.usb.UsbConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionFirmwareSyncPlanTest {
    @Test
    fun fromSettings_tracksDesiredValuesAndCapturePendingHardwareSync() {
        val settings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                brightnessPercent = 64,
                distanceCm = 90,
                ipdMm = 67,
                splendidMode = AirVisionSplendidMode.EyeCare,
                blueLightFilterPercent = 35,
                motionSyncEnabled = false,
                threeDModeEnabled = true,
            )
        val capabilities =
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
            ).airVisionFirmwareCapabilities()

        val plan = AirVisionFirmwareSyncPlans.fromSettings(settings, capabilities)

        assertEquals(AirVisionFirmwareFeature.entries.size, plan.items.size)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.pendingHardwareSyncCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.androidAppliedCount)
        assertEquals("firmware sync: 7 Android-applied, 7 pending ASUS HID sync", plan.summary)
        assertEquals(
            "Brightness=64% (capture pending)",
            plan.items.first { it.feature == AirVisionFirmwareFeature.Brightness }.summary,
        )
        assertEquals(
            "Eye Care",
            plan.items.first { it.feature == AirVisionFirmwareFeature.Splendid }.desiredValue,
        )
        assertEquals(
            "35%",
            plan.items.first { it.feature == AirVisionFirmwareFeature.BlueLightFilter }.desiredValue,
        )
        assertEquals(
            "on",
            plan.items.first { it.feature == AirVisionFirmwareFeature.ThreeDMode }.desiredValue,
        )
        assertTrue(plan.items.all { it.blockedReason.contains("vendor report payloads are not validated") })
    }

    @Test
    fun fromSettings_marksLightLoadLocksAndMissingWritableHidPath() {
        val settings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                lightLoadModeEnabled = true,
                threeDModeEnabled = true,
                splendidMode = AirVisionSplendidMode.Standard,
                blueLightFilterPercent = 50,
            ).normalized

        val plan = AirVisionFirmwareSyncPlans.fromSettings(settings, AirVisionFirmwareCapabilities())

        assertEquals(
            "67 mm (locked by Light Load Mode)",
            plan.items.first { it.feature == AirVisionFirmwareFeature.Ipd }.desiredValue,
        )
        assertEquals(
            "off (requires Eye Care)",
            plan.items.first { it.feature == AirVisionFirmwareFeature.BlueLightFilter }.desiredValue,
        )
        assertEquals(
            "off (locked by Light Load Mode)",
            plan.items.first { it.feature == AirVisionFirmwareFeature.ThreeDMode }.desiredValue,
        )
        assertTrue(plan.items.all { it.hardwareSyncStatus == "waiting for writable HID" })
    }
}
