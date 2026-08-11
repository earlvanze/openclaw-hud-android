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

    @Test
    fun accumulator_doesNotSuppressTheSamePhraseAfterARecognizerSessionResets() {
        val accumulator = CaptionTranscriptAccumulator()

        assertEquals("hello", accumulator.flushPartial("hello"))
        accumulator.reset()

        assertEquals("hello", accumulator.finish("hello"))
    }

    @Test
    fun accumulator_sendsOnlyTheNewWordsWhenTheSameSessionGetsItsFinalResult() {
        val accumulator = CaptionTranscriptAccumulator()

        assertEquals("hello", accumulator.flushPartial("hello"))

        assertEquals("world", accumulator.finish("hello world"))
    }
}
