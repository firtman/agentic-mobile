# Android validation

## Stage 0 — Foundation — 2026-11-17

Status: approved by the owner on 2026-11-17. The owner authorized committing/pushing stage 0 and starting stage 1. The owner’s plan requires approval at each stage boundary.

### Delivered

- Single `:app` Kotlin/Compose module, Gradle wrapper with distribution checksum and version catalog; applicationId/namespace `dev.master.lumio`, SDK 26/36/36.
- Approved Editorial light/dark colors and Fraunces / DM Sans typography, bundled variable fonts and OFL notices, 29 Lucide VectorDrawables with licence and reproducible conversion script.
- Serializable Navigation Compose routes, four placeholder destinations, adaptive bottom bar/rail, global Settings placeholder and return navigation.
- Manual application container, Retrofit/OkHttp client, typed health/movie/page/genres/catalog/trivia models, stream decoder for full catalog, synchronized lazy memory-only catalog repository.
- Localized base strings with matching 37-key sets in English, Spanish, German and Portuguese. Missing movie metadata keeps empty/zero values; no invented data or artwork URLs.
- Interim adaptive launcher uses the approved C raster. `.gitignore` excludes signing keys, local SDK config, build outputs and existing local catalog data.
- AGENTS.md and README reflect the owner’s online-only Android catalog decision. Ratings/undo and settings persistence remain later-stage work.

### Automated checks

Validated on macOS with JDK 21.0.11, SDK platform 36 and Gradle 8.13 / AGP 8.13.2. Dependency choices and official reference links are recorded in [Android setup](../android/README.md).

```sh
cd android
./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --no-build-cache --console=plain
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --console=plain
./gradlew :app:lintDebug --rerun-tasks --console=plain
```

All final commands passed. Debug and unsigned release APKs were generated. All **10 unit tests** passed (zero failures/errors): API query encoding, details ID encoding, unknown fields/missing metadata, HTTP error propagation, catalog decoding, stream closure on success and malformed JSON, concurrent catalog loading/retry, connection state/retry and cancellation.

Final fresh lint: **0 errors, 64 warnings**. Breakdown: 47 resources reserved for subsequent stages; 16 build/dependency update notices; 1 newer-target-SDK notice. The owner requested SDK 36; newer Compose releases requiring SDK 37 were not adopted. No lint baseline or blanket issue suppression was added.

Initial compile errors in navigation color configuration and experimental font API opt-in were fixed. A missing debug network-policy attribute was fixed. Moving launcher resources exposed stale Gradle resource outputs; a clean build with build cache disabled resolved it. The normal subsequent build also passed. Build tooling reported SDK XML version compatibility and inability to strip `libandroidx.graphics.path.so`; the library packaged successfully. No missing SDK component was installed.

### Device and API checks

Device: `Pixel_9_Pro_XL`, `emulator-5600`, API 36, owned by this chat, writable instance. Final debug APK installed successfully and the app launched without an observed crash.

| Check | Actual result |
| --- | --- |
| Local API | `GET http://127.0.0.1:3000/health` returned `{"status":"ok","movies":9919}` before testing and after restoration |
| Emulator API access | Explore placeholder displayed `Catalog connected · Movies: 9919` using debug `10.0.2.2` host |
| Four destinations | Explore, Swipe, Genie and Arcade opened with matching selected outline icons and Coral indicators |
| Settings return | Settings opened full screen with navigation hidden; top-bar Back returned to Arcade, the preceding destination |
| Appearance | Light Paper/Ink and dark Night/Paper renderings visually checked; system bar contrast changed with appearance |
| Spanish | App locale set to `es` with Android locale manager; translated headings, status and navigation were observed |
| 200% text | Spanish text scaled; body content remains scrollable and navigation labels wrap within their items after width adjustment |
| API stopped | Stopped the server owned by this chat, confirmed host health check failed, pressed Check connection; error message and Retry appeared |
| Recovery | Restarted API and pressed Retry; connected count returned |
| Release policy | Merged release manifest has `usesCleartextTraffic=false` and no debug network-security config; release BuildConfig uses the HTTPS public API |
| Repository hygiene | `git diff --check` passed; local.properties, APKs and data/movies.json are ignored; no catalog asset generated |

Screenshots: [light](screenshots/stage-0/light.png), [dark](screenshots/stage-0/dark.png), [API error](screenshots/stage-0/api-error.png), [Spanish at 200% text](screenshots/stage-0/spanish-large-text.png). Dark/error checks preceded the final navigation-width adjustment; the Spanish and light captures use the final shell. The large-text capture caught the genuine loading state during a connection request.

### Limits and follow-up

- These are foundation placeholders. No movie browsing, ratings, undo persistence, recommendations or games are claimed complete.
- Large translated navigation labels now wrap without overlap, including within words; label spacing/alignment needs refinement during stage 9’s accessibility pass. TalkBack, API 26 runtime, tablet/rail layout, and all-locale device walkthroughs were not validated. All four locale resource sets were checked for matching keys and valid XML.
- The emulator’s Android CLI hierarchy inspector intermittently returned an invalid layout; raw UIAutomator inspection exited 137. Plugin diagnostics found the device/process/display running and no reported diagnostic findings. Used fresh screenshots and coordinates for the affected device interactions; Minimap matching was sometimes ambiguous. This is not a completed automated UI test suite.
- Release APK is unsigned and was not installed or used against the public API. No production signing or publication was attempted.
- Full catalog streaming/lazy retry behavior was verified with synthetic unit-test responses, not a production-size catalog on device; the stage 0 shell uses `/health`. Genie/Trivia will exercise the full live dataset in their stages.
- Launcher artwork remains an interim raster; no vector logo, production launcher approval or trademark clearance is implied.

The API was restored and left running at **http://127.0.0.1:3000**. The emulator remains running with English locale, normal text scale and light appearance for owner review.

## Stage 1 — Explore — 2026-11-17

Status: approved by the owner on 2026-11-17 for commit and push. Stage 0 was approved, committed and pushed to the existing `origin/main` as `9ddf76d`. The owner explicitly instructed not to start stage 2; it remains unstarted.

### Delivered

- Explore replaces its foundation placeholder with a search field, horizontally scrolling API genre chips, six sort choices, spotlight, count and two-column adaptive poster grid.
- Search sends trimmed `q` for title/director/cast matching after 300 ms. Changing query, genre or sort cancels obsolete requests and resets the page offset. Filters use SavedStateHandle; scroll position is saveable across normal navigation/recreation.
- Initial loading, no-results/reset, full error/Retry, independent genre error/Retry and paging error/Retry states. Paging errors keep existing films visible. Explicit Load more requests pages of 24 without downloading the complete catalog.
- Spotlight shows the first matching film in the selected order; it is not a recommendation. All matching films, including the spotlight film, remain in the grid.
- Shared Coil loader with disk cache disabled, supplied poster URLs unchanged, layout-constrained decoding, and an id-derived neutral fallback while artwork is missing/loading/failed. Missing years are hidden; no ratings are invented or shown.
- Original per-record source/dataset links plus scrollable catalog attribution and source terms. Movie cards await stage 2 details; source buttons already work independently of that route.
- 69 aligned UI string keys across English, Spanish, German and Portuguese. Movie titles and genres retain their catalog language.

### Automated validation

