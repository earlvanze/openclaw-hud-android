package ai.openclaw.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
        val selected =
            AirVisionAudioRouteSelector.choose(
                outputs.map { output ->
                    AirVisionAudioOutputCandidate(
                        id = output.id,
                        type = output.type,
                        productName = output.productName?.toString().orEmpty(),
                    )
                },
            )
        if (selected != null) {
            return outputs.firstOrNull { it.id == selected.id }
        }

        Log.i(TAG, "no M1 output; available=${outputs.joinToString { it.routeLabel() }}")
        return null
    }

    @SuppressLint("SetAndClearCommunicationDevice")
    fun applyHudRoute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val target = preferredOutput(context) ?: return
        val applied = audioManager.setCommunicationDevice(target)
        Log.i(TAG, "preferred communication output=${target.routeLabel()} applied=$applied")
    }

    fun clearHudRoute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.clearCommunicationDevice()
    }

    private fun AudioDeviceInfo.routeLabel(): String =
        "type=$type name=${productName?.toString().orEmpty().ifBlank { "(blank)" }}"
}

internal data class AirVisionAudioOutputCandidate(
    val id: Int,
    val type: Int,
    val productName: String,
)

internal object AirVisionAudioRouteSelector {
    fun choose(outputs: List<AirVisionAudioOutputCandidate>): AirVisionAudioOutputCandidate? =
        outputs.firstOrNull { it.isNamedAirVisionM1Output() }
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_HDMI }

    private fun AirVisionAudioOutputCandidate.isNamedAirVisionM1Output(): Boolean {
        if (
            type != AudioDeviceInfo.TYPE_USB_DEVICE &&
            type != AudioDeviceInfo.TYPE_USB_HEADSET &&
            type != AudioDeviceInfo.TYPE_HDMI
        ) {
            return false
        }

        val name = productName.trim()
        if (name.contains("AirVision", ignoreCase = true)) return true
        return name.contains("ASUS", ignoreCase = true) && name.contains("M1", ignoreCase = true)
    }
}
