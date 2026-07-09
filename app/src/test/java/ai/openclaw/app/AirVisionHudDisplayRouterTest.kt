package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AirVisionHudDisplayRouterTest {
    @Test
    fun choose_prefersAirVisionNamedDisplayByDefault() {
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
    fun fromRawValue_fallsBackToAirVisionPreferred() {
        assertEquals(
            AirVisionHudDisplayTarget.AirVisionPreferred,
            AirVisionHudDisplayTarget.fromRawValue("unknown"),
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
}
