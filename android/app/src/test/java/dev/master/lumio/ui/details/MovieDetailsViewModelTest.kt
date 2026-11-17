package dev.master.lumio.ui.details

import dev.master.lumio.data.Movie
import dev.master.lumio.data.MovieNotFoundException
import dev.master.lumio.data.MovieRepository
import dev.master.lumio.data.ratings.FakeRatingDao
import dev.master.lumio.data.ratings.RatingValue
import dev.master.lumio.data.ratings.RatingsLedger
import dev.master.lumio.data.ratings.RatingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val movie = Movie(id = "m1", title = "Film", genres = listOf("Drama"))
    private class Movies(var load: suspend (String) -> Movie) : MovieRepository {
        var calls = 0
        override suspend fun movie(id: String): Movie { calls++; return load(id) }
    }
    private class Ratings(val dao: FakeRatingDao = FakeRatingDao()) : RatingsRepository {
        private val ledger = RatingsLedger(dao) { 1L }
        var fail = false
        override fun ratedIds() = dao.ratings.map { it.keys }
        override fun latestUndo() = dao.latest
        override fun rating(movieId: String): Flow<RatingValue?> = dao.ratings.map { it[movieId]?.value }
        override suspend fun rate(movie: Movie, value: RatingValue): Long? {
            if (fail) error("disk full")
            return ledger.rate(movie, value)
        }
        override suspend fun undo(entryId: Long) = ledger.undo(entryId) != null
    }

    @Test fun loadsMovieAndRetriesAfterError() = runTest(dispatcher) {
        val movies = Movies { error("offline") }
        val vm = MovieDetailsViewModel("m1", movies, Ratings())
        assertEquals(DetailsResult.Loading, vm.state.value.result)
        runCurrent()
        assertEquals(DetailsResult.Error, vm.state.value.result)
        movies.load = { movie }
        vm.retry(); runCurrent()
        assertEquals(DetailsResult.Content(movie), vm.state.value.result)
        assertEquals(2, movies.calls)
    }

    @Test fun missingMovieIsNotFound() = runTest(dispatcher) {
        val vm = MovieDetailsViewModel("gone", Movies { throw MovieNotFoundException(it) }, Ratings())
        runCurrent()
        assertEquals(DetailsResult.NotFound, vm.state.value.result)
    }

    @Test fun rateShowsStoredValueAndUndoRemovesFirstRating() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = MovieDetailsViewModel("m1", Movies { movie }, ratings)
        runCurrent()
        assertNull(vm.state.value.rating)
        vm.rate(RatingValue.LIKE); runCurrent()
        assertEquals(RatingValue.LIKE, vm.state.value.rating)
        val saved = vm.state.value.message as DetailsMessage.Saved
        assertEquals(RatingValue.LIKE, saved.value)
        vm.messageShown(saved.id)
        assertNull(vm.state.value.message)
        vm.undo(saved.undoId); runCurrent()
        assertNull(vm.state.value.rating)
        assertTrue(vm.state.value.message is DetailsMessage.Undone)
        assertNull(ratings.dao.get("m1"))
    }

    @Test fun changingRatingThenUndoRestoresPrevious() = runTest(dispatcher) {
        val vm = MovieDetailsViewModel("m1", Movies { movie }, Ratings())
        runCurrent()
        vm.rate(RatingValue.DISLIKE); runCurrent()
        vm.rate(RatingValue.UNSEEN); runCurrent()
        assertEquals(RatingValue.UNSEEN, vm.state.value.rating)
        vm.undo((vm.state.value.message as DetailsMessage.Saved).undoId); runCurrent()
        assertEquals(RatingValue.DISLIKE, vm.state.value.rating)
    }

    @Test fun cannotRateBeforeLoadAndSaveFailureIsReported() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = MovieDetailsViewModel("m1", Movies { movie }, ratings)
        vm.rate(RatingValue.LIKE); runCurrent()
        assertNull(ratings.dao.get("m1"))
        ratings.fail = true
        vm.rate(RatingValue.LIKE); runCurrent()
        assertTrue(vm.state.value.message is DetailsMessage.SaveFailed)
        assertFalse(vm.state.value.saving)
        assertNull(vm.state.value.rating)
    }

    @Test fun reselectingCurrentValueDoesNothing() = runTest(dispatcher) {
        val ratings = Ratings()
        val vm = MovieDetailsViewModel("m1", Movies { movie }, ratings)
        runCurrent()
        vm.rate(RatingValue.LIKE); runCurrent()
        vm.messageShown(vm.state.value.message!!.id)
        vm.rate(RatingValue.LIKE); runCurrent()
        assertNull(vm.state.value.message)
        assertEquals(1, ratings.dao.undo.size)
    }
}
