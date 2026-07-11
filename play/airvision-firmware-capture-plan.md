# AirVision M1 Firmware Protocol Capture Plan

Generated from `AirVisionFirmwareFeature` in `AirVisionUsbController.kt`. Run `node scripts/render-airvision-firmware-capture-plan.mjs --check` before capture work or release prep.

## Capture Setup

- Keep the AirVision M1 connected to the Windows/Cyber machine running the ASUS AirVision app when capturing vendor traffic.
- Capture USB traffic while changing one AirVision setting at a time through the probe values below.
- Export the Android HUD AirVision diagnostics JSON before and after the capture session, then compare its readable/writable HID report-path summaries against the USBPcap endpoints.
- Keep Android firmware writes disabled until the vendor report ID, payload length, endpoint, sanitized payload summary, and checksum behavior are validated from captures.
- Do not commit raw USB serial numbers, raw private capture payloads, or temporary review/demo credentials.

## Firmware Write Gate

- Status: `read_only_capture_pending`
- Summary: firmware writes: read-only; 0/9 validated captures, 0 protocol-ready, 9 blocked
- Firmware writes enabled: no
- Validated captures: 0/9
- Protocol-ready captures: 0/9
- Blocked features: 9
- Live M1 required before writes: yes
- Explicit user confirmation required: yes
- Next step: Capture and validate ASUS HID report payloads on Windows/Cyber for each Windows-style control.

## Capture Acceptance Criteria

- Record the exact Windows write report ID, payload length, endpoint, and sanitized payload summary for each feature before Android sends any vendor report. Keep raw bytes only in private capture files.
- Record any matching readback report ID, payload length, endpoint, timing, and sanitized payload summary after each Windows setting change.
- Identify checksum, framing, sequence, or padding behavior from at least two distinct probe values for the same feature.
- Confirm the ASUS app UI and the M1 visible state changed as expected for every captured probe value.
- Keep Android enablement as `blocked` until write, readback, checksum/framing, and visible-state evidence agree.

## Probe Matrix

| Feature | Raw key | Android status | Probe values |
| --- | --- | --- | --- |
| View Mode | `view_mode` | per-mode HUD profile active | working -> gaming -> infinity |
| Brightness | `brightness` | software HUD dimming active | 20% -> 50% -> 80% |
| Screen distance | `screen_distance` | virtual HUD distance scaling active | 50 cm -> 100 cm -> 150 cm |
| IPD | `ipd` | profile calibration stored | 60 mm -> 67 mm -> 72 mm |
| Splendid | `splendid` | HUD color preview active | standard -> theater -> eye_care |
| Blue Light Filter | `blue_light_filter` | Eye Care warm overlay active | 0% -> 50% -> 100% |
| Motion Sync | `motion_sync` | profile preference stored | off -> on |
| Light Load Mode | `light_load_mode` | low-overhead HUD profile active | off -> on |
| 3D Mode | `3d_mode` | profile preference stored | off -> on |

## Per-Feature Capture Checklist

### View Mode

- Raw key: `view_mode`
- Probe values: working -> gaming -> infinity
- Capture notes:
  - Start a fresh USB capture.
  - Change View Mode through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Brightness

- Raw key: `brightness`
- Probe values: 20% -> 50% -> 80%
- Capture notes:
  - Start a fresh USB capture.
  - Change Brightness through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Screen distance

- Raw key: `screen_distance`
- Probe values: 50 cm -> 100 cm -> 150 cm
- Capture notes:
  - Start a fresh USB capture.
  - Change Screen distance through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### IPD

- Raw key: `ipd`
- Probe values: 60 mm -> 67 mm -> 72 mm
- Capture notes:
  - Start a fresh USB capture.
  - Change IPD through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Splendid

- Raw key: `splendid`
- Probe values: standard -> theater -> eye_care
- Capture notes:
  - Start a fresh USB capture.
  - Change Splendid through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Blue Light Filter

- Raw key: `blue_light_filter`
- Probe values: 0% -> 50% -> 100%
- Capture notes:
  - Start a fresh USB capture.
  - Change Blue Light Filter through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Motion Sync

- Raw key: `motion_sync`
- Probe values: off -> on
- Capture notes:
  - Start a fresh USB capture.
  - Change Motion Sync through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### Light Load Mode

- Raw key: `light_load_mode`
- Probe values: off -> on
- Capture notes:
  - Start a fresh USB capture.
  - Change Light Load Mode through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

### 3D Mode

- Raw key: `3d_mode`
- Probe values: off -> on
- Capture notes:
  - Start a fresh USB capture.
  - Change 3D Mode through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

## Capture Result Template

| Feature | Write report ID | Write payload summary | Readback report ID | Readback payload summary | Checksum/framing notes | ASUS UI + M1 visible confirmation | Android enablement decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| View Mode | pending | pending | pending | pending | pending | pending | blocked |
| Brightness | pending | pending | pending | pending | pending | pending | blocked |
| Screen distance | pending | pending | pending | pending | pending | pending | blocked |
| IPD | pending | pending | pending | pending | pending | pending | blocked |
| Splendid | pending | pending | pending | pending | pending | pending | blocked |
| Blue Light Filter | pending | pending | pending | pending | pending | pending | blocked |
| Motion Sync | pending | pending | pending | pending | pending | pending | blocked |
| Light Load Mode | pending | pending | pending | pending | pending | pending | blocked |
| 3D Mode | pending | pending | pending | pending | pending | pending | blocked |

## Generated Metadata

- Feature count: 9
