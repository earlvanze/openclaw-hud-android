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
        assertEquals(0, plan.firmwareWriteAllowedCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.blockedFirmwareWriteCount)
        assertEquals("firmware sync: 9 Android-applied, 9 pending ASUS HID sync", plan.summary)
        assertEquals("firmware writes: read-only; 0/9 validated captures, 0 protocol-ready, 9 blocked", plan.writeGateSummary)
        assertEquals("read_only_capture_pending", plan.writeGate.status)
        assertEquals(false, plan.writeGate.firmwareWritesEnabled)
        assertEquals(0, plan.writeGate.validatedCaptureCount)
        assertEquals(0, plan.writeGate.writeEnabledCaptureCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.writeGate.blockedFeatureCount)
        assertEquals(emptyList<String>(), plan.writeGate.protocolReadyFeatureLabels)
        assertEquals(AirVisionFirmwareFeature.entries.map { it.label }, plan.writeGate.blockedFeatureLabels)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.writeGate.blockedFeatureSummaries.size)
        assertTrue(plan.writeGate.blockedFeatureSummaries.first().contains("View Mode:"))
        assertEquals(
            "Reconnect the AirVision M1 to the Android device.",
            plan.writeGate.liveTestChecklist.first(),
        )
        assertEquals(true, plan.writeGate.liveM1Required)
        assertEquals(true, plan.writeGate.explicitUserConfirmationRequired)
        assertTrue(plan.writeGate.nextStep.contains("Capture and validate ASUS HID report payloads"))
        assertEquals(
            "Working",
            plan.items.first { it.feature == AirVisionFirmwareFeature.ViewMode }.desiredValue,
        )
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
        assertTrue(plan.items.all { it.captureResultStatus == "pending_validated_capture_result" })
        assertTrue(plan.items.all { it.androidEnablementDecision == "blocked" })
        assertTrue(plan.items.none { it.firmwareWriteAllowed })
        assertTrue(plan.items.all { it.requiredEvidence.size == 9 })
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
        assertEquals(
            "on",
            plan.items.first { it.feature == AirVisionFirmwareFeature.LightLoadMode }.desiredValue,
        )
        assertTrue(plan.items.all { it.hardwareSyncStatus == "waiting for writable HID" })
    }

    @Test
    fun fromSettings_consumesImportedValidatedCaptureResultsWithoutEnablingWrites() {
        val settings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                brightnessPercent = 72,
            )
        val captureResults =
            AirVisionFirmwareCaptureResults(
                schema = AirVisionFirmwareCaptureResultFiles.SCHEMA,
                version = AirVisionFirmwareCaptureResultFiles.VERSION,
                source = AirVisionFirmwareCaptureResultsSource(windowsHost = "Cyber"),
                features =
                    AirVisionFirmwareFeature.entries.map { feature ->
                        if (feature == AirVisionFirmwareFeature.Brightness) {
                            validatedBrightnessCaptureResult(feature)
                        } else {
                            pendingCaptureResult(feature)
                        }
                    },
            )

        val plan =
            AirVisionFirmwareSyncPlans.fromSettings(
                settings = settings,
                capabilities = AirVisionFirmwareCapabilities(),
                captureResults = captureResults,
            )
        val brightness = plan.items.first { it.feature == AirVisionFirmwareFeature.Brightness }

        assertEquals("validated", brightness.captureResultStatus)
        assertEquals("enable_android_write", brightness.androidEnablementDecision)
        assertEquals("validated capture imported", brightness.hardwareSyncStatus)
        assertEquals(false, brightness.firmwareWriteAllowed)
        assertEquals(emptyList<String>(), brightness.requiredEvidence)
        assertTrue(brightness.blockedReason.contains("implementation remains disabled"))
        assertEquals(0, plan.firmwareWriteAllowedCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.blockedFirmwareWriteCount)
        assertEquals("read_only_live_test_required", plan.writeGate.status)
        assertEquals(false, plan.writeGate.firmwareWritesEnabled)
        assertEquals(1, plan.writeGate.validatedCaptureCount)
        assertEquals(1, plan.writeGate.writeEnabledCaptureCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, plan.writeGate.blockedFeatureCount)
        assertEquals(listOf("Brightness"), plan.writeGate.protocolReadyFeatureLabels)
        assertEquals(AirVisionFirmwareFeature.entries.map { it.label }, plan.writeGate.blockedFeatureLabels)
        assertTrue(
            plan.writeGate.blockedFeatureSummaries
                .first { it.startsWith("Brightness:") }
                .contains("live M1 testing"),
        )
        assertTrue(plan.writeGate.liveTestChecklist.any { it.contains("Replay only one validated feature") })
        assertTrue(plan.writeGate.nextStep.contains("live-tested with the M1 connected"))
    }

    @Test
    fun fromSettings_doesNotTreatUnvalidatedEnablementDecisionAsProtocolReady() {
        val captureResults =
            AirVisionFirmwareCaptureResults(
                schema = AirVisionFirmwareCaptureResultFiles.SCHEMA,
                version = AirVisionFirmwareCaptureResultFiles.VERSION,
                source = AirVisionFirmwareCaptureResultsSource(windowsHost = "Cyber"),
                features =
                    AirVisionFirmwareFeature.entries.map { feature ->
                        if (feature == AirVisionFirmwareFeature.Brightness) {
                            capturedBrightnessEnablementDecision(feature)
                        } else {
                            pendingCaptureResult(feature)
                        }
                    },
            )

        val plan =
            AirVisionFirmwareSyncPlans.fromSettings(
                settings = AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working),
                capabilities =
                    AirVisionFirmwareCapabilities(
                        writableReportPaths =
                            listOf(
                                AirVisionFirmwareReportPath(
                                    interfaceId = 2,
                                    endpointAddress = 0x02,
                                    direction = UsbConstants.USB_DIR_OUT,
                                    type = UsbConstants.USB_ENDPOINT_XFER_INT,
                                    maxPacketSize = 64,
                                    interval = 1,
                                ),
                            ),
                    ),
                captureResults = captureResults,
            )
        val brightness = plan.items.first { it.feature == AirVisionFirmwareFeature.Brightness }

        assertEquals("captured", brightness.captureResultStatus)
        assertEquals("enable_android_write", brightness.androidEnablementDecision)
        assertEquals(false, brightness.hasValidatedWriteEnablement)
        assertEquals("capture pending", brightness.hardwareSyncStatus)
        assertEquals(AirVisionFirmwareFeature.entries.size, brightness.requiredEvidence.size)
        assertEquals("read_only_capture_pending", plan.writeGate.status)
        assertEquals(0, plan.writeGate.validatedCaptureCount)
        assertEquals(0, plan.writeGate.writeEnabledCaptureCount)
        assertEquals(emptyList<String>(), plan.writeGate.protocolReadyFeatureLabels)
        assertTrue(plan.writeGate.nextStep.contains("Capture and validate ASUS HID report payloads"))
    }
}

