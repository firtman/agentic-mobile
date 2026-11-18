import SwiftUI

struct ExploreScreen: View {
    let model: ExploreModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize
    @AppStorage("explore.sort") private var savedSort = ExploreSort.yearDescending.rawValue
    @FocusState private var searching: Bool
    @State private var showingCredits = false

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Explore")
                    .editorialFont(.title)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("screen-title")
                searchField
                genreFilters
                switch model.status {
                case .loading:
                    ProgressView("Loading movies…")
                        .padding(.vertical, 16)
                    sortMenu
                case .failed:
                    sortMenu
                    message("Couldn’t connect to the catalog.", icon: "wifi-off", color: palette.error)
                    Button("Retry", action: model.refresh)
                        .buttonStyle(ExploreButtonStyle(selected: true))
                        .accessibilityIdentifier("catalog-retry")
                case .empty:
                    sortMenu
                    message("No movies match these filters.", icon: "search", color: palette.secondary)
                        .accessibilityIdentifier("explore-empty")
                    Button("Reset filters", action: reset)
                        .buttonStyle(ExploreButtonStyle(selected: true))
                        .accessibilityIdentifier("explore-reset")
                case .content:
                    if let first = model.movies.first { spotlight(first) }
                    results
                }
                Button("Source attribution") { showingCredits = true }
                    .editorialFont(.control)
                    .frame(minHeight: 44)
            }
            .frame(maxWidth: 900, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity)
        }
        .accessibilityIdentifier("explore-scroll")
        .scrollDismissesKeyboard(.interactively)
        .foregroundStyle(palette.primary)
        .background(palette.canvas)
        .refreshable { model.refresh() }
        .task { model.start() }
        .sheet(isPresented: $showingCredits) { ExploreCredits() }
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            LucideIcon(name: "search")
                .foregroundStyle(palette.secondary)
            TextField("Search movies", text: Binding(
                get: { model.filters.query },
                set: model.search
            ))
            .editorialFont(.body)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
            .submitLabel(.search)
            .focused($searching)
            .simultaneousGesture(TapGesture().onEnded { searching = true })
            .onSubmit { searching = false }
            .accessibilityIdentifier("explore-search")
            if !model.filters.query.isEmpty {
                Button {
                    model.search("")
                } label: {
                    LucideIcon(name: "x")
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.leading, 12)
        .padding(.trailing, model.filters.query.isEmpty ? 12 : 4)
        .frame(minHeight: 50)
        .background(palette.raised, in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .strokeBorder(searching ? palette.accent : palette.outline, lineWidth: 1)
                .allowsHitTesting(false)
        }
    }

    private var genreFilters: some View {
        VStack(alignment: .leading, spacing: 8) {
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    genreChip(Text("All genres"), genre: nil)
                    ForEach(model.genres, id: \.self) { genre in
                        genreChip(Text(genre), genre: genre)
                    }
                }
            }
            .scrollIndicators(.hidden)
            if model.genreStatus == .failed {
                message("Couldn’t load genres.", icon: "wifi-off", color: palette.error)
                Button("Retry genres", action: model.loadGenres)
                    .buttonStyle(ExploreButtonStyle())
            }
        }
    }

    private func genreChip(_ label: Text, genre: String?) -> some View {
        let selected = model.filters.genre == genre
        return Button {
            searching = false
            model.chooseGenre(genre)
        } label: {
            label.fixedSize()
        }
        .buttonStyle(ExploreButtonStyle(selected: selected))
        .accessibilityAddTraits(selected ? .isSelected : [])
        .accessibilityIdentifier("explore-genre-\(genre ?? "all")")
    }

    private var sortMenu: some View {
        Group {
            if #available(iOS 26, *) {
                sortOptions
                    .buttonStyle(.glass)
                    .buttonBorderShape(.capsule)
            } else {
                sortOptions
                    .background(palette.raised, in: RoundedRectangle(cornerRadius: 8))
                    .overlay {
                        RoundedRectangle(cornerRadius: 8).strokeBorder(palette.outline)
                            .allowsHitTesting(false)
                    }
            }
        }
        .accessibilityIdentifier("explore-sort")
    }

    private var sortOptions: some View {
        Menu {
            Picker("Sort", selection: Binding(
                get: { model.filters.sort },
                set: { sort in
                    savedSort = sort.rawValue
                    model.chooseSort(sort)
                }
            )) {
                ForEach(ExploreSort.allCases) { sort in
                    Text(sort.label)
                        .tag(sort)
                        .accessibilityIdentifier("explore-sort-\(sort.rawValue)")
                }
            }
        } label: {
            HStack(spacing: 8) {
                LucideIcon(name: "arrow-up-down")
                Text(model.filters.sort.label)
                    .editorialFont(.control)
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 44)
            .foregroundStyle(palette.primary)
        }
    }

    private func spotlight(_ movie: Movie) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Spotlight")
                .editorialFont(.section)
                .accessibilityAddTraits(.isHeader)
            let layout = typeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
                : AnyLayout(HStackLayout(alignment: .top, spacing: 14))
            layout {
                MoviePoster(movie: movie, aspect: 2.0 / 3)
                    .frame(width: typeSize.isAccessibilitySize ? 120 : 104)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 7) {
                    movieCaption(movie, style: .section)
                    if let synopsis = movie.synopsis, !synopsis.isEmpty {
                        Text(synopsis)
                            .editorialFont(.body)
                            .foregroundStyle(palette.secondary)
                            .lineLimit(3)
                    }
                    if let source = suppliedWebURL(movie.sourceUrl) {
                        Link("View source", destination: source)
                            .editorialFont(.control)
                            .frame(minHeight: 44)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(12)
            .background(palette.raised)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay { RoundedRectangle(cornerRadius: 12).strokeBorder(palette.divider) }
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier("explore-spotlight")
        }
    }

    private var results: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Catalog")
                .editorialFont(.section)
                .accessibilityAddTraits(.isHeader)
            sortMenu
            if model.filters != ExploreFilters() {
                Button("Reset filters", action: reset)
                    .buttonStyle(ExploreButtonStyle())
                    .accessibilityIdentifier("explore-reset")
            }
            Divider().overlay(palette.divider)
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: typeSize.isAccessibilitySize ? 225 : 150), spacing: 14, alignment: .top)],
                spacing: 20
            ) {
                ForEach(model.movies) { movie in
                    VStack(alignment: .leading, spacing: 6) {
                        MoviePoster(movie: movie, aspect: 2.0 / 3)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        movieCaption(movie, style: .movieTitle)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityElement(children: .combine)
                    .accessibilityIdentifier("explore-movie")
                }
            }
            if model.loadingMore {
                ProgressView("Loading movies…")
            } else if model.hasMore {
                if model.pageFailed {
                    message("Couldn’t load more movies.", icon: "wifi-off", color: palette.error)
                }
                Button(model.pageFailed ? "Retry" : "Load more", action: model.loadMore)
                    .buttonStyle(ExploreButtonStyle())
                    .accessibilityIdentifier("explore-more")
            }
        }
    }

    private func movieCaption(_ movie: Movie, style: EditorialTextStyle) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(movie.title)
                .editorialFont(style)
            let metadata = [
                movie.year.flatMap { $0 > 0 ? String($0) : nil },
                movie.genres?.first
            ].compactMap { $0 }.joined(separator: " · ")
            if !metadata.isEmpty {
                Text(metadata)
                    .editorialFont(.metadata)
                    .foregroundStyle(palette.secondary)
            }
        }
    }

    private func message(_ text: LocalizedStringKey, icon: String, color: Color) -> some View {
        Label {
            Text(text).editorialFont(.body)
        } icon: {
            LucideIcon(name: icon)
        }
        .foregroundStyle(color)
    }

    private func reset() {
        searching = false
        savedSort = ExploreSort.yearDescending.rawValue
        model.reset()
    }
}

