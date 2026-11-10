# M57

M57 is a free, ad-free, open-source Android companion for more intentional technology use. It can redirect supported AI apps toward [Lyra](https://github.com/lyraedu/lyra), interrupt selected short-video screens, and filter chosen website categories. The AI redirect is optional; M57's other controls can work without a Lyra account.

M57 supports Android 14 and newer. Its accessibility rules and local DNS VPN cover known apps and domains, not every screen or connection. The VPN does not encrypt general traffic, and personal-device protections cannot guarantee that M57 stays installed. See the [device checklist](DEVICE_TESTS.md) for coverage and testing limits.

## Install

Download the signed APK and corresponding source from [Releases](https://github.com/lyraedu/m57/releases). Open M57, follow **Set up M57**, choose an administrator password, and grant only the Android permissions needed for the controls you enable.

Current development builds use password setup. They do not migrate old PIN-only settings.

## Build

Use JDK 17, Android SDK platform 35 and build-tools 35.0.0.

```sh
sh scripts/check-jvm.sh
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is in `app/build/outputs/apk/debug/`. Signed releases follow the separate [release process](docs/releases.md).

## Guides

- [Device setup and acceptance](DEVICE_TESTS.md)
- [Lyra handoff](docs/handoff.md)
- [Website filtering](docs/hosts-integration.md)
- [Builds and releases](docs/releases.md)
- [Licensing and attribution](LICENSING.md)

## License

Original M57 work is AGPL-3.0-only. Curbox-derived portions retain GPL-3.0-only terms; other bundled components keep their notices. See [LICENSING.md](LICENSING.md), [LICENSE](LICENSE) and [NOTICE](NOTICE).
