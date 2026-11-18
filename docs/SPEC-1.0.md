# Lumio 1.0 — product specification

Status: **source of truth for Lumio 1.0**, closed by the owner on 2026-11-18 for Android and iOS.

This document describes what Lumio 1.0 does on both platforms. It supersedes the UX brief in [README.md](../README.md) and the stage tables in [ANDROID-PLAN.md](ANDROID-PLAN.md), [ANDROID-REFINEMENTS.md](ANDROID-REFINEMENTS.md) and [IOS-PLAN.md](IOS-PLAN.md) wherever they differ. Those files remain the delivery history; platform READMEs remain the build and architecture guides.

## How to use this spec

- A change to any **MUST** rule below is a product change. It needs owner approval, then an update to this document in the same commit as the code, on both platforms unless the change is marked platform-specific.
- Keywords: **MUST** is required 1.0 behavior; **SHOULD** is the intended behavior with known gaps; **Not in 1.0** is explicitly out of scope.
- Exact colors, type and icons come from [COLORS-TYPOGRAPHY.md](../design/branding/COLORS-TYPOGRAPHY.md) and [ICONS.md](../design/branding/ICONS.md). Brand decisions come from [DECISIONS.md](../design/branding/DECISIONS.md). Mockups and previews never override those files.
- Verified test results live in [validation/android.md](../validation/android.md) and [validation/ios.md](../validation/ios.md), not here.

## 1. Product summary

Lumio is a film discovery app. People explore a catalog, record whether they like, dislike or have not seen films, get personal recommendations, and play two cinema games. It has no accounts and no server-side personal data.

| Item | Android | iOS |
| --- | --- | --- |
| Stack | Kotlin, Jetpack Compose, Material 3 | Swift, SwiftUI |
| Identifier | `dev.master.lumio` | `dev.master.lumio` (development bundle ID) |
| Supported OS | minSdk 26, compile/target SDK 36 | iOS and iPadOS 17 and later |
| Devices | Phones; adaptive bar or rail on wider screens | iPhone and iPad |
| Ratings storage | Room | SwiftData |
| Settings storage | Preferences DataStore | UserDefaults |
| Interface languages | English, Spanish, German, Portuguese | English, Spanish, German, Portuguese |

The two apps are independent native implementations. They share product behavior, not code.

## 2. Global rules

### 2.1 Catalog access: online only

- The catalog comes from the read-only Lumio API (see §10). The apps MUST NOT bundle catalog data or write it to disk: no movie database, HTTP disk cache, catalog file or poster cache.
- Explore, Swipe and Movie details request pages or single records. Genie, Trivia and Poster Puzzle share one full `/catalog` download, loaded lazily into process memory on first use. Concurrent requests share one load. A failed load can be retried.
- When catalog access fails, the screen MUST show an error with **Retry**. There is no offline catalog mode. This replaces the offline paragraph of the original UX brief.

### 2.2 What stays on the device

Only these are persisted, and they MUST survive app restart and app updates:

1. **Choices:** one rating per film (`LIKE`, `DISLIKE` or `UNSEEN`) with its saved time.
2. **Film snapshots** for rated films: id, title, year, poster URL, genres, director and cast. History and scoring use these.
3. **Undo log:** a shared log of choice changes, at most **200** entries.
4. **Settings:** appearance and game sound.

Catalog pages, trivia questions, poster images, game boards and scores live in process memory only. Active games survive navigation and configuration changes but intentionally reset when the process ends.

### 2.3 Data honesty

- Fields with the sentinel values `rating 0`, `runtime 0`, empty `director` or empty `cast` are missing data. Screens MUST hide them and never invent values.
- Poster URLs MUST be used exactly as supplied. A missing or failed poster shows a procedural fallback: a neutral panel colored from a hash of the film id, with the title. **Poster Puzzle is the exception** (§8.2).
- The app MUST NOT fabricate artwork, ratings, match percentages or film facts.
- Catalog text (titles, synopses, genres, names, trivia) stays in its source language. Only interface strings are localized.
- Source and dataset attribution MUST remain reachable: per film from Movie details, and globally from Settings without a connection. See [data/SOURCE-ATTRIBUTION.md](../data/SOURCE-ATTRIBUTION.md).

### 2.4 Ratings and Undo

