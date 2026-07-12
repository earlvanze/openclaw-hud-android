package ai.openclaw.app

data class AirVisionFirmwareSyncPlan(
    val items: List<AirVisionFirmwareSyncItem>,
    val writeGate: AirVisionFirmwareWriteGate = AirVisionFirmwareWriteGate.fromItems(items),
) {
    val applyPreview: AirVisionFirmwareApplyPreview
        get() = AirVisionFirmwareApplyPreview.fromItems(items, writeGate)

    val pendingHardwareSyncCount: Int
        get() = items.count { it.hardwareSyncPending }

    val androidAppliedCount: Int
        get() = items.count { it.androidApplied }

    val firmwareWriteAllowedCount: Int
        get() = items.count { it.firmwareWriteAllowed }

    val blockedFirmwareWriteCount: Int
        get() = items.count { !it.firmwareWriteAllowed }

    val summary: String
        get() =
            if (items.isEmpty()) {
                "firmware sync: no Windows-style controls configured"
            } else {
                "firmware sync: $androidAppliedCount Android-applied, " +
                    "$pendingHardwareSyncCount pending ASUS HID sync"
            }

    val writeGateSummary: String
        get() = writeGate.summary

    val detailSummary: String
        get() = "firmware desired state: ${items.joinToString("; ") { it.summary }}"
}

data class AirVisionFirmwareApplyPreview(
    val status: String,
    val commands: List<AirVisionFirmwareApplyCommand>,
    val blockedFeatureLabels: List<String>,
    val summary: String,
) {
    val commandCount: Int
        get() = commands.size

    val readyCommandCount: Int
        get() = commands.count { it.protocolReady }

    companion object {
        fun fromItems(
            items: List<AirVisionFirmwareSyncItem>,
            writeGate: AirVisionFirmwareWriteGate,
        ): AirVisionFirmwareApplyPreview {
            val commands =
                items.mapNotNull { item ->
                    AirVisionFirmwareApplyCommand.fromItem(item)
                }
            val blockedLabels =
                items
                    .filterNot { it.hasValidatedWriteEnablement }
                    .map { it.feature.label }
            val status =
                when {
                    commands.isEmpty() -> "no_protocol_ready_commands"
                    writeGate.firmwareWritesEnabled -> "ready_to_apply"
                    else -> "blocked_until_live_m1_test"
                }
            return AirVisionFirmwareApplyPreview(
                status = status,
                commands = commands,
                blockedFeatureLabels = blockedLabels,
                summary =
                    "firmware apply preview: ${commands.size} protocol-ready, " +
                        "${blockedLabels.size} blocked, writes ${if (writeGate.firmwareWritesEnabled) "enabled" else "disabled"}",
            )
        }
    }
}

data class AirVisionFirmwareApplyCommand(
    val feature: AirVisionFirmwareFeature,
    val desiredValue: String,
    val writeReportId: String,
    val writeEndpoint: String,
    val writePayloadSummary: String,
    val readbackReportId: String,
    val readbackEndpoint: String,
    val readbackPayloadSummary: String,
    val checksumFramingNotes: String,
    val protocolReady: Boolean,
    val blockedReason: String,
) {
    val summary: String
        get() = "${feature.label}=$desiredValue via $writeReportId on $writeEndpoint"

    companion object {
        fun fromItem(item: AirVisionFirmwareSyncItem): AirVisionFirmwareApplyCommand? {
            val capture = item.captureResult ?: return null
            if (!item.hasValidatedWriteEnablement) return null
            return AirVisionFirmwareApplyCommand(
                feature = item.feature,
                desiredValue = item.desiredValue,
                writeReportId = capture.writeReportId.orEmpty(),
                writeEndpoint = capture.writeEndpoint.orEmpty(),
                writePayloadSummary = capture.writePayloadSummary.orEmpty(),
                readbackReportId = capture.readbackReportId.orEmpty(),
                readbackEndpoint = capture.readbackEndpoint.orEmpty(),
                readbackPayloadSummary = capture.readbackPayloadSummary.orEmpty(),
                checksumFramingNotes = capture.checksumFramingNotes.orEmpty(),
                protocolReady = true,
                blockedReason = item.blockedReason,
            )
        }
    }
}

