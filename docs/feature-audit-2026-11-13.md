# M57 feature and release audit, 13 November 2026

This is the current implementation checklist for the native app. It reconciles the M57 workstream, the older requirement IDs and the owner's subsequent requests with the owning source. A row marked **Source** has implementation, not full device acceptance. **Partial** means a specified part is missing. **Open** means the feature or required evidence is not delivered. No row makes the entire app production-ready.

The install requested today is the separate **M57 Preview** application (`com.lyra.blocker.preview`). It keeps the production application's identity, data, administrator and Android permissions separate. Preview is for reviewing current behavior, not a stable signed public release.

## Coverage and decision precedence

Reviewed in Lyra: `docs/roadmap/workstreams.md` (M57.1–13), `product-vision.md`, `experience.md`, `engineering-quality.md`, `coverage.md`, `decisions.md`, `release-tracker.md`, `launch-2026-11-18.md`, the November 12 requirements/public-package briefs, and `lyra-todo.md` sections G–L and its later repository/distribution additions. Public-page changes belong to Lyra; this audit does not claim to have revalidated that website.

Also retained: every app request in the current conversation, including the latest ring, light shared navigation, moss/apricot accents, readable sections, expressive Focus interactions, About destination, usage access, Strict-mode bypass controls, assisted recovery, imagery, tests, incremental commits and installation.

Latest instructions supersede the older exclusion of recovery: recovery is now required. Unsupported PIN migration remains deliberately removed. New onboarding explicitly chooses personal/family authority and optional Lyra, replacing the older home-first proposal. Native accessibility overlays remain the chosen intervention API; a second draw-over-apps permission is contingent on an actual tested need. Requested guarantees across every OS/browser/VPN cannot be claimed from a normal Android app; supported coverage needs measured evidence.

## Identity, visual design and onboarding

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| UI-01 / M57.2, M57-01: warm, polished native experience | **Source.** `LyraUi`, `PageShell`, native page classes; Sora, ivory/sand, terracotta, apricot and restrained moss. | Review every page on phone, tablet, large text and TalkBack with a nontechnical parent and personal user. |
| UI-02 / M57-02–03: clear structure and connection health | **Source.** Five personal destinations; protected parent Overview/Controls/Settings; Home distinguishes configured permissions from a connected service. | Test disconnected/reconnecting service, permission denial and pauses. |
| UI-03 / M57-05: beautiful shared light dock | **Source.** `NavigationDock` shared by `PersonalDock` and `AdminDock`; icons, labels, selected tint and scroll/inset/keyboard handling. | Verify narrow screens, large text, keyboard, touch exploration and hidden/revealed state. |
| UI-04: supplied ring replaces old foreground and in-app mark | **Source.** `branding/source/foreground.png` is the latest supplied transparent ring. Deterministic generator produces adaptive/background/monochrome layers, five density fallbacks, normal/round icons, in-app mark and system splash. | Mask simulations checked; actual Samsung/Pixel and other launcher/themed-icon checks remain. No universal OEM-shape claim. |
| UI-05: use supplied desktop background and icon reference | **Source.** Originals retained in `branding/source`; complete reference is separate from adaptive layers. | Confirm installed icon with owner. |
| UI-06: many original visual explainers | **Source.** Sixteen generated scenes, source PNGs and full prompts in `branding/illustrations`; optimized local WebP assets, worker decode and bounded cache; sixteen `FeatureGuide` lessons. | Contact sheet inspected; phone loading, memory and narrative comprehension still need acceptance. |
| UI-07: expressive motion and smooth Focus | **Partial.** Press/ripple, page entrances, disclosures, dock transitions, focus orbit/ring, mode transitions and preset feedback. `Motion` respects system settings, reduced motion, battery saver and touch exploration. | Further running-session choreography, live power-setting changes, performance measurements and owner visual review. Lots of motion must not obscure controls or keep hidden screens busy. |
| UI-08: readable sections with a little more color | **Source.** Shared section headings, moss/apricot icon tiles and Focus grouping; readable dark accent text instead of low-contrast brand text. | Contrast/large-text review on actual backgrounds; focus-field error placement and long translations. |
| UI-09: About opens requested page | **Source.** `UiLinks.ABOUT` is `https://lyra-dev.vercel.app/m57`; separate native source/build/license route. | Verify browser opening and offline failure handling. |
| SET-01 / M57.2, SETUP-01: resumable, understandable setup | **Source.** Six-step `OnboardingPages`, saved progress, sticky actions; separate parent password and existing permission sequence. | Process death, rotation, Android Settings return, Back and interrupted setup. Returning parent authority still requires password. |
| SET-02 / M57.4: personal and parent use share components | **Source.** Explicit use-case choice; account-free personal tools; authenticated family changes; no intent grants authority. | Full two-persona walkthrough, existing-password upgrade and attempted authority downgrade. |
| SET-03: Lyra optional and off for new independent setup | **Source.** Explicit switch; AI handoffs disabled when off; timer/video/routine/limit/family controls independent. | Offline first-run, no-browser device, existing settings preservation and all controls with Lyra off. |
| SET-04 / SETUP-02, M57-04: strong password form | **Source.** Full-page setup/confirmation/strength feedback; `PasswordCredential` work off UI. Existing password settings preserved. | Keyboard, paste/password-manager, cancellation, storage failures and device accessibility. |
| SET-05 / SETUP-03, SCREEN-07: explain only needed permissions | **Source.** Accessibility for interventions, optional usage for limits/insights, separate VPN/admin/background explanations. `OverlaySurface` uses accessibility windows; no redundant draw-over-apps request added. | Overlay reliability versus tested need for a fallback; explicit consent, denial/revocation, Android restricted-settings flow. |
| SET-06 / SETUP-04: removal limits explained | **Source.** Normal-device admin friction separated from intentionally managed device-owner policy. | Managed setup must remain unreleased until secure recovery/release is accepted. |
| SET-07 / SETUP-05,08: native background and OEM help | **Source.** `BackgroundAccess`, `BackgroundHelp`, six requested manufacturer families, actual exemption-state checks, startup settings/fallbacks and secondary troubleshooting links. | Samsung M13 first; Xiaomi/Redmi/Poco, Oppo, Vivo, Realme, OnePlus later. Opening a page is not success. |
| SET-08 / SETUP-06: explain inspectable FOSS source | **Source.** Native build/source/license notices and installed-release source-ref support. No security certification claim. | Candidate must embed the exact committed source reference. |
| SET-09: warm guidance and optional spoken help | **Partial.** Original written/illustrated guides exist. No read-aloud/voice feature exists. | If added, user-initiated offline-capable TTS, no microphone/private-content collection; test availability and lifecycle. |

