import Foundation
import Observation

@MainActor
@Observable
final class TriviaModel {
    enum Status: Equatable { case welcome, loading, unavailable, networkError, playing }

    private let repository: CatalogRepository
    private let playCue: (GameCue) -> Void
    private let now: () -> Duration
    @ObservationIgnored private var lastTick: Duration?
    private var visible = false
    private(set) var status = Status.welcome
    private(set) var round: TriviaRound?

    var isCounting: Bool { visible && round?.isCounting == true }

    var hasRound: Bool {
        status == .loading || (round != nil && round?.phase != .results)
    }

    init(repository: CatalogRepository, playCue: @escaping (GameCue) -> Void = { _ in }, now: (() -> Duration)? = nil) {
        self.repository = repository
        self.playCue = playCue
        let origin = ContinuousClock.now
        self.now = now ?? { origin.duration(to: .now) }
    }

    func start() {
        guard status != .loading else { return }
        status = .loading
        round = nil
        lastTick = nil
        Task { [weak self, repository] in
            do {
                let catalog = try await repository.load()
                let questions = TriviaRound.validQuestions(catalog.trivia)
                guard let self, !Task.isCancelled else { return }
                guard questions.count >= TriviaRound.questionCount else {
                    status = .unavailable
                    return
                }
                var fresh = TriviaRound(questions: Array(questions.shuffled().prefix(TriviaRound.questionCount)))
                if !visible { fresh.pause() }
                round = fresh
                status = .playing
                resetClock(at: now())
            } catch {
                guard !Task.isCancelled else { return }
                self?.status = .networkError
            }
        }
    }

    func setVisible(_ visible: Bool) {
        guard visible != self.visible else { return }
        if !visible { pause() }
        self.visible = visible
    }

    func refreshClock() {
        sync(at: now())
    }

    func answer(_ option: Int, question index: Int) {
        update { if $0.index == index { $0.answer(option) } }
    }

    func useFiftyFifty(question index: Int) {
        update { if $0.index == index { $0.useFiftyFifty() } }
    }

    func hold(question index: Int) {
        update { if $0.index == index { $0.hold() } }
    }

    func next(question index: Int) {
        update { if $0.index == index { $0.next() } }
    }

    func pause() {
        update { $0.pause() }
    }

    func resume() {
        guard visible else { return }
        update { $0.resume() }
    }

    func returnToWelcome() {
        guard round?.phase == .results else { return }
        lastTick = nil
        round = nil
        status = .welcome
    }

    private func update(_ action: (inout TriviaRound) -> Void) {
        let timestamp = now()
        sync(at: timestamp)
        guard var round else { return }
        action(&round)
        apply(round)
        resetClock(at: timestamp)
    }

    private func sync(at timestamp: Duration) {
        guard let lastTick, var round, round.isCounting else { return }
        round.advanceTime(timestamp - lastTick)
        apply(round)
        resetClock(at: timestamp)
    }

    private func apply(_ updated: TriviaRound) {
        let revealed = round?.phase == .question && updated.phase == .feedback
        round = updated
        if revealed {
            if updated.selected == nil { playCue(.timeout) }
            else { playCue(updated.earned > 0 ? .correct : .incorrect) }
        }
    }

    private func resetClock(at timestamp: Duration) {
        lastTick = round?.isCounting == true ? timestamp : nil
    }
}
