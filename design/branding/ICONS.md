# Lumio Editorial — approved UI icon family

Approved by the project owner on November 17, 2026, as recorded in [DECISIONS.md](DECISIONS.md). Continues the approved Editorial colors and Fraunces / DM Sans typography. This specification defines the selected icon family, 29-vector set, mappings, sizing, and selection rules. This defines design assets and behavior, not native app implementation.

## Family

Use **Lucide outline icons** consistently across the app. Their simple geometry and restrained detail complement the substantial serif headings without competing with posters or the Taste Ticket logo. Keep the brand ticket as the logo; do not turn every control into a ticket or add folded corners to utility icons.

- Master canvas: 24 × 24 units, 2-unit centered strokes, round line caps and joins, no fill. Preserve the supplied vectors' geometry and padding.
- Standard display size: 24 dp on Android / 24 pt on iOS. Supporting inline icons may use 20; use 24 for navigation and preference actions. Scale uniformly, including the stroke. Avoid 16 for essential actions.
- Aim for equal apparent size and visual weight, not equal path bounds. Review dense icons such as the gamepad alongside simpler ones at actual size. Do not stretch, crop, or independently thicken individual icons.
- Use one family for product icons on both platforms. Keep system-owned permission dialogs and native system controls platform-native. No mixing emoji, SF Symbols, Material icons, and Lucide within the product icon family.
- Keep 48 × 48 dp Android / at least 44 × 44 pt iOS interactive targets, even when the visible glyph is 24. Icons and their labels should scale and reflow with accessibility settings.

## Core mapping and labels

Names below are the exact downloaded SVG filenames without `.svg`. English, Spanish, German, and Portuguese labels are aligned starting strings for localization review, not tested native layouts.

| Purpose / source icon | English | Spanish | German | Portuguese |
| --- | --- | --- | --- | --- |
| Explore — `compass` | Explore | Explorar | Entdecken | Explorar |
| Swipe — `layers-2` | Swipe | Deslizar | Wischen | Deslizar |
| Recommendations — `sparkles` | Genie | Genie | Genie | Genie |
| Arcade — `gamepad-2` | Arcade | Arcade | Arcade | Arcade |
| Settings — `settings-2` | Settings | Ajustes | Einstellungen | Configurações |
| Search — `search` | Search | Buscar | Suchen | Buscar |
| Filter — `sliders-horizontal` | Filter | Filtrar | Filtern | Filtrar |
| Like — `thumbs-up` | Like | Me gusta | Gefällt mir | Gostei |
| Dislike — `thumbs-down` | Dislike | No me gusta | Gefällt mir nicht | Não gostei |
| Unseen — `eye-off` | Not seen yet | Aún no la vi | Noch nicht gesehen | Ainda não vi |
| Undo — `undo-2` | Undo | Deshacer | Rückgängig | Desfazer |
| Preference history — `list-checks` | Your choices | Tus elecciones | Deine Auswahl | Suas escolhas |
| Back — `arrow-left` | Back | Volver | Zurück | Voltar |
| Sound enabled — `volume-2` | Sound on | Sonido activado | Ton an | Som ativado |
| Sound disabled — `volume-x` | Sound off | Sonido desactivado | Ton aus | Som desativado |
| Pause — `pause` | Pause | Pausar | Pausieren | Pausar |
| Trivia — `circle-question-mark` | Trivia | Trivia | Filmquiz | Trivia |
| Poster Puzzle — `puzzle` | Poster Puzzle | Puzle de póster | Poster-Puzzle | Quebra-cabeça de pôster |

## Supporting mapping

| Purpose / source icon | English | Spanish | German | Portuguese |
| --- | --- | --- | --- | --- |
| Sort — `arrow-up-down` | Sort | Ordenar | Sortieren | Ordenar |
| Dismiss — `x` | Close | Cerrar | Schließen | Fechar |
| Source link — `external-link` | View source | Ver fuente | Quelle ansehen | Ver fonte |
| Resume — `play` | Resume | Reanudar | Fortsetzen | Retomar |
| Game replay — `rotate-ccw` | Play again | Jugar de nuevo | Erneut spielen | Jogar novamente |
| Light appearance — `sun` | Light | Claro | Hell | Claro |
| Dark appearance — `moon` | Dark | Oscuro | Dunkel | Escuro |
| System appearance — `monitor` | System | Sistema | System | Sistema |
| Offline — `wifi-off` | Offline | Sin conexión | Offline | Sem conexão |
| Loading — `loader-circle` | Loading | Cargando | Wird geladen | Carregando |
| Selected / correct — `check` | Selected | Seleccionado | Ausgewählt | Selecionado |