- Choosing a value saves the choice and its film snapshot atomically, together with an undo entry that holds the previous value and time (or "none" for a first choice).
- Saving the value a film already has is a no-op and creates no undo entry.
- **Undo** restores the previous value and time, or removes a first rating.
- **Global Undo** (Swipe and the Movie details confirmation) reverts the latest remaining change across all films.
- **Per-film Undo** (Your choices) reverts only that film's latest change. It validates that the displayed entry is still current, and leaves other films' entries in order.
- Both kinds share the 200-entry log. When a film's entries have aged out, its Undo is unavailable. Undo has no time limit.
- `LIKE` and `DISLIKE` affect recommendations. `UNSEEN` is saved, shown in history and excluded from Swipe and Genie, but it adds nothing to scoring.

## 3. Navigation

- Four main destinations: **Explore, Swipe, Genie, Arcade**. Each keeps its own navigation stack and position when the user switches away and back.
- Compact screens use a bottom navigation bar. Wide screens use a navigation rail (Android) or sidebar (iPad).
- **Settings** is reachable from every destination and game, and returns to where the user was.
- Movie details, Your choices, Trivia, Poster Puzzle and Settings are full-screen routes with clear back navigation to their origin. Android system Back follows the same paths.
- Selected navigation items use the outline icon in Ink on a Coral indicator, with a visible label.
- **iOS only:** on iOS 26 and later, the floating compact navigation bar, system toolbar controls and the Explore sort control use Liquid Glass. Reading surfaces stay opaque and use the Editorial palette. iOS 17 and 18 keep solid controls.

## 4. Explore

- **Search** by title, director or cast, debounced by 300 ms.
- **Genre filter** chips loaded from `/genres`. A genre-list failure MUST NOT block movies.
- **Six sort orders:** Newest first (default), Oldest first, Title A–Z, Title Z–A, Highest rated, Most popular.
- The default is **Newest first** (`year desc`). It also applies after Reset filters and when a saved sort is missing or invalid. A valid saved sort is restored.
- **Spotlight:** the first film of the current results, shown large. It is not personalized. The grid still includes it.
- **Poster grid** in pages of **24** with **Load more**.
- A new search, genre or sort cancels the earlier request and resets paging. Stale responses MUST NOT replace newer results.
- A failed next page keeps the loaded cards and offers Retry.
- States: loading, results, empty (with Reset filters), error with Retry.
- Selecting the Spotlight or a card opens Movie details.

## 5. Movie details

- Loaded online from `/movies/{id}`.
- Shows the poster or fallback, title, year · runtime · genres, synopsis and an information list: release year, genres, runtime, director, cast, and source rating with vote count. Missing fields are hidden.
- **View source** and dataset links open the supplied URLs.
- Three choices: **Like, Dislike, Not seen yet**. The current choice is shown. Every accepted change shows a confirmation with **Undo** (global Undo, §2.4).
- Wide layouts use two columns.
- States: loading, film not found, request error with Retry, failed save.

## 6. Swipe and Your choices

### 6.1 Swipe

- One card at a time from the catalog ordered by popularity (descending), in pages of 24.
- Every film with a saved choice, including `UNSEEN`, is excluded. Fully rated pages are skipped automatically.
- The card shows the **entire supplied poster** fitted inside a bounded neutral frame, at any aspect ratio.
- **Gestures:** right = Like, left = Dislike, down = Not seen yet. Only a clear drag past the threshold along the dominant axis commits. Short, upward or ambiguous diagonal drags do nothing.
- **Buttons:** Like, Dislike and Not seen yet perform the same actions. All three MUST have equal, neutral outlined styling; none appears pre-selected.
- The card advances only after the choice is saved.
- **Undo** reverts the latest remaining change across the app and returns that film to the queue when it becomes eligible again.
- Choices and Undo made in Movie details or Your choices update the queue.
- Tapping the card opens Movie details. A toolbar action opens **Your choices**.
- Accessibility services expose the three choices as card actions as well as buttons.
- With reduced motion (Android animator scale 0, iOS Reduce Motion), drag transforms and decorative motion are removed.
- States: loading, card, caught up (with Refresh), network error with Retry, saved-choice read error with Retry, failed save.

### 6.2 Your choices (history)

- Opens from Swipe and Genie.
- Lists saved snapshots **newest first**, with poster or fallback, title, current choice and saved time.
- Works with no catalog connection: viewing, changing a choice and per-film Undo all use local data. Opening full Movie details still needs the API.
- Each row can change the choice (three-choice editor) and **Undo** that film's latest change (§2.4).
- States: loading, empty, local read error with Retry, failed write, stale Undo.

## 7. Genie (recommendations)

### 7.1 Scoring

