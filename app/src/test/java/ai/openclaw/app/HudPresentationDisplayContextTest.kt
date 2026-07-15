package ai.openclaw.app

import android.hardware.display.DisplayManager
import android.view.Display
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HudPresentationDisplayContextTest {
    @Test
    fun composeContextUsesSelectedExternalDisplayConfiguration() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val displayId = ShadowDisplayManager.addDisplay("w1280dp-h720dp-mdpi")
        val display = activity.getSystemService(DisplayManager::class.java).getDisplay(displayId)
        val app = RuntimeEnvironment.getApplication() as NodeApp
        val presentation = HudPresentation(activity, display, MainViewModel(app), onHudKeyEvent = { false })

        val composeContext = presentation.composeDisplayContext()

        assertEquals(displayId, composeContext.display.displayId)
        assertNotEquals(Display.DEFAULT_DISPLAY, composeContext.display.displayId)
        assertEquals(1f, composeContext.resources.displayMetrics.density)
        assertEquals(1280, composeContext.resources.displayMetrics.widthPixels)
        assertEquals(720, composeContext.resources.displayMetrics.heightPixels)
    }
}
