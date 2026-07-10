package ai.openclaw.app

data class AirVisionFirmwareSyncPlan(
    val items: List<AirVisionFirmwareSyncItem>,
) {
    val pendingHardwareSyncCount: Int
        get() = items.count { it.hardwareSyncPending }

    val androidAppliedCount: Int
        get() = items.count { it.androidApplied }

    val summary: String
        get() =
            if (items.isEmpty()) {
                "firmware sync: no Windows-style controls configured"
            } else {
                "firmware sync: $androidAppliedCount Android-applied, " +
                    "$pendingHardwareSyncCount pending ASUS HID sync"
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
    val blockedReason: String,
) {
    val summary: String
        get() = "${feature.label}=$desiredValue ($hardwareSyncStatus)"
}

object AirVisionFirmwareSyncPlans {
    fun fromSettings(
        settings: AirVisionDisplaySettings,
        capabilities: AirVisionFirmwareCapabilities,
    ): AirVisionFirmwareSyncPlan {
        val normalized = settings.normalized
        return AirVisionFirmwareSyncPlan(
            items =
                AirVisionFirmwareFeature.entries.map { feature ->
                    feature.syncItemFor(
                        settings = normalized,
                        capabilities = capabilities,
                    )
                },
        )
    }

    private fun AirVisionFirmwareFeature.syncItemFor(
        settings: AirVisionDisplaySettings,
        capabilities: AirVisionFirmwareCapabilities,
    ): AirVisionFirmwareSyncItem {
        val hardwareSyncStatus =
            when {
                capabilities.hasWritableHidReports -> "capture pending"
                else -> "waiting for writable HID"
            }
        val blockedReason =
            when {
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
