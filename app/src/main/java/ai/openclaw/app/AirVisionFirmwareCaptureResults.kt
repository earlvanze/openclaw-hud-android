package ai.openclaw.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class AirVisionFirmwareCaptureResults(
    val schema: String,
    val version: Int,
    val payloadPolicy: String? = null,
    val source: AirVisionFirmwareCaptureResultsSource,
    val features: List<AirVisionFirmwareCaptureResult>,
)

@Serializable
data class AirVisionFirmwareCaptureResultsSource(
    val windowsHost: String? = null,
    val captureTool: String? = null,
    val asusAirVisionAppVersion: String? = null,
    val androidDiagnosticsExportSha256: String? = null,
    val notes: String? = null,
)

@Serializable
data class AirVisionFirmwareCaptureResult(
    val rawKey: String,
    val label: String,
    val status: String,
    val probeValues: List<String>,
    val writeReportId: String? = null,
    val writeEndpoint: String? = null,
    val writePayloadSummary: String? = null,
    val readbackReportId: String? = null,
    val readbackEndpoint: String? = null,
    val readbackPayloadSummary: String? = null,
    val checksumFramingNotes: String? = null,
    val visibleStateConfirmed: Boolean = false,
    val captureReferences: List<AirVisionFirmwareCaptureReference> = emptyList(),
    val androidEnablementDecision: String,
    val blockerReason: String? = null,
)

@Serializable
data class AirVisionFirmwareCaptureReference(
    val file: String? = null,
    val sha256: String? = null,
    val notes: String? = null,
)

data class AirVisionFirmwareCaptureResultsSummary(
    val featureCount: Int,
    val validatedFeatureCount: Int,
    val capturedFeatureCount: Int,
    val pendingFeatureCount: Int,
    val writeEnabledFeatureCount: Int,
    val blockedFeatureCount: Int,
    val writeEnabledFeatureLabels: List<String>,
    val reviewRequiredFeatureLabels: List<String>,
    val pendingFeatureLabels: List<String>,
    val blockedFeatureLabels: List<String>,
    val payloadPolicy: String,
    val sourceSummary: String,
    val summary: String,
) {
    val displayText: String
        get() = "$summary; $sourceSummary"

    val writeEnabledFeatureSummary: String
        get() = writeEnabledFeatureLabels.joinToString().ifBlank { "none" }

    val reviewRequiredFeatureSummary: String
        get() = reviewRequiredFeatureLabels.joinToString().ifBlank { "none" }

    val pendingFeatureSummary: String
        get() = pendingFeatureLabels.joinToString().ifBlank { "none" }

    val blockedFeatureSummary: String
        get() = blockedFeatureLabels.joinToString().ifBlank { "none" }

    val safetyPreviewText: String
        get() =
            "$payloadPolicy; raw USB captures, raw serials, payload bytes, " +
                "and token-shaped values are rejected"
}

object AirVisionFirmwareCaptureResultFiles {
    const val SCHEMA = "openclaw.airvision.firmwareCaptureResults"
    const val VERSION = 1

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun decode(raw: String): AirVisionFirmwareCaptureResults {
        val results =
            try {
                json.decodeFromString<AirVisionFirmwareCaptureResults>(raw)
            } catch (error: SerializationException) {
                throw IllegalArgumentException("Firmware capture results are not valid JSON.", error)
            }
        validate(results)
        return results
    }

    fun summarize(raw: String): AirVisionFirmwareCaptureResultsSummary = summarize(decode(raw))

    fun summarize(results: AirVisionFirmwareCaptureResults): AirVisionFirmwareCaptureResultsSummary {
        validate(results)
        val validated = results.features.count { it.status == "validated" }
        val captured = results.features.count { it.status == "captured" }
        val pending = results.features.count { it.status == "pending" }
        val writeEnabledLabels =
            results.features
                .filter { it.androidEnablementDecision == "enable_android_write" }
                .map { it.label }
        val reviewRequiredLabels =
            results.features
                .filter { it.status == "captured" }
                .map { it.label }
        val pendingLabels =
            results.features
                .filter { it.status == "pending" }
                .map { it.label }
        val blockedLabels =
            results.features
                .filter { it.androidEnablementDecision == "blocked" }
                .map { it.label }
        val sourceSummary =
            listOfNotNull(
                results.source.windowsHost?.trim()?.takeIf(String::isNotEmpty)?.let { "host=$it" },
                results.source.captureTool?.trim()?.takeIf(String::isNotEmpty)?.let { "tool=$it" },
                results.source.asusAirVisionAppVersion?.trim()?.takeIf(String::isNotEmpty)?.let { "asusApp=$it" },
                results.source.androidDiagnosticsExportSha256
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { "diagnosticsSha256=${it.take(12)}..." },
            ).joinToString(", ").ifBlank { "source=pending" }
        return AirVisionFirmwareCaptureResultsSummary(
            featureCount = results.features.size,
            validatedFeatureCount = validated,
            capturedFeatureCount = captured,
            pendingFeatureCount = pending,
            writeEnabledFeatureCount = writeEnabledLabels.size,
            blockedFeatureCount = blockedLabels.size,
            writeEnabledFeatureLabels = writeEnabledLabels,
            reviewRequiredFeatureLabels = reviewRequiredLabels,
            pendingFeatureLabels = pendingLabels,
            blockedFeatureLabels = blockedLabels,
            payloadPolicy =
                results.payloadPolicy
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: "Sanitized summaries only",
            sourceSummary = sourceSummary,
            summary =
                "capture results: $validated validated, " +
                    "$captured captured-review, $pending pending, " +
                    "${writeEnabledLabels.size} write-enabled, ${blockedLabels.size} blocked",
        )
    }

