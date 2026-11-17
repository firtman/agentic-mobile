package dev.master.lumio.ui.puzzle

import dev.master.lumio.data.*
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
class PuzzleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private val films = listOf(Movie("one", "One", posterUrl = "https://example.org/one.jpg"), Movie("two", "Two", posterUrl = "https://example.org/two.jpg"))
    private val catalog = Catalog(films, emptyList())
    private fun session(vm: PuzzleViewModel) = (vm.state.value as PuzzleState.Playing).session

    @Test fun selectionIsSilentAndSwapsAndCompletionEmitOnce() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(1))
        val cues = mutableListOf<GameCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.sounds.collect { cues += it } }
        vm.start(); runCurrent()
        vm.posterLoaded(session(vm).id)
        vm.choose(0); vm.choose(0); runCurrent()
        assertTrue(cues.isEmpty())
        repeat(9) { position ->
            val board = session(vm).board
            if (board.tiles[position] != position) {
                vm.choose(position); vm.choose(board.tiles.indexOf(position)); runCurrent()
            }
        }
        assertEquals(GameCue.COMPLETE, cues.last())
        assertEquals(session(vm).board.moves, cues.size)
        val size = cues.size
        vm.choose(0); vm.choose(1); vm.start(); runCurrent()
        assertEquals(size, cues.size)
    }

    @Test fun lazyStartCreatesUnsolvedBoardAndRepeatedStartRetainsIt() = runTest(dispatcher) {
        var calls = 0
        val vm = PuzzleViewModel({ calls++; catalog }, Random(1))
        assertEquals(PuzzleState.Ready, vm.state.value)
        assertEquals(0, calls)
        vm.start(); runCurrent()
        val first = session(vm)
        assertFalse(first.board.complete)
        assertEquals(PuzzlePoster.LOADING, first.poster)
        vm.choose(0); vm.choose(1)
        assertEquals(0, session(vm).board.moves)
        assertNull(session(vm).board.selected)
        vm.posterLoaded(first.id)
        vm.choose(0)
        vm.start(); runCurrent()
        assertEquals(0, session(vm).board.selected)
        assertEquals(first.id, session(vm).id)
        assertEquals(1, calls)
    }

    @Test fun referenceClearsSelectionAndBlocksSwapsUntilHidden() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(2))
        vm.start(); runCurrent()
        vm.posterLoaded(session(vm).id)
        vm.choose(0)
        vm.toggleReference()
        assertTrue(session(vm).reference)
        assertNull(session(vm).board.selected)
        val board = session(vm).board
        vm.choose(0); vm.choose(1)
        assertEquals(board, session(vm).board)
        vm.toggleReference()
        vm.choose(0); vm.choose(1)
        assertEquals(1, session(vm).board.moves)
    }

    @Test fun solveCompletesThenNextChangesMovieAndResetsAllBoardState() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(3))
        vm.start(); runCurrent()
        val first = session(vm)
        vm.posterLoaded(first.id)
        repeat(9) { position ->
            val board = session(vm).board
            if (board.tiles[position] != position) {
                vm.choose(position); vm.choose(board.tiles.indexOf(position))
            }
        }
        assertTrue(session(vm).board.complete)
        val solved = session(vm)
        vm.choose(0); vm.choose(1); vm.toggleReference()
        assertEquals(solved, session(vm))
        vm.nextPuzzle(); runCurrent()
        assertNotEquals(first.movie.id, session(vm).movie.id)
        assertNotEquals(first.id, session(vm).id)
        assertEquals(0, session(vm).board.moves)
        assertFalse(session(vm).board.complete)
        assertFalse(session(vm).reference)
        assertNull(session(vm).board.selected)
    }

    @Test fun failedPosterIsBlockedAndSkippedAndLateResultsCannotAffectNextPuzzle() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(4))
        vm.start(); runCurrent()
        val first = session(vm)
        vm.posterLoaded(first.id)
        vm.choose(0)
        vm.posterFailed(first.id)
        assertEquals(PuzzlePoster.FAILED, session(vm).poster)
        assertNull(session(vm).board.selected)
        vm.choose(0); vm.choose(1); vm.toggleReference()
        assertEquals(0, session(vm).board.moves)
        assertFalse(session(vm).reference)
        vm.start(); runCurrent()
        assertEquals(PuzzlePoster.FAILED, session(vm).poster)
        vm.nextPuzzle(); runCurrent()
        assertNotEquals(first.movie.id, session(vm).movie.id)
        vm.posterFailed(first.id); vm.posterLoaded(first.id)
        assertEquals(PuzzlePoster.LOADING, session(vm).poster)
    }

    @Test fun posterRetryRequiresSuccessfulArtworkBeforePlayAndRejectsOldAttempt() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(8))
        vm.start(); runCurrent()
        val original = session(vm)
        vm.posterFailed(original.id)
        vm.retryPoster()
        val retry = session(vm)
        assertEquals(original.movie, retry.movie)
        assertNotEquals(original.id, retry.id)
        assertEquals(PuzzlePoster.LOADING, retry.poster)
        vm.posterFailed(original.id)
        vm.choose(0); vm.choose(1)
        assertEquals(PuzzlePoster.LOADING, session(vm).poster)
        assertEquals(0, session(vm).board.moves)
        vm.posterLoaded(retry.id)
        vm.choose(0); vm.choose(1)
        assertEquals(1, session(vm).board.moves)
    }

    @Test fun everyFailedPosterProducesUnavailableAndRetryCanRecheckPosters() = runTest(dispatcher) {
        val vm = PuzzleViewModel({ catalog }, Random(9))
        vm.start(); runCurrent()
        vm.posterFailed(session(vm).id)
        vm.nextPuzzle(); runCurrent()
        vm.posterFailed(session(vm).id)
        vm.nextPuzzle(); runCurrent()
        assertEquals(PuzzleState.Unavailable, vm.state.value)
        vm.retry(); runCurrent()
        assertEquals(PuzzlePoster.LOADING, session(vm).poster)
    }

    @Test fun moviesWithoutPostersAndEmptyCatalogAreUnavailable() = runTest(dispatcher) {
        var data = Catalog(listOf(Movie("missing", "Missing poster")), emptyList())
        val vm = PuzzleViewModel({ data }, Random(5))
        vm.start(); runCurrent()
        assertEquals(PuzzleState.Unavailable, vm.state.value)
        vm.choose(0); vm.choose(1)
        assertEquals(PuzzleState.Unavailable, vm.state.value)
        data = Catalog(emptyList(), emptyList())
        vm.nextPuzzle(); runCurrent()
        assertEquals(PuzzleState.Unavailable, vm.state.value)
    }

    @Test fun networkFailureRetriesAndCancelledOldLoadCannotReplaceNewerBoard() = runTest(dispatcher) {
        var fail = true
        val gate = CompletableDeferred<Catalog>()
        var calls = 0
        val vm = PuzzleViewModel({
            calls++
            if (fail) error("offline")
            if (calls == 2) gate.await() else catalog
        }, Random(6))
        vm.start(); runCurrent()
        assertEquals(PuzzleState.Error, vm.state.value)
        fail = false
        vm.retry(); runCurrent()
        assertEquals(PuzzleState.Loading, vm.state.value)
        vm.nextPuzzle(); runCurrent()
        val current = session(vm)
        gate.complete(Catalog(emptyList(), emptyList())); runCurrent()
        assertEquals(current, session(vm))
    }
}
