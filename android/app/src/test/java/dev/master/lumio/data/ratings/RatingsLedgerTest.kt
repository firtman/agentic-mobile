package dev.master.lumio.data.ratings

import dev.master.lumio.data.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** In-memory stand-in for the Room DAO with the same observable behaviour. */
class FakeRatingDao : RatingDao {
    val ratings = MutableStateFlow<Map<String, RatingEntity>>(emptyMap())
    val undo = mutableListOf<UndoEntry>()
    val latest = MutableStateFlow<UndoEntry?>(null)
    override fun observeRatedIds(): Flow<List<String>> = ratings.map { it.keys.toList() }
    override fun observeLatestUndo(): Flow<UndoEntry?> = latest
    private var nextId = 1L
    override fun observeHistory(): Flow<List<HistoryChoice>> = combine(ratings, latest) { rows, _ ->
        rows.values.sortedWith(compareByDescending<RatingEntity> { it.updatedAt }.thenBy { it.movieId })
            .map { row -> HistoryChoice(row, undo.lastOrNull { it.movieId == row.movieId }?.id) }
    }
    override suspend fun latestUndoForMovie(movieId: String) = undo.lastOrNull { it.movieId == movieId }
    override fun observe(movieId: String): Flow<RatingEntity?> = ratings.map { it[movieId] }
    override suspend fun get(movieId: String) = ratings.value[movieId]
    override suspend fun upsert(rating: RatingEntity) { ratings.value += rating.movieId to rating }
    override suspend fun delete(movieId: String) { ratings.value -= movieId }
    override suspend fun push(entry: UndoEntry): Long = nextId++.also { undo += entry.copy(id = it); latest.value = undo.lastOrNull() }
    override suspend fun latestUndo() = undo.maxByOrNull { it.id }
    override suspend fun removeUndo(id: Long) { undo.removeAll { it.id == id }; latest.value = undo.lastOrNull() }
    override suspend fun trimUndo(keep: Int) {
        val kept = undo.sortedByDescending { it.id }.take(keep).toSet()
        undo.retainAll(kept)
    }
}

class RatingsLedgerTest {
    private val dao = FakeRatingDao()
    private var now = 100L
    private val ledger = RatingsLedger(dao) { now++ }
    private val movie = Movie(
        id = "m1", title = "Snapshot", year = 1999, posterUrl = "https://example.org/p.jpg",
        genres = listOf("Drama"), director = "D. Rector", cast = listOf("A", "B"),
    )

    @Test fun firstRatingStoresSnapshotAndUndoRemovesIt() = runTest {
        val undoId = ledger.rate(movie, RatingValue.LIKE)
        val saved = dao.get("m1")!!
        assertEquals(RatingValue.LIKE, saved.value)
        assertEquals(listOf("Drama"), saved.genres)
        assertEquals("D. Rector", saved.director)
        assertEquals(listOf("A", "B"), saved.cast)
        assertEquals("https://example.org/p.jpg", saved.posterUrl)
        assertEquals("m1", ledger.undo(undoId))
        assertNull(dao.get("m1"))
        assertTrue(dao.undo.isEmpty())
    }

    @Test fun undoRestoresPreviousValueAndTimestamp() = runTest {
        ledger.rate(movie, RatingValue.DISLIKE)
        val firstAt = dao.get("m1")!!.updatedAt
        val second = ledger.rate(movie, RatingValue.UNSEEN)
        assertEquals(RatingValue.UNSEEN, dao.get("m1")!!.value)
        ledger.undo(second)
        assertEquals(RatingValue.DISLIKE, dao.get("m1")!!.value)
        assertEquals(firstAt, dao.get("m1")!!.updatedAt)
        ledger.undo()
        assertNull(dao.get("m1"))
    }

    @Test fun sameValueIsNotAChange() = runTest {
        ledger.rate(movie, RatingValue.LIKE)
        assertNull(ledger.rate(movie, RatingValue.LIKE))
        assertEquals(1, dao.undo.size)
    }

    @Test fun staleUndoIdIsIgnoredSoNewerChangesSurvive() = runTest {
        val old = ledger.rate(movie, RatingValue.LIKE)
        ledger.rate(movie.copy(id = "m2"), RatingValue.DISLIKE)
        assertNull(ledger.undo(old))
        assertEquals(RatingValue.LIKE, dao.get("m1")!!.value)
        assertEquals(RatingValue.DISLIKE, dao.get("m2")!!.value)
        assertNull(RatingsLedger(FakeRatingDao()) { 0 }.undo())
    }

    @Test fun perMovieUndoRejectsStaleOrWrongMovieIds() = runTest {
        val first = ledger.rate(movie, RatingValue.LIKE)!!
        val second = ledger.rate(movie, RatingValue.DISLIKE)!!
        assertNull(ledger.undoMovie("missing", second))
        assertNull(ledger.undoMovie("m1", first))
        assertEquals("m1", ledger.undoMovie("m1", second))
        assertEquals(RatingValue.LIKE, dao.get("m1")!!.value)
    }

    @Test fun undoHistoryIsBounded() = runTest {
        repeat(RatingsLedger.UNDO_LIMIT + 5) { i ->
            ledger.rate(movie, if (i % 2 == 0) RatingValue.LIKE else RatingValue.DISLIKE)
        }
        assertEquals(RatingsLedger.UNDO_LIMIT, dao.undo.size)
    }
}
