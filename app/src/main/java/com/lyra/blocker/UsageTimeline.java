// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import java.util.HashMap;
import java.util.Map;

/** Foreground estimates from ordered Android lifecycle events; no activity text is retained. */
public final class UsageTimeline {
    private final long start;
    private final long end;
    private final Map<String, Long> totals = new HashMap<>();
    private String foreground;
    private long since;
    private long last = Long.MIN_VALUE;
    public UsageTimeline(long start, long end) {
        if (end < start) throw new IllegalArgumentException("Invalid interval");
        this.start = start; this.end = end;
    }
    public void event(long time, String pkg, int type) {
        if (time < last || time > end) return;
        last = time;
        if (type == 1) {
            if (pkg == null || pkg.isEmpty() || pkg.equals(foreground)) return;
            close(time); foreground = pkg; since = time;
        } else if ((type == 2 && pkg != null && pkg.equals(foreground)) || type == 16 || type == 17 || type == 26) close(time);
    }
    private void close(long time) {
        if (foreground != null) {
            long duration = Math.max(0, Math.min(time, end) - Math.max(start, since));
            totals.merge(foreground, duration, Long::sum);
        }
        foreground = null;
    }
    public Map<String, Long> finish() { close(end); return Map.copyOf(totals); }
}
