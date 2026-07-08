# Data Safety Notes

Package: `ai.openclaw.app.hud`

Expected Google Play declarations for the HUD build:

- No advertising.
- No SMS, Call Log, camera, location, contacts, calendar, or media-library permissions in the HUD manifest.
- Optional microphone access for wake-word, push-to-talk, and captions workflows.
- Optional notification-listener access for user-enabled notification summaries.
- Data is sent only to the OpenClaw gateway endpoint configured by the user and any model/provider services selected through that gateway.
- Gateway setup/auth state is stored locally with encrypted Android preferences.

Before production release, verify declarations against the exact final manifest and any screenshots/video submitted in Play Console.
