import SwiftUI

struct ArcadeScreen: View {
    let trivia: TriviaModel
    let puzzle: PosterPuzzleModel
    @Environment(\.colorScheme) private var scheme

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Arcade")
                    .editorialFont(.title)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("screen-title")
                NavigationLink(value: LumioRoute.trivia) {
                    gameCard(
                        title: "Trivia", icon: "circle-question-mark",
                        description: "Test your movie knowledge in five timed questions. Questions are in English.",
                        action: trivia.hasRound ? "Return to round" : "Play Trivia"
                    )
                    .contentShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("arcade-trivia")
                NavigationLink(value: LumioRoute.posterPuzzle) {
                    gameCard(
                        title: "Poster Puzzle", icon: "puzzle",
                        description: "Put a movie poster back together.",
                        action: puzzle.hasPuzzle ? "Return to puzzle" : "Play Poster Puzzle"
                    )
                    .contentShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("arcade-puzzle")
            }
            .frame(maxWidth: 620, alignment: .leading)
            .padding(20)
            .frame(maxWidth: .infinity)
        }
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Arcade")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
    }

    private func gameCard(
        title: LocalizedStringKey, icon: String, description: LocalizedStringKey, action: LocalizedStringKey
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Label {
                Text(title).editorialFont(.section)
            } icon: {
                LucideIcon(name: icon).foregroundStyle(palette.accent)
            }
            Text(description).editorialFont(.body).foregroundStyle(palette.secondary)
            Text(action).editorialFont(.control).foregroundStyle(palette.accent)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(palette.raised, in: RoundedRectangle(cornerRadius: 12))
    }
}
