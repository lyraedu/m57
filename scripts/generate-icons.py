#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Generate launcher assets from the owner's originals. Requires Pillow; no network."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "branding/source"
RES = ROOT / "app/src/main/res"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
LANCZOS = Image.Resampling.LANCZOS


def foreground() -> tuple[Image.Image, tuple[float, float], float]:
    image = Image.open(SOURCE / "foreground.png").convert("RGBA")
    alpha = image.getchannel("A")
    bounds = alpha.point(lambda value: 255 if value >= 240 else 0).getbbox()
    if bounds is None:
        raise ValueError("Foreground has no solid artwork")
    center = ((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2)
    # Ignore practically invisible export noise, retaining the ring and its shadow.
    alpha = alpha.point(lambda value: value if value >= 8 else 0)
    image.putalpha(alpha)
    radius = max(math.hypot(x - center[0], y - center[1])
                 for y in range(image.height) for x in range(image.width)
                 if alpha.getpixel((x, y)) >= 8)
    return image, center, radius


def layer(image: Image.Image, center: tuple[float, float], radius: float, size: int) -> Image.Image:
    # 108dp layers, essential artwork inside the central 66dp circle, with a 2dp margin.
    scale = (31 / 108 * size) / radius
    dimensions = (round(image.width * scale), round(image.height * scale))
    scaled = image.resize(dimensions, LANCZOS)
    result = Image.new("RGBA", (size, size))
    result.alpha_composite(scaled, (round(size / 2 - center[0] * scale), round(size / 2 - center[1] * scale)))
    return result


def mask(size: int, kind: str) -> Image.Image:
    output = Image.new("L", (size, size))
    draw = ImageDraw.Draw(output)
    if kind == "circle":
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    elif kind == "squircle":
        points = []
        for index in range(720):
            angle = index * math.pi / 360
            c, s = math.cos(angle), math.sin(angle)
            points.append((size / 2 + math.copysign(abs(c) ** 0.5, c) * (size / 2 - 1),
                           size / 2 + math.copysign(abs(s) ** 0.5, s) * (size / 2 - 1)))
        draw.polygon(points, fill=255)
    elif kind == "teardrop":
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 2, fill=255)
        draw.rectangle((size // 2, 0, size - 1, size // 2), fill=255)
    else:
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.24), fill=255)
    return output


def viewport(image: Image.Image) -> Image.Image:
    inset = image.width // 6  # launcher displays the central 72dp out of 108dp
    return image.crop((inset, inset, image.width - inset, image.height - inset))


