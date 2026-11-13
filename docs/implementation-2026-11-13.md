# M57 implementation checkpoint, 13 November 2026

The [feature audit](feature-audit-2026-11-13.md) is the current checklist for all
M57 roadmap and conversation requirements. It records owning source, partial
features and acceptance still required. This is a development preview, not a
production-readiness declaration.

Implemented in source: resumable personal/family onboarding, optional Lyra off
by default for new independent setup, shared light native navigation, Sora,
readable warm sections with moss/apricot accents, configurable focus timers,
stopwatch and Pomodoro, personal commitments, persisted setup drafts and short
presets, app/site focus lists and study allowlists, weekly routines, daily goals,
local history/export/deletion, a widget, opt-in local app-use estimates and daily
limits. Existing administrator/media/family controls and license notices remain.

Browsing additions include selectable Cloudflare malware/family upstreams only
inside the local DNS VPN, local categories/exceptions, HTTPS upstream transport
without silent unfiltered fallback, and supported browser-visible page URL rules.
VPN-capability discovery exists but is not wired as Strict-mode enforcement.
Google AI Mode has a bounded optional rule, off by default.

The latest owner-supplied transparent ring replaces the foreground and in-app
mark. Twenty-eight deterministic icon/preview outputs cover adaptive layers,
monochrome, normal/round density fallbacks, system splash and mask simulations.
The current simulations were visually inspected. Sixteen generated illustrations
and their full prompts/originals are included in corresponding source; optimized
local WebP assets are used in setup, pages and sixteen feature guides. Sora and
its OFL notice are bundled. No runtime image generation or remote art loading is
required.

## Verification boundary

Pure JVM checks cover timing, schedules, usage accounting, credentials,
settings/background policy, media/AI/browser/host boundaries, DNS packets and
handoff. Current core-policy commits are `3ad236e` and `3778669`.

The Android toolchain is available: JDK17, Gradle8.13, Android SDK36 and
build-tools35.0.0. Earlier SDK36 Preview builds succeeded. A previous full gate
assembled debug/release and ran their unit tests but failed one Android lint
issue in a graphic's typeface call; that issue was corrected. This does not
substitute for a new full gate on the final source.

Required final gate:

```sh
cd ~/m57
sh scripts/check-jvm.sh
python3 -m unittest discover -s scripts -p 'test_*.py'
python3 scripts/generate-icons.py --check
bash scripts/check-android.sh
./gradlew :app:assemblePreview --no-daemon --max-workers=2
```

The current build/install result is appended below after it completes. A previous
phone walkthrough did not pass: it could not establish that Preview was focused,
and the phone disconnected. No permission grants or enforcement acceptance were
inferred from that attempt.

## Still unfinished

Expanded parent Strict-mode settings/VPN/install/uninstall controls, secure
human-assisted recovery, manual focus breaks, richer multi-day app-use analytics,
optional spoken guides, broader OEM/browser acceptance, complete performance
measurements, CI expansion and final store metadata/recipe/submission are still
open or partial. Existing managed-device policies must not be called
release-ready before a tested recovery/release path exists. There is no universal
master password or unauthenticated reset shortcut.

No signed public candidate, current release upgrade, store approval, publication,
or complete [device acceptance](../DEVICE_TESTS.md) is claimed. A separate Preview
installation preserves the production package/data; it does not prove production
signing continuity.

## Current Preview build and installation

- `:app:assemblePreview` on SDK36 passed in 4m08s (38 tasks).
- All JVM policy suites and three Python release-tool tests passed again.
- The 28-output icon/digest/safe-circle check passed again.
- Installed `0.6.4-preview` (code10, target36) on Samsung SM-M135FU, Android14
  (API34), through Android package installation: **Success**.
- Android Activity Manager reported a successful cold launch of the Preview
  activity. The Preview UID's crash buffer contained no recorded fatal exception
  at the check. This is not a complete runtime or visual test.
- The production package's version and install/update timestamps were compared
  before/after and stayed unchanged. No production data was cleared and no
  accessibility, VPN, usage or device-admin permission was granted.
- Installed APK SHA-256:
  `d284a55eed9fbc5a0da570563544b576109dbaa5b84777da281620b836f40cb8`.

The installed development APK was built from the runtime/artwork now committed
in `9a774fe` and `2f25a4a`; its build-time `SOURCE_REF` was unset. It is not a
source-stamped signed release candidate. Phone interaction/enforcement and
production upgrade remain separate checks. The full gate completed in 11m17s: 33 debug and 33 release unit tests
passed; both APKs and the release bundle assembled, but debug/release lint
failed a text-layout constant. The constant is now corrected to the SDK’s
LineBreaker constant; the updated Preview build and lint passed in 5m03s. This failed gate is not
reported as a pass.

## Password feedback and visual update

