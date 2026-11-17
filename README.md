# Agentic iOS and Android Development

At [Master.dev](https://master.dev) · By [Maximiliano Firtman](https://firt.dev)

[Workshop slides (PDF)](slides/MasterDev-AgenticMobile.pdf)

Lumio is a film discovery app where people explore a catalog, swipe to record preferences, get personal recommendations, and play cinema games. Use the design prompts below to define the rest of the app’s identity.

The owner selected **Lumio**, the **Taste Ticket** logo direction, and **variation C — Editorial**. See the [branding decision document](design/branding/DECISIONS.md) for the selected design, visual reference, and remaining decisions.

The [design index](design/README.md) brings together approved colors, typography, icons, interactive previews, and exploratory screen references.

This repository contains a UX brief, design prompts, and a local Node.js catalog API. Catalog data is downloaded during setup rather than bundled in the repository. Android and iOS are independent native apps using their respective SDKs.

## Android implementation

The owner’s [staged Android plan](docs/ANDROID-PLAN.md) uses native Kotlin and Compose, package `dev.master.lumio`, and SDK 26/36/36. Android catalog access is online-only: no bundled or persisted catalog. Ratings and settings remain on device as their stages are implemented. This owner decision supersedes the original brief’s offline behavior for Android. See [Android setup](android/README.md) and [validation](validation/android.md).

## App UX brief

Use this prompt to understand the app’s sections and user experience.

> Understand the UX of a film discovery app. It helps people find movies, express their preferences, receive recommendations, and enjoy cinema games. Treat the following as a description of what the app offers; do not create an implementation plan or start building yet.
>
> **Explore.** Browse a visual movie catalog, search by title or director, filter by genre, and change the sort order. A featured movie and poster grid offer entry points for discovery. Selecting a movie opens its details.
>
> **Movie details.** Read the synopsis and available information such as release year, genres, runtime, director, cast, and source rating. View its poster and access source information. Mark the movie as liked, disliked, or not yet seen.
>
> **Swipe.** Discover movies one at a time. Swipe right to like, left to dislike, or down to mark a movie as unseen. Visible buttons provide the same actions. Review previous choices and undo a change. Movies already marked leave the discovery queue.
>
> **Genie.** Discover personal recommendations informed by likes and dislikes, with understandable reasons connecting suggestions to preferred genres, directors, or actors. Movies already marked, including unseen movies, stay out of recommendations. Before preferences are available, explain how to get started.
>
> **Arcade.** Choose between two cinema games. Trivia offers short rounds of four-choice movie questions, a countdown, points, streak bonuses, a 50:50 lifeline, answer feedback, and a final score. Players can pause, hold an answer reveal to read it, and play again. Poster Puzzle lets players reconstruct a movie poster from tiles, see their progress, and start another puzzle after completing it. Both games return to the Arcade hub.
>
> **Settings.** Choose system, light, or dark appearance and enable or disable game sound.  Preferences and movie choices remain available between sessions.
>
> **Navigation and everyday use.** Explore, Swipe, Genie, and Arcade are the main destinations; Settings is accessible globally. Movie details and games have clear return navigation. The experience adapts to different screen sizes and remains usable with larger text and assistive technology. Provide understandable loading, empty, error, and offline states. Once catalog data is available locally, browsing and personal preferences should remain useful offline. If a first offline launch has no catalog, explain that initial data setup is needed.
>
> Use this brief as product context; it does not prescribe an implementation plan.

## Design tools

Use these tools to explore the UX brief visually:

- [Google Stitch](https://stitch.withgoogle.com/): explore interface designs and screen flows. See [Google’s introduction](https://developers.googleblog.com/en/stitch-a-new-way-to-design-uis/).
- [Claude Design](https://claude.com/product/design): explore visual concepts and interactive prototypes. Use the official product page for current access options.

## Prompt ideas for design assets

Copy the App UX brief above into your chosen AI tool as context, then use one of these prompts. Replace `[APP NAME]` only after choosing a name.

### App name

> Read the App UX brief and suggest names for this project. Consider the full experience: exploring films, recording preferences, receiving personal recommendations, and playing cinema games. Propose 12 names across different directions, such as playful, cinematic, and discovery-focused. For each name, explain its connection to the experience and flag obvious pronunciation, spelling, or unintended-meaning issues in English, Spanish, German, and Portuguese. Avoid names that make the app sound limited to swiping or trivia. Shortlist your strongest three and explain the tradeoffs. Distinguish creative suggestions from verified availability; if you research existing apps, trademarks, or domains, link your sources and state what you checked. Do not claim a name is legally cleared. Present the options for my decision before changing project files, package identifiers, branding, or repository names. Do not register domains or publish anything.

### Logo

> Explore three distinct logo directions for the unnamed film discovery app described in the UX brief. Use `[APP NAME]` if a name has been selected; otherwise start with symbol-only concepts. Aim for a recognizable silhouette that works at small sizes, in one color, and on light and dark backgrounds. Explore discovery, personal taste, and cinema without relying only on a generic film reel. Explain the idea behind each direction and show how it could become an Android launcher icon. Once a direction and app name are selected, develop symbol and wordmark variations. For the final selected concept, provide an editable vector version if your tool supports it, plus transparent PNG exports. If you cannot export vectors, say so rather than labeling a raster image as SVG.

### Icons

> Design a coherent icon family for this film discovery app: Explore, Swipe, Genie recommendations, Arcade, Settings, Search, Filter, Like, Dislike, Unseen, Undo, History, Back, Sound on/off, Pause, Trivia, and Poster Puzzle. Start by showing three stylistic directions using five representative icons. Keep the final set consistent in optical size, stroke, corner treatment, and visual weight. Show default and selected navigation states at Android UI size, on light and dark backgrounds. Avoid text inside icons. Suggest accessible labels and identify symbols that may be ambiguous. Provide individual editable vectors if supported and a labeled overview sheet for review.

### Brand manual

> Create a concise brand manual for the film discovery app using the UX brief and the selected visual assets I supply. If no logo or name has been selected, keep those sections as proposals or placeholders. Include brand personality, logo use and clear space, minimum sizes, light/dark color palettes with exact color values, typography with links to font sources and license information, icon rules, artwork direction, spacing, and examples of correct and incorrect use. Show how the identity applies to Explore, Swipe, recommendations, and Arcade. Identify which decisions need my approval. Check proposed text/background color pairs and report their contrast ratios; do not claim accessibility compliance from appearance alone. Deliver an editable guide if supported, with a PDF for sharing.

### Navigation

> Explore the navigation UX for the app described in the brief. Show a screen map and low-fidelity mobile wireframes for the four main destinations: Explore, Swipe, Genie, and Arcade, with Settings accessible globally. Include routes to movie details, preference history, Trivia, and Poster Puzzle, plus return paths and Android Back behavior. Show how users keep their place when switching sections and how active games handle leaving and returning. Propose a wide-screen navigation variant. Annotate key user actions, labels, and empty states. Explain any alternatives or tradeoffs for review. Focus on the user experience; do not generate code or an implementation plan.

### Screen designs

> Using the app UX brief and any approved brand assets I supply, propose three visual directions using Explore and Swipe as representative screens. After I select one, create a consistent Android screen set for Explore, movie details, Swipe, preference history, Genie recommendations, the Arcade hub, Trivia question/answer feedback/results, Poster Puzzle, and Settings. Include meaningful loading, empty, error, and offline states. Show representative light/dark and larger-text variants, plus one wide-screen example. Use fictional placeholder movie titles and artwork placeholders; do not invent real ratings or present fabricated movie facts as data. Keep text and controls editable where the tool supports it. Deliver a screen overview, individual screens, and brief interaction annotations. Treat the output as design proposals for review, not a finished implementation.

## Tools for Android agentic development

Install the base toolchain, choose Codex, Claude Code, or Antigravity, and add the relevant official and optional community plugins. Plugin instructions were checked on November 17, 2026; consult their linked documentation for updates.

| Tool | Purpose and official/source link |
| --- | --- |
| Git | [Download Git](https://git-scm.com/downloads) |
| GitHub CLI | [Install gh](https://cli.github.com/) for fork/clone/auth workflows |
| Node.js 22+ | [Download Node.js](https://nodejs.org/en/download) for the local API; no npm dependencies required |
| Java 21 | [Eclipse Temurin JDK](https://adoptium.net/temurin/releases/?version=21); a supported starting Gradle runtime |
| Android Studio | [Download Android Studio](https://developer.android.com/studio); includes SDK Manager and Device Manager |
| Android SDK and emulator | [SDK Manager](https://developer.android.com/studio/intro/update#sdk-manager), [virtual devices](https://developer.android.com/studio/run/managing-avds), [ADB](https://developer.android.com/tools/adb); install platform 36, compatible Build Tools, Platform Tools, Emulator, and a matching system image |
| Codex | [OpenAI Codex](https://developers.openai.com/codex/) |
| Claude Code | [Claude Code setup](https://code.claude.com/docs/en/setup) |
| Antigravity | [Download](https://antigravity.google/download), [getting started](https://antigravity.google/docs/getting-started), [CLI setup](https://antigravity.google/docs/getting-started?tab=cli) |
| Google's official Android CLI plugin | [Installation for Codex, Claude, and Antigravity](https://developer.android.com/tools/agents/android-cli/agent-plugins), [plugin source](https://github.com/android/android-cli-plugin), [Android CLI](https://developer.android.com/tools/agents/android-cli) |
| Google's official Android skills | [Skills repository](https://github.com/android/skills), [documentation](https://developer.android.com/tools/agents/android-skills) |
| Community Android emulator plugin for Codex | [mttmcknn/android-emulator-plugin](https://github.com/mttmcknn/android-emulator-plugin), with a live device panel and agent inspection/control |
| Community Android emulator plugin for Claude | [tiwiz/android-emulator-plugin-claude](https://github.com/tiwiz/android-emulator-plugin-claude), a Claude-oriented fork; follow its current host and installation requirements |

The community emulator plugins are independent projects, not official Google, OpenAI, or Anthropic plugins. They complement the SDK/emulator installation. A physical device with USB debugging can also be used.

Install the Android CLI using Google's [download and installation instructions](https://developer.android.com/tools/agents/android-cli/download). The plugin's included `android-cli` skill also contains platform-specific installation instructions that the agent can follow if the executable is missing. Installing a plugin does not by itself establish that the CLI, SDK, and emulator are installed; verify the local setup before building.

The CLI plugin includes the `android-cli` skill. For the broader Android guidance collection, also install the Android skills plugin below, or use `android skills add --all` after installing the CLI. Choose one method for that collection.

Google's official Android CLI plugin, in a terminal:

```sh
# Codex
codex plugin marketplace add android/android-cli-plugin
codex plugin add android-cli@android-cli-plugin

# Claude Code
claude plugin marketplace add android/android-cli-plugin
claude plugin install android-cli@android-cli-plugin

# Antigravity
agy plugin install android-cli-plugin@antigravity-plugins-official
```

Official Android skills:

```sh
# Codex
codex plugin marketplace add android/skills
codex plugin add android-skills@android-skills

# Claude Code
claude plugin marketplace add android/skills
claude plugin install android-skills@android-skills
```

For Antigravity, install the broader skills collection through the Android CLI:

```sh
android skills add --all --agent=antigravity --project=.
```

Antigravity also offers Android CLI under **Customizations > Build with Google**. Its official Android integration provides a Device Hub for virtual/physical devices and interactive Compose previews; see [Google’s Android plugin guide](https://developer.android.com/tools/agents/android-cli/agent-plugins). Use that integration for device interaction in Antigravity. The community plugins below target Codex and Claude respectively.

Community emulator plugin for Codex:

```sh
codex plugin marketplace add mttmcknn/android-emulator-plugin
codex plugin add android-emulator-plugin@mttmcknn
```

For the Claude community emulator plugin, follow the [repository's installation guide](https://github.com/tiwiz/android-emulator-plugin-claude#readme) for its supported Claude host. Restart your agent application after installation if required. Review Android SDK terms yourself during SDK setup.

## Using Antigravity

After cloning and switching to `main` as described below, open the repository folder in Antigravity, or run `agy` from its root. Ask the agent to read this README, set up the local API, and understand the UX brief. Keep the API running in a separate terminal during app development. The same repository, branch, and local API workflow applies to all three agents.

## Coding agent: acquire the repository and preserve the baseline

Repository: [firtman/agentic-mobile](https://github.com/firtman/agentic-mobile). Choose one acquisition path.

Fork into your account and clone (usual contributor workflow):

```sh
gh auth login
gh repo fork firtman/agentic-mobile --clone
cd agentic-mobile
git switch main
git pull --ff-only origin main
```

Or clone directly if you have access and do not need a fork:

```sh
git clone https://github.com/firtman/agentic-mobile.git
cd agentic-mobile
git switch main
git pull --ff-only origin main
```

For an existing checkout, first check `git status`. Preserve uncommitted work and do not reset or overwrite it. Switch to `main` and fast-forward it from `origin/main` when the working tree is clean. In a fork, `origin` is your fork; use `upstream` (adding the source URL if absent) to fetch later source changes. Make ongoing project changes on `main`.

The `initial-state` branch preserves the starting code. Leave it unchanged. Use `main` for ongoing development and future commits. To inspect the starting code, run `git switch initial-state`; return with `git switch main` before making changes. If the branch is not available locally, first run `git fetch origin` and then `git switch --track origin/initial-state`.

## Public testing API

A public testing version of the movie catalog API is available on Vercel at **https://movie-api-delta-six.vercel.app**. You can explore the data before setting up the local server:

- [Health check](https://movie-api-delta-six.vercel.app/health): service status and movie count.
- [Full catalog](https://movie-api-delta-six.vercel.app/catalog): movie and trivia data.
- [Sample movies](https://movie-api-delta-six.vercel.app/movies?limit=10): ten movie records.
- [Search example](https://movie-api-delta-six.vercel.app/movies?q=batman&limit=5): search results.
- [Genres and languages](https://movie-api-delta-six.vercel.app/genres): available filters.

This read-only testing API requires no authentication. The local setup command below downloads its catalog, so you can run the API on your own computer.

## Set up and run the local API

From the repository root on `main`:

```sh
node --version                        # must be 22 or newer
npm --prefix api run setup            # download to ignored data/movies.json
npm --prefix api start                # keep this terminal running
```

The API uses Node.js built-in libraries, so there are no dependencies to install. The setup command downloads the catalog once; the server then reads that local file. The source defaults to the [public testing catalog](https://movie-api-delta-six.vercel.app/catalog). Override it with `CATALOG_SOURCE_URL` for another compatible normalized catalog. Downloads are not committed. See [data provenance](data/SOURCE-ATTRIBUTION.md).

In another terminal:

```sh
curl --fail http://127.0.0.1:3000/health
curl --fail 'http://127.0.0.1:3000/movies?q=batman&limit=5'
curl --fail http://127.0.0.1:3000/genres
```

| Route | Result |
| --- | --- |
| GET or HEAD /health | Service status and movie count |
| GET or HEAD /catalog | Full normalized movies/trivia catalog |
| GET or HEAD /movies | Paginated movies; default limit 50, maximum 500 |
| GET or HEAD /movies/{id} | One movie, or 404 |
| GET or HEAD /genres | Sorted genres and original-language codes |

`/movies` accepts `q`, `genre`, `language`, `year`, `minRating`, `sort` (title/year/rating/popularity), `order` (asc/desc), `limit`, and `offset`. Invalid or repeated parameters return 400; unsupported methods return 405. It is a local read-only service, with no user-rating upload.

Android emulator: configure the app's debug catalog URL as `http://10.0.2.2:3000/catalog` and permit cleartext HTTP in debug builds only. A USB-connected device can use `adb reverse tcp:3000 tcp:3000` and `http://127.0.0.1:3000/catalog`. For a LAN device, run `HOST=0.0.0.0 PORT=3000 npm --prefix api start` and use your computer's LAN IP. Release connections require HTTPS. The public testing catalog is available at `https://movie-api-delta-six.vercel.app/catalog`.

## Repository layout

- `README.md`: project description, UX brief, tools, and onboarding.
- `api/server.js`: dependency-free read-only API.
- `api/setup.js`: downloads and validates a local catalog.
- `data/README.md`, `data/SOURCE-ATTRIBUTION.md`: data setup and provenance; no movie records.
