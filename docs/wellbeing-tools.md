# M57 wellbeing tools and UI continuation

> **Current roadmap, 15 November:** [Current plan and status](current-roadmap.md) supersedes older online-recovery requirements and owns the offline/DNS decision, Strict/safe-mode scope and alpha/redesign sequence. Existing completion evidence below remains dated; it is not a new release claim.

The broader UI, focus, blocking, analytics, notifications and Strict-mode scope
remains active. On 15 November the owner removed online recovery/query from
active scope. Local/safe-mode release and the offline-versus-DNS architecture
now need design before more networking work.
The [feature audit](feature-audit-2026-11-13.md) distinguishes source from device
acceptance and publication.

## Current native work

- Warm shared personal/parent components, light dock, ring branding, Sora,
  moss/apricot section hierarchy, sixteen original illustrations and guides.
- Tactile press/release motion and gentle artwork breathing while visible;
  reduced motion, battery saver, touch exploration and hidden views stop it.
- Live passphrase-length/confirmation guidance, password check/save progress,
  clear errors, cooldown countdown, pinned actions and protected password screens.
- Personal permission stories for accessibility, app usage, background operation
  and sending M57 notifications. Parent authority is set up before its protected
  seven-page access/free-software/Strict-guard guide. Optional grants stay optional.
- A dedicated everyday-language free/open-source page and visual privacy story.
  Usage is local; optional list updates, DNS-provider queries and opened links
  are disclosed. No claim that M57 never connects to the Internet.
- Timer/stopwatch/Pomodoro, scheduled breaks, app/site lists, routines, daily
  limits/goals, history/export/deletion and a widget, with known acceptance gates.
- Automatic installed-PWA matching for the two requested trusted Lyra origins.

## Next tool slices, retained requirements

| Area | Required continuation |
| --- | --- |
| Usage | Today, Yesterday and 7-day app time, app icons and per-app summaries are in development source. Visit/unlock counts and physical acceptance remain open. Longer local aggregates need explicit retention/deletion/export controls. |
| Notifications | Quiet optional phone-use snapshots, correct onboarding permission/status, privacy on the lock screen, cancellation on revoked access and bounded background work. Sending notifications is distinct from reading other apps’ messages. |
| Focus | Manual timed breaks with correct crediting, session reflections/tags, richer live/completion UI, quick actions and additional widget polish. Personal breaks must never pause family protection. |
| Routines | Bedtime schedule, independent app selections and last-window usage are in development source. Reusable app groups, DND and physical acceptance remain open. |
| Distraction tools | Opening delays, intentional pauses, visit/frequency allowances, supported short-video counters and separate per-feature scope. No punitive streaks or fabricated wellbeing scores. |
| Browsing/network | Supported URL/day budgets, broader browser evidence, DNS/TCP/private-DNS/DoH/network-change coverage and optional app-network policies only after architecture and privacy validation. |
| Strict | Supported parent-owned settings, App info, uninstall, VPN configuration/client/store/installer paths; authenticated maintenance; positive/false-positive OEM tests. Personal Android controls and managed policy remain distinct. |
| UX/quality | Review every state, app-use page, onboarding return, keyboard, large text, TalkBack, empty/loading/error states, performance and CI/emulator/device gates. Material-expressive interaction with M57’s own warm visual language. |
| Recovery | Online recovery/query removed from active scope on 15 November. Design local/safe-mode release; no online service, universal password or unauthenticated reset is shipped. |

## Compatible source review

Review claims and actual implementation separately. A public README is not
proof of runtime correctness, and a compatible root license does not eliminate
file/dependency/asset checks. Do not copy paywalls, tracking, private-message
retention or weak recovery mechanisms into M57.

| Source snapshot | Checked terms and useful areas | Reuse state |
| --- | --- | --- |
| Curbox `9f8250c622092a13bd259f13cebeb13169f9a0a8` | GPLv3; native usage periods/aggregation, focus, permission flows and controls. [Source](https://github.com/curbox-app/curbox-android) | Existing inherited components retain notices. Newly reviewed helpers still need deliberate adaptation/tests/attribution. |
| Zenith `2cd777f9fb20d1b37320f262e86fed79290c9867` | GPLv3; expressive transitions, onboarding paths, pause/delay/bedtime/usage UI. [Source](https://github.com/1372Slash/Zenith) | Credited design source for the native bedtime, usage and transition work. See NOTICE. |
| SelfLock `57ea0a89cf1749e4b37f3232a4819c0fe61bbf17` | MIT; rule-state/usage/schedule organization. [Source](https://github.com/EtashTyagi/SelfLock) | No code copied. Its password override design is not M57’s recovery policy. |
| Rethink `07fe5373f8aec3b68344db98223a145c404ed386` | App Apache-2.0; transport dependencies have separate terms, including MPL-2.0. [Source](https://github.com/celzero/rethink-app) | Network architecture review; no stack imported and no DNS-query logging adopted. |
| Supplied Mindful `b6eb68d500463c54e1127b6cc7ad540f02ae0c8e` | File headers GPL-2.0, no later-version grant found; grouped controls, usage and focus interaction reference. | No source/assets imported into the AGPLv3/GPLv3 app under these terms. |

[Android’s wellbeing guide](https://support.google.com/android/answer/9346420?hl=en)
provides another feature reference for app/site timers, focus and bedtime.
[Android 15’s DND change](https://developer.android.com/about/versions/15/behavior-changes-15#dnd-changes)
requires app-owned rule behavior for apps targeting newer Android versions;
future DND integration must not assume that M57 can overwrite the global mode.

Keep completed slices committed. A current APK delivery does not mark the
remaining rows complete, cancel them or imply Play/F-Droid approval.
