# Personal M57

M57 has five personal destinations: Home, Focus, Plan, Insights and Settings.
New installations choose personal or family authority during a resumable,
six-step introduction. Lyra redirection is an optional switch, off by default
for new installations. Existing password installations retain family authority,
credentials, pauses, permissions and all existing rule selections.

## Independent use and authority

Personal use requires no password, account or Lyra connection. Family use
requires the existing administrator credential before changing protection,
app limits, routines, browser rules or device policies. Personal timers, goals
and focus history remain available without unlocking that space. There is no
in-app downgrade from an existing family installation to personal authority.
Intent extras can open the personal timer; they cannot grant administrator access.

Turning off AI redirection prevents AI/Meta/Google AI Mode interventions. The
timer, routines, app limits, short-video rules and family browsing continue to
have their own settings. General focus apps open an M57 boundary screen, never
a Lyra redirect. Problem reports use the M57 issue tracker.

## Focus and schedules

- Timer: 1–480 minutes. Stopwatch: up to eight hours. Pomodoro: configurable
  focus length, 1–60 minute breaks, up to 12 rounds and eight hours in total.
- A session is persisted before it starts. Timing uses elapsed realtime in the
  same boot and wall time across boots. A backwards clock across boots ends
  recovery without inventing focus credit. Reboot recovery depends on the
  phone's clock; it is not proof the person actually worked for that duration.
- Chosen launcher apps and focus sites are held during focus phases. A study
  allowlist can restrict supported visible browser addresses to chosen domains.
  Allowed study sites override personal denied sites, but cannot override family
  rules or the DNS VPN. Whole-browser app selection still holds the whole browser.
- Pomodoro breaks release the personal session's list. They never modify the
  administrator pause or family DNS state. A simultaneously active routine or
  daily limit may still hold an app during a timer break.
- Commitment timers/Pomodoros have no early-end action. Personal protection
  settings and focus-boundary changes wait until the commitment ends. This
  does not disable Android's permission, force-stop or removal controls. Family
  administrator authority remains independent. Stopwatch commitments are disabled.
- Up to 30 weekly routines reuse the focus list. Overnight intervals belong to
  their selected starting day; the end is exclusive. They follow the current
  timezone and include both repeated hours at a daylight-saving transition.
  Routines enforce while the accessibility service is connected, not through
  exact alarms. Android/OEM suspension can delay checks.

The launcher, default dialer, Settings, permission controller, M57 and the
configured Lyra PWA are excluded from the app picker. Calling emergency services
must remain available; verify changed default apps and OEM variants on a device.

## Usage, history and privacy

Usage processing is independently opt-in and requires Android Usage Access.
Android's ordered app lifecycle, screen-off, lock and shutdown events produce
local foreground estimates. Queries run off the UI thread and have a bounded
event count. M57 does not retain raw usage events, a browsing timeline or DNS
queries. Split screen, missing Android events and app transitions can affect
estimates. This is not a diagnostic score or a machine-learning recommendation.

Daily limits reset at local midnight. While the service is interactive, usage
is sampled about every 15 seconds. Missing/revoked access, incomplete queries,
stale samples and previous-day samples do not enforce a limit using invented
time. The UI explains that usage access is needed. This makes limits voluntary
in personal mode; a family user cannot toggle the processing setting without
administrator access.

The latest 500 completed/ended focus sessions are kept in private app storage.
Completion and insertion into history use a single preferences transaction.
Records contain a random session ID, the person's optional intention, end time,
focused duration and completion flag. Intention text never leaves the phone
unless the person explicitly exports history. Sessions are credited to the
local day they end. Insights supplies a seven-day view, optional local app-time
estimates, JSON export through Android's document picker and history deletion.
Deletion does not cancel an active session or erase already-exported files.
Android backups remain disabled. Storage-write failures are surfaced on Home.

The home-screen widget opens Focus and shows the saved daily goal progress.
It does not change protection or start sessions without an explicit action.
It refreshes on session changes, periodically through Android (at least a
30-minute update interval), and about once a minute with the service connected.
It does not claim a continuously current countdown.

## Design and accessibility

The experience follows Lyra's warm ivory/sand surfaces, terracotta accent,
umber text, moss detail, readable hierarchy and M57's own ring identity. Original
Canvas graphics scale alongside sixteen packaged local illustrations. The
Android-compatible Sora font is bundled with its OFL notice. Focus setup keeps
a local intention/mode/duration draft, offers short presets and previews the
whole Pomodoro plan; it does not start until the person explicitly chooses it.
Controls use at least 48dp targets, plain labels, grouped settings, progressive
disclosure and a bounded content width on tablets. The bottom navigation adapts
to insets, the keyboard and scroll direction; touch exploration keeps it visible.

Motion explains a state change: short page entrances, disclosure reveals, dock
transitions, session progress and slow illustration orbits. It respects the
system animator setting, the in-app Reduce motion switch, battery saver and
touch exploration. Decorative animations stop when detached or hidden. Phone
checks must still assess actual text sizing, TalkBack, power changes and motion.

## Google AI Mode boundary

The optional experimental rule is off by default and requires AI redirection.
It recognises `google.com/aimode`, bounded `google.com/search?udm=50` addresses,
and a narrow English Google-app assistant/selected toolbar title. Unselected
tabs, ordinary search queries mentioning AI Mode, adjacent paths, ambiguous
duplicate mode parameters and lookalike hosts do not qualify. Regional domains,
translated headers and hidden fields are not claimed. See [Google's Android
Chrome guidance](https://support.google.com/chrome/answer/16704170?co=GENIE.Platform%3DAndroid&hl=en)
and the [parameter categorization notice](https://knowledge.broadcom.com/external/article/431226/webpulse-categorization-update-for-googl.html).

## Verification boundary

Pure policy checks cover timer phases/recovery, clock uncertainty, routines,
overnight and DST boundaries, domain precedence/lookalikes, AI Mode false
positives and usage-event accounting. They do not run Android services or prove
blocking on a phone. The [complete feature audit](feature-audit-2026-11-13.md) tracks all roadmap
and conversation additions, including unfinished Strict-mode and recovery work.
The [device checklist](../DEVICE_TESTS.md) remains a release
gate alongside debug/release Gradle tests, lint, builds, signing continuity and
corresponding source. See [current implementation evidence](implementation-2026-11-13.md).
