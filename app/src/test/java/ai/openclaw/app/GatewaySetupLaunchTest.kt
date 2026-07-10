package ai.openclaw.app

import ai.openclaw.app.gateway.DeviceAuthStore
import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64
import java.util.UUID

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

    @Test
    fun handleSetupLaunchReplacesStaleLocalEndpointWithoutAutoConnect() {
        val context = RuntimeEnvironment.getApplication()
        val securePrefs =
            context.getSharedPreferences(
                "openclaw.node.secure.test.${UUID.randomUUID()}",
                Context.MODE_PRIVATE,
            )
        val prefs = SecurePrefs(context, securePrefsOverride = securePrefs)
        val deviceId = "fold-7"
        val deviceAuthStore = DeviceAuthStore(prefs)
        prefs.setManualEnabled(true)
        prefs.setManualHost("127.0.0.1")
        prefs.setManualPort(18789)
        prefs.setManualTls(false)
        prefs.setGatewayBootstrapToken("stale-bootstrap")
        prefs.setGatewayToken("stale-token")
        prefs.setGatewayPassword("stale-password")
        deviceAuthStore.saveToken(deviceId, "node", "stale-node-token")
        deviceAuthStore.saveToken(deviceId, "operator", "stale-operator-token")
        val setupCode =
            encodeSetupCode(
                """{"url":"ws://100.88.253.107:45219","bootstrapToken":" cyber-bootstrap "}""",
            )

        val config =
            applyGatewaySetupLaunchConfig(
                prefs = prefs,
                deviceId = deviceId,
                request = GatewaySetupLaunchRequest(setupCode = setupCode, autoConnect = false),
            )

        requireNotNull(config)
        assertEquals("100.88.253.107", config.host)
        assertEquals(45219, config.port)
        assertFalse(config.tls)
        assertTrue(prefs.manualEnabled.value)
        assertEquals("100.88.253.107", prefs.manualHost.value)
        assertEquals(45219, prefs.manualPort.value)
        assertFalse(prefs.manualTls.value)
        assertEquals("cyber-bootstrap", prefs.gatewayBootstrapToken.value)
        assertEquals("", prefs.gatewayToken.value)
        assertNull(prefs.loadGatewayPassword())
        assertTrue(prefs.onboardingCompleted.value)
        assertNull(deviceAuthStore.loadToken(deviceId, "node"))
        assertNull(deviceAuthStore.loadToken(deviceId, "operator"))
    }

    private fun encodeSetupCode(json: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
}
