# M57 artwork

The owner supplied `bg.png` and `appicon .png`, then selected
`m57-ring-symbol-transparent.png` as the replacement foreground on 13 November
2026. These current originals live in `source/` with portable filenames.
The earlier foreground is no longer packaged. Their SHA-256
digests are recorded in `source-sha256.json`. Original owner-supplied material
and these generated derivatives use the project's AGPL-3.0-only terms.

`app-icon-512.png` preserves the complete supplied reference image. Its baked
rounded tile and dark outer canvas are not used as an adaptive foreground.
Android's actual launcher resources combine the separate background and ring
layers, letting each launcher choose its own mask and parallax behavior.

Run from the repository root, using Python 3 and Pillow:

```sh
python3 scripts/generate-icons.py
python3 scripts/generate-icons.py --check
```

Generated assets include:

- Opaque background, transparent foreground and alpha-only monochrome layers at
  mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi. The 108dp canvas contains the essential
  ring within a central 66dp circle, with an extra margin. The monochrome contour
  removes the supplied translucent shadow and disconnected export specks.
- Normal and round fallback launcher PNGs at the five Android densities.
- Adaptive `ic_launcher` and `ic_launcher_round` resources, including the themed
  monochrome layer, referenced by the manifest. Android 14+ is the app's minimum.
- An updated transparent in-app ring and the standard Android system splash
  using the adaptive icon on M57's ivory background. There is no timed splash
  Activity or forced launch delay.
- [Shape and theme previews](../docs/assets/launcher-preview.png). These are
  rendered mask simulations, not screenshots from manufacturers' phones.

Circle, squircle, rounded-square and teardrop masks are illustrations of Android
launcher behavior, not promises that a given OEM always uses one shape. Themed
icon availability and colors depend on launcher support and user settings.
See [Android's adaptive icon specification](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
and [system splash specification](https://developer.android.com/develop/ui/views/launch/splash-screen).
Test Samsung, Pixel, Xiaomi/Redmi/Poco, Oppo/OnePlus/Realme and Vivo launchers,
including themed icons, screen scaling, badges, folder views and cold/warm starts.
