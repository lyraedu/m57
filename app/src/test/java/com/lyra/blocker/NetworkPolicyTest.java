// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;
import org.junit.Test;
public final class NetworkPolicyTest {
    @Test public void providerBoundariesAndVisiblePages() { NetworkPolicyChecks.run(); }
}
