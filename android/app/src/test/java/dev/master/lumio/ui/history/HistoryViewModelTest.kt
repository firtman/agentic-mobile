package dev.master.lumio.ui.history

import dev.master.lumio.data.Movie
import dev.master.lumio.data.ratings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private val movie = Movie("one", "First", year = 2001, genres = listOf("Drama"), director = "Director", cast = listOf("Actor"))
    private class Ratings : RatingsRepository {
        val dao = FakeRatingDao()
        var now = 100L
        private val ledger = RatingsLedger(dao) { now++ }
        var failRead = false
        var failWrite = false
        var stale = false
        var writes = 0
        override fun history(): Flow<List<HistoryChoice>> = flow {
            if (failRead) error("read failed")
            emitAll(dao.observeHistory())
        }
        override fun ratedIds() = dao.observeRatedIds().map { it.toSet() }
        override fun latestUndo() = dao.observeLatestUndo()
        override fun rating(movieId: String) = dao.observe(movieId).map { it?.value }
        override suspend fun rate(movie: Movie, value: RatingValue): Long? {
            writes++
            if (failWrite) error("disk full")
            return ledger.rate(movie, value)
        }
        override suspend fun undo(entryId: Long) = ledger.undo(entryId) != null
        override suspend fun undoMovie(movieId: String, entryId: Long): Boolean {
            if (failWrite) error("disk full")
            if (stale) return false
            return ledger.undoMovie(movieId, entryId) != null
        }
    }
    private fun rows(vm: HistoryViewModel) = (vm.state.value.result as HistoryResult.Content).choices

    @Test fun emptyThenReactiveNewestFirstHistoryIncludesUnseen() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = HistoryViewModel(ratings)
        assertEquals(HistoryResult.Loading, vm.state.value.result)
        runCurrent()
        assertEquals(HistoryResult.Empty, vm.state.value.result)
        ratings.rate(movie, RatingValue.UNSEEN)
        ratings.rate(movie.copy(id = "two"), RatingValue.DISLIKE)
        runCurrent()
        assertEquals(listOf("two", "one"), rows(vm).map { it.rating.movieId })
        assertEquals(RatingValue.UNSEEN, rows(vm)[1].rating.value)
        assertNotNull(rows(vm)[1].undoId)
    }

    @Test fun perFilmUndoPreservesNewerOtherFilmAndGlobalUndo() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.UNSEEN)
        val originalAt = ratings.dao.get("one")!!.updatedAt
        ratings.rate(movie, RatingValue.LIKE)
        val latest = ratings.rate(movie.copy(id = "two"), RatingValue.DISLIKE)!!
        val vm = HistoryViewModel(ratings)
        runCurrent()
        vm.undo("one"); runCurrent()
        assertEquals(RatingValue.UNSEEN, ratings.dao.get("one")!!.value)
        assertEquals(originalAt, ratings.dao.get("one")!!.updatedAt)
        assertEquals(RatingValue.DISLIKE, ratings.dao.get("two")!!.value)
        assertEquals(latest, ratings.dao.latest.value!!.id)
        vm.undo("one"); runCurrent()
        assertNull(ratings.dao.get("one"))
        assertEquals(listOf("two"), rows(vm).map { it.rating.movieId })
        assertTrue(ratings.undo(latest))
        runCurrent()
        assertEquals(HistoryResult.Empty, vm.state.value.result)
    }

    @Test fun changeUsesSnapshotAndCanBeUndoneWithoutNetwork() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.LIKE)
        val original = ratings.dao.get("one")!!
        val vm = HistoryViewModel(ratings)
        runCurrent()
        vm.rate("one", RatingValue.UNSEEN); runCurrent()
        assertEquals(original.copy(value = RatingValue.UNSEEN, updatedAt = original.updatedAt + 1), ratings.dao.get("one"))
        vm.undo("one"); runCurrent()
        assertEquals(original, ratings.dao.get("one"))
    }

    @Test fun sameChoiceAndDoubleTapDoNotAddExtraChanges() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.LIKE)
        val vm = HistoryViewModel(ratings)
        runCurrent()
        vm.rate("one", RatingValue.LIKE); runCurrent()
        assertEquals(1, ratings.writes)
        vm.rate("one", RatingValue.DISLIKE)
        vm.rate("one", RatingValue.UNSEEN)
        runCurrent()
        assertEquals(2, ratings.writes)
        assertEquals(RatingValue.DISLIKE, rows(vm).single().rating.value)
    }

    @Test fun readErrorRetryAndWriteFailurePreserveStoredChoice() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.LIKE)
        ratings.failRead = true
        val vm = HistoryViewModel(ratings)
        runCurrent()
        assertEquals(HistoryResult.Error, vm.state.value.result)
        ratings.failRead = false
        vm.retry(); runCurrent()
        ratings.failWrite = true
        vm.rate("one", RatingValue.UNSEEN); runCurrent()
        assertNotNull(vm.state.value.message)
        assertFalse(vm.state.value.saving)
        assertEquals(RatingValue.LIKE, rows(vm).single().rating.value)
        vm.messageShown()
        vm.undo("one"); runCurrent()
        assertNotNull(vm.state.value.message)
        assertEquals(RatingValue.LIKE, rows(vm).single().rating.value)
    }

    @Test fun staleUndoReportsConflictAndRetainsChoice() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.LIKE)
        ratings.stale = true
        val vm = HistoryViewModel(ratings)
        runCurrent()
        vm.undo("one"); runCurrent()
        assertEquals(dev.master.lumio.R.string.choice_changed, vm.state.value.message)
        assertEquals(RatingValue.LIKE, rows(vm).single().rating.value)
    }

    @Test fun trimmedUndoHistoryLeavesChoiceVisibleWithUndoUnavailable() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(movie, RatingValue.UNSEEN)
        repeat(RatingsLedger.UNDO_LIMIT) { ratings.rate(movie.copy(id = "other-$it"), RatingValue.LIKE) }
        val vm = HistoryViewModel(ratings)
        runCurrent()
        val old = rows(vm).first { it.rating.movieId == "one" }
        assertNull(old.undoId)
        vm.undo("one"); runCurrent()
        assertEquals(RatingValue.UNSEEN, ratings.dao.get("one")!!.value)
    }
}
