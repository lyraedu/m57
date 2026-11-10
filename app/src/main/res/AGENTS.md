# Android resources

Read the repository root instructions. Resources here affect visible UI, service declarations, bundled notices, and the installed app's identity.

- Keep accessibility service and device-admin XML aligned with the manifest and actual user consent flow. Do not silently broaden observed event types or device policies.
- Give controls readable text and content descriptions; check color/blur fallback and touch exploration on a device when changing layout or drawables.
- Preserve the license terms of copied launcher/logo assets and the in-app attribution route. New third-party assets need their own origin and license notice.
- When changing app names, launcher icons, or strings, verify the public README, About/source link, and `DEVICE_TESTS.md` still describe what users will see.
- Resource changes need a Gradle build/lint pass; XML parsing alone does not prove the Android resource merger or UI behavior.
