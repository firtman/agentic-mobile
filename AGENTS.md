# Agent instructions

- **Lumio 1.0 is closed for Android and iOS (2026-11-18).** [docs/SPEC-1.0.md](docs/SPEC-1.0.md) is the source of truth for product behavior. It supersedes the UX brief, stage plans and the behavior notes below where they differ. Any change to a MUST rule needs owner approval and an update to the spec in the same commit.
- Read README.md for UX context and [design/branding/DECISIONS.md](design/branding/DECISIONS.md) for approved branding. Do not create a development plan or start app implementation until the owner authorizes it. The owner selected Lumio, Taste Ticket, variation C — Editorial, the Editorial light/dark palette, Fraunces / DM Sans interface typography, and the Lucide outline UI icon family on 2026-11-17; availability and trademarks have not been checked. The Android namespace/applicationId is `dev.master.lumio`; use the approved C raster as an interim launcher. Choose production launcher artwork and remaining platform branding with the owner later.
- Preserve the initial-state branch unchanged. Use main for ongoing development and future commits. Never force-push or move the baseline branch.
- The repository contains a UX brief and a local API. Android and iOS are independent native implementations using their respective SDKs. Follow the owner’s implementation plan.
- Do not commit movie/trivia data, posters, downloaded archives, generated Android catalog assets, secrets, or machine configuration.
- Fetch local data with npm --prefix api run setup; run the server with npm --prefix api start.
- Preserve local ratings and undo. LIKE/DISLIKE influence scoring; UNSEEN is saved and excluded from the swipe queue and recommendations. Undo restores the previous value or removes a first rating.
- Recommendation weights are genre 1, director 3, actor 2, with equal negative weights for dislikes.
- Keep English, Spanish, German, and Portuguese UI strings aligned.
- Android catalog access is online-only, per the owner’s implementation plan on 2026-11-17. Do not bundle or persist catalog data. Keep ratings, undo, movie snapshots needed for history/scoring, and settings on device; show an error with Retry when catalog access fails. This supersedes the UX brief’s offline requirement for Android.
- Use supplied poster URLs and procedural fallbacks. Never fabricate artwork URLs or ratings. Preserve downloaded-source attribution. Poster Puzzle is an exception: use only successfully loaded supplied posters; skip missing/invalid URLs, never substitute a generated pattern, and block play when artwork fails. The owner clarified this on 2026-11-17.
- Use the Gradle wrapper. Keep android/local.properties ignored. Record actual validation results and limitations.
- After completing work and receiving the owner's approval, commit the approved work and push it to the existing `origin` on `main` without requesting approval again. The owner authorized this workflow on 2026-11-17. Include relevant documentation, design assets, and previews; do not include unrelated or unapproved changes. Never force-push or move the baseline branch. Creating a new remote or repository still requires an explicit request. If validation, authentication, or push fails, report the actual result and resolve it within the authorized scope.
- Save interactive design previews in `design/previews/` with their editable sources, standalone browser exports, and usage notes. Keep [design/README.md](design/README.md) updated so project design work is discoverable in one place. Distinguish approved decisions from exploratory references.

## Workspace context

- This workspace was cloned directly from https://github.com/firtman/agentic-mobile into `/Users/firt/Dev/Master.dev/MovieApp`. The existing `origin` points to that repository.
- Repository preparation and running the local API have been authorized. Approved branding selections are recorded in design/branding/DECISIONS.md, with palette/type specifications in COLORS-TYPOGRAPHY.md and UI icon specifications in ICONS.md beside it. The owner approved Android stage 0 and authorized stage 1 of `docs/ANDROID-PLAN.md` on 2026-11-17: Kotlin/Compose, SDK 26/36/36, online-only catalog, and one stage at a time. Stop after each stage for owner approval. iOS implementation and remaining branding decisions await direction. Approval of completed work authorizes committing and pushing that work under the rule above.
- The owner approved Android stage 1 for commit and push on 2026-11-17 (pushed as `7b47f94`) and authorized stage 2 (Movie details and Room ratings) the same day. The owner authorized stage 3 (Swipe) on 2026-11-17. The owner approved stages 2 and 3 for commit/push (pushed together as `9e468d4`) and authorized stage 4 (Your choices) on 2026-11-17. The owner approved stage 4 for commit/push (pushed as `d47edb4`) and authorized stage 5 (Genie) on 2026-11-17. The owner approved stage 5 for commit/push (pushed as `864a7c5`) and authorized stage 6 (Arcade hub and Trivia) on 2026-11-17. The owner selected 5 Trivia questions per round and 20 seconds per question. The owner approved stage 6 for commit/push (pushed as `4a937a7`) and authorized stage 7 (Poster Puzzle) on 2026-11-17. The owner approved stages 7 (Poster Puzzle) and 8 (Settings) for commit and push on 2026-11-17. The owner subsequently instructed not to do stage 9; do not start stage 9 without new direction.
- Stages 7 and 8 were committed and pushed as `b867c9a`. On 2026-11-18 the owner approved the scoped Android discovery/Arcade refinements (newest-first Explore, full-poster and neutral-action Swipe, single-pick Genie, Golden Ticket-inspired Trivia, and draggable 3×3/4×4/6×6 Puzzle); they were committed and pushed to existing origin/main as `1c0f47c`. See docs/ANDROID-REFINEMENTS.md and validation/android.md. Stage 9 and iOS remain unauthorized.
- Node.js 22 or newer is required. The API uses built-in Node.js libraries; no dependency installation is needed.
- Initial setup downloaded and validated 9,919 movies into ignored `data/movies.json`. The catalog can change; check the current file or health response rather than assuming this count.
- Do not assume an API process from an earlier chat turn is still running. Check its health before starting another instance. Keep the server running when asked to start it, and report the verified URL.

