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

object AirVisionHudDisplayRouter {
    fun choose(
        candidates: List<AirVisionHudDisplayCandidate>,
        target: AirVisionHudDisplayTarget,
    ): AirVisionHudDisplayCandidate? {
        if (candidates.isEmpty()) return null
        val eligibleCandidates = candidates.filter { it.isPresentation }.ifEmpty { candidates }
        return when (target) {
            AirVisionHudDisplayTarget.AirVisionPreferred ->
                eligibleCandidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { scoreAirVisionPreferred(it) }
                        .thenBy { it.areaPx }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.LargestExternal ->
                eligibleCandidates.maxWithOrNull(
                    compareBy<AirVisionHudDisplayCandidate> { it.areaPx }
                        .thenBy { scoreAirVisionPreferred(it) }
                        .thenBy { it.displayId },
                )
            AirVisionHudDisplayTarget.FirstExternal -> eligibleCandidates.minByOrNull { it.displayId }
            AirVisionHudDisplayTarget.LastExternal -> eligibleCandidates.maxByOrNull { it.displayId }
        }
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
