package ai.openclaw.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirVisionWindowsProfileHandoffTest {
    @Test
    fun renderMarkdown_exportsProfileValuesForWindowsAppWithoutRawSerial() {
        val activeSettings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Custom1).copy(
                brightnessPercent = 91,
                distanceCm = 88,
                hudScalePercent = 123,
                ipdMm = 67,
                splendidMode = AirVisionSplendidMode.EyeCare,
                blueLightFilterPercent = 42,
                motionSyncEnabled = false,
                threeDModeEnabled = true,
                lightLoadModeEnabled = false,
                hudPlacement = AirVisionHudPlacement.UpperCenter,
                safeAreaPercent = 7,
                physicalMainScreenVisible = false,
            )
        val gamingSettings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Gaming).copy(
                ipdMm = 69,
                lightLoadModeEnabled = true,
                threeDModeEnabled = false,
            )
        val markdown =
            AirVisionWindowsProfileHandoffs.renderMarkdown(
                profileBackup =
                    AirVisionProfileBackup(
                        activeViewMode = AirVisionViewMode.Custom1.rawValue,
                        customLabels =
                            AirVisionBackupCustomLabels(
                                custom1 = "Walking HUD",
                                custom2 = "Desk HUD",
                            ),
                        hudControls =
                            AirVisionBackupHudControls(
                                singleTapAction = "dismiss_notification",
                                doubleTapAction = "toggle_mic",
                                swipeAction = "scroll_chat",
                                brightnessKeyAction = "adjust_brightness",
                                mediaKeyAction = "double_tap_toggle_mic",
                            ),
                        appPreferences =
                            AirVisionBackupAppPreferences(
                                language = "system",
                                startupDestination = "hud",
                                hudDisplayTarget = "airvision_preferred",
                                demoModeEnabled = true,
                                speakerEnabled = false,
                                nativeCaptionsEnabled = true,
                                translationCaptionSourceLanguage = "auto",
                                translationCaptionTargetLanguage = "es",
                            ),
                        profiles =
                            listOf(
                                AirVisionProfileBackups.profileFromSettings(
                                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working),
                                ),
                                AirVisionProfileBackups.profileFromSettings(activeSettings),
                                AirVisionProfileBackups.profileFromSettings(gamingSettings),
                            ),
                        runtimeProfiles =
                            listOf(
                                AirVisionProfileBackups.runtimeProfileFromSettings(activeSettings),
                                AirVisionProfileBackups.runtimeProfileFromSettings(gamingSettings),
                            ),
                    ),
                usbState =
                    AirVisionUsbState(
                        connected = true,
                        permissionGranted = true,
                        deviceLabel = "ASUS AirVision M1",
                        vendorProduct = "0x0b05:0x1b3c",
                        deviceInfo =
                            AirVisionUsbDeviceInfo(
                                manufacturerName = "ASUS",
                                productName = "AirVision M1",
                                vendorProduct = "0x0b05:0x1b3c",
                                interfaceCount = 4,
                                serialNumber = "private-device-serial",
                                firmwareVersion = "USB descriptor 1.02",
                            ),
                    ),
            )

        assertTrue(markdown.startsWith("# AirVision M1 Windows App Profile Handoff"))
        assertTrue(markdown.contains("ASUS AirVision Windows app"))
        assertTrue(markdown.contains("## Active Android Profile"))
        assertTrue(markdown.contains("- View Mode: Custom 1"))
        assertTrue(markdown.contains("- Brightness: 91%"))
        assertTrue(markdown.contains("- Screen distance: 88 cm"))
        assertTrue(markdown.contains("- IPD: 67 mm"))
        assertTrue(markdown.contains("- Splendid: Eye Care"))
        assertTrue(markdown.contains("- Blue Light Filter: 42%"))
        assertTrue(markdown.contains("- Motion Sync: off"))
        assertTrue(markdown.contains("- 3D Mode: on"))
        assertTrue(markdown.contains("- Android HUD scale: 123%"))
        assertTrue(markdown.contains("- Android HUD placement: Upper Center"))
        assertTrue(markdown.contains("- Android safe area: 7%"))
        assertTrue(markdown.contains("- Android physical main screen visible: no"))
        assertTrue(
            markdown.contains(
                "- Android runtime summary: effective HUD scale 98%, transcript 8, captions 5, overlays on, dimming on",
            ),
        )
        assertTrue(markdown.contains("- Android runtime controls: IPD available, 3D available, Eye Care available"))
        assertTrue(markdown.contains("## Active Android Runtime"))
        assertTrue(markdown.contains("- Effective HUD scale: 98%"))
        assertTrue(markdown.contains("- Transcript entries: 8"))
        assertTrue(markdown.contains("- Caption entries: 5"))
        assertTrue(markdown.contains("- IPD adjustment available: yes"))
        assertTrue(markdown.contains("- 3D Mode available: yes"))
        assertTrue(markdown.contains("- Eye Care filter available: yes"))
        assertTrue(markdown.contains("- Color preview overlays: on"))
        assertTrue(markdown.contains("- Brightness dimming: on"))
        assertTrue(markdown.contains("- Embedded runtime metadata: current"))
        assertTrue(markdown.contains("## Windows App Apply Matrix"))
        assertTrue(markdown.contains("- View Mode: Windows app target Custom 1; Android effect Android profile slot Walking HUD; live M1 proof required: no; firmware gate: no."))
        assertTrue(markdown.contains("  - Windows surface: Mode combo and virtual-screen layout state"))
        assertTrue(markdown.contains("  - Observed keys: ModeComboBox_SelectionChanged, LayoutMode_User"))
        assertTrue(markdown.contains("- Brightness: Windows app target 91%; Android effect software HUD dimming; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("  - Windows surface: ASUS_AirVision_SDK HID brightness API"))
        assertTrue(markdown.contains("  - Observed keys: SET_BRIGHTNESS, GlassesBrightness, BrightnessChanged"))
        assertTrue(markdown.contains("  - Capture implication: Capture SDK SetBrightness writes and matching brightness-change readback."))
        assertTrue(markdown.contains("- Screen distance: Windows app target 88 cm; Android effect virtual HUD distance scale; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- IPD: Windows app target 67 mm; Android effect stored calibration and fit guidance; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- Splendid / Eye Care: Windows app target Eye Care, blue-light 42%; Android effect HUD color/warmth preview; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- Motion Sync: Windows app target off; Android effect stored desired state only; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- Light Load Mode: Windows app target off; Android effect full HUD history and unlocked IPD/3D controls; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- 3D Mode: Windows app target on; Android effect stored desired state only; live M1 proof required: yes; firmware gate: HID capture pending."))
        assertTrue(markdown.contains("- Android HUD layout: Windows app target none; Android effect HUD scale 123%, Upper Center, safe area 7%, physical main screen no; live M1 proof required: no; firmware gate: no."))
        assertTrue(markdown.contains("  - Windows surface: OpenClaw Android Presentation-only layout"))
        assertTrue(markdown.contains("  - Observed keys: none"))
        assertTrue(markdown.contains("- Display routing: Windows app target none; Android effect AirVision Preferred; live M1 proof required: yes for DeX/M1 topology; firmware gate: no."))
        assertTrue(markdown.contains("- Gesture and hotkey settings: Windows app target none; Android effect single tap Dismiss notification, double tap Toggle mic, vertical swipe Scroll chat, horizontal swipe Browse notifications, brightness key Adjust brightness, media key Double-tap mic; live M1 proof required: yes for firmware-delivered events; firmware gate: no."))
        assertTrue(markdown.contains("  - Observed keys: CenterCursorHotkey, DistanceHotkey, IsCursorAutoFollow, IsMenuIconAutoFollow, IsClipCursor"))
        assertTrue(markdown.contains("- Windows spatial/mirror features: Windows app target Cursor Follow, Center Cursor, 3DoF, or Unity mirror when needed; Android effect reports Windows-only state and offers Cast/Display fallback; live M1 proof required: Windows host; firmware gate: Windows-only."))
        assertTrue(markdown.contains("## Installed Windows App Evidence"))
        assertTrue(markdown.contains("- App: ASUS AirVision 1.0.7.1"))
        assertTrue(markdown.contains("- Build time: 20250414_112726"))
        assertTrue(markdown.contains("- SDK: 1.0.0.1"))
        assertTrue(markdown.contains("- HID library: hidapi 0.14.0"))
        assertTrue(markdown.contains("- Settings data version: 2"))
        assertTrue(markdown.contains("Observed from the Cyber-installed ASUS AirVision Windows app ReleaseInfo, SDK strings, and Data/Settings.json."))
        assertTrue(markdown.contains("it omits raw HID bytes, serials, and user-specific paths"))
        assertTrue(
            markdown.contains(
                "- Screen distance: Virtual space distance setting and hotkey; keys: VirtualSpaceDistance, DistanceHotkey; observed default: VirtualSpaceDistance=255, DistanceHotkey=69,4; Android mapping: virtual HUD distance scale; capture: Capture distance changes at near, middle, and far probe values.",
            ),
        )
        assertTrue(
            markdown.contains(
                "- IPD: Software IPD setting; keys: SoftwareIPD; observed default: SoftwareIPD=635 in tenths of millimeters; Android mapping: stored IPD calibration and fit guidance; capture: Capture 60 mm, 67 mm, and 72 mm writes and readback before enabling Android firmware apply.",
            ),
        )
        assertTrue(
            markdown.contains(
                "- Brightness: ASUS_AirVision_SDK HID brightness API; keys: SET_BRIGHTNESS, GlassesBrightness, BrightnessChanged;",
            ),
        )
        assertTrue(markdown.contains("- Android HUD layout: OpenClaw Android Presentation-only layout; keys: none; observed default: not an ASUS Windows app setting"))
        assertTrue(markdown.contains("### Walking HUD"))
        assertTrue(markdown.contains("- IPD: 69 mm (locked by Light Load Mode)"))
        assertTrue(markdown.contains("- 3D Mode: off (locked by Light Load Mode)"))
        assertTrue(
            markdown.contains(
                "- Android runtime summary: effective HUD scale 130%, transcript 3, captions 2, overlays off, dimming off",
            ),
        )
        assertTrue(markdown.contains("- Android runtime controls: IPD unavailable, 3D unavailable, Eye Care unavailable"))
        assertTrue(markdown.contains("## Android HUD Controls"))
        assertTrue(markdown.contains("- Single tap: Dismiss notification"))
        assertTrue(markdown.contains("- Double tap: Toggle mic"))
        assertTrue(markdown.contains("- Swipe: Scroll chat"))
        assertTrue(markdown.contains("- Brightness key: Adjust brightness"))
        assertTrue(markdown.contains("- Media key: Double-tap mic"))
        assertTrue(markdown.contains("## M1 Hardware Key Mapping"))
        assertTrue(markdown.contains("- Brightness key action: Adjust brightness"))
        assertTrue(markdown.contains("- Brightness key consumed by Android: yes"))
        assertTrue(markdown.contains("- Brightness key effect: steps Android HUD dimming by 5% per press"))
        assertTrue(markdown.contains("- Media key action: Double-tap mic"))
        assertTrue(markdown.contains("- Media key double-tap window: 500 ms"))
        assertTrue(markdown.contains("- Firmware brightness passthrough expected: no"))
        assertTrue(markdown.contains("## Windows Gesture Catalog"))
        assertTrue(markdown.contains("- Two-finger tap: ASUS instant transparent / lowest brightness; Android status firmware passthrough."))
        assertTrue(markdown.contains("- One-finger press and hold 1.5 seconds, then slide: ASUS shortcut menu for brightness, volume, and distance; Android maps brightness/distance through HUD controls and speaker routing through AirVision preferences."))
        assertTrue(markdown.contains("- Two-finger press and hold 1.5 seconds: ASUS 3D toggle; Android stores 3D preference and keeps panel write firmware-gated."))
        assertTrue(markdown.contains("## Shortcut Menu Parity"))
        assertTrue(markdown.contains("- Shortcut menu parity: brightness Adjust brightness, volume speaker muted, distance settings-only; 1 Android-mapped, 2 firmware/system-owned."))
        assertTrue(markdown.contains("- Brightness: Android HUD dimming, 5% per brightness-key press"))
        assertTrue(markdown.contains("- Volume: OpenClaw speaker/TTS output muted; hardware volume route remains Android system or M1 firmware owned"))
        assertTrue(markdown.contains("- Distance: Settings slider stores virtual projection distance; ASUS shortcut-menu distance remains firmware/Windows owned"))
        assertTrue(markdown.contains("- Android-mapped controls: 1"))
        assertTrue(markdown.contains("- Firmware/system-owned controls: 2"))
        assertTrue(markdown.contains("## Windows-Only Spatial & Mirror Controls"))
        assertTrue(markdown.contains("- Cursor Follow: Windows AirVision app only"))
        assertTrue(markdown.contains("- Center Cursor: Windows AirVision app only"))
        assertTrue(markdown.contains("- 3DoF: Windows laptop/AirVision app only"))
        assertTrue(markdown.contains("- Unity mirror window / Ctrl+Alt+E: Windows AirVision app only"))
        assertTrue(markdown.contains("- Android distance hotkey fallback: off"))
        assertTrue(markdown.contains("- Android mirror fallback: Use Android/DeX screen sharing outside OpenClaw HUD; the ASUS Unity mirror window is Windows-only."))
        assertTrue(markdown.contains("- Fallback action: Open Android Cast settings from AirVision M1 settings."))
        assertTrue(markdown.contains("- Fallback action: Open Android Display settings from AirVision M1 settings."))
        assertTrue(markdown.contains("- Fallback action: Use Samsung DeX or Android screen sharing outside OpenClaw HUD when a projected-glasses-view mirror is needed."))
        assertTrue(markdown.contains("- Launch action Cast: android.settings.CAST_SETTINGS (fallback android.settings.DISPLAY_SETTINGS)"))
        assertTrue(markdown.contains("  - Opens Android Cast settings, falling back to Display settings when Cast settings are unavailable."))
        assertTrue(markdown.contains("- Launch action Display: android.settings.DISPLAY_SETTINGS"))
        assertTrue(markdown.contains("  - Opens Android Display settings for DeX/external-display and screen-sharing fallback setup."))
        assertTrue(markdown.contains("- M1 touch hardware passthrough: yes"))
        assertTrue(markdown.contains("- Limitation: Cursor Follow requires Windows AirVision line-of-sight cursor control."))
        assertTrue(markdown.contains("- Limitation: ASUS documents 3DoF support as Windows laptop only; phones do not support it."))
        assertTrue(markdown.contains("## Android Companion Parity States"))
        assertTrue(
            markdown.contains(
                "- AirVision companion parity: 7 offline-reviewable, 6 M1-optional, 3 firmware-gated, 2 Windows-only",
            ),
        )
        assertTrue(markdown.contains("- Brightness: m1_optional; Android applies software HUD dimming offline"))
        assertTrue(markdown.contains("- Splendid, Eye Care, and blue-light filter: firmware_gated"))
        assertTrue(markdown.contains("- Captions and translation: reviewable_offline; Native captions preference is on"))
        assertTrue(markdown.contains("- Firmware apply and update: firmware_gated; Android firmware writes"))
        assertTrue(markdown.contains("## Android App Preferences"))
        assertTrue(markdown.contains("- Startup view: HUD"))
        assertTrue(markdown.contains("- HUD display target: AirVision Preferred"))
        assertTrue(markdown.contains("- Companion language: System"))
        assertTrue(markdown.contains("- Speaker output: off"))
        assertTrue(markdown.contains("- Samsung/native captions: on"))
        assertTrue(markdown.contains("- Translation captions: Auto -> Spanish"))
        assertTrue(markdown.contains("- Demo Mode: on"))
        assertTrue(markdown.contains("## Caption and Translation Mode"))
        assertTrue(markdown.contains("- Captions: native on, OpenClaw fallback Auto -> Spanish, model sage-router/fast, thinking off, speakers S1/S2."))
        assertTrue(markdown.contains("- Native provider: Android/Samsung native captions floating window"))
        assertTrue(markdown.contains("- OpenClaw fallback available: yes"))
        assertTrue(markdown.contains("- OpenClaw fallback model: sage-router/fast"))
        assertTrue(markdown.contains("- OpenClaw fallback thinking: off"))
        assertTrue(markdown.contains("- Speaker labels: S1, S2"))
        assertTrue(markdown.contains("## Support, Legal, and Registration"))
        assertTrue(markdown.contains("- AirVision support metadata: EULA note, privacy boundary, FAQ/tutorial, product registration, and ASUS support links."))
        assertTrue(markdown.contains("- EULA status: ASUS AirVision EULA is shown in the ASUS Windows app"))
        assertTrue(markdown.contains("- Legal note: ASUS displays the AirVision EULA inside the Windows AirVision app."))
        assertTrue(markdown.contains("- Privacy boundary: Support metadata contains public ASUS links and OpenClaw policy text only"))
        assertTrue(markdown.contains("- Product registration: https://account.asus.com/product_reg.aspx"))
        assertTrue(markdown.contains("Open the ASUS AirVision Windows app on Cyber"))
        assertTrue(markdown.contains("Review Android companion parity states"))
        assertTrue(markdown.contains("true panel preset writes"))
        assertTrue(markdown.contains("- Connected: yes"))
        assertTrue(markdown.contains("- USB ID: 0x0b05:0x1b3c"))
        assertTrue(markdown.contains("- Serial status: available"))
        assertTrue(markdown.contains("- Android-visible firmware/version context: USB descriptor 1.02"))
        assertTrue(markdown.contains("https://www.asus.com/support/faq/1054069/"))
        assertFalse(markdown.contains("private-device-serial"))
    }

    @Test
    fun renderMarkdown_handlesMissingProfiles() {
        val markdown =
            AirVisionWindowsProfileHandoffs.renderMarkdown(
                profileBackup =
                    AirVisionProfileBackup(
                        activeViewMode = AirVisionViewMode.Working.rawValue,
                        customLabels =
                            AirVisionBackupCustomLabels(
                                custom1 = AirVisionViewMode.Custom1.label,
                                custom2 = AirVisionViewMode.Custom2.label,
                            ),
                        hudControls =
                            AirVisionBackupHudControls(
                                singleTapAction = "clear_notification",
                                doubleTapAction = "toggle_mic",
                                swipeAction = "scroll_chat",
                                brightnessKeyAction = "adjust_brightness",
                                mediaKeyAction = "toggle_mic",
                            ),
                        appPreferences =
                            AirVisionBackupAppPreferences(
                                language = "system",
                                startupDestination = "hud",
                                hudDisplayTarget = "airvision_preferred",
                                demoModeEnabled = false,
                            ),
                        profiles = emptyList(),
                    ),
                usbState = AirVisionUsbState(),
            )

        assertTrue(markdown.contains("- No AirVision profile values were available."))
        assertTrue(markdown.contains("- Runtime profile unavailable because no active profile values were available."))
        assertTrue(markdown.contains("- Apply matrix unavailable because no active profile values were available."))
        assertTrue(markdown.contains("- Connected: no"))
        assertTrue(markdown.contains("- USB ID: not detected"))
        assertTrue(markdown.contains("- Serial status: not captured"))
    }

    @Test
    fun renderMarkdown_marksDistanceHotkeyFallbackWhenMappedToBrightnessKeys() {
        val markdown =
            AirVisionWindowsProfileHandoffs.renderMarkdown(
                profileBackup =
                    AirVisionProfileBackup(
                        activeViewMode = AirVisionViewMode.Working.rawValue,
                        customLabels =
                            AirVisionBackupCustomLabels(
                                custom1 = AirVisionViewMode.Custom1.label,
                                custom2 = AirVisionViewMode.Custom2.label,
                            ),
                        hudControls =
                            AirVisionBackupHudControls(
                                singleTapAction = "dismiss_notification",
                                doubleTapAction = "toggle_mic",
                                swipeAction = "scroll_chat",
                                brightnessKeyAction = "adjust_distance",
                                mediaKeyAction = "double_tap_toggle_mic",
                            ),
                        appPreferences =
                            AirVisionBackupAppPreferences(
                                language = "system",
                                startupDestination = "hud",
                                hudDisplayTarget = "airvision_preferred",
                                demoModeEnabled = false,
                            ),
                        profiles =
                            listOf(
                                AirVisionProfileBackups.profileFromSettings(
                                    AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working),
                                ),
                            ),
                    ),
                usbState = AirVisionUsbState(),
            )

        assertTrue(markdown.contains("- Android distance hotkey fallback: mapped to M1 brightness keys"))
        assertFalse(markdown.contains("- Android distance hotkey fallback: off"))
        assertTrue(markdown.contains("- Brightness key action: Adjust distance"))
        assertTrue(markdown.contains("- Brightness key effect: steps virtual projection distance by 5 cm per press"))
        assertTrue(markdown.contains("- Firmware brightness passthrough expected: no"))
        assertTrue(markdown.contains("Android maps the distance hotkey concept to M1 brightness-key virtual-distance changes"))
        assertTrue(markdown.contains("- Embedded runtime metadata: missing; recalculated from active profile values"))
    }

    @Test
    fun renderMarkdown_marksStaleRuntimeMetadata() {
        val settings =
            AirVisionDisplaySettings.defaultsForViewMode(AirVisionViewMode.Working).copy(
                brightnessPercent = 45,
                hudScalePercent = 135,
            )
        val markdown =
            AirVisionWindowsProfileHandoffs.renderMarkdown(
                profileBackup =
                    AirVisionProfileBackup(
                        activeViewMode = AirVisionViewMode.Working.rawValue,
                        customLabels =
                            AirVisionBackupCustomLabels(
                                custom1 = AirVisionViewMode.Custom1.label,
                                custom2 = AirVisionViewMode.Custom2.label,
                            ),
                        hudControls =
                            AirVisionBackupHudControls(
                                singleTapAction = "dismiss_notification",
                                doubleTapAction = "toggle_mic",
                                swipeAction = "scroll_chat",
                                brightnessKeyAction = "adjust_brightness",
                                mediaKeyAction = "double_tap_toggle_mic",
                            ),
                        appPreferences =
                            AirVisionBackupAppPreferences(
                                language = "system",
                                startupDestination = "hud",
                                hudDisplayTarget = "airvision_preferred",
                                demoModeEnabled = false,
                            ),
                        runtimeProfiles =
                            listOf(
                                AirVisionBackupRuntimeProfile(
                                    viewMode = AirVisionViewMode.Working.rawValue,
                                    ipdAdjustmentEnabled = true,
                                    threeDModeAvailable = true,
                                    blueLightFilterAvailable = false,
                                    hudTranscriptEntryCount = 1,
                                    hudCaptionEntryCount = 1,
                                    effectiveHudScalePercent = 10,
                                    colorPreviewOverlaysEnabled = false,
                                    brightnessDimmingEnabled = false,
                                ),
                            ),
                        profiles = listOf(AirVisionProfileBackups.profileFromSettings(settings)),
                    ),
                usbState = AirVisionUsbState(),
            )

        assertTrue(markdown.contains("- Effective HUD scale: 135%"))
        assertTrue(markdown.contains("- Embedded runtime metadata: stale; recalculated from active profile values"))
    }
}
