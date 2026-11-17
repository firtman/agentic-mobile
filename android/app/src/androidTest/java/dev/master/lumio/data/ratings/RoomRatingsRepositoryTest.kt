package dev.master.lumio.data.ratings

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.master.lumio.data.Movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Room schema, type converters and transactions on a device. */
@RunWith(AndroidJUnit4::class)
class RoomRatingsRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext, LumioDatabase::class.java,
    ).build()
    private var now = 10L
    private val repository = RoomRatingsRepository(database) { now++ }
    private val movie = Movie(id = "m/1", title = "Film", year = 2001, genres = listOf("Drama", "Comedy"),
        director = "Someone", cast = listOf("A, with comma", "B"))

    @After fun tearDown() = database.close()

    @Test fun historyAndPerMovieUndoKeepOtherMoviesIntact() = runTest {
        val first = repository.rate(movie, RatingValue.UNSEEN)!!
        val originalAt = database.ratings().get(movie.id)!!.updatedAt
        val second = repository.rate(movie, RatingValue.LIKE)!!
        val other = movie.copy(id = "other", title = "Other")
        val third = repository.rate(other, RatingValue.DISLIKE)!!
        val history = repository.history().first()
        assertEquals(listOf("other", movie.id), history.map { it.rating.movieId })
        assertEquals(second, history.last().undoId)
        assertFalse(repository.undoMovie(movie.id, first))
        assertTrue(repository.undoMovie(movie.id, second))
        assertEquals(RatingValue.UNSEEN, repository.rating(movie.id).first())
        assertEquals(originalAt, database.ratings().get(movie.id)!!.updatedAt)
        assertEquals(third, repository.latestUndo().first()!!.id)
        assertTrue(repository.undoMovie(movie.id, first))
        assertEquals(listOf("other"), repository.history().first().map { it.rating.movieId })
        assertTrue(repository.undo(third))
        assertTrue(repository.history().first().isEmpty())
    }

    @Test fun ratingSnapshotUndoAndObservation() = runTest {
        assertTrue(repository.ratedIds().first().isEmpty())
        assertNull(repository.latestUndo().first())
        assertNull(repository.rating(movie.id).first())
        val first = repository.rate(movie, RatingValue.UNSEEN)!!
        val second = repository.rate(movie, RatingValue.LIKE)!!
        assertEquals(setOf(movie.id), repository.ratedIds().first())
        assertEquals(second, repository.latestUndo().first()!!.id)
        assertNull(repository.rate(movie, RatingValue.LIKE))
        assertEquals(RatingValue.LIKE, repository.rating(movie.id).first())
        val stored = database.ratings().get(movie.id)!!
        assertEquals(listOf("A, with comma", "B"), stored.cast)
        assertEquals(listOf("Drama", "Comedy"), stored.genres)
        assertFalse(repository.undo(first))
        assertTrue(repository.undo(second))
        assertEquals(RatingValue.UNSEEN, repository.rating(movie.id).first())
        assertEquals(first, repository.latestUndo().first()!!.id)
        assertTrue(repository.undo(first))
        assertTrue(repository.ratedIds().first().isEmpty())
        assertNull(repository.latestUndo().first())
        assertNull(repository.rating(movie.id).first())
    }
}
