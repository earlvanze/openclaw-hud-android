package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Test

class HudPromptMirrorStateTest {
    @Test
    fun setDraftExposesTextForPhoneToPresentationMirroring() {
        val state = HudPromptMirrorState()

        state.setDraft("ask the gateway for status")

        assertEquals("ask the gateway for status", state.draft.value)
    }

    @Test
    fun clearDraftRemovesMirroredTextAfterSend() {
        val state = HudPromptMirrorState()
        state.setDraft("what is running")

        state.clearDraft()

        assertEquals("", state.draft.value)
    }
}
