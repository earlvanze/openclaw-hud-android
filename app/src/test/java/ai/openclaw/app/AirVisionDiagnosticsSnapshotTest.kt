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
                                deviceClass = 0,
                                deviceSubclass = 0,
                                deviceProtocol = 0,
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
                        hudScalePercent = 120,
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
                hudPresentationActive = true,
                hudDisplayRoute =
                    AirVisionHudDisplayRoute(
                        target = AirVisionHudDisplayTarget.AirVisionPreferred,
                        candidateCount = 2,
                        presentationCandidateCount = 1,
                        selectedCandidate =
                            AirVisionHudDisplayCandidate(
                                displayId = 5,
                                name = "ASUS AirVision M1",
                                widthPx = 1920,
                                heightPx = 1080,
                                isPresentation = true,
                            ),
                        reason = "selected_presentation_display",
                    ),
                demoModeEnabled = true,
                speakerEnabled = false,
                nativeCaptionsEnabled = true,
                translationCaptionSourceLanguage = "pt-BR",
                translationCaptionTargetLanguage = "ja",
            )

        val encoded = AirVisionDiagnosticsSnapshots.encode(snapshot)
        val root = Json.parseToJsonElement(encoded).jsonObject
        val usb = root.getValue("usb").jsonObject
        val deviceInfo = usb.getValue("deviceInfo").jsonObject
        val firmwareCapabilities = usb.getValue("firmwareCapabilities").jsonObject
        val firmwareSync = root.getValue("firmwareSync").jsonObject
        val activeProfile = root.getValue("activeProfile").jsonObject
        val hudRuntime = root.getValue("hudRuntime").jsonObject
        val appPreferences = root.getValue("appPreferences").jsonObject
        val firstInterface = usb.getValue("interfaces").jsonArray.first().jsonObject
        val firstEndpoint = firstInterface.getValue("endpoints").jsonArray.first().jsonObject
        val writableReportPath = firmwareCapabilities.getValue("writableReportPaths").jsonArray.first().jsonObject
        val firstFeatureReadiness = firmwareCapabilities.getValue("featureReadiness").jsonArray.first().jsonObject
        val captureTargets = firmwareCapabilities.getValue("captureTargets").jsonArray
        val firstCaptureTarget = captureTargets.first().jsonObject
        val ipdCaptureTarget =
            captureTargets
                .first { it.jsonObject.getValue("feature").jsonPrimitive.content == "ipd" }
                .jsonObject
        val firmwareSyncItems = firmwareSync.getValue("items").jsonArray
        val brightnessSync = firmwareSyncItems.first().jsonObject
        val ipdSync =
            firmwareSyncItems
                .first { it.jsonObject.getValue("feature").jsonPrimitive.content == "ipd" }
                .jsonObject

        assertEquals("openclaw.airvision.m1.diagnostics", root.getValue("schema").jsonPrimitive.content)
        assertEquals("13", root.getValue("version").jsonPrimitive.content)
        assertEquals("USB descriptor 1.02", deviceInfo.getValue("firmwareVersion").jsonPrimitive.content)
        assertEquals("0", deviceInfo.getValue("deviceClass").jsonPrimitive.content)
        assertEquals("0", deviceInfo.getValue("deviceSubclass").jsonPrimitive.content)
        assertEquals("0", deviceInfo.getValue("deviceProtocol").jsonPrimitive.content)
        assertEquals("4", deviceInfo.getValue("interfaceCount").jsonPrimitive.content)
        assertEquals("true", usb.getValue("firmwareControlReady").jsonPrimitive.content)
        assertEquals("true", firmwareCapabilities.getValue("hasWritableHidReports").jsonPrimitive.content)
        assertEquals("true", firmwareCapabilities.getValue("hasInterruptReportPath").jsonPrimitive.content)
        assertEquals("64", firmwareCapabilities.getValue("maxOutputPacketSize").jsonPrimitive.content)
        assertEquals("2", writableReportPath.getValue("interfaceId").jsonPrimitive.content)
        assertEquals("1", writableReportPath.getValue("endpointAddress").jsonPrimitive.content)
        assertEquals("out", writableReportPath.getValue("directionLabel").jsonPrimitive.content)
        assertEquals("interrupt", writableReportPath.getValue("typeLabel").jsonPrimitive.content)
        assertEquals(
            "out if=2 interrupt addr=0x1 max=64 int=1",
            writableReportPath.getValue("summary").jsonPrimitive.content,
        )
        assertEquals(
            "firmware reports: writable: out if=2 interrupt addr=0x1 max=64 int=1, interrupt out=1, max out=64",
            firmwareCapabilities.getValue("summary").jsonPrimitive.content,
        )
        assertEquals("brightness", firstFeatureReadiness.getValue("feature").jsonPrimitive.content)
        assertEquals("Brightness", firstFeatureReadiness.getValue("label").jsonPrimitive.content)
        assertEquals("software HUD dimming active", firstFeatureReadiness.getValue("androidStatus").jsonPrimitive.content)
        assertEquals("false", firstFeatureReadiness.getValue("firmwareApplyReady").jsonPrimitive.content)
        assertEquals(
            "ASUS HID protocol capture pending",
            firstFeatureReadiness.getValue("firmwareApplyStatus").jsonPrimitive.content,
        )
        assertEquals(
            "Brightness: ASUS HID protocol capture pending",
            firstFeatureReadiness.getValue("summary").jsonPrimitive.content,
        )
        assertEquals("brightness", firstCaptureTarget.getValue("feature").jsonPrimitive.content)
        assertEquals("Brightness", firstCaptureTarget.getValue("label").jsonPrimitive.content)
        assertEquals("true", firstCaptureTarget.getValue("captureReady").jsonPrimitive.content)
        assertEquals(
            listOf("20%", "50%", "80%"),
            firstCaptureTarget.getValue("suggestedProbeValues").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(
            listOf("out if=2 interrupt addr=0x1 max=64 int=1"),
            firstCaptureTarget.getValue("writeReportPathSummaries").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(
            "Brightness: capture 20% -> 50% -> 80% on out if=2 interrupt addr=0x1 max=64 int=1",
            firstCaptureTarget.getValue("summary").jsonPrimitive.content,
        )
        assertEquals(
            "IPD: capture 60 mm -> 67 mm -> 72 mm on out if=2 interrupt addr=0x1 max=64 int=1",
            ipdCaptureTarget.getValue("summary").jsonPrimitive.content,
        )
        assertEquals(
            "firmware apply: Brightness: ASUS HID protocol capture pending; " +
                "Screen distance: ASUS HID protocol capture pending; " +
                "IPD: ASUS HID protocol capture pending; " +
                "Splendid: ASUS HID protocol capture pending; " +
                "Blue Light Filter: ASUS HID protocol capture pending; " +
                "Motion Sync: ASUS HID protocol capture pending; " +
                "3D Mode: ASUS HID protocol capture pending",
            firmwareCapabilities.getValue("featureReadinessSummary").jsonPrimitive.content,
        )
        assertEquals(
            "firmware capture: Brightness: capture 20% -> 50% -> 80% on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "Screen distance: capture 50 cm -> 100 cm -> 150 cm on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "IPD: capture 60 mm -> 67 mm -> 72 mm on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "Splendid: capture standard -> theater -> eye_care on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "Blue Light Filter: capture 0% -> 50% -> 100% on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "Motion Sync: capture off -> on on out if=2 interrupt addr=0x1 max=64 int=1; " +
                "3D Mode: capture off -> on on out if=2 interrupt addr=0x1 max=64 int=1",
            firmwareCapabilities.getValue("capturePlanSummary").jsonPrimitive.content,
        )
        assertEquals("hid", firstInterface.getValue("classLabel").jsonPrimitive.content)
        assertEquals("out", firstEndpoint.getValue("directionLabel").jsonPrimitive.content)
        assertEquals("interrupt", firstEndpoint.getValue("typeLabel").jsonPrimitive.content)
        assertEquals("eye_care", activeProfile.getValue("splendidMode").jsonPrimitive.content)
        assertEquals("67", activeProfile.getValue("ipdMm").jsonPrimitive.content)
        assertEquals("7", firmwareSync.getValue("pendingHardwareSyncCount").jsonPrimitive.content)
        assertEquals("7", firmwareSync.getValue("androidAppliedCount").jsonPrimitive.content)
        assertEquals("0", firmwareSync.getValue("firmwareWriteAllowedCount").jsonPrimitive.content)
        assertEquals("7", firmwareSync.getValue("blockedFirmwareWriteCount").jsonPrimitive.content)
        assertEquals(
            "firmware sync: 7 Android-applied, 7 pending ASUS HID sync",
            firmwareSync.getValue("summary").jsonPrimitive.content,
        )
        assertEquals(
            "firmware writes: 0 enabled, 7 blocked pending validated capture results",
            firmwareSync.getValue("writeGateSummary").jsonPrimitive.content,
        )
        assertEquals("brightness", brightnessSync.getValue("feature").jsonPrimitive.content)
        assertEquals("72%", brightnessSync.getValue("desiredValue").jsonPrimitive.content)
        assertEquals("software HUD dimming", brightnessSync.getValue("androidEffect").jsonPrimitive.content)
        assertEquals("true", brightnessSync.getValue("hardwareSyncPending").jsonPrimitive.content)
        assertEquals("capture pending", brightnessSync.getValue("hardwareSyncStatus").jsonPrimitive.content)
        assertEquals(
            "openclaw.airvision.firmwareCaptureResults",
            brightnessSync.getValue("captureResultsSchema").jsonPrimitive.content,
        )
        assertEquals(
            "pending_validated_capture_result",
            brightnessSync.getValue("captureResultStatus").jsonPrimitive.content,
        )
        assertEquals("blocked", brightnessSync.getValue("androidEnablementDecision").jsonPrimitive.content)
        assertEquals("false", brightnessSync.getValue("firmwareWriteAllowed").jsonPrimitive.content)
        assertEquals(
            "validated write report ID",
            brightnessSync.getValue("requiredEvidence").jsonArray.first().jsonPrimitive.content,
        )
        assertEquals(
            "capture reference with SHA-256 digest",
            brightnessSync.getValue("requiredEvidence").jsonArray.last().jsonPrimitive.content,
        )
        assertEquals("67 mm", ipdSync.getValue("desiredValue").jsonPrimitive.content)
        assertEquals(
            "IPD=67 mm (capture pending)",
            ipdSync.getValue("summary").jsonPrimitive.content,
        )
        assertEquals("8", hudRuntime.getValue("transcriptEntryCount").jsonPrimitive.content)
        assertEquals("5", hudRuntime.getValue("captionEntryCount").jsonPrimitive.content)
        assertEquals("120", hudRuntime.getValue("effectiveHudScalePercent").jsonPrimitive.content)
        assertEquals("120", activeProfile.getValue("hudScalePercent").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("colorPreviewOverlaysEnabled").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("brightnessDimmingEnabled").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("ipdAdjustmentEnabled").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("presentationActive").jsonPrimitive.content)
        assertEquals("airvision_preferred", hudRuntime.getValue("displayTarget").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("presentationDisplayCategoryPreferred").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("nonDefaultDisplayFallbackEnabled").jsonPrimitive.content)
        assertEquals("2", hudRuntime.getValue("displayCandidateCount").jsonPrimitive.content)
        assertEquals("1", hudRuntime.getValue("presentationDisplayCandidateCount").jsonPrimitive.content)
        assertEquals("5", hudRuntime.getValue("selectedDisplayId").jsonPrimitive.content)
        assertEquals("ASUS AirVision M1", hudRuntime.getValue("selectedDisplayName").jsonPrimitive.content)
        assertEquals("1920", hudRuntime.getValue("selectedDisplayWidthPx").jsonPrimitive.content)
        assertEquals("1080", hudRuntime.getValue("selectedDisplayHeightPx").jsonPrimitive.content)
        assertEquals("true", hudRuntime.getValue("selectedDisplayPresentationEligible").jsonPrimitive.content)
        assertEquals("false", hudRuntime.getValue("usedNonDefaultDisplayFallback").jsonPrimitive.content)
        assertEquals("selected_presentation_display", hudRuntime.getValue("displayRouteReason").jsonPrimitive.content)
        assertEquals("es", appPreferences.getValue("language").jsonPrimitive.content)
        assertEquals("false", appPreferences.getValue("speakerEnabled").jsonPrimitive.content)
        assertEquals("true", appPreferences.getValue("nativeCaptionsEnabled").jsonPrimitive.content)
        assertEquals("pt", appPreferences.getValue("translationCaptionSourceLanguage").jsonPrimitive.content)
        assertEquals("ja", appPreferences.getValue("translationCaptionTargetLanguage").jsonPrimitive.content)
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

    @Test
    fun fromState_exportsDerivedLightLoadHudRuntime() {
        val encoded =
            AirVisionDiagnosticsSnapshots.encode(
                AirVisionDiagnosticsSnapshots.fromState(
                    usbState = AirVisionUsbState(),
                    displaySettings =
                        AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                            lightLoadModeEnabled = true,
                            threeDModeEnabled = true,
                        ).normalized,
                    hudControls = AirVisionHudControls(),
                    appLanguage = AirVisionAppLanguage.System,
                    startupDestination = AirVisionStartupDestination.Hud,
                    hudDisplayTarget = AirVisionHudDisplayTarget.AirVisionPreferred,
                    demoModeEnabled = false,
                ),
            )
        val hudRuntime =
            Json.parseToJsonElement(encoded)
                .jsonObject
                .getValue("hudRuntime")
                .jsonObject
        val firmwareSync =
            Json.parseToJsonElement(encoded)
                .jsonObject
                .getValue("firmwareSync")
                .jsonObject
        val firmwareSyncItems = firmwareSync.getValue("items").jsonArray
        val ipdSync =
            firmwareSyncItems
                .first { it.jsonObject.getValue("feature").jsonPrimitive.content == "ipd" }
                .jsonObject
        val threeDSync =
            firmwareSyncItems
                .first { it.jsonObject.getValue("feature").jsonPrimitive.content == "3d_mode" }
                .jsonObject

        assertEquals("3", hudRuntime.getValue("transcriptEntryCount").jsonPrimitive.content)
        assertEquals("2", hudRuntime.getValue("captionEntryCount").jsonPrimitive.content)
        assertEquals("false", hudRuntime.getValue("colorPreviewOverlaysEnabled").jsonPrimitive.content)
        assertEquals("false", hudRuntime.getValue("ipdAdjustmentEnabled").jsonPrimitive.content)
        assertEquals("false", hudRuntime.getValue("threeDModeAvailable").jsonPrimitive.content)
        assertEquals("67 mm (locked by Light Load Mode)", ipdSync.getValue("desiredValue").jsonPrimitive.content)
        assertEquals("off (locked by Light Load Mode)", threeDSync.getValue("desiredValue").jsonPrimitive.content)
        assertEquals("waiting for writable HID", ipdSync.getValue("hardwareSyncStatus").jsonPrimitive.content)
    }
}
