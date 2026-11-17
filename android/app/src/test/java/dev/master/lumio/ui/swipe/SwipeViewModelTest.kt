package dev.master.lumio.ui.swipe

import dev.master.lumio.data.*
import dev.master.lumio.data.ratings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private val films = (0..5).map { Movie("$it", "Film $it") }
    private class Movies(val films: List<Movie>) : ExploreRepository {
        val offsets = mutableListOf<Int>()
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun genres() = emptyList<String>()
        override suspend fun movies(filters: ExploreFilters, offset: Int): MoviePage {
            assertEquals(MovieSort.POPULARITY_DESC, filters.sort)
            offsets += offset
            gate?.await()
            if (fail) error("offline")
            return MoviePage(films.drop(offset).take(2), films.size, 2, offset)
        }
    }
    private class Ratings : RatingsRepository {
        val dao = FakeRatingDao()
        private val ledger = RatingsLedger(dao) { 1L }
        var fail = false
        var failRead = false
        override fun history() = dao.observeHistory()
        override suspend fun undoMovie(movieId: String, entryId: Long) = ledger.undoMovie(movieId, entryId) != null
        override fun ratedIds(): Flow<Set<String>> = flow {
            if (failRead) error("db unavailable")
            emitAll(dao.ratings.map { it.keys })
        }
        override fun latestUndo() = dao.latest
        override fun rating(movieId: String) = dao.observe(movieId).map { it?.value }
        override suspend fun rate(movie: Movie, value: RatingValue): Long? {
            if (fail) error("disk full")
            return ledger.rate(movie, value)
        }
        override suspend fun undo(entryId: Long): Boolean {
            if (fail) error("disk full")
            return ledger.undo(entryId) != null
        }
    }
    private fun current(vm: SwipeViewModel) = (vm.state.value.result as SwipeResult.Card).movie.id

    @Test fun skipsFullyRatedPagesIncludingUnseenAndContinuesToNextPage() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(films[0], RatingValue.UNSEEN)
        ratings.rate(films[1], RatingValue.DISLIKE)
        val movies = Movies(films)
        val vm = SwipeViewModel(movies, ratings)
        runCurrent()
        assertEquals("2", current(vm))
        assertEquals(listOf(0, 2), movies.offsets)
        vm.rate(RatingValue.LIKE); vm.rate(RatingValue.DISLIKE); runCurrent()
        assertEquals(RatingValue.LIKE, ratings.dao.get("2")!!.value)
        assertEquals("3", current(vm))
        vm.rate(RatingValue.UNSEEN); runCurrent()
        assertEquals("4", current(vm))
        assertEquals(listOf(0, 2, 4), movies.offsets)
    }

    @Test fun savedChoicesFromDetailsRemoveCurrentCardReactively() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = SwipeViewModel(Movies(films), ratings)
        runCurrent()
        ratings.rate(films[0], RatingValue.UNSEEN); runCurrent()
        assertEquals("1", current(vm))
    }

    @Test fun failedSaveDoesNotAdvanceAndUndoRestoresFirstRatingAcrossRecreation() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = SwipeViewModel(Movies(films), ratings)
        runCurrent()
        ratings.fail = true
        vm.rate(RatingValue.LIKE); runCurrent()
        assertEquals("0", current(vm))
        assertNotNull(vm.state.value.message)
        ratings.fail = false
        vm.rate(RatingValue.LIKE); runCurrent()
        val recreated = SwipeViewModel(Movies(films), ratings)
        runCurrent()
        assertEquals("1", current(recreated))
        assertNotNull(recreated.state.value.undoId)
        recreated.undo(); runCurrent()
        assertNull(ratings.dao.get("0"))
        assertEquals("0", current(recreated))
        assertNull(recreated.state.value.undoId)
    }

    @Test fun undoThatRestoresAnEarlierRatingKeepsMovieExcluded() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(films[0], RatingValue.DISLIKE)
        ratings.rate(films[0], RatingValue.LIKE)
        val vm = SwipeViewModel(Movies(films), ratings)
        runCurrent()
        vm.undo(); runCurrent()
        assertEquals(RatingValue.DISLIKE, ratings.dao.get("0")!!.value)
        assertEquals("1", current(vm))
        vm.undo(); runCurrent()
        assertEquals("0", current(vm))
    }

    @Test fun errorRetryAndEmptyQueueAreDistinct() = runTest(dispatcher) {
        val movies = Movies(films.take(1)).apply { fail = true }
        val vm = SwipeViewModel(movies, Ratings())
        runCurrent()
        assertEquals(SwipeResult.Error, vm.state.value.result)
        movies.fail = false
        vm.retry(); runCurrent()
        assertEquals("0", current(vm))
        vm.rate(RatingValue.UNSEEN); runCurrent()
        assertEquals(SwipeResult.Empty, vm.state.value.result)
        vm.undo(); runCurrent()
        assertEquals("0", current(vm))
    }

    @Test fun paginationFailureRetriesSameOffsetWithoutLosingRating() = runTest(dispatcher) {
        val movies = Movies(films)
        val ratings = Ratings()
        val vm = SwipeViewModel(movies, ratings)
        runCurrent()
        vm.rate(RatingValue.LIKE); runCurrent()
        movies.fail = true
        vm.rate(RatingValue.UNSEEN); runCurrent()
        assertEquals(SwipeResult.Error, vm.state.value.result)
        movies.fail = false
        vm.retry(); runCurrent()
        assertEquals("2", current(vm))
        assertEquals(listOf(0, 2, 2), movies.offsets)
        assertEquals(RatingValue.UNSEEN, ratings.dao.get("1")!!.value)
    }

    @Test fun ratingsChangedDuringNetworkRequestAreExcluded() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val movies = Movies(films).apply { this.gate = gate }
        val ratings = Ratings()
        val vm = SwipeViewModel(movies, ratings)
        runCurrent()
        ratings.rate(films[0], RatingValue.LIKE); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals("1", current(vm))
    }

    @Test fun undoInDetailsReturnsConsumedCardToQueue() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = SwipeViewModel(Movies(films), ratings)
        runCurrent()
        val id = ratings.rate(films[0], RatingValue.LIKE)!!
        runCurrent()
        assertEquals("1", current(vm))
        ratings.undo(id); runCurrent()
        assertEquals("0", current(vm))
    }

    @Test fun emptyRefreshChecksForNewMovies() = runTest(dispatcher) {
        val catalog = mutableListOf(films[0])
        val vm = SwipeViewModel(Movies(catalog), Ratings())
        runCurrent()
        vm.rate(RatingValue.UNSEEN); runCurrent()
        assertEquals(SwipeResult.Empty, vm.state.value.result)
        catalog += films[1]
        vm.retry(); runCurrent()
        assertEquals("1", current(vm))
    }

    @Test fun unreadableRatingsBlockQueueUntilRetry() = runTest(dispatcher) {
        val movies = Movies(films)
        val ratings = Ratings().apply { failRead = true }
        val vm = SwipeViewModel(movies, ratings)
        runCurrent()
        assertEquals(SwipeResult.StorageError, vm.state.value.result)
        assertTrue(movies.offsets.isEmpty())
        ratings.failRead = false
        vm.retry(); runCurrent()
        assertEquals("0", current(vm))
    }

    @Test fun gestureThresholdsAndPhysicalDirections() {
        assertEquals(RatingValue.LIKE, swipeChoice(100f, 20f, 72f))
        assertEquals(RatingValue.DISLIKE, swipeChoice(-100f, 20f, 72f))
        assertEquals(RatingValue.UNSEEN, swipeChoice(10f, 100f, 72f))
        assertNull(swipeChoice(10f, -100f, 72f))
        assertNull(swipeChoice(30f, 0f, 72f))
        assertNull(swipeChoice(100f, 100f, 72f))
    }
}
