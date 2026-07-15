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
- Collected data types selected in Play Console: Messages > Other in-app
  messages; Audio files > Voice or sound recordings; App activity > Other
  actions; App info and performance > Diagnostics. Each is optional, used for
  app functionality, not sold, and not used for advertising.
- App account creation: No. Data deletion path: Android app storage clear-data
  or uninstall for local app state; gateway/provider data is controlled by the
  configured gateway/provider.
- App access for review: Use HUD Demo Mode without a live gateway, or
  provide temporary setup code/demo gateway credentials only inside Play Console
  App access instructions.
- AI-generated assistant responses require an in-app offensive-content reporting
  path to ECO Systems LLC before the release is sent for Play review. This is a
  release gate, not a completed declaration for version 2026071500.

Before production release, verify declarations against the exact final manifest and any screenshots/video submitted in Play Console.