private struct ExploreButtonStyle: ButtonStyle {
    var selected = false
    @Environment(\.colorScheme) private var scheme

    func makeBody(configuration: Configuration) -> some View {
        let palette = EditorialPalette(scheme)
        configuration.label
            .editorialFont(.control)
            .foregroundStyle(selected ? palette.ink : palette.primary)
            .padding(.horizontal, 12)
            .frame(minHeight: 44)
            .background(
                selected ? palette.coral : (configuration.isPressed ? palette.subtle : palette.raised),
                in: RoundedRectangle(cornerRadius: 8)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(selected ? palette.coral : palette.outline)
            }
    }
}

private struct ExploreCredits: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                if let url = Bundle.main.url(forResource: "catalog_sources", withExtension: "txt"),
                   let notice = try? String(contentsOf: url, encoding: .utf8) {
                    Text((try? AttributedString(markdown: notice, options: .init(
                        interpretedSyntax: .inlineOnlyPreservingWhitespace
                    ))) ?? AttributedString(notice))
                    .editorialFont(.body)
                    .frame(maxWidth: 620, alignment: .leading)
                    .padding(24)
                }
            }
            .navigationTitle("Source attribution")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

private extension ExploreSort {
    var label: LocalizedStringKey {
        switch self {
        case .yearDescending: "Newest first"
        case .yearAscending: "Oldest first"
        case .titleAscending: "Title A–Z"
        case .titleDescending: "Title Z–A"
        case .ratingDescending: "Highest rated"
        case .popularityDescending: "Most popular"
        }
    }
}
