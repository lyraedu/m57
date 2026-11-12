// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

/** Pure session clock. Wall time is used only across boots; a backwards clock ends recovery. */
public final class FocusClock {
    public static final long MAX_SESSION = 8 * 60 * 60_000L;
    public record State(String phase, long remaining, long focused, int round, boolean finished) {}
    private FocusClock() {}

    public static long elapsed(long wall, long monotonic, int boot, long nowWall, long nowMonotonic, int nowBoot) {
        if (boot == nowBoot && boot >= 0) return nowMonotonic < monotonic ? -1 : nowMonotonic - monotonic;
        long delta = nowWall - wall;
        return delta < 0 ? -1 : Math.min(MAX_SESSION, delta);
    }

    public static State state(String kind, long elapsed, long focus, long rest, int rounds) {
        if (focus < 60_000 || focus > MAX_SESSION || rest < 0 || rest > 60 * 60_000L || rounds < 1 || rounds > 12)
            throw new IllegalArgumentException("Invalid session plan");
        if (!"timer".equals(kind) && !"pomodoro".equals(kind) && !"stopwatch".equals(kind)) throw new IllegalArgumentException("Unknown timer");
        if (elapsed < 0) return new State("interrupted", 0, 0, 1, true);
        if ("stopwatch".equals(kind)) return new State(elapsed >= MAX_SESSION ? "done" : "focus",
            Math.max(0, MAX_SESSION - elapsed), Math.min(elapsed, MAX_SESSION), 1, elapsed >= MAX_SESSION);
        if (!"timer".equals(kind) && !"pomodoro".equals(kind)) throw new IllegalArgumentException("Unknown timer");
        if ("timer".equals(kind)) return new State(elapsed >= focus ? "done" : "focus", Math.max(0, focus - elapsed), Math.min(elapsed, focus), 1, elapsed >= focus);
        long total = focus * rounds + rest * (rounds - 1L);
        if (total > MAX_SESSION) throw new IllegalArgumentException("Session exceeds eight hours");
        if (elapsed >= total) return new State("done", 0, focus * rounds, rounds, true);
        long cycle = focus + rest;
        int round = (int) (elapsed / cycle);
        long within = elapsed % cycle;
        boolean breaking = within >= focus;
        return new State(breaking ? "break" : "focus", breaking ? cycle - within : focus - within,
            round * focus + Math.min(within, focus), round + 1, false);
    }
}