data class AirVisionFirmwareWriteGate(
    val status: String,
    val firmwareWritesEnabled: Boolean,
    val validatedCaptureCount: Int,
    val writeEnabledCaptureCount: Int,
    val blockedFeatureCount: Int,
    val protocolReadyFeatureLabels: List<String>,
    val blockedFeatureLabels: List<String>,
    val blockedFeatureSummaries: List<String>,
    val liveTestChecklist: List<String>,
    val liveM1Required: Boolean,
    val explicitUserConfirmationRequired: Boolean,
    val summary: String,
    val nextStep: String,
) {
    companion object {
        private val liveM1WriteTestChecklist =
            listOf(
                "Reconnect the AirVision M1 to the Android device.",
                "Grant USB permission and confirm readable plus writable HID report paths.",
                "Replay only one validated feature report sequence at a time.",
                "Read back the matching report and verify checksum/framing.",
                "Confirm the visible M1 state changed as expected before enabling the next feature.",
            )

        fun fromItems(items: List<AirVisionFirmwareSyncItem>): AirVisionFirmwareWriteGate {
            if (items.isEmpty()) {
                return AirVisionFirmwareWriteGate(
                    status = "not_configured",
                    firmwareWritesEnabled = false,
                    validatedCaptureCount = 0,
                    writeEnabledCaptureCount = 0,
                    blockedFeatureCount = 0,
                    protocolReadyFeatureLabels = emptyList(),
                    blockedFeatureLabels = emptyList(),
                    blockedFeatureSummaries = emptyList(),
                    liveTestChecklist = liveM1WriteTestChecklist,
                    liveM1Required = true,
                    explicitUserConfirmationRequired = true,
                    summary = "firmware writes: no Windows-style controls configured",
                    nextStep = "Configure AirVision Windows-style controls before testing hardware writes.",
                )
            }

            val validatedCaptureCount = items.count { it.captureResultStatus == "validated" }
            val writeEnabledCaptureCount = items.count { it.hasValidatedWriteEnablement }
            val blockedFeatureCount = items.count { !it.firmwareWriteAllowed }
            val protocolReadyFeatureLabels =
                items
                    .filter { it.hasValidatedWriteEnablement }
                    .map { it.feature.label }
            val blockedFeatureLabels =
                items
                    .filter { !it.firmwareWriteAllowed }
                    .map { it.feature.label }
            val blockedFeatureSummaries =
                items
                    .filter { !it.firmwareWriteAllowed }
                    .map { "${it.feature.label}: ${it.blockedReason}" }
            val total = items.size
            val status =
                when {
                    writeEnabledCaptureCount > 0 -> "read_only_live_test_required"
                    validatedCaptureCount > 0 -> "read_only_capture_review_required"
                    items.any { it.hardwareSyncStatus == "capture pending" } -> "read_only_capture_pending"
                    else -> "read_only_hid_path_pending"
                }
            val nextStep =
                when (status) {
                    "read_only_live_test_required" ->
                        "Keep Android firmware writes disabled until the validated report sequence is implemented and live-tested with the M1 connected."
                    "read_only_capture_review_required" ->
                        "Review validated capture evidence and explicitly mark eligible features enable_android_write only after sanitized write/readback proof is complete."
                    "read_only_capture_pending" ->
                        "Capture and validate ASUS HID report payloads on Windows/Cyber for each Windows-style control."
                    else ->
                        "Reconnect the AirVision M1 and grant USB permission until Android exposes readable and writable HID report paths."
                }
            return AirVisionFirmwareWriteGate(
                status = status,
                firmwareWritesEnabled = false,
                validatedCaptureCount = validatedCaptureCount,
                writeEnabledCaptureCount = writeEnabledCaptureCount,
                blockedFeatureCount = blockedFeatureCount,
                protocolReadyFeatureLabels = protocolReadyFeatureLabels,
                blockedFeatureLabels = blockedFeatureLabels,
                blockedFeatureSummaries = blockedFeatureSummaries,
                liveTestChecklist = liveM1WriteTestChecklist,
                liveM1Required = true,
                explicitUserConfirmationRequired = true,
                summary =
                    "firmware writes: read-only; " +
                        "$validatedCaptureCount/$total validated captures, " +
                        "$writeEnabledCaptureCount protocol-ready, " +
                        "$blockedFeatureCount blocked",
                nextStep = nextStep,
            )
        }
    }
}

