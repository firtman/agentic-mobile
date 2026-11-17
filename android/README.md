# Lumio Android

Stage 1 Explore implementation on the approved foundation for the [owner's plan](../docs/ANDROID-PLAN.md). Native Kotlin, Compose and Material 3; namespace/applicationId `dev.master.lumio`; min/compile/target SDK 26/36/36. Explore now provides search by title/director/cast, genre filters, six sort orders, spotlight, and a 24-item paged poster grid. Stage 2 adds movie details and on-device ratings: grid cards and the spotlight open details, where Like / Dislike / Not seen yet are saved in Room with Undo. Swipe, Genie, Arcade and Settings remain placeholders.

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

Ratings, the undo log, and rated-movie snapshots are stored on the device in Room (`lumio.db`, excluded from backup and device transfer); the exported schema lives in `app/schemas/` and must be committed with schema changes. Settings persistence belongs to a later stage. Catalog text is not translated. Missing metadata remains empty/zero in models for screens to hide; poster/source URLs are preserved verbatim. Source provenance remains documented in [SOURCE-ATTRIBUTION.md](../data/SOURCE-ATTRIBUTION.md).

Navigation Compose uses serializable routes and saved destination state. The adaptive navigation suite selects a bottom bar or rail; Settings opens full screen and Back restores the previous destination. Theme follows system appearance in stage 0. Bundled typefaces and 29 converted Lucide icons follow the approved specifications; the launcher uses the approved raster symbol as an **interim** asset. See [asset provenance](ASSETS.md).

## Dependency pins

Dependencies are pinned in `gradle/libs.versions.toml`. SDK 36 is an owner requirement; the project therefore uses AGP 8.13.2 / Gradle 8.13 and a compatible Compose BOM 2025.12.00 / Kotlin 2.2.21 set rather than the SDK 37 template. Newer releases reported by lint are recorded, not applied automatically. The current Android CLI template was inspected, and official guidance was consulted at scaffold time:

- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [Compose compiler configuration](https://developer.android.com/develop/ui/compose/compiler)
- [Navigation releases](https://developer.android.com/jetpack/androidx/releases/navigation)
- [Retrofit 3.0.0 source](https://github.com/square/retrofit/tree/3.0.0)
- [Kotlin serialization 1.9.0](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0)

Review actual build, lint and emulator results in [validation/android.md](../validation/android.md). Stage 0 was approved and pushed as `9ddf76d`. Stage 1 was approved for commit and push on 2026-11-17. Stage 1 was pushed as `7b47f94`; stages 2 and 3 were approved for commit/push on 2026-11-17, and stage 4 was authorized.

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

Undo uses the latest persisted change across the app, survives process restart and reloads the queue online. It removes a first rating or restores a previous rating; a restored prior rating still excludes that movie. Details ratings update Swipe reactively, including returning a consumed film after details Undo. Network errors require Retry, local rating-read errors block the queue until Retry succeeds, and exhausted queues can refresh for new films. Undo has no timed expiry, subject to the ratings store’s 200-entry limit. With Android animator duration scale set to zero, drag transforms are disabled; no timed card transitions or fling animations are used. Stage 4 history is not implemented.
