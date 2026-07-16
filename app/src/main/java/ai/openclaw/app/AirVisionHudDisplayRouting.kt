package ai.openclaw.app

enum class AirVisionHudDisplayTarget(
    val rawValue: String,
    val label: String,
) {
    Automatic("automatic", "Automatic"),
    RememberedExternal("remembered_external", "Remembered Display"),
    AirVisionPreferred("airvision_preferred", "AirVision Preferred"),
    LargestExternal("largest_external", "Largest External"),
    FirstExternal("first_external", "First External"),
    LastExternal("last_external", "Last External"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudDisplayTarget {
            val normalized = rawValue?.trim()?.lowercase()
            return entries.firstOrNull { it.rawValue == normalized } ?: Automatic
        }
    }
}

data class AirVisionHudDisplayCandidate(
    val displayId: Int,
    val name: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val isPresentation: Boolean = true,
    val isInternal: Boolean = false,
) {
    val areaPx: Long
        get() = widthPx.coerceAtLeast(0).toLong() * heightPx.coerceAtLeast(0).toLong()

    fun fingerprint(): AirVisionHudDisplayFingerprint =
        AirVisionHudDisplayFingerprint(
            name = name.trim(),
            widthPx = widthPx.coerceAtLeast(0),
            heightPx = heightPx.coerceAtLeast(0),
        )
}

data class AirVisionHudDisplayFingerprint(
    val name: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
) {
    val isConfigured: Boolean
        get() = name.isNotBlank() || (widthPx > 0 && heightPx > 0)

    fun label(): String {
        val displayName = name.ifBlank { "Unnamed display" }
        val size = if (widthPx > 0 && heightPx > 0) " ${widthPx}x$heightPx" else ""
        return "$displayName$size"
    }

    internal val normalizedName: String
        get() = name.trim().lowercase()
}

data class AirVisionHudDisplayRoute(
    val target: AirVisionHudDisplayTarget = AirVisionHudDisplayTarget.Automatic,
    val candidateCount: Int = 0,
    val presentationCandidateCount: Int = 0,
    val selectedCandidate: AirVisionHudDisplayCandidate? = null,
    val rememberedDisplay: AirVisionHudDisplayFingerprint? = null,
    val usedNonDefaultDisplayFallback: Boolean = false,
    val reason: String = "not_evaluated",
) {
    fun summaryText(): String {
        val selected =
            selectedCandidate?.let { candidate ->
                val size =
                    if (candidate.widthPx > 0 && candidate.heightPx > 0) {
                        " ${candidate.widthPx}x${candidate.heightPx}"
                    } else {
                        ""
                    }
                "Selected display ${candidate.displayId}: ${candidate.name.ifBlank { "Unnamed" }}$size"
            } ?: when (reason) {
                "activity_on_external_display" -> "Activity is already running on an external display"
                "display_manager_unavailable" -> "Android display manager is unavailable"
                "no_external_displays" -> "No external display is available"
                "remembered_display_not_configured" -> "No external display has been remembered"
                "remembered_display_unavailable" ->
                    "Remembered display ${rememberedDisplay?.label() ?: "is unavailable"} is unavailable"
                else -> "No presentation display is selected yet"
            }
        val presentationSummary = "$presentationCandidateCount/$candidateCount presentation-capable external display(s)."
        val routeSummary =
            when {
                selectedCandidate == null -> "Waiting for an Android Presentation display."
                usedNonDefaultDisplayFallback -> "Using non-default display fallback."
                else -> "Using Android Presentation display category."
            }
        return "$selected. $presentationSummary $routeSummary"
    }
}

