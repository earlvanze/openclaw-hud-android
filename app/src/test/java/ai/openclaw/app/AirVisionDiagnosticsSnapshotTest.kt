package ai.openclaw.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionDiagnosticsSnapshotTest {
    @Test
    fun fromState_exportsUsbDescriptorsAndCurrentAirVisionSettings() {
        val snapshot =
            AirVisionDiagnosticsSnapshots.fromState(
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
                                deviceName = "/dev/bus/usb/001/002",
                                vendorProduct = "0x0b05:0x1b3c",
                                serialNumber = "private-device-serial",
                            ),
                        hidControlInterface = true,
                        audioInterface = true,
                        inputInterface = true,
                        interfaces =
                            listOf(
                                AirVisionUsbInterfaceInfo(
                                    id = 2,
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
                    ),
                displaySettings =
                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                        splendidMode = AirVisionSplendidMode.EyeCare,
                        brightnessPercent = 72,
                        blueLightFilterPercent = 40,
                        ipdMm = 67,
                    ),
                hudControls =
                    AirVisionHudControls(
                        singleTapAction = AirVisionHudTouchAction.DismissNotification,
                        doubleTapAction = AirVisionHudDoubleTapAction.ToggleMic,
                        swipeAction = AirVisionHudSwipeAction.ScrollChat,
                    ),
                appLanguage = AirVisionAppLanguage.Spanish,
                startupDestination = AirVisionStartupDestination.Hud,
                hudDisplayTarget = AirVisionHudDisplayTarget.AirVisionPreferred,
                demoModeEnabled = true,
            )

        val encoded = AirVisionDiagnosticsSnapshots.encode(snapshot)
        val root = Json.parseToJsonElement(encoded).jsonObject
        val usb = root.getValue("usb").jsonObject
        val firmwareCapabilities = usb.getValue("firmwareCapabilities").jsonObject
        val activeProfile = root.getValue("activeProfile").jsonObject
        val firstInterface = usb.getValue("interfaces").jsonArray.first().jsonObject
        val firstEndpoint = firstInterface.getValue("endpoints").jsonArray.first().jsonObject

        assertEquals("openclaw.airvision.m1.diagnostics", root.getValue("schema").jsonPrimitive.content)
        assertEquals("2", root.getValue("version").jsonPrimitive.content)
        assertEquals("true", usb.getValue("firmwareControlReady").jsonPrimitive.content)
        assertEquals("true", firmwareCapabilities.getValue("hasWritableHidReports").jsonPrimitive.content)
        assertEquals("true", firmwareCapabilities.getValue("hasInterruptReportPath").jsonPrimitive.content)
        assertEquals("64", firmwareCapabilities.getValue("maxOutputPacketSize").jsonPrimitive.content)
        assertEquals(
            "firmware reports: hid out if=2, interrupt out=1, max out=64",
            firmwareCapabilities.getValue("summary").jsonPrimitive.content,
        )
        assertEquals("hid", firstInterface.getValue("classLabel").jsonPrimitive.content)
        assertEquals("out", firstEndpoint.getValue("directionLabel").jsonPrimitive.content)
        assertEquals("interrupt", firstEndpoint.getValue("typeLabel").jsonPrimitive.content)
        assertEquals("eye_care", activeProfile.getValue("splendidMode").jsonPrimitive.content)
        assertEquals("67", activeProfile.getValue("ipdMm").jsonPrimitive.content)
        assertEquals("es", root.getValue("appPreferences").jsonObject.getValue("language").jsonPrimitive.content)
        assertTrue(encoded.contains("ASUS AirVision M1"))
    }

    @Test
    fun fromState_marksSerialAvailabilityWithoutExportingSerialValue() {
        val encoded =
            AirVisionDiagnosticsSnapshots.encode(
                AirVisionDiagnosticsSnapshots.fromState(
                    usbState =
                        AirVisionUsbState(
                            connected = true,
                            permissionGranted = true,
                            deviceInfo =
                                AirVisionUsbDeviceInfo(
                                    serialNumber = "private-device-serial",
                                ),
                        ),
                    displaySettings = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working),
                    hudControls = AirVisionHudControls(),
                    appLanguage = AirVisionAppLanguage.System,
                    startupDestination = AirVisionStartupDestination.Hud,
                    hudDisplayTarget = AirVisionHudDisplayTarget.AirVisionPreferred,
                    demoModeEnabled = false,
                ),
            )

        assertTrue(encoded.contains("\"serialStatus\": \"available\""))
        assertFalse(encoded.contains("private-device-serial"))
    }
}
