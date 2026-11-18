import SwiftUI

struct GenieScreen: View {
    let model: GenieModel
    let ratings: RatingsStore
    let startSwiping: () -> Void

    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 10) {
                    LucideIcon(name: "sparkles").foregroundStyle(palette.accent)
                    Text("Genie")
                        .editorialFont(.title)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("screen-title")
                }
                Text("Your likes and dislikes help Genie find films for you.")
                    .editorialFont(.body)
                    .foregroundStyle(palette.secondary)

                switch model.status {
                case .loading:
                    ProgressView("Finding recommendations…")
                case .getStarted:
                    notice("Give Genie a starting point", detail: "Like or dislike a few films to get your first picks.")
                        .accessibilityIdentifier("genie-get-started")
                case .noMatches:
                    notice("No matches yet", detail: "Rate more films so Genie can find a better match.")
                        .accessibilityIdentifier("genie-no-matches")
                case .networkError:
                    errorState("Couldn’t connect to the catalog.")
                case .storageError:
                    errorState("Couldn’t load your saved choices.")
                case .content:
                    if let pick = model.pick { recommendation(pick) }
                }
            }
            .frame(maxWidth: 620, alignment: .leading)
            .padding(20)
            .frame(maxWidth: .infinity)
        }
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Genie")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
        .onAppear { model.sync() }
        .onChange(of: ratings.revision) { model.sync() }
    }

    private func recommendation(_ pick: Recommendation) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline) {
                    Text("Chosen for you").editorialFont(.section).fixedSize()
                    Spacer(minLength: 12)
                    position
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("Chosen for you").editorialFont(.section)
                    position
                }
            }
            VStack(alignment: .leading, spacing: 14) {
                let layout = typeSize.isAccessibilitySize
                    ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
                    : AnyLayout(HStackLayout(alignment: .top, spacing: 14))
                layout {
                    MoviePoster(movie: pick.movie, aspect: 2.0 / 3, contentMode: .fit)
                        .frame(width: 112)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    VStack(alignment: .leading, spacing: 6) {
                        Text(pick.movie.title)
                            .editorialFont(.section)
                            .accessibilityIdentifier("genie-title")
                        if let year = pick.movie.year, year > 0 {
                            Text(String(year)).editorialFont(.metadata).foregroundStyle(palette.secondary)
                        }
                        if let genres = pick.movie.genres, !genres.isEmpty {
                            Text(genres.joined(separator: " · "))
                                .editorialFont(.metadata)
                                .foregroundStyle(palette.secondary)
                        }
                        NavigationLink(value: LumioRoute.movie(pick.movie.id)) {
                            Text("View details")
                                .editorialFont(.control)
                                .frame(minHeight: 44)
                                .contentShape(Rectangle())
                        }
                        .accessibilityIdentifier("genie-details")
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                Divider()
                Text("Why this film?")
                    .editorialFont(.control)
                    .accessibilityAddTraits(.isHeader)
                ForEach(pick.matches.indices, id: \.self) { index in
                    Text(reason(pick.matches[index]))
                        .editorialFont(.body)
                        .foregroundStyle(palette.secondary)
                        .accessibilityIdentifier("genie-reason")
                }
            }
            .padding(14)
            .background(palette.raised, in: RoundedRectangle(cornerRadius: 12))
            if model.hasNext {
                action("Next", perform: model.next)
                    .accessibilityIdentifier("genie-next")
            } else {
                Text("You’ve reached the end. Rate more films for new picks.")
                    .editorialFont(.body)
                    .foregroundStyle(palette.secondary)
                action("Restart picks", perform: model.restart)
                    .accessibilityIdentifier("genie-restart")
            }
        }
    }

    private var position: some View {
        Text("\(model.position) of \(model.count)")
            .editorialFont(.metadata)
            .monospacedDigit()
            .foregroundStyle(palette.secondary)
            .fixedSize()
            .accessibilityIdentifier("genie-position")
    }

    private func reason(_ match: TasteMatch) -> LocalizedStringKey {
        switch match.kind {
        case .genre: "Genre you like: \(match.name)"
        case .director: "Director you like: \(match.name)"
        case .actor: "Actor you like: \(match.name)"
        }
    }

    private func notice(_ title: LocalizedStringKey, detail: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).editorialFont(.section)
            Text(detail).editorialFont(.body).foregroundStyle(palette.secondary)
            action("Start swiping", perform: startSwiping)
        }
    }

    private func errorState(_ title: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).editorialFont(.body).foregroundStyle(palette.error)
            action("Retry") { model.sync(force: true) }
                .accessibilityIdentifier("genie-retry")
        }
    }

    private func action(_ title: LocalizedStringKey, perform: @escaping () -> Void) -> some View {
        Button(action: perform) {
            Text(title)
                .editorialFont(.control)
                .foregroundStyle(palette.ink)
                .padding(.horizontal, 18)
                .frame(minHeight: 44)
                .background(palette.coral, in: RoundedRectangle(cornerRadius: 8))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
