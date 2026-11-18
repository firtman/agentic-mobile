import SwiftUI

struct SwipeScreen: View {
    let model: SwipeModel
    let ratings: RatingsStore

    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        // Drags that start on the card rate it. Drags elsewhere scroll when the content overflows.
        ScrollView { content }
            .accessibilityIdentifier("swipe-scroll")
        .foregroundStyle(palette.primary)
        .background(palette.canvas)
        .onAppear(perform: model.sync)
        .onChange(of: ratings.revision) { model.sync() }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            switch model.status {
            case .loading:
                ProgressView("Loading movies…")
                    .frame(maxWidth: .infinity, minHeight: 200)
            case .failed:
                retryMessage("Couldn’t connect to the catalog.", icon: "wifi-off", action: "Retry")
            case .storageFailed:
                retryMessage("Couldn’t load your saved choices.", icon: "wifi-off", action: "Retry")
            case .exhausted:
                exhausted
            case .card:
                if let movie = model.current { card(movie) }
            }
        }
        .frame(maxWidth: 520, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline) {
                Text("A little more you.")
                    .editorialFont(.title)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("screen-title")
                Spacer(minLength: 8)
                Button(action: model.undo) {
                    Label {
                        Text("Undo").editorialFont(.control)
                    } icon: {
                        LucideIcon(name: "undo-2")
                    }
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .foregroundStyle(model.undoID == nil ? palette.secondary.opacity(0.5) : palette.accent)
                .disabled(model.undoID == nil)
                .accessibilityIdentifier("swipe-undo")
            }
            Text("Your choices shape your recommendations.")
                .editorialFont(.body)
                .foregroundStyle(palette.secondary)
            if let feedback = model.feedback {
                feedbackText(feedback)
            }
        }
    }

    @ViewBuilder
    private func feedbackText(_ feedback: SwipeModel.Feedback) -> some View {
        switch feedback {
        case .undone:
            Text("Choice undone")
                .editorialFont(.control)
                .foregroundStyle(palette.success)
                .accessibilityIdentifier("swipe-undone")
        case .stale:
            message("This change can no longer be undone.", icon: "x", color: palette.error)
        case .saveFailed, .undoFailed:
            message("Couldn’t update your choice. Try again.", icon: "x", color: palette.error)
                .accessibilityIdentifier("swipe-save-failed")
        }
    }

    private func card(_ movie: Movie) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SwipeCard(movie: movie, palette: palette, reduceMotion: reduceMotion, rate: rate)
                .id(movie.id)
                .transition(reduceMotion ? .identity : .opacity)
            VStack(alignment: .leading, spacing: 4) {
                Text(movie.title)
                    .editorialFont(.section)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityIdentifier("swipe-title")
                if let metadata = metadata(movie) {
                    Text(metadata)
                        .editorialFont(.metadata)
                        .foregroundStyle(palette.secondary)
                }
                NavigationLink(value: LumioRoute.movie(movie.id)) {
                    Text("View details")
                        .editorialFont(.control)
                        .foregroundStyle(palette.accent)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .accessibilityIdentifier("swipe-details")
            }
            HStack(spacing: 8) {
                ForEach([RatingChoice.dislike, .unseen, .like]) { choice in
                    actionButton(choice)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            Text("Left to dislike · Down for not seen yet · Right to like")
                .editorialFont(.metadata)
                .foregroundStyle(palette.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func actionButton(_ choice: RatingChoice) -> some View {
        Button { rate(choice) } label: {
            VStack(spacing: 6) {
                LucideIcon(name: choice.icon)
                    .padding(.top, 4)
                Text(choice.label)
                    .editorialFont(.control)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(8)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .frame(minHeight: 80)
            .foregroundStyle(palette.primary)
            .background(palette.raised, in: RoundedRectangle(cornerRadius: 8))
            .overlay {
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(palette.outline)
                    .allowsHitTesting(false)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("swipe-\(choice.rawValue)")
    }

    private var exhausted: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("You’re all caught up.")
                .editorialFont(.section)
                .accessibilityIdentifier("swipe-empty")
            Text("Every available movie has a saved choice. Undo a choice or check for new movies.")
                .editorialFont(.body)
                .foregroundStyle(palette.secondary)
            outlinedButton("Refresh movies", action: model.retry)
                .accessibilityIdentifier("swipe-refresh")
        }
        .padding(.vertical, 24)
    }

    private func retryMessage(_ text: LocalizedStringKey, icon: String, action: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            message(text, icon: icon, color: palette.error)
            outlinedButton(action, action: model.retry)
                .accessibilityIdentifier("swipe-retry")
        }
        .padding(.vertical, 24)
    }

    private func outlinedButton(_ title: LocalizedStringKey, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .editorialFont(.control)
                .padding(.horizontal, 16)
                .frame(minHeight: 44)
                .foregroundStyle(palette.ink)
                .background(palette.coral, in: RoundedRectangle(cornerRadius: 8))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func message(_ title: LocalizedStringKey, icon: String, color: Color) -> some View {
        Label {
            Text(title).editorialFont(.body)
        } icon: {
            LucideIcon(name: icon)
        }
        .foregroundStyle(color)
    }

    private func rate(_ choice: RatingChoice) {
        withAnimation(reduceMotion ? nil : .easeOut(duration: 0.2)) {
            model.rate(choice)
        }
    }

    private func metadata(_ movie: Movie) -> String? {
        let genres = (movie.genres ?? []).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        let parts = [movie.year.flatMap { $0 > 0 ? String($0) : nil }] + genres.prefix(3).map(Optional.some)
        let text = parts.compactMap { $0 }.joined(separator: " · ")
        return text.isEmpty ? nil : text
    }
}

private struct SwipeCard: View {
    let movie: Movie
    let palette: EditorialPalette
    let reduceMotion: Bool
    let rate: (RatingChoice) -> Void

    @State private var drag = CGSize.zero
    @ScaledMetric(relativeTo: .body) private var frameHeight: CGFloat = 300

    var body: some View {
        let pending = swipeChoice(for: drag)
        MoviePoster(movie: movie, aspect: 2.0 / 3, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(12)
            .frame(height: min(frameHeight, 420))
            .background(palette.subtle, in: RoundedRectangle(cornerRadius: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(palette.divider)
                    .allowsHitTesting(false)
            }
            .overlay(alignment: .top) {
                if let pending {
                    Text(pending.label)
                        .editorialFont(.control)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .foregroundStyle(palette.ink)
                        .background(palette.coral, in: RoundedRectangle(cornerRadius: 8))
                        .padding(16)
                        .accessibilityHidden(true)
                }
            }
            .offset(reduceMotion ? .zero : CGSize(
                width: drag.width.clamped(to: -120...120),
                height: drag.height.clamped(to: -90...90)
            ))
            .rotationEffect(.degrees(reduceMotion ? 0 : Double(drag.width / 50).clamped(to: -8...8)))
            .contentShape(Rectangle())
            .highPriorityGesture(
                DragGesture(minimumDistance: 10)
                    .onChanged { drag = $0.translation }
                    .onEnded { value in
                        let selected = swipeChoice(for: value.translation)
                        withAnimation(reduceMotion ? nil : .spring(duration: 0.25)) { drag = .zero }
                        if let selected { rate(selected) }
                    }
            )
            .sensoryFeedback(.selection, trigger: pending)
            .accessibilityElement()
            .accessibilityLabel(movie.title)
            .accessibilityHint("Left to dislike · Down for not seen yet · Right to like")
            .accessibilityAction(named: Text("Like")) { rate(.like) }
            .accessibilityAction(named: Text("Dislike")) { rate(.dislike) }
            .accessibilityAction(named: Text("Not seen yet")) { rate(.unseen) }
            .accessibilityIdentifier("swipe-card")
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
