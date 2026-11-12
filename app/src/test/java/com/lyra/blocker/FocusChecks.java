// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

public final class FocusChecks {
    public static void main(String[] args) { run(); System.out.println("Focus, routine, domain and usage checks passed"); }
    public static void run() {
        long minute = 60_000;
        var focus = FocusClock.state("pomodoro", 24 * minute, 25 * minute, 5 * minute, 4);
        check(focus.phase().equals("focus") && focus.remaining() == minute && focus.focused() == 24 * minute, "Focus phase drifted");
        var rest = FocusClock.state("pomodoro", 25 * minute, 25 * minute, 5 * minute, 4);
        check(rest.phase().equals("break") && rest.remaining() == 5 * minute && rest.focused() == 25 * minute, "Break leaked into focus credit");
        var second = FocusClock.state("pomodoro", 30 * minute, 25 * minute, 5 * minute, 4);
        check(second.round() == 2 && second.phase().equals("focus") && second.focused() == 25 * minute, "Next round incorrect");
        var done = FocusClock.state("pomodoro", 115 * minute, 25 * minute, 5 * minute, 4);
        check(done.finished() && done.focused() == 100 * minute, "Last round acquired an extra break");
        check(FocusClock.state("timer", 90 * minute, 25 * minute, 0, 1).focused() == 25 * minute, "Late completion inflated focus time");
        check(FocusClock.state("stopwatch", FocusClock.MAX_SESSION + minute, minute, 0, 1).finished(), "Stopwatch is unbounded");
        check(FocusClock.elapsed(1000, 500, 1, 1_000_000, 900, 1) == 400, "Wall-clock jump changed same-boot timer");
        check(FocusClock.elapsed(1000, 500, 1, 900, 100, 2) == -1, "Rollback across reboot was treated as trustworthy elapsed time");
        var interrupted = FocusClock.state("timer", -1, 25 * minute, 0, 1);
        check(interrupted.finished() && interrupted.focused() == 0 && interrupted.phase().equals("interrupted"), "Uncertain recovery invented focus credit");
        check(FocusClock.elapsed(1000, 500, 1, 1600, 100, 2) == 600, "Reboot recovery lost elapsed time");
        rejects(() -> FocusClock.state("pomodoro", 0, 480 * minute, minute, 2));
        rejects(() -> FocusClock.state("timer", 0, 0, 0, 1));
        rejects(() -> FocusClock.state("other", 0, minute, 0, 1));

        Set<Integer> monday = Set.of(1);
        check(RoutinePolicy.active(monday, 22 * 60, 6 * 60, ZonedDateTime.parse("2026-11-16T23:00:00+05:30")), "Overnight start missed");
        check(RoutinePolicy.active(monday, 22 * 60, 6 * 60, ZonedDateTime.parse("2026-11-17T05:59:00+05:30")), "Overnight tail used wrong day");
        check(!RoutinePolicy.active(monday, 22 * 60, 6 * 60, ZonedDateTime.parse("2026-11-17T06:00:00+05:30")), "Routine failed to end");
        check(!RoutinePolicy.active(monday, 22 * 60, 6 * 60, ZonedDateTime.parse("2026-11-18T03:00:00+05:30")), "Routine leaked into next day");
        check(!RoutinePolicy.active(monday, 60, 60, ZonedDateTime.parse("2026-11-16T01:00:00Z")), "Zero duration became all-day");
        check(RoutinePolicy.active(Set.of(7), 60, 180, ZonedDateTime.parse("2026-11-01T01:30:00-04:00[America/New_York]")), "DST first hour missing");
        check(RoutinePolicy.active(Set.of(7), 60, 180, ZonedDateTime.parse("2026-11-01T01:30:00-05:00[America/New_York]")), "DST second hour missing");
        check(FocusPolicy.blocks("sub.example.com", Set.of("example.com"), Set.of(), false), "Subdomain not held");
        check(!FocusPolicy.blocks("example.com.evil.test", Set.of("example.com"), Set.of(), false), "Lookalike host held");
        check(!FocusPolicy.blocks("study.example.com", Set.of("example.com"), Set.of("study.example.com"), true), "Study exception lost precedence");
        check(FocusPolicy.blocks("other.test", Set.of(), Set.of("study.example.com"), true), "Study allowlist not enforced");
        check(!FocusPolicy.blocks("not a domain", Set.of(), Set.of(), true), "Invalid address blocked unrelated content");
        check(GoogleAiPolicy.address("https://www.google.com/aimode"), "AI Mode entry missed");
        check(GoogleAiPolicy.address("www.google.com/search?q=math&udm=50"), "AI Mode results missed");
        check(!GoogleAiPolicy.address("https://www.google.com/search?q=AI+Mode"), "Ordinary search blocked");
        check(!GoogleAiPolicy.address("https://google.com.evil.test/search?udm=50"), "Google lookalike matched");
        check(!GoogleAiPolicy.address("https://www.google.com/search?q=udm%3D50"), "Query text treated as mode");
        check(!GoogleAiPolicy.address("https://www.google.com/search?udm=50&udm=14"), "Ambiguous mode matched");
        check(!GoogleAiPolicy.address("https://www.google.com/aimodern"), "AI path prefix blocked search");

        var usage = new UsageTimeline(100, 1000);
        usage.event(0, "app.a", 1);
        usage.event(250, "app.a", 1); // duplicate activity-resume must not reset the clock
        usage.event(300, "app.b", 1);
        usage.event(350, "app.a", 2); // stale pause from previous app must not close current app
        usage.event(500, null, 16); // screen off
        usage.event(600, null, 15); // screen on does not imply an app resumed
        usage.event(700, "app.b", 1);
        usage.event(800, null, 17); // lock screen
        usage.event(850, null, 18); // unlock without resume remains uncounted
        check(usage.finish().equals(Map.of("app.a", 200L, "app.b", 300L)), "Usage window counted lock time, duplicates or stale pauses");
        var reversed = new UsageTimeline(0, 100);
        reversed.event(50, "app.a", 1); reversed.event(40, "app.b", 1);
        check(reversed.finish().equals(Map.of("app.a", 50L)), "Out-of-order event corrupted usage");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid plan was accepted"); } catch (IllegalArgumentException expected) { /* expected */ }
    }
}
