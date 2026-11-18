# Lumio for iOS

Open Lumio.xcodeproj in Xcode and select the shared Lumio scheme. The native app targets iOS/iPadOS 17 and later. Simulator builds require no signing team. See the [implementation plan](../docs/IOS-PLAN.md) and [validation record](../validation/ios.md).

## Local development

From the repository root, verify the local API at http://127.0.0.1:3000/health. If it is down, run `npm --prefix api start`. Run `npm --prefix api run setup` first if the ignored catalog is missing. Debug uses the Mac’s loopback API in Simulator. The Explore placeholder displays the catalog connection result and offers Retry on failure.

For a physical device, select your local signing team and use `HOST=0.0.0.0 npm --prefix api start` on the Mac. Set the Debug app target’s `LUMIO_API_BASE_URL` build setting to the Mac’s LAN address and port. Keep this override local and allow local network access when prompted. Debug can also read the same named environment variable from the Xcode run scheme. Release uses the HTTPS testing endpoint. [Apple’s local networking transport setting](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsallowslocalnetworking)

## Tests

Run from the repository root with the local API running. Replace the destination with an installed iPhone or iPad Simulator.

```sh
xcodebuild -project ios/Lumio.xcodeproj -scheme Lumio \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.4' \
  -parallel-testing-enabled NO test
```

The URLProtocol unit tests use synthetic responses. The UI suite checks the live connection, a deliberately unavailable debug endpoint, and localized navigation.

## Assets

The two variable fonts and bundled licences are the reviewed copies listed in [Android asset provenance](../android/ASSETS.md). The Lucide asset catalog contains the 29 [pinned vectors](../design/branding/icons/lucide/) with their strokes set to black for template rendering. The interim universal app icon composites the approved [C symbol raster](../design/branding/taste-ticket/lumio-c-symbol-dark.png) on Editorial Paper. Film posters and catalog records come from the service. [Catalog attribution](../data/SOURCE-ATTRIBUTION.md)
