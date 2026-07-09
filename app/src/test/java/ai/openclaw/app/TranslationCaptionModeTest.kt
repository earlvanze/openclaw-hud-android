package ai.openclaw.app

import ai.openclaw.app.chat.ChatModelChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationCaptionModeTest {
    @Test
    fun normalizeLanguageCode_defaultsToAutoSourceAndSpanishTarget() {
        assertEquals("auto", TranslationCaptionMode.normalizeLanguageCode("unknown", "auto"))
        assertEquals("es", TranslationCaptionMode.normalizeLanguageCode("", "es"))
        assertEquals("fr", TranslationCaptionMode.normalizeLanguageCode(" FR ", "es"))
        assertEquals("pt", TranslationCaptionMode.normalizeLanguageCode("pt-BR", "es"))
        assertEquals("zh", TranslationCaptionMode.normalizeLanguageCode("zh_Hans", "es"))
    }

    @Test
    fun speakerLabelForTurn_alternatesTwoSpeakers() {
        assertEquals("S1", TranslationCaptionMode.speakerLabelForTurn(0))
        assertEquals("S2", TranslationCaptionMode.speakerLabelForTurn(1))
        assertEquals("S1", TranslationCaptionMode.speakerLabelForTurn(2))
    }

    @Test
    fun buildPrompt_forcesFastOneLineSpeakerPreservingTranslation() {
        val prompt =
            TranslationCaptionMode.buildPrompt(
                transcript = "Where is the train station?",
                speakerLabel = "S2",
                sourceLanguageCode = "auto",
                targetLanguageCode = "es",
            )

        assertTrue(prompt.contains("auto-detected language"))
        assertTrue(prompt.contains("Spanish"))
        assertTrue(prompt.contains("Keep it fast"))
        assertTrue(prompt.contains("S2: Where is the train station?"))
    }

    @Test
    fun preferredFastModel_prefersSageRouterFastProfile() {
        val model =
            TranslationCaptionMode.preferredFastModel(
                listOf(
                    ChatModelChoice(id = "balanced", name = "Balanced", provider = "sage-router"),
                    ChatModelChoice(id = "fast", name = "Fast", provider = "sage-router"),
                ),
            )

        assertEquals("sage-router/fast", model)
    }

    @Test
    fun languageMenuIncludesExpandedCaptionTargets() {
        val codes = TranslationCaptionMode.languages.map { it.code }

        assertTrue(codes.containsAll(listOf("ar", "hi", "nl", "pl", "ru", "th", "tr", "uk", "vi")))
    }

    @Test
    fun stripSpeakerPrefix_extractsSpeakerLabel() {
        val parsed = TranslationCaptionMode.stripSpeakerPrefix("S1: Hola")

        assertEquals("S1", parsed.first)
        assertEquals("Hola", parsed.second)
    }
}
