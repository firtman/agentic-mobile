import Foundation

struct TriviaRound {
    enum Phase: Hashable { case question, feedback, results }

    static let questionCount = 5
    static let questionTime: Duration = .seconds(20)
    static let feedbackTime: Duration = .seconds(4)

    let questions: [TriviaQuestion]
    private(set) var index = 0
    private(set) var phase = Phase.question
    private(set) var remaining = questionTime
    private(set) var score = 0
    private(set) var streak = 0
    private(set) var bestStreak = 0
    private(set) var correctCount = 0
    private(set) var selected: Int?
    private(set) var earned = 0
    private(set) var fiftyUsed = false
    private(set) var eliminated: Set<Int> = []
    private(set) var paused = false
    private(set) var holding = false

    init(questions: [TriviaQuestion]) {
        precondition(questions.count == Self.questionCount)
        self.questions = questions
    }

    var question: TriviaQuestion { questions[index] }
    var isCounting: Bool { !paused && (phase == .question || (phase == .feedback && !holding)) }

    static func validQuestions(_ questions: [TriviaQuestion]) -> [TriviaQuestion] {
        var ids = Set<String>()
        return questions.filter { question in
            let options = question.options.map {
                $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            }
            return !question.id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && !question.question.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && options.count == 4 && question.options.indices.contains(question.answerIndex)
                && !options.contains("") && Set(options).count == 4
                && ids.insert(question.id).inserted
        }
    }

    mutating func advanceTime(_ elapsed: Duration) {
        guard isCounting, elapsed > .zero else { return }
        if elapsed < remaining {
            remaining -= elapsed
        } else if phase == .question {
            answer(nil)
        } else {
            next()
        }
    }

    mutating func answer(_ option: Int?) {
        guard !paused, phase == .question else { return }
        if let option, !question.options.indices.contains(option) || eliminated.contains(option) { return }
        let correct = option == question.answerIndex
        streak = correct ? streak + 1 : 0
        earned = correct ? 100 + min(streak - 1, 4) * 25 : 0
        if correct { correctCount += 1 }
        bestStreak = max(bestStreak, streak)
        score += earned
        selected = option
        phase = .feedback
        remaining = Self.feedbackTime
    }

    mutating func useFiftyFifty() {
        guard !paused, phase == .question, !fiftyUsed else { return }
        let wrong = question.options.indices.filter { $0 != question.answerIndex }
        eliminated = Set(wrong.shuffled().prefix(2))
        fiftyUsed = true
    }

    mutating func next() {
        guard !paused, phase == .feedback else { return }
        holding = false
        if index == questions.count - 1 {
            phase = .results
            remaining = .zero
        } else {
            index += 1
            phase = .question
            remaining = Self.questionTime
            selected = nil
            earned = 0
            eliminated = []
        }
    }

    mutating func hold() {
        if phase == .feedback && !paused { holding = true }
    }

    mutating func pause() {
        if phase != .results { paused = true }
    }

    mutating func resume() {
        paused = false
    }
}