private fun pendingCaptureResult(feature: AirVisionFirmwareFeature): AirVisionFirmwareCaptureResult =
    AirVisionFirmwareCaptureResult(
        rawKey = feature.rawValue,
        label = feature.label,
        status = "pending",
        probeValues = feature.captureProbeValues,
        androidEnablementDecision = "blocked",
        blockerReason = "Windows ASUS HID protocol capture has not been validated.",
    )

private fun validatedBrightnessCaptureResult(feature: AirVisionFirmwareFeature): AirVisionFirmwareCaptureResult =
    AirVisionFirmwareCaptureResult(
        rawKey = feature.rawValue,
        label = feature.label,
        status = "validated",
        probeValues = feature.captureProbeValues,
        writeReportId = "0x05",
        writeEndpoint = "out if=2 interrupt addr=0x2 max=64 int=1",
        writePayloadSummary = "brightness byte changes only; sanitized",
        readbackReportId = "0x85",
        readbackEndpoint = "in if=1 interrupt addr=0x81 max=32 int=4",
        readbackPayloadSummary = "readback brightness byte matched; sanitized",
        checksumFramingNotes = "xor checksum observed; sanitized",
        visibleStateConfirmed = true,
        captureReferences =
            listOf(
                AirVisionFirmwareCaptureReference(
                    file = "airvision-brightness-summary.txt",
                    sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    notes = "sanitized summary only",
                ),
            ),
        androidEnablementDecision = "enable_android_write",
        blockerReason = null,
    )

private fun capturedBrightnessEnablementDecision(feature: AirVisionFirmwareFeature): AirVisionFirmwareCaptureResult =
    AirVisionFirmwareCaptureResult(
        rawKey = feature.rawValue,
        label = feature.label,
        status = "captured",
        probeValues = feature.captureProbeValues,
        writeReportId = "0x05",
        writeEndpoint = "out if=2 interrupt addr=0x2 max=64 int=1",
        writePayloadSummary = "brightness byte changes only; sanitized",
        readbackReportId = "0x85",
        readbackEndpoint = "in if=1 interrupt addr=0x81 max=32 int=4",
        readbackPayloadSummary = "readback brightness byte matched; sanitized",
        checksumFramingNotes = "checksum still under review",
        visibleStateConfirmed = true,
        captureReferences =
            listOf(
                AirVisionFirmwareCaptureReference(
                    file = "airvision-brightness-summary.txt",
                    sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    notes = "sanitized summary only",
                ),
            ),
        androidEnablementDecision = "enable_android_write",
        blockerReason = null,
    )