## Current Android behavior to preserve

These decisions consolidate the project chats reviewed on 2026-11-18. Use [android/README.md](android/README.md) for architecture/run instructions and [docs/ANDROID-REFINEMENTS.md](docs/ANDROID-REFINEMENTS.md) for the approved refinement details.

- **Explore:** newest year first (`year desc`) is the default, including Reset filters and invalid/missing saved sort. A valid explicit saved sort wins. Keep search, genre filters, spotlight and 24-item paging.
- **Swipe:** fit the entire supplied poster inside its bounded neutral frame. Like, Dislike and Unseen have equal neutral outlined styling; the former permanently filled Like button was a visual problem, not a stuck rating. Preserve right/left/down gestures, equivalent buttons, saved-film exclusion and global Undo.
- **Your choices:** render and edit saved snapshots locally, newest choice first. Per-film Undo affects only that film; global Swipe Undo targets the latest remaining change. Preserve the shared 200-entry undo limit and existing ratings when updating/installing builds.
- **Genie:** show one recommendation at a time, with Next, a finite position count and explicit restart at the end. Next does not rate or dismiss a film. Preserve weighted scoring, exclusion of every saved film including UNSEEN, positive-net-score candidates and up to three actual positive matching reasons. No likes/dislikes means getting-started guidance, even if unseen choices exist. Do not turn the experience back into a long scrolling list.
- **Trivia:** use the approved native Golden Ticket-inspired adaptation, with an illustrated introduction and explicit start, ticket question panel, animations and encouraging feedback. Keep five questions, 20 seconds each, one 50:50 per round beside the question counter, and automatic progression after four seconds of answer feedback. Keep reading holds feedback; manual Next, pause/resume, results and replay remain available. Pause hides the question. Correct answers earn 100 points plus consecutive-answer bonuses of 25, capped at 100 extra; wrong answers/timeouts reset the streak. A perfect round is 750 points.
- **Poster Puzzle:** offer 3×3, 4×4 and 6×6 boards. Dragging one tile onto another swaps exactly two pieces and counts one move; cancelled/outside/same-position drops do nothing. Preserve the two-tap alternative. Changing difficulty reshuffles the same poster and resets progress, moves, selection and reference; Next retains difficulty within the live process. Play/reference require a successfully loaded supplied poster; failures offer Retry/Next and must never become generated artwork. Puzzle actions do not change movie ratings.
- **Settings:** persist System/Light/Dark appearance and game sound in Preferences DataStore (defaults System and sound on). Apply changes without resetting navigation/game state. Keep bundled font/icon licences, source attribution and links accessible without catalog access. Sound covers Trivia feedback and Puzzle swaps/completion, with no replay after returning to a game.
- Catalog and game content stay in process memory only. Active games survive navigation/configuration changes within the live process, but intentionally reset on process death. Ratings, undo, rated-movie snapshots and settings persist locally. Do not broaden persistence to catalog/questions/posters.

## Design references and approval boundaries

