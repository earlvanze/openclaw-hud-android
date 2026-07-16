package ai.openclaw.app.ui

import ai.openclaw.app.node.DeviceNotificationEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudNotificationFormatterTest {
    @Test
    fun selectHudNotification_prioritizesGoogleMapsNavigation() {
        val line =
            selectHudNotification(
                listOf(
                    entry(
                        packageName = "com.chat.example",
                        title = "Alex",
                        text = "Are you nearby?",
                        category = "msg",
                        postTimeMs = 200,
                    ),
                    entry(
                        packageName = "com.google.android.apps.maps",
                        title = "12 min to Home",
                        text = "Continue on Main St",
                        postTimeMs = 100,
                        isOngoing = true,
                    ),
                ),
            )

        assertEquals(HudNotificationKind.Navigation, line?.kind)
        assertEquals("Maps", line?.source)
        assertEquals("Continue on Main St", line?.primary)
        assertEquals("12 min to Home", line?.secondary)
    }

    @Test
    fun selectHudNotification_usesNewestMessageWhenNoNavigationExists() {
        val line =
            selectHudNotification(
                listOf(
                    entry(packageName = "com.example.old", title = "Old", text = "Earlier", category = "msg", postTimeMs = 10),
                    entry(packageName = "com.example.new", title = "New", text = "Latest", category = "msg", postTimeMs = 20),
                ),
            )

        assertEquals(HudNotificationKind.Message, line?.kind)
        assertEquals("New", line?.primary)
        assertEquals("Latest", line?.secondary)
    }

    @Test
    fun selectHudNotifications_returnsRankedBrowsableLines() {
        val lines =
            selectHudNotifications(
                listOf(
                    entry(packageName = "com.example.old", title = "Old", category = "msg", postTimeMs = 10),
                    entry(packageName = "com.google.android.apps.maps", text = "Turn left", postTimeMs = 5),
                    entry(packageName = "com.example.new", title = "New", category = "msg", postTimeMs = 20),
                ),
            )

        assertEquals(listOf("Maps", "New", "Old"), lines.map { it.source })
    }

    @Test
    fun adjacentHudNotification_wrapsLeftAsNextAndRightAsPrevious() {
        val lines =
            listOf(
                HudNotificationLine("one", "One", "First", null, HudNotificationKind.Message, true),
                HudNotificationLine("two", "Two", "Second", null, HudNotificationKind.Message, true),
                HudNotificationLine("three", "Three", "Third", null, HudNotificationKind.Message, true),
            )

        assertEquals("two", adjacentHudNotification(lines, "one", HudHorizontalSwipeDirection.Left)?.key)
        assertEquals("three", adjacentHudNotification(lines, "one", HudHorizontalSwipeDirection.Right)?.key)
        assertEquals("one", adjacentHudNotification(lines, "three", HudHorizontalSwipeDirection.Left)?.key)
        assertEquals("three", adjacentHudNotification(lines, "one", offset = -1)?.key)
        assertEquals("two", adjacentHudNotification(lines, "one", offset = 1)?.key)
        assertNull(adjacentHudNotification(emptyList(), "one", HudHorizontalSwipeDirection.Left))
    }

    @Test
    fun selectHudNotification_ignoresEmptyNotifications() {
        val line =
            selectHudNotification(
                listOf(
                    entry(packageName = "com.example.empty", title = " ", text = null, subText = "null"),
                ),
            )

        assertNull(line)
    }

    @Test
    fun selectHudNotification_ignoresOpenClawAndGenericStatusNotifications() {
        val line =
            selectHudNotification(
                listOf(
                    entry(
                        packageName = "ai.openclaw.app.hud",
                        title = "OpenClaw HUD · Connected",
                        text = "Connected",
                        postTimeMs = 40,
                        isOngoing = true,
                    ),
                    entry(
                        packageName = "ai.openclaw.app",
                        title = "OpenClaw Node · Connected",
                        text = "Connected",
                        postTimeMs = 30,
                        isOngoing = true,
                    ),
                    entry(
                        packageName = "com.example.status",
                        title = "Background sync",
                        text = "Connected",
                        postTimeMs = 20,
                        isOngoing = true,
                    ),
                ),
            )

        assertNull(line)
    }

    @Test
    fun selectHudNotification_keepsFullSanitizedNotificationTextAndDismissMetadata() {
        val longText =
            "Take the pedestrian bridge, continue past the north lobby, then turn left at the elevator bank near the green signs."
        val line =
            selectHudNotification(
                listOf(
                    entry(
                        packageName = "com.google.android.apps.maps",
                        title = "8 min to Coffee",
                        text = longText,
                        postTimeMs = 100,
                        isOngoing = false,
                        isClearable = true,
                    ),
                ),
            )

        assertEquals(longText, line?.primary)
        assertEquals("com.google.android.apps.maps:100", line?.key)
        assertTrue(line?.isClearable == true)
    }

    @Test
    fun selectHudNotification_preservesReplyCapability() {
        val line =
            selectHudNotification(
                listOf(
                    entry(
                        packageName = "com.example.messages",
                        title = "Alex",
                        text = "On my way",
                        category = "msg",
                        canReply = true,
                    ),
                ),
            )

        assertTrue(line?.canReply == true)
    }

    @Test
    fun selectHudNotification_redactsSecretShapedNotificationText() {
        val line =
            selectHudNotification(
                listOf(
                    entry(
                        packageName = "com.example.chat",
                        title = "Agent",
                        text = "account: {'token': 'abc123456789', 'accountSignatureKey': 'secret-value-123'} password=very-secret",
                        category = "msg",
                        postTimeMs = 100,
                    ),
                ),
            )

        assertEquals(
            "account: {'token': '[redacted]', 'accountSignatureKey': '[redacted]'} password=[redacted]",
            line?.secondary,
        )
    }

    private fun entry(
        packageName: String,
        title: String? = null,
        text: String? = null,
        subText: String? = null,
        category: String? = null,
        postTimeMs: Long = 1,
        isOngoing: Boolean = false,
        isClearable: Boolean = !isOngoing,
        canReply: Boolean = false,
    ): DeviceNotificationEntry =
        DeviceNotificationEntry(
            key = "$packageName:$postTimeMs",
            packageName = packageName,
            title = title,
            text = text,
            subText = subText,
            category = category,
            channelId = null,
            postTimeMs = postTimeMs,
            isOngoing = isOngoing,
            isClearable = isClearable,
            canReply = canReply,
        )
}
