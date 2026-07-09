package ai.openclaw.app

import android.content.Intent

const val ACTION_PLAY_REVIEW_DEMO = "ai.openclaw.app.action.PLAY_REVIEW_DEMO"
const val EXTRA_PLAY_REVIEW_DESTINATION = "destination"

data class PlayReviewDemoLaunchRequest(
    val destination: AirVisionStartupDestination,
)

fun parsePlayReviewDemoLaunchIntent(intent: Intent?): PlayReviewDemoLaunchRequest? {
    if (intent?.action != ACTION_PLAY_REVIEW_DEMO) return null
    return PlayReviewDemoLaunchRequest(
        destination =
            AirVisionStartupDestination.fromRawValue(
                intent.getStringExtra(EXTRA_PLAY_REVIEW_DESTINATION),
            ),
    )
}
