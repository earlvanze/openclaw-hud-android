@file:Suppress("DEPRECATION")

package ai.openclaw.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

class SecurePrefs(
    context: Context,
    private val securePrefsOverride: SharedPreferences? = null,
) {
    companion object {
        val defaultWakeWords: List<String> = listOf("openclaw", "claude")
        private const val displayNameKey = "node.displayName"
        private const val locationModeKey = "location.enabledMode"
        private const val voiceWakeModeKey = "voiceWake.mode"
        private const val plainPrefsName = "openclaw.node"
        private const val securePrefsName = "openclaw.node.secure"
        private const val notificationsForwardingEnabledKey = "notifications.forwarding.enabled"
        private const val defaultNotificationForwardingEnabled = false
        private const val notificationsForwardingModeKey = "notifications.forwarding.mode"
        private const val notificationsForwardingPackagesKey = "notifications.forwarding.packages"
        private const val notificationsForwardingQuietHoursEnabledKey =
            "notifications.forwarding.quietHoursEnabled"
        private const val notificationsForwardingQuietStartKey = "notifications.forwarding.quietStart"
        private const val notificationsForwardingQuietEndKey = "notifications.forwarding.quietEnd"
        private const val notificationsForwardingMaxEventsPerMinuteKey =
            "notifications.forwarding.maxEventsPerMinute"
        private const val notificationsForwardingSessionKeyKey = "notifications.forwarding.sessionKey"
        private const val AIR_VISION_VIEW_MODE_KEY = "airVision.viewMode"
        private const val AIR_VISION_SPLENDID_MODE_KEY = "airVision.splendidMode"
        private const val AIR_VISION_HUD_PLACEMENT_KEY = "airVision.hudPlacement"
        private const val AIR_VISION_BRIGHTNESS_PERCENT_KEY = "airVision.brightnessPercent"
        private const val AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY = "airVision.blueLightFilterPercent"
        private const val AIR_VISION_DISTANCE_CM_KEY = "airVision.distanceCm"
        private const val AIR_VISION_IPD_MM_KEY = "airVision.ipdMm"
        private const val AIR_VISION_SAFE_AREA_PERCENT_KEY = "airVision.safeAreaPercent"
        private const val AIR_VISION_MOTION_SYNC_ENABLED_KEY = "airVision.motionSyncEnabled"
        private const val AIR_VISION_THREE_D_MODE_ENABLED_KEY = "airVision.threeDModeEnabled"
        private const val AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY = "airVision.lightLoadModeEnabled"
        private const val AIR_VISION_HUD_SINGLE_TAP_ACTION_KEY = "airVision.hud.singleTapAction"
        private const val AIR_VISION_HUD_DOUBLE_TAP_ACTION_KEY = "airVision.hud.doubleTapAction"
        private const val AIR_VISION_HUD_SWIPE_ACTION_KEY = "airVision.hud.swipeAction"
        private const val AIR_VISION_HUD_BRIGHTNESS_KEY_ACTION_KEY = "airVision.hud.brightnessKeyAction"
        private const val AIR_VISION_HUD_MEDIA_KEY_ACTION_KEY = "airVision.hud.mediaKeyAction"
        private const val AIR_VISION_APP_LANGUAGE_KEY = "airVision.app.language"
        private const val AIR_VISION_STARTUP_DESTINATION_KEY = "airVision.app.startupDestination"
        private const val AIR_VISION_HUD_DISPLAY_TARGET_KEY = "airVision.app.hudDisplayTarget"
        private const val AIR_VISION_CUSTOM_1_LABEL_KEY = "airVision.profile.custom1.label"
        private const val AIR_VISION_CUSTOM_2_LABEL_KEY = "airVision.profile.custom2.label"
        private const val AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY = "airVision.physicalMainScreenVisible"
        private const val AIR_VISION_DEMO_MODE_ENABLED_KEY = "airVision.demoModeEnabled"
    }

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val plainPrefs: SharedPreferences =
        appContext.getSharedPreferences(plainPrefsName, Context.MODE_PRIVATE)

    private val masterKey by lazy {
        MasterKey
            .Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    private val securePrefs: SharedPreferences by lazy { securePrefsOverride ?: createSecurePrefs(appContext, securePrefsName) }

    private val _instanceId = MutableStateFlow(loadOrCreateInstanceId())
    val instanceId: StateFlow<String> = _instanceId

    private val _displayName =
        MutableStateFlow(loadOrMigrateDisplayName(context = context))
    val displayName: StateFlow<String> = _displayName

    private val _cameraEnabled = MutableStateFlow(plainPrefs.getBoolean("camera.enabled", true))
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled

    private val _locationMode = MutableStateFlow(loadLocationMode())
    val locationMode: StateFlow<LocationMode> = _locationMode

    private val _locationPreciseEnabled =
        MutableStateFlow(plainPrefs.getBoolean("location.preciseEnabled", true))
    val locationPreciseEnabled: StateFlow<Boolean> = _locationPreciseEnabled

    private val _preventSleep = MutableStateFlow(plainPrefs.getBoolean("screen.preventSleep", true))
    val preventSleep: StateFlow<Boolean> = _preventSleep

    private val _manualEnabled =
        MutableStateFlow(plainPrefs.getBoolean("gateway.manual.enabled", false))
    val manualEnabled: StateFlow<Boolean> = _manualEnabled

    private val _manualHost =
        MutableStateFlow(plainPrefs.getString("gateway.manual.host", "") ?: "")
    val manualHost: StateFlow<String> = _manualHost

    private val _manualPort =
        MutableStateFlow(plainPrefs.getInt("gateway.manual.port", 18789))
    val manualPort: StateFlow<Int> = _manualPort

    private val _manualTls =
        MutableStateFlow(plainPrefs.getBoolean("gateway.manual.tls", true))
    val manualTls: StateFlow<Boolean> = _manualTls

    private val _gatewayToken = MutableStateFlow("")
    val gatewayToken: StateFlow<String> = _gatewayToken

    private val _gatewayBootstrapToken = MutableStateFlow("")
    val gatewayBootstrapToken: StateFlow<String> = _gatewayBootstrapToken

    private val _onboardingCompleted =
        MutableStateFlow(plainPrefs.getBoolean("onboarding.completed", false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted

    private val _lastDiscoveredStableId =
        MutableStateFlow(
            plainPrefs.getString("gateway.lastDiscoveredStableID", "") ?: "",
        )
    val lastDiscoveredStableId: StateFlow<String> = _lastDiscoveredStableId

    private val _canvasDebugStatusEnabled =
        MutableStateFlow(plainPrefs.getBoolean("canvas.debugStatusEnabled", false))
    val canvasDebugStatusEnabled: StateFlow<Boolean> = _canvasDebugStatusEnabled

    private val _notificationForwardingEnabled =
        MutableStateFlow(plainPrefs.getBoolean(notificationsForwardingEnabledKey, defaultNotificationForwardingEnabled))
    val notificationForwardingEnabled: StateFlow<Boolean> = _notificationForwardingEnabled

    private val _notificationForwardingMode =
        MutableStateFlow(
            NotificationPackageFilterMode.fromRawValue(
                plainPrefs.getString(notificationsForwardingModeKey, null),
            ),
        )
    val notificationForwardingMode: StateFlow<NotificationPackageFilterMode> = _notificationForwardingMode

    private val _notificationForwardingPackages = MutableStateFlow(loadNotificationForwardingPackages())
    val notificationForwardingPackages: StateFlow<Set<String>> = _notificationForwardingPackages

    private val storedQuietStart =
        normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietStartKey, "22:00").orEmpty())
            ?: "22:00"
    private val storedQuietEnd =
        normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietEndKey, "07:00").orEmpty())
            ?: "07:00"
    private val storedQuietHoursEnabled =
        plainPrefs.getBoolean(notificationsForwardingQuietHoursEnabledKey, false) &&
            normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietStartKey, "22:00").orEmpty()) != null &&
            normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietEndKey, "07:00").orEmpty()) != null

    private val _notificationForwardingQuietHoursEnabled =
        MutableStateFlow(storedQuietHoursEnabled)
    val notificationForwardingQuietHoursEnabled: StateFlow<Boolean> = _notificationForwardingQuietHoursEnabled

    private val _notificationForwardingQuietStart = MutableStateFlow(storedQuietStart)
    val notificationForwardingQuietStart: StateFlow<String> = _notificationForwardingQuietStart

    private val _notificationForwardingQuietEnd = MutableStateFlow(storedQuietEnd)
    val notificationForwardingQuietEnd: StateFlow<String> = _notificationForwardingQuietEnd

    private val _notificationForwardingMaxEventsPerMinute =
        MutableStateFlow(plainPrefs.getInt(notificationsForwardingMaxEventsPerMinuteKey, 20).coerceAtLeast(1))
    val notificationForwardingMaxEventsPerMinute: StateFlow<Int> = _notificationForwardingMaxEventsPerMinute

    private val _notificationForwardingSessionKey =
        MutableStateFlow(
            plainPrefs
                .getString(notificationsForwardingSessionKeyKey, "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
        )
    val notificationForwardingSessionKey: StateFlow<String?> = _notificationForwardingSessionKey

    private val _wakeWords = MutableStateFlow(loadWakeWords())
    val wakeWords: StateFlow<List<String>> = _wakeWords

    private val _voiceWakeMode = MutableStateFlow(loadVoiceWakeMode())
    val voiceWakeMode: StateFlow<VoiceWakeMode> = _voiceWakeMode

    private val _talkEnabled = MutableStateFlow(plainPrefs.getBoolean("talk.enabled", false))
    val talkEnabled: StateFlow<Boolean> = _talkEnabled

    private val _speakerEnabled = MutableStateFlow(plainPrefs.getBoolean("voice.speakerEnabled", true))
    val speakerEnabled: StateFlow<Boolean> = _speakerEnabled

    private val _nativeCaptionsEnabled = MutableStateFlow(plainPrefs.getBoolean("nativeCaptions.enabled", false))
    val nativeCaptionsEnabled: StateFlow<Boolean> = _nativeCaptionsEnabled

    private val _airVisionDisplaySettings = MutableStateFlow(loadAirVisionDisplaySettings())
    val airVisionDisplaySettings: StateFlow<AirVisionDisplaySettings> = _airVisionDisplaySettings

    private val _airVisionHudControls = MutableStateFlow(loadAirVisionHudControls())
    val airVisionHudControls: StateFlow<AirVisionHudControls> = _airVisionHudControls

    private val _airVisionAppLanguage =
        MutableStateFlow(
            AirVisionAppLanguage.fromRawValue(plainPrefs.getString(AIR_VISION_APP_LANGUAGE_KEY, null)),
        )
    val airVisionAppLanguage: StateFlow<AirVisionAppLanguage> = _airVisionAppLanguage

    private val _airVisionStartupDestination =
        MutableStateFlow(
            AirVisionStartupDestination.fromRawValue(
                plainPrefs.getString(AIR_VISION_STARTUP_DESTINATION_KEY, null),
            ),
        )
    val airVisionStartupDestination: StateFlow<AirVisionStartupDestination> = _airVisionStartupDestination

    private val _airVisionHudDisplayTarget =
        MutableStateFlow(
            AirVisionHudDisplayTarget.fromRawValue(
                plainPrefs.getString(AIR_VISION_HUD_DISPLAY_TARGET_KEY, null),
            ),
        )
    val airVisionHudDisplayTarget: StateFlow<AirVisionHudDisplayTarget> = _airVisionHudDisplayTarget

    private val _airVisionCustomProfileLabels =
        MutableStateFlow(loadAirVisionCustomProfileLabels())
    val airVisionCustomProfileLabels: StateFlow<AirVisionCustomProfileLabels> = _airVisionCustomProfileLabels

    private val _airVisionPhysicalMainScreenVisible =
        MutableStateFlow(_airVisionDisplaySettings.value.physicalMainScreenVisible)
    val airVisionPhysicalMainScreenVisible: StateFlow<Boolean> = _airVisionPhysicalMainScreenVisible

    private val _airVisionDemoModeEnabled =
        MutableStateFlow(plainPrefs.getBoolean(AIR_VISION_DEMO_MODE_ENABLED_KEY, false))
    val airVisionDemoModeEnabled: StateFlow<Boolean> = _airVisionDemoModeEnabled

    private val _translationCaptionSourceLanguage =
        MutableStateFlow(
            TranslationCaptionMode.normalizeLanguageCode(
                plainPrefs.getString("translationCaptions.sourceLanguage", TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE),
                TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
            ),
        )
    val translationCaptionSourceLanguage: StateFlow<String> = _translationCaptionSourceLanguage

    private val _translationCaptionTargetLanguage =
        MutableStateFlow(
            TranslationCaptionMode.normalizeLanguageCode(
                plainPrefs.getString("translationCaptions.targetLanguage", TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE),
                TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
            ),
        )
    val translationCaptionTargetLanguage: StateFlow<String> = _translationCaptionTargetLanguage

    fun setLastDiscoveredStableId(value: String) {
        val trimmed = value.trim()
        plainPrefs.edit { putString("gateway.lastDiscoveredStableID", trimmed) }
        _lastDiscoveredStableId.value = trimmed
    }

    fun setDisplayName(value: String) {
        val trimmed = value.trim()
        plainPrefs.edit { putString(displayNameKey, trimmed) }
        _displayName.value = trimmed
    }

    fun setCameraEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("camera.enabled", value) }
        _cameraEnabled.value = value
    }

    fun setLocationMode(mode: LocationMode) {
        plainPrefs.edit { putString(locationModeKey, mode.rawValue) }
        _locationMode.value = mode
    }

    fun setLocationPreciseEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("location.preciseEnabled", value) }
        _locationPreciseEnabled.value = value
    }

    fun setPreventSleep(value: Boolean) {
        plainPrefs.edit { putBoolean("screen.preventSleep", value) }
        _preventSleep.value = value
    }

    fun setManualEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("gateway.manual.enabled", value) }
        _manualEnabled.value = value
    }

    fun setManualHost(value: String) {
        val trimmed = value.trim()
        plainPrefs.edit { putString("gateway.manual.host", trimmed) }
        _manualHost.value = trimmed
    }

    fun setManualPort(value: Int) {
        plainPrefs.edit { putInt("gateway.manual.port", value) }
        _manualPort.value = value
    }

    fun setManualTls(value: Boolean) {
        plainPrefs.edit { putBoolean("gateway.manual.tls", value) }
        _manualTls.value = value
    }

    fun setGatewayToken(value: String) {
        val trimmed = value.trim()
        securePrefs.edit { putString("gateway.manual.token", trimmed) }
        _gatewayToken.value = trimmed
    }

    fun setGatewayPassword(value: String) {
        saveGatewayPassword(value)
    }

    fun setGatewayBootstrapToken(value: String) {
        saveGatewayBootstrapToken(value)
    }

    fun setOnboardingCompleted(value: Boolean) {
        plainPrefs.edit { putBoolean("onboarding.completed", value) }
        _onboardingCompleted.value = value
    }

    fun setCanvasDebugStatusEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("canvas.debugStatusEnabled", value) }
        _canvasDebugStatusEnabled.value = value
    }

    internal fun getNotificationForwardingPolicy(appPackageName: String): NotificationForwardingPolicy {
        val modeRaw = plainPrefs.getString(notificationsForwardingModeKey, null)
        val mode = NotificationPackageFilterMode.fromRawValue(modeRaw)

        val configuredPackages = loadNotificationForwardingPackages()
        val normalizedAppPackage = appPackageName.trim()
        val defaultBlockedPackages =
            if (normalizedAppPackage.isNotEmpty()) setOf(normalizedAppPackage) else emptySet()

        val packages =
            when (mode) {
                NotificationPackageFilterMode.Allowlist -> configuredPackages
                NotificationPackageFilterMode.Blocklist -> configuredPackages + defaultBlockedPackages
            }

        val maxEvents = plainPrefs.getInt(notificationsForwardingMaxEventsPerMinuteKey, 20)
        val quietStart =
            normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietStartKey, "22:00").orEmpty())
                ?: "22:00"
        val quietEnd =
            normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietEndKey, "07:00").orEmpty())
                ?: "07:00"
        val sessionKey =
            plainPrefs
                .getString(notificationsForwardingSessionKeyKey, "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val quietHoursEnabled =
            plainPrefs.getBoolean(notificationsForwardingQuietHoursEnabledKey, false) &&
                normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietStartKey, "22:00").orEmpty()) != null &&
                normalizeLocalHourMinute(plainPrefs.getString(notificationsForwardingQuietEndKey, "07:00").orEmpty()) != null

        return NotificationForwardingPolicy(
            enabled = plainPrefs.getBoolean(notificationsForwardingEnabledKey, defaultNotificationForwardingEnabled),
            mode = mode,
            packages = packages,
            quietHoursEnabled = quietHoursEnabled,
            quietStart = quietStart,
            quietEnd = quietEnd,
            maxEventsPerMinute = maxEvents.coerceAtLeast(1),
            sessionKey = sessionKey,
        )
    }

    internal fun setNotificationForwardingEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean(notificationsForwardingEnabledKey, value) }
        _notificationForwardingEnabled.value = value
    }

    internal fun setNotificationForwardingMode(mode: NotificationPackageFilterMode) {
        plainPrefs.edit { putString(notificationsForwardingModeKey, mode.rawValue) }
        _notificationForwardingMode.value = mode
    }

    internal fun setNotificationForwardingPackages(packages: List<String>) {
        val sanitized =
            packages
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
                .toList()
                .sorted()
        val encoded = JsonArray(sanitized.map { JsonPrimitive(it) }).toString()
        plainPrefs.edit { putString(notificationsForwardingPackagesKey, encoded) }
        _notificationForwardingPackages.value = sanitized.toSet()
    }

    internal fun setNotificationForwardingQuietHours(
        enabled: Boolean,
        start: String,
        end: String,
    ): Boolean {
        if (!enabled) {
            plainPrefs.edit { putBoolean(notificationsForwardingQuietHoursEnabledKey, false) }
            _notificationForwardingQuietHoursEnabled.value = false
            return true
        }
        val normalizedStart = normalizeLocalHourMinute(start) ?: return false
        val normalizedEnd = normalizeLocalHourMinute(end) ?: return false
        plainPrefs.edit {
            putBoolean(notificationsForwardingQuietHoursEnabledKey, enabled)
            putString(notificationsForwardingQuietStartKey, normalizedStart)
            putString(notificationsForwardingQuietEndKey, normalizedEnd)
        }
        _notificationForwardingQuietHoursEnabled.value = enabled
        _notificationForwardingQuietStart.value = normalizedStart
        _notificationForwardingQuietEnd.value = normalizedEnd
        return true
    }

    internal fun setNotificationForwardingMaxEventsPerMinute(value: Int) {
        val normalized = value.coerceAtLeast(1)
        plainPrefs.edit {
            putInt(notificationsForwardingMaxEventsPerMinuteKey, normalized)
        }
        _notificationForwardingMaxEventsPerMinute.value = normalized
    }

    internal fun setNotificationForwardingSessionKey(value: String?) {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        plainPrefs.edit {
            putString(notificationsForwardingSessionKeyKey, normalized.orEmpty())
        }
        _notificationForwardingSessionKey.value = normalized
    }

    fun loadGatewayToken(): String? {
        val manual =
            _gatewayToken.value.trim().ifEmpty {
                val stored = securePrefs.getString("gateway.manual.token", null)?.trim().orEmpty()
                if (stored.isNotEmpty()) _gatewayToken.value = stored
                stored
            }
        if (manual.isNotEmpty()) return manual
        val key = "gateway.token.${_instanceId.value}"
        val stored = securePrefs.getString(key, null)?.trim()
        return stored?.takeIf { it.isNotEmpty() }
    }

    fun saveGatewayToken(token: String) {
        val key = "gateway.token.${_instanceId.value}"
        securePrefs.edit { putString(key, token.trim()) }
    }

    fun loadGatewayBootstrapToken(): String? {
        val key = "gateway.bootstrapToken.${_instanceId.value}"
        val stored =
            _gatewayBootstrapToken.value.trim().ifEmpty {
                val persisted = securePrefs.getString(key, null)?.trim().orEmpty()
                if (persisted.isNotEmpty()) {
                    _gatewayBootstrapToken.value = persisted
                }
                persisted
            }
        return stored.takeIf { it.isNotEmpty() }
    }

    fun saveGatewayBootstrapToken(token: String) {
        val key = "gateway.bootstrapToken.${_instanceId.value}"
        val trimmed = token.trim()
        securePrefs.edit { putString(key, trimmed) }
        _gatewayBootstrapToken.value = trimmed
    }

    fun loadGatewayPassword(): String? {
        val key = "gateway.password.${_instanceId.value}"
        val stored = securePrefs.getString(key, null)?.trim()
        return stored?.takeIf { it.isNotEmpty() }
    }

    fun saveGatewayPassword(password: String) {
        val key = "gateway.password.${_instanceId.value}"
        securePrefs.edit { putString(key, password.trim()) }
    }

    fun clearGatewaySetupAuth() {
        val instanceId = _instanceId.value
        securePrefs.edit {
            remove("gateway.manual.token")
            remove("gateway.token.$instanceId")
            remove("gateway.bootstrapToken.$instanceId")
            remove("gateway.password.$instanceId")
        }
        _gatewayToken.value = ""
        _gatewayBootstrapToken.value = ""
    }

    fun loadGatewayTlsFingerprint(stableId: String): String? {
        val key = "gateway.tls.$stableId"
        return plainPrefs.getString(key, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun saveGatewayTlsFingerprint(
        stableId: String,
        fingerprint: String,
    ) {
        val key = "gateway.tls.$stableId"
        plainPrefs.edit { putString(key, fingerprint.trim()) }
    }

    fun getString(key: String): String? = securePrefs.getString(key, null)

    fun putString(
        key: String,
        value: String,
    ) {
        securePrefs.edit { putString(key, value) }
    }

    fun remove(key: String) {
        securePrefs.edit { remove(key) }
    }

    private fun createSecurePrefs(
        context: Context,
        name: String,
    ): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private fun loadOrCreateInstanceId(): String {
        val existing = plainPrefs.getString("node.instanceId", null)?.trim()
        if (!existing.isNullOrBlank()) return existing
        val fresh = UUID.randomUUID().toString()
        plainPrefs.edit { putString("node.instanceId", fresh) }
        return fresh
    }

    private fun loadOrMigrateDisplayName(context: Context): String {
        val existing = plainPrefs.getString(displayNameKey, null)?.trim().orEmpty()
        if (existing.isNotEmpty() && existing != "Android Node") return existing

        val candidate = DeviceNames.bestDefaultNodeName(context).trim()
        val resolved = candidate.ifEmpty { "Android Node" }

        plainPrefs.edit { putString(displayNameKey, resolved) }
        return resolved
    }

    fun setWakeWords(words: List<String>) {
        val sanitized = WakeWords.sanitize(words, defaultWakeWords)
        val encoded =
            JsonArray(sanitized.map { JsonPrimitive(it) }).toString()
        plainPrefs.edit { putString("voiceWake.triggerWords", encoded) }
        _wakeWords.value = sanitized
    }

    fun setVoiceWakeMode(mode: VoiceWakeMode) {
        plainPrefs.edit { putString(voiceWakeModeKey, mode.rawValue) }
        _voiceWakeMode.value = mode
    }

    fun setTalkEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("talk.enabled", value) }
        _talkEnabled.value = value
    }

    fun setSpeakerEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("voice.speakerEnabled", value) }
        _speakerEnabled.value = value
    }

    fun setNativeCaptionsEnabled(value: Boolean) {
        plainPrefs.edit { putBoolean("nativeCaptions.enabled", value) }
        _nativeCaptionsEnabled.value = value
    }

    fun setAirVisionViewMode(mode: AirVisionViewMode) {
        plainPrefs.edit { putString(AIR_VISION_VIEW_MODE_KEY, mode.rawValue) }
        val settings = loadAirVisionDisplaySettings(mode)
        _airVisionDisplaySettings.value = settings
        _airVisionPhysicalMainScreenVisible.value = settings.physicalMainScreenVisible
    }

    fun resetActiveAirVisionProfile() {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        val defaults = AirVisionDisplaySettings.defaultsForViewMode(viewMode)
        plainPrefs.edit {
            putString(AIR_VISION_VIEW_MODE_KEY, viewMode.rawValue)
            putString(AIR_VISION_SPLENDID_MODE_KEY, defaults.splendidMode.rawValue)
            putString(AIR_VISION_HUD_PLACEMENT_KEY, defaults.hudPlacement.rawValue)
            putInt(AIR_VISION_BRIGHTNESS_PERCENT_KEY, defaults.brightnessPercent)
            putInt(AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY, defaults.blueLightFilterPercent)
            putInt(AIR_VISION_DISTANCE_CM_KEY, defaults.distanceCm)
            putInt(AIR_VISION_IPD_MM_KEY, defaults.ipdMm)
            putInt(AIR_VISION_SAFE_AREA_PERCENT_KEY, defaults.safeAreaPercent)
            putBoolean(AIR_VISION_MOTION_SYNC_ENABLED_KEY, defaults.motionSyncEnabled)
            putBoolean(AIR_VISION_THREE_D_MODE_ENABLED_KEY, defaults.threeDModeEnabled)
            putBoolean(AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY, defaults.lightLoadModeEnabled)
            putString(airVisionProfileKey(AIR_VISION_SPLENDID_MODE_KEY, viewMode), defaults.splendidMode.rawValue)
            putString(airVisionProfileKey(AIR_VISION_HUD_PLACEMENT_KEY, viewMode), defaults.hudPlacement.rawValue)
            putInt(airVisionProfileKey(AIR_VISION_BRIGHTNESS_PERCENT_KEY, viewMode), defaults.brightnessPercent)
            putInt(
                airVisionProfileKey(AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY, viewMode),
                defaults.blueLightFilterPercent,
            )
            putInt(airVisionProfileKey(AIR_VISION_DISTANCE_CM_KEY, viewMode), defaults.distanceCm)
            putInt(airVisionProfileKey(AIR_VISION_IPD_MM_KEY, viewMode), defaults.ipdMm)
            putInt(airVisionProfileKey(AIR_VISION_SAFE_AREA_PERCENT_KEY, viewMode), defaults.safeAreaPercent)
            putBoolean(
                airVisionProfileKey(AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY, viewMode),
                defaults.physicalMainScreenVisible,
            )
            putBoolean(airVisionProfileKey(AIR_VISION_MOTION_SYNC_ENABLED_KEY, viewMode), defaults.motionSyncEnabled)
            putBoolean(airVisionProfileKey(AIR_VISION_THREE_D_MODE_ENABLED_KEY, viewMode), defaults.threeDModeEnabled)
            putBoolean(
                airVisionProfileKey(AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY, viewMode),
                defaults.lightLoadModeEnabled,
            )
        }
        _airVisionDisplaySettings.value = defaults
        _airVisionPhysicalMainScreenVisible.value = defaults.physicalMainScreenVisible
    }

    fun setAirVisionSplendidMode(mode: AirVisionSplendidMode) {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putString(AIR_VISION_SPLENDID_MODE_KEY, mode.rawValue)
            putString(airVisionProfileKey(AIR_VISION_SPLENDID_MODE_KEY, viewMode), mode.rawValue)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(splendidMode = mode)
    }

    fun setAirVisionHudPlacement(value: AirVisionHudPlacement) {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putString(AIR_VISION_HUD_PLACEMENT_KEY, value.rawValue)
            putString(airVisionProfileKey(AIR_VISION_HUD_PLACEMENT_KEY, viewMode), value.rawValue)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(hudPlacement = value)
    }

    fun setAirVisionBrightnessPercent(value: Int) {
        val normalized = AirVisionDisplaySettings.normalizeBrightnessPercent(value)
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putInt(AIR_VISION_BRIGHTNESS_PERCENT_KEY, normalized)
            putInt(airVisionProfileKey(AIR_VISION_BRIGHTNESS_PERCENT_KEY, viewMode), normalized)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(brightnessPercent = normalized)
    }

    fun adjustAirVisionBrightnessPercent(delta: Int) {
        setAirVisionBrightnessPercent(_airVisionDisplaySettings.value.brightnessPercent + delta)
    }

    fun setAirVisionBlueLightFilterPercent(value: Int) {
        if (!_airVisionDisplaySettings.value.blueLightFilterAvailable) return
        val normalized = AirVisionDisplaySettings.normalizeBlueLightFilterPercent(value)
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putInt(AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY, normalized)
            putInt(airVisionProfileKey(AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY, viewMode), normalized)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(blueLightFilterPercent = normalized)
    }

    fun setAirVisionDistanceCm(value: Int) {
        val normalized = AirVisionDisplaySettings.normalizeDistanceCm(value)
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putInt(AIR_VISION_DISTANCE_CM_KEY, normalized)
            putInt(airVisionProfileKey(AIR_VISION_DISTANCE_CM_KEY, viewMode), normalized)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(distanceCm = normalized)
    }

    fun adjustAirVisionDistanceCm(delta: Int) {
        setAirVisionDistanceCm(_airVisionDisplaySettings.value.distanceCm + delta)
    }

    fun setAirVisionIpdMm(value: Int) {
        if (!_airVisionDisplaySettings.value.ipdAdjustmentEnabled) return
        val normalized = AirVisionDisplaySettings.normalizeIpdMm(value)
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putInt(AIR_VISION_IPD_MM_KEY, normalized)
            putInt(airVisionProfileKey(AIR_VISION_IPD_MM_KEY, viewMode), normalized)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(ipdMm = normalized)
    }

    fun setAirVisionSafeAreaPercent(value: Int) {
        val normalized = AirVisionDisplaySettings.normalizeSafeAreaPercent(value)
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putInt(AIR_VISION_SAFE_AREA_PERCENT_KEY, normalized)
            putInt(airVisionProfileKey(AIR_VISION_SAFE_AREA_PERCENT_KEY, viewMode), normalized)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(safeAreaPercent = normalized)
    }

    fun setAirVisionMotionSyncEnabled(value: Boolean) {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putBoolean(AIR_VISION_MOTION_SYNC_ENABLED_KEY, value)
            putBoolean(airVisionProfileKey(AIR_VISION_MOTION_SYNC_ENABLED_KEY, viewMode), value)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(motionSyncEnabled = value)
    }

    fun setAirVisionThreeDModeEnabled(value: Boolean) {
        val current = _airVisionDisplaySettings.value
        if (value && !current.threeDModeAvailable) return
        val viewMode = current.viewMode
        plainPrefs.edit {
            putBoolean(AIR_VISION_THREE_D_MODE_ENABLED_KEY, value)
            putBoolean(airVisionProfileKey(AIR_VISION_THREE_D_MODE_ENABLED_KEY, viewMode), value)
        }
        _airVisionDisplaySettings.value = current.copy(threeDModeEnabled = value).normalized
    }

    fun setAirVisionLightLoadModeEnabled(value: Boolean) {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putBoolean(AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY, value)
            putBoolean(airVisionProfileKey(AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY, viewMode), value)
            if (value) {
                putBoolean(AIR_VISION_THREE_D_MODE_ENABLED_KEY, false)
                putBoolean(airVisionProfileKey(AIR_VISION_THREE_D_MODE_ENABLED_KEY, viewMode), false)
            }
        }
        _airVisionDisplaySettings.value =
            _airVisionDisplaySettings.value
                .copy(
                    lightLoadModeEnabled = value,
                    threeDModeEnabled = if (value) false else _airVisionDisplaySettings.value.threeDModeEnabled,
                ).normalized
    }

    fun setAirVisionHudSingleTapAction(action: AirVisionHudTouchAction) {
        plainPrefs.edit { putString(AIR_VISION_HUD_SINGLE_TAP_ACTION_KEY, action.rawValue) }
        _airVisionHudControls.value = _airVisionHudControls.value.copy(singleTapAction = action)
    }

    fun setAirVisionHudDoubleTapAction(action: AirVisionHudDoubleTapAction) {
        plainPrefs.edit { putString(AIR_VISION_HUD_DOUBLE_TAP_ACTION_KEY, action.rawValue) }
        _airVisionHudControls.value = _airVisionHudControls.value.copy(doubleTapAction = action)
    }

    fun setAirVisionHudSwipeAction(action: AirVisionHudSwipeAction) {
        plainPrefs.edit { putString(AIR_VISION_HUD_SWIPE_ACTION_KEY, action.rawValue) }
        _airVisionHudControls.value = _airVisionHudControls.value.copy(swipeAction = action)
    }

    fun setAirVisionHudBrightnessKeyAction(action: AirVisionHudKeyAction) {
        plainPrefs.edit { putString(AIR_VISION_HUD_BRIGHTNESS_KEY_ACTION_KEY, action.rawValue) }
        _airVisionHudControls.value = _airVisionHudControls.value.copy(brightnessKeyAction = action)
    }

    fun setAirVisionHudMediaKeyAction(action: AirVisionHudMediaKeyAction) {
        plainPrefs.edit { putString(AIR_VISION_HUD_MEDIA_KEY_ACTION_KEY, action.rawValue) }
        _airVisionHudControls.value = _airVisionHudControls.value.copy(mediaKeyAction = action)
    }

    fun setAirVisionAppLanguage(language: AirVisionAppLanguage) {
        plainPrefs.edit { putString(AIR_VISION_APP_LANGUAGE_KEY, language.rawValue) }
        _airVisionAppLanguage.value = language
    }

    fun setAirVisionStartupDestination(destination: AirVisionStartupDestination) {
        plainPrefs.edit { putString(AIR_VISION_STARTUP_DESTINATION_KEY, destination.rawValue) }
        _airVisionStartupDestination.value = destination
    }

    fun setAirVisionHudDisplayTarget(target: AirVisionHudDisplayTarget) {
        plainPrefs.edit { putString(AIR_VISION_HUD_DISPLAY_TARGET_KEY, target.rawValue) }
        _airVisionHudDisplayTarget.value = target
    }

    fun setAirVisionCustomProfileLabel(
        mode: AirVisionViewMode,
        label: String,
    ) {
        val key =
            when (mode) {
                AirVisionViewMode.Custom1 -> AIR_VISION_CUSTOM_1_LABEL_KEY
                AirVisionViewMode.Custom2 -> AIR_VISION_CUSTOM_2_LABEL_KEY
                else -> return
            }
        val normalized = AirVisionCustomProfileLabels.normalizeLabel(label, mode.label)
        plainPrefs.edit { putString(key, normalized) }
        _airVisionCustomProfileLabels.value =
            when (mode) {
                AirVisionViewMode.Custom1 -> _airVisionCustomProfileLabels.value.copy(custom1 = normalized)
                AirVisionViewMode.Custom2 -> _airVisionCustomProfileLabels.value.copy(custom2 = normalized)
            }
    }

    fun copyActiveAirVisionProfileTo(targetMode: AirVisionViewMode): Boolean {
        if (targetMode != AirVisionViewMode.Custom1 && targetMode != AirVisionViewMode.Custom2) {
            return false
        }
        val copied = _airVisionDisplaySettings.value.copy(viewMode = targetMode).normalized
        plainPrefs.edit {
            putAirVisionProfileSettings(copied)
        }
        if (_airVisionDisplaySettings.value.viewMode == targetMode) {
            _airVisionDisplaySettings.value = copied
            _airVisionPhysicalMainScreenVisible.value = copied.physicalMainScreenVisible
        }
        return true
    }

    fun exportAirVisionProfileBackup(): String =
        AirVisionProfileBackups.encode(
            AirVisionProfileBackup(
                activeViewMode = _airVisionDisplaySettings.value.viewMode.rawValue,
                customLabels =
                    AirVisionBackupCustomLabels(
                        custom1 = _airVisionCustomProfileLabels.value.custom1,
                        custom2 = _airVisionCustomProfileLabels.value.custom2,
                    ),
                hudControls =
                    AirVisionBackupHudControls(
                        singleTapAction = _airVisionHudControls.value.singleTapAction.rawValue,
                        doubleTapAction = _airVisionHudControls.value.doubleTapAction.rawValue,
                        swipeAction = _airVisionHudControls.value.swipeAction.rawValue,
                        brightnessKeyAction = _airVisionHudControls.value.brightnessKeyAction.rawValue,
                        mediaKeyAction = _airVisionHudControls.value.mediaKeyAction.rawValue,
                    ),
                appPreferences =
                    AirVisionBackupAppPreferences(
                        language = _airVisionAppLanguage.value.rawValue,
                        startupDestination = _airVisionStartupDestination.value.rawValue,
                        hudDisplayTarget = _airVisionHudDisplayTarget.value.rawValue,
                        demoModeEnabled = _airVisionDemoModeEnabled.value,
                    ),
                runtimeProfiles =
                    AirVisionViewMode.entries.map { mode ->
                        AirVisionProfileBackups.runtimeProfileFromSettings(loadAirVisionDisplaySettings(mode))
                    },
                profiles =
                    AirVisionViewMode.entries.map { mode ->
                        AirVisionProfileBackups.profileFromSettings(loadAirVisionDisplaySettings(mode))
                    },
            ),
        )

    fun importAirVisionProfileBackup(raw: String) {
        val backup = AirVisionProfileBackups.decode(raw)
        val activeViewMode = AirVisionProfileBackups.requireViewMode(backup.activeViewMode)
        val resolvedProfiles = backup.profiles.map(AirVisionProfileBackups::settingsFromProfile)
        val duplicatedModes =
            resolvedProfiles
                .groupingBy { it.viewMode }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplicatedModes.isEmpty()) {
            "Profile backup includes duplicate profiles: ${duplicatedModes.joinToString { it.label }}."
        }
        val profileByMode =
            resolvedProfiles.associateBy { it.viewMode }
        val missingModes = AirVisionViewMode.entries.filterNot { profileByMode.containsKey(it) }
        require(missingModes.isEmpty()) {
            "Profile backup is missing: ${missingModes.joinToString { it.label }}."
        }

        val labels = AirVisionProfileBackups.labelsFromBackup(backup.customLabels)
        val controls = AirVisionProfileBackups.controlsFromBackup(backup.hudControls)
        val appPreferences = AirVisionProfileBackups.appPreferencesFromBackup(backup.appPreferences)
        val activeSettings = checkNotNull(profileByMode[activeViewMode])

        plainPrefs.edit {
            putString(AIR_VISION_VIEW_MODE_KEY, activeViewMode.rawValue)
            putString(AIR_VISION_CUSTOM_1_LABEL_KEY, labels.custom1)
            putString(AIR_VISION_CUSTOM_2_LABEL_KEY, labels.custom2)
            putString(AIR_VISION_HUD_SINGLE_TAP_ACTION_KEY, controls.singleTapAction.rawValue)
            putString(AIR_VISION_HUD_DOUBLE_TAP_ACTION_KEY, controls.doubleTapAction.rawValue)
            putString(AIR_VISION_HUD_SWIPE_ACTION_KEY, controls.swipeAction.rawValue)
            putString(AIR_VISION_HUD_BRIGHTNESS_KEY_ACTION_KEY, controls.brightnessKeyAction.rawValue)
            putString(AIR_VISION_HUD_MEDIA_KEY_ACTION_KEY, controls.mediaKeyAction.rawValue)
            putString(AIR_VISION_APP_LANGUAGE_KEY, appPreferences.language.rawValue)
            putString(AIR_VISION_STARTUP_DESTINATION_KEY, appPreferences.startupDestination.rawValue)
            putString(AIR_VISION_HUD_DISPLAY_TARGET_KEY, appPreferences.hudDisplayTarget.rawValue)
            putBoolean(AIR_VISION_DEMO_MODE_ENABLED_KEY, appPreferences.demoModeEnabled)
            AirVisionViewMode.entries.forEach { mode ->
                putAirVisionProfileSettings(checkNotNull(profileByMode[mode]))
            }
        }

        _airVisionCustomProfileLabels.value = labels
        _airVisionHudControls.value = controls
        _airVisionAppLanguage.value = appPreferences.language
        _airVisionStartupDestination.value = appPreferences.startupDestination
        _airVisionHudDisplayTarget.value = appPreferences.hudDisplayTarget
        _airVisionDemoModeEnabled.value = appPreferences.demoModeEnabled
        _airVisionDisplaySettings.value = activeSettings
        _airVisionPhysicalMainScreenVisible.value = activeSettings.physicalMainScreenVisible
    }

    fun setAirVisionPhysicalMainScreenVisible(visible: Boolean) {
        val viewMode = _airVisionDisplaySettings.value.viewMode
        plainPrefs.edit {
            putBoolean(airVisionProfileKey(AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY, viewMode), visible)
        }
        _airVisionDisplaySettings.value = _airVisionDisplaySettings.value.copy(physicalMainScreenVisible = visible)
        _airVisionPhysicalMainScreenVisible.value = visible
    }

    fun setAirVisionDemoModeEnabled(enabled: Boolean) {
        plainPrefs.edit { putBoolean(AIR_VISION_DEMO_MODE_ENABLED_KEY, enabled) }
        _airVisionDemoModeEnabled.value = enabled
    }

    fun setTranslationCaptionSourceLanguage(value: String) {
        val normalized =
            TranslationCaptionMode.normalizeLanguageCode(
                value,
                TranslationCaptionMode.DEFAULT_SOURCE_LANGUAGE,
            )
        plainPrefs.edit { putString("translationCaptions.sourceLanguage", normalized) }
        _translationCaptionSourceLanguage.value = normalized
    }

    fun setTranslationCaptionTargetLanguage(value: String) {
        val normalized =
            TranslationCaptionMode.normalizeLanguageCode(
                value,
                TranslationCaptionMode.DEFAULT_TARGET_LANGUAGE,
            )
        plainPrefs.edit { putString("translationCaptions.targetLanguage", normalized) }
        _translationCaptionTargetLanguage.value = normalized
    }

    private fun loadNotificationForwardingPackages(): Set<String> {
        val raw = plainPrefs.getString(notificationsForwardingPackagesKey, null)?.trim()
        if (raw.isNullOrEmpty()) {
            return emptySet()
        }
        return try {
            val element = json.parseToJsonElement(raw)
            val array = element as? JsonArray ?: return emptySet()
            array
                .mapNotNull { item ->
                    when (item) {
                        is JsonNull -> null
                        is JsonPrimitive -> item.content.trim().takeIf { it.isNotEmpty() }
                        else -> null
                    }
                }.toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun loadVoiceWakeMode(): VoiceWakeMode {
        val raw = plainPrefs.getString(voiceWakeModeKey, null)
        val resolved = VoiceWakeMode.fromRawValue(raw)

        // Default ON (foreground) when unset.
        if (raw.isNullOrBlank()) {
            plainPrefs.edit { putString(voiceWakeModeKey, resolved.rawValue) }
        }

        return resolved
    }

    private fun loadLocationMode(): LocationMode {
        val raw = plainPrefs.getString(locationModeKey, "off")
        val resolved = LocationMode.fromRawValue(raw)
        if (raw?.trim()?.lowercase() == "always") {
            plainPrefs.edit { putString(locationModeKey, resolved.rawValue) }
        }
        return resolved
    }

    private fun loadAirVisionDisplaySettings(
        viewMode: AirVisionViewMode = AirVisionViewMode.fromRawValue(plainPrefs.getString(AIR_VISION_VIEW_MODE_KEY, null)),
    ): AirVisionDisplaySettings {
        val defaults = AirVisionDisplaySettings.defaultsForViewMode(viewMode)
        val legacyViewMode = AirVisionViewMode.fromRawValue(plainPrefs.getString(AIR_VISION_VIEW_MODE_KEY, null))
        val allowLegacyFallback = viewMode == legacyViewMode && !hasAnyAirVisionProfileValue()
        return AirVisionDisplaySettings(
            viewMode = viewMode,
            splendidMode =
                AirVisionSplendidMode.fromRawValue(
                    getAirVisionProfileString(
                        key = AIR_VISION_SPLENDID_MODE_KEY,
                        mode = viewMode,
                        allowLegacyFallback = allowLegacyFallback,
                        defaultValue = defaults.splendidMode.rawValue,
                    ),
                ),
            hudPlacement =
                AirVisionHudPlacement.fromRawValue(
                    getAirVisionProfileString(
                        key = AIR_VISION_HUD_PLACEMENT_KEY,
                        mode = viewMode,
                        allowLegacyFallback = allowLegacyFallback,
                        defaultValue = defaults.hudPlacement.rawValue,
                    ),
                ),
            brightnessPercent =
                getAirVisionProfileInt(
                    key = AIR_VISION_BRIGHTNESS_PERCENT_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.brightnessPercent,
                ),
            blueLightFilterPercent =
                getAirVisionProfileInt(
                    key = AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.blueLightFilterPercent,
                ),
            distanceCm =
                getAirVisionProfileInt(
                    key = AIR_VISION_DISTANCE_CM_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.distanceCm,
                ),
            ipdMm =
                getAirVisionProfileInt(
                    key = AIR_VISION_IPD_MM_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.ipdMm,
                ),
            safeAreaPercent =
                getAirVisionProfileInt(
                    key = AIR_VISION_SAFE_AREA_PERCENT_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.safeAreaPercent,
                ),
            physicalMainScreenVisible =
                getAirVisionProfileBoolean(
                    key = AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback || plainPrefs.contains(AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY),
                    defaultValue = defaults.physicalMainScreenVisible,
                ),
            motionSyncEnabled =
                getAirVisionProfileBoolean(
                    key = AIR_VISION_MOTION_SYNC_ENABLED_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.motionSyncEnabled,
                ),
            threeDModeEnabled =
                getAirVisionProfileBoolean(
                    key = AIR_VISION_THREE_D_MODE_ENABLED_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.threeDModeEnabled,
                ),
            lightLoadModeEnabled =
                getAirVisionProfileBoolean(
                    key = AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY,
                    mode = viewMode,
                    allowLegacyFallback = allowLegacyFallback,
                    defaultValue = defaults.lightLoadModeEnabled,
                ),
        ).normalized
    }

    private fun airVisionProfileKey(
        key: String,
        mode: AirVisionViewMode,
    ): String = "$key.${mode.rawValue}"

    private fun hasAnyAirVisionProfileValue(): Boolean {
        val keys =
            listOf(
                AIR_VISION_SPLENDID_MODE_KEY,
                AIR_VISION_HUD_PLACEMENT_KEY,
                AIR_VISION_BRIGHTNESS_PERCENT_KEY,
                AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY,
                AIR_VISION_DISTANCE_CM_KEY,
                AIR_VISION_IPD_MM_KEY,
                AIR_VISION_SAFE_AREA_PERCENT_KEY,
                AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY,
                AIR_VISION_MOTION_SYNC_ENABLED_KEY,
                AIR_VISION_THREE_D_MODE_ENABLED_KEY,
                AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY,
            )
        return AirVisionViewMode.entries.any { mode ->
            keys.any { key -> plainPrefs.contains(airVisionProfileKey(key, mode)) }
        }
    }

    private fun SharedPreferences.Editor.putAirVisionProfileSettings(settings: AirVisionDisplaySettings) {
        val viewMode = settings.viewMode
        putString(airVisionProfileKey(AIR_VISION_SPLENDID_MODE_KEY, viewMode), settings.splendidMode.rawValue)
        putString(airVisionProfileKey(AIR_VISION_HUD_PLACEMENT_KEY, viewMode), settings.hudPlacement.rawValue)
        putInt(airVisionProfileKey(AIR_VISION_BRIGHTNESS_PERCENT_KEY, viewMode), settings.brightnessPercent)
        putInt(airVisionProfileKey(AIR_VISION_BLUE_LIGHT_FILTER_PERCENT_KEY, viewMode), settings.blueLightFilterPercent)
        putInt(airVisionProfileKey(AIR_VISION_DISTANCE_CM_KEY, viewMode), settings.distanceCm)
        putInt(airVisionProfileKey(AIR_VISION_IPD_MM_KEY, viewMode), settings.ipdMm)
        putInt(airVisionProfileKey(AIR_VISION_SAFE_AREA_PERCENT_KEY, viewMode), settings.safeAreaPercent)
        putBoolean(
            airVisionProfileKey(AIR_VISION_PHYSICAL_MAIN_SCREEN_VISIBLE_KEY, viewMode),
            settings.physicalMainScreenVisible,
        )
        putBoolean(airVisionProfileKey(AIR_VISION_MOTION_SYNC_ENABLED_KEY, viewMode), settings.motionSyncEnabled)
        putBoolean(airVisionProfileKey(AIR_VISION_THREE_D_MODE_ENABLED_KEY, viewMode), settings.threeDModeEnabled)
        putBoolean(airVisionProfileKey(AIR_VISION_LIGHT_LOAD_MODE_ENABLED_KEY, viewMode), settings.lightLoadModeEnabled)
    }

    private fun getAirVisionProfileString(
        key: String,
        mode: AirVisionViewMode,
        allowLegacyFallback: Boolean,
        defaultValue: String,
    ): String {
        val profileKey = airVisionProfileKey(key, mode)
        if (plainPrefs.contains(profileKey)) {
            return plainPrefs.getString(profileKey, defaultValue) ?: defaultValue
        }
        if (allowLegacyFallback && plainPrefs.contains(key)) {
            return plainPrefs.getString(key, defaultValue) ?: defaultValue
        }
        return defaultValue
    }

    private fun getAirVisionProfileInt(
        key: String,
        mode: AirVisionViewMode,
        allowLegacyFallback: Boolean,
        defaultValue: Int,
    ): Int {
        val profileKey = airVisionProfileKey(key, mode)
        return when {
            plainPrefs.contains(profileKey) -> plainPrefs.getInt(profileKey, defaultValue)
            allowLegacyFallback && plainPrefs.contains(key) -> plainPrefs.getInt(key, defaultValue)
            else -> defaultValue
        }
    }

    private fun getAirVisionProfileBoolean(
        key: String,
        mode: AirVisionViewMode,
        allowLegacyFallback: Boolean,
        defaultValue: Boolean,
    ): Boolean {
        val profileKey = airVisionProfileKey(key, mode)
        return when {
            plainPrefs.contains(profileKey) -> plainPrefs.getBoolean(profileKey, defaultValue)
            allowLegacyFallback && plainPrefs.contains(key) -> plainPrefs.getBoolean(key, defaultValue)
            else -> defaultValue
        }
    }

    private fun loadAirVisionHudControls(): AirVisionHudControls =
        AirVisionHudControls(
            singleTapAction =
                AirVisionHudTouchAction.fromRawValue(
                    plainPrefs.getString(AIR_VISION_HUD_SINGLE_TAP_ACTION_KEY, null),
                ),
            doubleTapAction =
                AirVisionHudDoubleTapAction.fromRawValue(
                    plainPrefs.getString(AIR_VISION_HUD_DOUBLE_TAP_ACTION_KEY, null),
                ),
            swipeAction =
                AirVisionHudSwipeAction.fromRawValue(
                    plainPrefs.getString(AIR_VISION_HUD_SWIPE_ACTION_KEY, null),
                ),
            brightnessKeyAction =
                AirVisionHudKeyAction.fromRawValue(
                    plainPrefs.getString(AIR_VISION_HUD_BRIGHTNESS_KEY_ACTION_KEY, null),
                ),
            mediaKeyAction =
                AirVisionHudMediaKeyAction.fromRawValue(
                    plainPrefs.getString(AIR_VISION_HUD_MEDIA_KEY_ACTION_KEY, null),
                ),
        )

    private fun loadAirVisionCustomProfileLabels(): AirVisionCustomProfileLabels =
        AirVisionCustomProfileLabels(
            custom1 =
                AirVisionCustomProfileLabels.normalizeLabel(
                    plainPrefs.getString(AIR_VISION_CUSTOM_1_LABEL_KEY, null).orEmpty(),
                    AirVisionViewMode.Custom1.label,
                ),
            custom2 =
                AirVisionCustomProfileLabels.normalizeLabel(
                    plainPrefs.getString(AIR_VISION_CUSTOM_2_LABEL_KEY, null).orEmpty(),
                    AirVisionViewMode.Custom2.label,
                ),
        )

    private fun loadWakeWords(): List<String> {
        val raw = plainPrefs.getString("voiceWake.triggerWords", null)?.trim()
        if (raw.isNullOrEmpty()) return defaultWakeWords
        return try {
            val element = json.parseToJsonElement(raw)
            val array = element as? JsonArray ?: return defaultWakeWords
            val decoded =
                array.mapNotNull { item ->
                    when (item) {
                        is JsonNull -> null
                        is JsonPrimitive -> item.content.trim().takeIf { it.isNotEmpty() }
                        else -> null
                    }
                }
            WakeWords.sanitize(decoded, defaultWakeWords)
        } catch (_: Throwable) {
            defaultWakeWords
        }
    }
}
