# Lumio Android

Stage 0 foundation for the [owner's plan](../docs/ANDROID-PLAN.md). Native Kotlin, Compose and Material 3; namespace/applicationId `dev.master.lumio`; min/compile/target SDK 26/36/36. Four main destinations and Settings are placeholders. Explore currently checks `/health`, with loading, connection error and Retry; the real movie browser arrives in stage 1.

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
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android emulator. Debug requests use `http://10.0.2.2:3000/`; only this host is allowed cleartext access by the debug-only network security configuration. Release uses `https://movie-api-delta-six.vercel.app/` with cleartext disabled. `./gradlew :app:assembleRelease` produces an unsigned release APK; production signing is not configured.

## Data and architecture

`LumioApplication` owns a small manual container. `CatalogApi` models health, paged movies, details, genres and catalog responses. `CatalogRepository` lazily loads the full catalog once into process memory with concurrent requests serialized; failed loads can be retried. Its large response uses a dedicated stream decoder before Retrofit's normal converter. No HTTP disk cache, catalog asset, catalog database or catalog file is used.

Ratings/undo persistence and settings persistence belong to later stages. Catalog text is not translated. Missing metadata remains empty/zero in models for screens to hide; poster/source URLs are preserved verbatim. Source provenance remains documented in [SOURCE-ATTRIBUTION.md](../data/SOURCE-ATTRIBUTION.md).

Navigation Compose uses serializable routes and saved destination state. The adaptive navigation suite selects a bottom bar or rail; Settings opens full screen and Back restores the previous destination. Theme follows system appearance in stage 0. Bundled typefaces and 29 converted Lucide icons follow the approved specifications; the launcher uses the approved raster symbol as an **interim** asset. See [asset provenance](ASSETS.md).

## Dependency pins

Dependencies are pinned in `gradle/libs.versions.toml`. SDK 36 is an owner requirement; the project therefore uses AGP 8.13.2 / Gradle 8.13 and a compatible Compose BOM 2025.12.00 / Kotlin 2.2.21 set rather than the SDK 37 template. Newer releases reported by lint are recorded, not applied automatically. The current Android CLI template was inspected, and official guidance was consulted at scaffold time:

- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [Compose compiler configuration](https://developer.android.com/develop/ui/compose/compiler)
- [Navigation releases](https://developer.android.com/jetpack/androidx/releases/navigation)
- [Retrofit 3.0.0 source](https://github.com/square/retrofit/tree/3.0.0)
- [Kotlin serialization 1.9.0](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0)

Review actual build, lint and emulator results in [validation/android.md](../validation/android.md). Stop after this stage for owner approval before committing/pushing or starting stage 1.
