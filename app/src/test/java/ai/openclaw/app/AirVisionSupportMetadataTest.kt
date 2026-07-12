package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionSupportMetadataTest {
    @Test
    fun default_containsReviewablePublicSupportAndPrivacyBoundary() {
        val metadata = AirVisionSupportMetadata.default

        assertEquals(AirVisionSupportMetadata.FAQ_URL, metadata.faqUrl)
        assertEquals(AirVisionSupportMetadata.PRODUCT_REGISTRATION_URL, metadata.productRegistrationUrl)
        assertEquals(AirVisionSupportMetadata.SUPPORT_URL, metadata.supportUrl)
        assertTrue(metadata.eulaStatus.contains("ASUS AirVision EULA"))
        assertTrue(metadata.legalNote.contains("OpenClaw companion"))
        assertTrue(metadata.privacyBoundary.contains("excludes gateway endpoints"))
        assertTrue(metadata.privacyBoundary.contains("raw USB serials"))
    }
}
