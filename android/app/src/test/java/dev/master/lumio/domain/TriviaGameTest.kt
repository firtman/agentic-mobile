package dev.master.lumio.domain

import dev.master.lumio.data.TriviaQuestion
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class TriviaGameTest {
    private fun q(id: String) = TriviaQuestion(id, "movie-$id", "synthetic", "Question $id?", listOf("A", "B", "C", "D"), 1)
    private fun round(count: Int = 5) = TriviaRound((1..count).map { q("$it") }, 20)

    @Test fun selectionRejectsInvalidQuestionsAndDuplicatesAndSamplesWithoutReplacement() {
        val valid = (1..7).map { q("$it") }
        val invalid = listOf(q(" "), q("bad-question").copy(question = " "), q("bad-size").copy(options = listOf("A")),
            q("bad-answer").copy(answerIndex = 4), q("blank").copy(options = listOf("A", " ", "C", "D")),
            q("duplicate").copy(options = listOf("A", " a ", "C", "D")))
        val selected = selectTrivia(valid + valid[0] + invalid, 5, Random(12))
        assertEquals(5, selected.size)
        assertEquals(5, selected.map { it.id }.toSet().size)
        assertTrue(selected.all { it in valid })
        assertEquals(valid.toSet(), selectTrivia(valid + invalid, 20, Random(8)).toSet())
    }

    @Test fun scoringStreakBonusCapsAndWrongAnswerResets() {
        var game = round(7)
        val earned = mutableListOf<Int>()
        repeat(6) {
            game = game.answer(1)
            earned += game.earned
            game = game.next()
        }
        assertEquals(listOf(100, 125, 150, 175, 200, 200), earned)
        assertEquals(950, game.score)
        assertEquals(6, game.bestStreak)
        game = game.answer(0)
        assertEquals(0, game.earned)
        assertEquals(0, game.streak)
        assertEquals(6, game.correctCount)
        assertEquals(TriviaPhase.RESULTS, game.next().phase)
    }

    @Test fun timeoutAndFeedbackUseElapsedTimeAndManualNext() {
        var game = round()
        assertEquals(20_000L, game.elapse(-1).remainingMs)
        game = game.elapse(19_999)
        assertEquals(1L, game.remainingMs)
        game = game.elapse(1)
        assertEquals(TriviaPhase.FEEDBACK, game.phase)
        assertNull(game.selected)
        assertEquals(0, game.score)
        assertEquals(0, game.streak)
        assertEquals(1L, game.elapse(3_999).feedbackMs)
        assertEquals(1, game.elapse(4_000).index)
        assertEquals(1, game.next().index)
        assertEquals(20_000L, game.next().remainingMs)
    }

    @Test fun fiftyFiftyIsOncePerRoundPreservesCorrectAndCannotAnswerEliminated() {
        var game = round().fiftyFifty(Random(1))
        assertTrue(game.fiftyUsed)
        assertEquals(2, game.eliminated.size)
        assertFalse(1 in game.eliminated)
        assertEquals(game, game.fiftyFifty(Random(2)))
        assertEquals(game, game.answer(game.eliminated.first()))
        game = game.answer(1)
        assertEquals(game, game.answer(1))
        game = game.next()
        assertTrue(game.fiftyUsed)
        assertTrue(game.eliminated.isEmpty())
        assertEquals(game, game.fiftyFifty(Random(3)))
        assertFalse(round().fiftyUsed)
    }

    @Test fun pauseAndHoldKeepTimeAndActionsInert() {
        val paused = round().copy(paused = true)
        assertEquals(paused, paused.elapse(30_000))
        assertEquals(paused, paused.answer(1))
        assertEquals(paused, paused.fiftyFifty(Random(1)))
        val feedback = round().answer(1).copy(holdFeedback = true)
        assertEquals(feedback, feedback.elapse(9_000))
        assertEquals(1, feedback.next().index)
        val pausedFeedback = feedback.copy(paused = true)
        assertEquals(pausedFeedback, pausedFeedback.next())
        assertEquals(pausedFeedback, pausedFeedback.elapse(9_000))
    }

    @Test fun fiveQuestionsReachResultsAndResultsRemainInert() {
        var game = round()
        repeat(5) { game = game.answer(1).elapse(4_000) }
        assertEquals(TriviaPhase.RESULTS, game.phase)
        assertEquals(5, game.correctCount)
        assertEquals(750, game.score)
        assertEquals(game, game.answer(1))
        assertEquals(game, game.elapse(99_000))
        assertEquals(game, game.next())
    }
}
