#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Build a fresh Preview, update the selected device in place, and open the app.
set -euo pipefail
cd -- "$(dirname -- "$0")/.."
if [[ -f "$HOME/.config/m57/android-env.sh" ]]; then
    source "$HOME/.config/m57/android-env.sh"
fi
python3 - "$@" <<'PY'
import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess

parser = argparse.ArgumentParser(description='Build, install and open only M57 Preview. Existing app data and permissions are preserved.')
parser.add_argument('--serial', help='adb serial; required when more than one authorized device is connected')
args = parser.parse_args()
if shutil.which('adb') is None:
    parser.error('adb is unavailable. Run bash scripts/setup-android.sh first.')
sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
if not sdk or not (Path(sdk) / 'build-tools/35.0.0/aapt2').is_file():
    parser.error('Android build-tools 35.0.0 are unavailable. Run bash scripts/setup-android.sh first.')

def command(*arguments, timeout=60):
    return subprocess.run(list(arguments), check=True, capture_output=True, text=True, timeout=timeout).stdout

devices = [line.split()[0] for line in command('adb', 'devices').splitlines()[1:]
           if len(line.split()) >= 2 and line.split()[1] == 'device']
if args.serial:
    if args.serial not in devices:
        parser.error('The selected device is not connected and authorized. Connect/unlock it and approve USB debugging.')
    serial = args.serial
elif len(devices) == 1:
    serial = devices[0]
else:
    parser.error('Connect one authorized phone, or choose it with --serial. No APK was built or installed.')

def adb(*arguments, timeout=60):
    return command('adb', '-s', serial, *arguments, timeout=timeout)

def production_identity():
    result = adb('shell', 'dumpsys', 'package', 'com.lyra.blocker')
    return [line.strip() for line in result.splitlines() if any(key in line for key in
            ('versionCode=', 'versionName=', 'firstInstallTime=', 'lastUpdateTime='))]

# A failed build raises before the installation step. An old APK is never a fallback.
subprocess.run(['./gradlew', ':app:assemblePreview', '--no-daemon', '--max-workers=2'], check=True)
apk = Path('app/build/outputs/apk/preview/app-preview.apk')
badging = command(str(Path(sdk) / 'build-tools/35.0.0/aapt2'), 'dump', 'badging', str(apk))
if not re.search(r"^package: name='com\.lyra\.blocker\.preview' ", badging, re.M):
    raise SystemExit('Refusing installation: this artifact is not the isolated M57 Preview application.')
before = production_identity()
print(adb('install', '-r', str(apk), timeout=180), flush=True)
if production_identity() != before:
    raise SystemExit('Production package identity changed unexpectedly. Stop and inspect before continuing.')
launch = adb('shell', 'am', 'start', '-W', '-n', 'com.lyra.blocker.preview/com.lyra.blocker.MainActivity')
print(launch, flush=True)
if 'Status: ok' not in launch:
    raise SystemExit('Preview installed, but Android did not confirm a successful launch.')
print('M57 Preview updated and opened. No data clear or permission grant was performed.')
print('Installed APK SHA-256:', hashlib.sha256(apk.read_bytes()).hexdigest())
PY
