package ai.openclaw.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AirVisionAppLocaleTest {
    @Test
    fun storedLanguage_readsPlainAirVisionPreferenceWithoutSecurePrefs() {
        val context = RuntimeEnvironment.getApplication()
        val plainPrefs = context.getSharedPreferences("openclaw.node", Context.MODE_PRIVATE)
        plainPrefs.edit().clear().putString("airVision.app.language", "es").commit()

        assertEquals(AirVisionAppLanguage.Spanish, AirVisionAppLocale.storedLanguage(context))
    }

    @Test
    fun languageTagFor_mapsSystemToNull() {
        assertNull(AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.System))
    }

    @Test
    fun languageTagFor_mapsAirVisionLanguagesToAndroidTags() {
        assertEquals("en", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.English))
        assertEquals("es", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.Spanish))
        assertEquals("fr", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.French))
        assertEquals("de", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.German))
        assertEquals("ja", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.Japanese))
        assertEquals("ko", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.Korean))
    }

    @Test
    fun languageTagFor_usesScriptTagsForChineseVariants() {
        assertEquals("zh-Hans", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.ChineseSimplified))
        assertEquals("zh-Hant", AirVisionAppLocale.languageTagFor(AirVisionAppLanguage.ChineseTraditional))
    }
}
