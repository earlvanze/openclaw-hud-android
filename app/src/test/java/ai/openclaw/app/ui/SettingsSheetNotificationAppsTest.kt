package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionCaptionModeStatus
import ai.openclaw.app.TranslationCaptionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSheetNotificationAppsTest {
    @Test
    fun resolveNotificationCandidatePackages_keepsConfiguredPackagesVisible() {
        val packages =
            resolveNotificationCandidatePackages(
                launcherPackages = setOf("com.example.launcher"),
                recentPackages = listOf("com.example.recent", "com.example.launcher"),
                configuredPackages = setOf("com.example.configured"),
                appPackageName = "ai.openclaw.app",
            )

        assertEquals(
            setOf("com.example.launcher", "com.example.recent", "com.example.configured"),
            packages,
        )
    }

    @Test
    fun resolveNotificationCandidatePackages_filtersBlankAndSelfPackages() {
        val packages =
            resolveNotificationCandidatePackages(
                launcherPackages = setOf(" ", "ai.openclaw.app"),
                recentPackages = listOf("com.example.recent", "  "),
                configuredPackages = setOf("ai.openclaw.app", "com.example.configured"),
                appPackageName = "ai.openclaw.app",
            )

        assertEquals(setOf("com.example.recent", "com.example.configured"), packages)
    }

    @Test
    fun airVisionCaptionModeSettingsText_summarizesNativeAndFallbackCaptionState() {
        val status =
            AirVisionCaptionModeStatus.from(
                nativeCaptionsEnabled = true,
                sourceLanguageCode = "auto",
                targetLanguageCode = "es",
            )

        val text = airVisionCaptionModeSettingsText(status)

        assertTrue(text.contains("Captions: native on, OpenClaw fallback Auto -> Spanish"))
        assertTrue(text.contains("Native provider: Android/Samsung native captions floating window"))
        assertTrue(text.contains("OpenClaw fallback: available"))
        assertTrue(text.contains("Fallback model: ${TranslationCaptionMode.DEFAULT_FAST_MODEL}; thinking off"))
        assertTrue(text.contains("Languages: Auto -> Spanish"))
        assertTrue(text.contains("Speaker labels: S1, S2"))
    }
}
