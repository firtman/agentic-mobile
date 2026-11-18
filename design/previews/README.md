# Interactive design previews

Saved November 17, 2026 following owner approval of the Editorial colors, typography, and Lucide outline icon family.

| Preview | Standalone browser file | Editable fragment |
| --- | --- | --- |
| Colors and typography | [Open](lumio-editorial-foundations.html) | [Source](sources/lumio-editorial-foundations.html) |
| UI icon family | [Open](lumio-icon-family.html) | [Source](sources/lumio-icon-family.html) |
| Trivia directions (exploratory) | [Open](lumio-trivia-directions.html) | [Source](sources/lumio-trivia-directions.html) |

Open a standalone file in a modern browser. The source fragments preserve the interactive designs shown in chat, with approval labels updated. Standalone exports include their preview wrapper and fallback state storage; the original Codex conversation is not required. Appearance supports system/light/dark. The icon sheet supports 20/24 sizing and navigation selection. Navigation is a visual state demonstration.

The preview wrapper loads its pinned Lucide runtime from a CDN, and font specimens load Google Fonts. Exact fonts and runtime-rendered icons therefore require network access; system fonts are fallbacks. The approved editable icon masters and licenses are stored locally in [branding/icons/lucide](../branding/icons/lucide/), and may differ from aliases or the revision used by the preview renderer. Do not use a screenshot of this preview as the vector source.

The fragments remain the editable sources. Regenerate standalone exports after source changes using the Visualize skill's `scripts/render.py <absolute-source-path> <absolute-destination-path> --title <title> --force` in an environment with that skill installed. The checked-in browser exports can be opened without that authoring tool.

Specifications remain authoritative: [colors/type](../branding/COLORS-TYPOGRAPHY.md), [icons](../branding/ICONS.md), [approval history](../branding/DECISIONS.md). These browser specimens do not establish native rendering, assistive technology, or localization-layout validation.

## Trivia directions — exploratory, November 17, 2026

Compare Cinema Spotlight (dark cinematic stage and vertical answers), Arcade Rush (tactile two-column answer tiles), and Golden Ticket (editorial ticket question card). Use the preview's named picker to switch designs. Press Let's play to enter a five-question round, then tap an answer. The reveal lasts 1.2 seconds before advancing automatically, including to results after question five. Hold reveal suspends advancement; Continue resumes it. Pause hides the question and suspends both clocks. Switching designs or hiding the document suspends that design's clocks. Each design maintains its own round in memory. Back to Arcade returns to the opening specimen; no Arcade hub navigation is implemented in this preview.

The 20-second countdown, one-use 50:50, score, and streak bonus match the existing round rules (100 points plus 25 per consecutive correct answer, capped at 100 extra). The shorter reveal is a proposal. The optional host design controls adjust appearance and reveal duration. Fictional film titles and deliberately simple title-matching prompts are UI specimens, not catalog content or a proposed question set. No downloaded movie/trivia data is embedded. No native application code is changed by this design exploration; no layout has been approved. English preview only; native localization remains a later validation step.

The built-in imagegen tool generated the three original illustrations. [Artwork and exact generation prompts](assets/trivia/README.md) are saved locally. The editable fragment embeds JPEG copies so it works in conversation; the standalone browser export contains the same artwork. Original generated PNGs remain in the generator's output directory. Font and icon dependencies follow the notes above.

Validation: JavaScript syntax check passed. Headless Chrome exercised all three designs through five correct answers, automatic progression, pause/resume, 50:50, and the expected 750-point final score; no page runtime errors were reported. A 320px viewport check found no horizontal overflow. Spotlight opening and gameplay were visually inspected. These are browser preview checks, not native Android or assistive-technology validation.

Review corrections: moved 50:50 into the question header alongside the question count in all three designs. Added an early UTF-8 declaration so opening the editable fragment directly in a browser preserves curly quotes, apostrophes, and accented text. Chrome checks at 736px and 320px confirmed UTF-8 decoding, correctly rendered question quotes, the lifeline above the answers, functional elimination and automatic advancement, and no horizontal overflow.


## Native adaptation — approved November 18, 2026

The scoped Android refinements adapt Golden Ticket with an explicit welcome screen, notched question panel and animated answer/reveal feedback. [Native welcome capture](assets/trivia/android-welcome.png) shows the implemented screen; it contains original generated decorative artwork and no supplied trivia/catalog data. Its editable implementation is `android/app/src/main/java/dev/master/lumio/ui/trivia/TriviaScreen.kt`. The three-direction HTML comparison and its editable source above remain exploratory and are not an exact native prototype. See [refinement notes](../../docs/ANDROID-REFINEMENTS.md) for behavior differences and [validation](../../validation/android.md) for actual checks. The owner approved the completed native adaptation for commit and push on November 18, 2026. The alternative browser directions remain exploratory.
