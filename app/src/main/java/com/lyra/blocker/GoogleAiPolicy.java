// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/** Bounded opt-in Google AI Mode URL recognition. Ordinary search is left available. */
public final class GoogleAiPolicy {
    private GoogleAiPolicy() {}
    public static boolean address(String value) {
        if (value == null || value.length() > 16_384) return false;
        try {
            URI uri = new URI(value.contains("://") ? value.trim() : "https://" + value.trim());
            String host = uri.getHost();
            if (host == null || !Set.of("google.com", "www.google.com").contains(host.toLowerCase(Locale.ROOT)) ||
                !Set.of("https", "http").contains(uri.getScheme()) || uri.getUserInfo() != null ||
                (uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80)) return false;
            String path = uri.getPath();
            if ("/aimode".equals(path) || "/aimode/".equals(path)) return true;
            if (!"/search".equals(path) || uri.getRawQuery() == null) return false;
            int count = 0;
            boolean ai = false;
            for (String part : uri.getRawQuery().split("&")) {
                String[] pair = part.split("=", 2);
                if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals("udm")) {
                    count++;
                    ai = pair.length == 2 && URLDecoder.decode(pair[1], StandardCharsets.UTF_8).equals("50");
                }
            }
            return count == 1 && ai;
        } catch (IllegalArgumentException | java.net.URISyntaxException ignored) { return false; }
    }
}
