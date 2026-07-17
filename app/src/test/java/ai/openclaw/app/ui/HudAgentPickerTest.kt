package ai.openclaw.app.ui

import ai.openclaw.app.GatewayAgentSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudAgentPickerTest {
    @Test
    fun activeAgentUsesCanonicalSessionKeyThenGatewayDefault() {
        assertEquals("ops", hudActiveAgentId("agent:ops:main", "default"))
        assertEquals("default", hudActiveAgentId("global", " default "))
        assertEquals(null, hudActiveAgentId("global", " "))
    }

    @Test
    fun pickerRequiresConnectionMultipleAgentsAndIdleApprovalState() {
        assertTrue(
            hudAgentPickerEnabled(
                isConnected = true,
                agentCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = false,
            ),
        )
        assertFalse(
            hudAgentPickerEnabled(
                isConnected = true,
                agentCount = 2,
                pendingRunCount = 1,
                hasPendingExecApproval = false,
            ),
        )
        assertFalse(
            hudAgentPickerEnabled(
                isConnected = true,
                agentCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = true,
            ),
        )
        assertFalse(
            hudAgentPickerEnabled(
                isConnected = false,
                agentCount = 2,
                pendingRunCount = 0,
                hasPendingExecApproval = false,
            ),
        )
    }

    @Test
    fun labelUsesOptionalEmojiAndFallsBackToId() {
        assertEquals(
            "AI Assistant",
            hudAgentLabel(GatewayAgentSummary(id = "main", name = "Assistant", emoji = "AI")),
        )
        assertEquals(
            "ops",
            hudAgentLabel(GatewayAgentSummary(id = "ops", name = " ", emoji = null)),
        )
    }
}
