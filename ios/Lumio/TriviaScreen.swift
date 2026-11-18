import SwiftUI

struct TriviaScreen: View {
    let model: TriviaModel
    let covered: Bool
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isOnScreen = false

    private enum Anchor: Hashable { case top, feedback }
    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Color.clear.frame(height: 0).id(Anchor.top).accessibilityHidden(true)
                    switch model.status {
                    case .welcome:
                        welcome
                    case .loading:
                        ProgressView("Loading questions…")
                    case .networkError:
                        Text("Couldn’t connect to the catalog.")
                            .editorialFont(.body)
                            .foregroundStyle(palette.error)
                        action("Retry", id: "trivia-retry", perform: model.start)
                    case .unavailable:
                        Text("There aren’t enough questions for a round.")
                            .editorialFont(.section)
                        action("Back to Arcade", id: "trivia-back", perform: { dismiss() })
                    case .playing:
                        if let round = model.round {
                            if round.paused {
                                paused
                            } else if round.phase == .results {
                                results(round)
                            } else {
                                game(round)
                            }
                        }
                    }
                }
                .frame(maxWidth: 620, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
                .frame(maxWidth: .infinity)
            }
            .onChange(of: model.round?.phase) { scrollToCurrentPhase(using: proxy) }
            .onChange(of: model.round?.paused) {
                proxy.scrollTo(Anchor.top, anchor: .top)
            }
        }
        .background(palette.canvas)
        .foregroundStyle(palette.primary)
        .navigationTitle("Trivia")
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
        .onAppear {
            isOnScreen = true
            updateVisibility()
        }
        .onDisappear {
            isOnScreen = false
            model.setVisible(false)
            model.returnToWelcome()
        }
        .onChange(of: scenePhase) { updateVisibility() }
        .onChange(of: covered) { updateVisibility() }
        .task(id: model.isCounting) {
            guard model.isCounting else { return }
            while model.isCounting && !Task.isCancelled {
                do {
                    try await Task.sleep(for: .milliseconds(100))
                } catch { break }
                model.refreshClock()
            }
        }
    }

    private func scrollToCurrentPhase(using proxy: ScrollViewProxy) {
        let showingFeedback = model.round?.phase == TriviaRound.Phase.feedback
        let target = showingFeedback ? Anchor.feedback : Anchor.top
        let alignment = showingFeedback ? UnitPoint.bottom : UnitPoint.top
        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.22)) {
            proxy.scrollTo(target, anchor: alignment)
        }
    }

    private func updateVisibility() {
        model.setVisible(isOnScreen && scenePhase == .active && !covered)
    }

    private var welcome: some View {
        VStack(alignment: .leading, spacing: 14) {
            Color.clear
                .aspectRatio(2.25, contentMode: .fit)
                .overlay {
                    GeometryReader { geometry in
                        Image("trivia-ticket")
                            .resizable()
                            .scaledToFill()
                            .frame(width: geometry.size.width, height: geometry.size.height)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .accessibilityHidden(true)
            Text("Your ticket to movie night")
                .editorialFont(.title)
                .accessibilityAddTraits(.isHeader)
            Text("A little cinema knowledge. A streak worth chasing.")
                .editorialFont(.body)
                .foregroundStyle(palette.secondary)
            VStack(alignment: .leading, spacing: 5) {
                Text("5 questions · 20 seconds each")
                Text("One 50:50 · Streak bonus")
            }
            .editorialFont(.control)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            .background(palette.raised, in: TriviaTicketShape())
            action("Let’s play", icon: "play", id: "trivia-start", perform: model.start)
        }
    }

    private var paused: some View {
        VStack(alignment: .leading, spacing: 14) {
            Label {
                Text("Round paused").editorialFont(.title)
            } icon: { LucideIcon(name: "pause") }
            .accessibilityIdentifier("trivia-paused")
            Text("Resume when you’re ready.")
                .editorialFont(.body)
                .foregroundStyle(palette.secondary)
            action("Resume", icon: "play", id: "trivia-resume", perform: model.resume)
        }
    }

    private func game(_ round: TriviaRound) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            roundHeader(round)
            if round.phase == .question {
                let seconds = round.remaining.components
                let displayed = seconds.seconds + (seconds.attoseconds > 0 ? 1 : 0)
                ProgressView(value: Double(displayed), total: 20) {
                    Text("\(displayed)s left")
                        .editorialFont(.control)
                        .monospacedDigit()
                        .accessibilityIdentifier("trivia-timer")
                }
                .tint(displayed <= 5 ? palette.error : palette.accent)
            }
            Text(round.question.question)
                .editorialFont(.section)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("trivia-question")
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 24)
                .padding(.vertical, 18)
                .background(palette.raised, in: TriviaTicketShape())
                .id(round.question.id)
                .transition(reduceMotion ? .identity : .opacity)
            ForEach(round.question.options.indices, id: \.self) { option in
                answer(option, round: round)
            }
            if round.phase == .feedback {
                feedback(round)
                    .id(Anchor.feedback)
                    .transition(reduceMotion ? .identity : .opacity)
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: round.phase)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: round.index)
    }

    private func roundHeader(_ round: TriviaRound) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 5) {
                ForEach(round.questions.indices, id: \.self) { index in
                    Capsule()
                        .fill(index <= round.index ? palette.coral : palette.divider)
                        .frame(height: 5)
                }
            }
            .accessibilityHidden(true)
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 10) {
                    counter(round).fixedSize()
                    Spacer(minLength: 0)
                    roundControls(round)
                }
                VStack(alignment: .leading, spacing: 4) {
                    counter(round)
                    roundControls(round)
                }
            }
            HStack(spacing: 12) {
                Text("Score: \(round.score)")
                    .accessibilityIdentifier("trivia-score")
                Text("Streak: \(round.streak)")
            }
            .editorialFont(.control)
            .monospacedDigit()
        }
    }

    private func counter(_ round: TriviaRound) -> some View {
        Text("Question \(round.index + 1) of \(round.questions.count)")
            .editorialFont(.control)
            .accessibilityIdentifier("trivia-counter")
    }

    private func roundControls(_ round: TriviaRound) -> some View {
        HStack(spacing: 8) {
            Button {
                model.useFiftyFifty(question: round.index)
            } label: {
                Text(verbatim: "50:50")
                    .editorialFont(.control)
                    .frame(minHeight: 44)
            }
            .buttonStyle(.bordered)
            .disabled(round.fiftyUsed || round.phase != .question)
            .accessibilityIdentifier("trivia-lifeline")
            Button(action: model.pause) {
                LucideIcon(name: "pause").frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Pause")
            .accessibilityIdentifier("trivia-pause")
        }
        .fixedSize(horizontal: true, vertical: false)
    }

    private func answer(_ index: Int, round: TriviaRound) -> some View {
        let reveal = round.phase == .feedback
        let correct = reveal && index == round.question.answerIndex
        let wrong = reveal && round.selected == index && !correct
        let eliminated = round.eliminated.contains(index)
        let tone = correct ? palette.success : wrong ? palette.error : palette.outline
        return Button {
            model.answer(index, question: round.index)
        } label: {
            HStack(alignment: .center, spacing: 8) {
                Text(["A", "B", "C", "D"][index])
                    .editorialFont(.control)
                    .frame(minWidth: 22)
                    .accessibilityHidden(true)
                Text(round.question.options[index])
                    .editorialFont(.body)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if correct || wrong {
                    LucideIcon(name: correct ? "check" : "x").foregroundStyle(tone)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .frame(minHeight: 48)
            .foregroundStyle(correct || wrong ? tone : palette.primary)
            .background(palette.raised, in: RoundedRectangle(cornerRadius: 9))
            .overlay {
                RoundedRectangle(cornerRadius: 9)
                    .strokeBorder(tone, lineWidth: correct || wrong ? 2 : 1)
                    .allowsHitTesting(false)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(reveal || eliminated)
        .opacity(eliminated ? 0.43 : 1)
        .accessibilityIdentifier("trivia-answer-\(index)")
    }

    private func feedback(_ round: TriviaRound) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            let correct = round.earned > 0
            let title: LocalizedStringKey = correct ? "That’s right!" : round.selected == nil ? "Time’s up" : "Good try"
            Label {
                Text(title)
                    .editorialFont(.section)
            } icon: {
                LucideIcon(name: correct ? "check" : "x")
            }
            .foregroundStyle(correct ? palette.success : palette.error)
            .accessibilityIdentifier("trivia-feedback")
            if correct {
                Text("+\(round.earned) points").editorialFont(.control)
            } else {
                Text("Correct answer: \(round.question.options[round.question.answerIndex])")
                    .editorialFont(.body)
            }
            if !round.holding {
                Button("Keep reading") { model.hold(question: round.index) }
                    .frame(minHeight: 44)
                    .accessibilityIdentifier("trivia-hold")
            }
            action(round.index == round.questions.count - 1 ? "See results" : "Next",
                   id: "trivia-next", perform: { model.next(question: round.index) })
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(palette.raised, in: RoundedRectangle(cornerRadius: 12))
    }

    private func results(_ round: TriviaRound) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Round complete")
                .editorialFont(.title)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("trivia-results")
            VStack(alignment: .leading, spacing: 8) {
                Text("Score: \(round.score)").editorialFont(.section)
                Text("Correct: \(round.correctCount) of \(round.questions.count)")
                Text("Best streak: \(round.bestStreak)")
            }
            .editorialFont(.body)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
            .padding(.vertical, 18)
            .background(palette.raised, in: TriviaTicketShape())
            action("Play again", icon: "rotate-ccw", id: "trivia-replay", perform: model.start)
            Button("Back to Arcade") { dismiss() }.frame(minHeight: 44)
        }
    }

    private func action(
        _ title: LocalizedStringKey, icon: String? = nil, id: String, perform: @escaping () -> Void
    ) -> some View {
        Button(action: perform) {
            HStack(spacing: 8) {
                if let icon { LucideIcon(name: icon) }
                Text(title).editorialFont(.control)
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .padding(.horizontal, 12)
            .foregroundStyle(palette.ink)
            .background(palette.coral, in: RoundedRectangle(cornerRadius: 10))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(id)
    }
}

private struct TriviaTicketShape: Shape {
    func path(in rect: CGRect) -> Path {
        let middle = rect.midY
        let notch = min(12, rect.height / 4)
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: middle - notch))
        path.addQuadCurve(to: CGPoint(x: rect.maxX, y: middle + notch),
                          control: CGPoint(x: rect.maxX - notch * 2, y: middle))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: middle + notch))
        path.addQuadCurve(to: CGPoint(x: rect.minX, y: middle - notch),
                          control: CGPoint(x: rect.minX + notch * 2, y: middle))
        path.closeSubpath()
        return path
    }
}