Final command passed on the same JDK/SDK environment as stage 0:

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --console=plain
```

Debug APK and unsigned release APK generated. **16 tests passed, zero failures/errors.** New tests cover Explore repository query/genre/sort/offset contracts, debounce and stale response cancellation, independent genres/page failures and retries, filter resets/restoration, concurrent Load more guards, deduplication/cursor advancement, empty-page termination and paging retry. A transport regression primes a pooled connection, disconnects the subsequent GET after receiving it, and verifies successful recovery on a fresh connection with the same request path.

Final lint: **0 errors, 60 warnings** — 41 unused resources reserved for later stages, 18 dependency/build update notices, and 1 owner-selected target-SDK notice. The existing SDK XML compatibility/native-symbol stripping warnings remain. No lint baseline or blanket suppression was introduced.

`git diff --check` passed. All resource XML parses, all four locales have the same unique keys and matching format arguments, and generated APK/local SDK configuration/catalog files remain ignored.

### Device and live API validation

Used only this chat’s `Pixel_9_Pro_XL` (`emulator-5600`, API 36); the connected physical Pixel 3a was not used. The final debug build was installed and run.

| Flow | Actual result |
| --- | --- |
| Initial catalog | Genre chips, source poster imagery and catalog count loaded from the local API; final health response reported 9,919 movies |
| Search | Entered `batman`; 52 results appeared, matching the host API |
| Sort | Selected Newest first; first result and year matched the API’s descending-year response |
| Combined filter | Selected Action while search/sort remained active; count narrowed to 44 and first result matched the combined API query |
| Empty state | A deliberately unmatched query showed No films found, explanatory copy and Reset filters; reset restored the catalog/default sort/all genres |
| Paging failure | At Loaded: 24 / 9919, stopped the owned API process and pressed Load more; existing films stayed visible with paging error and Retry |
| Paging recovery | Restarted API, pressed Retry; next-page posters appeared at the preserved scroll position and matched the API’s `offset=24` response |
| Full failure | Stopped API, force-stopped/relaunched app; both catalog error/Retry and independent genre error/Retry appeared; host health check failed as expected |
| Full recovery | Restarted API and pressed Retry; genres, movie count and posters returned |
| Light/dark/localization | Captured English light mode and Spanish dark mode, including translated full-error and recovered content; restored English/light afterward |
| Image fallback | A neutral title fallback was observed while artwork was unavailable/loading; supplied artwork later appeared when successfully fetched |
| Catalog persistence | Debug app directory inspection after browsing showed an empty `cache/` and only `files/profileInstalled`; no catalog or poster disk files were observed |
| Review | Independent review identified scrollability and per-record attribution gaps; both were fixed and re-reviewed with no remaining actionable findings |

The first live search initially failed with `IOException: unexpected end of stream` while reading headers from a reused connection. Stage 0 had disabled OkHttp connection recovery. Enabling standard recovery for this GET-only API resolved live search, genre and sort requests after idle periods. Temporary request diagnostics were removed. The regression test covers this recovery; real API-down checks still produce explicit Retry states.

### Captures and limits

Ten full-resolution captures are saved locally in **ignored** `validation/screenshots/stage-1/`: `light.png`, `search.png`, `sorted.png`, `genre.png`, `empty.png`, `page-1.png`, `paging-error.png`, `page-2.png`, `error-spanish-dark.png`, `spanish-dark.png`. They contain downloaded catalog/poster imagery and are deliberately excluded from commits. The tracked stage 0 captures remain unchanged.

The emulator hierarchy inspector/selector-based typing still failed intermittently. Used fresh screenshots with coordinate taps and device-scoped input/swipes to complete those checks. Plugin diagnostics reported no concrete findings but did not resolve a target device when two devices were pinned. Minimap labels were best-effort; no automated UI-test-suite pass is claimed.

Not validated in stage 1: API 26/physical-device runtime, tablet/rail behavior, TalkBack, all four locales on device, complete 200% text behavior, process-death scroll restoration, every sort option against live data, and external browser navigation for each attribution link. Light/dark/large-text Compose previews exist but were not rendered separately. These accessibility/layout checks remain part of stage 9. An unsigned release was built but not installed, signed or published.

The API is restored and verified at **http://127.0.0.1:3000**. Emulator remains running on the Explore catalog, English locale, normal text size and light appearance for owner review.

### Subsequent physical-phone launch — 2026-11-17

At the owner’s request, installed and launched the current Stage 1 debug app on the connected **Pixel 3a, Android API 32**. The debug API address is now configurable with `-Plumio.apiBaseUrl`; the emulator default remains unchanged. Built using `http://127.0.0.1:3000/` and configured device-scoped ADB reverse forwarding for port 3000. Added debug-only loopback HTTP permissions; release retains fixed HTTPS configuration.

Phone-specific `assembleDebug` and `lintDebug` passed. APK installation returned Success; launch identified `dev.master.lumio/.MainActivity`. Fresh physical-device screenshot confirmed Explore in the phone’s existing dark appearance, loaded genre chips, supplied poster artwork and **Films: 9919**. No phone appearance, locale or personal data was changed. The hierarchy inspector again failed, so visual confirmation was used. This was a launch/catalog smoke check, not a complete physical-device walkthrough. The app, local API and ADB forwarding were left running for the owner.

## Stage 2 — Movie details and ratings — 2026-11-17

### Delivered

- `MovieDetails(id)` full-screen route from Explore grid cards and the spotlight (with "View details" click labels); back arrow uses `navigateUp`, so repeated taps cannot empty the back stack.
- Online `/movies/{id}` load with loading, not-found (404) and connection-error + Retry states. No catalog data is cached.
- Poster, year · runtime · genres, title, synopsis, and movie information: release year, genres, runtime, director, cast, source rating with locale-formatted score and pluralized vote count. Missing values are hidden; a "no further details" line appears only when every field is missing. Source/dataset links moved from Explore cards to details.
- Like / Dislike / Not seen yet as a single-choice group (`Role.RadioButton`, ≥72 dp tall). Room 2.8.5 (KSP 2.2.21-2.0.5) stores `ratings` with the planned snapshot (title, year, posterUrl, genres, director, cast) and an `undo_log` capped at 200 entries. Each save shows a snackbar with Undo; undo restores the previous value and timestamp, or removes a first rating, and only applies while it is still the latest change. Re-selecting the current value is not a change. Schema exported to `android/app/schemas/`. The database is already excluded from backup/device transfer.
- 21 new string keys plus one plural in all four locales (English, Spanish, German, Portuguese). Plural `many` forms added for Spanish/Portuguese.
- Two-column layout at ≥720 dp container width (not checked on a device).

### Automated validation

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
ANDROID_SERIAL=emulator-5600 ./gradlew :app:connectedDebugAndroidTest
```

All passed. **29 JVM unit tests, zero failures** (13 new): `RatingsLedgerTest` (first rating + undo removal, previous value/timestamp restore, same value no-op, stale undo id ignored, bounded history), `MovieDetailsViewModelTest` (error/retry, not found, rate + undo, change + undo, no rating before load, save failure, re-select no-op), `MovieRepositoryTest` (id path, 404 vs 500). **1 instrumented Room test passed on `emulator-5600`** (real schema, list converter with commas in names, transactions, undo through Room).

Final lint: **0 errors, 53 warnings**: 34 unused resources reserved for later stages, 18 dependency/build update notices, 1 owner-selected target-SDK notice. The three warnings introduced during this stage (two `MissingQuantity`, one `ConfigurationScreenWidthHeight`) were fixed. No baseline or suppression was added. All four `strings.xml` files contain the same 91 keys.

### Device and live API validation

`Pixel_9_Pro_XL` (`emulator-5600`, API 36) against the local API (health: 9,919 movies). The connected Pixel 3a was not used.

| Flow | Actual result |
| --- | --- |
| Open from spotlight | ¡A Volar, Joven! opened with supplied poster, "1947 · COMEDY", synopsis; bottom navigation hidden, back arrow shown |
| Like | Like highlighted in Coral; snackbar "Saved as Like" with Undo |
| Persistence | Force-stopped and relaunched app, reopened the movie; Like still selected |
| Change + Undo | Not seen yet saved with snackbar; tapping Undo showed "Rating undone" and restored Like |
| Open from grid, Spanish + dark | Per-app locale `es` and dark mode: ¡Qué Despadre! showed translated labels, "4,0 / 10 · 2 votos", missing runtime/director/cast hidden, catalog synopsis kept in its source language, one deduplicated source link |
| Error + Retry | With emulator airplane mode on, opening a movie showed the Spanish connection error and Reintentar; after restoring the network, Retry loaded synopsis and rating controls |

Snackbar action colour initially used Material's default purple; it now uses the Editorial palette (Ink surface, Coral action), verified in the later captures.

### Captures and limits

Six captures in **ignored** `validation/screenshots/stage-2/` (they contain supplied poster imagery): `details-light.png`, `liked-snackbar.png`, `unseen-snackbar.png`, `undo-restored.png`, `spanish-dark-info.png`, `spanish-dark-error.png`. `.gitignore` now excludes this folder.

The offline check used emulator airplane mode instead of stopping the API, because the API process was already running before this session. The not-found state was covered by unit tests only. Not validated on a device: German/Portuguese, the wide two-column layout, TalkBack, 200% text, process death during a save, and external browser opening of source links. Compose previews (light, dark at 200% text, error) compile but were not rendered. The connected instrumented test reinstalled the app, so earlier emulator ratings were cleared before the walkthrough.

An independent review found no blocking defects. Two of its low-severity suggestions were applied afterwards: rating buttons stay enabled while saving (the ViewModel already ignores repeat taps), for stable TalkBack focus; and details opens only while Explore is current, so two quick taps cannot stack two details screens. Build, the 29 unit tests and lint (0 errors, 53 warnings) passed again, and the final APK was reinstalled, but the walkthrough above was not repeated after these two changes.

Emulator left running on English, dark appearance, airplane mode off, showing a movie's details. Its appearance was switched to light and then dark during testing; its original setting was not recorded. API left running at **http://127.0.0.1:3000**.

### Physical-phone walkthrough — 2026-11-17

At the owner's request, personally tested the current Stage 2 build on the connected **Pixel 3a, Android 12L / API 32**, using its existing English UI, dark appearance and normal text size. No emulator was used for this walkthrough. Rebuilt with the phone's loopback API address and installed the APK:

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/ --console=plain
```