    fun validate(results: AirVisionFirmwareCaptureResults) {
        require(results.schema == SCHEMA) { "Firmware capture results schema is not supported." }
        require(results.version == VERSION) { "Firmware capture results version is not supported." }
        requireSha256OrNull(results.source.androidDiagnosticsExportSha256, "source.androidDiagnosticsExportSha256")
        val expectedByKey = AirVisionFirmwareFeature.entries.associateBy { it.rawValue }
        val seen = mutableSetOf<String>()
        results.features.forEachIndexed { index, result ->
            val feature = expectedByKey[result.rawKey]
                ?: throw IllegalArgumentException("Unknown firmware capture feature: ${result.rawKey}")
            require(seen.add(result.rawKey)) { "Duplicate firmware capture feature: ${result.rawKey}" }
            require(result.label == feature.label) { "Feature ${result.rawKey} label must be ${feature.label}." }
            require(result.probeValues == feature.captureProbeValues) {
                "Feature ${result.rawKey} probe values do not match the Android feature list."
            }
            require(result.status in setOf("pending", "captured", "validated")) {
                "Feature ${result.rawKey} status is not supported."
            }
            require(result.androidEnablementDecision in setOf("blocked", "enable_android_write")) {
                "Feature ${result.rawKey} Android enablement decision is not supported."
            }
            requireSanitizedText(result.writeReportId, "Feature ${result.rawKey} write report ID")
            requireSanitizedText(result.writeEndpoint, "Feature ${result.rawKey} write endpoint")
            requireSanitizedText(result.writePayloadSummary, "Feature ${result.rawKey} write payload summary")
            requireSanitizedText(result.readbackReportId, "Feature ${result.rawKey} readback report ID")
            requireSanitizedText(result.readbackEndpoint, "Feature ${result.rawKey} readback endpoint")
            requireSanitizedText(result.readbackPayloadSummary, "Feature ${result.rawKey} readback payload summary")
            requireSanitizedText(result.checksumFramingNotes, "Feature ${result.rawKey} checksum/framing notes")
            requireSanitizedText(result.blockerReason, "Feature ${result.rawKey} blocker reason")
            result.captureReferences.forEachIndexed { referenceIndex, reference ->
                requireSha256OrNull(reference.sha256, "features[$index].captureReferences[$referenceIndex].sha256")
                require(!hasRawCaptureDumpExtension(reference.file)) {
                    "features[$index].captureReferences[$referenceIndex].file must reference a sanitized summary, not a raw capture dump."
                }
                requireSanitizedText(reference.file, "features[$index].captureReferences[$referenceIndex].file")
                requireSanitizedText(reference.notes, "features[$index].captureReferences[$referenceIndex].notes")
            }
            val hasCaptureReferenceDigest =
                result.captureReferences.any { !it.sha256.isNullOrBlank() }
            val completeEvidence =
                result.status == "validated" &&
                    isFilled(result.writeReportId) &&
                    isFilled(result.writeEndpoint) &&
                    isFilled(result.writePayloadSummary) &&
                    isFilled(result.readbackReportId) &&
                    isFilled(result.readbackEndpoint) &&
                    isFilled(result.readbackPayloadSummary) &&
                    isFilled(result.checksumFramingNotes) &&
                    result.visibleStateConfirmed &&
                    hasCaptureReferenceDigest
            require(result.androidEnablementDecision != "enable_android_write" || completeEvidence) {
                "Feature ${result.rawKey} cannot enable Android writes without validated write/readback/checksum/visible-state evidence and a SHA-256 capture reference."
            }
            require(result.androidEnablementDecision != "blocked" || isFilled(result.blockerReason)) {
                "Feature ${result.rawKey} needs a blocker reason while Android writes are blocked."
            }
        }
        val missing = AirVisionFirmwareFeature.entries.filterNot { it.rawValue in seen }
        require(missing.isEmpty()) {
            "Firmware capture results missing: ${missing.joinToString { it.label }}."
        }
        requireSanitizedText(results.source.windowsHost, "source.windowsHost")
        requireSanitizedText(results.source.captureTool, "source.captureTool")
        requireSanitizedText(results.source.asusAirVisionAppVersion, "source.asusAirVisionAppVersion")
        requireSanitizedText(results.source.notes, "source.notes")
    }

    private fun requireSha256OrNull(
        value: String?,
        label: String,
    ) {
        require(value == null || value.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            "$label must be a SHA-256 hex digest or null."
        }
    }

    private fun isFilled(value: String?): Boolean =
        !value.isNullOrBlank() && value.trim().lowercase() != "pending"

    private fun requireSanitizedText(
        value: String?,
        label: String,
    ) {
        val text = value ?: return
        require(!containsForbiddenSecretShape(text)) {
            "$label contains a secret or raw-serial-shaped assignment."
        }
        require(!containsRawHexDumpShape(text)) {
            "$label looks like a raw byte dump; use a sanitized summary instead."
        }
    }

    private fun containsForbiddenSecretShape(text: String): Boolean =
        Regex("(token|password|secret|signaturekey|authorization|serial(?:number)?)\\s*[:=]", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    private fun containsRawHexDumpShape(text: String): Boolean =
        Regex("(?:\\b[0-9a-fA-F]{2}\\b[\\s,;:-]*){16,}").containsMatchIn(text)

    private fun hasRawCaptureDumpExtension(value: String?): Boolean =
        value
            ?.trim()
            ?.let { Regex("\\.(pcap|pcapng|usbpcap|etl|cap)$", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            ?: false
}
