package ai.openclaw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssistantShortcutMetadataTest {
    @Test
    fun assistantShortcutTargetsCurrentApplicationId() {
        val parser = RuntimeEnvironment.getApplication().resources.getXml(R.xml.shortcuts)
        var targetPackage: String? = null
        var targetClass: String? = null
        var action: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "intent") {
                targetPackage = parser.getAttributeValue(ANDROID_NS, "targetPackage")
                targetClass = parser.getAttributeValue(ANDROID_NS, "targetClass")
                action = parser.getAttributeValue(ANDROID_NS, "action")
                break
            }
            parser.next()
        }
        parser.close()

        assertNotNull("shortcuts.xml must declare an assistant intent", targetPackage)
        assertEquals(BuildConfig.APPLICATION_ID, targetPackage)
        assertEquals("ai.openclaw.app.MainActivity", targetClass)
        assertEquals(actionAskOpenClaw, action)
    }

    private companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
