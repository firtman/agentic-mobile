# iOS validation

## Stage 0 · Foundation · 2026-11-18

- Built Debug for Simulator with Xcode 27.2 and the iOS 27.2 SDK, deployment target 17.0. Built Release for generic iOS Simulator and checked the built Info.plist for the HTTPS testing URL. Release omits the Debug local-network transport exception.
- iPhone 16, iOS 18.4. Six XCTest cases passed. The UI suite verified the live catalog connection, Retry against an unavailable endpoint, and navigation through Swipe, Genie, and Arcade with Settings returning to each destination in German at an accessibility text size. Visually checked Explore in light appearance.
- iPad Pro 11-inch (M4), iPadOS 18.4. The first run found the sidebar hidden by default in portrait. Set the wide split view to show both columns and reran the suite. The final run passed all six cases on each Simulator. Visually checked the iPad sidebar and Explore in dark appearance with German and accessibility-size text.
- The live local API at http://127.0.0.1:3000/health returned status ok and 9,919 movies. The local server was left running.
- Verified the bundle includes both variable fonts, the four licence/attribution texts, and all four localizations. All 10 UI string keys align. All 29 Lucide vectors match the pinned geometry, with black strokes for templating. The interim icon is an opaque 1024 × 1024 PNG derived from the approved C raster.
- Physical device, signing, iOS 17 runtime, complete VoiceOver and iPad multitasking audits remain untested. This stage verifies the app shell and connection check. Feature validation begins with the corresponding later stage.

## Stage 1 · Explore · 2026-11-18

- Built Debug with Xcode 27.2. Ran the nine-case XCTest suite on iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4). Eight cases passed on each device. The search test exposed a keyboard-focus issue. Expanded the search field tap target, changed the Spotlight to show portrait artwork, and reran the search, empty-state, and reset test successfully on both devices.
- The suite covered the live Spotlight, unavailable-endpoint Retry, preservation of existing cards after a next-page failure, and successful retry with deduplication. Model tests also verified valid restored sort, invalid fallback, and reset to newest first.
- Visually verified light Explore on the live iPhone 18 Pro Codex tab. Tapping Action changed the Spotlight, and All genres restored the default view. Verified German dark iPad Explore with accessibility-size text and supplied posters.
- All 28 string keys align across English, Spanish, German, and Portuguese. No film records or poster captures were added to the repository. Physical-device, iOS 17 runtime, and full VoiceOver coverage remain untested.

### Liquid Glass review

- The revised build passed all nine XCTest cases on iPhone 18 Pro (iOS 27.2) and all nine on iPad Pro 11-inch (M4, iPadOS 18.4).
- Verified the floating Liquid Glass navigation, system Settings control, and glass Sort menu on the live Codex tab. The menu displayed all six choices with Newest first selected. The iPadOS 18.4 run exercised the earlier-OS styling.

## Stage 2 · Movie details and ratings · 2026-11-18

- Built Debug with Xcode 27.2. All 12 XCTest cases passed on both iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4). The live UI test opened details from Spotlight and a grid card, saved Like and Dislike, restored Like with Undo, and verified the restored choice after returning to Explore and reopening the same film.
- The SwiftData tests used a disposable on-disk store to check reopening, Unseen, repeated-choice no-ops, stale Undo rejection, previous-value restoration, and undoing a first choice. A separate test verified the 200-entry cap. Simulator UI tests used an isolated memory store.
- All 52 localization keys align across English, Spanish, German, and Portuguese. Physical device, iOS 17 runtime, full VoiceOver, two-column landscape layout, and an injected details-specific request failure were outside this validation run. Existing catalog Retry checks passed on both simulators.
