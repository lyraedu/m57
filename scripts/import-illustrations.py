#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Package reviewed imagegen outputs as bounded local Android WebP artwork."""
import argparse
import hashlib
import json
import os
import tempfile
from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', type=Path, default=ROOT / 'branding/illustrations/source')
args = parser.parse_args()
jobs = [json.loads(line) for line in (ROOT / 'branding/illustrations/prompts.jsonl').read_text().splitlines() if line.strip()]
inputs = [(job, args.source / job['out']) for job in jobs]
missing = [path.name for _, path in inputs if not path.is_file()]
if missing:
    raise SystemExit('No artwork changed. Missing generated images: ' + ', '.join(missing))
output = ROOT / 'app/src/main/assets/illustrations'
manifest = []
# Decode and validate the complete set before touching any packaged artwork.
with tempfile.TemporaryDirectory(prefix='.illustrations-', dir=ROOT / 'app/src/main') as temporary:
    stage = Path(temporary)
    for job, path in inputs:
        with Image.open(path) as source:
            if source.width < 768 or source.height < 512:
                raise SystemExit(f'No artwork changed. Image is too small: {path.name}')
            image = ImageOps.fit(source.convert('RGB'), (960, 640), method=Image.Resampling.LANCZOS)
            target = stage / (path.stem + '.webp')
            image.save(target, 'WEBP', quality=88, method=6)
            manifest.append({'name': path.stem, 'source_sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                             'asset_sha256': hashlib.sha256(target.read_bytes()).hexdigest(), 'width': 960, 'height': 640})
    output.mkdir(parents=True, exist_ok=True)
    for target in stage.glob('*.webp'):
        os.replace(target, output / target.name)
(ROOT / 'branding/illustrations/packaged-assets.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(f'Packaged {len(manifest)} local illustrations. Review every image before distributing an APK.')
