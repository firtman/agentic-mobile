import SwiftUI

struct RatingHistoryScreen: View {
    let ratings: RatingsStore

    @Environment(\.colorScheme) private var scheme
    @State private var state: LoadState = .loading
    @State private var editing: RatingHistoryItem?
    @State private var feedback: LocalizedStringKey?

    private enum LoadState {
        case loading, loaded([RatingHistoryItem]), failed
    }

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                Text("Your choices")
                    .editorialFont(.title)
                    .accessibilityAddTraits(.isHeader)

                if let feedback {
                    Text(feedback)
                        .editorialFont(.body)
                        .accessibilityIdentifier("history-feedback")
                }

                switch state {
                case .loading:
                    ProgressView("Loading your choices…")
                case .failed:
                    Text("Couldn’t load your saved choices.").foregroundStyle(palette.error)
                    Button("Retry", action: reload)
                        .buttonStyle(.bordered)
                        .controlSize(.large)
                case .loaded(let items) where items.isEmpty:
                    Text("No saved choices yet")
                        .editorialFont(.section)
                        .accessibilityIdentifier("history-empty")
                    Text("Rate a movie in Swipe or Movie Details to see it here.")
                        .editorialFont(.body)
                        .foregroundStyle(palette.secondary)
                case .loaded(let items):
                    ForEach(items) { item in
                        row(item)
                    }
                }
            }
            .frame(maxWidth: 640, alignment: .leading)
            .padding(20)
            .frame(maxWidth: .infinity)
        }
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Your choices")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
        .task(id: ratings.revision) { reload() }
        .sheet(item: $editing) { item in
            RatingHistoryEditor(item: item) { choice in
                guard try ratings.changeChoice(for: item, to: choice) else { return false }
                feedback = "Choice saved"
                return true
            }
        }
    }

    private func row(_ item: RatingHistoryItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            NavigationLink(value: LumioRoute.movie(item.id)) {
                HStack(alignment: .top, spacing: 14) {
                    MoviePoster(id: item.id, title: item.title, posterURL: item.posterURL, aspect: 2.0 / 3)
                        .frame(width: 72)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    VStack(alignment: .leading, spacing: 5) {
                        Text(item.title).editorialFont(.movieTitle)
                        if let year = item.year, year > 0 {
                            Text(String(year)).editorialFont(.metadata).foregroundStyle(palette.secondary)
                        }
                        Text(item.choice.label)
                            .editorialFont(.control)
                            .accessibilityIdentifier("history-rating")
                        Text(item.updatedAt, format: .dateTime.year().month().day().hour().minute())
                            .editorialFont(.metadata)
                            .foregroundStyle(palette.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    LucideIcon(name: "arrow-left")
                        .rotationEffect(.degrees(180))
                        .foregroundStyle(palette.accent)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint("View details")
            .accessibilityIdentifier("history-details")

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 10) { rowActions(item, singleLine: true) }
                VStack(alignment: .leading, spacing: 8) { rowActions(item, singleLine: false) }
            }
            .buttonStyle(.bordered)
        }
        .padding(14)
        .background(palette.raised, in: RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("history-row")
    }

    @ViewBuilder
    private func rowActions(_ item: RatingHistoryItem, singleLine: Bool) -> some View {
        Button {
            feedback = nil
            editing = item
        } label: {
            Text("Change choice").editorialFont(.control)
                .fixedSize(horizontal: singleLine, vertical: false)
                .frame(minHeight: 44)
        }
        .accessibilityLabel("Change choice for \(item.title)")
        .accessibilityIdentifier("history-edit")

        Button {
            undo(item)
        } label: {
            Label {
                Text("Undo").editorialFont(.control)
                    .fixedSize(horizontal: singleLine, vertical: false)
            } icon: {
                LucideIcon(name: "undo-2")
            }
            .frame(minHeight: 44)
        }
        .disabled(item.undoID == nil)
        .accessibilityLabel("Undo choice for \(item.title)")
        .accessibilityIdentifier("history-undo")
    }

    private func reload() {
        do {
            state = .loaded(try ratings.history())
        } catch {
            state = .failed
        }
    }

    private func undo(_ item: RatingHistoryItem) {
        guard let token = item.undoID else { return }
        do {
            if try ratings.undoMovie(item.id, expectedID: token) {
                feedback = "Choice undone"
            } else {
                feedback = "This change can no longer be undone."
                reload()
            }
        } catch {
            feedback = "Couldn’t update your choice. Try again."
        }
    }
}

private struct RatingHistoryEditor: View {
    let item: RatingHistoryItem
    let save: (RatingChoice) throws -> Bool

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var choice: RatingChoice
    @State private var saveError: LocalizedStringKey?
    @State private var stale = false

    init(item: RatingHistoryItem, save: @escaping (RatingChoice) throws -> Bool) {
        self.item = item
        self.save = save
        _choice = State(initialValue: item.choice)
    }

    var body: some View {
        let palette = EditorialPalette(scheme)
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(item.title).editorialFont(.title)
                    ForEach(RatingChoice.allCases) { option in
                        Button { choice = option } label: {
                            HStack {
                                LucideIcon(name: option.icon)
                                Text(option.label).editorialFont(.control)
                                Spacer()
                                if option == choice {
                                    LucideIcon(name: "check")
                                }
                            }
                            .padding(.horizontal, 12)
                            .frame(minHeight: 48)
                            .foregroundStyle(option == choice ? palette.ink : palette.primary)
                            .background(option == choice ? palette.coral : palette.raised,
                                        in: RoundedRectangle(cornerRadius: 8))
                            .overlay {
                                RoundedRectangle(cornerRadius: 8)
                                    .strokeBorder(option == choice ? palette.coral : palette.outline)
                                    .allowsHitTesting(false)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(option == choice ? .isSelected : [])
                        .accessibilityIdentifier("history-option-\(option.rawValue)")
                    }
                    if let saveError {
                        Text(saveError).editorialFont(.body).foregroundStyle(palette.error)
                    }
                }
                .padding(20)
            }
            .background(palette.canvas)
            .foregroundStyle(palette.primary)
            .navigationTitle("Change choice")
            .navigationBarTitleDisplayMode(.inline)
            .editorialNavigationBar(canvas: palette.canvas)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save choice", action: commit)
                        .disabled(choice == item.choice || stale)
                        .accessibilityIdentifier("history-save")
                }
            }
        }
    }

    private func commit() {
        do {
            if try save(choice) {
                dismiss()
            } else {
                stale = true
                saveError = "This choice has changed. Close and try again."
            }
        } catch {
            saveError = "Couldn’t update your choice. Try again."
        }
    }
}
