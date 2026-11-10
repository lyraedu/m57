# M57 GitHub Actions

Read the root instructions and `docs/releases.md` before editing a workflow.

- `android-blocker.yml` is the ordinary push/PR gate: Python release-tool tests, Android debug and release unit tests, lint, and assembly. It may upload a debug artifact; it does not publish a signed release.
- `prepare-release.yml` is manually dispatched from `main` and writes a version commit/tag. `release.yml` runs on release tags or explicit dispatch and publishes a prerelease only after signing and exact-source checks.
- Do not expose signing secrets to pull requests or ordinary build jobs. Limit job permissions to what each job needs. Keep the certificate continuity check and source archive in the signed release path.
- A tag pushed by `GITHUB_TOKEN` does not automatically trigger another push workflow; the prepare job explicitly dispatches the signed workflow. Preserve that handoff or replace it with an equally verified mechanism.
- Check a workflow's actual run and artifacts before saying a build or release succeeded. Do not weaken tests, lint, or signer validation to obtain a green badge.
