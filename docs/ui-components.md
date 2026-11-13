# Native UI component pass

The owner supplied the local `~/Mindful` repository, revision
`b6eb68d500463c54e1127b6cc7ad540f02ae0c8e`, as an additional wellness-app
reference. Reviewed areas include onboarding/permission explanations, grouped
controls, statistics summaries, focus configuration, active sessions, countdowns,
loading/empty states and navigation transitions.

The repository contains GPL version 2 and its file headers specify GPL-2.0
without an “or later” grant. No compatible additional grant was found in the
reviewed source. Direct code/asset incorporation into the existing M57
AGPLv3/GPLv3 program is therefore not enabled by those terms. See the
[GNU compatibility guidance](https://www.gnu.org/licenses/gpl-faq.html#v2v3Compatibility).
No Dart/Java source, artwork, strings or third-party dependencies from that
repository have been copied into M57. Direct reuse needs compatible permission
from the relevant copyright holders; the app's existing licenses are preserved.

The native M57 work uses common interaction patterns in original components:

| Component | Purpose and implementation |
| --- | --- |
| `NavigationDock` | Shared light, labelled parent/personal navigation with warm selected states, keyboard/inset behavior and accessible roles. |
| `LyraUi.section` | Distinct section names and short explanations; moss/apricot supporting accents and dark readable labels. |
| `SummaryTiles` | Responsive local summaries; stack on narrow windows or large text. No network or new usage collection. |
| `FocusModePicker` / `FocusSetup` | Explicit mode choice, short presets, whole-plan preview, saved draft and grouped labels/inputs. |
| `FocusArt` / `FocusRounds` | Original ring scene, focus/rest color state and round index; adjacent native clock/phase text remains authoritative. Decorative work stops when hidden and responds to power/animation/accessibility changes. |
| `ActionFeedback` | Shared inline working/error/info state. Password operations show immediate progress, disable duplicate submission and preserve readable failure feedback. |
| `Illustrations` / `FeatureGuide` | Sixteen original packaged scenes with accessible native explanations and explicit feature actions. |
| `PageShell` | Bounded content width, scroll/keyboard/system insets and pinned onboarding actions. |

Password verification and hashing still run on the existing worker and retain
credential strength, cooldown and lifecycle authorization checks. Leaving the
page invalidates the result before it can unlock settings or save a new
password. No spinner, animation or imported UI pattern grants authority.

The earlier Preview install is documented in the implementation record. Changes
after it need a new build/install and actual visual review; source implementation
is not owner acceptance. Follow the remaining feature/device gates rather than
calling any iteration the best or final by declaration.

For local review, the owner requested that every Preview build be installed and
opened automatically:

```sh
bash scripts/preview-on-device.sh
# If several devices are connected:
bash scripts/preview-on-device.sh --serial DEVICE_SERIAL
```

The helper builds a fresh Preview, checks its package identity, updates it with
`adb install -r`, checks that production package version/install timestamps are
unchanged and launches the Preview activity. A failed build never falls back to
an old APK. It does not clear data or grant Android permissions. It does not
unlock the phone or bypass its lock screen.
