// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import com.lyra.blocker.family.HostRules;
import java.util.Set;

/** Personal focus boundaries and edit authority. Family website enforcement stays separate. */
public final class FocusPolicy {
    private FocusPolicy() {}
    /** A child's personal commitment cannot suspend authenticated parent authority. */
    public static boolean editingAllowed(boolean authorised, boolean familyManaged, boolean commitmentActive) {
        return authorised && (familyManaged || !commitmentActive);
    }
    private static boolean matches(String host, Set<String> entries) {
        return entries.stream().anyMatch(entry -> host.equals(entry) || host.endsWith("." + entry));
    }
    public static boolean blocks(String host, Set<String> denied, Set<String> allowed, boolean study) {
        String domain = HostRules.domain(host);
        if (domain == null || matches(domain, allowed)) return false;
        return study || matches(domain, denied);
    }
}
