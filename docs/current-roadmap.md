# M57 current roadmap

Updated 15 November 2026. Source reviewed on `feat/independent-focus` at `520ab79`. This is documentation only: no Android code, network behavior, settings, phone installation or release is changed.

## Implemented, partial and open

| Area                      | Current source                                                                                                                                      | Remaining work                                                                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Independent/family setup  | Resumable onboarding, parent authority, illustrated permission/privacy/FOSS education; optional Lyra off for independent setup                      | More expressive flow, denial/revocation/process/OEM acceptance                                                                                    |
| Native design             | Ring identity, Sora, sixteen local illustrations, warm components, tactile/ambient motion and live password feedback                                | Full expressive redesign, richer accessible motion and a redesigned blocked/redirect page                                                         |
| Wellbeing                 | Focus timer/stopwatch/Pomodoro, personal commitments, lists/routines/goals, local history/export/delete, widget and optional usage estimates/limits | Usage dashboard, phone-use notifications, manual breaks/reflections and further tools; permission onboarding alone does not deliver notifications |
| AI/media/browser controls | Scoped app/browser/short-video recognition and optional Google AI Mode                                                                              | More browser/OEM evidence and bounded visible-keyword rules, including Reddit; no universal screen inspection                                     |
| Family DNS                | Local DNS VPN, categories/exceptions, downloaded lists, selectable Cloudflare HTTPS upstream                                                        | Owner-reported failure needs reproduction; live network, encrypted DNS and VPN-conflict acceptance                                                |
| VPN prevention            | `VpnApps` discovers visible VPN-capable packages                                                                                                    | Strict VPN opening/settings/store/installer/provider-search prevention not implemented                                                            |
| Strict protection         | Limited own-App-info/accessibility guard, optional admin and separate managed-owner support                                                         | Unified parent Strict enrollment; expanded permission/admin/uninstall/VPN page guards and authenticated release acceptance                        |
| Password/authority        | Local credentials/timing; `942794d` keeps authenticated parent authority during personal commitments                                                | Wider lifecycle/storage/lock/reboot/false-positive device acceptance                                                                              |
| Build/device evidence     | [Implementation record](implementation-2026-11-13.md): 35 debug tests, Preview lint/assembly; subsequent Samsung in-place install and launch        | No new checks today, no full enforcement acceptance, signed public alpha, production upgrade, store listing or hosted CI proof                    |

The current recorded artifact is development-signed `0.6.4-preview`, package `com.lyra.blocker.preview`. Basic onboarding/Focus was exercised; accessibility interception and broad Strict coverage were not established. Preserve production identity/data and Preview data. Do not infer current phone availability from prior installation.

## 20 November development update

Focus replaces the personal landing page. Protected Controls replaces Overview
and includes pause/resume. Plan includes bedtime with overnight app restrictions,
a live countdown and an optional estimate for the last scheduled night. Insights
adds selectable app-time periods and guidance to allow at least three days of use.
Zenith is credited in the bundled notices. Device acceptance, visit/unlock
counts, optional usage notifications, DND integration and the remaining roadmap
items are open.

## Updated owner decisions

### 1. Offline architecture before more network work

Remove online recovery/query from active scope; the owner now requests that M57 not connect to the Internet. No online reset service or universal password should be built. This supersedes the older assisted-online-recovery work, rather than merely postponing it.

Current code is not offline-only: `AndroidManifest.xml` declares `INTERNET`; `FamilyRepository` downloads hosts lists, `FamilySyncJob` schedules refreshes and `FamilyVpnService` can send Cloudflare HTTPS queries. Inventory these flows, diagnostics and external-link handoffs; choose bundled/manual list updates and the replacement DNS strategy. A literal zero-network app cannot retain external Cloudflare queries. Settle the conflict before runtime removal; do not silently disable existing filtering or falsely change privacy copy.

### 2. Diagnose Cloudflare, then choose coverage

Reproduce the reported failure across configured modes, Wi-Fi/mobile, IPv4/IPv6, outages, private/encrypted DNS, VPN replacement and reconnect/reboot. Compare retaining DNS with offline hostname and supported on-screen rules. A DNS denial cannot itself create an in-browser redirect page. Source presence is not live reliability.

### 3. Local visible-content interventions

Plan hostname plus visible-keyword rules for supported browser surfaces, Reddit and other specifically tested apps. Define local per-app/context/locale recognition, benign nearby cases, edited-input exclusion, debounce, no text/history retention and exceptions. Do not scan arbitrary private messages or redirect on an isolated keyword without trusted screen context. Hidden content remains unsupported. Record each supported target and limitation.

### 4. Parent Strict mode

Plan accessibility/admin/permission-revocation/App-info/uninstall and VPN configuration/client/store/installer/provider-search paths. Explicit parent enrollment, authenticated maintenance/release and setup escape paths are required. Parent authority and personal focus commitments remain separate. Each rule needs positive/negative cases and physical OEM acceptance; this is not already delivered by VPN discovery or ordinary device admin.

### 5. Local and safe-mode removal

Plan safe-mode removal and authenticated local parent release instead of an online recovery service. Review supported OS/OEM behavior, credential loss and managed-device release separately. The requested goal that safe mode be the only bypass cannot be stated as guaranteed ordinary-app behavior. Do not enable unrecoverable managed provisioning or add a universal bypass while this remains untested.

### 6. Finish alpha, then broader redesign

Retain existing native artwork/onboarding work; complete selected tools, quiet usage notifications, permission flows and a more expressive intervention page. Test supported controls before announcing the alpha. The larger end-to-end redesign and Lyra landing M57-section redesign follow the first settled alpha. Public version numbering is undecided; do not rename the existing Preview artifact or call it stable 1.0.

## Evidence and release gates

The detailed [feature audit](feature-audit-2026-11-13.md), [wellbeing continuation](wellbeing-tools.md), [implementation log](implementation-2026-11-13.md) and [device checklist](../DEVICE_TESTS.md) remain references. Older recovery and networking proposals are superseded by the decisions above; historical tests and installation observations remain dated evidence. Before a public candidate: relevant Android tests/lint/build, actual supported-device/network/permission/performance acceptance, source/license correspondence, stable signing/upgrade proof and explicit publication authorization.

Lyra's target architecture is parent-first, with separate student/parent/teacher web experiences and score-only parent reports. These web changes remain planned. M57 stays independent/account-free, with its existing minimal optional handoff; do not import the legacy school model or student chat content into it.

Platform references: [Android managed uninstall policy](<https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setUninstallBlocked(android.content.ComponentName,%20java.lang.String,%20boolean)>), [Android VPN lifecycle](https://developer.android.com/reference/android/net/VpnService), [Cloudflare resolver setup](https://developers.cloudflare.com/1.1.1.1/setup/). These are platform capabilities/limits, not passed M57 tests.