Build passed; **29 JVM tests passed, zero failures/errors/skips**. Lint reported **0 errors, 53 warnings**. Device-scoped ADB reverse forwards port 3000 to the existing host API. Final host health check returned `status: ok`, **9,919 movies**.

| Flow | Physical-phone result |
| --- | --- |
| Spotlight entry | Opened ¡A Volar, Joven!; details displayed title, year, genre, synopsis and rating controls, with bottom navigation hidden |
| First rating + Undo | Like highlighted with Saved as Like snackbar; Undo showed Rating undone and cleared the selection |
| Persist Like | Force-stopped/relaunched the app and reopened the movie; Like remained selected |
| Change + Undo | Changed Like to Dislike; Undo restored Like |
| Not seen yet | Saved Not seen yet; force-stop/relaunch preserved the selection |
| Missing metadata | Missing runtime, director and cast were omitted; source score 7.9 / 10 and 83 votes were displayed |
| Source link | View source opened the supplied Kaggle dataset page in Chrome Beta; system Back returned to Lumio |
| Grid entry + complete metadata | Searched 12 Angry Men, opened the 1957 film from its grid card, and scrolled through details. Runtime 96 min, director Sidney Lumet, cast Lee J. Cobb / Henry Fonda, score 8.5 / 10 and 6227 votes matched the live API record |
| Back navigation | Both system Back and toolbar Back returned to Explore; the 12 Angry Men search remained active after toolbar Back |
| Connection error + Retry | Removed only this phone's port-3000 forwarding, opened details and observed the connection error with Retry. Restored forwarding and tapped Retry; the film's details and rating controls returned |
| Cleanup | No ratings database existed before the first Stage 2 launch. With the app force-stopped, removed only the test-created lumio.db and its SQLite sidecars; reopened the tested movie and visually confirmed all rating choices unselected |

No blocking defects were observed in these flows, and no application code changes were needed. Saved 13 screenshots under ignored `validation/screenshots/stage-2/phone/`; downloaded catalog imagery remains excluded from commits. The hierarchy inspector failed, so testing used fresh screenshots and device-scoped taps, swipes and input; this was an actual manual walkthrough, not an automated UI-suite pass. Both supplied artwork and procedural title fallbacks were observed.

Physical-phone limits: no TalkBack, large-text, wide layout, other locales, process death during an in-flight save, rapid double-tap stress test, or live 404 check. The not-found state remains covered by unit tests. Instrumented Room tests were not rerun on the owner's phone. Phone appearance, font scale and locale were unchanged; no unrelated app data was cleared. The app is left open on movie details with no test ratings, API running and forwarding restored. Stage 2 remains pending owner approval; nothing was committed or pushed, and Stage 3 was not started.


## Stage 3 — Swipe — 2026-11-17

### Delivered

- Replaced Swipe placeholder with an Editorial screen: headline, one supplied poster/fallback, movie title/year/genres, online details link, Undo, three equally accessible rating actions and gesture guidance. All six new UI keys are present in English, Spanish, German and Portuguese (97 resource keys per locale).
- Popularity-descending 24-film API pages, excluding every saved id including UNSEEN. Fully-rated pages are skipped; only unread page records remain in memory. Queue reacts to choices saved in details. No catalog disk persistence, bundled data or new dependencies.
- Right/left/down dominant-axis drags of ≥72 dp match Like/Dislike/Not seen yet buttons. Short/upward/tied diagonal gestures do not rate. Saves are guarded against repeated taps and advance only after transaction success. Failed saves preserve the current movie with feedback.
- Persistent global Undo from the existing bounded log, available after restart and even when the queue is empty or the network fails. Undo restores a previous value/timestamp or removes a first rating, then reloads online; a still-rated film remains excluded. Details Undo returns consumed cards to eligibility.
- Loading, exhausted + Refresh, connection error + Retry, and local-storage error + Retry states. No timed card animation or fling. System animator disablement is observed while visible and removes drag transforms. Labeled buttons and card accessibility actions provide alternatives to gestures.

### Automated validation

`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed during development. After the review fix and light-mode link-colour correction, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed again. **40 JVM tests, zero failures** (11 new Swipe tests): fully-rated page skipping, UNSEEN exclusion, all button values, duplicate-tap protection, external details changes and Undo, persistence through ViewModel recreation, previous-value Undo exclusion, save failure, page-error retry at the same offset, empty refresh with new films, ratings changed during an in-flight request, storage-read failure + retry, and gesture thresholds/directions.

Installed app and test APKs with device-scoped `adb install -r`, then ran `adb -s emulator-5600 shell am instrument -w dev.master.lumio.test/androidx.test.runner.AndroidJUnitRunner`. **1 Room integration test passed**, extended to check rated-id and latest-undo observations. It uses an in-memory DB; existing emulator ratings were preserved. The final screen-only colour correction did not change the Room implementation; the device test was not repeated after it.

Lint: **0 errors, 52 warnings** (33 reserved unused resources, 18 dependency/build update notices, 1 target-SDK notice). All four string-resource key sets match. `git diff --check` passed.

The first pagination regression test caught an unintended extra retry caused by a ratings emission after a network failure; fixed to retain Error until Retry. Independent review caught a consumed movie missing after Undo in details; fixed by resetting the queue when observed rated ids shrink and covered by a regression test. Re-review found no remaining blocking defects.

### Device and live API validation

`Pixel_9_Pro_XL`, `emulator-5600`, API 36. Local API verified before and after at `http://127.0.0.1:3000/health`: 9,919 movies. Connected physical phone was not used.

| Flow | Actual result |
| --- | --- |
| Initial queue | Spider-Man: No Way Home appeared first, matching popularity order, with title/year/genres and three actions |
| Right / left / down gestures | Right advanced to The Batman; left advanced to No Exit; down advanced to Encanto |
| Undo | Returned No Exit with “Rating undone” feedback |
| Buttons | Like on No Exit, Not seen yet on Encanto, Dislike on The King’s Man each advanced the queue |
| Stored values / restart | Inspected emulator DB copy in `/tmp`: Spider-Man LIKE, The Batman DISLIKE, No Exit LIKE, Encanto UNSEEN, The King’s Man DISLIKE. Force-stop/relaunch retained choices and Undo availability; next card remained The Commando |
| Spanish + dark + animations disabled | Translated labels fit; titles/genres remain source text. Right swipe on The Commando advanced to Scream with animator duration scale 0 |
| Connection failure + Retry | Emulator airplane mode + fresh process showed Spanish connection error and Reintentar. Immediate retry while reconnecting still errored; retry after reconnection loaded Scream |
| Details → rating → Undo → Back | Opened Scream details from Swipe, saved Like, tapped snackbar Undo and returned: Scream remained eligible as the current Swipe card |

