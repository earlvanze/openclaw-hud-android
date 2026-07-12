package ai.openclaw.app

import kotlinx.serialization.Serializable

@Serializable
data class AirVisionDiagnosticsWindowsAppEvidence(
    val appName: String,
    val observedVersion: String,
    val observedBuildTime: String,
    val observedSdkVersion: String,
    val observedHidLibrary: String,
    val settingsDataVersion: Int,
    val sourceSummary: String,
    val privacyBoundary: String,
    val settingMappings: List<AirVisionDiagnosticsWindowsAppSettingMapping>,
    val summary: String,
)

@Serializable
data class AirVisionDiagnosticsWindowsAppSettingMapping(
    val feature: String,
    val windowsSurface: String,
    val observedSettingKeys: List<String>,
    val observedDefault: String,
    val androidMapping: String,
    val captureImplication: String,
)

object AirVisionWindowsAppEvidence {
    const val APP_NAME = "ASUS AirVision"
    const val OBSERVED_VERSION = "1.0.7.1"
    const val OBSERVED_BUILD_TIME = "20250414_112726"
    const val OBSERVED_SDK_VERSION = "1.0.0.1"
    const val OBSERVED_HID_LIBRARY = "hidapi 0.14.0"
    const val SETTINGS_DATA_VERSION = 2

    val mappings: List<AirVisionDiagnosticsWindowsAppSettingMapping> =
        listOf(
            mapping(
                feature = "View Mode",
                windowsSurface = "Mode combo and virtual-screen layout state",
                observedSettingKeys = listOf("ModeComboBox_SelectionChanged", "LayoutMode_User"),
                observedDefault = "layout mode persisted outside the observed Settings.json defaults",
                androidMapping = "AirVision view-mode profile slot",
                captureImplication = "Capture mode transitions together with layout state, not as a standalone Android write.",
            ),
            mapping(
                feature = "Brightness",
                windowsSurface = "ASUS_AirVision_SDK HID brightness API",
                observedSettingKeys = listOf("SET_BRIGHTNESS", "GlassesBrightness", "BrightnessChanged"),
                observedDefault = "not persisted in observed Settings.json; SDK reads panel state",
                androidMapping = "software HUD dimming plus desired panel brightness",
                captureImplication = "Capture SDK SetBrightness writes and matching brightness-change readback.",
            ),
            mapping(
                feature = "Screen distance",
                windowsSurface = "Virtual space distance setting and hotkey",
                observedSettingKeys = listOf("VirtualSpaceDistance", "DistanceHotkey"),
                observedDefault = "VirtualSpaceDistance=255, DistanceHotkey=69,4",
                androidMapping = "virtual HUD distance scale",
                captureImplication = "Capture distance changes at near, middle, and far probe values.",
            ),
            mapping(
                feature = "IPD",
                windowsSurface = "Software IPD setting",
                observedSettingKeys = listOf("SoftwareIPD"),
                observedDefault = "SoftwareIPD=635 in tenths of millimeters",
                androidMapping = "stored IPD calibration and fit guidance",
                captureImplication = "Capture 60 mm, 67 mm, and 72 mm writes and readback before enabling Android firmware apply.",
            ),
            mapping(
                feature = "Splendid / Eye Care",
                windowsSurface = "Display preset and eye-care level settings",
                observedSettingKeys = listOf("DisplaySplendidMode", "EyeCareLevel", "SplendidModeComboBox_SelectionChanged"),
                observedDefault = "DisplaySplendidMode=0, EyeCareLevel=1",
                androidMapping = "HUD color preview and warm Eye Care overlay",
                captureImplication = "Capture preset changes separately from eye-care intensity changes.",
            ),
            mapping(
                feature = "Motion Sync",
                windowsSurface = "Motion-blur prevention toggle",
                observedSettingKeys = listOf("PreventMotionBlur", "MotionSyncToggleButton_Checked"),
                observedDefault = "PreventMotionBlur=true",
                androidMapping = "stored desired state only",
                captureImplication = "Capture off/on toggles and visible smoothness state if exposed by firmware.",
            ),
            mapping(
                feature = "Light Load Mode",
                windowsSurface = "Eco mode toggle",
                observedSettingKeys = listOf("IsEcoMode", "EcoModeToggleButton_Checked"),
                observedDefault = "IsEcoMode=false",
                androidMapping = "low-overhead HUD profile with locked IPD/3D controls",
                captureImplication = "Capture off/on toggles while verifying IPD and 3D controls stay locked as expected.",
            ),
            mapping(
                feature = "3D Mode",
                windowsSurface = "Display 3D mode SDK commands",
                observedSettingKeys = listOf("DISPLAY_3D_60HZ", "DISPLAY_3D_72HZ", "DISPLAY_3D_60HZ_DPMD", "DISPLAY_3D_72HZ_DPMD"),
                observedDefault = "SDK exposes 60 Hz and 72 Hz 3D/DPMD command names",
                androidMapping = "stored 3D desired state, locked off in Light Load Mode",
                captureImplication = "Capture 3D off/on and refresh-mode command differences before Android write support.",
            ),
            mapping(
                feature = "Android HUD layout",
                windowsSurface = "OpenClaw Android Presentation-only layout",
                observedSettingKeys = emptyList(),
                observedDefault = "not an ASUS Windows app setting",
                androidMapping = "HUD scale, placement, safe area, and physical-main-screen visibility",
                captureImplication = "No ASUS HID capture needed; verify on Android/DeX presentation.",
            ),
            mapping(
                feature = "Display routing",
                windowsSurface = "Windows virtual display and Unity process layout",
                observedSettingKeys = listOf("OriginalPrimaryDisplayFrequency", "LastGraphicsCardForPrimaryDisplay"),
                observedDefault = "OriginalPrimaryDisplayFrequency=60",
                androidMapping = "AirVision-preferred external display routing",
                captureImplication = "Use Android diagnostics for DeX/M1 routing; Windows topology is handoff-only.",
            ),
            mapping(
                feature = "Gesture and hotkey settings",
                windowsSurface = "Cursor/distance hotkeys and ASUS-documented touchpad gestures",
                observedSettingKeys =
                    listOf(
                        "CenterCursorHotkey",
                        "DistanceHotkey",
                        "IsCursorAutoFollow",
                        "IsMenuIconAutoFollow",
                        "IsClipCursor",
                    ),
                observedDefault = "CenterCursorHotkey=46,4, DistanceHotkey=69,4, IsCursorAutoFollow=true; ASUS support documents brightness swipe, play/pause tap, instant transparent two-finger tap, 3D hold, and shortcut-menu hold/slide gestures",
                androidMapping = "HUD tap, double-tap, swipe, brightness-key, media-key, and exported Windows gesture catalog",
                captureImplication = "Android key delivery depends on firmware passthrough; brightness swipes, transparent mode, 3D hold, and shortcut-menu gestures may remain panel- or Windows-app-owned.",
            ),
            mapping(
                feature = "Windows spatial/mirror features",
                windowsSurface = "Windows-only cursor, 3DoF, and Unity mirror workflows",
                observedSettingKeys = listOf("IsCursorAutoFollow", "CenterCursorHotkey", "ShowViewer", "RestartUnity"),
                observedDefault = "cursor follow enabled in observed settings; Unity mirror remains Windows-only",
                androidMapping = "Windows-only status with Cast/Display fallback",
                captureImplication = "Do not claim Android parity for Windows virtual cursor, 3DoF, or Unity mirror windows.",
            ),
        )

