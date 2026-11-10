# Working in M57

This file applies to the whole repository. Read a nearer `AGENTS.md` before changing files in its directory. The public [README](README.md) is an overview; code, tests, manifests, and workflows are the source of truth for behavior. [docs/status.md](docs/status.md) is a dated implementation record, not proof that a later release or a particular phone passed acceptance.

## Project contract

- M57 is a native Android 14+ app with application ID `com.lyra.blocker`. Keep the application ID and signing certificate stable for compatible upgrades. The public name may differ from the package ID.
- It recognizes a bounded catalog of AI apps, browser fields, and short-video screens. Do not describe any rule as universal detection or tamperproof enforcement.
- The Lyra web app is separate. M57 sends only a source-family hint and new-chat intent to a configured HTTPS destination. Do not add chat text, account identifiers, or provider model IDs to this handoff.
- Administrator credentials and settings stay in private app storage. Do not add a password bypass or silently change pause, maintenance, or removal behavior.
- Family filtering is opt-in and local to the device. Its VPN filters DNS; it is not a general VPN or traffic-encryption service.
- Original M57 material is AGPL-3.0-only; inherited Curbox portions keep their GPL terms. Read [LICENSING.md](LICENSING.md) and preserve SPDX headers, `NOTICE`, bundled licenses, attribution, and corresponding-source packaging.

## Where things live

| Area | Owner files |
| --- | --- |
| Android configuration | `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/` |
| Accessibility and app recognition | `LyraAccessibilityService.kt`, `BlockPolicy.kt`, `MetaPolicy.java`, `MediaPolicy.java`, `ShortsPolicy.kt` |
| Administrator UI and settings | `MainActivity.kt`, `AdminPage.kt`, `*Pages.kt`, `ProtectionStore.kt`, `PasswordCredential.java`, `ProtectionTiming.java` |
| Lyra opening and source links | `LyraLauncher.kt`, `HandoffPolicy.java`, `app/build.gradle.kts`, `docs/handoff.md` |
| Family browsing | `app/src/main/java/com/lyra/blocker/family/`; see its scoped instructions |
| Tests | `app/src/test/java/com/lyra/blocker/`, `scripts/test_release_tools.py`, `DEVICE_TESTS.md` |
| Release tooling | `scripts/`, `.github/workflows/`, `docs/releases.md` |

## Change workflow

If a requested result needs a missing asset or tool, outside help, or command
permission, tell Akshay immediately and state exactly what is needed. Do not
substitute a fragile visual or technical workaround. Continue independent work
while waiting for the needed input.

For requested work, choose the appropriate branch and commit the completed
changes without asking Akshay whether to branch or commit. Follow the repository's
deployment and release boundaries when deciding where to push.

1. Check the working tree and read the owning source, tests, and relevant docs. Preserve unrelated local changes.
2. For detection changes, include a positive case and a nearby false-positive boundary. Review supported package names, UI labels, localized/OEM behavior, and the inability to inspect hidden content.
3. For settings or lifecycle changes, trace save/load, service reconnect, screen lock, reboot, permission revocation, and explicit pause or maintenance windows.
4. For privacy or security changes, minimize collected data, keep sensitive work off the UI thread, and avoid logs containing passwords, browsing history, host queries, or private configuration.
5. Update the public explanation and `DEVICE_TESTS.md` when user-visible behavior or limitations change. Separate implemented code, CI evidence, and actual device acceptance.

## Verification

Run the narrow check for the change, then the relevant broader gate:

```sh
sh scripts/check-jvm.sh
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease --no-daemon --max-workers=2
```

The Gradle gate needs JDK 17, Android SDK platform 35, and build-tools 35.0.0. `scripts/check-android.sh` wraps the full local gate when the toolchain is set up. A JVM check does not prove Android compilation, a CI build does not prove behavior on a phone, and a debug APK does not prove release signing. State which checks actually ran. Use [DEVICE_TESTS.md](DEVICE_TESTS.md) for device acceptance.

## Distribution boundary

Pushes to `main` run Android checks; they do not publish a signed release. The prepare workflow creates a version commit and tag; the signed workflow validates the certificate, builds the APK, and packages exact corresponding source and notices. Do not create or replace a release tag, upload signing secrets, publish an APK, or change the app's signing identity unless that action is explicitly requested. Keep private signing material out of Git, logs, issues, and artifacts.
