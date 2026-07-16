package ai.openclaw.app

import android.view.InputDevice

internal fun shouldForwardExternalHudTouch(
    deviceIsExternal: Boolean,
    eventSource: Int,
    eventDisplayId: Int,
    hudDisplayId: Int,
): Boolean =
    deviceIsExternal &&
        eventSource and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN &&
        eventDisplayId != hudDisplayId
