# Icon sources

Exported on 2026-10-19 from the maintainer-provided
[KrPlay Figma file](https://www.figma.com/design/ELQg5K0rDp17rLlXKRPsfI/KrPlay?node-id=0-1).
SVG files are the unmodified full-frame exports (256 × 256, artwork inset 18).
PNG/WebP files are deterministic raster derivatives; no temporary URLs are used
at runtime.

| Source | Figma node | Used by |
| --- | --- | --- |
| `krplay.svg` | `3:22` | Receiver adaptive/legacy launcher, store image, README |
| `krplay-client.svg` | `8:23` | Client adaptive/legacy launcher, README |
| `zeekr.svg` | `3:7` | Default CarPlay return-to-car icon, `res/raw/ic_car_home.png` |

Both Android apps provide mdpi through xxxhdpi legacy icons and Android 8+
adaptive resources. Adaptive foregrounds use the complete artwork with an 18dp
inset on a 108dp canvas, keeping the visible mark in the central safe area.
The original gradients and glass effects are rasterized rather than redrawn
as approximate Android vectors. The adaptive background extends beyond the
artwork for launcher masking; no inaccurate monochrome approximation is shipped.
The existing cast notification icon stays monochrome as required for Android
status icons. Settings, playback, Wi-Fi and other action symbols retain their
semantic Material icons. The Apple CarPlay category image is not replaced by
an unrelated app logo.

Regenerate from the repository root with Node.js and `sharp` available:

```sh
npm install --no-save --package-lock=false sharp
node scripts/generate_app_icons.cjs
```

`krplay/scripts/generate-app-icons.mjs` delegates to this generator for
compatibility. `krplay/docs/assets/app-icon.svg` is a generated compatibility copy.

The Figma file supplies no explicit image license. Confirm authorship and
redistribution terms before publication. Zeekr and Apple assets/marks are not
relicensed by the project's GPL or AGPL texts; see
[third-party notices](../../../THIRD_PARTY_NOTICES.md).
