package ai.openclaw.app

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayReviewDemoLaunchTest {
    @Test
    fun parsesPlayReviewDemoActionWithDefaultHudDestination() {
        val parsed = parsePlayReviewDemoLaunchIntent(Intent(ACTION_PLAY_REVIEW_DEMO))

        assertNotNull(parsed)
        assertEquals(AirVisionStartupDestination.Hud, parsed?.destination)
    }

    @Test
    fun parsesDestinationExtra() {
        val parsed =
            parsePlayReviewDemoLaunchIntent(
                Intent(ACTION_PLAY_REVIEW_DEMO)
                    .putExtra(EXTRA_PLAY_REVIEW_DESTINATION, " settings "),
            )

        assertNotNull(parsed)
        assertEquals(AirVisionStartupDestination.Settings, parsed?.destination)
    }

    @Test
    fun ignoresMissingOrUnrelatedAction() {
        assertNull(parsePlayReviewDemoLaunchIntent(null))
        assertNull(parsePlayReviewDemoLaunchIntent(Intent(Intent.ACTION_VIEW)))
    }
}
