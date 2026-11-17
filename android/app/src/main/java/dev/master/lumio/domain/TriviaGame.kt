package dev.master.lumio.domain

import dev.master.lumio.data.TriviaQuestion
import kotlin.random.Random

/** Catalog content stays in memory; only validated four-choice source questions enter a round. */
fun selectTrivia(questions: List<TriviaQuestion>, count: Int, random: Random): List<TriviaQuestion> =
    questions.filter {
        it.id.isNotBlank() && it.question.isNotBlank() && it.options.size == 4 &&
            it.answerIndex in 0..3 && it.options.all(String::isNotBlank) &&
            it.options.map { option -> option.trim().lowercase(java.util.Locale.ROOT) }.distinct().size == 4
    }.distinctBy { it.id }.shuffled(random).take(count)

enum class TriviaPhase { QUESTION, FEEDBACK, RESULTS }
data class TriviaRound(
    val questions: List<TriviaQuestion>,
    val secondsPerQuestion: Int,
    val index: Int = 0,
    val phase: TriviaPhase = TriviaPhase.QUESTION,
    val remainingMs: Long = secondsPerQuestion * 1_000L,
    val feedbackMs: Long = 4_000L,
    val score: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val correctCount: Int = 0,
    val selected: Int? = null,
    val earned: Int = 0,
    val fiftyUsed: Boolean = false,
    val eliminated: Set<Int> = emptySet(),
    val paused: Boolean = false,
    val holdFeedback: Boolean = false,
) {
    init { require(questions.isNotEmpty()); require(secondsPerQuestion > 0) }
    val question: TriviaQuestion get() = questions[index]

    fun answer(option: Int?): TriviaRound {
        if (paused || phase != TriviaPhase.QUESTION || (option != null && (option !in 0..3 || option in eliminated))) return this
        val correct = option == question.answerIndex
        val nextStreak = if (correct) streak + 1 else 0
        val points = if (correct) 100 + (nextStreak - 1).coerceAtMost(4) * 25 else 0
        return copy(phase = TriviaPhase.FEEDBACK, selected = option, earned = points,
            score = score + points, streak = nextStreak, bestStreak = maxOf(bestStreak, nextStreak),
            correctCount = correctCount + if (correct) 1 else 0, feedbackMs = 4_000, holdFeedback = false)
    }

    /** Remaining time is supplied from a monotonic clock by the controller, never frame counts. */
    fun elapse(elapsedMs: Long): TriviaRound {
        if (paused || elapsedMs <= 0) return this
        return when (phase) {
            TriviaPhase.QUESTION -> if (elapsedMs >= remainingMs) copy(remainingMs = 0).answer(null)
                else copy(remainingMs = remainingMs - elapsedMs)
            TriviaPhase.FEEDBACK -> if (holdFeedback) this else if (elapsedMs >= feedbackMs) next()
                else copy(feedbackMs = feedbackMs - elapsedMs)
            TriviaPhase.RESULTS -> this
        }
    }

    fun fiftyFifty(random: Random): TriviaRound {
        if (paused || phase != TriviaPhase.QUESTION || fiftyUsed) return this
        val wrong = (0..3).filter { it != question.answerIndex }.shuffled(random).take(2).toSet()
        return copy(fiftyUsed = true, eliminated = wrong)
    }

    fun next(): TriviaRound {
        if (paused || phase != TriviaPhase.FEEDBACK) return this
        return if (index == questions.lastIndex) copy(phase = TriviaPhase.RESULTS, holdFeedback = false)
        else copy(index = index + 1, phase = TriviaPhase.QUESTION, remainingMs = secondsPerQuestion * 1_000L,
            selected = null, earned = 0, eliminated = emptySet(), holdFeedback = false, feedbackMs = 4_000)
    }
}