data class AirVisionFirmwareSyncItem(
    val feature: AirVisionFirmwareFeature,
    val desiredValue: String,
    val androidApplied: Boolean,
    val androidEffect: String,
    val hardwareSyncPending: Boolean,
    val hardwareSyncStatus: String,
    val captureResultStatus: String,
    val androidEnablementDecision: String,
    val firmwareWriteAllowed: Boolean,
    val requiredEvidence: List<String>,
    val blockedReason: String,
    val captureResult: AirVisionFirmwareCaptureResult? = null,
) {
    val summary: String
        get() = "${feature.label}=$desiredValue ($hardwareSyncStatus)"

    val hasValidatedWriteEnablement: Boolean
        get() = captureResultStatus == "validated" && androidEnablementDecision == "enable_android_write"

    val companionStatusBadge: String
        get() =
            when {
                hasValidatedWriteEnablement -> "READY"
                captureResultStatus == "validated" -> "REVIEW"
                hardwareSyncStatus == "capture pending" -> "CAPTURE"
                else -> "WAIT"
            }

    val companionStatusText: String
        get() =
            buildList {
                add("Target: $desiredValue.")
                add("Android: $androidEffect.")
                add("Firmware: $hardwareSyncStatus.")
                if (captureResultStatus != "pending_validated_capture_result") {
                    add("Capture: $captureResultStatus; $androidEnablementDecision.")
                }
                if (requiredEvidence.isNotEmpty()) {
                    add("Needs: ${requiredEvidence.companionCompactLabelList()}.")
                }
                if (blockedReason.isNotBlank()) {
                    add("Blocked: $blockedReason")
                }
            }.joinToString("\n")
}

private fun List<String>.companionCompactLabelList(): String {
    val visible = take(4).joinToString()
    val hiddenCount = size - 4
    return if (hiddenCount > 0) "$visible, +$hiddenCount" else visible
}

object AirVisionFirmwareSyncPlans {
    const val CAPTURE_RESULTS_SCHEMA = "openclaw.airvision.firmwareCaptureResults"

    private val validatedWriteEvidence =
        listOf(
            "validated write report ID",
            "validated write endpoint",
            "sanitized write payload summary",
            "validated readback report ID",
            "validated readback endpoint",
            "sanitized readback payload summary",
            "checksum/framing notes",
            "visible M1 state confirmation",
            "capture reference with SHA-256 digest",
        )

    fun fromSettings(
        settings: AirVisionDisplaySettings,
        capabilities: AirVisionFirmwareCapabilities,
        captureResults: AirVisionFirmwareCaptureResults? = null,
    ): AirVisionFirmwareSyncPlan {
        val normalized = settings.normalized
        val captureResultsByFeature =
            captureResults
                ?.features
                ?.associateBy { it.rawKey }
                .orEmpty()
        return AirVisionFirmwareSyncPlan(
            items =
                AirVisionFirmwareFeature.entries.map { feature ->
                    feature.syncItemFor(
                        settings = normalized,
                        capabilities = capabilities,
                        captureResult = captureResultsByFeature[feature.rawValue],
                    )
                },
        )
    }