Three captures, visually inspected, in ignored `validation/screenshots/stage-3/`: `swipe-light.png`, `swipe-spanish-dark-reduced-motion.png`, `swipe-spanish-error.png`. Supplied poster imagery stays uncommitted. First light capture caught a navigation transition and was replaced with a settled final-build capture.

### Limits and handoff

Network isolation used emulator airplane mode instead of stopping the pre-existing shared API. Ratings and their snapshots remain on the emulator after this walkthrough. Animations were disabled for the gesture smoke check, but motion during a held drag was not recorded. German/Portuguese rendering, TalkBack action execution/focus, 200% text, wide screens, end-of-catalog exhaustion and actual on-device disk failures were not manually exercised; queue exhaustion, read/save failures and page boundaries are unit-tested. Light/dark previews compile but were not separately rendered. History remains stage 4.

Emulator left on Swipe, English, dark appearance (the starting appearance), airplane mode off; animator duration scale restored to its original unset value. The local API remains running. Stage 3 awaits owner approval; no commit/push or stage 4 implementation in this session.

### Subsequent physical-phone launch — 2026-11-17

At the owner’s request, built the current stage 3 debug app with `./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/` (passed), configured device-scoped ADB reverse port 3000 and installed with `adb install -r` on the connected Pixel 3a. Installation returned Success and `am start` launched `dev.master.lumio/.MainActivity`. Local API health was verified: 9,919 movies.

The phone opened on Swipe in its existing dark appearance. Hierarchy and visually inspected capture confirmed Spider-Man: No Way Home loaded with supplied artwork, year/genres, details link and all three rating buttons. A short scroll outside the poster revealed the full buttons on the smaller screen. No rating was made, no app data was cleared and no phone appearance/locale settings were changed. This was a launch/catalog smoke check, not a complete physical-device walkthrough. Capture: ignored `validation/screenshots/stage-3/phone-swipe.png`. App, API and ADB forwarding left running for owner use; the connection to this computer is required for the local catalog.


## Stages 2–3 approval and publication — 2026-11-17

The owner approved stages 2 and 3, requested commit/push, and authorized stage 4. Approved app source, Room schema, tests, dependency declarations, documentation and ignore rules were committed on `main` as **`9e468d4`** and pushed successfully to the existing `origin/main` (`7b47f94..9e468d4`). Local exploratory design assets, movie/trivia data, posters, captured screenshots, secrets and machine configuration were excluded. `initial-state` was not changed.

## Stage 4 — Your choices — 2026-11-17

### Delivered

- Full-screen `History` route from Swipe’s checklist action, with back navigation to Swipe and online movie-details navigation/back to history. A lazy list shows saved snapshots newest first, each with title, available year, supplied poster/fallback, current choice and locale-formatted saved date/time. Stable movie-id keys retain list identity.
- Choice editor with three single-choice options, Save and Cancel. Saving the existing value is a no-op. Writes use the saved snapshot fields; viewing/editing history does not fetch the catalog.
- Per-film Undo targets that film’s most recent retained entry, checked transactionally against the displayed id. It restores the previous value/timestamp or removes a first rating, preserving all other films and global undo entries. The existing 200-entry limit remains; Undo is disabled for a film with no retained step. Swipe remains reactive to history changes.
- Loading, empty guidance, local-read error + Retry, write-failure and stale-undo feedback. Row-specific accessibility labels, single-choice dialog semantics, 48 dp minimum actions, light/dark/large-text preview source. Twelve new UI keys; all four locales contain the same **109 keys**.

### Automated validation

```sh
./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Passed. **48 JVM tests, zero failures**: eight new tests cover empty/reactive newest-first history including UNSEEN, per-film Undo with a newer other-film choice, previous-value/timestamp and first-rating removal, snapshot-preserving edits without network, unchanged values and repeated taps, read-error Retry, failed rate/undo writes, stale ids, trimmed undo availability, and wrong-movie/stale ledger protection.

App and test APKs installed using device-scoped `adb install -r`, preserving existing emulator data. `adb -s emulator-5600 shell am instrument -w dev.master.lumio.test/androidx.test.runner.AndroidJUnitRunner`: **2 Room tests passed**, including the real history projection/query, ordering, per-film stale-id rejection, restoration, removal and unaffected global Undo. No database schema version change or migration was needed.

Lint: **0 errors, 50 warnings** (31 reserved unused resources, 18 dependency/build update notices, 1 selected target-SDK notice). Four-locale resource alignment and `git diff --check` passed. Independent read-only review found no blocking defects in history, per-film Undo, offline snapshot use or navigation.

### Emulator validation

`Pixel_9_Pro_XL` (`emulator-5600`, API 36), local API verified before and after: `http://127.0.0.1:3000/health`, 9,919 movies. The physical phone was not changed during stage 4.

| Flow | Actual result |
| --- | --- |
| Open history | Swipe’s Your choices opened full-screen history, newest first: The Commando LIKE, The King’s Man DISLIKE, Encanto UNSEEN; bottom navigation hidden and back arrow present |
| Change an older choice | Changed The King’s Man to UNSEEN through the dialog; updated film moved to the first row with a new saved time and “Choice updated” feedback |
| Undo change | Restored DISLIKE and the original saved time/order; newer The Commando choice remained LIKE |
| Undo first rating | Per-film Undo removed The King’s Man while keeping other rows; Back to Swipe immediately showed it as the current eligible film |
| Spanish + dark, catalog disconnected | Airplane mode on: history still showed localized labels/dates. Changed The Commando from LIKE to DISLIKE and undid it, restoring LIKE and its original timestamp |
| Details from history | While offline, Ver detalles showed the catalog connection error + Reintentar. Restored network/English and tapped Retry: online The Commando details loaded; Back returned to history |

Two screenshots captured and visually inspected in ignored `validation/screenshots/stage-4/`: `history-light.png`, `history-spanish-dark-offline.png`. Supplied poster imagery remains uncommitted. Existing loaded images remained in memory during the offline history check; this does not establish offline poster availability. Catalog isolation used emulator airplane mode, preserving the shared API for the owner’s phone.

### Limits and handoff

Empty/local-read-error/stale-undo/disk-write-failure/trimmed-log states were covered by unit tests, not induced on the device. TalkBack execution/focus, German/Portuguese rendering, 200% font scale, wide screens, and cold offline thumbnail fallbacks were not manually exercised. Preview functions compile but were not separately rendered. Full accessibility/layout hardening remains stage 9.

The emulator retains the walkthrough’s personal choices: The King’s Man’s first rating was removed to test return to Swipe; the temporary The Commando edit was undone. Left on Your choices, English, original dark appearance, airplane mode off. API remains running. The physical phone retains stage 3. **Stage 4 is uncommitted and awaits owner approval; stage 5 was not started.**


### Subsequent physical-phone launch — 2026-11-17

At the owner’s request, built stage 4 with `./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/` (passed), reapplied phone-scoped ADB reverse port 3000, and installed with `adb install -r` on the connected Pixel 3a. Install returned Success; `am start` launched Lumio. API health verified: 9,919 movies. No app data was cleared or ratings changed.

Hierarchy confirmed Swipe with the new Your choices action and a loaded catalog movie. The phone UI changed between inspection and the attempted history tap; the subsequent visually inspected capture showed Explore with the search keyboard open, so history opening was not established in this launch check. No further UI actions were made. Capture renamed to ignored `validation/screenshots/stage-4/phone-launch.png`. Step 4 is installed; access history through Swipe → Your choices. App, API and forwarding remain available for owner use.


## Stage 4 approval and publication — 2026-11-17

The owner approved Step 4, requested commit/push, and authorized Genie. Step 4 source, tests, documentation and ignore rules were committed on `main` as **`d47edb4`** and pushed successfully to the existing `origin/main` (`9e468d4..d47edb4`). Exploratory design assets, catalog data, posters, captured screenshots and machine configuration remained excluded. `initial-state` was not touched.

## Stage 5 — Genie — 2026-11-17

### Delivered

