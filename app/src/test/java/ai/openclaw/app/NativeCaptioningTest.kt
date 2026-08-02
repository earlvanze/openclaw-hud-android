package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeCaptioningTest {
    @Test
    fun systemCaptionProvider_targetsLiveTranscribePackage() {
        assertEquals(
            "com.google.audio.hearing.visualization.accessibility.scribe",
            SYSTEM_LIVE_TRANSCRIBE_PACKAGE,
        )
    }
}