## Focus, wellbeing and local data

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| FOC-01 / M57.12: timer | **Source.** `FocusClock`, `FocusStore`, `PersonalPages`; 1–480 minutes, persisted session, early finish for normal sessions. | Real-time/device/background/process-death checks. |
| FOC-02: stopwatch | **Source.** Count-up mode, eight-hour bound, no commitment option. | Screen lock, process/reboot recovery and exact cap. |
| FOC-03: configurable Pomodoro | **Source.** Focus/rest/round choices, eight-hour total; pure phase/credit boundaries tested. New form requires 2–12 rounds. | Actual phase transitions, app release/re-block and final-round completion on phone. |
| FOC-04: personal breaks | **Partial.** Scheduled Pomodoro breaks release the session's focus list; family protection is never paused by them. | Manual pause/timed-break controls and break accounting are not implemented. An overlapping routine/limit still applies. |
| FOC-05: clear setup, presets and preserved choices | **Source.** `FocusSetup`, `FocusModePicker`, 15/25/45-minute presets, live total preview, grouped fields and saved local draft. Hidden inputs hide their labels. | Switch every mode, invalid/empty values, eight-hour bounds, leave/return and rotation. |
| FOC-06: focus app list | **Source.** Async searchable installed-app picker; selected apps held during focus/routines. Essential/system/own apps excluded. | Changed launcher/dialer and emergency-call accessibility; picker completeness and service revocation. |
| FOC-07: focus sites and study allowlist | **Source.** Domain/subdomain boundaries, local allow-before-deny; supported visible browser addresses only; study mode handles visible IP literals conservatively. | Hidden/incognito/custom browser fields, whole-browser selection, overlapping family rules. |
| FOC-08: personal commitment | **Source.** No early finish; personal protection edits wait; family authority remains independent. | Restart/clock boundaries. Commitment does not remove Android controls or guarantee tamper resistance. |
| FOC-09: recurring routines | **Source.** Up to 30 local weekly routines, day/time editor, overnight and DST policy tests. | Local timezone changes, overlapping routines, suspension/reconnect and reboot. No exact-alarm guarantee. |
| FOC-10: daily goals | **Source.** Editable 5–480-minute goal, saved focus totals and progress. | Midnight/clock changes and long-session attribution. No punitive streaks. |
| FOC-11: widget | **Source.** Goal/today snapshot and explicit Focus shortcut; periodic and session-change updates. | Launcher add/resize, stale state, reboot; no continuous countdown promise. |
| FOC-12: usage permission and app-time analytics | **Partial.** Explicit opt-in plus Android Usage Access; off-main bounded events processing, today app-time estimates. No raw event persistence/upload. | Seven-day app-time analytics, revocation/locked-user/multi-window acceptance and comparison to Android estimates. Seven-day **focus** chart already exists. |
| FOC-13: daily app allowances | **Source.** Local 1–1440-minute limits, reset at local midnight, service sample freshness checks. | Approximate 15-second enforcement, missing events/revocation, family edit authority, midnight and clock changes. Never invent usage when unavailable. |
| FOC-14: local history and seven-day focus chart | **Source.** Up to 500 records; atomic finish/history write; local-day end attribution. | Crash/storage failure, duplicate completion, interrupted clock and midnight. |
| FOC-15: export and delete | **Source.** Document-picker JSON focus-history export and deletion. Export/deletion do not change active protection. | Actual document providers/errors; usage-summary export/delete controls remain to define. Clearing history must not reset app allowances. |
| FOC-16: timing and restoration | **Source with policy tests.** Monotonic within boot; wall time across reboot; backwards/uncertain recovery yields no fabricated credit. | Real reboot, changed clock, killed process, low storage and absent service. |

