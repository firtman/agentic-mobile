# Android asset provenance

The interface font and icon selections are approved in `design/branding/DECISIONS.md`. These resources are bundled for local use. Font files and notices were downloaded on 2026-11-17 from the Google Fonts repository's `main` branch; the hashes below identify the exact copies bundled here.

| Resource | Source | SHA-256 |
| --- | --- | --- |
| `res/font/fraunces_variable.ttf` | [Fraunces variable font](https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/Fraunces%5BSOFT%2CWONK%2Copsz%2Cwght%5D.ttf) | `177ff6c0f14e5550a3c624247cd1189611d4eb65d000b14944c63d967958abbb` |
| `res/font/dm_sans_variable.ttf` | [DM Sans variable font](https://raw.githubusercontent.com/google/fonts/main/ofl/dmsans/DMSans%5Bopsz%2Cwght%5D.ttf) | `8cd08d97e89c24d0aa92edd2f0f4c8ee6195eee9b7c9f154865a58b02f0c1c0d` |
| `assets/licenses/fraunces_ofl.txt` | [Fraunces OFL notice](https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/OFL.txt) | `bdf4c22802eaf804f998195871c6b8938aac2ac14b2d78a8bd66a6f1eced833b` |
| `assets/licenses/dm_sans_ofl.txt` | [DM Sans OFL notice](https://raw.githubusercontent.com/google/fonts/main/ofl/dmsans/OFL.txt) | `9af36190332437f5ecd09974de43c1f7c77a310a996cdd8ceb25628b458840e1` |
| `assets/licenses/lucide.txt` | Copy of `design/branding/icons/lucide/LICENSE` | `b495047bd93a9b06913511076f504daba17d5bbeb3e0650f3bb53a4220329c57` |

The paths in the table are relative to `android/app/src/main/`. Fraunces supports the weight, optical size, softness and wonk axes; DM Sans supports weight and optical size. Both carry the SIL Open Font License in the bundled notices.

The 29 `lucide_*.xml` drawables are converted from the approved SVGs in `design/branding/icons/lucide/`, pinned to Lucide commit `149eba0aea48178a5ab1efe2c701de1da94f4be8`. The source manifest there records the exact upstream URLs and hashes. The converter preserves paths and turns circle, line, and rounded rectangle elements into equivalent path geometry; it retains the 24-unit viewport, two-unit strokes, and round caps and joins. Black strokes are intended to be tinted by the UI. Recreate the files from the repository root with:

```sh
python3 android/tools/convert_lucide.py
```

The converter handles the element types present in the pinned masters and fails on unsupported elements. Native rendering and accessibility still require app-level validation.

The interim adaptive launcher copies `design/branding/taste-ticket/lumio-c-symbol-dark.png` unchanged into `res/drawable-nodpi/launcher_symbol.png`, displayed with an inset on Paper. This is raster packaging, not a vector logo master or production launcher approval.
