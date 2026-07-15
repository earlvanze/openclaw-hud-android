# OpenClaw HUD Content Report Receiver

Deploy `report.php` to `public_html/openclaw-hud-api/report.php` and deploy
`storage.htaccess` as `public_html/.openclaw-hud-reports/.htaccess`.

The receiver accepts a bounded JSON payload from the Android app, stores one
JSON Lines record per report, attempts an email notification to the developer,
and deletes report files after 90 days. It does not include or persist request
IP addresses, device identifiers, gateway URLs, credentials, session keys, or
chat history outside the user-confirmed assistant excerpt.

The endpoint has a global daily capacity guard. Hostinger and Cloudflare may
still process normal connection metadata in their infrastructure logs.