- Replaced Genie’s placeholder with an Editorial recommendations screen, loading indicator, getting-started guidance, no-positive-match state, catalog error + Retry and saved-choice-read error + Retry. History and online details are reachable with back navigation. No dependency, database schema, API or data-persistence changes.
- Existing `CatalogRepository` lazily downloads/stream-decodes `/catalog` once per successful process session under its mutex, keeping it in memory only. No fetch is needed before a like/dislike preference exists.
- Pure deterministic engine sums genre ±1, director ±3 and actor ±2 feature affinities across saved snapshots. All saved ids (including UNSEEN) are excluded; UNSEEN contributes zero. Blank metadata is ignored; repeated/case-varied features count once per movie, normalized using `Locale.ROOT`. Positive net scores only; top 30 ordered by score, popularity, normalized title and id. No invented ratings, facts or confidence percentages.
- Each recommendation displays up to three strongest positive genre/director/actor contributions using supplied names. Dislikes still reduce the total used to rank/filter candidates; introductory text explains that behavior. Full source attribution remains reachable through online details.
- Ratings/history/Undo changes recompute off the main thread; `collectLatest` cancels prior loads/scoring so stale choices cannot replace current results. Storage errors also cancel pending catalog work. Thirteen new keys; all four locales contain the same **122 keys**.

### Automated validation

`./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed. **61 JVM tests, zero failures** (13 new): six engine tests cover exact weights/reason contributions, summed likes/dislikes and nonpositive exclusion, all-rated-id exclusion/UNSEEN neutrality, duplicate/blank/case normalization under Turkish locale, feature-type separation, deterministic ties and result limits. Seven Genie ViewModel tests cover getting-started without requests, positive matches/exclusion, reactive Undo and dislike cancellation, missing metadata/dislike-only no-match, network Retry, read error + Retry, late catalog data after storage failure, newer preferences cancelling older results, and removal of the last effective preference.

Lint: **0 errors, 49 warnings** (30 reserved unused resources, 18 dependency/build update notices, 1 selected target-SDK notice). Resource-key alignment and `git diff --check` passed. The independent reviewer authored the engine tests and found no blocking defects in the implementation. Their own Gradle attempt was blocked by sandbox access to the wrapper lock; the lead’s full build above successfully executed all six tests. Existing Room code was unchanged; its two device tests from stage 4 were not rerun.

### Emulator and live API validation

`Pixel_9_Pro_XL` (`emulator-5600`, API 36). Installed with device-scoped `adb install -r`, preserving saved choices. The physical phone was not modified and remains on Step 4. Local API health verified before/after: `http://127.0.0.1:3000/health`, 9,919 movies.

| Flow | Actual result |
| --- | --- |
| Full catalog / loading | Genie showed “Finding films for you…” then recommendations; first was Yu-Gi-Oh! The Movie with supplied poster/year/genres and Action, Adventure and Comedy reasons |
| Details / unseen exclusion | Opened the recommendation’s online details and saved Not seen yet; Back to Genie removed it and Zombie Tidal Wave became first |
| History / Undo | Opened Your choices from Genie and undid that first UNSEEN choice; Back returned to Genie with Yu-Gi-Oh! The Movie first again |
| Spanish + dark | Intro, heading, history action, reasons and details label translated; source titles/genres retained their catalog language |
| Fresh offline process | Emulator airplane mode + force-stop/relaunch removed the in-memory session; opening Genie showed Spanish connection error and Reintentar |
| Recovery | Restored connectivity/English and tapped Retry; loading completed and the recommendation list returned |

Three captures were visually inspected and remain ignored in `validation/screenshots/stage-5/`: `genie-light.png`, `genie-spanish-dark.png`, `genie-spanish-error.png`. Network isolation used emulator airplane mode to preserve the pre-existing shared API. Saved snapshots and undo were preserved; the temporary UNSEEN choice was undone.

### Limits

Getting-started/no-positive-match/local-read-error states were unit-tested but not manually induced on the main emulator (existing preferences were preserved). Director/actor reason calculations were validated with synthetic fixtures; live walkthrough reasons were genre-based, reflecting the catalog’s sparse director/cast metadata. German/Portuguese rendering, TalkBack action/focus, 200% font scale, wide screens, background memory pressure and a full 30-card scroll were not manually exercised. Light/dark/large-text previews compile but were not separately rendered. The memory-only session catalog can be reused until process death; no disk fallback exists.

Stage 5 is uncommitted and awaits owner approval; stage 6 is not started.

Genie handoff: emulator left on the recommendation list, English, original dark appearance, airplane mode off. Local API remains running. The physical phone retains Step 4.

### Subsequent physical-phone launch — 2026-11-17

At the owner’s request, built Genie for the phone with `./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/` (passed), reapplied phone-scoped ADB reverse port 3000, and installed with `adb install -r` on the connected Pixel 3a. Installation returned Success and `am start` launched Lumio; no app data was cleared or ratings changed. Local API health verified: 9,919 movies.

The phone was on Genie when inspected: initially loading, then actual recommendations with supplied posters, year/genres, positive genre reasons and View details. Visually inspected ignored capture: `validation/screenshots/stage-5/phone-genie.png`. This was a launch/full-catalog smoke check; no further phone UI interactions or preference changes were made. App, API and forwarding left available for owner use. The physical phone now has Step 5.


## Step 5 approval and publication — 2026-11-17

The owner approved Genie, requested commit/push and authorized Arcade hub + Trivia. Step 5 source, tests, documentation and ignore rules were committed on `main` as **`864a7c5`** and pushed successfully to the existing `origin/main` (`d47edb4..864a7c5`). Catalog/question/poster data, captured screenshots, exploratory assets, secrets and machine configuration were excluded. `initial-state` was not changed.

## Stage 6 — Arcade hub and Trivia — 2026-11-17

### Owner rules and delivered behavior

The owner selected **5 questions per round, 20 seconds each** when asked about the plan’s proposed round length. This supersedes its original 10-question proposal.

- Arcade hub with Trivia launch/return-to-round action and a clearly marked forthcoming Poster Puzzle card. Poster Puzzle implementation remains stage 7.
- Full-screen Trivia loads the existing process-memory catalog only on first play, validates four distinct nonblank options and a valid answer index, deduplicates question ids and samples five without replacement. Fewer than five valid source questions produces an unavailable state; network failures show Retry. Current local catalog inspection found **1,109 valid questions across 247 movies**; counts may change. No source questions are bundled, downloaded into Git, translated or fabricated.
- Monotonic 20-second countdown, four labeled answer buttons, one 50:50 per round removing two wrong options, explicit pause/resume, correct/incorrect/timeout feedback, four-second automatic feedback advance, Keep reading hold and manual Next / See results.
- Correct answers earn 100 plus 25 per consecutive correct answer after the first, capped at 100 extra. Wrong/timeout earns zero and resets the streak. Five perfect answers earn 750. Results show points, correct count and best streak; Play again resets score/streak/lifeline and samples a new round.
- Controller synchronizes time before every input, preventing a late tap from earning points. Pause hides the question. Leaving/backgrounding pauses and stops polling; returning/recreation requires Resume. Arcade-scoped ViewModel retains a paused round through hub/section navigation and configuration changes in the live process. Questions/scores are intentionally lost on process death; there is no catalog/round persistence.
- UI strings aligned across English, Spanish, German and Portuguese: **154 resource keys each**. Supplied questions/answers remain English, disclosed in the hub. Pine/Sage and Brick/Rose feedback has explicit correct-answer text. Game sounds/settings remain stage 8; no sound was added here.

### Automated validation

