# M57 documentation

Read the root instructions. Keep the README short and current; use these guides for detailed behavior, operators, and dated evidence.

- `handoff.md` describes the M57-to-Lyra protocol and limits. Keep it aligned with Android `HandoffPolicy.java`/`LyraLauncher.kt` and Lyra's companion implementation.
- `hosts-integration.md` owns family-list sources, update rules, licenses, and VPN/browser coverage. Distinguish DNS filtering from full traffic inspection.
- `releases.md` owns signing, versioning, GitHub Actions, and corresponding-source instructions. Keep example versions illustrative or current; never imply that a debug build is a signed release.
- `status.md` is dated historical evidence. Add a dated update or link to a verified run for newer claims instead of silently rewriting older observations.
- `releases/` notes describe shipped tags. Do not edit a published tag's notes to conceal a later fix; write a new version.
- For user-facing claims, separate source implementation, unit/CI results, and real-device acceptance. Link [DEVICE_TESTS.md](../DEVICE_TESTS.md) for unverified device behaviors.
