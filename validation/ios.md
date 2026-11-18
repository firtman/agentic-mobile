# iOS validation

## Stage 0 · Foundation · 2026-11-18

- Built Debug for Simulator with Xcode 27.2 and the iOS 27.2 SDK, deployment target 17.0. Built Release for generic iOS Simulator and checked the built Info.plist for the HTTPS testing URL. Release omits the Debug local-network transport exception.
- iPhone 16, iOS 18.4. Six XCTest cases passed. The UI suite verified the live catalog connection, Retry against an unavailable endpoint, and navigation through Swipe, Genie, and Arcade with Settings returning to each destination in German at an accessibility text size. Visually checked Explore in light appearance.
- iPad Pro 11-inch (M4), iPadOS 18.4. The first run found the sidebar hidden by default in portrait. Set the wide split view to show both columns and reran the suite. The final run passed all six cases on each Simulator. Visually checked the iPad sidebar and Explore in dark appearance with German and accessibility-size text.
- The live local API at http://127.0.0.1:3000/health returned status ok and 9,919 movies. The local server was left running.
- Verified the bundle includes both variable fonts, the four licence/attribution texts, and all four localizations. All 10 UI string keys align. All 29 Lucide vectors match the pinned geometry, with black strokes for templating. The interim icon is an opaque 1024 × 1024 PNG derived from the approved C raster.
- Physical device, signing, iOS 17 runtime, complete VoiceOver and iPad multitasking audits remain untested. This stage verifies the app shell and connection check. Feature validation begins with the corresponding later stage.
