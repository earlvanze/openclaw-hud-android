package ai.openclaw.app

import android.view.InputDevice

internal fun shouldForwardExternalHudTouch(
    deviceIsExternal: Boolean,
    eventSource: Int,
    eventDisplayId: Int,
    hudDisplayId: Int,
): Boolean =
    deviceIsExternal &&
        isHudTouchLikeSource(eventSource) &&
        eventDisplayId != hudDisplayId

internal fun isHudTouchLikeSource(eventSource: Int): Boolean =
    eventSource and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN ||
        eventSource and InputDevice.SOURCE_TOUCHPAD == InputDevice.SOURCE_TOUCHPAD ||
        eventSource and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
        eventSource and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE ||
        eventSource and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE ||
        eventSource and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS ||
        eventSource and InputDevice.SOURCE_TRACKBALL == InputDevice.SOURCE_TRACKBALL
