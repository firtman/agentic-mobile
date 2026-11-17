package dev.master.lumio.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.Movie
import dev.master.lumio.data.MovieNotFoundException
import dev.master.lumio.data.MovieRepository
import dev.master.lumio.data.ratings.RatingValue
import dev.master.lumio.data.ratings.RatingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DetailsResult {
    data object Loading : DetailsResult
    data object NotFound : DetailsResult
    data object Error : DetailsResult
    data class Content(val movie: Movie) : DetailsResult
}

/** One-shot feedback shown in a snackbar, then cleared with [MovieDetailsViewModel.messageShown]. */
sealed interface DetailsMessage {
    val id: Long
    data class Saved(override val id: Long, val value: RatingValue, val undoId: Long) : DetailsMessage
    data class Undone(override val id: Long) : DetailsMessage
    data class SaveFailed(override val id: Long) : DetailsMessage
}

data class MovieDetailsUiState(
    val result: DetailsResult = DetailsResult.Loading,
    val rating: RatingValue? = null,
    val saving: Boolean = false,
    val message: DetailsMessage? = null,
)

class MovieDetailsViewModel(
    private val movieId: String,
    private val movies: MovieRepository,
    private val ratings: RatingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MovieDetailsUiState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var nextMessageId = 0L

    init {
        viewModelScope.launch {
            ratings.rating(movieId)
                .catch { /* The rating stays unknown; saving reports its own errors. */ }
                .collect { value -> mutableState.update { it.copy(rating = value) } }
        }
        retry()
    }

    fun retry() {
        loadJob?.cancel()
        mutableState.update { it.copy(result = DetailsResult.Loading) }
        loadJob = viewModelScope.launch {
            val result = try {
                DetailsResult.Content(movies.movie(movieId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: MovieNotFoundException) {
                DetailsResult.NotFound
            } catch (_: Exception) {
                DetailsResult.Error
            }
            mutableState.update { it.copy(result = result) }
        }
    }

    fun rate(value: RatingValue) {
        val movie = (state.value.result as? DetailsResult.Content)?.movie ?: return
        if (state.value.saving || state.value.rating == value) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val message = try {
                ratings.rate(movie, value)?.let { DetailsMessage.Saved(++nextMessageId, value, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DetailsMessage.SaveFailed(++nextMessageId)
            }
            mutableState.update { it.copy(saving = false, message = message ?: it.message) }
        }
    }

    fun undo(undoId: Long) {
        viewModelScope.launch {
            val message = try {
                if (ratings.undo(undoId)) DetailsMessage.Undone(++nextMessageId) else null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DetailsMessage.SaveFailed(++nextMessageId)
            }
            if (message != null) mutableState.update { it.copy(message = message) }
        }
    }

    fun messageShown(id: Long) {
        mutableState.update { if (it.message?.id == id) it.copy(message = null) else it }
    }
}
