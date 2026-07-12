package ai.openclaw.app

import ai.openclaw.app.gateway.DeviceAuthStore
import ai.openclaw.app.ui.decodeGatewaySetupCode
import ai.openclaw.app.ui.parseGatewayEndpointResult
import android.content.Intent

const val ACTION_SETUP_OPENCLAW_GATEWAY = "ai.openclaw.app.action.SETUP_GATEWAY"
const val EXTRA_GATEWAY_SETUP_CODE = "setup_code"
const val EXTRA_GATEWAY_AUTO_CONNECT = "auto_connect"

data class GatewaySetupLaunchRequest(
    val setupCode: String,
    val autoConnect: Boolean,
)

data class GatewaySetupLaunchConfig(
    val host: String,
    val port: Int,
    val tls: Boolean,
    val bootstrapToken: String,
    val token: String,
    val password: String,
)

fun parseGatewaySetupLaunchIntent(intent: Intent?): GatewaySetupLaunchRequest? {
    val action = intent?.action ?: return null
    if (action != ACTION_SETUP_OPENCLAW_GATEWAY) return null

    val setupCode =
        intent
            .getStringExtra(EXTRA_GATEWAY_SETUP_CODE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: intent
                .getStringExtra(Intent.EXTRA_TEXT)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: return null

    return GatewaySetupLaunchRequest(
        setupCode = setupCode,
        autoConnect = intent.getBooleanExtra(EXTRA_GATEWAY_AUTO_CONNECT, true),
    )
}

fun resolveGatewaySetupLaunchConfig(request: GatewaySetupLaunchRequest): GatewaySetupLaunchConfig? {
    val setup = decodeGatewaySetupCode(request.setupCode) ?: return null
    val parsed = parseGatewayEndpointResult(setup.url).config ?: return null
    return GatewaySetupLaunchConfig(
        host = parsed.host,
        port = parsed.port,
        tls = parsed.tls,
        bootstrapToken = setup.bootstrapToken.orEmpty().trim(),
        token = setup.token.orEmpty().trim(),
        password = setup.password.orEmpty().trim(),
    )
}

fun applyGatewaySetupLaunchConfig(
    prefs: SecurePrefs,
    deviceId: String,
    request: GatewaySetupLaunchRequest,
): GatewaySetupLaunchConfig? {
    val config = resolveGatewaySetupLaunchConfig(request) ?: return null
    clearGatewaySetupAuthForDevice(prefs, deviceId)
    prefs.setManualEnabled(true)
    prefs.setManualHost(config.host)
    prefs.setManualPort(config.port)
    prefs.setManualTls(config.tls)
    prefs.setGatewayBootstrapToken(config.bootstrapToken)
    prefs.setGatewayToken(config.token)
    prefs.setGatewayPassword(config.password)
    prefs.setAirVisionDemoModeEnabled(false)
    prefs.setOnboardingCompleted(true)
    return config
}

fun clearGatewaySetupAuthForDevice(
    prefs: SecurePrefs,
    deviceId: String,
) {
    prefs.clearGatewaySetupAuth()
    val deviceAuthStore = DeviceAuthStore(prefs)
    deviceAuthStore.clearToken(deviceId, "node")
    deviceAuthStore.clearToken(deviceId, "operator")
}
