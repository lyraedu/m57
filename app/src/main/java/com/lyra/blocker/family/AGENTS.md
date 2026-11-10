# Family browsing and DNS

Read the root and parent Android instructions. This package owns the opt-in hosts catalog, synchronization, browser URL checks, and local DNS VPN.

- Keep VPN consent explicit. Start/stop and revoke paths must release descriptors, callbacks, workers, and notifications. Choose an eligible underlying network; do not route DNS back into M57's VPN.
- `DnsPacket.java` validates packet boundaries and constructs replies. Keep malformed requests bounded and preserve correct IPv4/IPv6 UDP lengths and checksums. Test query types, failure responses, and truncated input.
- `HostRules.java` and `BrowserDomains.java` must use exact host and subdomain boundaries; avoid substring matches that block unrelated sites. Administrator allow entries take precedence over block entries.
- `FamilyRepository.kt`/`FamilySyncJob.kt` validate downloaded lists and update the cache atomically. Failed or unexpectedly shrunken downloads should retain the last usable copy. Do not bundle changing host lists into the APK without reviewing their separate licenses.
- DNS filtering is not encrypted transport or full browser coverage. Do not claim support for Private DNS, Secure DNS, direct IP access, or another simultaneous VPN unless implemented and tested.
- Do not persist DNS queries or browsing history or put them in logs. Check the `FamilyRuleChecks` and `DnsPacketChecks` suites and document device/network evidence for VPN changes.
