package ai.openclaw.app

import kotlinx.serialization.Serializable

@Serializable
data class AirVisionCaptionModeStatus(
    val nativeCaptionsEnabled: Boolean,
    val nativeProvider: String,
    val openClawFallbackAvailable: Boolean,
    val openClawFallbackModel: String,
    val openClawFallbackThinking: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceLanguageLabel: String,
    val targetLanguageLabel: String,
    val speakerLabels: List<String>,
    val summary: String,
) {
    companion object {
        fun from(
            nativeCaptionsEnabled: Boolean,
            sourceLanguageCode: String,
            targetLanguageCode: String,
        ): AirVisionCaptionModeStatus {
            val sourceCode =
                TranslationCaptionMode.normalizeLanguageCode(
                    sourceLanguageCode,
                    TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
                )
            val targetCode =
                TranslationCaptionMode.normalizeLanguageCode(
                    targetLanguageCode,
                    TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
                )
            val source = TranslationCaptionMode.languageFor(sourceCode)
            val target = TranslationCaptionMode.languageFor(targetCode)
            return AirVisionCaptionModeStatus(
                nativeCaptionsEnabled = nativeCaptionsEnabled,
                nativeProvider = "Android/Samsung native captions floating window",
                openClawFallbackAvailable = true,
                openClawFallbackModel = TranslationCaptionMode.DEFAULT_FAST_MODEL,
                openClawFallbackThinking = "off",
                sourceLanguage = source.code,
                targetLanguage = target.code,
                sourceLanguageLabel = source.label,
                targetLanguageLabel = target.label,
                speakerLabels =
                    listOf(
                        TranslationCaptionMode.speakerLabelForTurn(0),
                        TranslationCaptionMode.speakerLabelForTurn(1),
                    ),
                summary =
                    "Captions: native ${if (nativeCaptionsEnabled) "on" else "off"}, " +
                        "OpenClaw fallback ${source.label} -> ${target.label}, " +
                        "model ${TranslationCaptionMode.DEFAULT_FAST_MODEL}, thinking off, speakers S1/S2.",
            )
        }

        fun from(preferences: AirVisionBackupAppPreferences): AirVisionCaptionModeStatus =
            from(
                nativeCaptionsEnabled = preferences.nativeCaptionsEnabled,
                sourceLanguageCode = preferences.translationCaptionSourceLanguage,
                targetLanguageCode = preferences.translationCaptionTargetLanguage,
            )
    }
}
