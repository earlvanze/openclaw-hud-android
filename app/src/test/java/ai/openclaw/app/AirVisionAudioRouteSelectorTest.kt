package ai.openclaw.app

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirVisionAudioRouteSelectorTest {
    @Test
    fun choose_prefersNamedAirVisionOutputOverGenericHdmi() {
        val selected =
            AirVisionAudioRouteSelector.choose(
                listOf(
                    output(id = 1, type = AudioDeviceInfo.TYPE_HDMI, productName = "External display"),
                    output(id = 2, type = AudioDeviceInfo.TYPE_USB_DEVICE, productName = "ASUS AirVision M1"),
                ),
            )

        assertEquals(2, selected?.id)
    }

    @Test
    fun choose_usesHdmiAsExternalDisplayFallback() {
        val selected =
            AirVisionAudioRouteSelector.choose(
                listOf(
                    output(id = 1, type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, productName = "Phone speaker"),
                    output(id = 7, type = AudioDeviceInfo.TYPE_HDMI, productName = "Samsung DeX display"),
                ),
            )

        assertEquals(7, selected?.id)
    }

    @Test
    fun choose_doesNotTreatGenericM1NameAsAirVision() {
        val selected =
            AirVisionAudioRouteSelector.choose(
                listOf(
                    output(id = 3, type = AudioDeviceInfo.TYPE_USB_DEVICE, productName = "Studio M1 DAC"),
                    output(id = 4, type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, productName = "Phone speaker"),
                ),
            )

        assertNull(selected)
    }

    @Test
    fun choose_ignoresNamedAirVisionInputOnlyDevices() {
        val selected =
            AirVisionAudioRouteSelector.choose(
                listOf(
                    output(id = 5, type = AudioDeviceInfo.TYPE_BUILTIN_MIC, productName = "ASUS AirVision M1"),
                    output(id = 6, type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, productName = "Phone speaker"),
                ),
            )

        assertNull(selected)
    }

    private fun output(
        id: Int,
        type: Int,
        productName: String,
    ): AirVisionAudioOutputCandidate =
        AirVisionAudioOutputCandidate(
            id = id,
            type = type,
            productName = productName,
        )
}
