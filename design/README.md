# Lumio design

## Approved identity

- [Branding decisions](branding/DECISIONS.md): Lumio, Taste Ticket C — Editorial, colors, typography, and UI icons, with approval history and outstanding decisions.
- [Colors and typography](branding/COLORS-TYPOGRAPHY.md): exact light/dark tokens, Fraunces / DM Sans, type scale, contrast measurements, and font licenses.
- [UI icon family](branding/ICONS.md): Lucide outline rules, action mappings, accessible labels, and localized starting strings.
- [Editable icon vectors](branding/icons/lucide/): 29 pinned SVGs, source manifest, and upstream license.

## Interactive previews

Open these standalone HTML files in a modern browser:

- [Colors and typography](previews/lumio-editorial-foundations.html): appearance selector, palette, and type specimen.
- [UI icon family](previews/lumio-icon-family.html): appearance and size selectors, plus navigation selection states.

[Preview sources and usage notes](previews/README.md) explain dependencies, limitations, and maintenance. Preview controls demonstrate styling; they do not implement app navigation or save personal movie preferences.

## Local exploratory references

The following references remain in the local workspace and are not included in the approved design-system commit. Their links may be unavailable in a fresh checkout.

- [Taste Ticket refinement board](branding/taste-ticket/lumio-taste-ticket-variations.png): column C is the selected logo direction; see the decisions document for production asset limitations.
- [Editorial screen concepts](screens/editorial-v1/README.md): six raster design proposals with fictional movie content. Earlier colors, fonts, and icons in these images are superseded by the approved specifications.
- [Stitch references](stitch/13911445762657244453/README.md): export metadata and local reference notes. Screen exports remain ignored because they may contain movie/poster imagery.

Saving exploratory material does not approve its design or authorize native app implementation.

## Native Android foundation

Stage 0 applies the approved palette, typography and icons in the native shell. These [device screenshots](../validation/screenshots/stage-0/) document implementation checks, not new approved design decisions. See [validation](../validation/android.md) for scope and limitations.

Stage 1 adds native Explore using the Editorial screen reference and approved brand tokens. Its validation notes are in [Android validation](../validation/android.md). Captures with real catalog/poster imagery are kept locally under ignored `validation/screenshots/stage-1/` and are excluded from Git. This implementation does not approve new branding choices.
