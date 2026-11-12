// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Set;

/** Local wall-clock routines include overnight windows and follow the phone's timezone. */
public final class RoutinePolicy {
    private RoutinePolicy() {}
    public static boolean active(Set<Integer> days, int start, int end, ZonedDateTime now) {
        if (start < 0 || start >= 1440 || end < 0 || end >= 1440 || start == end || days.isEmpty()) return false;
        int minute = now.getHour() * 60 + now.getMinute();
        int day = now.getDayOfWeek().getValue();
        if (start < end) return days.contains(day) && minute >= start && minute < end;
        int previous = DayOfWeek.of(day).minus(1).getValue();
        return (minute >= start && days.contains(day)) || (minute < end && days.contains(previous));
    }
}
