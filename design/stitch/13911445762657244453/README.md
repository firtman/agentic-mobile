# Lumio Film Discovery App — Stitch exports

Source project: `13911445762657244453` in Google Stitch. Retrieved November 17, 2026, at the owner's request.

These are downloaded design references and original Stitch HTML exports. They do not authorize app implementation or approve additional branding choices. Approved branding remains in [DECISIONS.md](../../branding/DECISIONS.md).

| Screen | Screenshot | HTML code | Stitch screen ID |
| --- | --- | --- | --- |
| Settings | [PNG](screens/settings/screen.png) | [HTML](screens/settings/screen.html) | `05620f5a87624207bdcd52009675bccc` |
| Film Details | [PNG](screens/film-details/screen.png) | [HTML](screens/film-details/screen.html) | `81d134fa932d41098f2f02fa22ea00d4` |
| Arcade | [PNG](screens/arcade/screen.png) | [HTML](screens/arcade/screen.html) | `48c527db61a34742a46d2e5024f36fa4` |
| Genie Recommendations | [PNG](screens/genie-recommendations/screen.png) | [HTML](screens/genie-recommendations/screen.html) | `9e8742089b804bfeacd9599cf39fdbc1` |
| Explore | [PNG](screens/explore/screen.png) | [HTML](screens/explore/screen.html) | `159239ddfde1447aa65176fbd8150c08` |
| Swipe | [PNG](screens/swipe/screen.png) | [HTML](screens/swipe/screen.html) | `91db4b9ac3304deba68e1048ee98e268` |

## Retrieval and validation

Screen metadata and source URLs are preserved in [manifest.json](manifest.json). Authenticated `curl --location` requests downloaded the files; the API's JSON wrappers were decoded into PNG bytes and HTML text. Files were not redesigned or converted into native application code.

All six images have PNG signatures and dimensions matching Stitch metadata. All six code files contain an HTML document. Sizes and SHA-256 checksums are recorded in [download-validation.json](download-validation.json).

The `screens/` directory is ignored by Git because these references may include movie and poster imagery. The API key was read from the local Codex profile and was not saved here. HTML may depend on remote scripts, fonts, or images; those dependencies were not downloaded, and browser behavior has not been tested. Movie facts and ratings shown in mockups have not been validated against the local catalog.
