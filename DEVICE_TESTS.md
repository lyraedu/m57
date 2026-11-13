# Android 14 acceptance checklist

These are required device checks, not results already observed. Run on an
Android 14 phone/emulator and repeat core flows on the school’s OEM devices.

## Personal-experience candidate, 13 November 2026

These new source flows are unverified on a phone. The numbered historical
checklist below remains relevant to the features/settings it describes; new
personal onboarding and navigation supersede its old Home/setup expectations.
Record the actual APK commit, phone, Android/OEM build, locale and app versions.

- Fresh setup: resume every onboarding step after rotation, backgrounding and
  process recreation. Choose personal or family authority. With Lyra off, verify
  AI apps are usable and no AI handoff opens; enable it and repeat supported
  app, Meta, browser and optional Google AI Mode boundaries. Ordinary Google
  search and unselected AI Mode tabs must remain usable. Test localizations and
  explicitly record unsupported variants.
- Personal mode: no account/password prompt; timer works with accessibility
  declined. Enabling access must agree with the disclosure, then Home must
  distinguish configured permission from a connected service. Family mode must
  finish password setup before any protection/admin control is considered ready.
  Update an existing password installation and confirm credentials, cooldown,
  selections, pause and maintenance behavior remain intact.
- Timer/stopwatch/Pomodoro: check focus/break/round boundaries, short durations,
  total eight-hour validation, screen lock, service/activity/process restart,
  reboot, wall-clock changes and uncertain rollback recovery. Finish twice from
  different surfaces and verify one history entry. A break must not pause family
  DNS, video rules, daily limits or an overlapping routine.
- Commitment mode: no early finish; personal protection settings and boundary
  changes stay unavailable until completion. Stopwatch does not offer commitment.
  Android system navigation, calls and deliberate OS permission/revocation paths
  must remain available. Family administrator authority must not be acquired by
  starting, finishing or exporting a personal session.
- App picker: search, selection across queries, no matching apps, 60-row bound,
  uninstalled apps, work profiles and changed default launcher/dialer. Never hold
  M57, Settings, the active launcher or emergency calling. Check focus rules
  immediately after edits and on reconnect, while preserving ordinary messaging
  when only short-video surfaces are selected.
- Websites: hold/allow subdomains, reject lookalikes, keep ordinary browser text
  and URL editing usable. Study-site exceptions must not override family DNS or
  browser rules. Record hidden URL, unsupported browser and whole-browser-list
  limits. Test overlapping routine and timer scopes.
- Usage/limits: explicitly agree, deny/revoke usage access, disable local
  processing, cross local midnight/timezone/DST, screen off, switch activities,
  split screen and fail/empty a query. Missing/stale/incomplete data must never
  become fabricated limit enforcement. Measure actual sampling/overlay delay
  and battery cost. Check all usage work stays off the UI thread.
- Routines: selected weekdays, overnight tail/end exclusivity, overlapping
  intervals, timezone/DST, disabled/deleted routines, device idle, reboot and
  disconnected accessibility. Settings must describe the actual connection
  requirement; no exact-alarm promise is made.
- History: 500-record bound, completion/early end, seven-day view, end-day
  crediting, JSON export/cancel/write failure, deletion and low-storage errors.
  Check no passwords/settings or raw Android events enter the export. Already
  exported files remain under the user's document-provider control.
- UX: five personal tabs, three protected tabs, Android Back, keyboard, 320dp
  width, landscape/tablet, 200% font scaling, TalkBack, battery saver, disabled
  system animations and in-app Reduce motion. Verify previews explain controls,
  do not trap gestures, and never claim to be enforcement tests.
- Widget: add, resize, remove/re-add, open Focus, session completion, restart and
  delayed Android updates. Its saved progress is a snapshot, not a live countdown.
- Branding: compare circle/squircle/rounded/teardrop mask simulations with the
  actual launchers on Samsung, Pixel, Xiaomi/Redmi/Poco, Oppo/OnePlus/Realme and
  Vivo devices. Verify uncut ring, opaque edge, density scaling, themed icons,
  badge placement, app drawer/folders, App info, recents and system splash.
  Cold/warm starts must have no forced delay or duplicated splash Activity.

