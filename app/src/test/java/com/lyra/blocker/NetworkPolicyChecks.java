// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

import com.lyra.blocker.family.BrowserDomains;
import com.lyra.blocker.family.BrowserUrlPolicy;
import com.lyra.blocker.family.ResolverPolicy;
import java.util.Set;

public final class NetworkPolicyChecks {
    public static void main(String[] args) { run(); System.out.println("Resolver, browser page and VPN capability checks passed"); }
    public static void run() {
        check(ResolverPolicy.effectiveMode(ResolverPolicy.FAMILY, true, "blocked.example", Set.of()).equals(ResolverPolicy.NETWORK), "Parent pause did not release provider filtering");
        check(ResolverPolicy.effectiveMode(ResolverPolicy.FAMILY, false, "sub.allowed.example", Set.of("allowed.example")).equals(ResolverPolicy.NETWORK), "Explicit parent allow entry lost precedence");
        check(ResolverPolicy.effectiveMode(ResolverPolicy.FAMILY, false, "allowed.example.evil.test", Set.of("allowed.example")).equals(ResolverPolicy.FAMILY), "Lookalike bypassed filtered DNS");
        check(ResolverPolicy.endpoint(ResolverPolicy.FAMILY).startsWith("https://family."), "Adult provider lost encrypted DNS");
        try { ResolverPolicy.endpoint("unknown"); throw new AssertionError("Unknown provider silently accepted"); } catch (IllegalArgumentException expected) { /* expected */ }
        check(VpnAppPolicy.vpnService("com.lyra.blocker", "example.vpn", "android.permission.BIND_VPN_SERVICE"), "Declared VPN not identified");
        check(!VpnAppPolicy.vpnService("com.lyra.blocker", "com.lyra.blocker", "android.permission.BIND_VPN_SERVICE"), "Own filter was blocked");
        check(!VpnAppPolicy.vpnService("com.lyra.blocker", "example.vpn", "example.BIND_VPN_SERVICE"), "App-name/permission lookalike was classified as VPN");
        check(BrowserUrlPolicy.matches("example.com/private", "https://sub.example.com/private/lesson?x=1"), "Page subtree missed");
        check(!BrowserUrlPolicy.matches("example.com/private", "https://example.com/privately"), "Adjacent path was blocked");
        check(!BrowserUrlPolicy.matches("example.com/private", "https://example.com.evil.test/private"), "Lookalike host matched");
        check(BrowserUrlPolicy.matches("https://example.com/watch?v=123", "https://example.com/watch?tracking=yes&v=123"), "Required query changed with parameter ordering");
        check(!BrowserUrlPolicy.matches("https://example.com/watch?v=123", "https://example.com/watch?v=1234"), "Nearby query value matched");
        check(BrowserUrlPolicy.matches("example.com/private", "https://example.com/other/%2e%2e/private"), "Encoded dot path bypassed rule");
        check(BrowserUrlPolicy.matches("https://bücher.example:9443/private", "https://xn--bcher-kva.example:9443/private"), "IDN rule not normalized");
        check(!BrowserUrlPolicy.matches("https://bücher.example:9443/private", "https://xn--bcher-kva.example:9444/private"), "Nondefault port boundary ignored");
        check(BrowserUrlPolicy.parse("https://[2001:db8::1]/page") != null, "IPv6 browser address was ignored");
        check(BrowserUrlPolicy.matches("http://192.0.2.1/page", "https://192.0.2.1/page"), "Numeric browser address missed");
        check(!BrowserUrlPolicy.matches("192.0.2.1/page", "https://sub.192.0.2.1/page"), "An IP rule treated a lookalike as its subdomain");
        for (String invalid : new String[]{"javascript:alert(1)", "https://user:password@example.com/", "not an address", "https://example.com:0/", "http://999.0.0.1/"})
            check(!BrowserUrlPolicy.validRule(invalid), "Invalid page rule accepted");
        check(BrowserDomains.trustedField("com.android.chrome", "com.android.chrome:id/url_bar"), "Trusted browser field missed");
        check(!BrowserDomains.trustedField("com.android.chrome", "web.content:id/url_bar"), "Web content imitated the trusted address field");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
