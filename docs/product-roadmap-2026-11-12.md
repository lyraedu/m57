# M57 product and public-page additions

Owner decisions, 12 November 2026. These requirements are planned, not shipping claims. The shared current brief is in Lyra `docs/roadmap/product-vision.md`; preserve separate repository/licensing boundaries.

## Purpose and identity

**Room for what matters.** Describe practical actions instead of leading with “digital wellbeing”: interrupt endless Shorts/Reels, choose app/site limits, schedule focus time, control configured adult-content categories and optionally redirect supported AI surfaces into Lyra. Fully FOSS, free, no ads, no premium locks; independent mode requires no Lyra account. Controls are individually configurable under clearly explained personal/family authority. Uninstall friction is optional within Strict mode, with managed policy and personal-device limits kept distinct.

## Required additions

- Add Google AI Mode to the AI surface roadmap: distinguish AI Mode from ordinary search, test supported Google app/browser variants and localization, allow optional redirect, and document unsupported/false-positive cases.
- Use original designs, assets and code. Require independent M57 device acceptance for every supported control.
- Separate whole-app focus, short-video surfaces, family/site DNS policies, allowed study sources, AI redirects and Strict mode. Personal breaks must not acquire family administrator pause authority.
- Track timer/stopwatch/Pomodoro, breaks, local history/insights, routines/goals/widgets independently, with monotonic timing, process/reboot recovery, deletion/export and real-device gates.
- Dedicated interactive public page in Lyra `/m57`: readable action-led overview, separate ring identity, feature toggles, original animations, independent mode and exact FOSS/source/license notices.
- Permission education covers display over apps, accessibility, background operation, administrator and local DNS VPN only where required by enabled controls. Show what each grants, data scope, decline/revocation and OEM return-state checks. DNS filtering is not traffic encryption; accessibility does not inspect every hidden surface.
- GitHub Releases supplies the stable signed APK destination with matching source/notices. F-Droid distribution needs eligibility/submission tracking and a real listing before an active download badge. Google Play can be Coming soon; M57 is Android-only and an App Store badge must not imply iOS enforcement exists.

## Hearts Shaping Lyra

This is the accepted private-beta community name across the wider project. Parents, teachers and students talk directly with Akshay about kids, AI and technology, shaping both Lyra and M57. Lifetime direct founder contact and occasional goodies are intended; no unagreed lifetime paid-feature or discount promises. The Lyra application/parent account will manage optional, private fulfilment addresses and communication/shipment preferences. M57 independent use remains account-free.

## Evidence

All new requirements need owning implementation, meaningful boundary tests and phone acceptance. No native runtime, signing identity, store listing or APK is changed by this document.

## Public package checkpoint, November 12

The owner clarified the November 18 public package: polished standalone M57,
a bounded real Lyra demo and the visual storytelling website. This replaces the
older assumption that every private Lyra roadmap feature ships that day; native
M57 requirements remain tracked separately and are not declared finished by the
website work. November 22 special FOSS/distribution planning remains separate.

Lyra's `feat/explore-story-choreography` now implements the public M57 page's
ring-led animated opening, interactive feature switches, permission explanations,
independent-use/FOSS story and honest GitHub/F-Droid/store states. The Lyra record
`docs/roadmap/updates/2026-11-12/implementation-status.md` owns website checks.
Native implementation still needs the independent product implementation,
Android tests/build, supported-device evidence, signing/source correspondence and
release acceptance. A feature animation is not proof of phone enforcement.

## Native implementation checkpoint, November 13

The [personal experience](personal-experience.md) now defines the implemented
native flows and their authority, timing, retention and permission boundaries.
[Implementation evidence](implementation-2026-11-13.md) records available checks
and the remaining Android/device gates. These source additions do not declare
the earlier launch or distribution requirements complete.

The [13 November feature audit](feature-audit-2026-11-13.md) reconciles the
full native requirement set, subsequent owner additions and remaining release
gates. It supersedes older completion impressions without erasing requirements.
