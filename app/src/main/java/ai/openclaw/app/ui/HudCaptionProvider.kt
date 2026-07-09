package ai.openclaw.app.ui

enum class HudCaptionProvider {
    Off,
    Native,
    OpenClaw,
}

object HudCaptionProviders {
    fun current(
        nativeEnabled: Boolean,
        openClawEnabled: Boolean,
    ): HudCaptionProvider =
        when {
            nativeEnabled -> HudCaptionProvider.Native
            openClawEnabled -> HudCaptionProvider.OpenClaw
            else -> HudCaptionProvider.Off
        }

    fun next(current: HudCaptionProvider): HudCaptionProvider =
        when (current) {
            HudCaptionProvider.Off -> HudCaptionProvider.Native
            HudCaptionProvider.Native -> HudCaptionProvider.OpenClaw
            HudCaptionProvider.OpenClaw -> HudCaptionProvider.Off
        }

    fun next(
        nativeEnabled: Boolean,
        openClawEnabled: Boolean,
    ): HudCaptionProvider = next(current(nativeEnabled = nativeEnabled, openClawEnabled = openClawEnabled))
}
