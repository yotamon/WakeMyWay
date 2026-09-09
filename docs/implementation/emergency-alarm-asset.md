# Bundled emergency alarm asset

M1 ships a tiny deterministic local WAV as the last-resort alarm sound. It is intentionally simple and loops under `AudioAttributes.USAGE_ALARM`.

This asset is **not** the final Wake My Way sonic brand. The branded Wake Motif will replace it after the reliability/device pass, but the replacement must preserve these invariants:

- packaged inside the APK
- usable before network/cloud initialization
- usable before first unlock after reboot
- no runtime download dependency
- verified during build/release
- playback owned by Active Wake Execution, not `WakeActivity`

`ToneGenerator` remains a second code fallback if the packaged asset cannot be decoded on a device.
