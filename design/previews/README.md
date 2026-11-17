# Interactive design previews

Saved November 17, 2026 following owner approval of the Editorial colors, typography, and Lucide outline icon family.

| Preview | Standalone browser file | Editable fragment |
| --- | --- | --- |
| Colors and typography | [Open](lumio-editorial-foundations.html) | [Source](sources/lumio-editorial-foundations.html) |
| UI icon family | [Open](lumio-icon-family.html) | [Source](sources/lumio-icon-family.html) |

Open a standalone file in a modern browser. The source fragments preserve the interactive designs shown in chat, with approval labels updated. Standalone exports include their preview wrapper and fallback state storage; the original Codex conversation is not required. Appearance supports system/light/dark. The icon sheet supports 20/24 sizing and navigation selection. Navigation is a visual state demonstration.

The preview wrapper loads its pinned Lucide runtime from a CDN, and font specimens load Google Fonts. Exact fonts and runtime-rendered icons therefore require network access; system fonts are fallbacks. The approved editable icon masters and licenses are stored locally in [branding/icons/lucide](../branding/icons/lucide/), and may differ from aliases or the revision used by the preview renderer. Do not use a screenshot of this preview as the vector source.

The fragments remain the editable sources. Regenerate standalone exports after source changes using the Visualize skill's `scripts/render.py <absolute-source-path> <absolute-destination-path> --title <title> --force` in an environment with that skill installed. The checked-in browser exports can be opened without that authoring tool.

Specifications remain authoritative: [colors/type](../branding/COLORS-TYPOGRAPHY.md), [icons](../branding/ICONS.md), [approval history](../branding/DECISIONS.md). These browser specimens do not establish native rendering, assistive technology, or localization-layout validation.