- Feature weights from each saved choice: **genre 1, director 3, actor 2**. `LIKE` adds the weight; `DISLIKE` subtracts the same amount. `UNSEEN` contributes nothing.
- Features are trimmed and case-normalized. Blank features are ignored. A repeated feature counts once per film.
- A candidate's score is the sum of the affinities of its features.
- **Every** saved film, including `UNSEEN`, is excluded.
- Only films with a **positive** net score are recommended. Keep the top **30**, ordered by score, then popularity, normalized title and id.
- Missing metadata is ignored, never guessed. Most catalog films lack director and cast, so reasons are mostly genres.

### 7.2 Experience

- **One recommendation at a time** with poster, title, and up to **three** genuine positive reasons that name the matching genre, director or actor.
- A finite **position count** (for example 3 of 30) and **Next**. Next does not rate or dismiss the film.
- After the last pick, offer an explicit **restart** and guidance to record more choices.
- The current pick survives navigation and recalculation while it remains eligible. If it leaves the set, show the first current pick.
- The internal score is never shown as a rating or percentage.
- Choice changes elsewhere recalculate recommendations in the background. Stale results are discarded.
- Opens Movie details and Your choices.
- States: **getting started** when there is no Like or Dislike (even if Unseen choices exist; the catalog is not downloaded), loading, no positive matches, network error with Retry, local read error with Retry.
- Genie MUST NOT become a long scrolling list.

## 8. Arcade

The Arcade hub offers two games: **Trivia** and **Poster Puzzle**. Both return to the hub. Game actions never change movie ratings.

### 8.1 Trivia

Visual direction: the approved native **Golden Ticket** adaptation. The other two preview directions are not part of 1.0.

- **Welcome:** a decorative ticket illustration and an explicit **Let's play** start. The illustration is generated decoration, not a catalog poster or launcher art ([provenance](../design/previews/assets/trivia/README.md)).
- **Round:** **5** questions, sampled without repeats from validated catalog questions. A valid question has an id, a non-blank prompt, four distinct non-blank options and a valid answer index. Fewer than five valid questions shows an unavailable state.
- **Timer:** **20 seconds** per question, measured with a monotonic clock. A tap after the deadline cannot score.
- **Layout:** segmented round progress, prominent score and streak, a notched ticket question panel, animated question and answer transitions, check/cross feedback and encouraging copy.
- **50:50:** one per round, placed **beside the question counter**. It removes two wrong options.
- **Scoring:** a correct answer earns **100** points plus **25** for each consecutive correct answer after the first, capped at **100** extra. Wrong answers and timeouts earn 0 and reset the streak. A perfect round scores **750**.
- **Feedback:** shows the correct answer and points earned, then advances automatically after **4 seconds**. **Keep reading** holds the feedback. **Next** (or **See results** on the last question) advances manually. The view scrolls to the feedback and back to the next question.
- **Pause:** Pause **hides the question**. Leaving the screen, opening Settings or backgrounding the app pauses the round. Only an explicit **Resume** continues.
- **Results:** score, correct count and best streak, with **Play again** (fresh sample, cleared score, streak and lifeline) and **Back to Arcade**.
- Questions stay in English (source language).
- States: loading, network error with Retry, not enough questions.

### 8.2 Poster Puzzle

- **Sizes:** 3×3, 4×4 and 6×6 (9, 16 and 36 pieces).
- **Artwork rule:** play only uses a supplied poster with a valid HTTP(S) URL that has **successfully loaded**. Missing or invalid URLs are skipped. If artwork fails, moves and the reference are blocked, and **Retry** and **Next puzzle** are offered. A generated pattern or fallback MUST NEVER become puzzle artwork.
- **Shuffle:** every new board starts unsolved.
- **Drag:** dragging a piece onto another swaps exactly those two and counts **one move**. The piece follows the finger and the target is highlighted. Cancelled, outside-the-board and same-position drops do nothing.
- **Two-tap:** tap one piece, then another, to swap them. Tapping the selected piece again cancels.
- **Progress:** pieces in their correct place and number of moves. Progress can go down.
- **Reference:** show or hide the full poster with the same crop as the tiles. Showing it clears the selection.
- **Completion:** locks the board, shows completion feedback and offers **Next puzzle** and **Back to Arcade**. There is no timer.
- **Changing size** reshuffles the same poster and resets progress, moves, selection and reference.
- **Next puzzle** keeps the chosen size for the live process and avoids repeating the previous film when another poster is available.
- On compact screens the page scrolls around the board. Drags inside the board move pieces.
- A **View details** action opens the film's Movie details.
- States: loading catalog, catalog error with Retry, no usable posters, loading artwork, artwork error, ready.

