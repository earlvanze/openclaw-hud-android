package ai.openclaw.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudHostDisplayPolicyTest {
    @Test
    fun relaunchesHudHostWhenStartedOnExternalDisplay() {
        assertTrue(
            shouldRelaunchHudHostOnDefaultDisplay(
                openClawDefaultHud = true,
                activityOnExternalDisplay = true,
                relaunchAlreadyAttempted = false,
            ),
        )
    }

    @Test
    fun leavesDefaultDisplayAndNonHudBuildsAlone() {
        assertFalse(
            shouldRelaunchHudHostOnDefaultDisplay(
                openClawDefaultHud = true,
                activityOnExternalDisplay = false,
                relaunchAlreadyAttempted = false,
            ),
        )
        assertFalse(
            shouldRelaunchHudHostOnDefaultDisplay(
                openClawDefaultHud = false,
                activityOnExternalDisplay = true,
                relaunchAlreadyAttempted = false,
            ),
        )
    }

    @Test
    fun doesNotLoopWhenDisplayLaunchOptionsAreIgnored() {
        assertFalse(
            shouldRelaunchHudHostOnDefaultDisplay(
                openClawDefaultHud = true,
                activityOnExternalDisplay = true,
                relaunchAlreadyAttempted = true,
            ),
        )
    }
}
