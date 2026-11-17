package dev.master.lumio.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.R
import dev.master.lumio.data.Movie
import dev.master.lumio.data.ratings.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface HistoryResult {
    data object Loading : HistoryResult
    data object Empty : HistoryResult
    data object Error : HistoryResult
    data class Content(val choices: List<HistoryChoice>) : HistoryResult
}
data class HistoryUiState(
    val result: HistoryResult = HistoryResult.Loading,
    val saving: Boolean = false,
    val message: Int? = null,
)

/** History uses only the saved personal-choice snapshots, never the catalog. */
class HistoryViewModel(private val ratings: RatingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(HistoryUiState())
    val state = mutableState.asStateFlow()
    private var observeJob: Job? = null

    init { retry() }

    fun retry() {
        observeJob?.cancel()
        mutableState.update { it.copy(result = HistoryResult.Loading) }
        observeJob = viewModelScope.launch {
            ratings.history().catch { e ->
                if (e is CancellationException) throw e
                mutableState.update { it.copy(result = HistoryResult.Error) }
            }.collect { choices ->
                mutableState.update { it.copy(result = if (choices.isEmpty()) HistoryResult.Empty else HistoryResult.Content(choices)) }
            }
        }
    }

    fun rate(movieId: String, value: RatingValue) {
        val row = choice(movieId) ?: return
        if (row.rating.value == value) return
        save {
            ratings.rate(row.rating.snapshotMovie(), value)
            R.string.choice_updated
        }
    }

    fun undo(movieId: String) {
        val row = choice(movieId) ?: return
        val id = row.undoId ?: return
        save {
            if (ratings.undoMovie(movieId, id)) R.string.rating_undone else R.string.choice_changed
        }
    }

    private fun choice(id: String) = (state.value.result as? HistoryResult.Content)?.choices?.find { it.rating.movieId == id }

    private fun save(operation: suspend () -> Int) {
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val message = try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                R.string.rating_error
            }
            mutableState.update { it.copy(saving = false, message = message) }
        }
    }

    fun messageShown() { mutableState.update { it.copy(message = null) } }
}

/** Exactly the snapshot fields; missing catalog metadata is not fabricated or persisted. */
fun RatingEntity.snapshotMovie() = Movie(
    id = movieId, title = title, year = year, posterUrl = posterUrl,
    genres = genres, director = director, cast = cast,
)