## AI, short videos and intervention screens

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| AI-01 / M57-10–11: independent per-app AI controls | **Source.** `AppRulesPages`, `BlockPolicy`, editable defaults/custom packages and restore behavior. | Installed supported versions and neighboring false positives. |
| AI-02 / HANDOFF-01: scoped PWA discovery/launch | **Source.** `LyraLauncher`, destination validation and manual selection; browser fallback. | Target-phone actual installed PWA, scope mismatch, uninstall, no browser and offline behavior. |
| AI-03 / HANDOFF-02–05: maintainable model/source handoff | **Source.** Only untrusted source-family/new-chat hints. No chat/account/model catalog in APK; web owns authenticated availability and manual-choice precedence. | Cross-product end-to-end validation in Lyra. APK cannot establish web entitlement/mapping correctness. |
| AI-04 / HANDOFF-06: packaged Lyra wrapper | **Open, retained future scope.** No native wrapper shipped here. | Separate product/platform/distribution acceptance. |
| AI-05 / M57.10: Google AI Mode | **Source, bounded experimental rule.** Default off; exact supported Google URLs/selected English app surfaces; ordinary search/lookalikes and duplicate-mode tests. | Google/Chrome versions, regional/localized surfaces, ordinary-search device negatives. Not universal coverage. |
| VID-01 / SHORT-01–03: Shorts/Reels block or reminder | **Source.** Separate platform controls and grouped UI; `MediaPolicy`, `ShortsPolicy`, dedicated intervention. | Fullscreen viewers versus Instagram feed, ordinary YouTube, Stories/Explore/browser versions and reminders on device. |
| VID-02 / SHORT-04: extra short-video platforms | **Source, experimental.** Collapsed extra-platform choices; bounded platform rules. | Installed Snapchat/Josh/other supported versions and false-positive reporting; no all-platform promise. |
| VID-03 / SHORT-05–06: dedicated scrolling screen | **Source.** `ShortsReminderOverlay`, stable visit message, actions separate from AI. | Timer/dismissal/block paths, screen lock, repeated events and no unrelated-app obstruction. |
| META-01 / META-01–03: grouped optional Meta AI | **Source, experimental.** Group plus per-service choices and pure policies. | WhatsApp/Instagram/Facebook supported AI versus ordinary chats/feed device matrix. |
| INT-01 / SCREEN-01–02: fullscreen intervention/countdown | **Source.** Shared `OverlaySurface`, `CountdownRing`, monotonic launch timing, blur fallback and motion policy. | Blur unavailable, battery saver, large text/TalkBack, orientation, cancellation, app switch and lock. |
| INT-02 / SCREEN-03–04: warm rotating, accurate messages | **Source.** `PopupMessages` avoids immediate repeat and keeps visit text stable; no model-equivalence entitlement promise. | Non-shaming comprehension and enough time to read. |
| INT-03 / SCREEN-05–06: missing-feature contact | **Source.** Explicit Contact navigation with topic/source hints; separate Go home. | Lyra topic selection and actual support delivery. No messages sent by the agent. |
| INT-04: separate focus/site/video/AI semantics | **Source.** Distinct overlay classes and service targets; ordinary focus intervention does not redirect into Lyra. | Precedence when rules overlap; leave/reenter and every overlay cleanup path. |