- Exact approved tokens and typography come from [design/branding/COLORS-TYPOGRAPHY.md](design/branding/COLORS-TYPOGRAPHY.md); icon vectors/mappings come from [design/branding/ICONS.md](design/branding/ICONS.md). Image mockups do not override those specifications.
- [design/screens/editorial-v1/](design/screens/editorial-v1/README.md) contains six imagegen screen concepts; [design/stitch/13911445762657244453/](design/stitch/13911445762657244453/README.md) contains Stitch reference notes/metadata. These are exploratory references, not additional approved branding or implementation scope. Stitch screen exports remain ignored.
- [design/previews/](design/previews/README.md) holds editable sources and standalone exports for foundations, icons and the three Trivia directions: Cinema Spotlight, Arcade Rush and Golden Ticket. The owner requested a more game-like complete Trivia flow, automatic progression, 50:50 close to the top/question counter, and corrected UTF-8 accents/quotes. Preserve those corrections.
- The approved native Golden Ticket adaptation does not approve every browser-preview detail or either alternative direction. Browser demos use a 1.2-second reveal; native Trivia retains its four-second feedback rule. The generated ticket illustration is decorative artwork, not a catalog poster or approved production launcher. Prompts/provenance are in [design/previews/assets/trivia/README.md](design/previews/assets/trivia/README.md).
- Production vector logo, clean white export, final launcher/platform artwork and remaining brand-system work still need owner direction. Do not describe raster exports as editable vectors or treat the earlier pre-C ticket symbol as the final C master.

## Validation and handoff context

- Latest recorded Android refinement validation: debug build successful, **98 JVM tests passing**, lint **0 errors / 39 warnings**, and **201 aligned UI string keys/placeholders** across all four languages. These are historical results, not checks performed by a future chat. Read [validation/android.md](validation/android.md) and record fresh results for new work.
- Refinement device checks covered newest-first Explore, full-poster/neutral-action Swipe, Genie Next, Trivia start/50:50/automatic advance, Puzzle drag swaps and all three sizes, plus a compact German dark-mode/150% text check. No refinement physical-phone install, Room instrumentation rerun, complete TalkBack/accessibility pass, wide-screen audit or complete UI automation suite was performed. Sound quality still needs a listening check. These gaps do not authorize Stage 9.
- Use the Gradle wrapper with the established JDK 21/SDK 36 setup and pinned dependencies. A past JDK 25 configuration failed. Duplicate generated `* 2.dex`, `* 2.xml` and font intermediates were resolved by clean/fresh temporary build output; do not add source workarounds or commit temporary build scripts/machine paths for those artifacts.
- The owner has used a connected Pixel 3a. For physical-phone debug builds, follow `android/README.md`: device-scoped ADB reverse on port 3000, `-Plumio.apiBaseUrl=http://127.0.0.1:3000/`, and `adb install -r` to preserve personal data. Recheck device availability and forwarding; do not assume a previous emulator, phone connection or API process is alive.
- At this review, local `main` contains owner-authored commit `9b04d34` (`some iOS changes`) after `1c0f47c`, adding design/reference assets and slide/README changes. Despite its title, it adds no iOS app directory and does not establish iOS implementation authorization. Preserve this work; inspect current Git state before staging or pushing and keep unrelated changes outside future scoped commits.
- Some documentation contains chronological statements such as “uncommitted” or “forthcoming.” Read later approval/delivery entries before treating those earlier snapshots as current status. Android stages 0–8 and the refinement set are delivered; Stage 9 remains explicitly stopped by the owner. No new app work is authorized by this documentation refresh.

## Local API workflow

Run commands from the repository root:

```sh
# Only needed when the local catalog is missing or a refresh is requested
npm --prefix api run setup

# Start the API and keep its process running
npm --prefix api start

# Verify startup
curl --fail http://127.0.0.1:3000/health
```

- The default address is `http://127.0.0.1:3000`. The root `/` returns 404; use the routes below.
- In this Codex environment, listening on a local port and accessing it may require execution outside the sandbox. If permissions block startup or a health check, use the tool's approval mechanism. A sandbox connection failure alone does not establish that the server is down.
- Setup downloads the catalog from `https://movie-api-delta-six.vercel.app/catalog`. Keep `data/movies.json` ignored and preserve the attribution described in `data/SOURCE-ATTRIBUTION.md`.
- For Android emulator development, the host catalog URL is `http://10.0.2.2:3000/catalog`; allow cleartext HTTP in debug builds only. Release connections require HTTPS.

Useful test routes:

| Route | Purpose |
| --- | --- |
| `/health` | Status and current movie count |
| `/movies?limit=10` | First ten movies |
| `/movies?q=batman&limit=5` | Search example |
| `/movies?genre=Action&limit=10` | Genre filter |
| `/movies?year=2020&limit=10` | Release-year filter |
| `/movies?minRating=8&sort=rating&order=desc&limit=10` | Highest-rated movies with a minimum rating |
| `/movies/{id}` | One movie using an ID from catalog results |
| `/genres` | Available genres and language codes |
| `/catalog` | Full normalized movie and trivia catalog |

The API is read-only and supports GET and HEAD. It does not store personal preferences. Record actual validation results; do not describe a service as healthy without checking it.