    private fun AirVisionFirmwareFeature.syncItemFor(
        settings: AirVisionDisplaySettings,
        capabilities: AirVisionFirmwareCapabilities,
        captureResult: AirVisionFirmwareCaptureResult?,
    ): AirVisionFirmwareSyncItem {
        val captureResultStatus = captureResult?.status ?: "pending_validated_capture_result"
        val androidEnablementDecision = captureResult?.androidEnablementDecision ?: "blocked"
        val hasValidatedAndroidWriteEvidence =
            captureResultStatus == "validated" &&
                androidEnablementDecision == "enable_android_write"
        val hardwareSyncStatus =
            when {
                hasValidatedAndroidWriteEvidence -> "validated capture imported"
                capabilities.hasWritableHidReports -> "capture pending"
                else -> "waiting for writable HID"
            }
        val blockedReason =
            when {
                hasValidatedAndroidWriteEvidence ->
                    "Validated capture result imported; Android HID firmware-write implementation remains disabled until live M1 testing."
                captureResult?.blockerReason?.isNotBlank() == true -> captureResult.blockerReason
                capabilities.hasWritableHidReports ->
                    "Writable HID path detected, but ASUS vendor report payloads are not validated."
                else ->
                    "Android has not exposed a writable AirVision HID report path."
            }
        return AirVisionFirmwareSyncItem(
            feature = this,
            desiredValue = desiredValueFor(settings),
            androidApplied = true,
            androidEffect = androidEffectFor(settings),
            hardwareSyncPending = true,
            hardwareSyncStatus = hardwareSyncStatus,
            captureResultStatus = captureResultStatus,
            androidEnablementDecision = androidEnablementDecision,
            firmwareWriteAllowed = false,
            requiredEvidence = if (hasValidatedAndroidWriteEvidence) emptyList() else validatedWriteEvidence,
            blockedReason = blockedReason,
            captureResult = captureResult,
        )
    }

    private fun AirVisionFirmwareFeature.desiredValueFor(settings: AirVisionDisplaySettings): String =
        when (this) {
            AirVisionFirmwareFeature.ViewMode -> settings.viewMode.label
            AirVisionFirmwareFeature.Brightness -> "${settings.brightnessPercent}%"
            AirVisionFirmwareFeature.ScreenDistance -> "${settings.distanceCm} cm"
            AirVisionFirmwareFeature.Ipd ->
                if (settings.ipdAdjustmentEnabled) {
                    "${settings.ipdMm} mm"
                } else {
                    "${settings.ipdMm} mm (locked by Light Load Mode)"
                }
            AirVisionFirmwareFeature.Splendid -> settings.splendidMode.label
            AirVisionFirmwareFeature.BlueLightFilter ->
                if (settings.blueLightFilterAvailable) {
                    "${settings.blueLightFilterPercent}%"
                } else {
                    "off (requires Eye Care)"
                }
            AirVisionFirmwareFeature.MotionSync -> if (settings.motionSyncEnabled) "on" else "off"
            AirVisionFirmwareFeature.LightLoadMode -> if (settings.lightLoadModeEnabled) "on" else "off"
            AirVisionFirmwareFeature.ThreeDMode ->
                if (settings.threeDModeAvailable && settings.threeDModeEnabled) {
                    "on"
                } else if (!settings.threeDModeAvailable) {
                    "off (locked by Light Load Mode)"
                } else {
                    "off"
                }
        }

    private fun AirVisionFirmwareFeature.androidEffectFor(settings: AirVisionDisplaySettings): String =
        when (this) {
            AirVisionFirmwareFeature.ViewMode -> "per-mode HUD profile"
            AirVisionFirmwareFeature.Brightness -> "software HUD dimming"
            AirVisionFirmwareFeature.ScreenDistance -> "virtual HUD scaling"
            AirVisionFirmwareFeature.Ipd ->
                if (settings.ipdAdjustmentEnabled) {
                    "profile calibration"
                } else {
                    "profile calibration locked by Light Load Mode"
                }
            AirVisionFirmwareFeature.Splendid -> "HUD color preview"
            AirVisionFirmwareFeature.BlueLightFilter ->
                if (settings.blueLightFilterAvailable) {
                    "Eye Care warm overlay"
                } else {
                    "inactive until Eye Care mode"
                }
            AirVisionFirmwareFeature.MotionSync -> "profile preference"
            AirVisionFirmwareFeature.LightLoadMode -> "low-overhead HUD profile"
            AirVisionFirmwareFeature.ThreeDMode ->
                if (settings.threeDModeAvailable) {
                    "profile preference"
                } else {
                    "disabled by Light Load Mode"
                }
        }
}
