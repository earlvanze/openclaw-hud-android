package ai.openclaw.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HudCaptionProviderTest {
    @Test
    fun next_cyclesOffNativeOpenClawOff() {
        assertEquals(HudCaptionProvider.Native, HudCaptionProviders.next(HudCaptionProvider.Off))
        assertEquals(HudCaptionProvider.OpenClaw, HudCaptionProviders.next(HudCaptionProvider.Native))
        assertEquals(HudCaptionProvider.Off, HudCaptionProviders.next(HudCaptionProvider.OpenClaw))
    }

    @Test
    fun current_prefersNativeWhenStatesConflict() {
        assertEquals(
            HudCaptionProvider.Native,
            HudCaptionProviders.current(nativeEnabled = true, openClawEnabled = true),
        )
    }

    @Test
    fun next_usesCurrentState() {
        assertEquals(
            HudCaptionProvider.Native,
            HudCaptionProviders.next(nativeEnabled = false, openClawEnabled = false),
        )
        assertEquals(
            HudCaptionProvider.OpenClaw,
            HudCaptionProviders.next(nativeEnabled = true, openClawEnabled = false),
        )
        assertEquals(
            HudCaptionProvider.Off,
            HudCaptionProviders.next(nativeEnabled = false, openClawEnabled = true),
        )
    }
}
