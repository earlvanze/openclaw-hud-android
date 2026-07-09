# Data Safety Notes

Package: `ai.openclaw.app.hud`

Expected Google Play declarations for the HUD build:

- No advertising.
- No SMS, Call Log, camera, location, contacts, calendar, or media-library permissions in the HUD manifest.
- Optional microphone access for wake-word, push-to-talk, and captions workflows.
- Optional notification-listener access for user-enabled notification summaries.
- Data is sent only to the OpenClaw gateway endpoint configured by the user and any model/provider services selected through that gateway.
- Gateway setup/auth state is stored locally with encrypted Android preferences.
- Public gateway URLs require `wss://` or Tailscale Serve. The app also allows
  localhost, Android emulator, private LAN, link-local, and Tailnet-style
  cleartext `ws://` endpoints for self-hosted developer/operator workflows.
- Draft collected data types for Play Console: Audio; App activity; App info
  and performance. Each is for app functionality, is not sold, and is not used
  for advertising.
- App account creation: No. Data deletion path: Android app storage clear-data
  or uninstall for local app state; gateway/provider data is controlled by the
  configured gateway/provider.
- App access for review: Use AirVision Demo Mode without a live gateway, or
  provide temporary setup code/demo gateway credentials only inside Play Console
  App access instructions.

Before production release, verify declarations against the exact final manifest and any screenshots/video submitted in Play Console.
