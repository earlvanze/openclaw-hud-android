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
        assertTrue(policy.contains("report an offensive assistant response"))
        assertTrue(policy.contains("eco systems llc"))
        assertTrue(policy.contains("retained for up to 90 days"))
        assertTrue(policy.contains("do not include images"))
    }
}
