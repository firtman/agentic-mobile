package dev.master.lumio.data.ratings

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** A user's rating. UNSEEN is saved but never influences scoring. */
enum class RatingValue { LIKE, DISLIKE, UNSEEN }

/**
 * A rating plus the movie fields that history and Genie scoring need. The catalog itself is
 * online-only; only this small snapshot of rated movies is stored on the device.
 */
@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey val movieId: String,
    val value: RatingValue,
    val updatedAt: Long,
    val title: String,
    val year: Int,
    val posterUrl: String,
    val genres: List<String>,
    val director: String,
    @ColumnInfo(name = "cast_members") val cast: List<String>,
)

/** One step of the undo stack: what the movie's rating was before the change (null = unrated). */
@Entity(tableName = "undo_log")
data class UndoEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movieId: String,
    val previousValue: RatingValue?,
    val previousUpdatedAt: Long?,
)

/** One saved choice and its most recent still-available undo step, read in one SQLite query. */
data class HistoryChoice(@Embedded val rating: RatingEntity, val undoId: Long?)

/** Storage operations used by [RatingsLedger]; Room implements them, tests use an in-memory fake. */
@Dao
interface RatingDao {
    @Query("""
        SELECT ratings.*, (SELECT MAX(id) FROM undo_log WHERE undo_log.movieId = ratings.movieId) AS undoId
        FROM ratings ORDER BY updatedAt DESC, movieId ASC
    """)
    fun observeHistory(): Flow<List<HistoryChoice>>

    @Query("SELECT * FROM undo_log WHERE movieId = :movieId ORDER BY id DESC LIMIT 1")
    suspend fun latestUndoForMovie(movieId: String): UndoEntry?

    @Query("SELECT movieId FROM ratings")
    fun observeRatedIds(): Flow<List<String>>

    @Query("SELECT * FROM undo_log ORDER BY id DESC LIMIT 1")
    fun observeLatestUndo(): Flow<UndoEntry?>

    @Query("SELECT * FROM ratings WHERE movieId = :movieId")
    fun observe(movieId: String): Flow<RatingEntity?>

    @Query("SELECT * FROM ratings WHERE movieId = :movieId")
    suspend fun get(movieId: String): RatingEntity?

    @Upsert suspend fun upsert(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE movieId = :movieId")
    suspend fun delete(movieId: String)

    @Insert suspend fun push(entry: UndoEntry): Long

    @Query("SELECT * FROM undo_log ORDER BY id DESC LIMIT 1")
    suspend fun latestUndo(): UndoEntry?

    @Query("DELETE FROM undo_log WHERE id = :id")
    suspend fun removeUndo(id: Long)

    @Query("DELETE FROM undo_log WHERE id NOT IN (SELECT id FROM undo_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trimUndo(keep: Int)
}

class RatingConverters {
    private val strings = ListSerializer(String.serializer())
    @TypeConverter fun fromList(value: List<String>): String = Json.encodeToString(strings, value)
    @TypeConverter fun toList(value: String): List<String> = Json.decodeFromString(strings, value)
}

@Database(entities = [RatingEntity::class, UndoEntry::class], version = 1, exportSchema = true)
@TypeConverters(RatingConverters::class)
abstract class LumioDatabase : RoomDatabase() {
    abstract fun ratings(): RatingDao

    companion object {
        fun create(context: Context): LumioDatabase =
            Room.databaseBuilder(context, LumioDatabase::class.java, "lumio.db").build()
    }
}
