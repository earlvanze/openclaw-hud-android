package ai.openclaw.app.ui

import ai.openclaw.app.voice.VoiceConversationEntry
import ai.openclaw.app.voice.VoiceConversationRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudCaptionEntriesTest {
    @Test
    fun finalizedSourceStaysVisibleUntilItsTranslationArrives() {
        val source =
            VoiceConversationEntry(
                id = "source",
                role = VoiceConversationRole.User,
                text = "Where is the train station?",
            )

        val pending = hudCaptionEntries(listOf(source), liveTranscript = null)
        assertEquals("S1", pending.single().speaker)
        assertEquals("Where is the train station?", pending.single().text)
        assertTrue(pending.single().isLive)
        assertTrue(pending.single().isSource)

        val translated =
            hudCaptionEntries(
                conversation =
                    listOf(
                        source,
                        VoiceConversationEntry(
                            id = "translation",
                            role = VoiceConversationRole.Assistant,
                            text = "S1: ¿Dónde está la estación de tren?",
                        ),
                    ),
                liveTranscript = null,
            )
        assertEquals(1, translated.size)
        assertEquals("S1", translated.single().speaker)
        assertEquals("¿Dónde está la estación de tren?", translated.single().text)
        assertFalse(translated.single().isLive)
        assertFalse(translated.single().isSource)
    }
}
