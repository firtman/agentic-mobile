# Agent instructions

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
- On 2026-11-18 the owner approved the scoped Android discovery/Arcade refinements (newest-first Explore, full-poster and neutral-action Swipe, single-pick Genie, Golden Ticket-inspired Trivia, and draggable 3×3/4×4/6×6 Puzzle) and requested commit/push to existing origin/main. See docs/ANDROID-REFINEMENTS.md and validation/android.md. Stage 9 and iOS remain unauthorized.
- Node.js 22 or newer is required. The API uses built-in Node.js libraries; no dependency installation is needed.
- Initial setup downloaded and validated 9,919 movies into ignored `data/movies.json`. The catalog can change; check the current file or health response rather than assuming this count.
- Do not assume an API process from an earlier chat turn is still running. Check its health before starting another instance. Keep the server running when asked to start it, and report the verified URL.

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
