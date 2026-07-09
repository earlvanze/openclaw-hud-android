package ai.openclaw.app

enum class AirVisionAppLanguage(
    val rawValue: String,
    val label: String,
) {
    System("system", "System"),
    English("en", "English"),
    Spanish("es", "Spanish"),
    French("fr", "French"),
    German("de", "German"),
    Japanese("ja", "Japanese"),
    Korean("ko", "Korean"),
    ChineseSimplified("zh_cn", "Chinese (Simplified)"),
    ChineseTraditional("zh_tw", "Chinese (Traditional)"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionAppLanguage =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: System
    }
}

enum class AirVisionStartupDestination(
    val rawValue: String,
    val label: String,
) {
    Hud("hud", "HUD"),
    Chat("chat", "Chat"),
    Voice("voice", "Voice"),
    Agents("agents", "Agents"),
    Settings("settings", "Settings"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionStartupDestination =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: Hud
    }
}
