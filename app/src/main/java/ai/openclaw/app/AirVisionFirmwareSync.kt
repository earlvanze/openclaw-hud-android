package ai.openclaw.app

data class AirVisionFirmwareSyncPlan(
    val items: List<AirVisionFirmwareSyncItem>,
) {
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
        get() =
            if (items.isEmpty()) {
                "firmware writes: no Windows-style controls configured"
            } else {
                "firmware writes: $firmwareWriteAllowedCount enabled, " +
                    "$blockedFirmwareWriteCount blocked pending validated capture results"
            }

    val detailSummary: String
        get() = "firmware desired state: ${items.joinToString("; ") { it.summary }}"
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
) {
    val summary: String
        get() = "${feature.label}=$desiredValue ($hardwareSyncStatus)"
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
        )
    }

    private fun AirVisionFirmwareFeature.desiredValueFor(settings: AirVisionDisplaySettings): String =
        when (this) {
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
            AirVisionFirmwareFeature.ThreeDMode ->
                if (settings.threeDModeAvailable) {
                    "profile preference"
                } else {
                    "disabled by Light Load Mode"
                }
        }
}
