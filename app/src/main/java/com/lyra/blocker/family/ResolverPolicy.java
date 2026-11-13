// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker.family;

import java.util.Set;

/** Explicit DNS provider selection; a filtered provider never falls back to unfiltered DNS. */
public final class ResolverPolicy {
    public static final String NETWORK = "network";
    public static final String MALWARE = "cloudflare-malware";
    public static final String FAMILY = "cloudflare-family";
    public static final Set<String> MODES = Set.of(NETWORK, MALWARE, FAMILY);
    private ResolverPolicy() {}
    public static String endpoint(String mode) {
        if (!MODES.contains(mode)) throw new IllegalArgumentException("Unknown resolver mode");
        if (NETWORK.equals(mode)) return null;
        return FAMILY.equals(mode) ? "https://family.cloudflare-dns.com/dns-query" : "https://security.cloudflare-dns.com/dns-query";
    }
    public static String effectiveMode(String mode, boolean paused, String host, Set<String> allowed) {
        if (!MODES.contains(mode)) throw new IllegalArgumentException("Unknown resolver mode");
        if (paused) return NETWORK;
        String domain = HostRules.domain(host);
        if (domain != null && allowed.stream().anyMatch(entry -> domain.equals(entry) || domain.endsWith("." + entry))) return NETWORK;
        return mode;
    }
}
