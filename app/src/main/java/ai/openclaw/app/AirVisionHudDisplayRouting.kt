package ai.openclaw.app

enum class AirVisionHudDisplayTarget(
    val rawValue: String,
    val label: String,
) {
    AirVisionPreferred("airvision_preferred", "AirVision Preferred"),
    LargestExternal("largest_external", "Largest External"),
    FirstExternal("first_external", "First External"),
    LastExternal("last_external", "Last External"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudDisplayTarget =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: AirVisionPreferred
    }
}

data class AirVisionHudDisplayCandidate(
    val displayId: Int,
    val name: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val isPresentation: Boolean = true,
) {
    val areaPx: Long
        get() = widthPx.coerceAtLeast(0).toLong() * heightPx.coerceAtLeast(0).toLong()
}

data class AirVisionHudDisplayRoute(
    val target: AirVisionHudDisplayTarget = AirVisionHudDisplayTarget.AirVisionPreferred,
    val candidateCount: Int = 0,
    val presentationCandidateCount: Int = 0,
    val selectedCandidate: AirVisionHudDisplayCandidate? = null,
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
    ): AirVisionHudDisplayRoute {
        if (candidates.isEmpty()) {
            return AirVisionHudDisplayRoute(target = target, reason = "no_external_displays")
        }
        val presentationCandidates = candidates.filter { it.isPresentation }
        val eligibleCandidates = presentationCandidates.ifEmpty { candidates }
        val selectedCandidate = chooseFromEligible(eligibleCandidates, target)
        return AirVisionHudDisplayRoute(
            target = target,
            candidateCount = candidates.size,
            presentationCandidateCount = presentationCandidates.size,
            selectedCandidate = selectedCandidate,
            usedNonDefaultDisplayFallback = presentationCandidates.isEmpty(),
            reason =
                when {
                    selectedCandidate == null -> "no_display_selected"
                    presentationCandidates.isEmpty() -> "selected_non_default_display_fallback"
                    else -> "selected_presentation_display"
                },
        )
    }

    fun choose(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
    ): AirVisionHudDisplayCandidate? = select(candidates, target).selectedCandidate

    private fun chooseFromEligible(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
    ): AirVisionHudDisplayCandidate? =
        when (target) {
            AirVisionHudDisplayTarget.AirVisionPreferred ->
                candidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { scoreAirVisionPreferred(it) }
                        .thenBy { it.areaPx }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.LargestExternal ->
                candidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { it.areaPx }
                        .thenBy { scoreAirVisionPreferred(it) }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.FirstExternal -> candidates.minByOrNull { it.displayId }
            AirVisionHudDisplayTarget.LastExternal -> candidates.maxByOrNull { it.displayId }
        }

    fun scoreAirVisionPreferred(candidate: AirVisionHudDisplayCandidate): Int {
        val name = candidate.name
        var score = 1
        if (name.contains("AirVision", ignoreCase = true)) score += 20
        if (name.contains("ASUS", ignoreCase = true)) score += 10
        if (name.contains("M1", ignoreCase = true)) score += 6
        return score
    }
}
