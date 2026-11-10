# Android implementation

Read the repository root `AGENTS.md` first. This directory owns runtime behavior; Android framework boundaries and device variants matter more than a passing pure JVM test.

## Boundaries

- Keep `LyraAccessibilityService.kt` focused on supported packages and visible node metadata. Do not read message bodies or broaden rules from a single label without a false-positive test. Accessibility events, window state, screen-off cancellation, and service reconnect all affect interventions.
- `BlockPolicy.kt`, `MediaPolicy.java`, `MetaPolicy.java`, `ShortsPolicy.kt`, and `HandoffPolicy.java` hold reusable recognition and URL rules. Prefer testing pure policy boundaries there before changing service wiring.
- `ProtectionStore.kt` is persisted local state. Preserve current password and control settings when adding keys; unsupported prerelease PIN-only settings are not a compatibility target. Password derivation and verification must stay off the UI thread; do not weaken `PasswordCredential.java` or cooldown rules.
- Administrative changes must respect authentication, timed pauses, maintenance windows, and Android's explicit accessibility/VPN/device-admin consent. Never implement a hidden override or bypass Android permission prompts.
- `LyraLauncher.kt` must validate the configured HTTPS destination and installed PWA scope. A browser fallback is valid; do not hardcode generated WebAPK package IDs or imply that a source-app hint grants a model.
- Keep behavior readable and accessible when blur is unavailable, touch exploration is enabled, the keyboard is open, or the app returns from the background.

## Verification

Add tests beside the relevant policy in `app/src/test/java/com/lyra/blocker/`. For UI, accessibility, WebAPK, device-owner, or OEM-specific changes, record the device/Android/app versions and actual observations in `DEVICE_TESTS.md`; unit tests cannot replace them. Run the root JVM and Gradle gates when the toolchain is available.
