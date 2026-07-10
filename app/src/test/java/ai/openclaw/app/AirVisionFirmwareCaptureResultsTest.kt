package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionFirmwareCaptureResultsTest {
    @Test
    fun summarize_acceptsPendingTemplateAndReportsBlockedFeatures() {
        val summary = AirVisionFirmwareCaptureResultFiles.summarize(pendingResultsJson())

        assertEquals(AirVisionFirmwareFeature.entries.size, summary.featureCount)
        assertEquals(0, summary.validatedFeatureCount)
        assertEquals(0, summary.writeEnabledFeatureCount)
        assertEquals(AirVisionFirmwareFeature.entries.size, summary.blockedFeatureCount)
        assertEquals("host=Cyber, tool=USBPcap/Wireshark", summary.sourceSummary)
        assertEquals("capture results: 0 validated, 0 write-enabled, 7 blocked", summary.summary)
    }

    @Test
    fun decode_rejectsMissingFeature() {
        val missingIpd =
            pendingResultsJson(
                features = AirVisionFirmwareFeature.entries.filterNot { it == AirVisionFirmwareFeature.Ipd },
            )

        val error =
            runCatching { AirVisionFirmwareCaptureResultFiles.decode(missingIpd) }
                .exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("missing: IPD"))
    }

    @Test
    fun decode_rejectsUnsafeAndroidWriteEnablement() {
        val unsafe =
            pendingResultsJson(
                overridesByRawKey =
                    mapOf(
                        "brightness" to
                            """
                            "status": "captured",
                            "androidEnablementDecision": "enable_android_write",
                            "blockerReason": null
                            """.trimIndent(),
                    ),
            )

        val error =
            runCatching { AirVisionFirmwareCaptureResultFiles.decode(unsafe) }
                .exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("cannot enable Android writes"))
    }

    @Test
    fun decode_acceptsValidatedWriteEvidence() {
        val validated =
            pendingResultsJson(
                overridesByRawKey =
                    mapOf(
                        "brightness" to
                            """
                            "status": "validated",
                            "writeReportId": "0x05",
                            "writeEndpoint": "out if=2 interrupt addr=0x2 max=64 int=1",
                            "writePayloadSummary": "brightness byte changes only; sanitized",
                            "readbackReportId": "0x85",
                            "readbackEndpoint": "in if=1 interrupt addr=0x81 max=32 int=4",
                            "readbackPayloadSummary": "readback brightness byte matched; sanitized",
                            "checksumFramingNotes": "xor checksum observed; sanitized",
                            "visibleStateConfirmed": true,
                            "captureReferences": [
                              {
                                "file": "airvision-brightness-summary.txt",
                                "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "notes": "sanitized summary only"
                              }
                            ],
                            "androidEnablementDecision": "enable_android_write",
                            "blockerReason": null
                            """.trimIndent(),
                    ),
            )

        val summary = AirVisionFirmwareCaptureResultFiles.summarize(validated)

        assertEquals(1, summary.validatedFeatureCount)
        assertEquals(1, summary.writeEnabledFeatureCount)
        assertEquals(6, summary.blockedFeatureCount)
    }

    @Test
    fun decode_rejectsSecretShapedAssignments() {
        val unsafe =
            pendingResultsJson(
                overridesByRawKey =
                    mapOf(
                        "brightness" to
                            """
                            "writePayloadSummary": "token=do-not-store-this",
                            "blockerReason": "Windows ASUS HID protocol capture has not been validated."
                            """.trimIndent(),
                    ),
            )

        val error =
            runCatching { AirVisionFirmwareCaptureResultFiles.decode(unsafe) }
                .exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("secret-shaped"))
    }

    @Test
    fun decode_rejectsMalformedJsonAsJsonFailure() {
        val error =
            runCatching { AirVisionFirmwareCaptureResultFiles.decode("{not json") }
                .exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("not valid JSON"))
    }

    private fun pendingResultsJson(
        features: List<AirVisionFirmwareFeature> = AirVisionFirmwareFeature.entries,
        overridesByRawKey: Map<String, String> = emptyMap(),
    ): String =
        """
        {
          "schema": "openclaw.airvision.firmwareCaptureResults",
          "version": 1,
          "payloadPolicy": "Sanitized summaries only.",
          "source": {
            "windowsHost": "Cyber",
            "captureTool": "USBPcap/Wireshark",
            "asusAirVisionAppVersion": null,
            "androidDiagnosticsExportSha256": null,
            "notes": "test"
          },
          "features": [
            ${features.joinToString(",\n") { featureJson(it, overridesByRawKey[it.rawValue]) }}
          ]
        }
        """.trimIndent()

    private fun featureJson(
        feature: AirVisionFirmwareFeature,
        overrideFields: String?,
    ): String {
        val overrideKeys =
            overrideFields
                ?.lineSequence()
                ?.mapNotNull { line -> line.trim().substringBefore(":", missingDelimiterValue = "").trim('"').takeIf(String::isNotBlank) }
                ?.toSet()
                .orEmpty()
        val defaults =
            linkedMapOf(
                "status" to "\"pending\"",
                "writeReportId" to "null",
                "writeEndpoint" to "null",
                "writePayloadSummary" to "null",
                "readbackReportId" to "null",
                "readbackEndpoint" to "null",
                "readbackPayloadSummary" to "null",
                "checksumFramingNotes" to "null",
                "visibleStateConfirmed" to "false",
                "captureReferences" to "[]",
                "androidEnablementDecision" to "\"blocked\"",
                "blockerReason" to "\"Windows ASUS HID protocol capture has not been validated.\"",
            ).filterKeys { it !in overrideKeys }
        val fields =
            listOf(
                "\"rawKey\": \"${feature.rawValue}\"",
                "\"label\": \"${feature.label}\"",
                "\"probeValues\": [${feature.captureProbeValues.joinToString(", ") { "\"$it\"" }}]",
            ) +
                defaults.map { (key, value) -> "\"$key\": $value" } +
                listOfNotNull(overrideFields)
        return "{\n${fields.joinToString(",\n")}\n}"
    }
}
