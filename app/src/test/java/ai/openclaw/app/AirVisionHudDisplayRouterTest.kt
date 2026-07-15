package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AirVisionHudDisplayRouterTest {
    @Test
    fun choose_automaticUsesLargestPresentationDisplay() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(
                    displayId = 2,
                    name = "Generic HDMI",
                    widthPx = 3840,
                    heightPx = 2160,
                ),
                AirVisionHudDisplayCandidate(
                    displayId = 5,
                    name = "ASUS AirVision M1",
                    widthPx = 1920,
                    heightPx = 1080,
                ),
            )

        val selected = AirVisionHudDisplayRouter.choose(candidates, AirVisionHudDisplayTarget.Automatic)

        assertEquals(2, selected?.displayId)
    }

    @Test
    fun choose_airVisionPreferenceRemainsAvailableExplicitly() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(
                    displayId = 2,
                    name = "Samsung DeX Monitor",
                    widthPx = 3840,
                    heightPx = 2160,
                ),
                AirVisionHudDisplayCandidate(
                    displayId = 5,
                    name = "ASUS AirVision M1",
                    widthPx = 1920,
                    heightPx = 1080,
                ),
            )

        val selected = AirVisionHudDisplayRouter.choose(candidates, AirVisionHudDisplayTarget.AirVisionPreferred)

        assertEquals(5, selected?.displayId)
    }

    @Test
    fun select_ignoresPhoneDefaultDisplayWhenRoutingPresentation() {
        val selection =
            AirVisionHudDisplayRouter.select(
                candidates =
                    listOf(
                        AirVisionHudDisplayCandidate(
                            displayId = 0,
                            name = "Built-in Fold display",
                            widthPx = 2176,
                            heightPx = 1812,
                            isPresentation = true,
                        ),
                        AirVisionHudDisplayCandidate(
                            displayId = 5,
                            name = "ASUS AirVision M1",
                            widthPx = 1920,
                            heightPx = 1080,
                            isPresentation = true,
                        ),
                    ),
                target = AirVisionHudDisplayTarget.LargestExternal,
            )

        assertEquals(1, selection.candidateCount)
        assertEquals(1, selection.presentationCandidateCount)
        assertEquals(5, selection.selectedCandidate?.displayId)
        assertEquals("selected_presentation_display", selection.reason)
    }

    @Test
    fun choose_canPreferLargestExternalDisplay() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(
                    displayId = 2,
                    name = "Samsung DeX Monitor",
                    widthPx = 3840,
                    heightPx = 2160,
                ),
                AirVisionHudDisplayCandidate(
                    displayId = 5,
                    name = "ASUS AirVision M1",
                    widthPx = 1920,
                    heightPx = 1080,
                ),
            )

        val selected = AirVisionHudDisplayRouter.choose(candidates, AirVisionHudDisplayTarget.LargestExternal)

        assertEquals(2, selected?.displayId)
    }

    @Test
    fun choose_supportsFirstAndLastExternalFallbacks() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(displayId = 8, name = "External 8"),
                AirVisionHudDisplayCandidate(displayId = 3, name = "External 3"),
                AirVisionHudDisplayCandidate(displayId = 6, name = "External 6"),
            )

        assertEquals(
            3,
            AirVisionHudDisplayRouter
                .choose(candidates, AirVisionHudDisplayTarget.FirstExternal)
                ?.displayId,
        )
        assertEquals(
            8,
            AirVisionHudDisplayRouter
                .choose(candidates, AirVisionHudDisplayTarget.LastExternal)
                ?.displayId,
        )
    }

    @Test
    fun fromRawValue_fallsBackToAutomatic() {
        assertEquals(
            AirVisionHudDisplayTarget.Automatic,
            AirVisionHudDisplayTarget.fromRawValue("unknown"),
        )
    }

    @Test
    fun fromRawValue_preservesExplicitAirVisionPreference() {
        assertEquals(
            AirVisionHudDisplayTarget.AirVisionPreferred,
            AirVisionHudDisplayTarget.fromRawValue("airvision_preferred"),
        )
    }

    @Test
    fun choose_rememberedDisplaySurvivesDisplayIdChanges() {
        val remembered = AirVisionHudDisplayFingerprint("Generic HDMI", 1920, 1080)
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(12, "Projector", 1920, 1080),
                AirVisionHudDisplayCandidate(41, "Generic HDMI", 1920, 1080),
            )

        val selected =
            AirVisionHudDisplayRouter.choose(
                candidates,
                AirVisionHudDisplayTarget.RememberedExternal,
                remembered,
            )

        assertEquals(41, selected?.displayId)
    }

    @Test
    fun choose_rememberedDisplayUsesUniqueNameWhenDisplayModeChanges() {
        val remembered = AirVisionHudDisplayFingerprint("Wireless living room", 1920, 1080)
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(4, "Wireless living room", 1280, 720),
                AirVisionHudDisplayCandidate(8, "Desk monitor", 1920, 1080),
            )

        val selected =
            AirVisionHudDisplayRouter.choose(
                candidates,
                AirVisionHudDisplayTarget.RememberedExternal,
                remembered,
            )

        assertEquals(4, selected?.displayId)
    }

    @Test
    fun choose_rememberedDisplayRefusesAmbiguousFallback() {
        val remembered = AirVisionHudDisplayFingerprint("Disconnected display", 1920, 1080)
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(4, "External display", 1920, 1080),
                AirVisionHudDisplayCandidate(8, "External display", 1920, 1080),
            )

        val selection =
            AirVisionHudDisplayRouter.select(
                candidates,
                AirVisionHudDisplayTarget.RememberedExternal,
                remembered,
            )

        assertEquals(null, selection.selectedCandidate)
        assertEquals("remembered_display_unavailable", selection.reason)
        assertEquals(false, selection.usedNonDefaultDisplayFallback)
    }

    @Test
    fun select_reportsRememberedDisplayNotConfigured() {
        val selection =
            AirVisionHudDisplayRouter.select(
                candidates = listOf(AirVisionHudDisplayCandidate(2, "HDMI", 1920, 1080)),
                target = AirVisionHudDisplayTarget.RememberedExternal,
            )

        assertEquals(null, selection.selectedCandidate)
        assertEquals("remembered_display_not_configured", selection.reason)
        assertEquals(
            "No external display has been remembered. 1/1 presentation-capable external display(s). " +
                "Waiting for an Android Presentation display.",
            selection.summaryText(),
        )
    }

    @Test
    fun choose_prefersPresentationEligibleDisplays() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(
                    displayId = 9,
                    name = "ASUS AirVision M1 mirrored desktop",
                    widthPx = 3840,
                    heightPx = 2160,
                    isPresentation = false,
                ),
                AirVisionHudDisplayCandidate(
                    displayId = 2,
                    name = "Samsung DeX Presentation",
                    widthPx = 1920,
                    heightPx = 1080,
                    isPresentation = true,
                ),
            )

        val selected = AirVisionHudDisplayRouter.choose(candidates, AirVisionHudDisplayTarget.AirVisionPreferred)

        assertEquals(2, selected?.displayId)
    }

    @Test
    fun select_reportsPresentationRouteMetadata() {
        val selection =
            AirVisionHudDisplayRouter.select(
                candidates =
                    listOf(
                        AirVisionHudDisplayCandidate(
                            displayId = 9,
                            name = "ASUS AirVision M1 mirrored desktop",
                            widthPx = 3840,
                            heightPx = 2160,
                            isPresentation = false,
                        ),
                        AirVisionHudDisplayCandidate(
                            displayId = 5,
                            name = "ASUS AirVision M1",
                            widthPx = 1920,
                            heightPx = 1080,
                            isPresentation = true,
                        ),
                    ),
                target = AirVisionHudDisplayTarget.AirVisionPreferred,
            )

        assertEquals(2, selection.candidateCount)
        assertEquals(1, selection.presentationCandidateCount)
        assertEquals(5, selection.selectedCandidate?.displayId)
        assertEquals(false, selection.usedNonDefaultDisplayFallback)
        assertEquals("selected_presentation_display", selection.reason)
    }

    @Test
    fun summaryText_reportsPresentationSelection() {
        val selection =
            AirVisionHudDisplayRoute(
                candidateCount = 2,
                presentationCandidateCount = 1,
                selectedCandidate =
                    AirVisionHudDisplayCandidate(
                        displayId = 5,
                        name = "ASUS AirVision M1",
                        widthPx = 1920,
                        heightPx = 1080,
                        isPresentation = true,
                    ),
                reason = "selected_presentation_display",
            )

        assertEquals(
            "Selected display 5: ASUS AirVision M1 1920x1080. " +
                "1/2 presentation-capable external display(s). " +
                "Using Android Presentation display category.",
            selection.summaryText(),
        )
    }

    @Test
    fun choose_fallsBackWhenNoPresentationEligibleDisplaysExist() {
        val candidates =
            listOf(
                AirVisionHudDisplayCandidate(
                    displayId = 9,
                    name = "ASUS AirVision M1",
                    widthPx = 1920,
                    heightPx = 1080,
                    isPresentation = false,
                ),
                AirVisionHudDisplayCandidate(
                    displayId = 2,
                    name = "Generic HDMI",
                    widthPx = 3840,
                    heightPx = 2160,
                    isPresentation = false,
                ),
            )

        val selected = AirVisionHudDisplayRouter.choose(candidates, AirVisionHudDisplayTarget.AirVisionPreferred)

        assertEquals(9, selected?.displayId)
    }

    @Test
    fun select_reportsFallbackRouteMetadata() {
        val selection =
            AirVisionHudDisplayRouter.select(
                candidates =
                    listOf(
                        AirVisionHudDisplayCandidate(
                            displayId = 9,
                            name = "ASUS AirVision M1",
                            widthPx = 1920,
                            heightPx = 1080,
                            isPresentation = false,
                        ),
                    ),
                target = AirVisionHudDisplayTarget.AirVisionPreferred,
            )

        assertEquals(1, selection.candidateCount)
        assertEquals(0, selection.presentationCandidateCount)
        assertEquals(9, selection.selectedCandidate?.displayId)
        assertEquals(true, selection.usedNonDefaultDisplayFallback)
        assertEquals("selected_non_default_display_fallback", selection.reason)
    }

    @Test
    fun summaryText_reportsFallbackSelection() {
        val selection =
            AirVisionHudDisplayRoute(
                candidateCount = 1,
                presentationCandidateCount = 0,
                selectedCandidate =
                    AirVisionHudDisplayCandidate(
                        displayId = 9,
                        name = "ASUS AirVision M1",
                        isPresentation = false,
                    ),
                usedNonDefaultDisplayFallback = true,
                reason = "selected_non_default_display_fallback",
            )

        assertEquals(
            "Selected display 9: ASUS AirVision M1. " +
                "0/1 presentation-capable external display(s). " +
                "Using non-default display fallback.",
            selection.summaryText(),
        )
    }

    @Test
    fun summaryText_reportsNoExternalDisplays() {
        val selection =
            AirVisionHudDisplayRoute(
                reason = "no_external_displays",
            )

        assertEquals(
            "No external display is available. " +
                "0/0 presentation-capable external display(s). " +
                "Waiting for an Android Presentation display.",
            selection.summaryText(),
        )
    }
}
