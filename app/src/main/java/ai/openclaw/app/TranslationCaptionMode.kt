package ai.openclaw.app

import ai.openclaw.app.chat.ChatModelChoice

data class TranslationCaptionLanguage(
    val code: String,
    val label: String,
    val promptName: String,
)

object TranslationCaptionMode {
    const val DEFAULT_SOURCE_LANGUAGE = "auto"
    const val DEFAULT_TARGET_LANGUAGE = "es"
    const val DEFAULT_FAST_MODEL = "sage-router/fast"

    val languages: List<TranslationCaptionLanguage> =
        listOf(
            TranslationCaptionLanguage("auto", "Auto", "auto-detected language"),
            TranslationCaptionLanguage("en", "English", "English"),
            TranslationCaptionLanguage("es", "Spanish", "Spanish"),
            TranslationCaptionLanguage("fr", "French", "French"),
            TranslationCaptionLanguage("de", "German", "German"),
            TranslationCaptionLanguage("it", "Italian", "Italian"),
            TranslationCaptionLanguage("pt", "Portuguese", "Portuguese"),
            TranslationCaptionLanguage("ja", "Japanese", "Japanese"),
            TranslationCaptionLanguage("ko", "Korean", "Korean"),
            TranslationCaptionLanguage("zh", "Chinese", "Chinese"),
        )

    fun normalizeLanguageCode(
        raw: String?,
        defaultCode: String,
    ): String {
        val code = raw?.trim()?.lowercase().orEmpty()
        return languages.firstOrNull { it.code == code }?.code ?: defaultCode
    }

    fun languageFor(code: String): TranslationCaptionLanguage =
        languages.firstOrNull { it.code == code } ?: languages.first { it.code == DEFAULT_TARGET_LANGUAGE }

    fun speakerLabelForTurn(turnIndex: Int): String = if (turnIndex % 2 == 0) "S1" else "S2"

    fun buildPrompt(
        transcript: String,
        speakerLabel: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
    ): String {
        val source = languageFor(normalizeLanguageCode(sourceLanguageCode, DEFAULT_SOURCE_LANGUAGE)).promptName
        val target = languageFor(normalizeLanguageCode(targetLanguageCode, DEFAULT_TARGET_LANGUAGE)).promptName
        return listOf(
            "Translate this live caption turn from $source to $target.",
            "Keep it fast. Do not explain. Do not add commentary.",
            "Preserve the speaker label exactly.",
            "Respond with exactly one line in this format:",
            "$speakerLabel: <translation>",
            "",
            "$speakerLabel: ${transcript.trim()}",
        ).joinToString("\n")
    }

    fun preferredFastModel(choices: List<ChatModelChoice>): String =
        choices
            .asSequence()
            .map { choice -> normalizedModelId(choice) to modelSearchText(choice) }
            .firstOrNull { (id, text) -> id == DEFAULT_FAST_MODEL || text.contains("sage-router") && text.contains("fast") }
            ?.first
            ?: choices
                .asSequence()
                .map { choice -> normalizedModelId(choice) to modelSearchText(choice) }
                .firstOrNull { (_, text) -> text.contains("fast") }
                ?.first
            ?: DEFAULT_FAST_MODEL

    fun stripSpeakerPrefix(text: String): Pair<String?, String> {
        val trimmed = text.trim()
        val match = Regex("^(S[12])\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE).find(trimmed)
        if (match == null) return null to trimmed
        return match.groupValues[1].uppercase() to match.groupValues[2].trim()
    }

    private fun normalizedModelId(choice: ChatModelChoice): String {
        val id = choice.id.trim()
        val provider = choice.provider.trim()
        if (provider.isEmpty() || id.startsWith("$provider/")) return id
        return "$provider/$id"
    }

    private fun modelSearchText(choice: ChatModelChoice): String =
        listOf(choice.provider, choice.id, choice.name, choice.alias.orEmpty())
            .joinToString(" ")
            .lowercase()
}
