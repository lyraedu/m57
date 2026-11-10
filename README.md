<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="M57 ring app icon" width="108" height="108" />
</p>

<h1 align="center">M57</h1>

<p align="center">
  An open-source Android companion for guided learning with <a href="https://github.com/lyraedu/lyra">Lyra</a>.
</p>

<p align="center">
  <a href="https://github.com/lyraedu/m57/actions/workflows/android-blocker.yml"><img src="https://github.com/lyraedu/m57/actions/workflows/android-blocker.yml/badge.svg" alt="Android checks" /></a>
  <a href="https://github.com/lyraedu/m57/releases"><img src="https://img.shields.io/github/v/release/lyraedu/m57?include_prereleases&amp;label=release" alt="Latest release" /></a>
  <img src="https://img.shields.io/badge/Android-14%2B-FF6B18?logo=android&amp;logoColor=white" alt="Android 14 or newer" />
</p>

<p align="center">
  <a href="#get-m57">Get M57</a> · <a href="#what-it-does">Features</a> · <a href="#build-from-source">Build</a> · <a href="#project-guides">Guides</a> · <a href="#contributing">Contribute</a>
</p>

---

M57 detects supported AI apps and selected short-video screens, then offers a route back to Lyra. An administrator can configure app rules, schedules, and optional family browsing controls on the device.

M57 targets **Android 14 and later**. It is a focus aid with an explicit detection catalog; it cannot guarantee that every AI app, browser view, DNS request, or device settings screen is covered. Review the [device acceptance checklist](DEVICE_TESTS.md) before relying on it for a particular phone or school deployment.

## What it does

| Area | Behavior |
| --- | --- |
| AI redirection | Recognizes configured apps and selected in-app AI surfaces, shows a short intervention, and opens the configured Lyra destination. |
| Short videos | Offers block or reminder modes for supported YouTube Shorts and Instagram Reels views, with additional experimental rules. |
| Family browsing | Optionally filters selected domain categories through an on-device DNS VPN and checks known browser address fields. |
| Administrator controls | Protects settings with a local password and offers rule editing, timed pauses, and device setup guidance. |
| Lyra handoff | Sends a service-family hint for a new Lyra chat; Lyra suggests a model configured on its server. No chat text or provider model ID is sent by M57. |

M57 uses Android Accessibility to recognize supported screens. The family VPN handles DNS, **not general traffic encryption**; Private DNS, browser Secure DNS, other DNS transports, and direct IP connections can fall outside it. Android permits only one VPN in a profile. Detection and background behavior depend on Android and the installed apps' UI versions. See the [coverage notes](DEVICE_TESTS.md) and [family filtering details](docs/hosts-integration.md).

## Get M57

Download a signed prerelease and its source/checksum files from [GitHub Releases](https://github.com/lyraedu/m57/releases). Read that release's notes and verify its `SHA256SUMS` and signer information before distributing it. The existing release workflow builds a signed APK with corresponding source; a successful CI build does not replace testing on your target device.

After installing, open **Set up M57**, read the accessibility disclosure, and create an administrator password. Enable the M57 accessibility service in Android settings. Sideloaded apps on Android 14 may also require **App info → Allow restricted settings** before Android lets you enable the service. Keep the password safe: M57 has no self-service password recovery.

The default destination is `https://lyra-edu.vercel.app/chat`. An administrator can set another HTTPS Lyra destination. For standalone PWA launching, install that Lyra site from Chrome, then use **Administrator access → Controls → AI redirection → PWA and advanced app rules** to find or select the installed PWA. M57 falls back to a browser when a matching PWA is unavailable. See the [handoff guide](docs/handoff.md).

## Build from source

Use **JDK 17**, **Android SDK platform 35**, and **build-tools 35.0.0**. Set `ANDROID_HOME` to your SDK directory, then run:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Pure JVM policy checks need no Android SDK:

```sh
sh scripts/check-jvm.sh
python3 -m unittest discover -s scripts -p 'test_*.py'
```

The [Android workflow](https://github.com/lyraedu/m57/actions/workflows/android-blocker.yml) runs debug/release tests, lint, and APK assembly on pushes and pull requests. Debug APKs are CI artifacts, **not signed distribution releases**. For signing continuity, release automation, and exact-source packaging, read [building and publishing](docs/releases.md). Never commit a keystore or its passwords.

## Project guides

- [Device acceptance checklist](DEVICE_TESTS.md): what to check on a real phone before deployment.
- [Lyra handoff](docs/handoff.md): destination, PWA selection, and source hints.
- [Family filtering](docs/hosts-integration.md): host sources, synchronization, and DNS/browser limits.
- [Building and publishing](docs/releases.md): signed releases and certificate continuity.
- [Licensing and attribution](LICENSING.md): component terms and corresponding-source obligations.

## Contributing

Issues and pull requests are welcome through [GitHub Issues](https://github.com/lyraedu/m57/issues) and [Pull Requests](https://github.com/lyraedu/m57/pulls). For a behavior change, describe the Android version and device, the app or browser version involved, the expected and observed behavior, and the checks you ran. Redact account details, browsing history, credentials, and private device logs. Start with [AGENTS.md](AGENTS.md) for the repository map and verification commands.

## License

Original M57 work is **AGPL-3.0-only**. Curbox-derived portions retain **GPL-3.0-only** terms; other bundled components have their own notices. See [LICENSING.md](LICENSING.md), [LICENSE](LICENSE), [NOTICE](NOTICE), and [LICENSES](LICENSES/) before redistributing a modified build. The Lyra web application is maintained in [its own repository](https://github.com/lyraedu/lyra).
