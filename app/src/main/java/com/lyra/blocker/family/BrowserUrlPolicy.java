// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker.family;

import java.net.URI;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Visible HTTP(S) page rules. Never used as a DNS/content classifier. */
public final class BrowserUrlPolicy {
    public record Address(String host, int port, String path, List<String> query, String fragment) {}
    private BrowserUrlPolicy() {}
    public static Address parse(String value) {
        if (value == null || value.isBlank() || value.length() > 8192 || value.chars().anyMatch(Character::isWhitespace)) return null;
        try {
            URI uri = new URI(value.contains("://") ? value.trim() : "https://" + value.trim());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getRawUserInfo() != null) return null;
            String host = HostRules.address(value);
            if (host == null) {
                String candidate = uri.getHost();
                if (candidate == null) return null;
                if (candidate.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                    for (String part : candidate.split("\\.")) if (Integer.parseInt(part) > 255 || (part.length() > 1 && part.startsWith("0"))) return null;
                    host = candidate;
                } else if (candidate.matches("\\[[0-9a-fA-F:.]+\\]")) {
                    // This branch accepts only a numeric IPv6 literal; it never performs hostname DNS.
                    host = "[" + InetAddress.getByName(candidate).getHostAddress() + "]";
                } else return null;
            }
            int port = uri.getPort();
            if (port == -1 && uri.getHost() == null && uri.getRawAuthority() != null && uri.getRawAuthority().contains(":"))
                port = Integer.parseInt(uri.getRawAuthority().substring(uri.getRawAuthority().lastIndexOf(':') + 1));
            if (port < -1 || port == 0 || port > 65535) return null;
            if (port == 443 && "https".equalsIgnoreCase(uri.getScheme()) || port == 80 && "http".equalsIgnoreCase(uri.getScheme())) port = -1;
            String path = uri.normalize().getPath();
            if (path == null || path.isEmpty()) path = "/";
            // A decoded dot segment must be normalized too; encoded separators cannot evade a page rule.
            path = new URI(null, null, path.replace('\\', '/'), null).normalize().getPath();
            List<String> query = new ArrayList<>();
            if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&")) query.add(URLDecoder.decode(part, StandardCharsets.UTF_8));
            return new Address(host.toLowerCase(Locale.ROOT), port, path, List.copyOf(query), uri.getFragment() == null ? "" : uri.getFragment());
        } catch (Exception ignored) { return null; }
    }
    public static boolean validRule(String value) { return parse(value) != null; }
    public static boolean matches(String rule, String address) {
        Address expected = parse(rule), actual = parse(address);
        if (expected == null || actual == null || expected.port != actual.port) return false;
        boolean ip = expected.host.startsWith("[") || expected.host.matches("[0-9.]+");
        if (!(actual.host.equals(expected.host) || (!ip && actual.host.endsWith("." + expected.host)))) return false;
        List<String> remaining = new ArrayList<>(actual.query);
        for (String required : expected.query) if (!remaining.remove(required)) return false;
        String prefix = expected.path.endsWith("/") ? expected.path : expected.path + "/";
        return (actual.path.equals(expected.path) || actual.path.startsWith(prefix)) &&
            (expected.fragment.isEmpty() || expected.fragment.equals(actual.fragment));
    }
}
