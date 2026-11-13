#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Exercise M57 Preview through Android's real UI. Never clears or configures the production app."""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'com.lyra.blocker.preview'
COMPONENT = PACKAGE + '/com.lyra.blocker.MainActivity'


class PreviewDevice:
    def __init__(self, serial: str, output: Path):
        self.prefix = ['adb', '-s', serial]
        self.output = output
        output.mkdir(parents=True, exist_ok=True)
        self.actions: list[str] = []

    def command(self, *arguments: str, binary: bool = False):
        return subprocess.run(self.prefix + list(arguments), check=True, capture_output=True,
                              text=not binary, timeout=30).stdout

    def shell(self, *arguments: str) -> str:
        return self.command('shell', *arguments)

    def own_window(self) -> None:
        # Samsung's "window windows" subsection omits global focus on some builds.
        focus = self.shell('dumpsys', 'window')
        lines = [line for line in focus.splitlines() if 'mCurrentFocus=' in line]
        if not any(PACKAGE + '/' in line for line in lines):
            raise RuntimeError('M57 Preview is not the focused app; refusing further UI actions')

    def tree(self) -> ET.Element:
        self.own_window()
        self.shell('uiautomator', 'dump', '--compressed', '/sdcard/m57-preview-window.xml')
        data = self.shell('cat', '/sdcard/m57-preview-window.xml')
        return ET.fromstring(data)

    def screenshot(self, name: str) -> None:
        # Check the actual accessibility tree too: focus alone may precede drawing.
        root = self.tree()
        if not any(node.get('package') == PACKAGE for node in root.iter('node')):
            raise RuntimeError('Preview has no visible UI; refusing screenshot')
        data = self.command('exec-out', 'screencap', '-p', binary=True)
        self.own_window()
        (self.output / (name + '.png')).write_bytes(data)
        self.actions.append(name)

    def find(self, text: str, *, partial: bool = False, scroll: bool = True) -> ET.Element:
        for attempt in range(7 if scroll else 1):
            root = self.tree()
            for node in root.iter('node'):
                labels = [node.get('text', ''), node.get('content-desc', '')]
                if any(text in label if partial else label == text for label in labels) and node.get('package') == PACKAGE:
                    return node
            if scroll:
                bounds = root.find('node').get('bounds', '[0,0][720,1600]')
                values = [int(value) for value in re.findall(r'\d+', bounds)]
                width, height = values[2], values[3]
                self.shell('input', 'swipe', str(width // 2), str(height * 4 // 5), str(width // 2), str(height // 3), '280')
        raise AssertionError('UI label not found: ' + text)

    def tap(self, text: str, *, partial: bool = False) -> None:
        node = self.find(text, partial=partial)
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
        self.shell('input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(0.4)

    def reset(self) -> None:
        self.shell('am', 'force-stop', PACKAGE)
        result = self.shell('pm', 'clear', PACKAGE)
        if 'Success' not in result:
            raise RuntimeError('Could not reset the dedicated Preview installation')
        self.shell('am', 'start', '-W', '-n', COMPONENT)
        time.sleep(1)

    def personal(self) -> None:
        self.reset()
        self.screenshot('01-welcome')
        self.tap('Continue')
        self.screenshot('02-use-case')
        self.tap('For me', partial=True)
        self.tap('Continue')
        lyra = self.find('Include Lyra for AI learning')
        if lyra.get('checked') != 'false':
            raise AssertionError('Independent setup enabled Lyra without consent')
        self.screenshot('03-optional-lyra')
        self.tap('Continue')
        self.screenshot('04-video-choice')
        self.tap('Continue')
        self.screenshot('05-permission-explanation')
        self.tap('Continue with timer only')
        self.screenshot('06-ready')
        self.tap('Start using M57')
        self.find('Start a focus session')
        self.screenshot('07-home')
        self.tap('Focus')
        self.screenshot('08-focus-plan')
        self.tap('Begin my session')
        self.find('Your focus time')
        self.screenshot('09-running-session')
        self.tap('Finish session')
        self.tap('Continue')
        self.find('Recent sessions')
        self.screenshot('10-history')
        data = self.command('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/m57_focus.xml')
        preferences = ET.fromstring(data)
        history = preferences.find("string[@name='history']")
        if history is None or len(json.loads(history.text)) != 1:
            raise AssertionError('Ending one session did not produce exactly one record')
        self.tap('Plan')
        self.find('Focus list')
        self.screenshot('11-plan')
        self.tap('Settings')
        self.tap('See what M57 does', partial=True)
        self.tap('Breaks belong in the plan.')
        self.find('A personal break never pauses', partial=True)
        self.screenshot('12-feature-guide')
        self.shell('input', 'keyevent', 'KEYCODE_BACK')

    def parent_intro(self) -> None:
        self.reset()
        self.tap('Continue')
        self.tap('For my child', partial=True)
        self.tap('Continue')
        self.tap('Continue')
        self.tap('Continue')
        self.find('Keep the controls', partial=True)
        self.screenshot('13-parent-password-explanation')
        self.tap('Continue')
        self.tap('Create parent password')
        self.find('Keep the controls with you.')
        self.screenshot('14-parent-password-form')
        # Do not enter credentials or accept any Android permission on a person's phone.


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='Explicit adb device or emulator serial')
    parser.add_argument('--output', type=Path, default=Path('/tmp/m57-preview-smoke'))
    parser.add_argument('--reset-preview', action='store_true', help='Allow clearing only com.lyra.blocker.preview test data')
    args = parser.parse_args()
    if not args.reset_preview:
        parser.error('--reset-preview is required; the dedicated Preview app will be reset')
    device = PreviewDevice(args.serial, args.output)
    try:
        device.personal()
        device.parent_intro()
        report = {'package': PACKAGE, 'screens': device.actions, 'status': 'passed',
                  'scope': 'Personal setup, independent mode, timer/history, navigation, guides and parent introduction; no permission or enforcement acceptance'}
        (args.output / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
        print('M57 Preview smoke passed:', len(device.actions), 'screens; production app and Android permissions were not changed.')
    finally:
        device.shell('am', 'force-stop', PACKAGE)
        device.shell('rm', '-f', '/sdcard/m57-preview-window.xml')


if __name__ == '__main__':
    main()
