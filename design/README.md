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
- [Trivia design directions](previews/lumio-trivia-directions.html): exploratory Cinema Spotlight, Arcade Rush, and Golden Ticket; playable opening, questions, feedback, pause, and results with automatic progression. These layouts await owner selection.

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


Stages 2–4 add native movie details, Swipe and Your choices using the existing approved tokens and Lucide mappings. History uses `list-checks`; Undo uses `undo-2`. See [Android validation](../validation/android.md) for actual checks. Catalog/poster captures for these stages remain in ignored `validation/screenshots/stage-2/`, `stage-3/` and `stage-4/`; no new branding choices or exploratory assets were approved by this implementation.


Stage 5 adds native Genie using the Editorial tokens and `sparkles` icon, with actual metadata-based recommendation reasons. Light/dark/large-text preview sources live with `GenieScreen.kt`; live catalog captures stay in ignored `validation/screenshots/stage-5/`. This does not approve new branding choices.


Stage 6 adds the native Arcade hub and Trivia question, paused, feedback and results views. They use the existing Editorial palette, Fraunces/DM Sans and Lucide outline icons; correct feedback uses Pine/Sage and incorrect feedback uses Brick/Rose with explicit answer text. Preview sources live with `TriviaScreen.kt`. Captures containing source questions remain in ignored `validation/screenshots/stage-6/`. Poster Puzzle is a forthcoming card, not an implemented game or a new approved design.


Stage 7 activates the native Poster Puzzle card and adds tile selection, reference and completion states using existing Editorial tokens. Preview sources live with `PuzzleScreen.kt`; source-poster captures remain in ignored `validation/screenshots/stage-7/`. The owner clarified that Poster Puzzle must use actual available posters only; the earlier procedural fallback has been removed.


Stage 8 adds native Settings with appearance and game sound choices, bundled font/icon notices and catalog attribution. Light and German dark/large-text preview sources live with `SettingsScreen.kt`. Stages 7 and 8 were approved for commit and push on November 17, 2026; this does not approve exploratory design directions.


## Android discovery and Arcade refinements — approved November 18, 2026

The owner requested native refinements after reviewing the running app. [Implementation notes](../docs/ANDROID-REFINEMENTS.md) describe newest-first Explore, full-poster Swipe, focused Genie, the ticket-inspired Trivia adaptation and draggable 3×3/4×4/6×6 puzzles. The Trivia direction reuses the saved Golden Ticket artwork and question-card idea from the [editable three-direction preview](previews/sources/lumio-trivia-directions.html); the original [standalone browser export](previews/lumio-trivia-directions.html) remains an exploratory comparison, not an exact replica of the native implementation. [Native welcome capture](previews/assets/trivia/android-welcome.png) documents the implemented adaptation; native composables remain the editable implementation source. The owner approved the completed native refinement set on November 18, 2026; the approved identity remains unchanged. See [validation](../validation/android.md) for actual checks and limits.
