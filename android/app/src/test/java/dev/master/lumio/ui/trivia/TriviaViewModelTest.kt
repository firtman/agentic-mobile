package dev.master.lumio.ui.trivia

import dev.master.lumio.data.*
import androidx.lifecycle.ViewModelStore
import dev.master.lumio.domain.TriviaPhase
import dev.master.lumio.ui.arcade.GameCue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    private fun model(catalog: suspend () -> Catalog, clock: () -> Long, random: Random = Random.Default): TriviaViewModel =
        TriviaViewModel(catalog, clock, random).also { vm -> stores += ViewModelStore().apply { put("test", vm) } }
    private fun gameTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { stores.forEach { it.clear() }; stores.clear() }
    }
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private val questions = (1..8).map { TriviaQuestion("q$it", "film$it", "synthetic", "Question $it?", listOf("A", "B", "C", "D"), 2) }
    private val catalog = Catalog(emptyList(), questions)
    private fun game(vm: TriviaViewModel) = (vm.state.value as TriviaState.Playing).round

    @Test fun soundsOnlyForNewFeedbackAndNeverReplayAfterReturning() = gameTest {
        val vm = model({ catalog }, { testScheduler.currentTime })
        val cues = mutableListOf<GameCue>()
        val collect = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sounds.collect { cues += it } }
        vm.setVisible(true); vm.start(); runCurrent()
        vm.answer(2); runCurrent()
        vm.answer(2); runCurrent()
        assertEquals(listOf(GameCue.CORRECT), cues)
        vm.next(); vm.answer(1); runCurrent()
        assertEquals(GameCue.INCORRECT, cues.last())
        vm.next(); advanceTimeBy(20_001); runCurrent()
        assertEquals(listOf(GameCue.CORRECT, GameCue.INCORRECT, GameCue.INCORRECT), cues)
        collect.cancel()
        val returned = mutableListOf<GameCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sounds.collect { returned += it } }
        runCurrent(); assertTrue(returned.isEmpty())
    }

    @Test fun lazyStartLoadsFiveSourceQuestionsAndStartsTwentySecondClock() = gameTest {
        var calls = 0
        val vm = model({ calls++; catalog }, { testScheduler.currentTime }, Random(1))
        assertEquals(TriviaState.Ready, vm.state.value)
        assertEquals(0, calls)
        vm.setVisible(true); vm.start(); runCurrent()
        assertEquals(5, game(vm).questions.size)
        assertEquals(20_000L, game(vm).remainingMs)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(19_000L, game(vm).remainingMs)
        vm.start(); runCurrent()
        assertEquals(1, calls)
    }

    @Test fun pauseAndBackgroundFreezeTimerAndReturningRequiresResume() = gameTest {
        val vm = model({ catalog }, { testScheduler.currentTime })
        vm.setVisible(true); vm.start(); runCurrent()
        advanceTimeBy(2_500); runCurrent()
        vm.pause()
        val remaining = game(vm).remainingMs
        advanceTimeBy(10_000); runCurrent()
        assertEquals(remaining, game(vm).remainingMs)
        vm.resume()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(remaining - 1_000, game(vm).remainingMs)
        vm.setVisible(false)
        val hidden = game(vm).remainingMs
        advanceTimeBy(30_000); runCurrent()
        vm.setVisible(true)
        assertTrue(game(vm).paused)
        assertEquals(hidden, game(vm).remainingMs)
        vm.resume()
        assertFalse(game(vm).paused)
    }

    @Test fun exactDeadlineAnswerCannotWinAfterTimeExpires() = gameTest {
        var now = 0L
        val vm = model({ catalog }, { now })
        vm.setVisible(true); vm.start(); runCurrent()
        now = 20_000
        vm.answer(2)
        assertEquals(TriviaPhase.FEEDBACK, game(vm).phase)
        assertNull(game(vm).selected)
        assertEquals(0, game(vm).score)
        vm.answer(2)
        assertEquals(0, game(vm).score)
    }

    @Test fun feedbackAutoAdvanceHoldAndLifelineWorkThroughController() = gameTest {
        val vm = model({ catalog }, { testScheduler.currentTime }, Random(7))
        vm.setVisible(true); vm.start(); runCurrent()
        vm.fiftyFifty()
        assertEquals(2, game(vm).eliminated.size)
        assertFalse(2 in game(vm).eliminated)
        vm.answer(2); vm.answer(2)
        assertEquals(100, game(vm).score)
        vm.hold()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(0, game(vm).index)
        assertTrue(game(vm).holdFeedback)
        vm.next()
        assertEquals(1, game(vm).index)
        assertTrue(game(vm).fiftyUsed)
        vm.answer(2)
        advanceTimeBy(4_000); runCurrent()
        assertEquals(2, game(vm).index)
        assertEquals(225, game(vm).score)
    }

    @Test fun completeRoundAndPlayAgainResetScoreLifelineAndQuestionSet() = gameTest {
        val vm = model({ catalog }, { testScheduler.currentTime }, Random(5))
        vm.setVisible(true); vm.start(); runCurrent()
        vm.fiftyFifty()
        repeat(5) { vm.answer(2); vm.next() }
        assertEquals(TriviaPhase.RESULTS, game(vm).phase)
        assertEquals(750, game(vm).score)
        assertEquals(5, game(vm).correctCount)
        vm.playAgain(); runCurrent()
        assertEquals(TriviaPhase.QUESTION, game(vm).phase)
        assertEquals(0, game(vm).score)
        assertFalse(game(vm).fiftyUsed)
        assertEquals(5, game(vm).questions.map { it.id }.distinct().size)
    }

    @Test fun failedCatalogRetriesAndTooFewValidQuestionsAreUnavailable() = gameTest {
        var fail = true
        var data = catalog
        val vm = model({ if (fail) error("offline") else data }, { testScheduler.currentTime })
        vm.setVisible(true); vm.start(); runCurrent()
        assertEquals(TriviaState.Error, vm.state.value)
        fail = false
        data = Catalog(emptyList(), questions.take(4))
        vm.retry(); runCurrent()
        assertEquals(TriviaState.Unavailable, vm.state.value)
        data = catalog
        vm.retry(); runCurrent()
        assertEquals(5, game(vm).questions.size)
    }

    @Test fun loadFinishingOffscreenIsPausedAndOldLoadsCannotReplaceReplay() = gameTest {
        val gate = CompletableDeferred<Catalog>()
        var calls = 0
        val vm = model({ if (++calls == 1) gate.await() else catalog }, { testScheduler.currentTime })
        vm.setVisible(true); vm.start(); runCurrent()
        vm.setVisible(false)
        vm.playAgain(); runCurrent()
        assertTrue(game(vm).paused)
        gate.complete(Catalog(emptyList(), emptyList())); runCurrent()
        assertTrue(vm.state.value is TriviaState.Playing)
        advanceTimeBy(30_000); runCurrent()
        assertEquals(20_000L, game(vm).remainingMs)
        vm.setVisible(true)
        assertTrue(game(vm).paused)
    }
}