## Browsing, DNS and VPN controls

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| WEB-01 / FAMILY-02–03: independent categories | **Source.** `FamilyCatalog`, per-category page/toggle; local licensed hosts lists for configured categories. | Current downloaded sources, category independence and chosen defaults. |
| WEB-02 / FAMILY-04: sources and licenses | **Source.** Native source/license popup and bundled notices. | Verify current source URLs and license correspondence before bundling/changing lists. |
| WEB-03 / FAMILY-05–07: sync, status, mirrors, last good copy | **Source.** Manual resync, daily job, ordered HTTPS sources, validation and atomic cache retention. | Real offline/server error/shrunken/malformed responses, reboot, restrictions and stale state. |
| WEB-04 / M57.8: local VPN | **Source.** Split-route IPv4/IPv6 UDP DNS TUN, local category/allow/block rules first; explicit Android VPN consent and notification. | Physical network handoff, shutdown/revoke/replacement, packet/battery tests. No general traffic encryption. |
| WEB-05: Cloudflare malware/family modes only in VPN | **Source, new transport.** Select network DNS, malware-only or malware/adult upstream. Selected Cloudflare queries use HTTPS on underlying non-VPN network; no silent unfiltered fallback. Android global Private DNS is unchanged. | Live consented VPN queries, network failures/timeouts, privacy disclosure and performance. Provider does not supply every local category. |
| WEB-06: allow/block precedence and pause | **Source.** Explicit allow entries override local denies; selected allow queries and authenticated protection pause use network DNS. | Confirm intended exact/subdomain exceptions and parent understanding of upstream bypass. |
| WEB-07 / FAMILY-08–09: browser-visible domains | **Source.** Trusted package/resource-ID fields, host boundaries, local exceptions; edited address text not treated as a loaded page. | Supported browsers/incognito/hidden fields. All-browser goal remains unfulfilled. |
| WEB-08: full page URL blocking | **Source, new rule.** `BrowserUrlPolicy`, validated bounded page list, path/query matching, browser toggle and allow precedence. | Loaded visible address examples, nearby paths, URL editing and app/browser version matrix. No hidden browsing capture. |
| WEB-09 / FAMILY-10–11: compassionate category interventions | **Source.** Category-specific `FamilyBlockedOverlay`, adult-content guidance and protected category controls. | Age-appropriate comprehension, no shame, repeated-entry behavior. DNS denial alone cannot render a browser category page. |
| WEB-10: encrypted/private DNS, cache/direct IP, TCP | **Partial.** Limits disclosed; UDP DNS packet policies tested and selected upstream uses HTTPS. | TCP DNS is not implemented. Apps' own DoH/Private DNS, cached/direct-IP traffic and hidden URLs are not universally filtered; test actual coverage. |
| WEB-11: detect other installed VPN apps | **Partial.** `VpnApps` discovers visible apps declaring VPN service permission; pure capability policy tested. | Discovery is **not enforcement**. Runtime refresh/catalog and Strict-mode intervention are not wired. Hidden/nonstandard clients remain a limit. |
| WEB-12: all VPN opening/install/store bypass controls | **Open.** Expanded parent Strict-mode rules, including the requested Proton VPN example and supported store/install/open paths, are not implemented. | Trusted detail-page recognition, package-change handling and nearby negatives. A normal app cannot guarantee prevention of every installation/background VPN. |
| WEB-13: one active VPN conflict and offline handling | **Source with open device gate.** Replacement/revocation recorded; no unfiltered fallback for selected upstream. | Actual second-VPN conflict, loss of network, IPv4/IPv6 and service restart. Do not enable lockdown on a DNS-only split tunnel without a different validated architecture. |
| WEB-14 / FAMILY-01: unspecified “change phone” request | **Open, requirement unclear.** No invented device-transfer/remote-control behavior. | Owner clarification when this feature is developed; ordinary filtering work can continue. |

