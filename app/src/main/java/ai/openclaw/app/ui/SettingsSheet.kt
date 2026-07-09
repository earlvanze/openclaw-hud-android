package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionDisplaySettings
import ai.openclaw.app.AirVisionHudDoubleTapAction
import ai.openclaw.app.AirVisionHudKeyAction
import ai.openclaw.app.AirVisionHudMediaKeyAction
import ai.openclaw.app.AirVisionHudPlacement
import ai.openclaw.app.AirVisionHudSwipeAction
import ai.openclaw.app.AirVisionHudTouchAction
import ai.openclaw.app.AirVisionSplendidMode
import ai.openclaw.app.AirVisionViewMode
import ai.openclaw.app.BuildConfig
import ai.openclaw.app.LocationMode
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.NotificationPackageFilterMode
import ai.openclaw.app.node.DeviceNotificationListenerService
import ai.openclaw.app.normalizeLocalHourMinute
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

@Composable
fun SettingsSheet(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val instanceId by viewModel.instanceId.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val cameraEnabled by viewModel.cameraEnabled.collectAsState()
    val locationMode by viewModel.locationMode.collectAsState()
    val locationPreciseEnabled by viewModel.locationPreciseEnabled.collectAsState()
    val preventSleep by viewModel.preventSleep.collectAsState()
    val canvasDebugStatusEnabled by viewModel.canvasDebugStatusEnabled.collectAsState()
    val notificationForwardingEnabled by viewModel.notificationForwardingEnabled.collectAsState()
    val notificationForwardingMode by viewModel.notificationForwardingMode.collectAsState()
    val notificationForwardingPackages by viewModel.notificationForwardingPackages.collectAsState()
    val notificationForwardingQuietHoursEnabled by viewModel.notificationForwardingQuietHoursEnabled.collectAsState()
    val notificationForwardingQuietStart by viewModel.notificationForwardingQuietStart.collectAsState()
    val notificationForwardingQuietEnd by viewModel.notificationForwardingQuietEnd.collectAsState()
    val notificationForwardingMaxEventsPerMinute by viewModel.notificationForwardingMaxEventsPerMinute.collectAsState()
    val notificationForwardingSessionKey by viewModel.notificationForwardingSessionKey.collectAsState()
    val airVisionDisplaySettings by viewModel.airVisionDisplaySettings.collectAsState()
    val airVisionHudControls by viewModel.airVisionHudControls.collectAsState()
    val airVisionUsbState by viewModel.airVisionUsbState.collectAsState()

    var notificationQuietStartDraft by remember(notificationForwardingQuietStart) {
        mutableStateOf(notificationForwardingQuietStart)
    }
    var notificationQuietEndDraft by remember(notificationForwardingQuietEnd) {
        mutableStateOf(notificationForwardingQuietEnd)
    }
    var notificationRateDraft by remember(notificationForwardingMaxEventsPerMinute) {
        mutableStateOf(notificationForwardingMaxEventsPerMinute.toString())
    }
    var notificationSessionKeyDraft by remember(notificationForwardingSessionKey) {
        mutableStateOf(notificationForwardingSessionKey.orEmpty())
    }
    val normalizedQuietStartDraft =
        remember(notificationQuietStartDraft) {
            normalizeLocalHourMinute(notificationQuietStartDraft)
        }
    val normalizedQuietEndDraft =
        remember(notificationQuietEndDraft) {
            normalizeLocalHourMinute(notificationQuietEndDraft)
        }
    val quietHoursDraftValid = normalizedQuietStartDraft != null && normalizedQuietEndDraft != null
    val selectedPackagesSummary =
        remember(notificationForwardingMode, notificationForwardingPackages) {
            when (notificationForwardingMode) {
                NotificationPackageFilterMode.Allowlist ->
                    if (notificationForwardingPackages.isEmpty()) {
                        "Selected: none — allowlist mode forwards nothing until you add apps."
                    } else {
                        "Selected: ${notificationForwardingPackages.size} app(s) allowed."
                    }
                NotificationPackageFilterMode.Blocklist ->
                    if (notificationForwardingPackages.isEmpty()) {
                        "Selected: none — blocklist mode forwards all apps except OpenClaw."
                    } else {
                        "Selected: ${notificationForwardingPackages.size} app(s) blocked."
                    }
            }
        }
    val quietHoursCanEnable = notificationForwardingEnabled && quietHoursDraftValid
    val quietHoursDraftDirty =
        notificationForwardingQuietStart != (normalizedQuietStartDraft ?: notificationQuietStartDraft.trim()) ||
            notificationForwardingQuietEnd != (normalizedQuietEndDraft ?: notificationQuietEndDraft.trim())
    val quietHoursSaveEnabled = notificationForwardingEnabled && quietHoursDraftValid && quietHoursDraftDirty

    val listState = rememberLazyListState()
    val deviceModel =
        remember {
            listOfNotNull(Build.MANUFACTURER, Build.MODEL)
                .joinToString(" ")
                .trim()
                .ifEmpty { "Android" }
        }
    val appVersion =
        remember {
            val versionName = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
            if (BuildConfig.DEBUG && !versionName.contains("dev", ignoreCase = true)) {
                "$versionName-dev"
            } else {
                versionName
            }
        }
    var assistantRoleAvailable by remember(context) { mutableStateOf(isAssistantRoleAvailable(context)) }
    var assistantRoleHeld by remember(context) { mutableStateOf(isAssistantRoleHeld(context)) }
    val listItemColors =
        ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = mobileText,
            supportingColor = mobileTextSecondary,
            trailingIconColor = mobileTextSecondary,
            leadingIconColor = mobileTextSecondary,
        )

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val cameraOk = perms[Manifest.permission.CAMERA] == true
            viewModel.setCameraEnabled(cameraOk)
        }

    var pendingLocationRequest by remember { mutableStateOf(false) }
    var pendingPreciseToggle by remember { mutableStateOf(false) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val fineOk = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseOk = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val granted = fineOk || coarseOk

            if (pendingPreciseToggle) {
                pendingPreciseToggle = false
                viewModel.setLocationPreciseEnabled(fineOk)
                return@rememberLauncherForActivityResult
            }

            if (pendingLocationRequest) {
                pendingLocationRequest = false
                viewModel.setLocationMode(if (granted) LocationMode.WhileUsing else LocationMode.Off)
            }
        }

    var micPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val audioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micPermissionGranted = granted
        }

    val smsPermissionAvailable =
        remember {
            BuildConfig.OPENCLAW_ENABLE_SMS &&
                context.packageManager?.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) == true
        }
    val callLogPermissionAvailable = remember { BuildConfig.OPENCLAW_ENABLE_CALL_LOG }
    val photosPermission =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    val motionPermissionRequired = true
    val motionAvailable = remember(context) { hasMotionCapabilities(context) }

    var notificationsPermissionGranted by
        remember {
            mutableStateOf(hasNotificationsPermission(context))
        }
    val notificationsPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsPermissionGranted = granted
        }

    var notificationListenerEnabled by
        remember {
            mutableStateOf(isNotificationListenerEnabled(context))
        }
    val notificationForwardingAvailable = notificationForwardingEnabled && notificationListenerEnabled
    val notificationForwardingControlsAlpha = if (notificationForwardingAvailable) 1f else 0.6f

    var notificationPickerExpanded by remember { mutableStateOf(false) }
    var notificationAppSearch by remember { mutableStateOf("") }
    var notificationShowSystemApps by remember { mutableStateOf(false) }
    var installedNotificationApps by
        remember(context, notificationForwardingPackages) {
            mutableStateOf(queryInstalledApps(context, notificationForwardingPackages))
        }

    var photosPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, photosPermission) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val photosPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            photosPermissionGranted = granted
        }

    var contactsPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val readOk = perms[Manifest.permission.READ_CONTACTS] == true
            val writeOk = perms[Manifest.permission.WRITE_CONTACTS] == true
            contactsPermissionGranted = readOk && writeOk
        }

    var calendarPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val calendarPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val readOk = perms[Manifest.permission.READ_CALENDAR] == true
            val writeOk = perms[Manifest.permission.WRITE_CALENDAR] == true
            calendarPermissionGranted = readOk && writeOk
        }

    var callLogPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val callLogPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            callLogPermissionGranted = granted
        }

    var motionPermissionGranted by
        remember {
            mutableStateOf(
                !motionPermissionRequired ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val motionPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            motionPermissionGranted = granted
        }

    var smsPermissionGranted by
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    val smsPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            smsPermissionGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
            viewModel.refreshGatewayConnection()
        }

    val assistantRoleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            assistantRoleAvailable = isAssistantRoleAvailable(context)
            assistantRoleHeld = isAssistantRoleHeld(context)
        }

    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    micPermissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    notificationsPermissionGranted = hasNotificationsPermission(context)
                    notificationListenerEnabled = isNotificationListenerEnabled(context)
                    installedNotificationApps = queryInstalledApps(context, notificationForwardingPackages)
                    photosPermissionGranted =
                        ContextCompat.checkSelfPermission(context, photosPermission) ==
                        PackageManager.PERMISSION_GRANTED
                    contactsPermissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED
                    calendarPermissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED
                    callLogPermissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                        PackageManager.PERMISSION_GRANTED
                    motionPermissionGranted =
                        !motionPermissionRequired ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                        PackageManager.PERMISSION_GRANTED
                    smsPermissionGranted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                        PackageManager.PERMISSION_GRANTED
                    assistantRoleAvailable = isAssistantRoleAvailable(context)
                    assistantRoleHeld = isAssistantRoleHeld(context)
                    viewModel.refreshAirVisionUsb()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun setCameraEnabledChecked(checked: Boolean) {
        if (!checked) {
            viewModel.setCameraEnabled(false)
            return
        }

        val cameraOk =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (cameraOk) {
            viewModel.setCameraEnabled(true)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    fun requestLocationPermissions() {
        val fineOk =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarseOk =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (fineOk || coarseOk) {
            viewModel.setLocationMode(LocationMode.WhileUsing)
        } else {
            pendingLocationRequest = true
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    fun setPreciseLocationChecked(checked: Boolean) {
        if (!checked) {
            viewModel.setLocationPreciseEnabled(false)
            return
        }
        val fineOk =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (fineOk) {
            viewModel.setLocationPreciseEnabled(true)
        } else {
            pendingPreciseToggle = true
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    val normalizedAppSearch = notificationAppSearch.trim().lowercase()
    val filteredNotificationApps =
        remember(installedNotificationApps, normalizedAppSearch, notificationShowSystemApps) {
            installedNotificationApps
                .asSequence()
                .filter { app -> notificationShowSystemApps || !app.isSystemApp }
                .filter { app ->
                    normalizedAppSearch.isEmpty() ||
                        app.label.lowercase().contains(normalizedAppSearch) ||
                        app.packageName.lowercase().contains(normalizedAppSearch)
                }.toList()
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(mobileBackgroundGradient),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // -- Gateway --
            item {
                Text(
                    "GATEWAY",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                ConnectTabScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                    scrollable = false,
                )
            }

            // ── Node ──
            item {
                Text(
                    "DEVICE",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = viewModel::setDisplayName,
                        label = { Text("Name", style = mobileCaption1, color = mobileTextSecondary) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = mobileBody.copy(color = mobileText),
                        colors = settingsTextFieldColors(),
                    )
                    HorizontalDivider(color = mobileBorder)
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("$deviceModel · $appVersion", style = mobileCallout, color = mobileTextSecondary)
                        Text(
                            instanceId.take(8) + "…",
                            style = mobileCaption1.copy(fontFamily = FontFamily.Monospace),
                            color = mobileTextTertiary,
                        )
                    }
                    if (assistantRoleAvailable) {
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text("Default Assistant", style = mobileHeadline) },
                            supportingContent = {
                                Text(
                                    if (assistantRoleHeld) {
                                        "OpenClaw is registered as the device assistant."
                                    } else {
                                        "Let Android launch OpenClaw from the assistant gesture. Google Assistant App Actions still work separately."
                                    },
                                    style = mobileCallout,
                                )
                            },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        assistantRoleLauncher.launch(
                                            context
                                                .getSystemService(RoleManager::class.java)
                                                .createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                                        )
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(
                                        if (assistantRoleHeld) "Manage" else "Enable",
                                        style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // -- AirVision M1 --
            item {
                Text(
                    "AIRVISION M1",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Firmware Link", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionUsbStatusText(
                                    statusText = airVisionUsbState.statusText,
                                    deviceLabel = airVisionUsbState.deviceLabel,
                                    vendorProduct = airVisionUsbState.vendorProduct,
                                    hidControlInterface = airVisionUsbState.hidControlInterface,
                                    audioInterface = airVisionUsbState.audioInterface,
                                    inputInterface = airVisionUsbState.inputInterface,
                                ),
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (airVisionUsbState.connected && !airVisionUsbState.permissionGranted) {
                                        viewModel.requestAirVisionUsbPermission()
                                    } else {
                                        viewModel.refreshAirVisionUsb()
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (airVisionUsbState.connected && !airVisionUsbState.permissionGranted) {
                                        "Grant"
                                    } else {
                                        "Refresh"
                                    },
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Viewing Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Current: ${airVisionDisplaySettings.viewMode.label}. Each mode keeps its own saved HUD profile.",
                                style = mobileCallout,
                            )
                        },
                    )
                    AirVisionViewMode.entries.forEach { mode ->
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text(mode.label, style = mobileHeadline) },
                            supportingContent = {
                                Text(airVisionViewModeDescription(mode), style = mobileCallout)
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = airVisionDisplaySettings.viewMode == mode,
                                    onClick = { viewModel.setAirVisionViewMode(mode) },
                                )
                            },
                        )
                    }
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "HUD Placement",
                        currentLabel = airVisionDisplaySettings.hudPlacement.label,
                        supportingText = "Positions the presentation content inside the M1 view.",
                        options = AirVisionHudPlacement.entries.toList(),
                        selected = airVisionDisplaySettings.hudPlacement,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionHudPlacementDescription,
                        onSelected = viewModel::setAirVisionHudPlacement,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "Safe Area",
                        valueText = "${airVisionDisplaySettings.safeAreaPercent}%",
                        supportingText = "Adds extra edge padding for DeX shelves, projector alignment, and walking comfort.",
                        value = airVisionDisplaySettings.safeAreaPercent.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_SAFE_AREA_PERCENT
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_SAFE_AREA_PERCENT.toFloat()),
                        onValueChange = { viewModel.setAirVisionSafeAreaPercent(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "Brightness",
                        valueText = "${airVisionDisplaySettings.brightnessPercent}%",
                        supportingText = "Software HUD dimming while firmware brightness remains controlled by the M1 touch bar.",
                        value = airVisionDisplaySettings.brightnessPercent.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_BRIGHTNESS_PERCENT
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_BRIGHTNESS_PERCENT.toFloat()),
                        onValueChange = { viewModel.setAirVisionBrightnessPercent(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "Virtual Distance",
                        valueText = "${airVisionDisplaySettings.distanceCm} cm",
                        supportingText = "Scales the HUD content to mimic closer or farther projection distance.",
                        value = airVisionDisplaySettings.distanceCm.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_DISTANCE_CM
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_DISTANCE_CM.toFloat()),
                        onValueChange = { viewModel.setAirVisionDistanceCm(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "IPD",
                        valueText = "${airVisionDisplaySettings.ipdMm} mm",
                        supportingText = "Stored for calibration. Firmware-level IPD apply still needs ASUS HID protocol support.",
                        value = airVisionDisplaySettings.ipdMm.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_IPD_MM
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_IPD_MM.toFloat()),
                        onValueChange = { viewModel.setAirVisionIpdMm(it.roundToInt()) },
                    )
                }
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Splendid Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Current: ${airVisionDisplaySettings.splendidMode.label}. Eye Care adds warm HUD filtering.",
                                style = mobileCallout,
                            )
                        },
                    )
                    AirVisionSplendidMode.entries.forEach { mode ->
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text(mode.label, style = mobileHeadline) },
                            supportingContent = {
                                Text(airVisionSplendidModeDescription(mode), style = mobileCallout)
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = airVisionDisplaySettings.splendidMode == mode,
                                    onClick = { viewModel.setAirVisionSplendidMode(mode) },
                                )
                            },
                        )
                    }
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "Blue Light Filter",
                        valueText = "${airVisionDisplaySettings.blueLightFilterPercent}%",
                        supportingText = "Applies a warm overlay in the Android HUD presentation.",
                        value = airVisionDisplaySettings.blueLightFilterPercent.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_BLUE_LIGHT_FILTER_PERCENT
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_BLUE_LIGHT_FILTER_PERCENT.toFloat()),
                        onValueChange = { viewModel.setAirVisionBlueLightFilterPercent(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Motion Sync", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Stored with the AirVision profile while Android protocol support is being hardened.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = airVisionDisplaySettings.motionSyncEnabled,
                                onCheckedChange = viewModel::setAirVisionMotionSyncEnabled,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Light Load Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text("Stores the Windows-style preference for lower overhead HUD operation.", style = mobileCallout)
                        },
                        trailingContent = {
                            Switch(
                                checked = airVisionDisplaySettings.lightLoadModeEnabled,
                                onCheckedChange = viewModel::setAirVisionLightLoadModeEnabled,
                            )
                        },
                    )
                }
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Gesture & Hotkey Settings", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Configures HUD touch gestures and M1 key handling while the HUD is focused.",
                                style = mobileCallout,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "Single Tap",
                        currentLabel = airVisionHudControls.singleTapAction.label,
                        supportingText = "Default clears the visible notification.",
                        options = AirVisionHudTouchAction.entries.toList(),
                        selected = airVisionHudControls.singleTapAction,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionTouchActionDescription,
                        onSelected = viewModel::setAirVisionHudSingleTapAction,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "Double Tap",
                        currentLabel = airVisionHudControls.doubleTapAction.label,
                        supportingText = "Default toggles microphone capture.",
                        options = AirVisionHudDoubleTapAction.entries.toList(),
                        selected = airVisionHudControls.doubleTapAction,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionDoubleTapActionDescription,
                        onSelected = viewModel::setAirVisionHudDoubleTapAction,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "Swipe",
                        currentLabel = airVisionHudControls.swipeAction.label,
                        supportingText = "Controls drag gestures on the HUD surface.",
                        options = AirVisionHudSwipeAction.entries.toList(),
                        selected = airVisionHudControls.swipeAction,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionSwipeActionDescription,
                        onSelected = viewModel::setAirVisionHudSwipeAction,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "M1 Brightness Keys",
                        currentLabel = airVisionHudControls.brightnessKeyAction.label,
                        supportingText = "Scroll chat consumes M1 brightness key events while HUD is focused.",
                        options = AirVisionHudKeyAction.entries.toList(),
                        selected = airVisionHudControls.brightnessKeyAction,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionBrightnessKeyActionDescription,
                        onSelected = viewModel::setAirVisionHudBrightnessKeyAction,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "M1 Media/Tap Key",
                        currentLabel = airVisionHudControls.mediaKeyAction.label,
                        supportingText = "Controls media-key handling from the M1 touch hardware.",
                        options = AirVisionHudMediaKeyAction.entries.toList(),
                        selected = airVisionHudControls.mediaKeyAction,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionMediaKeyActionDescription,
                        onSelected = viewModel::setAirVisionHudMediaKeyAction,
                    )
                }
            }
            item {
                Text(
                    "Android applies HUD brightness, Eye Care filtering, virtual distance, and per-mode profile slots now. Hardware IPD, true Splendid panel presets, and multi-screen desktop layouts require ASUS AirVision HID report support after the firmware link is verified.",
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            }

            // ── Media ──
            item {
                Text(
                    "MEDIA",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Microphone", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                if (micPermissionGranted) "Granted" else "Required for voice transcription.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (micPermissionGranted) {
                                        openAppSettings(context)
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (micPermissionGranted) "Manage" else "Grant",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Camera", style = mobileHeadline) },
                        supportingContent = { Text("Photos and video clips (foreground only).", style = mobileCallout) },
                        trailingContent = { Switch(checked = cameraEnabled, onCheckedChange = ::setCameraEnabledChecked) },
                    )
                }
            }

            // ── Notifications & Messaging ──
            item {
                Text(
                    "NOTIFICATIONS",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("System Notifications", style = mobileHeadline) },
                        supportingContent = {
                            Text("Alerts and foreground service.", style = mobileCallout)
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (notificationsPermissionGranted || Build.VERSION.SDK_INT < 33) {
                                        openAppSettings(context)
                                    } else {
                                        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (notificationsPermissionGranted) "Manage" else "Grant",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Notification Listener Access", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Required for `notifications.list`, `notifications.actions`, and forwarded notification events.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = { openNotificationListenerSettings(context) },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (notificationListenerEnabled) "Manage" else "Enable",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    if (smsPermissionAvailable) {
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text("SMS", style = mobileHeadline) },
                            supportingContent = {
                                Text("Send and search SMS from this device.", style = mobileCallout)
                            },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        if (smsPermissionGranted) {
                                            openAppSettings(context)
                                        } else {
                                            smsPermissionLauncher.launch(
                                                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS),
                                            )
                                        }
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(
                                        if (smsPermissionGranted) {
                                            "Manage"
                                        } else {
                                            "Grant"
                                        },
                                        style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item {
                ListItem(
                    modifier = Modifier.settingsRowModifier(),
                    colors = listItemColors,
                    headlineContent = { Text("Forward Notification Events", style = mobileHeadline) },
                    supportingContent = {
                        Text(
                            if (notificationListenerEnabled) {
                                "Forward listener events into gateway node events. Off by default until you enable it."
                            } else {
                                "Notification listener access is off, so no notification events can be forwarded yet."
                            },
                            style = mobileCallout,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = notificationForwardingEnabled,
                            onCheckedChange = viewModel::setNotificationForwardingEnabled,
                            enabled = notificationListenerEnabled,
                        )
                    },
                )
            }
            item {
                Text(
                    if (notificationListenerEnabled) {
                        "Forwarding is available when enabled below."
                    } else {
                        "Forwarding controls stay disabled until Notification Listener Access is enabled in system Settings."
                    },
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            }
            item {
                Column(
                    modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Package Filter: Allowlist", style = mobileHeadline) },
                        supportingContent = {
                            Text("Only listed package IDs are forwarded.", style = mobileCallout)
                        },
                        trailingContent = {
                            RadioButton(
                                selected = notificationForwardingMode == NotificationPackageFilterMode.Allowlist,
                                onClick = {
                                    viewModel.setNotificationForwardingMode(NotificationPackageFilterMode.Allowlist)
                                },
                                enabled = notificationForwardingAvailable,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Package Filter: Blocklist", style = mobileHeadline) },
                        supportingContent = {
                            Text("All packages except listed IDs are forwarded.", style = mobileCallout)
                        },
                        trailingContent = {
                            RadioButton(
                                selected = notificationForwardingMode == NotificationPackageFilterMode.Blocklist,
                                onClick = {
                                    viewModel.setNotificationForwardingMode(NotificationPackageFilterMode.Blocklist)
                                },
                                enabled = notificationForwardingAvailable,
                            )
                        },
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { notificationPickerExpanded = !notificationPickerExpanded },
                        enabled = notificationForwardingAvailable,
                        colors = settingsPrimaryButtonColors(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (notificationPickerExpanded) "Close App Picker" else "Open App Picker",
                            style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            item {
                Text(
                    selectedPackagesSummary,
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            }
            if (notificationPickerExpanded) {
                item {
                    OutlinedTextField(
                        value = notificationAppSearch,
                        onValueChange = { notificationAppSearch = it },
                        label = {
                            Text("Search apps", style = mobileCaption1, color = mobileTextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = mobileBody.copy(color = mobileText),
                        colors = settingsTextFieldColors(),
                        enabled = notificationForwardingAvailable,
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
                        colors = listItemColors,
                        headlineContent = { Text("Show System Apps", style = mobileHeadline) },
                        supportingContent = {
                            Text("Include Android/system packages in results.", style = mobileCallout)
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationShowSystemApps,
                                onCheckedChange = { notificationShowSystemApps = it },
                                enabled = notificationForwardingAvailable,
                            )
                        },
                    )
                }
                items(filteredNotificationApps, key = { it.packageName }) { app ->
                    ListItem(
                        modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
                        colors = listItemColors,
                        headlineContent = { Text(app.label, style = mobileHeadline) },
                        supportingContent = { Text(app.packageName, style = mobileCallout) },
                        trailingContent = {
                            Switch(
                                checked = notificationForwardingPackages.contains(app.packageName),
                                onCheckedChange = { checked ->
                                    val next = notificationForwardingPackages.toMutableSet()
                                    if (checked) {
                                        next.add(app.packageName)
                                    } else {
                                        next.remove(app.packageName)
                                    }
                                    viewModel.setNotificationForwardingPackagesCsv(next.sorted().joinToString(","))
                                },
                                enabled = notificationForwardingAvailable,
                            )
                        },
                    )
                }
            }
            item {
                ListItem(
                    modifier = Modifier.settingsRowModifier().alpha(notificationForwardingControlsAlpha),
                    colors = listItemColors,
                    headlineContent = { Text("Quiet Hours", style = mobileHeadline) },
                    supportingContent = {
                        Text("Suppress forwarding during a local time window.", style = mobileCallout)
                    },
                    trailingContent = {
                        Switch(
                            checked = notificationForwardingQuietHoursEnabled,
                            onCheckedChange = {
                                if (!quietHoursCanEnable && it) return@Switch
                                viewModel.setNotificationForwardingQuietHours(
                                    enabled = it,
                                    start = notificationQuietStartDraft,
                                    end = notificationQuietEndDraft,
                                )
                            },
                            enabled = if (notificationForwardingQuietHoursEnabled) notificationForwardingAvailable else quietHoursCanEnable,
                        )
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = notificationQuietStartDraft,
                    onValueChange = { notificationQuietStartDraft = it },
                    label = { Text("Quiet Start (HH:mm)", style = mobileCaption1, color = mobileTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = mobileBody.copy(color = mobileText),
                    colors = settingsTextFieldColors(),
                    enabled = notificationForwardingAvailable,
                    isError = notificationForwardingAvailable && normalizedQuietStartDraft == null,
                    supportingText = {
                        if (notificationForwardingAvailable && normalizedQuietStartDraft == null) {
                            Text("Use 24-hour HH:mm format, for example 22:00.", style = mobileCaption1, color = mobileDanger)
                        }
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = notificationQuietEndDraft,
                    onValueChange = { notificationQuietEndDraft = it },
                    label = { Text("Quiet End (HH:mm)", style = mobileCaption1, color = mobileTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = mobileBody.copy(color = mobileText),
                    colors = settingsTextFieldColors(),
                    enabled = notificationForwardingAvailable,
                    isError = notificationForwardingAvailable && normalizedQuietEndDraft == null,
                    supportingText = {
                        if (notificationForwardingAvailable && normalizedQuietEndDraft == null) {
                            Text("Use 24-hour HH:mm format, for example 07:00.", style = mobileCaption1, color = mobileDanger)
                        }
                    },
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            viewModel.setNotificationForwardingQuietHours(
                                enabled = notificationForwardingQuietHoursEnabled,
                                start = notificationQuietStartDraft,
                                end = notificationQuietEndDraft,
                            )
                        },
                        enabled = quietHoursSaveEnabled,
                        colors = settingsPrimaryButtonColors(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Save Quiet Hours", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = notificationRateDraft,
                    onValueChange = { notificationRateDraft = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Events / Minute", style = mobileCaption1, color = mobileTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = mobileBody.copy(color = mobileText),
                    colors = settingsTextFieldColors(),
                    enabled = notificationForwardingAvailable,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            val parsed = notificationRateDraft.toIntOrNull() ?: notificationForwardingMaxEventsPerMinute
                            viewModel.setNotificationForwardingMaxEventsPerMinute(parsed)
                        },
                        enabled = notificationForwardingAvailable,
                        colors = settingsPrimaryButtonColors(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Save Rate", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = notificationSessionKeyDraft,
                    onValueChange = { notificationSessionKeyDraft = it },
                    label = {
                        Text(
                            "Route Session Key (optional)",
                            style = mobileCaption1,
                            color = mobileTextSecondary,
                        )
                    },
                    placeholder = {
                        Text(
                            "Blank keeps notification events on this device's default notification route. Set a key only to pin forwarding into a different session.",
                            style = mobileCaption1,
                            color = mobileTextSecondary,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = mobileBody.copy(color = mobileText),
                    colors = settingsTextFieldColors(),
                    enabled = notificationForwardingAvailable,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            viewModel.setNotificationForwardingSessionKey(notificationSessionKeyDraft.trim().ifEmpty { null })
                        },
                        enabled = notificationForwardingAvailable,
                        colors = settingsPrimaryButtonColors(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Save Session Route", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            item { HorizontalDivider(color = mobileBorder) }

            // ── Data Access ──
            item {
                Text(
                    "DATA ACCESS",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Photos", style = mobileHeadline) },
                        supportingContent = { Text("Access recent photos.", style = mobileCallout) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (photosPermissionGranted) {
                                        openAppSettings(context)
                                    } else {
                                        photosPermissionLauncher.launch(photosPermission)
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (photosPermissionGranted) "Manage" else "Grant",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Contacts", style = mobileHeadline) },
                        supportingContent = { Text("Search and add contacts.", style = mobileCallout) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (contactsPermissionGranted) {
                                        openAppSettings(context)
                                    } else {
                                        contactsPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                                        )
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (contactsPermissionGranted) "Manage" else "Grant",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Calendar", style = mobileHeadline) },
                        supportingContent = { Text("Read and create events.", style = mobileCallout) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (calendarPermissionGranted) {
                                        openAppSettings(context)
                                    } else {
                                        calendarPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                                        )
                                    }
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    if (calendarPermissionGranted) "Manage" else "Grant",
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        },
                    )
                    if (callLogPermissionAvailable) {
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text("Call Log", style = mobileHeadline) },
                            supportingContent = { Text("Search recent call history.", style = mobileCallout) },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        if (callLogPermissionGranted) {
                                            openAppSettings(context)
                                        } else {
                                            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                                        }
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(
                                        if (callLogPermissionGranted) "Manage" else "Grant",
                                        style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            },
                        )
                    }
                    if (motionAvailable) {
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = { Text("Motion", style = mobileHeadline) },
                            supportingContent = { Text("Track steps and activity.", style = mobileCallout) },
                            trailingContent = {
                                val motionButtonLabel =
                                    when {
                                        !motionPermissionRequired -> "Manage"
                                        motionPermissionGranted -> "Manage"
                                        else -> "Grant"
                                    }
                                Button(
                                    onClick = {
                                        if (!motionPermissionRequired || motionPermissionGranted) {
                                            openAppSettings(context)
                                        } else {
                                            motionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                        }
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(motionButtonLabel, style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                            },
                        )
                    }
                }
            }

            // ── Location ──
            item {
                Text(
                    "LOCATION",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Off", style = mobileHeadline) },
                        supportingContent = { Text("Disable location sharing.", style = mobileCallout) },
                        trailingContent = {
                            RadioButton(
                                selected = locationMode == LocationMode.Off,
                                onClick = { viewModel.setLocationMode(LocationMode.Off) },
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("While Using", style = mobileHeadline) },
                        supportingContent = { Text("Only while OpenClaw is open.", style = mobileCallout) },
                        trailingContent = {
                            RadioButton(
                                selected = locationMode == LocationMode.WhileUsing,
                                onClick = { requestLocationPermissions() },
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Precise Location", style = mobileHeadline) },
                        supportingContent = { Text("Use precise GPS when available.", style = mobileCallout) },
                        trailingContent = {
                            Switch(
                                checked = locationPreciseEnabled,
                                onCheckedChange = ::setPreciseLocationChecked,
                                enabled = locationMode != LocationMode.Off,
                            )
                        },
                    )
                }
            }

            // ── Preferences ──
            item {
                Text(
                    "PREFERENCES",
                    style = mobileCaption1.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = mobileAccent,
                )
            }
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Prevent Sleep", style = mobileHeadline) },
                        supportingContent = { Text("Keep screen awake while open.", style = mobileCallout) },
                        trailingContent = { Switch(checked = preventSleep, onCheckedChange = viewModel::setPreventSleep) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Debug Canvas", style = mobileHeadline) },
                        supportingContent = { Text("Show status overlay on canvas.", style = mobileCallout) },
                        trailingContent = {
                            Switch(
                                checked = canvasDebugStatusEnabled,
                                onCheckedChange = viewModel::setCanvasDebugStatusEnabled,
                            )
                        },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun <T> AirVisionOptionGroup(
    title: String,
    currentLabel: String,
    supportingText: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    optionDescription: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = mobileText,
                    supportingColor = mobileTextSecondary,
                    trailingIconColor = mobileTextSecondary,
                    leadingIconColor = mobileTextSecondary,
                ),
            headlineContent = { Text(title, style = mobileHeadline) },
            supportingContent = {
                Text("Current: $currentLabel. $supportingText", style = mobileCallout)
            },
        )
        options.forEach { option ->
            HorizontalDivider(color = mobileBorder)
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = mobileText,
                        supportingColor = mobileTextSecondary,
                        trailingIconColor = mobileTextSecondary,
                        leadingIconColor = mobileTextSecondary,
                    ),
                headlineContent = { Text(optionLabel(option), style = mobileHeadline) },
                supportingContent = {
                    Text(optionDescription(option), style = mobileCallout)
                },
                trailingContent = {
                    RadioButton(
                        selected = selected == option,
                        onClick = { onSelected(option) },
                    )
                },
            )
        }
    }
}

@Composable
private fun AirVisionSliderRow(
    label: String,
    valueText: String,
    supportingText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = mobileHeadline, color = mobileText)
            Text(valueText, style = mobileCallout.copy(fontWeight = FontWeight.Bold), color = mobileAccent)
        }
        Text(supportingText, style = mobileCallout, color = mobileTextSecondary)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

private fun airVisionViewModeDescription(mode: AirVisionViewMode): String =
    when (mode) {
        AirVisionViewMode.Working -> "Balanced text scale for walking and workstation HUD use."
        AirVisionViewMode.Gaming -> "Larger HUD content for glanceable, high-motion sessions."
        AirVisionViewMode.Infinity -> "Smaller, farther-feeling content for less intrusive overlays."
        AirVisionViewMode.Custom1 -> "Compact custom profile."
        AirVisionViewMode.Custom2 -> "Large custom profile."
    }

private fun airVisionSplendidModeDescription(mode: AirVisionSplendidMode): String =
    when (mode) {
        AirVisionSplendidMode.Standard -> "Neutral HUD color."
        AirVisionSplendidMode.Theater -> "Stored Windows-style profile; panel-level color needs HID support."
        AirVisionSplendidMode.Office -> "Stored Windows-style profile for document work."
        AirVisionSplendidMode.Game -> "Stored Windows-style profile for low-latency visual preference."
        AirVisionSplendidMode.EyeCare -> "Adds a warm Android HUD overlay."
    }

private fun airVisionHudPlacementDescription(placement: AirVisionHudPlacement): String =
    when (placement) {
        AirVisionHudPlacement.UpperLeft -> "Keeps glance text high and left for walking HUD use."
        AirVisionHudPlacement.UpperCenter -> "Centers glance text near the top of the M1 display."
        AirVisionHudPlacement.UpperRight -> "Moves content away from left-side app or DeX overlays."
        AirVisionHudPlacement.Center -> "Places content in the central view for focused seated use."
        AirVisionHudPlacement.LowerCenter -> "Drops content lower when top captions or system UI need space."
    }

private fun airVisionTouchActionDescription(action: AirVisionHudTouchAction): String =
    when (action) {
        AirVisionHudTouchAction.None -> "Let the tap pass without a HUD action."
        AirVisionHudTouchAction.DismissNotification -> "Clear the visible notification when it can be dismissed."
        AirVisionHudTouchAction.ToggleMic -> "Toggle microphone capture on each tap."
    }

private fun airVisionDoubleTapActionDescription(action: AirVisionHudDoubleTapAction): String =
    when (action) {
        AirVisionHudDoubleTapAction.None -> "Let double taps pass without a HUD action."
        AirVisionHudDoubleTapAction.ToggleMic -> "Toggle microphone capture on double tap."
        AirVisionHudDoubleTapAction.DismissNotification -> "Clear the visible notification when it can be dismissed."
    }

private fun airVisionSwipeActionDescription(action: AirVisionHudSwipeAction): String =
    when (action) {
        AirVisionHudSwipeAction.None -> "Let Android or device firmware handle the gesture."
        AirVisionHudSwipeAction.ScrollChat -> "Drag up or down on the HUD to scroll chat history."
    }

private fun airVisionBrightnessKeyActionDescription(action: AirVisionHudKeyAction): String =
    when (action) {
        AirVisionHudKeyAction.None -> "Let Android or M1 firmware handle brightness keys."
        AirVisionHudKeyAction.ScrollChat -> "Use brightness key events as chat scroll controls."
    }

private fun airVisionMediaKeyActionDescription(action: AirVisionHudMediaKeyAction): String =
    when (action) {
        AirVisionHudMediaKeyAction.None -> "Let Android handle media key events."
        AirVisionHudMediaKeyAction.DoubleTapToggleMic -> "Toggle the microphone only after a double tap."
    }

private fun airVisionUsbStatusText(
    statusText: String,
    deviceLabel: String?,
    vendorProduct: String?,
    hidControlInterface: Boolean,
    audioInterface: Boolean,
    inputInterface: Boolean,
): String {
    val interfaces =
        listOfNotNull(
            "hid-out".takeIf { hidControlInterface },
            "audio".takeIf { audioInterface },
            "input".takeIf { inputInterface },
        ).joinToString(", ")
    return listOfNotNull(
        statusText,
        deviceLabel?.takeIf { it.isNotBlank() },
        vendorProduct?.takeIf { it.isNotBlank() },
        interfaces.takeIf { it.isNotBlank() }?.let { "interfaces: $it" },
    ).joinToString("\n")
}

data class InstalledApp(
    val label: String,
    val packageName: String,
    val isSystemApp: Boolean,
)

private fun queryInstalledApps(
    context: Context,
    configuredPackages: Set<String>,
): List<InstalledApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

    val launcherPackages =
        packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull {
                it.activityInfo
                    ?.packageName
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }.toMutableSet()

    val recentNotificationPackages =
        DeviceNotificationListenerService
            .recentPackages(context)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    val candidatePackages =
        resolveNotificationCandidatePackages(
            launcherPackages = launcherPackages,
            recentPackages = recentNotificationPackages,
            configuredPackages = configuredPackages,
            appPackageName = context.packageName,
        )

    return candidatePackages
        .asSequence()
        .mapNotNull { packageName ->
            runCatching {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString().trim()
                InstalledApp(
                    label = if (label.isEmpty()) packageName else label,
                    packageName = packageName,
                    isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }.getOrNull()
        }.sortedWith(compareBy<InstalledApp> { it.label.lowercase() }.thenBy { it.packageName })
        .toList()
}

internal fun resolveNotificationCandidatePackages(
    launcherPackages: Set<String>,
    recentPackages: List<String>,
    configuredPackages: Set<String>,
    appPackageName: String,
): Set<String> {
    val blockedPackage = appPackageName.trim()
    return sequenceOf(
        configuredPackages.asSequence(),
        launcherPackages.asSequence(),
        recentPackages.asSequence(),
    ).flatten()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != blockedPackage }
        .toSet()
}

@Composable
private fun settingsTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = mobileSurface,
        unfocusedContainerColor = mobileSurface,
        focusedBorderColor = mobileAccent,
        unfocusedBorderColor = mobileBorder,
        focusedTextColor = mobileText,
        unfocusedTextColor = mobileText,
        cursorColor = mobileAccent,
    )

@Composable
private fun Modifier.settingsRowModifier() =
    this
        .fillMaxWidth()
        .border(width = 1.dp, color = mobileBorder, shape = RoundedCornerShape(14.dp))
        .background(mobileCardSurface, RoundedCornerShape(14.dp))

@Composable
private fun settingsPrimaryButtonColors() =
    ButtonDefaults.buttonColors(
        containerColor = mobileAccent,
        contentColor = Color.White,
        disabledContainerColor = mobileAccent.copy(alpha = 0.45f),
        disabledContentColor = Color.White.copy(alpha = 0.9f),
    )

@Composable
private fun settingsDangerButtonColors() =
    ButtonDefaults.buttonColors(
        containerColor = mobileDanger,
        contentColor = Color.White,
        disabledContainerColor = mobileDanger.copy(alpha = 0.45f),
        disabledContentColor = Color.White.copy(alpha = 0.9f),
    )

private fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    runCatching {
        context.startActivity(intent)
    }.getOrElse {
        openAppSettings(context)
    }
}

private fun hasNotificationsPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private fun isNotificationListenerEnabled(context: Context): Boolean = DeviceNotificationListenerService.isAccessEnabled(context)

private fun hasMotionCapabilities(context: Context): Boolean {
    val sensorManager = context.getSystemService(SensorManager::class.java) ?: return false
    return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
}

private fun isAssistantRoleAvailable(context: Context): Boolean =
    context.getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_ASSISTANT)

private fun isAssistantRoleHeld(context: Context): Boolean =
    context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
