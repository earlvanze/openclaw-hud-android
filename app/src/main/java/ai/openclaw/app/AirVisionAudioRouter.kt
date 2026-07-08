package ai.openclaw.app

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

object AirVisionAudioRouter {
    private const val TAG = "AirVisionAudioRouter"

    fun preferredOutput(context: Context): AudioDeviceInfo? {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD) return null
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        val outputs =
            audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .toList()
        val namedM1 = outputs.firstOrNull { it.isAirVisionM1Output() }
        if (namedM1 != null) return namedM1

        val externalDisplayAudio = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_HDMI }
        if (externalDisplayAudio != null) return externalDisplayAudio

        Log.i(TAG, "no M1 output; available=${outputs.joinToString { it.routeLabel() }}")
        return null
    }

    fun applyHudRoute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val target = preferredOutput(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val applied = audioManager.setCommunicationDevice(target)
            Log.i(TAG, "preferred communication output=${target.routeLabel()} applied=$applied")
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            Log.i(TAG, "preferred output=${target.routeLabel()}; communication routing requires Android 12+")
        }
    }

    private fun AudioDeviceInfo.isAirVisionM1Output(): Boolean {
        val name = productName?.toString().orEmpty()
        return (
            name.contains("AirVision", ignoreCase = true) ||
                name.contains("ASUS", ignoreCase = true) ||
                name.contains("M1", ignoreCase = true)
        ) && (
            type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_HDMI
        )
    }

    private fun AudioDeviceInfo.routeLabel(): String =
        "type=$type name=${productName?.toString().orEmpty().ifBlank { "(blank)" }}"
}
