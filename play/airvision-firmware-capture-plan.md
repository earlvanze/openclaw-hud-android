# AirVision M1 Firmware Protocol Capture Plan

Generated from `AirVisionFirmwareFeature` in `AirVisionUsbController.kt`. Run `node scripts/render-airvision-firmware-capture-plan.mjs --check` before capture work or release prep.

## Capture Setup

- Keep the AirVision M1 connected to the Windows/Cyber machine running the ASUS AirVision app when capturing vendor traffic.
- Capture USB traffic while changing one AirVision setting at a time through the probe values below.
- Export the Android HUD AirVision diagnostics JSON before and after the capture session, then compare its readable/writable HID report-path summaries against the USBPcap endpoints.
- Keep Android firmware writes disabled until the vendor report payload, report ID, length, and checksum behavior are validated from captures.
- Do not commit raw USB serial numbers, raw private capture payloads, or temporary review/demo credentials.

## Probe Matrix

| Feature | Raw key | Android status | Probe values |
| --- | --- | --- | --- |
| Brightness | `brightness` | software HUD dimming active | 20% -> 50% -> 80% |
| Screen distance | `screen_distance` | virtual HUD distance scaling active | 50 cm -> 100 cm -> 150 cm |
| IPD | `ipd` | profile calibration stored | 60 mm -> 67 mm -> 72 mm |
| Splendid | `splendid` | HUD color preview active | standard -> theater -> eye_care |
| Blue Light Filter | `blue_light_filter` | Eye Care warm overlay active | 0% -> 50% -> 100% |
| Motion Sync | `motion_sync` | profile preference stored | off -> on |
| 3D Mode | `3d_mode` | profile preference stored | off -> on |

## Per-Feature Capture Checklist

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

### 3D Mode

- Raw key: `3d_mode`
- Probe values: off -> on
- Capture notes:
  - Start a fresh USB capture.
  - Change 3D Mode through the probe values in order, pausing briefly after each value.
  - Save the capture with the feature key and value sequence in the filename.
  - Record any matching writable HID report path from the Android diagnostics export.

## Generated Metadata

- Feature count: 7