`./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed, including the final ticker/palette cleanup. **74 JVM tests, zero failures** (13 new): six pure TriviaGame tests cover valid selection/deduplication, exact points/capped streaks, wrong/timeout resets, elapsed-time and feedback transitions, repeated-answer rejection, 50:50 correctness/one-per-round/removed-option rejection, pause/hold and five-question results. Seven controller tests cover lazy five-question/20-second load, pause/background/resume, exact-deadline tap rejection, hold/manual/automatic advance, replay resets, network Retry and insufficient questions, offscreen loads and cancelled stale loads.

The initial controller test invocation was interrupted because test-created paused ViewModels were not cleared, leaving scheduled ticker jobs alive after assertions. Tests now clear their ViewModelStore in `finally`. Production code also avoids creating a ticker for an unavailable catalog and stops it for paused/hidden games. The final complete suite passed after these corrections. No tests were removed or disabled.

Final lint: **0 errors, 40 warnings** (21 reserved unused resources, 18 dependency/build update notices, 1 selected target-SDK notice). Two new quantity warnings and a German spelling heuristic were resolved through clearer label wording; no suppression/baseline added. Four-locale key alignment and `git diff --check` passed. Independent review authored the six domain tests and found no blocking defects; re-review of final ticker/lifecycle changes also found no blocking regression. Room code was unchanged; its two prior device tests were not rerun.

### Emulator and API validation

`Pixel_9_Pro_XL` (`emulator-5600`, API 36), device-scoped `adb install -r` preserving ratings. Local API health checked before and after: `http://127.0.0.1:3000/health`, 9,919 movies. Physical phone was not modified and still has Step 5.

| Flow | Actual result |
| --- | --- |
| Hub | Two cards with approved five-question/20-second rules; Trivia playable, Poster Puzzle marked Coming soon |
| Load / question | Loading Trivia appeared, followed by a supplied English question and four options with a countdown |
| Pause / 50:50 | Pause hid the question; Resume retained Question 1. 50:50 left two options including the supplied correct answer; later questions showed it used |
| Timeout / automatic advance | The first resumed question expired and advanced to Question 2 with score/streak 0 |
| Correct / hold | Answered Question 2 correctly using supplied answer data for validation; 100 points and streak 1. Keep reading stopped reveal progression across subsequent tool calls |
| Hub return | Back to Arcade showed Return to round; reopening preserved Question 2, score 100 and a paused state requiring Resume |
| Complete round | Answered remaining three questions using supplied answers, earning 125/150/175. Final results: **550 points, 4/5 correct, best streak 4** |
| Replay | Play again returned to Question 1 with score/streak 0, a fresh 20-second timer and unused 50:50 |
| Background | Home then bringing Lumio forward retained the round in Paused state requiring Resume |
| Spanish / dark | Localized statistics, controls, countdown and hub disclosure; English question/answers retained their supplied language |
| Fresh offline process / recovery | Airplane mode plus force-stop/relaunch, then Trivia: Spanish connection error + Reintentar. After network/English restoration, Retry loaded a playable round |
| Final build smoke | After final ticker/palette cleanup, reinstalled and verified Pause → Resume countdown → Pause, then Arcade → Explore → Arcade → Return to round retained Question 1 paused |

Five captures visually inspected in ignored `validation/screenshots/stage-6/`: `arcade-light.png`, `trivia-held-feedback.png`, `trivia-results.png`, `trivia-spanish-dark.png`, `trivia-spanish-error.png`. Captures predate only the final timer-polling cleanup and replacement of the countdown’s default track colour with `outlineVariant`. The full round was not repeated after those final changes; final build tests and the pause/resume/section-switch smoke above passed. Source questions in screenshots remain uncommitted.

### Limits and handoff

Network isolation used emulator airplane mode, preserving the pre-existing shared API for the owner’s phone. Wrong-answer calculations and insufficient-question state were tested with synthetic unit fixtures rather than manually forced in the live walkthrough. Process-death round persistence is intentionally unsupported; a new round must load online in a fresh process. TalkBack focus/actions, German/Portuguese rendering, 200% text, wide screens and device rotation were not manually exercised. Light/dark/large-text preview sources compile but were not separately rendered. Feedback hold/Next can require scrolling on smaller screens; further accessibility/layout checks remain stage 9. No signed release, publication or physical-phone walkthrough was performed for Step 6.

Left on Arcade, English, original dark appearance, airplane mode off, with a paused in-memory round available through Return to round. API remains running. Local ratings were not changed by Trivia testing. **Stage 6 remains uncommitted and awaits owner approval; stage 7 has not started.**

### Subsequent physical-phone launch — 2026-11-17

At the owner’s request, built Step 6 using `./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/` (passed), reapplied phone-scoped ADB reverse port 3000, and installed with `adb install -r` on the connected Pixel 3a. Installation returned Success; `am start` launched Lumio. No app data was cleared, ratings changed or phone appearance/locale settings modified. Local API health verified: 9,919 movies.

Opened Arcade and visually inspected ignored `validation/screenshots/stage-6/phone-arcade.png`: hub displayed in the phone’s current light appearance, five-question/20-second rules and Play Trivia button. Left the round unstarted so the owner can begin when ready. This was an install/hub smoke check, not a physical-phone game walkthrough. App, API and ADB forwarding remain available. The phone now has Step 6.


## Step 6 approval and publication — 2026-11-17

The owner approved Arcade hub/Trivia, requested commit/push and authorized Poster Puzzle. Step 6 source, tests, documentation and ignore rules were committed on `main` as **`4a937a7`** and pushed successfully to the existing `origin/main` (`864a7c5..4a937a7`). Catalog/trivia/poster data, screenshots, exploratory assets, secrets and machine configuration remained excluded. `initial-state` was not changed.

## Stage 7 — Poster Puzzle — 2026-11-17

**Historical initial validation:** the procedural-pattern behavior and its captures below were subsequently removed at the owner’s request. The poster-only correction at the end of this document supersedes those parts of this record.

### Delivered

- Activated the Poster Puzzle card in Arcade, with Start puzzle / Return to puzzle. Full-screen puzzle has back navigation and online movie details (including existing source/dataset links).
- Random eligible catalog movie, preferring supplied HTTP(S) posters; duplicate movie ids removed. Next avoids the immediately previous movie when alternatives exist. No URL rewriting or fabricated artwork URLs.
- Pure immutable 3×3 board, guaranteed shuffled rather than solved. Tap one tile to select/check it, another to swap; tap the same tile to cancel. Progress counts correct positions, swaps count actual exchanges. All permutations are solvable by arbitrary swaps. Completion locks moves and offers Next puzzle / Back to Arcade. No timer.
- Reference view shows the coherent full image, clears selection and prevents swaps until hidden. Next resets selection, reference and swap count. Arcade-scoped ViewModel retains a partial board within the live process through navigation/configuration changes, but catalog/rounds/images are never persisted to disk.
- One bounded Coil painter supplies nine clipped regions of a single centered crop, without nine downloads or sliced-bitmap copies. Missing/failed artwork becomes an explicitly labeled id-derived procedural pattern, stable after navigation/reconnection. Session ids reject late errors from old artwork requests. Final pattern drawing uses Paper on dark fills for contrast.
- Catalog loading/error + Retry and no-movie states; artwork-loading feedback and Next allow skipping slow artwork. Nineteen new keys plus updated hub intro, aligned across all **173 resource keys** in English, Spanish, German and Portuguese. Piece/position/selection semantics supplement visual selection. Preview source included.

### Automated validation

`./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed, including the final pattern-contrast correction. **86 JVM tests, zero failures** (12 new): six pure domain tests validate unsolved shuffles/permutations, selection/cancel/swap/count/completion, solving arbitrary boards, valid unchanged supplied URLs, poster/movie preference/dedup/previous avoidance and fallback/empty selection. Six controller tests validate lazy loading and retained start, reference selection clearing/input blocking, completion and Next resets, stable artwork fallback/stale-image rejection, missing poster/empty catalog, and Retry/cancelled stale loads.

Lint: **0 errors, 39 warnings** (20 reserved unused resources, 18 dependency/build update notices, 1 selected target-SDK notice). Locale-key alignment and `git diff --check` passed. Independent review authored the six domain tests and found no blocking defects in board behavior, image crop/sharing, fallback session or navigation. Existing Room implementation was unchanged; its two prior device tests were not rerun.

### Device / API validation

Initially `Pixel_9_Pro_XL`, `emulator-5600`, API 36, against the local API. That emulator disconnected before failure-path testing; device inventory confirmed it had stopped. Restarted the existing Pixel AVD through Android CLI, which assigned **`emulator-5554`**, and reinstalled with `adb install -r` without clearing ratings. Remaining Spanish/dark/error checks used this restarted device. The physical phone was not changed and retains Step 6. API health verified before/after: `http://127.0.0.1:3000/health`, 9,919 movies.