## Parent authority, Strict mode and recovery

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| SEC-01 / M57-06–08, SETUP-07: authenticated pause/cooldown | **Source.** Presets, custom end date/time, indefinite pause, monotonic/reboot-aware policies; wrong-password waits escalate. | Correct-password immediate change, lock/reboot/clock changes, UI and cooldown device checks. |
| SEC-02 / M57-07: clear custom date/time selection | **Source.** `CustomPausePicker`, expiry/timezone description. | DST, past date, locale/24-hour settings, large text and process recreation. |
| SEC-03 / M57-09: current own App info/accessibility guard | **Source, limited coverage.** Existing scoped Settings recognition, family password and explicit 60-second maintenance. New installations default guard off. | Samsung M13 direct/search/long-press entry; unrelated settings and initial permission setup must remain available. Not guaranteed revocation prevention. |
| SEC-04: expanded optional parent Strict mode | **Open.** Unified Strict enrollment/settings screen is not delivered. Personal commitment is a separate feature. | Explicit parent authority, recovery prerequisite, supported-package policy, truthful limitations and device tests. |
| SEC-05: guard VPN configuration | **Open.** No newly added Strict interception for the VPN settings page. | Trusted headings/activity recognition across tested OEM layouts, authorized maintenance, no unrelated network-settings block. |
| SEC-06: guard every M57 permission detail page | **Open.** Expanded overlay/background/usage/admin/permission page recognition is not implemented beyond the existing scoped guard. | Target app identity plus trusted page controls, positive/negative OEM tests, no arbitrary text/search blocking. |
| SEC-07: guard uninstall detail/confirmation | **Partial.** Existing own App info guard and optional device admin; broader supported uninstall confirmation policy not delivered. | Long-press launcher, app stores, Settings, admin removal and explicit authorized release. |
| SEC-08: managed protection/release | **Source, not release-accepted.** `ManagedProtection` can prevent uninstall/user-control only after separate deliberate device-owner provisioning and has an authenticated release action. | Real managed test device, recovery and release protocol; do not enroll the owner's normal phone during preview. |
| SEC-09 / OSS-04: optional repository page guard | **Source, limited browser coverage.** Path-aware rule, parent-authorized source access; new installations default off. | Public notices/source remain accessible to reviewers/parents; browser false positives and maintenance boundaries. |
| REC-01 / M57.6, RECOVERY-01–05: remotely assisted recovery | **Open.** No operational password reset, master password or self-service shortcut. Current app warns honestly. | Full protocol/implementation and separate issuer/operator setup required. |
| REC-02: parent ownership proof safe from child hijack | **Open.** Intended direction: separately held parent recovery material plus human-assisted, device-bound approval. This is a design direction, not an enabled reset path. | Decide lost-material policy; child with only an unlocked phone cannot gain reset authority. Parent material alone must not grant self-service reset. |
| REC-03: challenge/expiry/replay/signing-key rotation/audit | **Open.** No issuer key embedded or generated as an app backdoor. | Device-bound pending challenge, short-lived signed approval, rotation/revocation, atomic one-time consumption/password replacement, no secret logs. Test wrong-device/forged/expired/replayed approvals, restart/clock changes and interrupted writes. |
| REC-04: parent-friendly remote support UX | **Open.** Guided request/approval sharing and reset flow not delivered. | Clear steps, explicit user sharing, no forced account, support availability and real remote walkthrough. No promise of immediate support. |

## Quality, delivery and public claims

