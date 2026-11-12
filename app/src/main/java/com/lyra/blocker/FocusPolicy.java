// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import com.lyra.blocker.family.HostRules;
import java.util.Set;

/** Independent focus-site policy. Family rules are evaluated separately and cannot be allowed here. */
public final class FocusPolicy {
    private FocusPolicy() {}
    private static boolean matches(String host, Set<String> entries) {
        return entries.stream().anyMatch(entry -> host.equals(entry) || host.endsWith("." + entry));
    }
    public static boolean blocks(String host, Set<String> denied, Set<String> allowed, boolean study) {
        String domain = HostRules.domain(host);
        if (domain == null || matches(domain, allowed)) return false;
        return study || matches(domain, denied);
    }
}