The next Preview update installed and opened successfully on the same Samsung
phone, retaining Preview data and leaving production package identity unchanged.
APK SHA-256: `7d44bfe6b0aee97e6cab23c223e673dc8d8619114e53f476de2c6d7b807ab3a5`.

Added immediate inline working indicators for password verification/creation,
duplicate-submission protection, persistent errors and cooldown-button countdown.
New responsive summary tiles, focus/rest phase colors, a round index and shared
readable sections extend the original M57 UI. Decorative focus motion responds
to power, system animation and touch-exploration changes while attached.

The [component record](ui-components.md) describes the supplied-code license
boundary and original native implementations. New phone visual/password-flow
acceptance remains distinct from the successful install/launch.

## Announced ten-minute phone review

Samsung SM-M135FU / Android14, 13 November, approximately 06:14–06:24 IST.
New personal setup completed with Lyra off and accessibility declined; Home,
mode selection and running Focus were inspected as real rendered screens.
Stopwatch hid the complete duration input group. A normal session started and
finished through its confirmation. The walkthrough originally expected title
case for the native dialog's uppercase CONTINUE; the helper now handles case
and shared dialogs use consistent sentence case.

A second observed issue was automatic EditText focus scrolling Focus away from
its header. Entry focus was corrected and the installed update was checked: the
page now starts at its header. The latest successful build/install/open artifact
is `3b3fc2ef16963b46df6d412f6aecde51398e8ab11d1497260b6a123c3896438f`.
Production package identity stayed unchanged. No Android permission was granted
and no production data was cleared. The walkthrough created a short local
Preview session record.

The installed Lyra WebAPK declares `https://lyra-dev.vercel.app/chat` and root
scope. It was launched from M57 without package selection and appeared through
Chrome's SameTaskWebApkActivity, with the installed WebAPK task present. The
initial test checked foreground package too soon and did not account for that
host activity; this was a test expectation failure, not proof of failed launch.
AI redirection was restored to off afterwards. Both supported origins now have
anchored HTTPS/host/scope matching and alias-boundary policy tests. Local public
checks returned HTTP200 for lyra-dev.vercel.app; lyraedu.app was unreachable from
this machine. Domain availability is separate from matching support.

The actual Samsung App info tree exposed entity_header_title=M57 Preview and
button1/2/3=Open/Uninstall/Force stop, matching the current scoped detector.
Accessibility was off, so this does not establish live interception, uninstall
prevention or other-OEM coverage. Device-shaped positive and nearby negative
cases were added without removing the existing policy tests.

A debug walkthrough sample reported 462 frames, 41 janky frames, p50 27ms, p95
53ms and about 59,432 KiB PSS. These are uncontrolled debug observations, not a
release benchmark or an accepted performance target. Further profiling is needed.

The phone was left open on Preview Home before the announced window ended.
Screen interaction is paused for the owner's following three-hour unavailability.
Subsequent native code and build checks can continue; new Preview installation
waits for the next available phone window.

## Current illustrated Preview snapshot

The owner subsequently requested a current APK and installation, while retaining
the broader UI and wellbeing scope and deferring online recovery only. The
current source snapshot is `942794d2a7fe238022cfcb44cf43acec70283f5c`.

This snapshot includes the illustrated permission/free-software onboarding,
visual privacy explanation, live password feedback, protected password screens,
ambient artwork and shared tactile interactions. A security review also fixed
an authority boundary: an authenticated parent can edit family focus rules even
while a child's personal commitment is active. An unauthenticated child cannot;
personal commitments still lock personal rule editing. Pure policy checks cover
these separate cases.

The final `:app:testDebugUnitTest :app:lintPreview :app:assemblePreview` run
passed in 4m35s. All 35 debug unit tests passed with no failures, errors or
skips; the standalone JVM policy suites also passed. Preview lint passed with
warnings. This is not a new full release gate or phone acceptance result.

The isolated `com.lyra.blocker.preview` APK was signature-verified and packaged
with exact Git source, license notices, build evidence and SHA-256 checksums in
the ignored local artifact directory `dist/preview-2026-11-13-942794d/`.
APK SHA-256:
`2a08461fd44d82f3440e43ac1e0689e7f0e5de3a26bce262ced5a8afbd12e6ec`.
Its embedded source stamp is unset; the packaged source and BUILD.json record
the exact commit. It uses the development certificate, not release signing.

**Installation is pending:** the authorized Samsung disconnected before the
new APK could be installed, and ADB listed no device at packaging time. No
installation or launch of this snapshot is claimed. The earlier installed
Preview remains the last device-verified build. Reconnect the authorized phone
to update in place and open it; preserve Preview and production data.

The [retained wellbeing scope](wellbeing-tools.md) remains active. In particular,
phone-use summary notifications, the broader usage dashboard, expanded Strict
coverage and further visual/device acceptance remain unfinished. Notification
permission onboarding alone does not implement usage-summary notifications.
