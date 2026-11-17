# Lumio Editorial — approved colors and typography

Approved by the project owner on November 17, 2026. This specification continues Taste Ticket C — Editorial and defines the selected light/dark color system, interface font pairing, weights, type scale, and usage rules. Approval is recorded in [DECISIONS.md](DECISIONS.md). No native implementation is included.

## Intent

Warm paper, strong ink, restrained coral, and substantial serif headings create the character of a contemporary cinema journal. Use flat color rather than paper textures behind reading text. Let supplied movie posters retain their original colors. Keep most interface area neutral; reserve coral for primary actions and selected states.

## Color tokens

| Role | Light | Dark |
| --- | --- | --- |
| Canvas | Paper `#F7F2E8` | Night `#1D1C1A` |
| Raised surface | Warm white `#FFFCF7` | Charcoal `#292723` |
| Subtle surface / input fill | Parchment `#EEE7DC` | Deep stone `#34312C` |
| Primary text | Ink `#242320` | Paper `#F7F2E8` |
| Secondary text | Stone `#69635C` | Pale stone `#BDB4A8` |
| Primary action / selection fill | Coral `#F47B65` | Coral `#F47B65` |
| Text and icons on coral | Ink `#242320` | Ink `#242320` |
| Link / active foreground / focus ring | Ember `#A83F30` | Coral `#F47B65` |
| Essential control outline | Taupe `#8C8378` | Taupe `#8C8378` |
| Decorative divider | Sand `#D9CFC0` | Warm gray `#49443D` |
| Like / correct / success foreground | Pine `#3C6750` | Sage `#9CC8AA` |
| Dislike / incorrect / error foreground | Brick `#A73838` | Rose `#F3A09B` |
| Warning foreground | Ochre `#785A18` | Wheat `#E4C47D` |
| Unseen / neutral feedback | Secondary text | Secondary text |

Semantic foregrounds are for use on the canvas or raised surface, not arbitrary poster images or coral. Use visible text and an icon alongside feedback colors. Dislike is a preference action, so pair its color with the explicit Dislike label; do not present it as a system failure. Keep source ratings neutral. Use labels and shape as well as coral to identify selection.

Primary buttons use Ink on Coral in both themes. Links use Ember in light mode and Coral in dark mode, with an underline in running text. Do not use ivory/white text on Coral or Coral as small text on Paper. Secondary buttons use normal text and the essential outline token. Decorative dividers must not be the only boundary identifying an input. Focus rings need space from the control so they contrast with the surrounding surface.

Do not reduce enabled text opacity to create a secondary style; use its token. Disabled controls need a separate state specification during component design. Place text over artwork on an opaque neutral panel unless that image-specific pairing has been checked.

## Measured contrast

Calculated from the exact opaque sRGB values above using relative luminance: `(Llighter + 0.05) / (Ldarker + 0.05)`. Rounded only for display.

| Foreground / background | Ratio |
| --- | --- |
| Ink / Paper | 14.08:1 |
| Stone / Paper | 5.32:1 |
| Ink / Coral | 5.89:1 |
| Paper / Night | 15.26:1 |
| Pale stone / Charcoal | 7.28:1 |
| Ember / Paper | 5.51:1 |
| Coral / Paper — do not use for text | 2.39:1 |
| Taupe / Paper | 3.34:1 |
| Taupe / Charcoal | 4.00:1 |
| Pine / Paper | 5.79:1 |
| Sage / Charcoal | 7.99:1 |
| Brick / Paper | 5.76:1 |
| Rose / Charcoal | 7.32:1 |
| Ochre / Paper | 5.75:1 |
| Wheat / Charcoal | 8.87:1 |

These are pair measurements, not validation of a complete interface. Typography, control states, image overlays, focus, and native accessibility still require review in context.

## Typeface pairing

**Fraunces** for screen titles, section headings, and prominent movie titles. Start with weight 600 for large display titles and 600 for smaller headings; use 700 sparingly for a short hero headline. Keep SOFT at 0 and WONK at 0 for a restrained result; use optical sizing appropriate to the rendered size. Avoid italic as the default heading style.

**DM Sans** for body copy, metadata, navigation, buttons, filters, settings, trivia questions and answers. Use 400 for reading text, 500 for metadata emphasis, and 600 for controls. Use tabular numerals for countdowns and changing scores, while retaining ordinary numerals in prose.

This defines approved interface typography. Fraunces is not identified as the font in the generated Lumio wordmark, and interface font approval does not replace the approved logo artwork. The wordmark still needs a separately reviewed production master.

## Type scale

Sizes are nominal Android sp / iOS pt design targets before user scaling. Line heights describe design intent; respect native font metrics and accessibility scaling rather than fixing text containers.

| Role | Typeface / weight | Size / line height |
| --- | --- | --- |
| Display / short hero | Fraunces 600 | 36 / 42 |
| Screen title | Fraunces 600 | 30 / 36 |
| Section heading | Fraunces 600 | 24 / 30 |
| Movie card title | Fraunces 600 | 20 / 26 |
| Reading / trivia question | DM Sans 400 | 17 / 26 |
| Button / answer / filter | DM Sans 600 | 16 / 22 |
| Metadata / supporting text | DM Sans 400 or 500 | 14 / 20 |
| Navigation / compact caption | DM Sans 500 | 13 / 18 |

Use sentence case, natural tracking, and normal word spacing. Reserve uppercase for short, optional editorial eyebrows at 13/18 with modest tracking; never use uppercase for long labels or essential instructions. Let translated headings wrap, and increase component height rather than shrinking text. Keep long synopses comfortably narrow on wide screens. Use the same semantic roles in Android and iOS, adapted to each platform's text scaling.

## Sources and licenses

Verified against Google Fonts source files on November 17, 2026:

- [Fraunces metadata](https://github.com/google/fonts/blob/main/ofl/fraunces/METADATA.pb), [font project](https://github.com/undercasetype/Fraunces), [SIL OFL 1.1 license](https://github.com/google/fonts/blob/main/ofl/fraunces/OFL.txt).
- [DM Sans metadata](https://github.com/google/fonts/blob/main/ofl/dmsans/METADATA.pb), [font project](https://github.com/googlefonts/dm-fonts), [SIL OFL 1.1 license](https://github.com/google/fonts/blob/main/ofl/dmsans/OFL.txt).

Both metadata files list Latin and Latin Extended subsets. Verify the actual chosen font builds with English, Spanish, German, and Portuguese before packaging: `Discover · Películas · Empfehlungen · Recomendações · ação · coração · mañana · für · groß · ÄÖÜäöüß · ¿Qué ver? · 0–9`.

When distributing fonts, retain each font's copyright notice and license. Native apps should bundle the reviewed font files for offline availability; no app font resources have been downloaded or packaged in this design step. Preview fonts may load from Google Fonts and fall back if unavailable. No native rendering or language-layout validation has been performed.

## Approval boundary

The palette and interface font system are approved. Component states, logo masters, icon family, launcher artwork, and remaining branding decisions are separate work. Approval does not establish that native rendering, localization layouts, or accessibility have been validated.