## Observed Preview installation, 13 November 2026

Current artifact: `0.6.4-preview`, code10, target36, package
`com.lyra.blocker.preview`. Samsung SM-M135FU, Android14/API34. APK SHA-256:
`d284a55eed9fbc5a0da570563544b576109dbaa5b84777da281620b836f40cb8`.

Package installation returned Success and Activity Manager reported a successful
cold launch. The checked Preview crash buffer had no recorded fatal exception.
Production M57 version/install timestamps were unchanged; no production data
clear, Android permission grant or managed enrollment occurred. This passes only
installation/startup, not the new-flow visual/enforcement checklist above.
See the [complete requirement audit](docs/feature-audit-2026-11-13.md) and
[implementation record](docs/implementation-2026-11-13.md).

A Preview-only walkthrough harness is available for an explicitly selected test
device/emulator. It resets **only Preview**, never grants Android permissions,
and checks the visible app before acting. It has been syntax-checked but its
full walkthrough has not passed on the physical phone:

```sh
source ~/.config/m57/android-env.sh
python3 scripts/device-smoke.py --serial DEVICE_SERIAL --reset-preview
```

Do not run this while the owner is reviewing Preview or has valuable Preview
data. Real permission/enforcement acceptance is a separate manual flow.

## Historical protection checklist

Current 0.6.0 target: Samsung Galaxy M13 4G, One UI 6, English (UK). No checks
below have yet been run on that device. Version 0.1–0.3 update checks are
historical baselines; the current password-migration checks are listed below.

1. Install the APK, confirm disclosure, set a 12–128 character password and enable accessibility.
   Confirm the home screen shows protection enabled. Canceling consent or password
   setup must leave protection inactive.
2. Open ChatGPT and Gemini from launcher, recents, notifications and assistant
   shortcuts. Confirm the Lyra shield covers the app, a tap redirects immediately,
   and the default five-second countdown redirects once. Repeat rapid switches.
3. Install Lyra as a Chrome PWA. Verify redirection opens the installed standalone
   PWA at `/chat`; test explicit PWA-package configuration, uninstall the PWA,
   and verify Chrome fallback. Repeat with Chrome disabled and another browser.
   With no handler installed, verify the shield stays with retry instructions.
4. In WhatsApp and WhatsApp Business, open Meta AI and verify the redirect.
   Check home-screen search, shortcut, ordinary chats, a message containing
   `Meta AI`, quoted messages, contact names and group names. Record actual
   resource IDs/locales. Inline `@Meta AI` is not covered by the initial rule.
5. Open Gemini through the Google app and overlay/assistant invocation. Confirm
   the supported header is exposed and detected. Search Google for `Gemini` and
   verify ordinary search results remain usable.
6. Open the catalog AI domains in Chrome (including a subdomain); verify blocking.
   Open Lyra and lookalike URLs such as `chatgpt.com.example.com`; verify no
   block. Check URL editing, keyboard, tab switching and private browsing.
7. During countdown, go Home, press Back, switch apps, rotate, open quick settings,
   turn the screen off and unlock. No stale redirect should occur outside the
   blocked context. Reopening a blocked app must show a new countdown.
8. Pause with the administrator password. Check all three presets, a custom
   future time and an indefinite pause. Verify automatic resumption without a
   new foreground event, after service restart and after phone reboot. An
   indefinite pause must stay paused until an administrator resumes it. Verify
   cancelling the custom picker does not change the schedule. Same-boot clock
   changes must not extend/shorten elapsed waits; check the documented reboot
   dependence on wall time separately.
9. Enter five wrong passwords; verify a 30-second wait, including after reopening
   the activity. Verify escalation at ten/fifteen failures and the cap. Background
   the app while verification or password creation is running: returning
   must not unlock settings. Confirm URL/rule edits, pause and policy controls
   cannot be reached using arbitrary launcher intent extras or saved state.
