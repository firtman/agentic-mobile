import SwiftUI

struct PosterPuzzleScreen: View {
    let model: PosterPuzzleModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dismiss) private var dismiss

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let movie = model.movie {
                    Text(movie.title)
                        .editorialFont(.title)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("puzzle-movie")
                    if let year = movie.year, year > 0 {
                        Text(String(year)).editorialFont(.metadata).foregroundStyle(palette.secondary)
                    }
                }
                switch model.status {
                case .idle, .loadingCatalog:
                    ProgressView("Finding a poster…")
                case .catalogError:
                    Text("Couldn’t connect to the catalog.").foregroundStyle(palette.error)
                    action("Retry", id: "puzzle-retry", perform: model.retry)
                case .noPosters:
                    Text("No playable posters are available.").editorialFont(.section)
                case .loadingArtwork:
                    ProgressView("Loading poster…")
                    action("Next puzzle", id: "puzzle-next", perform: model.next)
                case .artworkError:
                    Text("Couldn’t load this poster.").foregroundStyle(palette.error)
                        .accessibilityIdentifier("puzzle-artwork-error")
                    action("Retry", id: "puzzle-retry", perform: model.retry)
                    action("Next puzzle", id: "puzzle-next", perform: model.next)
                case .ready:
                    if let artwork = model.artwork { playable(artwork) }
                }
                if let movie = model.movie {
                    NavigationLink(value: LumioRoute.movie(movie.id)) {
                        Text("View details")
                            .editorialFont(.control)
                            .frame(minHeight: 44)
                            .contentShape(Rectangle())
                    }
                }
            }
            .frame(maxWidth: 480, alignment: .leading)
            .padding(20)
            .frame(maxWidth: .infinity)
        }
        .accessibilityIdentifier("puzzle-scroll")
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Poster Puzzle")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
        .task { model.startIfNeeded() }
    }

    private func playable(_ artwork: CGImage) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            difficultyControls
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) {
                    progress.fixedSize()
                    Spacer(minLength: 0)
                    moves.fixedSize()
                }
                VStack(alignment: .leading, spacing: 4) {
                    progress
                    moves
                }
            }
            .editorialFont(.control)
            .monospacedDigit()

            Group {
                if model.showingReference {
                    Image(artwork, scale: 1, label: Text("Poster reference"))
                        .resizable()
                        .scaledToFit()
                        .accessibilityIdentifier("puzzle-reference-image")
                } else {
                    PosterPuzzleTiles(
                        artwork: artwork, board: model.board, canMove: model.canMove,
                        choose: model.choose, swap: model.swap
                    )
                    .id(model.difficulty)
                }
            }
            .frame(maxWidth: 300)
            .frame(maxWidth: .infinity)

            if model.board.complete {
                Label {
                    Text("Poster complete!")
                        .editorialFont(.section)
                        .accessibilityIdentifier("puzzle-complete")
                } icon: {
                    LucideIcon(name: "check")
                }
                .foregroundStyle(palette.success)
            } else {
                Text(instructions)
                    .editorialFont(.metadata)
                    .foregroundStyle(palette.secondary)
            }
            action(model.showingReference ? "Hide reference" : "Show reference",
                   id: "puzzle-reference", perform: model.toggleReference)
            action("Next puzzle", id: "puzzle-next", perform: model.next)
            if model.board.complete {
                action("Back to Arcade", id: "puzzle-back", perform: { dismiss() })
            }
        }
    }

    private var instructions: LocalizedStringKey {
        if model.showingReference { return "Hide the reference to keep playing." }
        if model.board.selected != nil { return "Tap another tile to swap." }
        return "Drag tiles to swap them, or tap two tiles."
    }

    private var difficultyControls: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Board size").editorialFont(.control).accessibilityAddTraits(.isHeader)
            HStack(spacing: 8) {
                ForEach(PuzzleDifficulty.allCases) { size in
                    let selected = size == model.difficulty
                    Button {
                        model.changeDifficulty(size)
                    } label: {
                        Text(verbatim: "\(size.rawValue)×\(size.rawValue)")
                            .editorialFont(.control)
                            .foregroundStyle(selected ? palette.ink : palette.primary)
                            .frame(maxWidth: .infinity, minHeight: 44)
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
                    .accessibilityIdentifier("puzzle-size-\(size.rawValue)")
                }
            }
            Text("Changing the size reshuffles this poster.")
                .editorialFont(.metadata)
                .foregroundStyle(palette.secondary)
        }
    }

    private var progress: some View {
        Text("\(model.board.correct) of \(model.board.tiles.count) in place")
            .accessibilityIdentifier("puzzle-progress")
    }

    private var moves: some View {
        Text("Moves: \(model.board.moves)")
            .accessibilityIdentifier("puzzle-moves")
    }

    private func action(_ text: LocalizedStringKey, id: String, perform: @escaping () -> Void) -> some View {
        Button(action: perform) {
            Text(text)
                .editorialFont(.control)
                .padding(.horizontal, 14)
                .frame(minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.bordered)
        .accessibilityIdentifier(id)
    }
}
