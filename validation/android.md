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
