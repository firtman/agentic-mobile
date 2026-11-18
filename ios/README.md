# Lumio for iOS

Open Lumio.xcodeproj in Xcode and select the shared Lumio scheme. The native app targets iOS/iPadOS 17 and later. Simulator builds require no signing team. See the [implementation plan](../docs/IOS-PLAN.md) and [validation record](../validation/ios.md).

## Local development

From the repository root, verify the local API at http://127.0.0.1:3000/health. If it is down, run `npm --prefix api start`. Run `npm --prefix api run setup` first if the ignored catalog is missing. Debug uses the Mac’s loopback API in Simulator. Explore loads the live catalog with search, genres, sorting, and 24-film pages. If a request fails, use Retry to reload the catalog or the failed page.

For a physical device, select your local signing team and use `HOST=0.0.0.0 npm --prefix api start` on the Mac. Set the Debug app target’s `LUMIO_API_BASE_URL` build setting to the Mac’s LAN address and port. Keep this override local and allow local network access when prompted. Debug can also read the same named environment variable from the Xcode run scheme. Release uses the HTTPS testing endpoint. [Apple’s local networking transport setting](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsallowslocalnetworking)

## Movie details and choices

Open a film from the Spotlight or a poster card. Details are fetched from the catalog on demand. Like, Dislike, and Not seen yet are stored on-device in SwiftData with the rated film’s scoring and history metadata. An accepted change offers Undo. Undo restores the previous choice or removes a first choice. The shared local ledger retains up to 200 changes.

## Swipe

Swipe shows the most popular films that have no saved choice, one at a time, with the full supplied poster in a neutral frame. Drag right to like, left to dislike, or down for Not seen yet, or use the three equivalent buttons. VoiceOver exposes the same choices as card actions. Fully rated catalog pages are skipped, and choices made in Movie Details update the queue. Undo reverts the latest remaining change in the shared ledger and returns that film to the queue. Network, saved-choice loading, and save failures are shown separately with Retry. When every available film has a choice, Swipe shows a caught-up state with Refresh movies.

## Your choices

Open Your choices from the Swipe toolbar or Genie. The history shows saved snapshots and dates, newest first. Change a choice or undo a film’s latest available change without a catalog connection. Opening full Movie Details fetches the film from the catalog. Swipe’s global Undo and per-film Undo share the same 200-change ledger.

## Genie

Genie uses the shared local ratings ledger and fetches the full catalog into process memory when the user has a like or dislike. It presents one ranked movie at a time with matching reasons, a position indicator, and an explicit restart. The app keeps the current pick on return when that film is still eligible. Review or edit saved choices from the Genie toolbar.

## Tests

Run from the repository root with the local API running. Replace the destination with an installed iPhone or iPad Simulator.

```sh
xcodebuild -project ios/Lumio.xcodeproj -scheme Lumio \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro,OS=27.2' \
  -parallel-testing-enabled NO test
```

The URLProtocol unit tests use synthetic responses. The UI suite checks live Explore, empty search and filter reset, Retry with a deliberately unavailable debug endpoint, localized navigation, the Movie Details rating and Undo flow, and Swipe buttons, gestures, Undo, and details handoff. Ratings tests use an isolated store.

## Interface

On iOS 26 and later, Lumio uses Liquid Glass for the floating compact navigation bar, system toolbar controls, and Explore sorting. Editorial surfaces keep the approved palette, with solid controls and navigation on earlier iOS versions. [Apple’s Liquid Glass guidance](https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass)

## Assets

The two variable fonts and bundled licences are the reviewed copies listed in [Android asset provenance](../android/ASSETS.md). The Lucide asset catalog contains the 29 [pinned vectors](../design/branding/icons/lucide/) with their strokes set to black for template rendering. The interim universal app icon composites the approved [C symbol raster](../design/branding/taste-ticket/lumio-c-symbol-dark.png) on Editorial Paper. Film posters and catalog records come from the service. [Catalog attribution](../data/SOURCE-ATTRIBUTION.md)