10. Restart the phone with accessibility enabled; verify service rebind and normal
    blocking after unlock. Check OEM battery restrictions, screen readers,
    large text, landscape, display scaling, split screen, keyboards and navigation.
11. Enable ordinary administrator protection and verify the removal confirmation.
    Verify the documented Android Settings revocation path remains available.
12. On an eligible test device only, provision device ownership, apply managed
    protection, then verify uninstall/force-stop/clear-data restrictions. Use the
    password to pause and release policies/ownership; verify uninstall works afterward.
    Recovery is not implemented; validate a deliberate recovery plan before production enrollment.
13. Measure APK size, idle CPU/battery and event processing on the actual device.
    No size, latency, reboot-resilience or tamper-resistance figures are claimed
    until these measurements and checks have been completed.
14. Select Reminder mode, then open the native YouTube Shorts viewer. Verify the glass reminder appears,
    waiting at least 20 seconds does not redirect, and Dismiss reminder keeps the current
    video playing. Swipe through several Shorts: no repeated popup. Leave the
    viewer for at least two seconds, return, and verify a new reminder. Check
    YouTube Home, search results and ordinary videos remain unaffected. Repeat
    on supported ReVanced/Morphe builds and Chrome `/shorts/` URLs. Leave Shorts
    must perform Back only on a deliberate tap and never open Lyra.
15. Inspect the UI with several apps behind it, large text, landscape and a screen
    reader. Confirm warm glass, orange-only icons, neutral text and no purple/blue
    accents. Toggle battery saver/system blur while the popup is visible: the
    opaque fallback must stay readable. Reopen repeatedly and check message
    variation without a consecutive repeat. AI countdown behavior stays intact.
16. Install this update over version 0.1.0 using the same signing key. Verify PIN/settings remain intact,
    version 0.2.0 is shown, and the seven new AI packages receive redirects.
17. Update to version 0.3.0 with the same signing key. Verify **M57** appears in
    the launcher, App info and Accessibility settings, while Lyra remains the
    PWA destination. Existing PIN/settings must remain intact.
    Check that the launcher uses the M57 ring icon and the M57 screens and
    overlays show the same ring without a Lyra symbol.
18. With accessibility/protection enabled, long-press M57 → App info. Verify the
    page closes and M57's administrator guidance opens. Repeat several times,
    including through Settings → Apps. Other apps' App info, the app list/search,
    and other services' accessibility controls
    must stay usable. Record the OEM's actual header/action IDs if detection fails.
19. Use **Administrator access → Settings → Device access → Open App info · allow 60 seconds**. Verify App
    info is usable and AI rules still work. Stay there past 60 seconds: the guard
    should resume. Repeat after service reconnection and reboot. Test the
    chosen protection pause and the maintenance window after releasing removal protection.
20. Install Lyra through Chrome, then use M57's PWA guide and detector. Check
    that selecting a detected package requires Save settings, blank means automatic,
    and an uninstalled/missing PWA shows clear instructions with Chrome fallback.
    Test a different saved Lyra host, multiple matching PWAs and a plain shortcut.
21. Verify the public Home has no administrator dock. After unlock, check all
    three labelled tabs, selected states, search (including multiword searches),
    category breadcrumbs, Android Back and Lock & leave. Scroll down/up and
    near the bottom; check dock visibility, keyboard overlap, touch exploration,
    large fonts and landscape. Planned categories must not imply filtering is active.
22. Update a current password-based installation with the same signing key.
    Verify the administrator password still unlocks, its cooldown still works,
    and destination/package/countdown settings survive. PIN-only prerelease
    builds are unsupported; test a fresh setup after clearing that app's data.
23. On each of the six supported manufacturers, check **Allow background running**
    opens Android's real battery-optimisation request. Deny/dismiss it and ensure
    the UI still says it is not allowed. Grant it and verify the PowerManager
    status after returning/re-unlocking. Check already-exempt devices, unavailable
    OEM activities and standard settings fallbacks. Extra startup controls must
    not be marked granted just because their screen opened. Verify collapsed
    Troubleshooting opens the matching dontkillmyapp.com guide. On other brands,
    the manufacturer-specific section must not appear.
