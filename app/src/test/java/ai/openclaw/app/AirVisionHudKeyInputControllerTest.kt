package ai.openclaw.app

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirVisionHudKeyInputControllerTest {
    @Test
    fun accessoryCancelKeysAbortOnlyWhileRunIsActive() {
        val controller = AirVisionHudKeyInputController()

        for (keyCode in listOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE)) {
            assertEquals(
                AirVisionHudKeyDecision(
                    consume = true,
                    command = AirVisionHudKeyCommand.AbortActiveRun,
                ),
                controller.handleKeyEvent(
                    keyCode = keyCode,
                    action = KeyEvent.ACTION_DOWN,
                    eventTimeMs = 1_000L,
                    isHudAccessoryEvent = true,
                    controls = AirVisionHudControls(),
                    hasActiveRun = true,
                ),
            )
            assertEquals(
                AirVisionHudKeyDecision(consume = false),
                controller.handleKeyEvent(
                    keyCode = keyCode,
                    action = KeyEvent.ACTION_DOWN,
                    eventTimeMs = 1_100L,
                    isHudAccessoryEvent = true,
                    controls = AirVisionHudControls(),
                    hasActiveRun = false,
                ),
            )
            assertEquals(
                AirVisionHudKeyDecision(consume = true),
                controller.handleKeyEvent(
                    keyCode = keyCode,
                    action = KeyEvent.ACTION_UP,
                    eventTimeMs = 1_200L,
                    isHudAccessoryEvent = true,
                    controls = AirVisionHudControls(),
                    hasActiveRun = true,
                ),
            )
        }
    }

    @Test
    fun internalCancelKeysNeverAbortActiveRun() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_ESCAPE,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
                hasActiveRun = true,
            ),
        )
    }

    @Test
    fun accessoryGamepadXStartsNotificationReply() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.StartNotificationReply,
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_X,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_X,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun accessoryDpadBrowsesNotifications() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.BrowseNotifications(-1),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.BrowseNotifications(1),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BUTTON_R1,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun notificationBrowseKeysRequireAccessoryAndEnabledControl() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = true,
                controls =
                    AirVisionHudControls(
                        horizontalSwipeAction = AirVisionHudHorizontalSwipeAction.None,
                    ),
            ),
        )
    }

    @Test
    fun brightnessKeysDefaultToScrollChatAndConsumeUpEvents() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ScrollChat(-160f),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BRIGHTNESS_DOWN,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(consume = true),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BRIGHTNESS_DOWN,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_010L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun brightnessKeysCanBeDisabledOrMappedToHudAdjustments() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BRIGHTNESS_UP,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(brightnessKeyAction = AirVisionHudKeyAction.None),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.AdjustBrightness(5),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BRIGHTNESS_UP,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(brightnessKeyAction = AirVisionHudKeyAction.AdjustBrightness),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.AdjustDistance(-5),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_BRIGHTNESS_DOWN,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_200L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(brightnessKeyAction = AirVisionHudKeyAction.AdjustDistance),
            ),
        )
    }

    @Test
    fun externalDpadAndPageKeysScrollWithSwipePolicy() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ScrollChat(160f),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_UP,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ScrollChat(-480f),
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_PAGE_DOWN,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(consume = true),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_UP,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_200L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun navigationKeysPassThroughForInternalDevicesOrDisabledSwipe() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_100L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(swipeAction = AirVisionHudSwipeAction.None),
            ),
        )
    }

    @Test
    fun firstMediaTapOnlyArmsDoubleTapEvenNearBoot() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(AirVisionHudKeyCommand.ArmMicDoubleTap, controller.handleMicTap(100L))
        assertEquals(AirVisionHudKeyCommand.ToggleMic, controller.handleMicTap(300L))
    }

    @Test
    fun mediaDoubleTapRespectsTimeout() {
        val controller = AirVisionHudKeyInputController(doubleTapTimeoutMs = 500L)

        assertEquals(AirVisionHudKeyCommand.ArmMicDoubleTap, controller.handleMicTap(1_000L))
        assertEquals(AirVisionHudKeyCommand.ArmMicDoubleTap, controller.handleMicTap(1_700L))
        assertEquals(AirVisionHudKeyCommand.ToggleMic, controller.handleMicTap(2_000L))
    }

    @Test
    fun accessoryCenterKeyRequiresDoubleTapButBuiltInCenterKeyPassesThrough() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = false),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ArmMicDoubleTap,
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun globalMediaKeyDoubleTapWorksWithoutAccessoryIdentity() {
        val controller = AirVisionHudKeyInputController()

        assertEquals(
            AirVisionHudKeyDecision(consume = true),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                action = KeyEvent.ACTION_DOWN,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ArmMicDoubleTap,
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_010L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
        assertEquals(
            AirVisionHudKeyDecision(
                consume = true,
                command = AirVisionHudKeyCommand.ToggleMic,
            ),
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_300L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(),
            ),
        )
    }

    @Test
    fun unhandledAccessoryKeyIsLoggedButNotConsumed() {
        val controller = AirVisionHudKeyInputController()
        val decision =
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_A,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = true,
                controls = AirVisionHudControls(),
            )

        assertEquals(false, decision.consume)
        assertEquals(AirVisionHudKeyCommand.LogUnhandledHudAccessoryKey, decision.command)
    }

    @Test
    fun disabledMediaActionPassesThrough() {
        val controller = AirVisionHudKeyInputController()
        val decision =
            controller.handleKeyEvent(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                action = KeyEvent.ACTION_UP,
                eventTimeMs = 1_000L,
                isHudAccessoryEvent = false,
                controls = AirVisionHudControls(mediaKeyAction = AirVisionHudMediaKeyAction.None),
            )

        assertEquals(false, decision.consume)
        assertNull(decision.command)
    }
}
