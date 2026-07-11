package ai.openclaw.app

import android.app.Notification
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NodeForegroundServiceTest {
    @Test
    fun buildNotificationSetsLaunchIntent() {
        val service = Robolectric.buildService(NodeForegroundService::class.java).get()
        val notification = buildNotification(service)

        val pendingIntent = notification.contentIntent
        assertNotNull(pendingIntent)

        val savedIntent = Shadows.shadowOf(pendingIntent).savedIntent
        assertNotNull(savedIntent)
        assertEquals(MainActivity::class.java.name, savedIntent.component?.className)

        val expectedFlags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        assertEquals(expectedFlags, savedIntent.flags and expectedFlags)
    }

    @Test
    fun foregroundTitleUsesCurrentFlavorAppLabel() {
        val service = Robolectric.buildService(NodeForegroundService::class.java).get()
        val appName = service.getString(R.string.app_name)

        assertEquals(appName, nodeForegroundNotificationTitle(appName, connected = false))
        assertEquals("$appName · Connected", nodeForegroundNotificationTitle(appName, connected = true))

        val notification = buildNotification(service, title = appName)
        assertEquals(appName, notification.extras.getCharSequence(Notification.EXTRA_TITLE))
    }

    private fun buildNotification(service: NodeForegroundService): Notification {
        return buildNotification(service, title = "Title")
    }

    private fun buildNotification(
        service: NodeForegroundService,
        title: String,
    ): Notification {
        val method =
            NodeForegroundService::class.java.getDeclaredMethod(
                "buildNotification",
                String::class.java,
                String::class.java,
            )
        method.isAccessible = true
        return method.invoke(service, title, "Text") as Notification
    }
}
