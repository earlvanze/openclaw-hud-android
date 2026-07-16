package ai.openclaw.app

import ai.openclaw.app.ui.OpenClawTheme
import ai.openclaw.app.ui.RootScreen
import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DeviceProductInfo
import android.hardware.display.DisplayManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionRequester: PermissionRequester
    private var didAttachRuntimeUi = false
    private var didStartNodeService = false
    private var hudMediaSession: MediaSession? = null
    private val hudPresentationSession = HudPresentationSessionController<HudPresentation>()
    private var hudPresentationRecoveryRunnable: Runnable? = null
    private var hudPresentationStabilityRunnable: Runnable? = null
    private var hudDisplayListenerRegistered = false
    private var hudUnlockReceiverRegistered = false
    private var appliedAirVisionAppLanguage: AirVisionAppLanguage? = null
    private val hudKeyInputController = AirVisionHudKeyInputController()
    private val hudSystemBarsHandler = Handler(Looper.getMainLooper())
    private val hudDisplayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                showHudPresentationIfAvailable()
            }

            override fun onDisplayChanged(displayId: Int) {
                showHudPresentationIfAvailable()
            }

            override fun onDisplayRemoved(displayId: Int) {
                val presentation = hudPresentationSession.current
                if (presentation?.display?.displayId == displayId) {
                    releaseHudPresentation(presentation, dismiss = true)
                }
                showHudPresentationIfAvailable()
            }
        }
    private val hudUnlockReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action != Intent.ACTION_USER_PRESENT) return
                recoverHudPresentationAfterUnlock()
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AirVisionAppLocale.wrap(newBase, AirVisionAppLocale.storedLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.OPENCLAW_DEFAULT_HUD) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        super.onCreate(savedInstanceState)
        if (relaunchHudHostOnDefaultDisplayIfNeeded()) return
        wakeDefaultDisplayForExternalHudInputIfNeeded()
        handleGatewaySetupIntent(intent)
        handleAssistantIntent(intent)
        handlePlayReviewDemoIntent(intent)
        if (BuildConfig.OPENCLAW_DEFAULT_HUD) {
            applyPhoneSystemBars()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        AirVisionAudioRouter.applyHudRoute(this)
        permissionRequester = PermissionRequester(this)
        setupHudMediaSession()
        registerHudDisplayListener()
        registerHudUnlockReceiver()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.preventSleep.collect { enabled ->
                    if (enabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.airVisionHudDisplayTarget.collect {
                    showHudPresentationIfAvailable()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.airVisionRememberedDisplay.collect {
                    showHudPresentationIfAvailable()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.airVisionAppLanguage.collect { language ->
                    val previous = appliedAirVisionAppLanguage
                    appliedAirVisionAppLanguage = language
                    AirVisionAppLocale.apply(this@MainActivity, language)
                    if (previous != null && previous != language) {
                        recreate()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.runtimeInitialized.collect { ready ->
                    if (!ready || didAttachRuntimeUi) return@collect
                    viewModel.attachRuntimeUi(owner = this@MainActivity, permissionRequester = permissionRequester)
                    didAttachRuntimeUi = true
                    if (!didStartNodeService) {
                        NodeForegroundService.start(this@MainActivity)
                        didStartNodeService = true
                    }
                }
            }
        }

        setContent {
            OpenClawTheme {
                Surface(modifier = Modifier) {
                    RootScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setForeground(true)
        applyPhoneSystemBars()
        AirVisionAudioRouter.applyHudRoute(this)
        setHudMediaSessionActive(true)
        registerHudDisplayListener()
        showHudPresentationIfAvailable()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyPhoneSystemBars()
            AirVisionAudioRouter.applyHudRoute(this)
            showHudPresentationIfAvailable()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (handleHudKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val presentation = hudPresentationSession.current
        val inputDevice = event.device
        if (
            presentation?.isShowing == true &&
            inputDevice != null &&
            shouldForwardExternalHudTouch(
                deviceIsExternal = inputDevice.isExternal,
                eventSource = event.source,
                eventDisplayId = Display.DEFAULT_DISPLAY,
                hudDisplayId = presentation.display.displayId,
            ) &&
            presentation.dispatchExternalTouchEvent(event)
        ) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                Log.d(
                    TAG,
                    "Forwarded external HUD touch from ${inputDevice.name} source=0x${event.source.toString(16)} " +
                        "to HUD display ${presentation.display.displayId}",
                )
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onStop() {
        if (BuildConfig.OPENCLAW_DEFAULT_HUD && hudPresentationSession.current?.isShowing == true) {
            setHudMediaSessionActive(true)
            viewModel.setForeground(true)
        } else {
            setHudMediaSessionActive(false)
            viewModel.setForeground(false)
        }
        super.onStop()
    }

    override fun onDestroy() {
        unregisterHudDisplayListener()
        unregisterHudUnlockReceiver()
        cancelHudPresentationRecovery(resetAttempts = true)
        cancelHudPresentationStability()
        hudPresentationSession.current?.let { releaseHudPresentation(it, dismiss = true) }
        viewModel.setAirVisionHudPresentationActive(false)
        hudMediaSession?.release()
        hudMediaSession = null
        AirVisionAudioRouter.clearHudRoute(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGatewaySetupIntent(intent)
        handleAssistantIntent(intent)
        handlePlayReviewDemoIntent(intent)
    }

    private fun handleGatewaySetupIntent(intent: android.content.Intent?) {
        val request = parseGatewaySetupLaunchIntent(intent) ?: return
        viewModel.handleGatewaySetupLaunch(request)
    }

    private fun handleAssistantIntent(intent: android.content.Intent?) {
        val request = parseAssistantLaunchIntent(intent) ?: return
        viewModel.handleAssistantLaunch(request)
    }

    private fun handlePlayReviewDemoIntent(intent: android.content.Intent?) {
        val request = parsePlayReviewDemoLaunchIntent(intent) ?: return
        viewModel.handlePlayReviewDemoLaunch(request)
    }

    private fun registerHudDisplayListener() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD || hudDisplayListenerRegistered) return
        val displayManager = getSystemService(DisplayManager::class.java) ?: return
        displayManager.registerDisplayListener(hudDisplayListener, hudSystemBarsHandler)
        hudDisplayListenerRegistered = true
    }

    private fun unregisterHudDisplayListener() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD || !hudDisplayListenerRegistered) return
        val displayManager = getSystemService(DisplayManager::class.java) ?: return
        displayManager.unregisterDisplayListener(hudDisplayListener)
        hudDisplayListenerRegistered = false
    }

    private fun registerHudUnlockReceiver() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD || hudUnlockReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            hudUnlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        hudUnlockReceiverRegistered = true
    }

    private fun unregisterHudUnlockReceiver() {
        if (!hudUnlockReceiverRegistered) return
        unregisterReceiver(hudUnlockReceiver)
        hudUnlockReceiverRegistered = false
    }

    private fun recoverHudPresentationAfterUnlock() {
        val current = hudPresentationSession.current
        if (current != null) {
            Log.d(TAG, "Recreating HUD presentation after device unlock")
            releaseHudPresentation(current, dismiss = true)
        }
        showHudPresentationIfAvailable()
    }

    private fun showHudPresentationIfAvailable() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD || isFinishing || isDestroyed) return
        if (isOnExternalDisplay()) {
            cancelHudPresentationRecovery(resetAttempts = true)
            cancelHudPresentationStability()
            hudPresentationSession.current?.let { releaseHudPresentation(it, dismiss = true) }
            viewModel.setAirVisionHudPresentationActive(false)
            viewModel.setAirVisionHudDisplayRoute(
                AirVisionHudDisplayRoute(
                    target = viewModel.airVisionHudDisplayTarget.value,
                    rememberedDisplay = viewModel.airVisionRememberedDisplay.value,
                    reason = "activity_on_external_display",
                ),
            )
            applyPhoneSystemBars()
            return
        }
        val displayManager =
            getSystemService(DisplayManager::class.java)
                ?: run {
                    viewModel.setAirVisionHudDisplayRoute(
                        AirVisionHudDisplayRoute(
                            target = viewModel.airVisionHudDisplayTarget.value,
                            rememberedDisplay = viewModel.airVisionRememberedDisplay.value,
                            reason = "display_manager_unavailable",
                        ),
                    )
                    if (hudPresentationSession.current == null) {
                        scheduleHudPresentationRecovery("display_manager_unavailable")
                    }
                    return
                }
        val presentationDisplayIds =
            displayManager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                .map { it.displayId }
                .toSet()
        val externalDisplays =
            displayManager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY && it.isValid }
        val displayRoute =
            AirVisionHudDisplayRouter.select(
                candidates = externalDisplays.map { it.toHudDisplayCandidate(presentationDisplayIds) },
                target = viewModel.airVisionHudDisplayTarget.value,
                rememberedDisplay = viewModel.airVisionRememberedDisplay.value,
            )
        viewModel.setAirVisionHudDisplayRoute(displayRoute)
        val targetDisplay =
            externalDisplays.firstOrNull { it.displayId == displayRoute.selectedCandidate?.displayId }
                ?: run {
                    cancelHudPresentationRecovery(resetAttempts = true)
                    cancelHudPresentationStability()
                    hudPresentationSession.current?.let { releaseHudPresentation(it, dismiss = true) }
                    viewModel.setAirVisionHudPresentationActive(false)
                    return
                }

        hudPresentationSession.current?.let { current ->
            if (current.display.displayId == targetDisplay.displayId && current.isShowing) {
                cancelHudPresentationRecovery(resetAttempts = false)
                hudPresentationSession.markShown(current)
                scheduleHudPresentationStability(current)
                viewModel.setAirVisionHudPresentationActive(true)
                return
            }
            releaseHudPresentation(current, dismiss = true)
        }

        val presentation = HudPresentation(this, targetDisplay, viewModel, onHudKeyEvent = ::handleHudKeyEvent)
        hudPresentationSession.attach(presentation)
        presentation.setOnDismissListener {
            if (hudPresentationSession.release(presentation)) {
                cancelHudPresentationStability()
                viewModel.setAirVisionHudPresentationActive(false)
                scheduleHudPresentationRecovery("presentation_dismissed")
            }
        }
        runCatching { presentation.show() }
            .onSuccess {
                if (hudPresentationSession.markShown(presentation)) {
                    cancelHudPresentationRecovery(resetAttempts = false)
                    scheduleHudPresentationStability(presentation)
                    Log.d(TAG, "HUD presentation shown on display ${targetDisplay.displayId} ${targetDisplay.name}")
                    viewModel.setAirVisionHudPresentationActive(true)
                    applyPhoneSystemBars()
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed to show HUD presentation on display ${targetDisplay.displayId}", error)
                if (releaseHudPresentation(presentation, dismiss = true)) {
                    scheduleHudPresentationRecovery("presentation_show_failed")
                }
            }
    }

    private fun releaseHudPresentation(
        presentation: HudPresentation,
        dismiss: Boolean,
    ): Boolean {
        if (!hudPresentationSession.release(presentation)) return false
        cancelHudPresentationStability()
        if (dismiss) {
            runCatching { presentation.dismiss() }
                .onFailure { error -> Log.w(TAG, "Failed to dismiss HUD presentation", error) }
        }
        viewModel.setAirVisionHudPresentationActive(false)
        return true
    }

    private fun scheduleHudPresentationRecovery(reason: String) {
        if (
            !BuildConfig.OPENCLAW_DEFAULT_HUD ||
            isFinishing ||
            isDestroyed ||
            hudPresentationRecoveryRunnable != null
        ) {
            return
        }
        val delayMs = hudPresentationSession.nextRecoveryDelayMs()
        lateinit var recovery: Runnable
        recovery =
            Runnable {
                if (hudPresentationRecoveryRunnable !== recovery) return@Runnable
                hudPresentationRecoveryRunnable = null
                showHudPresentationIfAvailable()
            }
        hudPresentationRecoveryRunnable = recovery
        hudSystemBarsHandler.postDelayed(recovery, delayMs)
        Log.d(TAG, "Scheduled HUD presentation recovery in ${delayMs}ms reason=$reason")
    }

    private fun cancelHudPresentationRecovery(resetAttempts: Boolean) {
        hudPresentationRecoveryRunnable?.let(hudSystemBarsHandler::removeCallbacks)
        hudPresentationRecoveryRunnable = null
        if (resetAttempts) {
            hudPresentationSession.resetRecovery()
        }
    }

    private fun scheduleHudPresentationStability(presentation: HudPresentation) {
        cancelHudPresentationStability()
        lateinit var stability: Runnable
        stability =
            Runnable {
                if (hudPresentationStabilityRunnable !== stability) return@Runnable
                hudPresentationStabilityRunnable = null
                if (presentation.isShowing && hudPresentationSession.markStable(presentation)) {
                    Log.d(TAG, "HUD presentation stable on display ${presentation.display.displayId}")
                }
            }
        hudPresentationStabilityRunnable = stability
        hudSystemBarsHandler.postDelayed(stability, PRESENTATION_STABILITY_INTERVAL_MS)
    }

    private fun cancelHudPresentationStability() {
        hudPresentationStabilityRunnable?.let(hudSystemBarsHandler::removeCallbacks)
        hudPresentationStabilityRunnable = null
    }

    private fun Display.toHudDisplayCandidate(presentationDisplayIds: Set<Int>): AirVisionHudDisplayCandidate =
        AirVisionHudDisplayCandidate(
            displayId = displayId,
            name = name.orEmpty(),
            widthPx = mode?.physicalWidth ?: 0,
            heightPx = mode?.physicalHeight ?: 0,
            isPresentation = displayId in presentationDisplayIds,
            isInternal =
                deviceProductInfo?.connectionToSinkType ==
                    DeviceProductInfo.CONNECTION_TO_SINK_BUILT_IN,
        )

    private fun setupHudMediaSession() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD || hudMediaSession != null) return
        hudMediaSession =
            MediaSession(this, "OpenClawHudMedia").apply {
                @Suppress("DEPRECATION")
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(
                    object : MediaSession.Callback() {
                        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                            val event = mediaButtonIntent.mediaButtonKeyEvent() ?: return false
                            if (event.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                                return false
                            }
                            return handleHudKeyEvent(event, source = "media-button")
                        }

                        override fun onPlay() {
                            handleHudMicTap(source = "media-play")
                        }

                        override fun onPause() {
                            handleHudMicTap(source = "media-pause")
                        }
                    },
                )
                setPlaybackState(buildHudMediaPlaybackState())
            }
    }

    private fun Intent.mediaButtonKeyEvent(): KeyEvent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }

    private fun setHudMediaSessionActive(active: Boolean) {
        hudMediaSession?.apply {
            setPlaybackState(buildHudMediaPlaybackState())
            isActive = active
        }
    }

    private fun refreshHudMediaSessionState() {
        hudMediaSession?.setPlaybackState(buildHudMediaPlaybackState())
    }

    private fun buildHudMediaPlaybackState(): PlaybackState =
        PlaybackState
            .Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE,
            ).setState(
                if (viewModel.micEnabled.value) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1f,
            ).build()

    internal fun handleHudKeyEvent(event: KeyEvent): Boolean {
        return handleHudKeyEvent(event, source = "key")
    }

    private fun handleHudKeyEvent(
        event: KeyEvent,
        source: String,
    ): Boolean {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD) return false
        val decision =
            hudKeyInputController.handleKeyEvent(
                keyCode = event.keyCode,
                action = event.action,
                eventTimeMs = event.eventTime.takeIf { it > 0L } ?: SystemClock.uptimeMillis(),
                isHudAccessoryEvent = event.isHudAccessoryEvent(),
                controls = viewModel.airVisionHudControls.value,
            )
        handleHudKeyCommand(decision.command, event = event, source = source)
        return decision.consume
    }

    private fun handleHudMicTap(source: String) {
        if (viewModel.airVisionHudControls.value.mediaKeyAction != AirVisionHudMediaKeyAction.DoubleTapToggleMic) {
            return
        }
        handleHudKeyCommand(
            command = hudKeyInputController.handleMicTap(SystemClock.uptimeMillis()),
            event = null,
            source = source,
        )
    }

    private fun handleHudKeyCommand(
        command: AirVisionHudKeyCommand?,
        event: KeyEvent?,
        source: String,
    ) {
        when (command) {
            is AirVisionHudKeyCommand.ScrollChat -> {
                viewModel.requestHudScroll(command.deltaPx)
                Log.d(TAG, "HUD brightness key scrolled chat keyCode=${event?.keyCode} delta=${command.deltaPx}")
            }
            is AirVisionHudKeyCommand.AdjustBrightness -> {
                viewModel.adjustAirVisionBrightnessPercent(command.deltaPercent)
                Log.d(
                    TAG,
                    "HUD brightness key adjusted dimming keyCode=${event?.keyCode} delta=${command.deltaPercent}",
                )
            }
            is AirVisionHudKeyCommand.AdjustDistance -> {
                viewModel.adjustAirVisionDistanceCm(command.deltaCm)
                Log.d(TAG, "HUD brightness key adjusted distance keyCode=${event?.keyCode} deltaCm=${command.deltaCm}")
            }
            AirVisionHudKeyCommand.ToggleMic -> {
                toggleMicFromHudInput()
                Log.d(TAG, "HUD $source double-tap toggled mic enabled=${viewModel.micEnabled.value}")
                refreshHudMediaSessionState()
                applyPhoneSystemBars()
            }
            AirVisionHudKeyCommand.ArmMicDoubleTap -> {
                Log.d(TAG, "HUD $source tap armed mic double-tap")
            }
            AirVisionHudKeyCommand.LogUnhandledHudAccessoryKey -> {
                Log.d(TAG, "unhandled HUD accessory key keyCode=${event?.keyCode} scanCode=${event?.scanCode}")
            }
            null -> Unit
        }
    }

    private fun toggleMicFromHudInput() {
        if (viewModel.micEnabled.value) {
            viewModel.setMicEnabled(false)
            return
        }

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.toggleMicEnabled()
            return
        }

        lifecycleScope.launch {
            val granted =
                permissionRequester.requestIfMissing(listOf(Manifest.permission.RECORD_AUDIO))[Manifest.permission.RECORD_AUDIO] == true
            if (granted) {
                viewModel.setMicEnabled(true)
            }
        }
    }

    private fun KeyEvent.isHudAccessoryEvent(): Boolean {
        val inputDevice = device ?: InputDevice.getDevice(deviceId) ?: return false
        return inputDevice.isExternal ||
            inputDevice.name.contains("AirVision", ignoreCase = true) ||
            inputDevice.name.contains("ASUS AirVision M1", ignoreCase = true) ||
            (inputDevice.vendorId == ASUS_VENDOR_ID && inputDevice.productId == AIRVISION_M1_PRODUCT_ID)
    }

    private fun isOnExternalDisplay(): Boolean =
        display?.let { activeDisplay ->
            activeDisplay.displayId != Display.DEFAULT_DISPLAY &&
                activeDisplay.deviceProductInfo?.connectionToSinkType !=
                DeviceProductInfo.CONNECTION_TO_SINK_BUILT_IN
        } == true

    private fun relaunchHudHostOnDefaultDisplayIfNeeded(): Boolean {
        val attempted = intent.getBooleanExtra(EXTRA_HUD_HOST_RELAUNCH_ATTEMPTED, false)
        if (!shouldRelaunchHudHostOnDefaultDisplay(BuildConfig.OPENCLAW_DEFAULT_HUD, isOnExternalDisplay(), attempted)) {
            return false
        }

        val relaunchIntent =
            Intent(intent).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                putExtra(EXTRA_HUD_HOST_RELAUNCH_ATTEMPTED, true)
            }
        val options =
            ActivityOptions
                .makeBasic()
                .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
                .toBundle()
        return runCatching {
            startActivity(relaunchIntent, options)
            Log.i(TAG, "Relaunching HUD host on display ${Display.DEFAULT_DISPLAY} for external input routing")
            finish()
            true
        }.getOrElse { error ->
            Log.w(TAG, "Failed to relaunch HUD host on the default display", error)
            false
        }
    }

    private fun wakeDefaultDisplayForExternalHudInputIfNeeded() {
        if (
            !BuildConfig.OPENCLAW_DEFAULT_HUD ||
            !intent.getBooleanExtra(EXTRA_HUD_HOST_RELAUNCH_ATTEMPTED, false) ||
            display?.displayId != Display.DEFAULT_DISPLAY
        ) {
            return
        }
        setTurnScreenOn(true)
        Log.i(TAG, "Waking display ${Display.DEFAULT_DISPLAY} for external HUD input routing")
    }

    @Suppress("DEPRECATION")
    private fun applyPhoneSystemBars() {
        if (!BuildConfig.OPENCLAW_DEFAULT_HUD) return
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.attributes =
            window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        private const val TAG = "MainActivity"
        private const val ASUS_VENDOR_ID = 0x0b05
        private const val AIRVISION_M1_PRODUCT_ID = 0x1b3c
        private const val PRESENTATION_STABILITY_INTERVAL_MS = 10_000L
        private const val EXTRA_HUD_HOST_RELAUNCH_ATTEMPTED =
            "ai.openclaw.app.extra.HUD_HOST_RELAUNCH_ATTEMPTED"
    }
}
