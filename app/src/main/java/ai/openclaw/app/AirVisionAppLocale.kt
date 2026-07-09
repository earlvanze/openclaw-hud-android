package ai.openclaw.app

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AirVisionAppLocale {
    private const val PLAIN_PREFS_NAME = "openclaw.node"
    private const val APP_LANGUAGE_KEY = "airVision.app.language"

    fun storedLanguage(context: Context): AirVisionAppLanguage =
        AirVisionAppLanguage.fromRawValue(
            context
                .getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(APP_LANGUAGE_KEY, null),
        )

    fun languageTagFor(language: AirVisionAppLanguage): String? =
        when (language) {
            AirVisionAppLanguage.System -> null
            AirVisionAppLanguage.English -> "en"
            AirVisionAppLanguage.Spanish -> "es"
            AirVisionAppLanguage.French -> "fr"
            AirVisionAppLanguage.German -> "de"
            AirVisionAppLanguage.Japanese -> "ja"
            AirVisionAppLanguage.Korean -> "ko"
            AirVisionAppLanguage.ChineseSimplified -> "zh-Hans"
            AirVisionAppLanguage.ChineseTraditional -> "zh-Hant"
        }

    fun wrap(
        base: Context,
        language: AirVisionAppLanguage,
    ): Context {
        val tag = languageTagFor(language) ?: return base
        val configuration = Configuration(base.resources.configuration)
        val locale = Locale.forLanguageTag(tag)
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList.forLanguageTags(tag))
        return base.createConfigurationContext(configuration)
    }

    fun apply(
        context: Context,
        language: AirVisionAppLanguage,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        val tag = languageTagFor(language)
        localeManager.applicationLocales =
            if (tag == null) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
    }
}