object AirVisionHudDisplayRouter {
    fun select(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
        rememberedDisplay: AirVisionHudDisplayFingerprint? = null,
    ): AirVisionHudDisplayRoute {
        val externalCandidates =
            candidates.filter { candidate ->
                candidate.displayId != ANDROID_DEFAULT_DISPLAY_ID && !candidate.isInternal
            }
        if (externalCandidates.isEmpty()) {
            return AirVisionHudDisplayRoute(
                target = target,
                rememberedDisplay = rememberedDisplay,
                reason = "no_external_displays",
            )
        }
        val presentationCandidates = externalCandidates.filter { it.isPresentation }
        val eligibleCandidates = presentationCandidates.ifEmpty { externalCandidates }
        val selectedCandidate = chooseFromEligible(eligibleCandidates, target, rememberedDisplay)
        val rememberedSelection = target == AirVisionHudDisplayTarget.RememberedExternal
        return AirVisionHudDisplayRoute(
            target = target,
            candidateCount = externalCandidates.size,
            presentationCandidateCount = presentationCandidates.size,
            selectedCandidate = selectedCandidate,
            rememberedDisplay = rememberedDisplay,
            usedNonDefaultDisplayFallback = selectedCandidate != null && presentationCandidates.isEmpty(),
            reason =
                when {
                    rememberedSelection && rememberedDisplay?.isConfigured != true -> "remembered_display_not_configured"
                    rememberedSelection && selectedCandidate == null -> "remembered_display_unavailable"
                    rememberedSelection && presentationCandidates.isEmpty() ->
                        "selected_remembered_non_default_display_fallback"
                    rememberedSelection -> "selected_remembered_presentation_display"
                    selectedCandidate == null -> "no_display_selected"
                    presentationCandidates.isEmpty() -> "selected_non_default_display_fallback"
                    else -> "selected_presentation_display"
                },
        )
    }

    fun choose(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
        rememberedDisplay: AirVisionHudDisplayFingerprint? = null,
    ): AirVisionHudDisplayCandidate? = select(candidates, target, rememberedDisplay).selectedCandidate

    private fun chooseFromEligible(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
        rememberedDisplay: AirVisionHudDisplayFingerprint?,
    ): AirVisionHudDisplayCandidate? =
        when (target) {
            AirVisionHudDisplayTarget.Automatic ->
                candidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { it.areaPx }
                        .thenBy { scoreAirVisionPreferred(it) }
                        .thenBy { it.name.trim().lowercase() }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.RememberedExternal ->
                chooseRemembered(candidates, rememberedDisplay)
            AirVisionHudDisplayTarget.AirVisionPreferred ->
                candidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { scoreAirVisionPreferred(it) }
                        .thenBy { it.areaPx }
                        .thenBy { it.name.trim().lowercase() }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.LargestExternal ->
                candidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { it.areaPx }
                        .thenBy { scoreAirVisionPreferred(it) }
                        .thenBy { it.name.trim().lowercase() }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.FirstExternal -> candidates.minByOrNull { it.displayId }
            AirVisionHudDisplayTarget.LastExternal -> candidates.maxByOrNull { it.displayId }
        }

    private fun chooseRemembered(
        candidates: List<AirVisionHudDisplayCandidate>,
        rememberedDisplay: AirVisionHudDisplayFingerprint?,
    ): AirVisionHudDisplayCandidate? {
        if (rememberedDisplay?.isConfigured != true) return null
        val exactMatches =
            candidates.filter { candidate ->
                candidate.name.trim().lowercase() == rememberedDisplay.normalizedName &&
                    candidate.widthPx == rememberedDisplay.widthPx &&
                    candidate.heightPx == rememberedDisplay.heightPx
            }
        if (exactMatches.isNotEmpty()) return exactMatches.minByOrNull { it.displayId }

        if (rememberedDisplay.normalizedName.isNotBlank()) {
            val nameMatches = candidates.filter { it.name.trim().lowercase() == rememberedDisplay.normalizedName }
            if (nameMatches.size == 1) return nameMatches.single()
        }

        if (rememberedDisplay.widthPx > 0 && rememberedDisplay.heightPx > 0) {
            val sizeMatches =
                candidates.filter {
                    it.widthPx == rememberedDisplay.widthPx && it.heightPx == rememberedDisplay.heightPx
                }
            if (sizeMatches.size == 1) return sizeMatches.single()
        }
        return null
    }

    fun scoreAirVisionPreferred(candidate: AirVisionHudDisplayCandidate): Int {
        val name = candidate.name
        var score = 1
        if (name.contains("AirVision", ignoreCase = true)) score += 20
        if (name.contains("ASUS", ignoreCase = true)) score += 10
        if (name.contains("M1", ignoreCase = true)) score += 6
        return score
    }

    private const val ANDROID_DEFAULT_DISPLAY_ID = 0
}
