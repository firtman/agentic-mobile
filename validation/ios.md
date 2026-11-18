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

## Stage 3 · Swipe · 2026-11-18

- Built Debug with Xcode 27.2. All 19 XCTest cases passed on both iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4) with the final layout. The live API at http://127.0.0.1:3000/health returned status ok and 9,919 movies.
- Six synthetic-fixture model tests covered gesture direction and threshold, popularity-ordered paging, skipping fully rated pages including Unseen, advancing after a choice, global Undo returning the restored film, choices and Undo made outside Swipe updating the queue, exhaustion then Undo, and network failure with Retry of the same page.
- The live UI test, using an isolated memory store, rated with the Like button, a left swipe and a downward drag. Undo then restored the latest film. The test opened its details and saved Not seen yet there, and the film left the Swipe queue after returning.
- Visually verified light Swipe on the live iPhone 18 Pro tab without changing its saved choices. All content fits above the floating navigation. German dark mode on iPhone 16 (iOS 18.4) with an isolated store first showed a truncated hint and misaligned action icons. After the fixes, the hint wraps, the icons align, and the page scrolls. A downward drag on the card saved Not seen yet instead of scrolling.
- All 60 localization keys align across English, Spanish, German, and Portuguese. Physical device, iOS 17 runtime, full VoiceOver (card actions are implemented but were not exercised with VoiceOver), large accessibility text on Swipe, an injected storage failure, and the exhausted state against the live catalog were outside this run.

## Stage 4 · Your choices · 2026-11-18

- Built Debug with Xcode 27.2. All 22 XCTest cases passed on iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4).
- Live UI tests saved a film from Swipe, changed the choice in history, undid the edit, opened the same film’s details, and undid the first choice. A separate UI test opened the empty history from Genie with the catalog endpoint unavailable. Both used isolated in-memory ratings.
- The SwiftData regression test used an injected clock to verify newest-first ordering, stale action guards, timestamp restoration, and per-film Undo leaving the newest remaining global Undo entry intact. The existing persistence and 200-entry cap tests passed.
- All 70 localization keys align across English, Spanish, German, and Portuguese. This run did not exercise a populated offline UI with connection loss, injected storage failure, or a complete accessibility audit. Physical device and iOS 17 runtime coverage remain unverified.

## Stage 5 · Genie · 2026-11-18

- Built Debug with Xcode 27.2. All 27 XCTest cases passed on iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4).
- Live UI tests opened Genie without preferences, used Start swiping, saved a like in an isolated ratings store, and verified a real recommendation and reason. Next advanced the position, and returning from Movie Details kept the current film.
- Synthetic tests checked signed feature weights, blank and duplicate features, saved-film exclusion including Unseen, positive-net ranking, reason order, tie-breaks, and the 30-film cap. They also covered the Unseen-only fast path, finite navigation and restart, selected-film retention and removal after recomputation, catalog failure/retry, and shared memory cache reuse.
- All 86 localization keys align across English, Spanish, German, and Portuguese. The suite did not walk through all 30 live recommendations or inject a SwiftData failure in Genie. Physical device, iOS 17 runtime, and full accessibility coverage remain unverified.

## Stage 6 · Arcade and Trivia · 2026-11-18

- Built Debug with Xcode 27.2 and tested on iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4). The full suite passed 31 of 32 cases on each device. The remaining live test found that the pause panel’s accessibility identifier overrode Resume’s identifier. After adjusting it, the final focused run passed all five Trivia cases on both simulators.
- The live UI test covered the Arcade hub, explicit start with catalog questions, leaving and resuming a hidden question, the single 50:50, and automatic advancement after feedback. The clock-driven tests covered five valid distinct questions, the 20-second deadline, late taps, four-second feedback, holding, wrong-answer and timeout resets, 750-point perfect rounds, and replay.
- All 120 string keys align across English, Spanish, German, and Portuguese. The bundled illustration matches the approved Golden Ticket artwork. The full live results/replay path, backgrounding through the Home gesture, Trivia-specific network failure, physical devices, and full accessibility checks were outside this run.
- Visually verified the Arcade cards and the Golden Ticket welcome screen in the live Codex iPhone 18 Pro tab.

## Stage 7 · Poster Puzzle · 2026-11-18

- Built Debug with Xcode 27.2. All 35 XCTest cases passed on iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch (M4, iPadOS 18.4).
- The live UI test swapped exactly two pieces with a drag, reversed the move with two taps, retained the board through Arcade navigation, and exercised all three sizes. It also checked the reference image, the move reset, and retaining 6×6 on the next poster.
- Synthetic tests checked unsolved shuffles, completion and move locking, same-position and invalid swaps, URL eligibility, failed-artwork blocking, retrying the same supplied poster, and avoiding an immediate repeat when another poster is available.
- All 138 localizable UI keys align across English, Spanish, German, and Portuguese. No poster image or catalog data was added to the repository. The full VoiceOver flow, physical devices, iOS 17 runtime, and gesture cancellation during a live drag were outside this run.

## Stage 8 · Settings · 2026-11-18

- The final Debug run passed all 27 selected tests on each simulator, iPhone 18 Pro (iOS 27.2) and iPad Pro 11-inch M4 (iPadOS 18.4). It included every unit test plus the Settings, localized navigation, and live Trivia UI checks.
- With the catalog deliberately unreachable, both devices changed appearance and sound, relaunched, verified both saved choices, and opened all four bundled credit notices. German navigation with accessibility-sized text returned to each original destination. During a live Trivia round, changing appearance paused play and Resume returned to the same question.
- The first Settings UI run found the appearance picker exposed an empty accessibility value. The final control gives each option a selected trait and a visible checkmark. The final tests verified the selected option after relaunch.
- Game-model tests verified single feedback events for correct answers, errors, timeouts, and completed swaps, including the final completion cue. Tile selection and invalid swaps emitted no cue. All five original PCM tones and four text notices were present in the built app. Physical-device sound quality, silent-switch behavior, and iOS 17 runtime coverage remain unverified.
- Checked 149 localizable keys and format placeholders across English, Spanish, German, and Portuguese. The live catalog health endpoint returned 9,919 movies during validation.

### Performance fixes — 2026-11-18 (owner approved for commit)

Scope: poster cache raised to 96 MB with display-sized decodes (240/480/720 px), one shared download per poster and no fallback flash on a cache hit; Swipe skips saved films with one larger request (up to the API's 500 limit), warms the next poster and applies its own save without re-reading every choice; game sound configures the audio session once and prepares players.

Results on iPhone 18 Pro Simulator (iOS 27.2) with the local API: 24 unit tests passed; UI tests `SwipeTests`, `ShellTests`, `PosterPuzzleFlowTests` and `TriviaFlowTests` passed (7 tests); Explore posters confirmed rendering in a simulator screenshot. Limits: the remaining UI suites were not rerun; no physical device, Instruments profile, listening check or before/after timing.
