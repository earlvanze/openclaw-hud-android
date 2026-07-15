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
                captureResults = firmwareCaptureResults(),
            )

        assertTrue(markdown.startsWith("# AirVision M1 Firmware Update Handoff"))
        assertTrue(markdown.contains("ASUS AirVision app firmware-update session"))
        assertTrue(markdown.contains("- Connected: yes"))
        assertTrue(markdown.contains("- USB ID: 0x0b05:0x1b3c"))
        assertTrue(markdown.contains("- Manufacturer: ASUS"))
        assertTrue(markdown.contains("- Interface count: 4"))
        assertTrue(markdown.contains("- Serial status: available"))
        assertTrue(markdown.contains("- Android-visible firmware/version context: USB descriptor 1.02"))
        assertTrue(markdown.contains("## Companion Firmware Readiness"))
        assertTrue(markdown.contains("- Status: limited Android firmware protocol ready"))
        assertTrue(markdown.contains("- Android action: only run firmware writes for validated protocol-ready features"))
        assertTrue(markdown.contains("- Windows action: use Cyber/Windows ASUS app for firmware updates and any unvalidated controls"))
        assertTrue(markdown.contains("- Firmware report path visible: yes"))
        assertTrue(markdown.contains("- Imported protocol evidence: 1 validated, 1 protocol-ready, 8 blocked"))
        assertTrue(markdown.contains("- Android firmware writes allowed: yes"))
        assertTrue(markdown.contains("- Reason: sanitized capture evidence enabled 1 feature write path(s)"))
        assertTrue(markdown.contains("- HID control interface: yes"))
        assertTrue(markdown.contains("- Audio interface: yes"))
        assertTrue(markdown.contains("- Input interface: yes"))
        assertTrue(markdown.contains("writable: out if=2 interrupt addr=0x2 max=64 int=1"))
        assertTrue(markdown.contains("## Imported Protocol-Capture Evidence"))
        assertTrue(
            markdown.contains(
                "capture results: 1 validated, 0 captured-review, 8 pending, " +
                    "1 protocol-ready, 0 validated-blocked, 8 blocked",
            ),
        )
        assertTrue(markdown.contains("- Source evidence: complete"))
        assertTrue(markdown.contains("host=Cyber, tool=USBPcap/Wireshark"))
        assertTrue(markdown.contains("- Schema/version: openclaw.airvision.firmwareCaptureResults v1"))
        assertTrue(markdown.contains("- Payload policy: Sanitized summaries only."))
        assertTrue(markdown.contains("- Feature coverage: 9 AirVision firmware features"))
        assertTrue(markdown.contains("- Android protocol-ready features: Brightness"))
        assertTrue(markdown.contains("- Blocked features: View Mode"))
        assertTrue(markdown.contains("- Source notes: sanitized Windows capture worksheet"))
        assertTrue(markdown.contains("Raw USB captures, raw serial values, and payload bytes are intentionally excluded."))
        assertTrue(markdown.contains("Connect the AirVision M1 directly to the Windows/Cyber machine"))
        assertTrue(markdown.contains("Use the ASUS AirVision Windows app to check for and install firmware updates."))
        assertTrue(markdown.contains("Compare the pre/post Android-visible USB descriptor"))
        assertTrue(markdown.contains("https://www.asus.com/support/faq/1054069/"))
        assertTrue(markdown.contains("https://www.asus.com/displays-desktops/glasses/airvision/asus-airvision-m1"))
        assertTrue(markdown.contains("intentionally omits raw USB serial numbers"))
        assertFalse(markdown.contains("private-device-serial"))
        assertFalse(markdown.contains("05 11 aa bb"))
    }

    @Test
    fun renderMarkdown_marksMissingVersionAsPending() {
        val markdown = AirVisionFirmwareUpdateHandoffs.renderMarkdown(AirVisionUsbState())

        assertTrue(markdown.contains("- Connected: no"))
        assertTrue(markdown.contains("- Device: not detected"))
        assertTrue(markdown.contains("- USB ID: not detected"))
        assertTrue(markdown.contains("- Serial status: not captured"))
        assertTrue(markdown.contains("- Android-visible firmware/version context: pending ASUS HID protocol"))
        assertTrue(markdown.contains("- Status: waiting for AirVision M1"))
        assertTrue(markdown.contains("- Android action: connect the AirVision M1 to inspect USB descriptors"))
        assertTrue(markdown.contains("- Windows action: use Cyber/Windows ASUS app for firmware updates and protocol capture"))
        assertTrue(markdown.contains("- Firmware report path visible: no"))
        assertTrue(markdown.contains("- Imported protocol evidence: none"))
        assertTrue(markdown.contains("- Android firmware writes allowed: no"))
        assertTrue(markdown.contains("- Reason: no Android-visible M1 device is present"))
        assertTrue(markdown.contains("- Status: M1 USB device not detected."))
        assertTrue(markdown.contains("- Imported capture results: none"))
        assertTrue(markdown.contains("Android firmware writes remain blocked"))
    }

    @Test
    fun readiness_marksConnectedDeviceAsReadOnlyUntilCaptureEvidenceIsImported() {
        val readiness =
            AirVisionFirmwareUpdateReadiness.from(
                usbState =
                    AirVisionUsbState(
                        connected = true,
                        permissionGranted = true,
                        hidControlInterface = true,
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
                captureResults = null,
            )

        assertTrue(readiness.firmwareReportPathVisible)
        assertFalse(readiness.androidFirmwareWritesAllowed)
        assertTrue(readiness.status.contains("read-only Android inspection ready"))
        assertTrue(readiness.androidAction.contains("keep writes blocked"))
    }

    private fun firmwareCaptureResults(): AirVisionFirmwareCaptureResults =
        AirVisionFirmwareCaptureResults(
            schema = AirVisionFirmwareCaptureResultFiles.SCHEMA,
            version = AirVisionFirmwareCaptureResultFiles.VERSION,
            payloadPolicy = "Sanitized summaries only.",
            source =
                AirVisionFirmwareCaptureResultsSource(
                    windowsHost = "Cyber",
                    captureTool = "USBPcap/Wireshark",
                    asusAirVisionAppVersion = "1.0.0",
                    androidDiagnosticsExportSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    notes = "sanitized Windows capture worksheet",
                ),
            features =
                AirVisionFirmwareFeature.entries.map { feature ->
                    if (feature == AirVisionFirmwareFeature.Brightness) {
                        validatedBrightnessResult(feature)
                    } else {
                        blockedResult(feature)
                    }
                },
        )

    private fun validatedBrightnessResult(feature: AirVisionFirmwareFeature): AirVisionFirmwareCaptureResult =
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
                        sha256 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        notes = "sanitized summary only",
                    ),
                ),
            androidEnablementDecision = "enable_android_write",
            blockerReason = null,
        )

    private fun blockedResult(feature: AirVisionFirmwareFeature): AirVisionFirmwareCaptureResult =
        AirVisionFirmwareCaptureResult(
            rawKey = feature.rawValue,
            label = feature.label,
            status = "pending",
            probeValues = feature.captureProbeValues,
            androidEnablementDecision = "blocked",
            blockerReason = "Windows ASUS HID protocol capture has not been validated.",
        )
}
