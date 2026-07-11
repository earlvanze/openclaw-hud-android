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
        assertTrue(markdown.contains("firmware sync: 9 Android-applied, 9 pending ASUS HID sync"))
        assertTrue(markdown.contains("## Firmware Write Gate"))
        assertTrue(markdown.contains("- Status: `read_only_capture_pending`"))
        assertTrue(markdown.contains("- Summary: firmware writes: read-only; 0/9 validated captures, 0 protocol-ready, 9 blocked"))
        assertTrue(markdown.contains("- Firmware writes enabled: no"))
        assertTrue(markdown.contains("- Validated captures: 0/9"))
        assertTrue(markdown.contains("- Protocol-ready captures: 0/9"))
        assertTrue(markdown.contains("- Blocked features: 9"))
        assertTrue(markdown.contains("- Protocol-ready feature labels: none"))
        assertTrue(
            markdown.contains(
                "- Blocked feature labels: View Mode, Brightness, Screen distance, IPD, Splendid, Blue Light Filter, Motion Sync, Light Load Mode, 3D Mode",
            ),
        )
        assertTrue(markdown.contains("- Live M1 required before writes: yes"))
        assertTrue(markdown.contains("- Explicit user confirmation required: yes"))
        assertTrue(markdown.contains("- Next step: Capture and validate ASUS HID report payloads"))
        assertTrue(markdown.contains("### Live M1 Write-Test Checklist"))
        assertTrue(markdown.contains("- Reconnect the AirVision M1 to the Android device."))
        assertTrue(markdown.contains("- Read back the matching report and verify checksum/framing."))
        assertTrue(
            markdown.contains(
                "| Brightness | 64% | software HUD dimming | capture pending | " +
                    "Writable HID path detected, but ASUS vendor report payloads are not validated. |",
            ),
        )
        assertTrue(markdown.contains("| View Mode | Working | per-mode HUD profile | capture pending |"))
        assertTrue(markdown.contains("| Light Load Mode | off | low-overhead HUD profile | capture pending |"))
        assertTrue(markdown.contains("| IPD | 67 mm | profile calibration | capture pending |"))
        assertTrue(markdown.contains("- Desired value: 35%"))
        assertTrue(markdown.contains("- Hardware sync: capture pending"))
        assertTrue(markdown.contains("| View Mode | `view_mode` | per-mode HUD profile active | working -> gaming -> infinity |"))
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
    fun renderMarkdown_reflectsImportedValidatedCaptureResultsInWriteGate() {
        val markdown =
            AirVisionFirmwareCapturePlans.renderMarkdown(
                usbState = AirVisionUsbState(),
                displaySettings =
                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                        brightnessPercent = 72,
                    ),
                captureResults =
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
                    ),
            )

        assertTrue(markdown.contains("- Status: `read_only_live_test_required`"))
        assertTrue(markdown.contains("- Summary: firmware writes: read-only; 1/9 validated captures, 1 protocol-ready, 9 blocked"))
        assertTrue(markdown.contains("- Validated captures: 1/9"))
        assertTrue(markdown.contains("- Protocol-ready captures: 1/9"))
        assertTrue(markdown.contains("- Protocol-ready feature labels: Brightness"))
        assertTrue(markdown.contains("- Next step: Keep Android firmware writes disabled until the validated report sequence"))
        assertTrue(
            markdown.contains(
                "| Brightness | 72% | software HUD dimming | validated capture imported | " +
                    "Validated capture result imported; Android HID firmware-write implementation remains disabled until live M1 testing. |",
            ),
        )
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
        assertTrue(markdown.contains("| Light Load Mode | on |"))
        assertTrue(markdown.contains("| 3D Mode | off (locked by Light Load Mode) |"))
        assertTrue(markdown.contains("- Hardware sync: waiting for writable HID"))
        assertTrue(
            markdown.contains(
                "- Blocked reason: Android has not exposed a writable AirVision HID report path.",
            ),
        )
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
        readbackEndpoint = "in if=2 interrupt addr=0x81 max=32 int=4",
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