There are 29 unique vectors. Additional wording follows the actual action: for example, the check icon uses “Correct answer” in trivia instead of “Selected”. Use text `50:50` for the lifeline; a decorative icon would obscure its specific effect. Keep movie source ratings as labeled data rather than reusing Like as a rating symbol.

## Color and states

- Default navigation uses secondary text: Stone `#69635C` on Paper and Pale stone `#BDB4A8` on Night/Charcoal.
- Selected navigation uses the same outline glyph and weight, Ink `#242320` on a Coral `#F47B65` indicator behind the icon, and a stronger DM Sans label. Expose the selected state semantically. Do not switch to filled silhouettes or rely only on recoloring.
- Selected navigation indicator: 48 × 32 dp/pt rounded rectangle, radius 8, with the icon centered; label outside below it. This approved icon selection treatment does not define the rest of the app's spacing system.
- Standard toolbar actions use primary text. Standalone active icons may use Ember `#A83F30` in light mode or Coral in dark mode. Coral directly on Paper has insufficient contrast for an essential icon; use Ink on a Coral indicator instead.
- Like feedback uses Pine/Sage, Dislike feedback Brick/Rose, and Unseen secondary text. Retain the visible label and selected check or explicit state text. These are personal choices; a dislike must not display system-error copy.
- Focus uses the approved link/focus token, with separation from the control. Pressed and disabled component styling still needs review; never remove essential meaning by lowering an enabled icon's opacity.
- Place controls on opaque approved surfaces over posters. Do not add shadows or circles to every glyph to compensate for imagery.

## Ambiguity and interaction

- **Swipe:** stacked cards indicate the discovery deck, not a gesture direction. Keep the Swipe label and explain right = Like, left = Dislike, down = Not seen yet. Show equivalent labeled action buttons.
- **Genie:** sparkles mean personal recommendations. Keep Genie visible and use a descriptive accessible name such as “Genie recommendations”, localized in each language. The icon does not promise an AI chat feature.
- **Unseen:** the slashed eye may suggest hiding. Always label it Not seen yet, and explain that it records the choice and removes the movie from discovery and recommendations. Do not reuse it for spoilers or password visibility in this app.
- **History versus undo:** use the checklist for the collection of saved choices and the curved arrow only for Undo. Undo restores the previous choice or removes a first rating; it is not game replay.
- **Sound:** the glyph expresses current state. On a toggle, expose a localized Game sound label and checked state; on an action button, name the next action, such as Mute game sound or Enable game sound.
- **Pause/resume:** swap Pause and Play based on actual state and update the accessible action. Trivia and Poster Puzzle remain visibly named at the Arcade hub.
- **Source:** the external-link glyph accompanies View source, not an unlabeled rating. The question-mark icon identifies Trivia; it should not also represent a global Help destination.
- Hide decorative icons from assistive technology when visible text supplies the name. Icon-only toolbar buttons require localized action names. Never expose source filenames as labels.
- Preserve tab and focus order, and expose selection without duplicate announcements. Back arrows should follow platform directionality if future RTL support is added; do not mirror all icons. The four current UI languages are LTR.
- Loading must have a text equivalent; honor reduced motion. Offline indicates connectivity while cached browsing can remain available, not that all functionality is blocked.

## Assets, source, and validation

Individual editable SVGs are in [icons/lucide](icons/lucide/), with `currentColor` strokes and no raster images. Source repository: [Lucide](https://github.com/lucide-icons/lucide), pinned to commit `149eba0aea48178a5ab1efe2c701de1da94f4be8`. Exact URLs and SHA-256 hashes are in [manifest.json](icons/lucide/manifest.json).

The [upstream license](https://lucide.dev/license) and local [LICENSE](icons/lucide/LICENSE) include ISC terms and MIT notices for Feather-derived icons. Preserve all supplied notices with distributions. Geometry follows the [Lucide design language](https://lucide.dev/contribute/icons/design-principles).

Validation performed: all 29 files parsed as SVG; each has a 24 × 24 viewBox, a 2-unit stroke, and round caps/joins. The review sheet uses the host's Lucide renderer, which may use aliases or a different library revision; the pinned SVGs are the approved asset masters. Actual native rendering, visual weight at 20/24, touch targets, localization layout, assistive technology, and interactive states have not been validated in apps. SVGs have not been converted into Android or iOS resources.

This approval covers UI icons. It does not replace the Taste Ticket logo or define adaptive, themed, or iOS launcher artwork. Label translations remain subject to localization review; approval does not establish that native layouts or accessibility have been validated.
