# M57

M57 is a free, ad-free, open-source Android app for making room for what matters. Choose focus timers, Pomodoro breaks, app limits, routines, short-video boundaries and website filtering. Optional AI redirection can open supported AI surfaces through [Lyra](https://github.com/lyraedu/lyra). Independent use needs no Lyra account.

M57 supports Android 14 and newer. Its accessibility rules and local DNS VPN cover known apps and domains, not every screen or connection. The VPN does not encrypt general traffic, and personal-device protections cannot guarantee that M57 stays installed. See the [device checklist](DEVICE_TESTS.md) for coverage and testing limits.

## Install

Download the signed APK and corresponding source from [Releases](https://github.com/lyraedu/m57/releases). Releases contain their own feature set; the redesigned personal experience described in [current implementation evidence](docs/implementation-2026-11-13.md) is development source awaiting the full Android and device gates.

In the new setup, choose personal or family use, decide whether to enable Lyra redirection, and review access before granting it. Family setup creates an administrator password; personal setup is account-free. The timer works without accessibility, while app, video and browser enforcement need it. App-time insights and limits separately need optional usage access.

Current development builds use password setup. They do not migrate old PIN-only settings.

## Build

Use JDK 17, Android SDK platform 36 and build-tools 35.0.0.

```sh
sh scripts/check-jvm.sh
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is in `app/build/outputs/apk/debug/`. Signed releases follow the separate [release process](docs/releases.md).

## Guides

- [Device setup and acceptance](DEVICE_TESTS.md)
- [Personal experience, timing and privacy](docs/personal-experience.md)
- [Current implementation and verification](docs/implementation-2026-11-13.md)
- [Lyra handoff](docs/handoff.md)
- [Website filtering](docs/hosts-integration.md)
- [Builds and releases](docs/releases.md)
- [Licensing and attribution](LICENSING.md)

## License

Original M57 work is AGPL-3.0-only. Curbox-derived portions retain GPL-3.0-only terms; other bundled components keep their notices. See [LICENSING.md](LICENSING.md), [LICENSE](LICENSE) and [NOTICE](NOTICE).

The current [feature and release audit](docs/feature-audit-2026-11-13.md) tracks implemented source, missing features and
remaining acceptance. Development previews are separate from the stable app.
