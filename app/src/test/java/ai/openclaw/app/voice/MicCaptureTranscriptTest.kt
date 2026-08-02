package ai.openclaw.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MicCaptureTranscriptTest {
    @Test
    fun unsentFinalTranscript_skipsAnExactFlushedPartial() {
        assertNull(unsentFinalTranscript("hello there", "hello there"))
    }

    @Test
    fun unsentFinalTranscript_keepsOnlyWordsAddedAfterFlushedPartial() {
        assertEquals("world", unsentFinalTranscript("hello world", "hello"))
    }

    @Test
    fun unsentFinalTranscript_keepsARevisedFinalIntact() {
        assertEquals("I can't", unsentFinalTranscript("I can't", "I can"))
    }
}
