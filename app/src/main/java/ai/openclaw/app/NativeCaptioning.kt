package ai.openclaw.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

internal const val SYSTEM_LIVE_TRANSCRIBE_PACKAGE =
    "com.google.audio.hearing.visualization.accessibility.scribe"

fun openNativeCaptionSettings(context: Context) {
    val liveTranscribeIntent =
        context.packageManager
            .getLaunchIntentForPackage(SYSTEM_LIVE_TRANSCRIBE_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (liveTranscribeIntent != null) {
        try {
            context.startActivity(liveTranscribeIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Continue to the Android caption-preferences fallback below.
        } catch (_: SecurityException) {
            // Some OEM system transcription packages are installed but do not
            // expose their launcher activity to third-party apps.
        }
    }

    val captionIntent =
        Intent(Settings.ACTION_CAPTIONING_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(captionIntent)
        return
    } catch (_: ActivityNotFoundException) {
    }

    val accessibilityIntent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(accessibilityIntent)
}
