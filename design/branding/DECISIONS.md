# Lumio branding decisions

Status: selected by the project owner on November 17, 2026.

## Approved choices

| Item | Owner's selection |
| --- | --- |
| App name | **Lumio** |
| Logo direction | **Taste Ticket** (direction 02 in the initial exploration) |
| Logo variation | **C — Editorial** in the Taste Ticket refinement board |
| Color system | **Editorial light/dark palette** in [COLORS-TYPOGRAPHY.md](COLORS-TYPOGRAPHY.md), including semantic colors and usage rules |
| Interface typography | **Fraunces** for editorial headings and movie titles; **DM Sans** for body text and controls, with the weights and type scale in [COLORS-TYPOGRAPHY.md](COLORS-TYPOGRAPHY.md) |
| UI icon family | **Lucide outline**, with the 29-vector set, mappings, sizing, and selection rules in [ICONS.md](ICONS.md) |

These choices supersede the other explored directions and variations. Continue from C; do not treat A — Original or B — Compact as the selected design. The lowercase wordmark from A is not the selected wordmark.

The owner approved the color and typography system on November 17, 2026. Core colors are Paper `#F7F2E8`, Ink `#242320`, and Coral `#F47B65`, with Night `#1D1C1A` and Charcoal `#292723` for dark surfaces and Ember `#A83F30` for light-mode links. The linked specification is the source of truth for all exact tokens, theme mappings, type sizes, and usage rules. Interface font approval does not identify or replace the generated logo's wordmark font.

The owner approved the UI icon family on November 17, 2026: Lucide outline icons on a 24 × 24-unit canvas, with 2-unit strokes and round caps/joins. Use the pinned editable SVGs and mappings in ICONS.md. Selected navigation retains the outline glyph and uses Ink on a Coral indicator with a visible label. This approval covers UI icons, not launcher artwork or a replacement for the Taste Ticket logo.

## Selected design

The symbol is a wide horizontal cinema ticket with subtly rounded, nearly square outer corners, two semicircular inward side notches, and a triangular negative-space folded-corner detail in the upper right. The ticket conveys cinema; the folded corner suggests a saved personal favorite and individual taste.

The selected wordmark is **Lumio**, with a capital L and lowercase umio, in a substantial editorial serif style. The primary lockup places the ticket to the left of the wordmark. Preserve the wider ticket proportions and serif character of variation C.

Use the third column of the [refinement board](taste-ticket/lumio-taste-ticket-variations.png) as the visual selection reference. Its light/dark treatments and circular/rounded-square launcher examples illustrate the direction; they are not packaged Android resources.

## Asset status and limitations

- Concepts and raster exports were created with the built-in imagegen tool.
- C exports: [dark horizontal lockup](taste-ticket/lumio-c-lockup-dark.png) and [dark standalone symbol](taste-ticket/lumio-c-symbol-dark.png), both PNGs with alpha channels. These are raster artwork, not editable typography or vector masters.
- White transparent lockup generation was attempted twice, but both results showed stray edge artifacts and damaged negative spaces. Neither was accepted as a final asset. A clean white export remains outstanding; use the refinement board to understand the intended reversed treatment.
- The earlier [transparent ticket symbol](taste-ticket/lumio-taste-ticket-symbol-transparent.png) predates selection of C. Keep it as exploration history, not the final C master.
- Raster generation can introduce small geometric differences between exports. Production artwork still needs consistency and actual small-size review.
- No editable vector master has been delivered. A PNG or a PNG embedded in SVG must not be described as an editable vector.
- The illustrated logo wordmark has no confirmed font family or font license. The approved Fraunces / DM Sans interface pairing does not establish the wordmark's font identity. Do not claim a specific wordmark font or reproduce it using an unverified substitute.

## Choices still open

- Component states and layout spacing; logo clear space and minimum-size specifications. Color tokens and the interface type system are approved, but native rendering, localization layouts, and accessibility still need validation.
- Production Android adaptive/themed and iOS launcher assets. The UI icon family is approved; native resource conversion, rendering, localization layouts, and accessibility validation remain outstanding.
- Package identifiers and other platform branding details.
- Name, domain, app-store, and trademark availability have not been checked.

Branding selection does not authorize native app implementation, a development plan, publication, remote creation, or pushing repository changes.

## Decision history

1. The owner selected the app name **Lumio**.
2. After exploring six directions, the owner said: “Let's use taste ticket”.
3. After reviewing Taste Ticket variations A, B, and C, the owner said: “Let's use C”.
4. The owner requested this document so future chats retain these choices.
5. On November 17, 2026, the owner approved the Editorial color and typography specification: “Let's use these colors and fonts. Record them as approved in the branding decisions.”
6. On November 17, 2026, the owner approved the Lucide UI icon family: “Let's use this icon family. Record it as approved in the branding decisions.”

## Export prompt record

The built-in imagegen workflow used the refinement board as the design reference and requested only column C — Editorial: a wide ticket with subtly rounded corners, semicircular inward side notches, and an upper-right triangular transparent fold, paired with a substantial title-case serif **Lumio** wordmark. The dark lockup prompt requested flat near-black ink, transparent negative space, generous clear space, and no backdrop, texture, extra labels, or mockup. A separate prompt requested the left-hand symbol alone on a square transparent canvas. White-on-transparent export prompts request the same C design in solid white for dark backgrounds. These instructions describe intent; they do not establish exact color values, identical geometry across generated files, or font identity.
