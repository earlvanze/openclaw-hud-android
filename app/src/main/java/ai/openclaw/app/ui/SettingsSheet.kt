package ai.openclaw.app.ui

import ai.openclaw.app.AirVisionAppLanguage
import ai.openclaw.app.AirVisionCompanionParity
import ai.openclaw.app.AirVisionDisplaySettings
import ai.openclaw.app.AirVisionDiagnosticsCompanionParity
import ai.openclaw.app.AirVisionFirmwareCaptureResultsSummary
import ai.openclaw.app.AirVisionFirmwareSyncPlans
import ai.openclaw.app.AirVisionHudDisplayTarget
import ai.openclaw.app.AirVisionHudDoubleTapAction
import ai.openclaw.app.AirVisionHudKeyAction
import ai.openclaw.app.AirVisionHudMediaKeyAction
import ai.openclaw.app.AirVisionHudPlacement
import ai.openclaw.app.AirVisionHudSwipeAction
import ai.openclaw.app.AirVisionHudTouchAction
import ai.openclaw.app.AirVisionProfileBackupPreview
import ai.openclaw.app.AirVisionSplendidMode
import ai.openclaw.app.AirVisionStartupDestination
import ai.openclaw.app.AirVisionViewMode
import ai.openclaw.app.BuildConfig
import ai.openclaw.app.LocationMode
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.NotificationPackageFilterMode
import ai.openclaw.app.PrivacyPolicyText
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
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
    val airVisionAppLanguage by viewModel.airVisionAppLanguage.collectAsState()
    val airVisionStartupDestination by viewModel.airVisionStartupDestination.collectAsState()
    val airVisionHudDisplayTarget by viewModel.airVisionHudDisplayTarget.collectAsState()
    val airVisionHudDisplayRoute by viewModel.airVisionHudDisplayRoute.collectAsState()
    val airVisionCustomProfileLabels by viewModel.airVisionCustomProfileLabels.collectAsState()
    val airVisionPhysicalMainScreenVisible by viewModel.airVisionPhysicalMainScreenVisible.collectAsState()
    val airVisionDemoModeEnabled by viewModel.airVisionDemoModeEnabled.collectAsState()
    val airVisionUsbState by viewModel.airVisionUsbState.collectAsState()
    val airVisionFirmwareCaptureResults by viewModel.airVisionFirmwareCaptureResults.collectAsState()
    val airVisionFirmwareCaptureResultsSummary by viewModel.airVisionFirmwareCaptureResultsSummary.collectAsState()
    val nativeCaptionsEnabled by viewModel.nativeCaptionsEnabled.collectAsState()
    val translationCaptionSourceLanguage by viewModel.translationCaptionSourceLanguage.collectAsState()
    val translationCaptionTargetLanguage by viewModel.translationCaptionTargetLanguage.collectAsState()
    val airVisionFirmwareSyncPlan =
        AirVisionFirmwareSyncPlans.fromSettings(
            settings = airVisionDisplaySettings,
            capabilities = airVisionUsbState.firmwareCapabilities,
            captureResults = airVisionFirmwareCaptureResults,
        )
    val airVisionCompanionParity =
        remember(
            airVisionHudControls,
            nativeCaptionsEnabled,
            translationCaptionSourceLanguage,
            translationCaptionTargetLanguage,
        ) {
            AirVisionCompanionParity.fromState(
                hudControls = airVisionHudControls,
                nativeCaptionsEnabled = nativeCaptionsEnabled,
                translationCaptionSourceLanguage = translationCaptionSourceLanguage,
                translationCaptionTargetLanguage = translationCaptionTargetLanguage,
            )
        }

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
    val appBuild = remember { BuildConfig.VERSION_CODE.toString() }
    var showAirVisionLegalNote by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var pendingAirVisionCaptureResultsImportRaw by remember { mutableStateOf<String?>(null) }
    var pendingAirVisionCaptureResultsImportSummary by remember {
        mutableStateOf<AirVisionFirmwareCaptureResultsSummary?>(null)
    }
    var pendingAirVisionProfileImportRaw by remember { mutableStateOf<String?>(null) }
    var pendingAirVisionProfileImportPreview by remember { mutableStateOf<AirVisionProfileBackupPreview?>(null) }
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

    if (showAirVisionLegalNote) {
        AlertDialog(
            onDismissRequest = { showAirVisionLegalNote = false },
            containerColor = mobileCardSurface,
            title = {
                Text("AirVision Legal", style = mobileTitle2, color = mobileText)
            },
            text = {
                Text(
                    "ASUS displays the AirVision EULA inside the Windows AirVision app. This Android HUD is an OpenClaw companion and does not replace ASUS firmware, warranty, registration, or license terms.",
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAirVisionLegalNote = false }) {
                    Text("Understood", color = mobileAccent)
                }
            },
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            containerColor = mobileCardSurface,
            title = {
                Text(PrivacyPolicyText.TITLE, style = mobileTitle2, color = mobileText)
            },
            text = {
                Text(
                    PrivacyPolicyText.BODY,
                    style = mobileCallout,
                    color = mobileTextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text("Close", color = mobileAccent)
                }
            },
        )
    }

    val captureResultsImportSummary = pendingAirVisionCaptureResultsImportSummary
    val captureResultsImportRaw = pendingAirVisionCaptureResultsImportRaw
    if (captureResultsImportSummary != null && captureResultsImportRaw != null) {
        AlertDialog(
            onDismissRequest = {
                pendingAirVisionCaptureResultsImportSummary = null
                pendingAirVisionCaptureResultsImportRaw = null
            },
            containerColor = mobileCardSurface,
            title = {
                Text("Apply AirVision firmware capture results?", style = mobileTitle2, color = mobileText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("- ${captureResultsImportSummary.displayText}", style = mobileCallout, color = mobileTextSecondary)
                    Text(
                        "- Source evidence: ${captureResultsImportSummary.sourceCompletenessSummary}",
                        style = mobileCallout,
                        color =
                            if (captureResultsImportSummary.sourceCompletenessWarnings.isEmpty()) {
                                mobileTextSecondary
                            } else {
                                mobileWarning
                            },
                    )
                    Text(
                        "- ${captureResultsImportSummary.featureCount} expected AirVision firmware features covered",
                        style = mobileCallout,
                        color = mobileTextSecondary,
                    )
                    Text(
                        "- ${captureResultsImportSummary.validatedFeatureCount} validated, " +
                            "${captureResultsImportSummary.capturedFeatureCount} captured-review, " +
                            "${captureResultsImportSummary.pendingFeatureCount} pending, " +
                            "${captureResultsImportSummary.writeEnabledFeatureCount} protocol-ready, " +
                            "${captureResultsImportSummary.blockedFeatureCount} blocked",
                        style = mobileCallout,
                        color = mobileTextSecondary,
                    )
                    Text(
                        "- Android protocol-ready: ${captureResultsImportSummary.writeEnabledFeatureSummary}",
                        style = mobileCallout,
                        color =
                            if (captureResultsImportSummary.writeEnabledFeatureCount > 0) {
                                mobileWarning
                            } else {
                                mobileTextSecondary
                            },
                    )
                    Text(
                        "- Needs validation: ${captureResultsImportSummary.reviewRequiredFeatureSummary}",
                        style = mobileCallout,
                        color =
                            if (captureResultsImportSummary.capturedFeatureCount > 0) {
                                mobileWarning
                            } else {
                                mobileTextSecondary
                            },
                    )
                    Text(
                        "- Pending capture: ${captureResultsImportSummary.pendingFeatureSummary}",
                        style = mobileCallout,
                        color = mobileTextSecondary,
                    )
                    Text(
                        "- Still blocked: ${captureResultsImportSummary.blockedFeatureSummary}",
                        style = mobileCallout,
                        color = mobileTextSecondary,
                    )
                    Text(
                        "- ${captureResultsImportSummary.safetyPreviewText}",
                        style = mobileCallout,
                        color = mobileWarning,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = viewModel.importAirVisionFirmwareCaptureResults(captureResultsImportRaw)
                        Toast
                            .makeText(
                                context,
                                if (imported) "Validated AirVision capture results" else "AirVision capture results rejected",
                                Toast.LENGTH_SHORT,
                            ).show()
                        pendingAirVisionCaptureResultsImportSummary = null
                        pendingAirVisionCaptureResultsImportRaw = null
                    },
                    colors = settingsPrimaryButtonColors(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Apply", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingAirVisionCaptureResultsImportSummary = null
                        pendingAirVisionCaptureResultsImportRaw = null
                    },
                ) {
                    Text("Cancel", color = mobileAccent)
                }
            },
        )
    }

    val importPreview = pendingAirVisionProfileImportPreview
    val importRaw = pendingAirVisionProfileImportRaw
    if (importPreview != null && importRaw != null) {
        AlertDialog(
            onDismissRequest = {
                pendingAirVisionProfileImportPreview = null
                pendingAirVisionProfileImportRaw = null
            },
            containerColor = mobileCardSurface,
            title = {
                Text(importPreview.title, style = mobileTitle2, color = mobileText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    importPreview.details.forEach { detail ->
                        Text("- $detail", style = mobileCallout, color = mobileTextSecondary)
                    }
                    importPreview.warnings.forEach { warning ->
                        Text("- $warning", style = mobileCallout, color = mobileWarning)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = viewModel.importAirVisionProfileBackup(importRaw)
                        Toast
                            .makeText(
                                context,
                                if (imported) "Imported AirVision profile backup" else "AirVision import failed",
                                Toast.LENGTH_SHORT,
                            ).show()
                        pendingAirVisionProfileImportPreview = null
                        pendingAirVisionProfileImportRaw = null
                    },
                    colors = settingsPrimaryButtonColors(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Apply", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingAirVisionProfileImportPreview = null
                        pendingAirVisionProfileImportRaw = null
                    },
                ) {
                    Text("Cancel", color = mobileAccent)
                }
            },
        )
    }

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

    val airVisionProfileExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportAirVisionProfileBackup())
                } ?: error("Unable to open profile backup file.")
            }.onSuccess {
                Toast.makeText(context, "Exported AirVision profile backup", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("Exported AirVision profile backup")
            }.onFailure { error ->
                Toast.makeText(context, "AirVision export failed", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("AirVision export failed: ${error.message.orEmpty()}")
            }
        }

    val airVisionDiagnosticsExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportAirVisionDiagnosticsSnapshot())
                } ?: error("Unable to open diagnostics file.")
            }.onSuccess {
                Toast.makeText(context, "Exported AirVision diagnostics", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("Exported AirVision diagnostics")
            }.onFailure { error ->
                Toast.makeText(context, "AirVision diagnostics export failed", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("AirVision diagnostics export failed: ${error.message.orEmpty()}")
            }
        }

    val airVisionFirmwareCapturePlanExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportAirVisionFirmwareCapturePlan())
                } ?: error("Unable to open firmware capture plan file.")
            }.onSuccess {
                Toast.makeText(context, "Exported AirVision capture plan", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("Exported AirVision capture plan")
            }.onFailure { error ->
                Toast.makeText(context, "AirVision capture plan export failed", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("AirVision capture plan export failed: ${error.message.orEmpty()}")
            }
        }

    val airVisionFirmwareUpdateHandoffExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportAirVisionFirmwareUpdateHandoff())
                } ?: error("Unable to open firmware update handoff file.")
            }.onSuccess {
                Toast.makeText(context, "Exported AirVision update handoff", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("Exported AirVision update handoff")
            }.onFailure { error ->
                Toast.makeText(context, "AirVision update handoff export failed", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("AirVision update handoff export failed: ${error.message.orEmpty()}")
            }
        }

    val airVisionWindowsProfileHandoffExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(viewModel.exportAirVisionWindowsProfileHandoff())
                } ?: error("Unable to open Windows profile handoff file.")
            }.onSuccess {
                Toast.makeText(context, "Exported AirVision Windows handoff", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("Exported AirVision Windows handoff")
            }.onFailure { error ->
                Toast.makeText(context, "AirVision Windows handoff export failed", Toast.LENGTH_SHORT).show()
                viewModel.showHudTransientMessage("AirVision Windows handoff export failed: ${error.message.orEmpty()}")
            }
        }

    val airVisionFirmwareCaptureResultsImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: error("Unable to open firmware capture results file.")
            }.map { raw ->
                val summary = viewModel.previewAirVisionFirmwareCaptureResults(raw)
                pendingAirVisionCaptureResultsImportRaw = raw
                pendingAirVisionCaptureResultsImportSummary = summary
                viewModel.showHudTransientMessage("Validated AirVision capture results")
            }.getOrElse { error ->
                viewModel.showHudTransientMessage("AirVision capture results import failed: ${error.message.orEmpty()}")
                Toast.makeText(context, "AirVision capture results rejected", Toast.LENGTH_SHORT).show()
            }
        }

    val airVisionProfileImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: error("Unable to open profile backup file.")
            }.map { raw ->
                val preview = viewModel.previewAirVisionProfileBackup(raw)
                pendingAirVisionProfileImportRaw = raw
                pendingAirVisionProfileImportPreview = preview
                viewModel.showHudTransientMessage("Validated AirVision profile backup")
            }.getOrElse { error ->
                viewModel.showHudTransientMessage("AirVision import failed: ${error.message.orEmpty()}")
                Toast.makeText(context, "AirVision import failed", Toast.LENGTH_SHORT).show()
            }
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
                                    deviceInfoText = airVisionUsbState.deviceInfoText,
                                    hidControlInterface = airVisionUsbState.hidControlInterface,
                                    audioInterface = airVisionUsbState.audioInterface,
                                    inputInterface = airVisionUsbState.inputInterface,
                                    firmwareCapabilitySummary = airVisionUsbState.firmwareCapabilities.summary,
                                    firmwareFeatureReadinessSummary = airVisionUsbState.firmwareCapabilities.featureReadinessSummary,
                                    firmwareSyncSummary = airVisionFirmwareSyncPlan.summary,
                                    firmwareWriteGateSummary = airVisionFirmwareSyncPlan.writeGateSummary,
                                    firmwareWriteGateProtocolReadyFeatureLabels =
                                        airVisionFirmwareSyncPlan.writeGate.protocolReadyFeatureLabels,
                                    firmwareWriteGateBlockedFeatureLabels =
                                        airVisionFirmwareSyncPlan.writeGate.blockedFeatureLabels,
                                    firmwareWriteGateLiveTestChecklist =
                                        airVisionFirmwareSyncPlan.writeGate.liveTestChecklist,
                                    firmwareWriteGateNextStep = airVisionFirmwareSyncPlan.writeGate.nextStep,
                                    firmwareApplyPreviewSummary = airVisionFirmwareSyncPlan.applyPreview.summary,
                                    firmwareApplyCommandSummaries =
                                        airVisionFirmwareSyncPlan.applyPreview.commands.map { it.summary },
                                    diagnosticsText = airVisionUsbState.diagnosticsText,
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
                        headlineContent = { Text("Firmware Capture Results", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionFirmwareCaptureResultsSummary
                                    ?: "Import the sanitized Windows/Cyber capture-results JSON to validate ASUS HID evidence before any Android firmware-write enablement.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (airVisionFirmwareCaptureResultsSummary != null) {
                                    TextButton(
                                        onClick = viewModel::clearAirVisionFirmwareCaptureResults,
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Text("Clear", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                                Button(
                                    onClick = {
                                        airVisionFirmwareCaptureResultsImportLauncher.launch(
                                            arrayOf("application/json", "text/*"),
                                        )
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Import", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Firmware Capture Plan", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Save a Windows/Cyber USB capture worksheet with current Android HID report-path status and AirVision probe values.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    airVisionFirmwareCapturePlanExportLauncher.launch(
                                        "openclaw-airvision-m1-firmware-capture-plan.md",
                                    )
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Export", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Firmware Updates", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionFirmwareUpdateStatusText(airVisionUsbState.deviceInfo.firmwareVersion),
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    airVisionFirmwareUpdateHandoffExportLauncher.launch(
                                        "openclaw-airvision-m1-firmware-update-handoff.md",
                                    )
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Export", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Windows App Handoff", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Save current AirVision profile values, derived HUD runtime state, and ASUS Windows app apply steps for Cyber sessions.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    airVisionWindowsProfileHandoffExportLauncher.launch(
                                        "openclaw-airvision-m1-windows-profile-handoff.md",
                                    )
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Export", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Diagnostics Export", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Save current M1 USB readiness, descriptors, active AirVision settings, and derived HUD runtime state for protocol capture.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    airVisionDiagnosticsExportLauncher.launch(
                                        "openclaw-airvision-m1-diagnostics.json",
                                    )
                                },
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Export", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "HUD Display Target",
                        currentLabel = airVisionHudDisplayTarget.label,
                        supportingText = "Chooses which external display receives the Android Presentation HUD.",
                        options = AirVisionHudDisplayTarget.entries.toList(),
                        selected = airVisionHudDisplayTarget,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionHudDisplayTargetDescription,
                        onSelected = viewModel::setAirVisionHudDisplayTarget,
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("HUD Display Route", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionHudDisplayRoute.summaryText(),
                                style = mobileCallout,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Viewing Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Current: ${airVisionCustomProfileLabels.labelFor(airVisionDisplaySettings.viewMode)}. " +
                                    "Each mode keeps its own saved HUD profile.",
                                style = mobileCallout,
                            )
                        },
                    )
                    AirVisionViewMode.entries.forEach { mode ->
                        HorizontalDivider(color = mobileBorder)
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = listItemColors,
                            headlineContent = {
                                Text(airVisionCustomProfileLabels.labelFor(mode), style = mobileHeadline)
                            },
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
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Custom Profile Names", style = mobileHeadline, color = mobileText)
                        OutlinedTextField(
                            value = airVisionCustomProfileLabels.custom1,
                            onValueChange = {
                                viewModel.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom1, it)
                            },
                            label = { Text(AirVisionViewMode.Custom1.label, style = mobileCaption1, color = mobileTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = mobileBody.copy(color = mobileText),
                            colors = settingsTextFieldColors(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = airVisionCustomProfileLabels.custom2,
                            onValueChange = {
                                viewModel.setAirVisionCustomProfileLabel(AirVisionViewMode.Custom2, it)
                            },
                            label = { Text(AirVisionViewMode.Custom2.label, style = mobileCaption1, color = mobileTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = mobileBody.copy(color = mobileText),
                            colors = settingsTextFieldColors(),
                            singleLine = true,
                        )
                        Text(
                            "Copy the active profile settings into a custom slot.",
                            style = mobileCallout,
                            color = mobileTextSecondary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { viewModel.copyActiveAirVisionProfileTo(AirVisionViewMode.Custom1) },
                                modifier = Modifier.weight(1f),
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    "To ${airVisionCustomProfileLabels.labelFor(AirVisionViewMode.Custom1)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            Button(
                                onClick = { viewModel.copyActiveAirVisionProfileTo(AirVisionViewMode.Custom2) },
                                modifier = Modifier.weight(1f),
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    "To ${airVisionCustomProfileLabels.labelFor(AirVisionViewMode.Custom2)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = mobileCallout.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Identify HUD Display", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Shows a temporary HUD 1 marker on the external presentation, mirroring the AirVision identify action.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = viewModel::identifyAirVisionHudDisplay,
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Identify", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Physical Main Screen", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                if (airVisionPhysicalMainScreenVisible) {
                                    "Phone controls stay visible while the M1 presentation is active. Saved with this viewing mode."
                                } else {
                                    "When the M1 presentation is active, this viewing mode switches the phone to a black restore screen."
                                },
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = airVisionPhysicalMainScreenVisible,
                                onCheckedChange = viewModel::setAirVisionPhysicalMainScreenVisible,
                            )
                        },
                    )
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
                        label = "HUD Scale",
                        valueText = "${airVisionDisplaySettings.hudScalePercent}%",
                        supportingText = "Zooms the Android HUD text and controls within this viewing mode.",
                        value = airVisionDisplaySettings.hudScalePercent.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_HUD_SCALE_PERCENT
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_HUD_SCALE_PERCENT.toFloat()),
                        onValueChange = { viewModel.setAirVisionHudScalePercent(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionSliderRow(
                        label = "IPD",
                        valueText =
                            if (airVisionDisplaySettings.ipdAdjustmentEnabled) {
                                "${airVisionDisplaySettings.ipdMm} mm"
                            } else {
                                "${airVisionDisplaySettings.ipdMm} mm locked"
                            },
                        supportingText =
                            if (airVisionDisplaySettings.ipdAdjustmentEnabled) {
                                "Stored for calibration. Firmware-level IPD apply still needs ASUS HID protocol support."
                            } else {
                                "Disabled while Light Load Mode is on, matching the ASUS AirVision app behavior."
                            },
                        value = airVisionDisplaySettings.ipdMm.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_IPD_MM
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_IPD_MM.toFloat()),
                        enabled = airVisionDisplaySettings.ipdAdjustmentEnabled,
                        onValueChange = { viewModel.setAirVisionIpdMm(it.roundToInt()) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Fit & Clarity", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionFitAndClarityText(airVisionDisplaySettings),
                                style = mobileCallout,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Reset Profile", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Restore ${airVisionCustomProfileLabels.labelFor(airVisionDisplaySettings.viewMode)} " +
                                    "placement, safe area, brightness, distance, IPD, color, and performance defaults.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = viewModel::resetActiveAirVisionProfile,
                                colors = settingsPrimaryButtonColors(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Reset", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                            }
                        },
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
                                "Current: ${airVisionDisplaySettings.splendidMode.label}. Non-standard modes add subtle HUD color previews.",
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
                        valueText =
                            if (airVisionDisplaySettings.blueLightFilterAvailable) {
                                "${airVisionDisplaySettings.blueLightFilterPercent}%"
                            } else {
                                "${airVisionDisplaySettings.blueLightFilterPercent}% locked"
                            },
                        supportingText =
                            if (airVisionDisplaySettings.blueLightFilterAvailable) {
                                "Applies a warm overlay in the Android HUD presentation."
                            } else {
                                "Available only in Eye Care mode, matching the ASUS AirVision app."
                            },
                        value = airVisionDisplaySettings.blueLightFilterPercent.toFloat(),
                        valueRange =
                            AirVisionDisplaySettings.MIN_BLUE_LIGHT_FILTER_PERCENT
                                .toFloat()
                                .rangeTo(AirVisionDisplaySettings.MAX_BLUE_LIGHT_FILTER_PERCENT.toFloat()),
                        enabled = airVisionDisplaySettings.blueLightFilterAvailable,
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .alpha(if (airVisionDisplaySettings.threeDModeAvailable) 1f else 0.72f),
                        colors = listItemColors,
                        headlineContent = { Text("3D Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                if (airVisionDisplaySettings.threeDModeAvailable) {
                                    "Stores the SBS 3D preference. Firmware-level apply still needs ASUS HID protocol support."
                                } else {
                                    "Disabled while Light Load Mode is on, matching the ASUS AirVision app behavior."
                                },
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = airVisionDisplaySettings.threeDModeEnabled,
                                enabled = airVisionDisplaySettings.threeDModeAvailable,
                                onCheckedChange = viewModel::setAirVisionThreeDModeEnabled,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Light Load Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Uses a lower-overhead HUD by trimming history and color previews. IPD and 3D are locked while enabled.",
                                style = mobileCallout,
                            )
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
                        supportingText = "Controls brightness key handling while the HUD is focused.",
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
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Windows Spatial & Mirror Controls", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                if (airVisionHudControls.brightnessKeyAction == AirVisionHudKeyAction.AdjustDistance) {
                                    "Distance hotkey parity is mapped to M1 brightness keys. Cursor Follow, Center Cursor, Unity mirror window, and 3DoF remain Windows-only."
                                } else {
                                    "Cursor Follow, Center Cursor, Unity mirror window, and 3DoF remain Windows-only. Use Android or DeX screen sharing outside the HUD when you need a mirror fallback."
                                },
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        openSettingsAction(
                                            context = context,
                                            action = AIR_VISION_CAST_SETTINGS_ACTION,
                                            fallbackAction = Settings.ACTION_DISPLAY_SETTINGS,
                                        )
                                        viewModel.showHudTransientMessage("Opened Android Cast settings")
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Cast", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                                Button(
                                    onClick = {
                                        openSettingsAction(context, Settings.ACTION_DISPLAY_SETTINGS)
                                        viewModel.showHudTransientMessage("Opened Android Display settings")
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Display", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Companion Parity", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                airVisionCompanionParitySettingsText(airVisionCompanionParity),
                                style = mobileCallout,
                            )
                        },
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
            item {
                Column(modifier = Modifier.settingsRowModifier()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("App Preferences", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Windows-style AirVision companion preferences, support links, and release metadata.",
                                style = mobileCallout,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "Startup View",
                        currentLabel = airVisionStartupDestination.label,
                        supportingText = "Selects the default view when OpenClaw HUD launches.",
                        options = AirVisionStartupDestination.entries.toList(),
                        selected = airVisionStartupDestination,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionStartupDestinationDescription,
                        onSelected = viewModel::setAirVisionStartupDestination,
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionOptionGroup(
                        title = "Language",
                        currentLabel = airVisionAppLanguage.label,
                        supportingText = "Applies the preferred AirVision companion language; System follows Android settings.",
                        options = AirVisionAppLanguage.entries.toList(),
                        selected = airVisionAppLanguage,
                        optionLabel = { it.label },
                        optionDescription = ::airVisionAppLanguageDescription,
                        onSelected = viewModel::setAirVisionAppLanguage,
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Demo Mode", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Shows a deterministic AirVision HUD sample for Play review, tutorials, screenshots, and fit checks without a live gateway or live M1. ASUS Windows demo shortcut state stays Windows-only.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = airVisionDemoModeEnabled,
                                onCheckedChange = viewModel::setAirVisionDemoModeEnabled,
                            )
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Profile Backup", style = mobileHeadline) },
                        supportingContent = {
                            Text(
                                "Export or import AirVision M1 tuning profiles, labels, gestures, app preferences, and runtime metadata as JSON.",
                                style = mobileCallout,
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        airVisionProfileExportLauncher.launch(
                                            "openclaw-airvision-m1-profile.json",
                                        )
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text("Export", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                                Button(
                                    onClick = {
                                        airVisionProfileImportLauncher.launch(
                                            arrayOf("application/json", "text/*", "application/octet-stream"),
                                        )
                                    },
                                    colors = settingsPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text("Import", style = mobileCallout.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = listItemColors,
                        headlineContent = { Text("Software Version", style = mobileHeadline) },
                        supportingContent = {
                            Text("OpenClaw HUD $appVersion ($appBuild)", style = mobileCallout)
                        },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionLinkRow(
                        title = "End-User License",
                        supportingText = "Shows the Android companion legal note and ASUS EULA availability.",
                        buttonText = "View",
                        onClick = { showAirVisionLegalNote = true },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionLinkRow(
                        title = "Privacy Policy",
                        supportingText = "Shows how OpenClaw HUD handles microphone, notification, gateway, and local app data.",
                        buttonText = "View",
                        onClick = { showPrivacyPolicy = true },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionLinkRow(
                        title = "FAQ & Tutorials",
                        supportingText = "Open the official ASUS AirVision M1 setup, app, and troubleshooting FAQ.",
                        buttonText = "Open",
                        onClick = { openExternalUrl(context, AIR_VISION_FAQ_URL) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionLinkRow(
                        title = "Product Registration",
                        supportingText = "Open the official ASUS product registration page.",
                        buttonText = "Open",
                        onClick = { openExternalUrl(context, AIR_VISION_PRODUCT_REGISTRATION_URL) },
                    )
                    HorizontalDivider(color = mobileBorder)
                    AirVisionLinkRow(
                        title = "ASUS Support",
                        supportingText = "Open the AirVision M1 support page for warranty, service, and manuals.",
                        buttonText = "Open",
                        onClick = { openExternalUrl(context, AIR_VISION_SUPPORT_URL) },
                    )
                }
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
private fun AirVisionLinkRow(
    title: String,
    supportingText: String,
    buttonText: String,
    onClick: () -> Unit,
) {
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
        supportingContent = { Text(supportingText, style = mobileCallout) },
        trailingContent = {
            Button(
                onClick = onClick,
                colors = settingsPrimaryButtonColors(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(buttonText, style = mobileCallout.copy(fontWeight = FontWeight.Bold))
            }
        },
    )
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
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(if (enabled) 1f else 0.72f),
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
            enabled = enabled,
        )
    }
}

private fun airVisionViewModeDescription(mode: AirVisionViewMode): String =
    when (mode) {
        AirVisionViewMode.Working -> "Balanced text scale for walking and workstation HUD use."
        AirVisionViewMode.Gaming -> "Larger HUD content for glanceable, high-motion sessions."
        AirVisionViewMode.Infinity -> "Smaller, farther-feeling content for less intrusive overlays."
        AirVisionViewMode.Custom1 -> "User-named compact saved profile."
        AirVisionViewMode.Custom2 -> "User-named large saved profile."
    }

private fun airVisionSplendidModeDescription(mode: AirVisionSplendidMode): String =
    when (mode) {
        AirVisionSplendidMode.Standard -> "Neutral HUD color."
        AirVisionSplendidMode.Theater -> "Adds a subtle warm theater preview while panel presets need HID support."
        AirVisionSplendidMode.Office -> "Adds a subtle cool office preview for document work."
        AirVisionSplendidMode.Game -> "Adds a subtle cool game preview for high-motion sessions."
        AirVisionSplendidMode.EyeCare -> "Adds a warm Android HUD overlay and enables blue-light filtering."
    }

private fun airVisionHudPlacementDescription(placement: AirVisionHudPlacement): String =
    when (placement) {
        AirVisionHudPlacement.UpperLeft -> "Keeps glance text high and left for walking HUD use."
        AirVisionHudPlacement.UpperCenter -> "Centers glance text near the top of the M1 display."
        AirVisionHudPlacement.UpperRight -> "Moves content away from left-side app or DeX overlays."
        AirVisionHudPlacement.Center -> "Places content in the central view for focused seated use."
        AirVisionHudPlacement.LowerCenter -> "Drops content lower when top captions or system UI need space."
    }

private fun airVisionHudDisplayTargetDescription(target: AirVisionHudDisplayTarget): String =
    when (target) {
        AirVisionHudDisplayTarget.AirVisionPreferred -> "Prefer displays whose name looks like ASUS AirVision M1."
        AirVisionHudDisplayTarget.LargestExternal -> "Use the largest external display Android exposes."
        AirVisionHudDisplayTarget.FirstExternal -> "Use the lowest-numbered external display."
        AirVisionHudDisplayTarget.LastExternal -> "Use the highest-numbered external display."
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
        AirVisionHudKeyAction.AdjustBrightness -> "Use brightness up and down to step Android HUD brightness."
        AirVisionHudKeyAction.AdjustDistance -> "Use brightness up and down to step virtual distance farther or closer."
    }

private fun airVisionAppLanguageDescription(language: AirVisionAppLanguage): String =
    when (language) {
        AirVisionAppLanguage.System -> "Follow the Android system language."
        else -> "Apply ${language.label} to the AirVision companion app."
    }

private fun airVisionStartupDestinationDescription(destination: AirVisionStartupDestination): String =
    when (destination) {
        AirVisionStartupDestination.Hud -> "Launch directly into the external-display HUD."
        AirVisionStartupDestination.Chat -> "Launch with assistant chat ready above the input bar."
        AirVisionStartupDestination.Voice -> "Launch with microphone and caption controls visible."
        AirVisionStartupDestination.Agents -> "Launch with the agent picker and model controls visible."
        AirVisionStartupDestination.Settings -> "Launch into settings for setup and display tuning."
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
    deviceInfoText: String,
    hidControlInterface: Boolean,
    audioInterface: Boolean,
    inputInterface: Boolean,
    firmwareCapabilitySummary: String,
    firmwareFeatureReadinessSummary: String,
    firmwareSyncSummary: String,
    firmwareWriteGateSummary: String,
    firmwareWriteGateProtocolReadyFeatureLabels: List<String>,
    firmwareWriteGateBlockedFeatureLabels: List<String>,
    firmwareWriteGateLiveTestChecklist: List<String>,
    firmwareWriteGateNextStep: String,
    firmwareApplyPreviewSummary: String,
    firmwareApplyCommandSummaries: List<String>,
    diagnosticsText: String,
): String {
    val interfaces =
        listOfNotNull(
            "hid-out".takeIf { hidControlInterface },
            "audio".takeIf { audioInterface },
            "input".takeIf { inputInterface },
        ).joinToString(", ")
    return listOfNotNull(
        statusText,
        deviceInfoText.takeIf { it.isNotBlank() && (!deviceLabel.isNullOrBlank() || !vendorProduct.isNullOrBlank()) },
        interfaces.takeIf { it.isNotBlank() }?.let { "interfaces: $it" },
        firmwareCapabilitySummary.takeIf { it.isNotBlank() && (!deviceLabel.isNullOrBlank() || !vendorProduct.isNullOrBlank()) },
        firmwareFeatureReadinessSummary.takeIf { it.isNotBlank() && (!deviceLabel.isNullOrBlank() || !vendorProduct.isNullOrBlank()) },
        firmwareSyncSummary.takeIf { it.isNotBlank() },
        firmwareWriteGateSummary.takeIf { it.isNotBlank() },
        firmwareWriteGateProtocolReadyFeatureLabels
            .takeIf { it.isNotEmpty() }
            ?.let { "protocol-ready: ${it.joinToString()}" },
        firmwareWriteGateBlockedFeatureLabels
            .takeIf { it.isNotEmpty() }
            ?.let { "blocked: ${airVisionCompactLabelList(it)}" },
        firmwareWriteGateLiveTestChecklist
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { "live-test: $it" },
        firmwareWriteGateNextStep.takeIf { it.isNotBlank() }?.let { "next: $it" },
        firmwareApplyPreviewSummary.takeIf { it.isNotBlank() }?.let { "apply: $it" },
        firmwareApplyCommandSummaries
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "apply commands: ") { it },
        diagnosticsText.takeIf { it.isNotBlank() && (!deviceLabel.isNullOrBlank() || !vendorProduct.isNullOrBlank()) },
    ).joinToString("\n")
}

private fun airVisionCompactLabelList(labels: List<String>): String {
    val visible = labels.take(4).joinToString()
    val hiddenCount = labels.size - 4
    return if (hiddenCount > 0) "$visible, +$hiddenCount" else visible
}

private fun airVisionFirmwareUpdateStatusText(firmwareVersion: String?): String =
    listOfNotNull(
        "Firmware update checks and installs require the ASUS AirVision Windows app.",
        firmwareVersion
            ?.takeIf { it.isNotBlank() }
            ?.let { "Android-visible version context: $it" }
            ?: "Android-visible firmware version is pending ASUS HID protocol.",
        "Use Windows/Cyber with the M1 connected for ASUS firmware updates; Android firmware writes stay blocked.",
    ).joinToString("\n")

private fun airVisionCompanionParitySettingsText(parity: AirVisionDiagnosticsCompanionParity): String =
    buildList {
        add(parity.summary)
        parity.entries.forEach { entry ->
            add("${entry.feature}: ${airVisionCompanionParityStateLabel(entry.androidState)}")
        }
        add("Live M1 required for hardware proof: ${parity.liveM1RequiredCount}; firmware protocol gated: ${parity.firmwareGatedCount}.")
    }.joinToString("\n")

private fun airVisionCompanionParityStateLabel(state: String): String =
    when (state) {
        "reviewable_offline" -> "offline-reviewable"
        "m1_optional" -> "M1-optional"
        "firmware_gated" -> "firmware-gated"
        "windows_only" -> "Windows-only"
        else -> state
    }

private fun airVisionFitAndClarityText(settings: AirVisionDisplaySettings): String {
    val effectiveHudScalePercent =
        (
            AirVisionDisplaySettings.hudScaleForDistanceCm(settings.distanceCm) *
                AirVisionDisplaySettings.hudScaleMultiplierForViewMode(settings.viewMode) *
                AirVisionDisplaySettings.hudScaleMultiplierForPercent(settings.hudScalePercent) *
                100f
        ).toInt()
    val ipdStatus =
        if (settings.ipdMm.toDouble() in 53.5..74.5) {
            "IPD ${settings.ipdMm} mm is within ASUS documented range."
        } else {
            "IPD ${settings.ipdMm} mm is outside ASUS documented range; verify fit and prescription."
        }
    val threeDStatus =
        if (settings.threeDModeEnabled) {
            "If normal text looks split or blurry, turn 3D Mode off."
        } else {
            "3D Mode is off for normal text clarity."
        }
    return listOf(
        ipdStatus,
        threeDStatus,
        "Effective HUD scale: $effectiveHudScalePercent%. Increase HUD Scale or pull Virtual Distance closer to enlarge text.",
        "Use Android/DeX display scale or browser zoom for content outside the HUD.",
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

private fun openSettingsAction(
    context: Context,
    action: String,
    fallbackAction: String = Settings.ACTION_SETTINGS,
) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        context.startActivity(intent)
    }.getOrElse {
        context.startActivity(Intent(fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openExternalUrl(
    context: Context,
    url: String,
) {
    val intent =
        Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        context.startActivity(intent)
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

private const val AIR_VISION_FAQ_URL = "https://www.asus.com/support/faq/1054069/"
private const val AIR_VISION_PRODUCT_REGISTRATION_URL = "https://account.asus.com/product_reg.aspx"
private const val AIR_VISION_SUPPORT_URL =
    "https://www.asus.com/displays-desktops/glasses/airvision/asus-airvision-m1/helpdesk_knowledge?model2Name=ASUS-AirVision-M1"
private const val AIR_VISION_CAST_SETTINGS_ACTION = "android.settings.CAST_SETTINGS"

private fun isAssistantRoleHeld(context: Context): Boolean =
    context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