## 9. Settings

- **Appearance:** System (default), Light, Dark. Changes apply immediately everywhere without resetting navigation or game state.
- **Game sound:** on by default. It covers Trivia correct, incorrect and timeout cues, and Puzzle swap and completion cues. A cue plays once and is never replayed when returning to a game. Android uses media volume; iOS uses an ambient audio session that respects the silent switch.
- **Credits**, available without a connection: bundled Fraunces and DM Sans OFL licences, the Lucide licence, catalog attribution and source links.
- States: settings read error with Retry, failed write that keeps the previous value.

## 10. Catalog API contract

The apps depend only on these read-only GET routes. The API never receives personal data.

| Route | Used by |
| --- | --- |
| `/movies` with `q`, `genre`, `sort`, `order`, `limit`, `offset` | Explore, Swipe |
| `/movies/{id}` | Movie details |
| `/genres` | Explore filters |
| `/catalog` (movies and trivia) | Genie, Trivia, Poster Puzzle |
| `/health` | iOS connection check, development |

| Build | Base URL |
| --- | --- |
| Android debug, emulator | `http://10.0.2.2:3000/` (cleartext allowed in debug only) |
| Android debug, USB phone | `http://127.0.0.1:3000/` with `adb reverse` and `-Plumio.apiBaseUrl` |
| iOS debug, Simulator | `http://127.0.0.1:3000` (local networking allowed in Debug only) |
| iOS debug, device | `LUMIO_API_BASE_URL` set to the Mac's LAN address |
| Release, both | `https://movie-api-delta-six.vercel.app/` (HTTPS only) |

Run the local API with `npm --prefix api start` (Node.js 22+); see [README.md](../README.md#set-up-and-run-the-local-api).

## 11. Design, localization and accessibility

- **Brand:** Lumio, Taste Ticket, variation C — Editorial ([DECISIONS.md](../design/branding/DECISIONS.md)).
- **Palette:** Editorial light and dark. Core colors are Paper `#F7F2E8`, Ink `#242320` and Coral `#F47B65`, with Night `#1D1C1A` and Charcoal `#292723` for dark surfaces. Exact tokens are in [COLORS-TYPOGRAPHY.md](../design/branding/COLORS-TYPOGRAPHY.md).
- **Type:** Fraunces for headings and film titles, DM Sans for body text and controls. Both fonts are bundled.
- **Icons:** the 29 pinned Lucide outline icons (24-unit canvas, 2-unit stroke, round caps and joins). No Material or SF Symbols replacements for mapped icons.
- **App icon:** interim raster derived from the approved C symbol. Not final production artwork.
- **Localization:** every interface string MUST exist in English, Spanish, German and Portuguese, with matching keys and format placeholders. Any new string is added to all four in the same change.
- **Accessibility:** touch targets of at least 48 dp / 44 pt, labelled controls, card and puzzle-tile accessibility actions and labels, support for large text (Android font scale, iOS Dynamic Type) and reduced motion. These SHOULD hold everywhere; see §13 for what has not been audited.

## 12. Not in 1.0

- Offline catalog, catalog cache or bundled catalog data.
- Accounts, sign-in, sync, cloud backup or uploading ratings. Android excludes the ratings database from backup and device transfer.
- Saved game progress, high scores or game history after the process ends.
- Translated catalog content or trivia.
- The Cinema Spotlight and Arcade Rush Trivia directions.
- Production vector logo, final launcher or adaptive icons, App Store or Play Store listings, release signing.
- Android Stage 9 / iOS optional hardening (full accessibility, wide-screen and regression audit).

## 13. Known gaps at 1.0

These are recorded limitations, not features:

- No complete TalkBack or VoiceOver pass, wide-screen audit or iPad multitasking audit has been done.
- iOS 17 runtime and physical iPhone/iPad builds are untested; signing is not configured.
- Android refinements were not reinstalled on a physical phone, and the Room instrumentation test was not rerun after them.
- Game sounds still need a listening check.
- Validation of iOS Settings and game sounds (iOS Stage 8) is not yet recorded in [validation/ios.md](../validation/ios.md).
- Availability and trademark checks for the Lumio name have not been done.

## 14. Change history

| Date | Change |
| --- | --- |
| 2026-11-18 | 1.0 closed for Android and iOS; this specification created as the source of truth. |