    val diagnostics: AirVisionDiagnosticsWindowsAppEvidence =
        AirVisionDiagnosticsWindowsAppEvidence(
            appName = APP_NAME,
            observedVersion = OBSERVED_VERSION,
            observedBuildTime = OBSERVED_BUILD_TIME,
            observedSdkVersion = OBSERVED_SDK_VERSION,
            observedHidLibrary = OBSERVED_HID_LIBRARY,
            settingsDataVersion = SETTINGS_DATA_VERSION,
            sourceSummary =
                "Observed from the Cyber-installed ASUS AirVision Windows app ReleaseInfo, SDK strings, and Data/Settings.json.",
            privacyBoundary =
                "Evidence keeps feature names, version/build metadata, and settings keys only; it omits raw HID bytes, serials, and user-specific paths.",
            settingMappings = mappings,
            summary =
                "$APP_NAME $OBSERVED_VERSION build $OBSERVED_BUILD_TIME; " +
                    "${mappings.size} Windows setting mappings documented for Android handoff and capture planning.",
        )

    fun mappingFor(feature: String): AirVisionDiagnosticsWindowsAppSettingMapping? =
        mappings.firstOrNull { it.feature == feature }

    private fun mapping(
        feature: String,
        windowsSurface: String,
        observedSettingKeys: List<String>,
        observedDefault: String,
        androidMapping: String,
        captureImplication: String,
    ): AirVisionDiagnosticsWindowsAppSettingMapping =
        AirVisionDiagnosticsWindowsAppSettingMapping(
            feature = feature,
            windowsSurface = windowsSurface,
            observedSettingKeys = observedSettingKeys,
            observedDefault = observedDefault,
            androidMapping = androidMapping,
            captureImplication = captureImplication,
        )
}
