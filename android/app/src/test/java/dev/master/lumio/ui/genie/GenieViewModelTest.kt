package dev.master.lumio.ui.genie

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
class GenieViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private val liked = Movie("liked", "Liked", genres = listOf("Drama"))
    private val first = Movie("one", "One", genres = listOf("Drama"), popularity = 2.0)
    private val second = Movie("two", "Two", genres = listOf("Drama"), popularity = 1.0)
    private val catalog = Catalog(listOf(liked, first, second), emptyList())
    private class Ratings : RatingsRepository {
        val dao = FakeRatingDao()
        val fail = MutableStateFlow(false)
        private val ledger = RatingsLedger(dao) { 1L }
        override fun history() = combine(dao.observeHistory(), fail) { rows, failure ->
            if (failure) error("db unavailable")
            rows
        }
        override fun ratedIds() = dao.observeRatedIds().map { it.toSet() }
        override fun latestUndo() = dao.latest
        override fun rating(movieId: String) = dao.observe(movieId).map { it?.value }
        override suspend fun rate(movie: Movie, value: RatingValue) = ledger.rate(movie, value)
        override suspend fun undo(entryId: Long) = ledger.undo(entryId) != null
        override suspend fun undoMovie(movieId: String, entryId: Long) = ledger.undoMovie(movieId, entryId) != null
    }
    private fun ids(vm: GenieViewModel) = (vm.state.value as GenieState.Content).recommendations.map { it.movie.id }

    @Test fun noPreferencesOrOnlyUnseenShowsGetStartedWithoutCatalogRequest() = runTest(dispatcher) {
        val ratings = Ratings()
        var calls = 0
        val vm = GenieViewModel({ calls++; catalog }, ratings, dispatcher)
        runCurrent()
        assertEquals(GenieState.GetStarted, vm.state.value)
        ratings.rate(first, RatingValue.UNSEEN); runCurrent()
        assertEquals(GenieState.GetStarted, vm.state.value)
        assertEquals(0, calls)
    }

    @Test fun preferencesLoadMatchesAndExcludeSavedUnseenThenReactToUndo() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(liked, RatingValue.LIKE)
        val unseen = ratings.rate(first, RatingValue.UNSEEN)!!
        val vm = GenieViewModel({ catalog }, ratings, dispatcher)
        runCurrent()
        assertEquals(listOf("two"), ids(vm))
        ratings.undo(unseen); runCurrent()
        assertEquals(listOf("one", "two"), ids(vm))
        ratings.rate(first, RatingValue.DISLIKE); runCurrent()
        assertEquals(GenieState.NoMatches, vm.state.value)
    }

    @Test fun dislikeOnlyAndMissingFeaturesProduceNoMatches() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(liked, RatingValue.DISLIKE)
        val vm = GenieViewModel({ catalog }, ratings, dispatcher)
        runCurrent()
        assertEquals(GenieState.NoMatches, vm.state.value)
        ratings.undo(ratings.dao.latest.value!!.id)
        ratings.rate(Movie("missing", "No metadata"), RatingValue.LIKE)
        runCurrent()
        assertEquals(GenieState.NoMatches, vm.state.value)
    }

    @Test fun catalogFailureRetriesAndReadFailureHasDistinctState() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(liked, RatingValue.LIKE)
        var failed = true
        val vm = GenieViewModel({ if (failed) error("offline") else catalog }, ratings, dispatcher)
        runCurrent()
        assertEquals(GenieState.NetworkError, vm.state.value)
        failed = false
        vm.retry(); runCurrent()
        assertEquals(listOf("one", "two"), ids(vm))
        ratings.fail.value = true; runCurrent()
        assertEquals(GenieState.StorageError, vm.state.value)
        ratings.fail.value = false
        vm.retry(); runCurrent()
        assertEquals(listOf("one", "two"), ids(vm))
    }

    @Test fun storageFailureCancelsPendingCatalogSoLateDataCannotReplaceError() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(liked, RatingValue.LIKE)
        val gate = CompletableDeferred<Catalog>()
        val vm = GenieViewModel({ gate.await() }, ratings, dispatcher)
        runCurrent()
        assertEquals(GenieState.Loading, vm.state.value)
        ratings.fail.value = true; runCurrent()
        gate.complete(catalog); runCurrent()
        assertEquals(GenieState.StorageError, vm.state.value)
    }

    @Test fun newerPreferencesCancelInFlightResults() = runTest(dispatcher) {
        val ratings = Ratings()
        ratings.rate(liked, RatingValue.LIKE)
        val gate = CompletableDeferred<Catalog>()
        var calls = 0
        val vm = GenieViewModel({ if (++calls == 1) gate.await() else catalog }, ratings, dispatcher)
        runCurrent()
        ratings.rate(first, RatingValue.UNSEEN); runCurrent()
        assertEquals(listOf("two"), ids(vm))
        gate.complete(catalog); runCurrent()
        assertEquals(listOf("two"), ids(vm))
    }

    @Test fun removingLastEffectivePreferenceReturnsToGetStarted() = runTest(dispatcher) {
        val ratings = Ratings()
        val saved = ratings.rate(liked, RatingValue.LIKE)!!
        val vm = GenieViewModel({ catalog }, ratings, dispatcher)
        runCurrent()
        assertEquals(listOf("one", "two"), ids(vm))
        ratings.undo(saved); runCurrent()
        assertEquals(GenieState.GetStarted, vm.state.value)
    }
}
