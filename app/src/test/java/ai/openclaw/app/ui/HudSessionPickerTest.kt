package ai.openclaw.app.ui

import ai.openclaw.app.chat.ChatSessionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudSessionPickerTest {
    @Test
    fun choicesPreferMainAndRecentSessionsAndStayCompact() {
        val now = 1_700_000_000_000L
        val sessions =
            buildList {
                add(ChatSessionEntry(key = "main", updatedAtMs = now - 2 * 24 * 60 * 60 * 1000L))
                repeat(10) { index ->
                    add(ChatSessionEntry(key = "recent-$index", updatedAtMs = now - index * 1000L))
                }
            }

        val result =
            hudSessionChoices(
                currentSessionKey = "recent-9",
                sessions = sessions,
                mainSessionKey = "main",
                nowMs = now,
            )

        assertEquals(8, result.size)
        assertEquals("main", result.first().key)
        assertEquals("recent-0", result[1].key)
        assertTrue(result.any { it.key == "recent-9" })
    }

    @Test
    fun pickerRequiresConnectionMultipleSessionsAndIdleApprovalState() {
        assertTrue(
            hudSessionPickerEnabled(
                isConnected = true,
                sessionCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = false,
            ),
        )
        assertFalse(
            hudSessionPickerEnabled(
                isConnected = true,
                sessionCount = 2,
                pendingRunCount = 1,
                hasPendingExecApproval = false,
            ),
        )
        assertFalse(
            hudSessionPickerEnabled(
                isConnected = true,
                sessionCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = true,
            ),
        )
        assertFalse(
            hudSessionPickerEnabled(
                isConnected = false,
                sessionCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = false,
            ),
        )
    }

    @Test
    fun labelUsesDisplayNameAndFallsBackToFriendlyKey() {
        assertEquals(
            "Operations",
            hudSessionLabel(ChatSessionEntry(key = "agent:ops:main", updatedAtMs = null, displayName = " Operations ")),
        )
        assertEquals(
            "Daily Review",
            hudSessionLabel(ChatSessionEntry(key = "daily-review", updatedAtMs = null)),
        )
    }
}
