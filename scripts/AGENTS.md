# Build and release scripts

Read the root instructions and [docs/releases.md](../docs/releases.md). Scripts here prepare, check, sign, and package distributable artifacts.

- Preserve failure as failure: do not reuse an old APK, ignore a failed test/lint step, or emit a release from a dirty or changing source tree.
- Keep the exact source commit, version, certificate digest, checksums, licenses, notices, and APK together. `M57_SOURCE_REF` is a full 40-character commit SHA; generated source links must point to the current repository.
- Signing material lives outside Git. Never print or interpolate passwords or base64 keystores into shell commands, logs, issues, or artifacts. `upload-signing-secrets.py` targets an explicitly named repository.
- Keep version validation and monotonic versionCode behavior in `prepare-release.py`/`check-release.py`. Do not rewrite published tags or release notes to make a check pass.
- Run `python3 -m unittest discover -s scripts -p 'test_*.py'` for script changes. For signing/release changes, also inspect the GitHub workflows and verify the established signer before publishing.
