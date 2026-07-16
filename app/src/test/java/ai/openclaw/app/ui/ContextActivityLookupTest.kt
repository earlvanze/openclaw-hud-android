package ai.openclaw.app.ui

import android.app.Activity
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextActivityLookupTest {
    @Test
    fun `unwraps presentation-style context wrappers`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextThemeWrapper(ContextWrapper(activity), android.R.style.Theme_DeviceDefault)

        assertSame(activity, wrapped.findActivityOrNull())
    }
}
