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
