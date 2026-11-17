package dev.master.lumio.data.ratings

import androidx.room.withTransaction
import dev.master.lumio.data.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Rating and undo rules, independent of Room so they can be unit-tested on the JVM.
 * Callers must run each operation inside one database transaction.
 */
class RatingsLedger(private val dao: RatingDao, private val clock: () -> Long) {
    /** Saves [value]; returns the undo entry id, or null when the movie already had that value. */
    suspend fun rate(movie: Movie, value: RatingValue): Long? {
        val current = dao.get(movie.id)
        if (current?.value == value) return null
        dao.upsert(RatingEntity(
            movieId = movie.id, value = value, updatedAt = clock(),
            title = movie.title, year = movie.year, posterUrl = movie.posterUrl,
            genres = movie.genres, director = movie.director, cast = movie.cast,
        ))
        val id = dao.push(UndoEntry(movieId = movie.id, previousValue = current?.value, previousUpdatedAt = current?.updatedAt))
        dao.trimUndo(UNDO_LIMIT)
        return id
    }

    /**
     * Reverts the most recent change: restores the previous value, or removes a first rating.
     * With [entryId], only that change is reverted, and only while it is still the most recent.
     * Returns the affected movie id, or null when nothing was undone.
     */
    suspend fun undo(entryId: Long? = null): String? {
        val entry = dao.latestUndo() ?: return null
        if (entryId != null && entry.id != entryId) return null
        val previous = entry.previousValue
        if (previous == null) {
            dao.delete(entry.movieId)
        } else {
            dao.get(entry.movieId)?.let {
                dao.upsert(it.copy(value = previous, updatedAt = entry.previousUpdatedAt ?: it.updatedAt))
            }
        }
        dao.removeUndo(entry.id)
        return entry.movieId
    }

    companion object { const val UNDO_LIMIT = 200 }
}

interface RatingsRepository {
    fun ratedIds(): Flow<Set<String>>
    fun latestUndo(): Flow<UndoEntry?>
    fun rating(movieId: String): Flow<RatingValue?>
    suspend fun rate(movie: Movie, value: RatingValue): Long?
    suspend fun undo(entryId: Long): Boolean
}

class RoomRatingsRepository(
    private val database: LumioDatabase,
    clock: () -> Long = System::currentTimeMillis,
) : RatingsRepository {
    private val dao = database.ratings()
    private val ledger = RatingsLedger(dao, clock)
    override fun ratedIds(): Flow<Set<String>> = dao.observeRatedIds().map { it.toSet() }.distinctUntilChanged()
    override fun latestUndo(): Flow<UndoEntry?> = dao.observeLatestUndo().distinctUntilChanged()
    override fun rating(movieId: String): Flow<RatingValue?> =
        dao.observe(movieId).map { it?.value }.distinctUntilChanged()
    override suspend fun rate(movie: Movie, value: RatingValue): Long? =
        database.withTransaction { ledger.rate(movie, value) }
    override suspend fun undo(entryId: Long): Boolean =
        database.withTransaction { ledger.undo(entryId) } != null
}
