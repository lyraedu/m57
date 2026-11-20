// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Set;

/** Start days belong to the evening of an overnight window. The end is exclusive. */
public final class BedtimePolicy {
    private BedtimePolicy() {}

    public record Window(ZonedDateTime start, ZonedDateTime end) {
        public boolean active(ZonedDateTime now) {
            return !now.isBefore(start) && now.isBefore(end);
        }
    }

    public static Window currentOrNext(Set<Integer> days, int start, int end, ZonedDateTime now) {
        if (!validHours(start, end)) return null;
        for (int offset = -1; offset <= 7; offset++) {
            Window window = onDate(days, start, end, now, offset);
            if (window != null && window.end().isAfter(now)) return window;
        }
        return null;
    }

    public static Window latestCompleted(Set<Integer> days, int start, int end, ZonedDateTime now) {
        if (!validHours(start, end)) return null;
        for (int offset = 0; offset >= -7; offset--) {
            Window window = onDate(days, start, end, now, offset);
            if (window != null && !window.end().isAfter(now)) return window;
        }
        return null;
    }

    private static boolean validHours(int start, int end) {
        return start >= 0 && start < 1440 && end >= 0 && end < 1440 && start != end;
    }

    private static Window onDate(Set<Integer> days, int start, int end, ZonedDateTime now, int offset) {
        LocalDate date = now.toLocalDate().plusDays(offset);
        if (!days.contains(date.getDayOfWeek().getValue())) return null;
        LocalTime from = LocalTime.of(start / 60, start % 60);
        LocalTime until = LocalTime.of(end / 60, end % 60);
        ZonedDateTime begins = date.atTime(from).atZone(now.getZone());
        LocalDate endDate = end < start ? date.plusDays(1) : date;
        // An ambiguous end during the fall clock change includes both occurrences.
        ZonedDateTime finishes = endDate.atTime(until).atZone(now.getZone()).withLaterOffsetAtOverlap();
        return finishes.isAfter(begins) ? new Window(begins, finishes) : null;
    }
}
