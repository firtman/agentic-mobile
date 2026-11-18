import SwiftUI

struct MovieDetailsScreen: View {
    let movieID: String
    let catalog: CatalogClient
    let ratings: RatingsStore

    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var state: LoadState = .loading
    @State private var attempt = 0
    @State private var choice: RatingChoice?
    @State private var ratingAvailable = false
    @State private var ratingFeedback: RatingFeedback?

    private enum LoadState {
        case loading, available(Movie), missing, failed
    }

    private enum RatingFeedback {
        case saved(UUID), undone, unavailable, failed, stale
    }

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                Group {
                    switch state {
                    case .loading:
                        ProgressView("Loading movie…")
                    case .missing:
                        message("This movie is unavailable.", icon: "circle-question-mark")
                    case .failed:
                        VStack(alignment: .leading, spacing: 14) {
                            message("Couldn’t connect to the catalog.", icon: "wifi-off")
                            Button("Retry") { attempt += 1 }
                                .frame(minHeight: 44)
                                .accessibilityIdentifier("movie-retry")
                        }
                    case .available(let movie):
                        details(movie, wide: geometry.size.width >= 630 && !typeSize.isAccessibilitySize)
                    }
                }
                .frame(maxWidth: 900, alignment: .leading)
                .padding(20)
                .frame(maxWidth: .infinity)
            }
            .accessibilityIdentifier("movie-detail-scroll")
        }
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Movie details")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
        .task(id: attempt) {
            readRating()
            state = .loading
            do {
                let movie = try await catalog.movie(id: movieID)
                try Task.checkCancellation()
                state = .available(movie)
            } catch let error as CatalogHTTPError where error.statusCode == 404 {
                if !Task.isCancelled { state = .missing }
            } catch {
                if !Task.isCancelled { state = .failed }
            }
        }
    }

    private func details(_ movie: Movie, wide: Bool) -> some View {
        let layout = wide
            ? AnyLayout(HStackLayout(alignment: .top, spacing: 28))
            : AnyLayout(VStackLayout(alignment: .leading, spacing: 18))
        return layout {
            MoviePoster(movie: movie, aspect: 2.0 / 3, contentMode: .fit)
                .frame(maxWidth: wide ? 245 : 184)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .frame(maxWidth: wide ? nil : .infinity)
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(movie.title)
                        .editorialFont(.title)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("movie-detail-title")
                    if let metadata = summary(movie) {
                        Text(metadata)
                            .editorialFont(.metadata)
                            .foregroundStyle(palette.secondary)
                    }
                }
                ratingPanel(movie)
                if let synopsis = nonblank(movie.synopsis) {
                    Text(synopsis).editorialFont(.body)
                }
                movieInformation(movie)
                sourceLinks(movie)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func ratingPanel(_ movie: Movie) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Your choice")
                .editorialFont(.section)
                .accessibilityAddTraits(.isHeader)
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) {
                    ForEach(RatingChoice.allCases) { item in
                        ratingButton(item, movie: movie)
                            .fixedSize(horizontal: true, vertical: false)
                    }
                }
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(RatingChoice.allCases) { item in
                        ratingButton(item, movie: movie)
                    }
                }
            }
            .disabled(!ratingAvailable)
            if let ratingFeedback {
                feedback(ratingFeedback)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(palette.raised, in: RoundedRectangle(cornerRadius: 12))
    }

    private func ratingButton(_ item: RatingChoice, movie: Movie) -> some View {
        let selected = choice == item
        return Button {
            save(item, movie: movie)
        } label: {
            HStack(spacing: 5) {
                LucideIcon(name: item.icon)
                Text(item.label).editorialFont(.control)
            }
            .padding(.horizontal, 9)
            .frame(minHeight: 44)
            .foregroundStyle(selected ? palette.ink : palette.primary)
            .background(selected ? palette.coral : palette.raised, in: RoundedRectangle(cornerRadius: 8))
            .overlay {
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(selected ? palette.coral : palette.outline)
                    .allowsHitTesting(false)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
        .accessibilityIdentifier("movie-rate-\(item.rawValue)")
    }

    @ViewBuilder
    private func feedback(_ state: RatingFeedback) -> some View {
        switch state {
        case .saved(let id):
            HStack {
                Text("Choice saved").foregroundStyle(palette.success)
                Spacer()
                Button("Undo") { undo(id) }
                    .frame(minHeight: 44)
                    .accessibilityIdentifier("movie-rating-undo")
            }
            .editorialFont(.control)
        case .undone:
            Text("Choice undone")
                .editorialFont(.body)
                .accessibilityIdentifier("movie-rating-undone")
        case .unavailable:
            message("Couldn’t load your choice.", icon: "wifi-off")
            Button("Retry", action: readRating).frame(minHeight: 44)
        case .failed:
            message("Couldn’t update your choice. Try again.", icon: "x")
        case .stale:
            message("This change can no longer be undone.", icon: "x")
        }
    }

    private func movieInformation(_ movie: Movie) -> some View {
        let rows: [(LocalizedStringKey, String?)] = [
            ("Release year", positive(movie.year)),
            ("Genres", joined(movie.genres)),
            ("Runtime", movie.runtime.flatMap { $0 > 0 ? Duration.seconds(Double($0) * 60).formatted(.units(allowed: [.hours, .minutes], width: .abbreviated)) : nil }),
            ("Director", nonblank(movie.director)),
            ("Cast", joined(movie.cast)),
            ("Source rating", sourceRating(movie))
        ]
        let visible = rows.compactMap { row in row.1.map { (row.0, $0) } }
        return VStack(alignment: .leading, spacing: 12) {
            if !visible.isEmpty {
                Text("Movie information")
                    .editorialFont(.section)
                    .accessibilityAddTraits(.isHeader)
            }
            ForEach(visible.indices, id: \.self) { index in
                let row = visible[index]
                VStack(alignment: .leading, spacing: 3) {
                    Text(row.0).editorialFont(.control)
                    Text(row.1).editorialFont(.body).foregroundStyle(palette.secondary)
                }
                .accessibilityElement(children: .combine)
            }
        }
    }

    @ViewBuilder
    private func sourceLinks(_ movie: Movie) -> some View {
        let source = suppliedWebURL(movie.sourceUrl)
        let dataset = suppliedWebURL(movie.datasetSourceUrl)
        if let source { sourceLink("View source", to: source) }
        if let dataset, dataset != source { sourceLink("Dataset source", to: dataset) }
    }

    private func sourceLink(_ title: LocalizedStringKey, to url: URL) -> some View {
        Link(destination: url) {
            Label {
                Text(title).editorialFont(.control)
            } icon: {
                LucideIcon(name: "external-link")
            }
            .frame(minHeight: 44)
        }
    }

    private func message(_ title: LocalizedStringKey, icon: String) -> some View {
        Label {
            Text(title).editorialFont(.body)
        } icon: {
            LucideIcon(name: icon)
        }
        .foregroundStyle(palette.error)
    }

    private func readRating() {
        do {
            choice = try ratings.choice(for: movieID)
            ratingAvailable = true
            ratingFeedback = nil
        } catch {
            ratingAvailable = false
            ratingFeedback = .unavailable
        }
    }

    private func save(_ newChoice: RatingChoice, movie: Movie) {
        do {
            if let token = try ratings.rate(movie, as: newChoice) {
                choice = newChoice
                ratingFeedback = .saved(token)
            }
        } catch {
            ratingFeedback = .failed
        }
    }

    private func undo(_ id: UUID) {
        do {
            let undone = try ratings.undoLatest(expectedID: id)
            choice = try ratings.choice(for: movieID)
            ratingFeedback = undone ? .undone : .stale
        } catch {
            ratingFeedback = .failed
        }
    }

    private func summary(_ movie: Movie) -> String? {
        joined([positive(movie.year), movie.genres?.first].compactMap { $0 }, separator: " · ")
    }

    private func sourceRating(_ movie: Movie) -> String? {
        guard let rating = movie.rating, rating > 0 else { return nil }
        let formatted = rating.formatted(.number.precision(.fractionLength(1)))
        if let count = movie.voteCount, count > 0 {
            return String(localized: "\(formatted) out of 10 · \(count) votes")
        }
        return String(localized: "\(formatted) out of 10")
    }

    private func positive(_ value: Int?) -> String? {
        value.flatMap { $0 > 0 ? String($0) : nil }
    }

    private func nonblank(_ value: String?) -> String? {
        value.flatMap {
            let trimmed = $0.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }
    }

    private func joined(_ values: [String]?, separator: String = ", ") -> String? {
        nonblank(values?.compactMap { nonblank($0) }.joined(separator: separator))
    }
}

private extension RatingChoice {
    var label: LocalizedStringKey {
        switch self {
        case .like: "Like"
        case .dislike: "Dislike"
        case .unseen: "Not seen yet"
        }
    }

    var icon: String {
        switch self {
        case .like: "thumbs-up"
        case .dislike: "thumbs-down"
        case .unseen: "eye-off"
        }
    }
}
