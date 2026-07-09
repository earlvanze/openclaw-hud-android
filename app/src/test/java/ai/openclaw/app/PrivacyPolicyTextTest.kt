package ai.openclaw.app

import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTextTest {
    @Test
    fun bodyDisclosesPlayReviewDataHandlingTopics() {
        val policy = PrivacyPolicyText.BODY.lowercase()

        assertTrue(policy.contains("microphone"))
        assertTrue(policy.contains("notification"))
        assertTrue(policy.contains("gateway"))
        assertTrue(policy.contains("encrypted storage"))
        assertTrue(policy.contains("does not sell personal data"))
        assertTrue(policy.contains("clear data"))
        assertTrue(policy.contains("does not include advertising"))
    }
}
