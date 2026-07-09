package ai.openclaw.app

object PrivacyPolicyText {
    const val TITLE = "OpenClaw HUD Privacy Policy"

    val BODY =
        """
        OpenClaw HUD connects your Android device to an OpenClaw gateway that you configure. The app stores gateway connection details locally on your device using Android encrypted storage.

        The Google Play HUD build may request microphone, notification, nearby-device, network, foreground-service, and audio-routing permissions depending on the features you enable. It does not request SMS, Call Log, camera, location, contacts, calendar, or media-library permissions.

        Notification access is optional. If enabled, notification text may be summarized locally in the app UI and sent to your configured OpenClaw gateway so your assistant can display glanceable context. Secret-shaped fields such as tokens and passwords are redacted before HUD display.

        Voice and caption features may send microphone transcripts, caption text, chat text, assistant status, and selected AirVision HUD settings to your configured OpenClaw gateway and its selected model provider for app functionality. Public gateway URLs require wss:// or Tailscale Serve. Developer/self-hosted connections may use local, private LAN, Tailnet, or emulator ws:// endpoints that you configure.

        OpenClaw HUD does not sell personal data and does not include advertising. The app does not create an OpenClaw cloud account. To delete app data stored on this Android device, use Android Settings > Apps > OpenClaw HUD > Storage > Clear data, or uninstall the app. Gateway-side accounts, logs, model-provider data, and backups are controlled by the gateway or provider you configured.
        """.trimIndent()
}
