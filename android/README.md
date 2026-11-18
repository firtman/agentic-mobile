# Lumio Android

Stages 0–8 implementation following the approved foundation for the [owner's plan](../docs/ANDROID-PLAN.md). Native Kotlin, Compose and Material 3; namespace/applicationId `dev.master.lumio`; min/compile/target SDK 26/36/36. Explore now provides search by title/director/cast, genre filters, six sort orders, spotlight, and a 24-item paged poster grid. Stage 2 adds movie details and on-device ratings: grid cards and the spotlight open details, where Like / Dislike / Not seen yet are saved in Room with Undo. Swipe, Your choices, Genie, Arcade/Trivia, Poster Puzzle and Settings are implemented.

## Build and run

Use JDK 17 or newer (validated with JDK 21), Android SDK platform 36 and Build Tools 35.0.0. Set `ANDROID_HOME` or create ignored `local.properties` with `sdk.dir` for your SDK. No machine paths belong in version control.

From the repository root, start the local API if its health check fails:

```sh
curl --fail http://127.0.0.1:3000/health
npm --prefix api start
```

In another terminal:

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Room instrumented test on a running emulator
ANDROID_SERIAL=emulator-5600 ./gradlew :app:connectedDebugAndroidTest
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android emulator. Debug requests use `http://10.0.2.2:3000/`; the emulator host and loopback addresses are allowed cleartext access by the debug-only network security configuration. Release uses `https://movie-api-delta-six.vercel.app/` with cleartext disabled. `./gradlew :app:assembleRelease` produces an unsigned release APK; production signing is not configured.

## Data and architecture

`LumioApplication` owns a small manual container. `CatalogApi` models health, paged movies, details, genres and catalog responses. `CatalogRepository` lazily loads the full catalog once into process memory with concurrent requests serialized; failed loads can be retried. Its large response uses a dedicated stream decoder before Retrofit's normal converter. No HTTP disk cache, catalog asset, catalog database or catalog file is used.

Ratings, the undo log, and rated-movie snapshots are stored on the device in Room (`lumio.db`, excluded from backup and device transfer); the exported schema lives in `app/schemas/` and must be committed with schema changes. Appearance and game sound preferences are stored locally in Preferences DataStore. Catalog text is not translated. Missing metadata remains empty/zero in models for screens to hide; poster/source URLs are preserved verbatim. Source provenance remains documented in [SOURCE-ATTRIBUTION.md](../data/SOURCE-ATTRIBUTION.md).

Navigation Compose uses serializable routes and saved destination state. The adaptive navigation suite selects a bottom bar or rail; Settings opens full screen and Back restores the previous destination. Theme follows system appearance in stage 0. Bundled typefaces and 29 converted Lucide icons follow the approved specifications; the launcher uses the approved raster symbol as an **interim** asset. See [asset provenance](ASSETS.md).

## Dependency pins

Dependencies are pinned in `gradle/libs.versions.toml`. SDK 36 is an owner requirement; the project therefore uses AGP 8.13.2 / Gradle 8.13 and a compatible Compose BOM 2025.12.00 / Kotlin 2.2.21 set rather than the SDK 37 template. Newer releases reported by lint are recorded, not applied automatically. The current Android CLI template was inspected, and official guidance was consulted at scaffold time:

- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [Compose compiler configuration](https://developer.android.com/develop/ui/compose/compiler)
- [Navigation releases](https://developer.android.com/jetpack/androidx/releases/navigation)
- [Retrofit 3.0.0 source](https://github.com/square/retrofit/tree/3.0.0)
- [Kotlin serialization 1.9.0](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0)

Review actual build, lint and emulator results in [validation/android.md](../validation/android.md). Stage 0 was approved and pushed as `9ddf76d`. Stage 1 was approved for commit and push on 2026-11-17. Stage 1 was pushed as `7b47f94`; stages 2 and 3 were approved and pushed together as `9e468d4` on 2026-11-17. Stage 4 was approved and pushed as `d47edb4` on 2026-11-17. Stage 5 (Genie) was approved and pushed as `864a7c5` on 2026-11-17. Stage 6 (Arcade hub and Trivia) was approved and pushed as `4a937a7` on 2026-11-17. The owner approved stages 7 (Poster Puzzle) and 8 (Settings) for commit and push on 2026-11-17. Stage 9 remains unstarted at the owner’s direction.

## Explore behavior

Search debounces for 300 ms. A new query, genre or sort cancels the prior page request and resets pagination; stale responses cannot replace newer filters. Genre loading is independent, so its failure does not block movies. Refresh retries movie loading and failed genres. Load more requests the next 24 records using the response offset and count; paging failures preserve existing cards and provide Retry. The spotlight is the first film in the current result order, not a personalized recommendation. The grid repeats that film as part of the complete result set.

Queries, genre and sort survive recreation through SavedStateHandle; fetched catalog pages remain in memory only. Coil 3.3.0 uses constrained image sizing and a shared loader with disk cache disabled. Supplied poster URLs remain unchanged; unavailable artwork uses a neutral id-derived fallback. Per-film source/dataset buttons open original supplied URLs, and a scrollable attribution dialog credits the datasets and Wikipedia terms. Movie cards open online details; source/dataset links are available there.

[Coil Compose](https://coil-kt.github.io/coil/compose/) and [image-loader guidance](https://coil-kt.github.io/coil/image_loaders/) informed this integration; version 3.3.0 is pinned to fit the existing Kotlin/SDK 36 build. All API methods remain read-only GETs; standard OkHttp recovery is enabled for stale pooled connections. Stage 1 device captures include catalog/poster imagery, so they are stored locally under ignored `validation/screenshots/stage-1/` and must not be committed.

## Connected physical phone

With an authorized phone connected through USB or wireless ADB, forward its loopback port to the host API and build with the debug-only URL override:

```sh
adb -s <phone-serial> reverse tcp:3000 tcp:3000
cd android
./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/
adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <phone-serial> shell am start -n dev.master.lumio/.MainActivity
```

Keep the API and ADB connection running while using the local catalog. Reapply port forwarding after reconnecting if needed. Omitting `lumio.apiBaseUrl` rebuilds for the emulator’s `10.0.2.2` host. The override applies only to debug builds; release retains its fixed HTTPS endpoint. Loopback HTTP is permitted only by debug resources.


## Swipe behavior

Swipe requests 24-film pages from `/movies?sort=popularity&order=desc`, keeping unread records in memory only. Room supplies all rated ids, including Not seen yet, before any card appears. Fully-rated pages are skipped automatically. Right, left and down gestures match Like, Dislike and Not seen yet buttons; only a dominant-axis drag of at least 72 dp commits, while short, upward and tied diagonal drags do nothing. Saves advance only after a successful transaction. The scrollable screen also exposes accessibility actions on the card and ordinary labeled buttons.

Undo uses the latest persisted change across the app, survives process restart and reloads the queue online. It removes a first rating or restores a previous rating; a restored prior rating still excludes that movie. Details ratings update Swipe reactively, including returning a consumed film after details Undo. Network errors require Retry, local rating-read errors block the queue until Retry succeeds, and exhausted queues can refresh for new films. Undo has no timed expiry, subject to the ratings store’s 200-entry limit. With Android animator duration scale set to zero, drag transforms are disabled; no timed card transitions or fling animations are used. Your choices history is available from Swipe in stage 4.


## Your choices behavior

The history route opens from Swipe’s Your choices action. A single observable Room query returns saved personal-choice snapshots and each film’s latest undo id, sorted by saved time descending with movie id as a stable tie-breaker. It never downloads the catalog to render or edit history. Thumbnail requests use the supplied saved URL with the same memory-only image loader and fallback; opening full movie details requests `/movies/{id}` online.

Each row offers a choice editor and per-film Undo. Saving an unchanged value is a no-op. The per-film Undo transaction rejects stale entry ids, restores the previous choice/timestamp or removes a first rating, and leaves other films’ undo entries untouched. Global Undo on Swipe continues to target the latest remaining change across all films. When a film has no retained undo step (the shared limit is 200), its Undo button is disabled. Changes update history and Swipe reactively. No schema migration or dependency change was needed for the new queries.


## Genie behavior

Genie loads `/catalog` lazily through the existing mutex-protected `CatalogRepository`; one successful catalog is shared in process memory, never written to disk. A fresh process needs a connection. With no like/dislike choices (including when all saved films are unseen), Genie shows getting-started guidance without downloading the catalog.

The pure Kotlin engine builds feature affinities from saved rating snapshots: each LIKE adds genre 1, director 3 and actor 2; DISLIKE subtracts equal amounts. Features are trimmed, case-normalized with `Locale.ROOT` and counted once per film. A candidate’s score is the sum of its matching affinities. All saved ids, including UNSEEN, are excluded; unseen contributes no affinity. Only positive net scores are recommended. The top 30 are ordered by score, then supplied popularity, normalized title and id. Missing metadata is ignored rather than guessed.

Cards show up to three strongest positive matching features with supplied names. The score is internal and is not presented as a source rating or confidence percentage. Negative matches still lower the net score even though the displayed reasons describe positive connections. The introductory text explains the effect of dislikes. Catalog titles, genres and names remain in their source language; UI templates are localized.

Changes from details, Swipe or history trigger fresh scoring on `Dispatchers.Default`; newer choices and read failures cancel older work before it can publish stale recommendations. Distinct getting-started, no-positive-match, loading, network-error and local-read-error states provide appropriate guidance/Retry. Details still load online and history edits still use personal snapshots. Director/actor reasons depend on metadata availability; most catalog records lack these fields, so genre reasons dominate. Arcade and Trivia are available in stage 6.


## Arcade and Trivia behavior

The owner selected five questions per round with 20 seconds each. The hub opens Trivia and Poster Puzzle. Trivia validates supplied questions (nonblank prompt/id, four distinct nonblank options, valid answer index), removes duplicate question ids, then samples five using Kotlin Random. Fewer than five valid questions produces an unavailable state. The existing lazy in-memory catalog is shared with Genie; no questions, rounds or scores are persisted or bundled.

A correct answer gives 100 points. Consecutive correct answers add 25 per answer after the first, capped at 100 extra (five perfect answers total 750). A wrong answer or timeout gives zero and resets the streak. One 50:50 per round removes two wrong choices and stays used for the rest of that round. Feedback identifies the correct answer and earned points; it advances after four seconds unless Keep reading is selected. Next / See results allow explicit advance. Play again clears score, streak and lifeline and samples a fresh round (individual questions may recur between rounds).

The controller synchronizes against `SystemClock.elapsedRealtime` before inputs as well as on periodic updates, so a tap after the deadline cannot earn points. Pause and lifecycle visibility changes stop timer polling; returning requires explicit Resume and paused screens hide the question. The ViewModel belongs to the Arcade back-stack entry, retaining an active round when leaving and returning within the live process and through configuration changes. It is intentionally lost on process death because catalog data remains memory-only. The first round waits for a resumed lifecycle before starting. Full-screen Trivia has back navigation to Arcade. Settings/sound remain stage 8; no game sounds were added in stage 6.


## Poster Puzzle behavior

Arcade opens a full-screen 3×3 puzzle only from a titled catalog movie with a valid supplied HTTP(S) poster URL. Duplicate movie ids are removed and Next avoids the immediately previous film when another eligible poster exists. Missing/invalid URLs are excluded. No artwork URL is fabricated or rewritten, and no pattern substitutes for a poster.

Tap one tile to select it, then another to swap them; tapping the selection again cancels. Each swap counts once. Progress is the number of tiles in their original positions; it can decrease when a correctly placed tile is moved. Shuffles are guaranteed unsolved and arbitrary swaps make every permutation solvable. Completion disables tile moves, shows completion feedback, and offers Next puzzle or Back to Arcade. There is no timer. Show reference displays the coherent full artwork and clears selection; Hide reference returns to the unchanged board. Tile controls expose piece/position and selection labels to accessibility services.

A single size-bounded Coil `AsyncImagePainter` loads the supplied poster (900×1350 decode request), shared by all tiles; canvas drawing clips nine regions of the same centered crop. No sliced bitmap copies, poster disk cache or catalog persistence are added. Coil’s [Compose painter guidance](https://coil-kt.github.io/coil/compose/) informed this usage; dependency versions remain pinned. Tiles and reference controls become playable only after the actual poster loads successfully. Artwork failures show Retry and Next puzzle; failed movies are skipped by Next. Retry uses a fresh attempt id so late callbacks from the old request cannot affect it. If no usable poster candidates remain, the unavailable screen offers Retry to recheck them.

Puzzle state belongs to the Arcade back-stack entry, preserving the movie, permutation, selection, reference and swap count through navigation/configuration changes in the live process. It intentionally resets on process death. Catalog failures require Retry; empty catalogs show unavailable guidance. View details opens the existing online route, retaining access to source/dataset links. No local movie ratings are changed by puzzle actions. Settings and sound remain stage 8.

## Settings behavior

Stage 8 adds local Preferences DataStore settings for System / Light / Dark appearance and game sound (defaults: System, sound on). Saved appearance is loaded before the main UI appears. Changes apply across destinations immediately without recreating navigation or game state. Read errors show Retry and disable preference edits; failed writes retain the saved value and allow another attempt. Credits remain available without catalog access.

Trivia emits short correct/incorrect tones, including timeouts; Poster Puzzle emits swap/completion tones. Sound uses Android media volume, is optional if the audio service fails, and releases its player when leaving the resumed lifecycle. Events are never replayed when returning to a game or changing appearance. The switch controls both games; there is no downloaded audio asset.

Settings exposes the complete bundled Fraunces / DM Sans OFL and Lucide ISC/MIT notices, plus a bundled copy of `data/SOURCE-ATTRIBUTION.md` and source links. Movie details retain per-record attribution. Notices preserve their original language. DataStore 1.3.0 was selected from the [official release notes](https://developer.android.com/jetpack/androidx/releases/datastore); the [DataStore guide](https://developer.android.com/topic/libraries/architecture/datastore) documents the preferences API. Catalog data remains online-only and memory-only; DataStore holds only appearance and game sound.


## Owner-requested refinements — approved November 18, 2026

See [refinement notes](../docs/ANDROID-REFINEMENTS.md) and [validation](../validation/android.md). Explore now defaults to newest year first and retains explicit sort selections. Swipe fits the full poster in a bounded neutral frame; all three rating actions have equal neutral styling. Genie presents one scored recommendation at a time with Next, a finite position count and an explicit restart after the last pick. Its scoring, exclusions, reasons and online catalog behavior are unchanged.

Trivia now opens with a Golden Ticket-inspired introduction and explicit start, then uses a ticket question panel, round segments, animated question/answer feedback and encouraging copy. The five-question, 20-second rules, 4-second automatic reveal, pause, hold, lifeline and points remain unchanged. Its decorative illustration is a copy of the original generated preview artwork, not a catalog poster. Poster Puzzle supports direct drag-and-drop swaps and the existing two-tap alternative, with 3×3, 4×4 and 6×6 sizes. Changing size reshuffles the current poster and resets moves/selection/reference; the chosen size carries into Next during the live process. Artwork must successfully load before moves are enabled.
