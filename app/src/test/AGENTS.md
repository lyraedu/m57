# Android tests

Read the repository root instructions. These tests protect both intended detection and nearby false positives.

- Keep pure policy checks runnable from `scripts/check-jvm.sh` where possible; JUnit wrappers should exercise the same policy behavior in Gradle CI.
- For app/browser recognition, test a matching package or UI label and a similar nonmatching app, URL, translated/unsupported surface, or path boundary. Avoid tests that only repeat the new implementation's constants.
- For credentials, timing, DNS, and release identity, include malformed input, stale state, and recovery/failure paths. Use synthetic data; never add real passwords, browsing history, or signing material.
- JVM tests do not run Android framework services or prove a phone's accessibility tree, WebAPK behavior, VPN routing, or OEM settings. Record those results separately in `DEVICE_TESTS.md` with device and software versions.
- Run `sh scripts/check-jvm.sh` for pure checks and `./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest` when the Android toolchain is available.