def generate() -> dict[Path, Image.Image]:
    image, center, radius = foreground()
    background = Image.open(SOURCE / "background.png").convert("RGBA")
    if background.getchannel("A").getextrema() != (255, 255):
        raise ValueError("Adaptive background must be opaque")
    outputs: dict[Path, Image.Image] = {}
    mono_source = Image.new("RGBA", image.size, "white")
    mono_alpha = image.getchannel("A").point(lambda value: 255 if value >= 245 else 0)
    # Remove isolated export specks, keeping only the connected solid ring contour.
    pixels = mono_alpha.load()
    seen = bytearray(image.width * image.height)
    largest: list[tuple[int, int]] = []
    for y in range(image.height):
        for x in range(image.width):
            key = y * image.width + x
            if not pixels[x, y] or seen[key]:
                continue
            stack = [(x, y)]
            seen[key] = 1
            component = []
            while stack:
                px, py = stack.pop()
                component.append((px, py))
                for nx, ny in ((px - 1, py), (px + 1, py), (px, py - 1), (px, py + 1)):
                    if 0 <= nx < image.width and 0 <= ny < image.height:
                        index = ny * image.width + nx
                        if pixels[nx, ny] and not seen[index]:
                            seen[index] = 1
                            stack.append((nx, ny))
            if len(component) > len(largest):
                largest = component
    contour = Image.new("L", image.size)
    contour_pixels = contour.load()
    for x, y in largest:
        contour_pixels[x, y] = 255
    mono_source.putalpha(contour.filter(ImageFilter.GaussianBlur(0.7)))
    for name, density in DENSITIES.items():
        size = round(108 * density)
        fg = layer(image, center, radius, size)
        bg = ImageOps.fit(background, (size, size), method=LANCZOS)
        outputs[RES / f"drawable-{name}/ic_launcher_foreground.png"] = fg
        outputs[RES / f"drawable-{name}/ic_launcher_background.png"] = bg
        # Alpha contour from the original solid ring, excluding its translucent inner shadow.
        outputs[RES / f"drawable-{name}/ic_launcher_monochrome.png"] = layer(mono_source, center, radius, size)
        composite = viewport(Image.alpha_composite(bg, fg))
        legacy_size = round(48 * density)
        for suffix, shape in [("", "rounded"), ("_round", "circle")]:
            legacy = composite.resize((legacy_size, legacy_size), LANCZOS)
            legacy.putalpha(mask(legacy_size, shape))
            outputs[RES / f"mipmap-{name}/ic_launcher{suffix}.png"] = legacy
        alpha = fg.getchannel("A")
        limit = 33 / 108 * size
        for y in range(size):
            for x in range(size):
                if alpha.getpixel((x, y)) >= 128 and math.hypot(x - size / 2, y - size / 2) > limit:
                    raise ValueError(f"Foreground exceeds safe circle at {name}")
    tight = image.crop(image.getchannel("A").getbbox())
    logo = Image.new("RGBA", (512, 512))
    fitted = ImageOps.contain(tight, (488, 488), LANCZOS)
    logo.alpha_composite(fitted, ((512 - fitted.width) // 2, (512 - fitted.height) // 2))
    outputs[RES / "drawable-nodpi/m57_logo.png"] = logo
    outputs[ROOT / "branding/app-icon-512.png"] = Image.open(SOURCE / "app-icon.png").convert("RGBA").resize((512, 512), LANCZOS)
    preview = Image.new("RGB", (1200, 620), "#FBF8F5")
    draw = ImageDraw.Draw(preview)
    draw.text((32, 22), "M57 / original layers, launcher masks and themed icons", fill="#3E2E27")
    composite = viewport(Image.alpha_composite(outputs[RES / "drawable-xxxhdpi/ic_launcher_background.png"], outputs[RES / "drawable-xxxhdpi/ic_launcher_foreground.png"]))
    mono = viewport(outputs[RES / "drawable-xxxhdpi/ic_launcher_monochrome.png"])
    for index, shape in enumerate(["circle", "squircle", "rounded", "teardrop"]):
        tile = composite.resize((220, 220), LANCZOS)
        tile.putalpha(mask(220, shape))
        x = 35 + index * 295
        preview.paste(tile, (x, 65), tile)
        draw.text((x, 300), shape, fill="#3E2E27")
        themed = Image.new("RGBA", (220, 220), "#E1E8DB")
        symbol = Image.new("RGBA", (220, 220), "#3D5138")
        symbol.putalpha(mono.getchannel("A").resize((220, 220), LANCZOS))
        themed = Image.alpha_composite(themed, symbol)
        themed.putalpha(mask(220, shape))
        preview.paste(themed, (x, 350), themed)
        draw.text((x, 585), "themed / " + shape, fill="#3E2E27")
    outputs[ROOT / "docs/assets/launcher-preview.png"] = preview
    return outputs


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify generated files and source digests without changing files")
    args = parser.parse_args()
    outputs = generate()
    manifest = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(SOURCE.glob("*.png"))}
    manifest_path = ROOT / "branding/source-sha256.json"
    failures = []
    for path, image in outputs.items():
        if args.check:
            if not path.exists():
                failures.append(str(path.relative_to(ROOT)))
            else:
                with Image.open(path) as actual:
                    if actual.convert("RGBA").size != image.size or actual.convert("RGBA").tobytes() != image.convert("RGBA").tobytes():
                        failures.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            image.save(path, optimize=True)
    if args.check:
        if not manifest_path.exists() or json.loads(manifest_path.read_text()) != manifest:
            failures.append(str(manifest_path.relative_to(ROOT)))
        if failures:
            raise SystemExit("Icon assets differ; regenerate: " + ", ".join(failures))
        print(f"Verified {len(outputs)} icon/preview assets and all source digests; essential foreground fits the 66dp safe circle.")
    else:
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Generated {len(outputs)} icon/preview assets from owner-supplied sources.")


if __name__ == "__main__":
    main()