| Flow | Actual result |
| --- | --- |
| Live poster load | Chilangolandia (2021) loaded with nine distinct crop regions; initial progress 2/9, swaps 0 |
| Reference | Selecting a tile then Show reference displayed the coherent full poster, cleared selection, kept swaps 0 and blocked board moves until Hide reference |
| Complete solve | Used the spoken piece/position labels to perform five swaps; progress reached **9/9, swaps 5**, coherent poster and completion/Next/back controls confirmed visually |
| Next | Selected In the Heart of the Sea (2015), a different movie; swaps 0, fresh unsolved board |
| Partial-board retention | Swapped its first two positions, left through Arcade and returned; same movie/permutation and swaps 1 retained |
| Spanish / dark | Restarted emulator loaded a new live puzzle with translated instructions, progress and controls; source title/year unchanged |
| Failed artwork | With session catalog already in memory, airplane mode + Next selected Matando Cabos 2 and failed to load its poster; labeled procedural pattern appeared. Tiles swapped successfully (count 1) |
| Fallback retention | Restored network, left through Arcade and returned; the same pattern/permutation and swap count stayed stable |
| Fresh offline process | Airplane mode + force-stop/relaunch then Start puzzle showed the Spanish catalog connection error + Reintentar |
| Retry | Restored connectivity, tapped Reintentar; online movie Killing Jesus (2015) loaded with playable tiles |

Five captures were visually inspected under ignored `validation/screenshots/stage-7/`: `puzzle-light.png`, `puzzle-reference.png`, `puzzle-complete.png`, `puzzle-spanish-pattern.png`, `puzzle-spanish-error.png`. They contain supplied poster imagery; none is committed. The pattern capture predates only the final foreground-contrast correction. Network isolation used emulator airplane mode, preserving the shared API for the owner’s phone. Puzzle actions did not change personal movie ratings.

### Limits

No-movie and stale-network/image-response cases were validated with synthetic unit fixtures rather than induced with live data. TalkBack execution/focus, German/Portuguese rendering, 200% text, wide screens, rotation and a physical-phone puzzle walkthrough were not manually exercised. Reference/complete geometry was checked on the emulator with one live poster; cropping for unusual source aspect ratios was reviewed in code. Light/dark/large-text preview sources compile but were not separately rendered. The screen scrolls, and lower controls or tiles may require scrolling on smaller windows. Process death intentionally loses a puzzle; only the personal ratings store persists. Settings and sound remain stage 8.

**Stage 7 is uncommitted and awaits owner approval; stage 8 has not started.**


### Poster-only correction — 2026-11-17

The owner clarified: “If there is no poster for the movie, you should NOT use it for the puzzle.” Poster Puzzle now excludes movies with missing, malformed or non-HTTP(S) poster URLs entirely. All generated-pattern rendering and its UI string were removed. Other screens’ existing artwork fallbacks remain outside this puzzle-specific correction.

A puzzle’s swap/reference controls stay blocked until the supplied poster has actually loaded successfully. A failed image request shows an error with Retry and Next puzzle; it never creates playable substitute artwork. Next excludes movies whose artwork has failed in this process. If no eligible posters remain, an unavailable state offers Retry to recheck availability. Retrying an individual poster uses a new attempt id; late success/failure callbacks from the previous attempt cannot affect it. Existing game sound events were preserved and cannot fire for blocked swaps.

Validation: `./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed on the current shared workspace. **96 JVM tests, zero failures**, including **6 puzzle-domain tests and 9 puzzle-controller tests**. Tests cover missing-poster exclusion, no play before successful loading, failed-artwork exclusion, stale callbacks after retry, all-posters-failed/unavailable recovery, normal solve/next behavior and swap/completion sound events. Lint: **0 errors, 39 warnings**. All four resource sets align (187 keys in the current workspace, including concurrent Settings work). `git diff --check` passed. Independent read-only review found no blocking defects in the correction.

The emulator walkthrough was not repeated for this correction; the prior emulator package/reboot stall interrupted the final visual check. Earlier pattern screenshots are obsolete and are not evidence of current puzzle behavior. No phone installation or rating changes were made for this correction. Step 7 remains uncommitted pending owner approval; concurrent Settings work was preserved.

## Stage 8 — Settings — 2026-11-17

Status: authorized by the owner while Stage 7 is being finished. Implemented locally, pending review/approval. No commit or push was made in this chat; Stage 9 was not started.

### Delivered

- Full-screen Settings with System / Light / Dark radio choices and a game-sound switch. Preferences DataStore 1.3.0 stores only appearance and sound, with defaults System and on. First preference read completes before the app content is displayed; later edits keep navigation and game state intact.
- Theme and system-bar icon contrast follow the selected appearance. Trivia feedback colours follow app appearance instead of the device setting.
- Read failures show Retry and disable edits; failed writes preserve the saved value and permit retry. Settings and bundled notices do not request catalog data.
- Short Android media-stream tones for Trivia correct/incorrect/timeout feedback and Poster Puzzle swaps/completion. No replay across navigation, no downloaded sound files; audio service failure is nonfatal and the player releases when the game leaves the resumed lifecycle.
- Full existing Fraunces, DM Sans and Lucide notices, a bundled exact copy of `data/SOURCE-ATTRIBUTION.md`, and external source links. All 187 resource keys align in English, Spanish, German and Portuguese. Legal notices retain their original language.
- Light and German dark/200% Compose previews in `ui/settings/SettingsScreen.kt`.

### Automated validation

Final `JAVA_HOME=/Users/firt/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home ./android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` passed. Combined current Stage 7/8 results: **96 JVM tests, zero failures; lint zero errors and 39 warnings** (20 unused resources, 18 build/dependency notices, one newer-target-SDK notice). Eight Stage 8 tests cover actual file-backed DataStore recreation/persistence and independent preferences, unknown appearance fallback, ViewModel load/save, read Retry, failed write/retry, overlapping-write rejection, Trivia correct/wrong/timeout events and no replay, and Puzzle selection silence/swap/completion events.

The first Gradle invocation was blocked by sandbox cache access, so validation ran through approved escalation. Android Studio's bundled JDK 25.0.4 failed Gradle configuration; switching to the project's established JDK 21 resolved it. A concurrent Stage 7 update temporarily left tests referencing the old procedural-fallback state; the updated poster-state tests and integration passed together. Final checks include the system-bar and light-mode text-button contrast fixes. XML key alignment and `git diff --check` pass. Existing SDK XML compatibility and unstripped native-library warnings remain.

### Device checks and limits

- The configured `Pixel_9_Pro_XL` AVD failed boot with an existing-instance lock, including a read-only attempt. Plugin diagnostics were checked without stopping another chat's device. Created temporary `Lumio_Stage8`, same Pixel 9 Pro XL hardware profile and API 36, serial `emulator-5600`, owned by this chat; it is removed when the chat is archived.
- Installed and launched the debug APK successfully, then updated to the final APK. Settings opens full screen from the global gear. Light and forced-dark screens were visually checked. Dark appearance and sound-off persisted through force-stop/restart; the final build displays correct light system-bar icons on dark backgrounds. Light overrides system night mode; selecting System follows night mode. Returned to System/light, English, normal text scale, sound on for review.
- Bundled Fraunces licence dialog opened and rendered readable scrollable text. Spanish Settings at 200% text size showed wrapped choices and sound copy with scrollable credits. English/normal scale was restored.
- The emulator plugin's Android CLI UI hierarchy inspection repeatedly failed; fresh screenshots plus coordinate taps verified these flows. Diagnostics reported the device running and the stream active. This is manual visual validation, not a completed automated UI suite.
- Sound-event emission and suppression/replay rules are unit-tested; audible output/volume quality has not been confirmed by listening. Complete TalkBack and wide-screen audits remain Stage 9.
- Host API health was verified outside the sandbox: `http://127.0.0.1:3000/health` returned `{"status":"ok","movies":9919}`. The shared API was left running; it was not stopped for a repeated catalog-failure test during simultaneous Stage 7 work. Stage 8 does not alter catalog networking or persist catalog data.

