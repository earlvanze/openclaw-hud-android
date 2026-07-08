package ai.openclaw.app

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GatewaySetupLaunchTest {
    @Test
    fun parsesSetupCodeExtra() {
        val parsed =
            parseGatewaySetupLaunchIntent(
                Intent(ACTION_SETUP_OPENCLAW_GATEWAY)
                    .putExtra(EXTRA_GATEWAY_SETUP_CODE, "  setup-code  "),
            )

        requireNotNull(parsed)
        assertEquals("setup-code", parsed.setupCode)
        assertTrue(parsed.autoConnect)
    }

    @Test
    fun parsesTextExtraAndAutoConnectFlag() {
        val parsed =
            parseGatewaySetupLaunchIntent(
                Intent(ACTION_SETUP_OPENCLAW_GATEWAY)
                    .putExtra(Intent.EXTRA_TEXT, "  text-setup-code  ")
                    .putExtra(EXTRA_GATEWAY_AUTO_CONNECT, false),
            )

        requireNotNull(parsed)
        assertEquals("text-setup-code", parsed.setupCode)
        assertFalse(parsed.autoConnect)
    }

    @Test
    fun ignoresMissingSetupCode() {
        assertNull(parseGatewaySetupLaunchIntent(Intent(ACTION_SETUP_OPENCLAW_GATEWAY)))
    }

    @Test
    fun ignoresUnrelatedIntent() {
        assertNull(
            parseGatewaySetupLaunchIntent(
                Intent(Intent.ACTION_VIEW).putExtra(EXTRA_GATEWAY_SETUP_CODE, "setup-code"),
            ),
        )
    }

    @Test
    fun resolvesSetupCodeToGatewayConfig() {
        val setupCode =
            encodeSetupCode(
                """{"url":"wss://fold-gateway.example:18789","bootstrapToken":" boot ","token":" tok ","password":" pass "}""",
            )

        val resolved =
            resolveGatewaySetupLaunchConfig(
                GatewaySetupLaunchRequest(setupCode = setupCode, autoConnect = true),
            )

        requireNotNull(resolved)
        assertEquals("fold-gateway.example", resolved.host)
        assertEquals(18789, resolved.port)
        assertTrue(resolved.tls)
        assertEquals("boot", resolved.bootstrapToken)
        assertEquals("tok", resolved.token)
        assertEquals("pass", resolved.password)
    }

    @Test
    fun rejectsInvalidSetupCodeConfig() {
        val setupCode = encodeSetupCode("""{"url":"ws://gateway.example:18789","bootstrapToken":"boot"}""")

        val resolved =
            resolveGatewaySetupLaunchConfig(
                GatewaySetupLaunchRequest(setupCode = setupCode, autoConnect = true),
            )

        assertNull(resolved)
    }

    private fun encodeSetupCode(json: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
}
