package ai.openclaw.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

fun openNativeCaptionSettings(context: Context) {
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
