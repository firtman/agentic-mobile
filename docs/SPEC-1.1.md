# Lumio 1.1 — Daily Genie pick

The owner authorized the native **Daily Genie pick** Home Screen widget for Android, iPhone and iPad on 2026-11-18. This specification governs the widget. The [Lumio 1.0 product specification](SPEC-1.0.md) continues to govern the app, with the widget storage exception below. Changes to a **MUST** rule require owner approval and an update to this specification in the same commit.

## 1. Add the widget

- **Android.** Adding the widget from the launcher widget picker starts preparing picks from existing choices.
- **iPhone and iPad.** The Home Screen gallery offers small and medium widgets. Opening Lumio after installation or update lets it prepare picks from existing choices.
- The first personalized pick requires saved choices that produce a positive match and access to the online catalog to prepare recommendations.

## 2. Choose and refresh the daily pick

- The widget MUST use the same scoring, eligibility and ordering as Genie in [1.0 §7.1](SPEC-1.0.md#71-scoring). It rotates through the top 30 eligible films, or all eligible films if there are fewer. Every film with a saved choice is excluded, including **Unseen**. The rotation is independent of the in-app Genie position and its **Next** action.
- With a stable ranking, the same local calendar date selects the same film, and each successive date advances one place in the ranked list, wrapping at the end. With one eligible film, the widget keeps showing it. A prepared widget can rotate while Lumio is closed.
- Local midnight determines the next day's selection. The platform controls when that selection appears on the Home Screen and may display it later. [Android widget updates](https://developer.android.com/develop/ui/views/appwidgets/advanced) · [WidgetKit timelines](https://developer.apple.com/documentation/widgetkit/timeline)
- Saving a choice or undoing a change MUST trigger a recommendation refresh. If no pick is available, the widget MUST show guidance and open Genie, where the user can get started, record more choices or retry as appropriate.

## 3. Display and open the pick

- The widget MUST show one film title and its release year when available. It shows a thumbnail of the displayed film's supplied poster when the artwork loads. While the artwork is pending, missing or unavailable, the title, available year and tap action remain usable.
- Tapping a pick MUST open the exact displayed film in Movie details, whether Lumio is already running or starts from the widget. Movie details uses the existing online request and its loading, unavailable and Retry states. [Lumio 1.0 §5](SPEC-1.0.md#5-movie-details)
- Widget interface text MUST be available in English, Spanish, German and Portuguese. Movie titles retain their catalog language. The widget follows the system's light or dark appearance and uses the Editorial palette and Lucide Genie icon. [Colors and typography](../design/branding/COLORS-TYPOGRAPHY.md) · [Icons](../design/branding/ICONS.md)

## 4. Device storage and network access

This widget is the limited exception to [1.0 §2.1 and §2.2](SPEC-1.0.md#21-catalog-access-online-only). The local widget snapshot MUST contain at most 30 ranked entries. The allowed movie fields are ID, title and optional release year, plus the supplied poster URL on iOS. The snapshot supports rotation without another catalog download. Ratings and Undo stay in their existing private local stores.

- **Android.** Lumio looks up the displayed film's poster URL online and passes an in-memory thumbnail to the launcher. Removing the last widget clears the saved picks.
- **iOS.** The app shares the snapshot with the WidgetKit extension on the same device. The extension fetches the current pick's thumbnail online from the supplied URL.
- Lumio MUST keep poster images out of app-managed disk storage. Catalog data outside the widget snapshot remains subject to the 1.0 online-only rules.

## 5. Validation

Platform validation logs record the checks performed and their limitations. [Android validation](../validation/android.md) · [iOS validation](../validation/ios.md)