| ID / requirement | State and owning implementation | Remaining acceptance |
| --- | --- | --- |
| REL-01 / OSS-01–02: independent fully FOSS app | **Source.** Separate repository, native code, no ads/payment locks/account requirement; original AGPL terms and inherited GPL/other notices retained. | Full ownership/license inventory and dependency/source inspection for candidate. |
| REL-02 / OSS-03,07: stable production package/signing/data | **Source.** Production ID unchanged; Preview isolated. Signing tools verify established certificate and exact source metadata. | Signed candidate upgrade on actual installed release with settings preserved. Installing Preview does not prove this. |
| REL-03 / OSS-05: incremental commits and release pipeline | **Partial.** Tested core-policy commits exist; existing tag-based signed prerelease/source workflow remains. | Commit remaining coherent UI/runtime/assets/docs slices, expand and validate gates; no tag/publication authorized by this install request. |
| REL-04 / OSS-06: Gradle tests, lint and both builds | **Partial.** JVM policy/release-tool checks have passed; earlier preview and debug/release assembly succeeded. A previous full gate failed a lint issue, since fixed. | Re-run current stable source: debug/release tests and lint, APKs, AAB and Preview. New source is not accepted by an earlier build. |
| REL-05: CI/CD for regressions | **Partial.** Workflow now includes standalone policy checks, debug/release tests/lint/APKs, release bundle, separate Preview artifact and both lint reports. Signing checks are retained; SDK36 updates are in source. | Asset/metadata gates and Preview UI emulator flow still need expansion; hosted workflow execution is unverified. |
| REL-06: phone/OEM acceptance and performance | **Open.** Owner authorized announced phone use; Preview install requested. | Run `DEVICE_TESTS.md`: actual apps/browsers, permission journeys, foreground/background, lock/reboot, networks, overlays, large text, TalkBack, reduced motion, cold start, memory/frame/battery measurements. No zero-edge-case claim. |
| REL-07 / M57.9: Play/F-Droid preparation | **Partial.** Target/compile SDK36 and existing signing/source tooling. | Complete public/native privacy disclosures (including selected DNS-provider queries), store metadata/screenshots, current eligibility/build recipe, reproducibility and actual candidate acceptance. No submitted/approved listing claimed. |
| REL-08: public release/source/support | **Open for new candidate.** Existing release infrastructure/source/license routes remain. | Stable signed/source-corresponding candidate, checksums/notices, exact build ref, rollback/support and publication authorization. |
| REL-09: clean professional repository/history | **Partial.** Product branch and new commits use original feature descriptions; local removed input documents are excluded from new tree. Required attributions stay. | Finish uncommitted changes, inspect tracked tree/commit messages and remove obsolete unpublished local references without rewriting published history. |
| REL-10 / M57.11,13: separate animated public page | **External owning repository.** Lyra owns `/m57`; native About now links there. | Revalidate live page phone/desktop, truthful per-feature/permission stories, latest signed download and real store states. Native Preview does not validate the website. |
| REL-11 / M57.9: distribution introduction and submissions | **Open.** No external post, store submission or signing secret publication performed. | Actual release link, accurate tested claims, each platform's review rules and separately authorized publication. |

## Separate retained product scope

A separate parent/remote-control application, packaged Lyra wrapper and native
iOS enforcement are not implemented by this Android preview. They retain their
own roadmap/platform/authorization gates. More languages, notification-content
filtering and channel-level educational-video allowlists also remain distinct
future capabilities; no general app/site toggle is proof of those features.
The unspecified device-change request stays open for clarification.

## Latest owner UI additions

- AUTH-UX-01: password checking/saving needs immediate visible progress,
  disabled duplicate submission, a readable wrong-password/error state and
  a clear cooldown. The installed Preview does not yet include this correction.
- UI-10: inspect the supplied `~/Mindful` app and reuse components only where
  its actual licensing permits. The checked local revision is
  `b6eb68d500463c54e1127b6cc7ad540f02ae0c8e`; its GPL-2.0 file headers have
  no “or later” grant. Direct incorporation into the current AGPLv3/GPLv3
  app requires compatible permission. General interaction ideas can inform
  original native M57 controls. No source/assets have been imported.
- UI-11: continue a substantive Preview UI pass: readable compact summaries,
  responsive focus state changes, grouped actions, clear empty/loading/error
  states and consistent personal/parent components. Owner acceptance remains
  the visual gate; no “best in the world” claim is a verification result.

## Work order after this preview

1. Verify the current Preview builds and installs; record its exact artifact digest and phone identity without exposing private data. Let the owner review the visual/flow changes.
2. Finish focus/usage lifecycle and accessibility/performance polish, including missing manual breaks and permission-return journeys; preserve family authority.
3. Implement the ownership-proof and human-assisted recovery protocol, tests and operating setup. No universal password and no reset based solely on access to the child phone.
4. Add explicit parent Strict enrollment and supported settings/VPN/install/uninstall guards. Test false positives and authenticated maintenance on each supported OEM; separately validate managed release.
5. Complete Android/CI/source/signature/license/store packaging gates and the full device matrix. Only then describe the candidate as release-ready or prepare a publication action.

## Evidence for this audit

Core-policy commits: `3ad236e` (focus, schedule, usage and AI boundaries) and `3778669` (resolver, browser-page and VPN capability policies). Artwork is committed as `2f25a4a`; the native UI/runtime is committed as `9a774fe`. The install/build result is recorded separately. Local verification and today's exact phone installation belong in [the implementation record](implementation-2026-11-13.md). [Device acceptance](../DEVICE_TESTS.md) remains a separate required gate. Rows stay open until the implementation and its required evidence exist.
