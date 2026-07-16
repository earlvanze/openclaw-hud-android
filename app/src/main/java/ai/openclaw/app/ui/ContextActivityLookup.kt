package ai.openclaw.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.findActivityOrNull(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityOrNull()
        else -> null
    }
