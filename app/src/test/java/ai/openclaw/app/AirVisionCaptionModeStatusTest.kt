package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionCaptionModeStatusTest {
    @Test
    fun from_normalizesLanguagePairAndDocumentsFastFallback() {
        val status =
            AirVisionCaptionModeStatus.from(
                nativeCaptionsEnabled = true,
                sourceLanguageCode = "pt-BR",
                targetLanguageCode = "ja",
            )

        assertEquals(true, status.nativeCaptionsEnabled)
        assertEquals("Android/Samsung native captions floating window", status.nativeProvider)
        assertEquals(true, status.openClawFallbackAvailable)
        assertEquals(TranslationCaptionMode.DEFAULT_FAST_MODEL, status.openClawFallbackModel)
        assertEquals("off", status.openClawFallbackThinking)
        assertEquals("pt", status.sourceLanguage)
        assertEquals("ja", status.targetLanguage)
        assertEquals(listOf("S1", "S2"), status.speakerLabels)
        assertTrue(status.summary.contains("Portuguese -> Japanese"))
        assertTrue(status.summary.contains("thinking off"))
    }
}
