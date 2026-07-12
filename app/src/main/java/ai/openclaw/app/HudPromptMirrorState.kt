package ai.openclaw.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HudPromptMirrorState {
    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    fun setDraft(value: String) {
        _draft.value = value
    }

    fun clearDraft() {
        _draft.value = ""
    }
}