The emulator remains running on Settings for owner review. Stages 7 and 8 remain uncommitted pending approval.

### Physical phone install — 2026-11-17

At the owner's request, built the current Stage 7/8 debug app with `./gradlew :app:assembleDebug -Plumio.apiBaseUrl=http://127.0.0.1:3000/` using JDK 21 (passed), reapplied phone-scoped ADB reverse port 3000, and installed on the connected Pixel 3a. Installation returned Success; `am start` reported `Status: ok` for `dev.master.lumio/.MainActivity`. A fresh screenshot confirmed Settings rendered on the physical phone. No app data was cleared, ratings edited, or phone appearance/locale settings changed. Host API health verified: 9,919 movies. The app and API were left running.


### Stages 7 and 8 approval — 2026-11-17

The owner approved the current Poster Puzzle (including its poster-only correction) and Settings for commit and push to the existing `origin/main`. Final combined build, JVM tests and lint were rerun before commit; results remain 96 passing JVM tests, zero failures and zero lint errors (39 warnings). Source, tests, relevant documentation and the Stage 7 screenshot ignore rule are included. Unrelated exploratory branding, screen exports and Trivia design previews remain outside this commit, as do all catalog/poster/trivia data, machine configuration and secrets. The owner subsequently said “don't do stage 9”; no Stage 9 work was started.

## Owner-requested discovery and Arcade refinements — 2026-11-17

Status: implemented for review, not approved for commit/push. This work follows the owner's voice review, not Stage 9. See `docs/ANDROID-REFINEMENTS.md` for the requested scope, design provenance and resulting behavior.

### Automated validation

Final JDK 21 / SDK 36 wrapper command: `cd android && ./gradlew --init-script /tmp/lumio-refinements-build.gradle --no-build-cache clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` — **BUILD SUCCESSFUL** in 1m 9s, 98 JVM tests, zero failures/errors; lint zero errors and 39 warnings. The temporary, untracked init script changes only each project’s build output to `/private/tmp/lumio-refinements-build/<project-name>` to avoid recurring duplicate intermediates in the normal output. No repository build configuration or dependency was changed. Test-result XML totals and the lint text report in that temporary output were inspected. The verified APK was copied back to the normal `android/app/build/outputs/apk/debug/app-debug.apk` path, with matching SHA-256 digests. An earlier ordinary wrapper clean build also passed in 9 seconds using available caches.

Added domain coverage for unsolved permutations, exact drag/tap equivalence, invalid/same-position no-ops and solvability at all three sizes. Added controller coverage for difficulty reset/retention and moves blocked until artwork is ready, while the reference is visible, and after artwork failure. Explore tests now assert newest-first defaults while verifying explicit alphabetical sorting and restoration. Existing recommendation/UNSEEN, ratings/Undo, Trivia timing/lifeline/scoring and settings tests pass unchanged.

All **201** string keys and positional format placeholders align across English, Spanish, German and Portuguese; XML parses without duplicate keys. `git diff --check` passes. No catalog/trivia/poster downloads, generated catalog assets, machine settings, signing material or secrets were added to tracked source.

Validation recovery: the first wrapper invocation could not access the sandboxed Gradle cache, so the wrapper ran through authorized escalation. A compile check caught a misplaced composable invocation and a wrong GenericShape import; both were corrected before successful validation. An older test assumed the old default sort; it now tests a deliberate alternate selection. A later rebuild found duplicate generated `MovieCardsKt 2.dex` / `PuzzleScreenKt 2.dex` files in ignored intermediates; the wrapper clean removed them and the final build passed without a source workaround. A subsequent generated-resource parse failure involved duplicate `values-pt 2.xml` and `font/* 2.ttf` files in ignored intermediates. That failing invocation was cancelled; the final build used fresh output outside Documents and disabled the build cache. The final source also retains the original opaque selection checkmark on puzzle tiles. Unstripped native-library and SDK XML-version notices remain.

### Device checks

- Installed the debug APK on chat-owned read-only `Lumio_Stage8`, API 36, `emulator-5600`. Explore displayed **Newest first** and the newest-year spotlight. Swipe displayed a complete supplied poster in its neutral Fit frame with equal neutral rating buttons. Like advanced the queue and enabled Undo; Undo and rating actions were exercised. Existing persistence/undo regression tests remained green.
- Genie displayed exactly one movie and its actual reasons. Next changed pick 1 to pick 2 and changed the displayed title/poster. The final revision shortens the heading area and replaces the old “up to 30 films” header with “Chosen for you.” Scoring and exclusions were not changed. The finite end/restart branch is implemented but was not walked through all 30 picks on device.
- Trivia welcome artwork and the game screen were visually checked. Visual QA caught an image sizing overlap; explicit artwork/frame heights resolved it. Started a round, used the top-positioned 50:50, answered, and observed automatic progression to question 2 with 100 points and streak 1. Controller tests cover timed reveal, pause/resume, hold, results and replay; the entire redesigned results/replay flow was not separately completed on device. The welcome capture contains only generated decorative artwork and is saved at `design/previews/assets/trivia/android-welcome.png`.
- A direct horizontal Puzzle drag visibly swapped exactly two pieces, changed Swaps from 0 to 1 and updated the in-place count. Switched to 4×4 and 6×6, with board/reset state confirmed. The final correction uses approved Coral/Ink for selected difficulty rather than a Material default container color. Real loaded supplied posters were used; no procedural puzzle pattern was introduced.
- That emulator session later ended. Diagnostics showed no pinned device; restarting the old AVD timed out. Created temporary **Lumio_Refinements**, Small Phone/API 36, `emulator-5600`, owned by this chat and deleted on chat archive. Installed the final clean APK successfully. Its compact layout rendered a loaded poster, Coral-selected sizes and 36-piece board. Scroll reached the reference and Next controls. Board gestures capture piece movement; on compact screens scroll using the surrounding page/margins to bring the full board into view.
- Applied German app locale, system dark mode and 150% text on the Small Phone. Confirmed localized instructions/reset guidance, selected 6×6, wrapping progress/move text and the dark palette. Restored English, light mode and 100% text afterward. This is a scoped visual check, not full coverage of every screen/locale or TalkBack.
- The emulator plugin's ordinary Android CLI hierarchy reader intermittently failed. Diagnostics found no device/stream issue. Screenshots, alternate Minimap selectors and plugin-scoped raw input verified the flows. Some changing Trivia views could not settle for route learning; screenshots and the subsequent state confirmed action outcomes. No connected physical phone was modified.

### API, artifacts and limits

Initial outside-sandbox API health returned `{"status":"ok","movies":9919}`. Later the process was no longer listening. Restarted with `npm --prefix /Users/firt/Dev/Master.dev/MovieApp/api start`, reusing the existing ignored catalog without modifying it, and verified the same healthy count. The API is left running at **http://127.0.0.1:3000** (health: `/health`); the debug app uses `http://10.0.2.2:3000/`.

The Small Phone emulator is left running for review. Captures with supplied artwork/questions remain ignored under `validation/screenshots/stage-3/refinements/`, `stage-5/refinements/`, `stage-6/refinements/` and `stage-7/refinements/`. The generated-art-only welcome capture is suitable for the design notes. No Room instrumentation test, physical-phone install, full accessibility pass, wide-screen audit or complete UI automation suite was run for this refinement set. These limits do not imply Stage 9 authorization. Inherited unrelated design explorations remain preserved; no commit or push has been made.


### Refinements approval and delivery — 2026-11-18

The owner reviewed the completed refinement set and explicitly requested commit and push to main. The implementation, aligned strings, tests, generated Trivia decoration, native welcome capture, relevant original editable/standalone Trivia references and documentation are included. The two alternative browser directions remain exploratory. Saved final wrapper validation remains 98 passing JVM tests and lint zero errors/39 warnings; no source change was made after that validation, only approval/delivery documentation. `git diff --check` passed before commit.

The original main checkout also contains unrelated slide edits and branding/screen explorations. Those remain outside this commit. Overlapping Trivia drafts were verified identical to the approved references, and the full original design-index/preview notes are preserved in the committed versions. Main integration uses a fast-forward; no baseline branch is changed and no force push is used.