24. Open M57's own accessibility detail page from Samsung Installed apps, Settings
    search and any available shortcut. Verify the narrow guard returns to M57's
    administrator guidance, including repeated visits. Initial enablement,
    authorised 60-second maintenance and a protection pause must remain usable.
    Check the guard resumes after expiry while the page stays open. Inspect the
    actual Settings package/resource IDs if detection fails; never mark this
    device supported based solely on synthetic snapshot tests. Other services,
    Installed apps lists, notifications settings and other App info pages must
    remain usable. Record any OS revocation/bypass cases rather than claiming
    universal prevention.
25. Enable family categories one at a time. Check initial downloads, manual sync,
    category counts/examples, last successful time, HTTPS mirror fallback and
    the approximate daily job. Simulate unavailable/invalid/partial/shrunken
    downloads and confirm the last good cache remains active after restart.
    No remote URL being configured is proof its mirror works on the phone.
26. Start the family VPN through Android approval. Deny and grant it separately;
    only a running service should show active. Check the foreground notification,
    IPv4/IPv6 A/AAAA/HTTPS queries, allowed DNS replies, NXDOMAIN for listed
    domains and SERVFAIL on resolver timeout/offline/overload. Check Wi-Fi/mobile
    changes, DNS answers larger than the MTU, service death, boot, pause/resume
    and VPN revocation/replacement. Verify ordinary web traffic is not routed
    to a remote M57 server. TCP DNS, encrypted/direct/cached paths are explicit
    limits; do not claim they are covered by a UDP packet unit test.
27. Enable the separate browser guard and visit synthetic/custom blocked domains
    in Chrome, Samsung Internet and each claimed browser family. Verify only
    trusted address fields trigger it, leaving Home/search/text unaffected.
    Check compassionate adult guidance, Back/Home/administrator actions, screen
    lock and URL-hidden/private modes. Test supported packages/resource IDs on
    actual versions before claiming complete coverage.
28. Verify allow/block exception precedence across VPN and browser layers and
    category changes. Turning off a category must remove its rules, while other
    enabled categories remain. Test subdomain boundaries, IDNs, no-list/partial
    list states, cancelled VPN approval and no existing VPN consent. Record the
    one-VPN-per-profile tradeoff. Check no query/domain-visit history is written
    to logs or app storage, and that source/license metadata is available.

29. Check Block (the default) and Reminder modes separately on YouTube/ReVanced/
    Morphe and Instagram full-screen viewers. Block must offer no dismiss path;
    failed Back must not remove the shield. Test native/browser Reels and feed
    previews. Verify Snapchat chat, Facebook normal feeds and TikTok recognition
    across actual versions, and acknowledge Josh's whole-app chat/call boundary.
30. Disable/restore each built-in AI app/domain rule, save custom rules and verify
    immediate enforcement. Both app and website switches should visibly refresh
    after restoring defaults. Keep browsers, M57 and the Lyra PWA usable.
    Check the Meta master/service switches and ordinary messages/feeds.
31. Redirect ChatGPT and Gemini to a new Lyra chat and verify the current available
    model suggestion. A manual model choice and a first send must win over a late
    response. Open an existing chat with hint parameters: its model must remain.
    Test login return, invalid hints, unavailable endpoint/provider, school hint
    configuration, different-school PWAs and launcher-only WebAPK discovery.
32. Open the M57 repo and its raw source in a supported browser; verify the path
    guard, adjacent repositories, the Settings toggle, pause and 60-second
    authorised source-review window. About must show the build's exact source
    commit and complete AGPL/GPL/Apache/StevenBlack notices.
33. Build a signed candidate through the tag workflow, verify its public signing
    fingerprint/checksums and source archive, and install over the previous APK
    using the same signing identity. Record actual CI/device results before
    describing a build as accepted. Recovery remains excluded.
