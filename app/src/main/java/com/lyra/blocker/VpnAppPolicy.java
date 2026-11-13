// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

/** Recognise a declared VPN capability rather than guessing from app names. */
public final class VpnAppPolicy {
    private VpnAppPolicy() {}
    public static boolean vpnService(String ownPackage, String candidatePackage, String permission) {
        return candidatePackage != null && !candidatePackage.equals(ownPackage) &&
            "android.permission.BIND_VPN_SERVICE".equals(permission);
    }
}
