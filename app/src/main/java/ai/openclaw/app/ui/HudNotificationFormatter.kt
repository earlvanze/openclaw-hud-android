package ai.openclaw.app.ui

import ai.openclaw.app.isOpenClawAppPackage
import ai.openclaw.app.node.DeviceNotificationEntry

internal enum class HudNotificationKind {
    Navigation,
    Message,
    Status,
}

internal data class HudNotificationLine(
    val key: String,
    val source: String,
    val primary: String,
    val secondary: String?,
    val kind: HudNotificationKind,
    val isClearable: Boolean,
)

private const val MAPS_PACKAGE = "com.google.android.apps.maps"
private const val WAZE_PACKAGE = "com.waze"
private val hudSecretAssignmentRegex =
    Regex(
        "(['\"]?\\b[\\w.-]*(?:api[_-]?key|token|secret|password|signaturekey|privatekey|accesskey)[\\w.-]*\\b['\"]?\\s*[:=]\\s*['\"]?)([^'\"\\s,;}\\]]{6,})(['\"]?)",
        RegexOption.IGNORE_CASE,
    )
private val hudPrivateKeyBlockRegex =
    Regex(
        "-----BEGIN [^-]*PRIVATE KEY-----.*?-----END [^-]*PRIVATE KEY-----",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

internal fun selectHudNotification(notifications: List<DeviceNotificationEntry>): HudNotificationLine? {
    return selectHudNotifications(notifications).firstOrNull()
}

internal fun selectHudNotifications(notifications: List<DeviceNotificationEntry>): List<HudNotificationLine> {
    val rankedNotifications =
        notifications
            .asSequence()
            .filterNot { it.packageName.isBlank() }
            .filterNot { isOpenClawAppPackage(it.packageName) }
            .sortedWith(
                compareByDescending<DeviceNotificationEntry> { hudNotificationPriority(it) }
                    .thenByDescending { it.postTimeMs },
            )

    return rankedNotifications
        .mapNotNull { it.toHudNotificationLine() }
        .filterNot { it.kind == HudNotificationKind.Status }
        .toList()
}

internal fun adjacentHudNotification(
    notifications: List<HudNotificationLine>,
    currentKey: String?,
    direction: HudHorizontalSwipeDirection,
): HudNotificationLine? {
    if (notifications.isEmpty()) return null
    val currentIndex = notifications.indexOfFirst { it.key == currentKey }.takeIf { it >= 0 } ?: 0
    val offset = if (direction == HudHorizontalSwipeDirection.Left) 1 else -1
    val nextIndex = (currentIndex + offset).mod(notifications.size)
    return notifications[nextIndex]
}

private fun DeviceNotificationEntry.toHudNotificationLine(): HudNotificationLine? {
    val kind = classifyHudNotification()
    val title = cleanHudText(title)
    val text = cleanHudText(text)
    val subText = cleanHudText(subText)
    val primary =
        when (kind) {
            HudNotificationKind.Navigation -> text ?: title ?: subText
            HudNotificationKind.Message, HudNotificationKind.Status -> title ?: text ?: subText
        } ?: return null
    val secondary =
        when (kind) {
            HudNotificationKind.Navigation -> listOfNotNull(title, subText).firstOrNull { it != primary }
            HudNotificationKind.Message, HudNotificationKind.Status -> listOfNotNull(text, subText).firstOrNull { it != primary }
        }
    return HudNotificationLine(
        key = key,
        source = hudNotificationSourceLabel(packageName),
        primary = primary,
        secondary = secondary,
        kind = kind,
        isClearable = isClearable,
    )
}

private fun DeviceNotificationEntry.classifyHudNotification(): HudNotificationKind =
    when {
        packageName == MAPS_PACKAGE || packageName == WAZE_PACKAGE -> HudNotificationKind.Navigation
        category == "msg" -> HudNotificationKind.Message
        else -> HudNotificationKind.Status
    }

private fun hudNotificationPriority(entry: DeviceNotificationEntry): Int =
    when {
        entry.packageName == MAPS_PACKAGE || entry.packageName == WAZE_PACKAGE -> 100
        entry.isOngoing -> 70
        entry.category == "msg" -> 50
        else -> 10
    }

private fun hudNotificationSourceLabel(packageName: String): String =
    when (packageName) {
        MAPS_PACKAGE -> "Maps"
        WAZE_PACKAGE -> "Waze"
        "com.ubercab" -> "Uber"
        "com.google.android.gm" -> "Gmail"
        "com.google.android.calendar" -> "Calendar"
        else ->
            packageName
                .substringAfterLast('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
    }

private fun cleanHudText(raw: String?): String? {
    val text =
        raw
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    return maskHudSecrets(text)
        .trim()
        .takeIf { it.isNotEmpty() && it.lowercase() != "null" }
}

private fun maskHudSecrets(text: String): String =
    hudSecretAssignmentRegex.replace(
        hudPrivateKeyBlockRegex.replace(text, "[redacted private key]"),
    ) { match ->
        "${match.groupValues[1]}[redacted]${match.groupValues[3]}"
    }
