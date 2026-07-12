package ai.openclaw.app

import kotlinx.serialization.Serializable

@Serializable
data class AirVisionSupportMetadata(
    val eulaStatus: String,
    val legalNote: String,
    val faqUrl: String,
    val productRegistrationUrl: String,
    val supportUrl: String,
    val privacyBoundary: String,
    val summary: String,
) {
    companion object {
        const val FAQ_URL = "https://www.asus.com/support/faq/1054069/"
        const val PRODUCT_REGISTRATION_URL = "https://account.asus.com/product_reg.aspx"
        const val SUPPORT_URL =
            "https://www.asus.com/displays-desktops/glasses/airvision/asus-airvision-m1/helpdesk_knowledge?model2Name=ASUS-AirVision-M1"
        const val EULA_STATUS = "ASUS AirVision EULA is shown in the ASUS Windows app; Android shows an OpenClaw companion legal note."
        const val LEGAL_NOTE =
            "ASUS displays the AirVision EULA inside the Windows AirVision app. This Android HUD is an OpenClaw companion and does not replace ASUS firmware, warranty, registration, or license terms."
        const val PRIVACY_BOUNDARY =
            "Support metadata contains public ASUS links and OpenClaw policy text only; it excludes gateway endpoints, auth tokens, chat history, raw USB serials, and raw HID captures."

        val default: AirVisionSupportMetadata =
            AirVisionSupportMetadata(
                eulaStatus = EULA_STATUS,
                legalNote = LEGAL_NOTE,
                faqUrl = FAQ_URL,
                productRegistrationUrl = PRODUCT_REGISTRATION_URL,
                supportUrl = SUPPORT_URL,
                privacyBoundary = PRIVACY_BOUNDARY,
                summary = "AirVision support metadata: EULA note, privacy boundary, FAQ/tutorial, product registration, and ASUS support links.",
            )
    }
}
