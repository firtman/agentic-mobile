package dev.master.lumio.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.*
import dev.master.lumio.data.ratings.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SwipeResult {
    data object Loading : SwipeResult
    data object Empty : SwipeResult
    data object Error : SwipeResult
    data object StorageError : SwipeResult
    data class Card(val movie: Movie) : SwipeResult
}
data class SwipeUiState(
    val result: SwipeResult = SwipeResult.Loading,
    val saving: Boolean = false,
    val undoId: Long? = null,
    val message: Int? = null,
)

/** Only unread pages live in memory. Saved ratings and undo remain in Room. */
class SwipeViewModel(private val movies: ExploreRepository, private val ratings: RatingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SwipeUiState())
    val state = mutableState.asStateFlow()
    private val queue = mutableListOf<Movie>()
    private var ratedIds: Set<String>? = null
    private var offset = 0
    private var hasMore = true
    private var loadJob: Job? = null
    private var observeJob: Job? = null

    init { observeRatings() }

    private fun observeRatings() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(ratings.ratedIds(), ratings.latestUndo()) { ids, undo -> ids to undo }
                .catch { e ->
                    if (e is CancellationException) throw e
                    loadJob?.cancel()
                    mutableState.update { it.copy(result = SwipeResult.StorageError) }
                }.collect { (ids, undo) ->
                    val removed = ratedIds.orEmpty().any { it !in ids }
                    ratedIds = ids
                    // An Undo in details can make an already-consumed card eligible again.
                    if (removed && !state.value.saving) resetQueue()
                    mutableState.update { it.copy(undoId = undo?.id) }
                    if (!state.value.saving) showNext()
                }
        }
    }

    private fun resetQueue() {
        loadJob?.cancel()
        loadJob = null
        queue.clear()
        offset = 0
        hasMore = true
        mutableState.update { it.copy(result = SwipeResult.Loading) }
    }

    private fun showNext() {
        val ids = ratedIds ?: return
        // Keep no rated catalog records in the queue. Undo reloads online in popularity order.
        queue.removeAll { it.id in ids }
        if (queue.isNotEmpty()) {
            mutableState.update { it.copy(result = SwipeResult.Card(queue.first())) }
        } else if (!hasMore) {
            mutableState.update { it.copy(result = SwipeResult.Empty) }
        } else if (loadJob?.isActive != true && state.value.result != SwipeResult.Error &&
            state.value.result != SwipeResult.StorageError) {
            loadPage()
        }
    }

    private fun loadPage() {
        mutableState.update { it.copy(result = SwipeResult.Loading) }
        loadJob = viewModelScope.launch {
            try {
                // Fully-rated pages are skipped without displaying a false empty state.
                while (queue.isEmpty() && hasMore) {
                    val page = movies.movies(ExploreFilters(sort = MovieSort.POPULARITY_DESC), offset)
                    hasMore = page.hasMore && page.movies.isNotEmpty()
                    offset = page.offset + page.movies.size
                    queue.addAll(page.movies.distinctBy { it.id }.filter { it.id !in ratedIds.orEmpty() })
                }
                mutableState.update { it.copy(result = queue.firstOrNull()?.let(SwipeResult::Card) ?: SwipeResult.Empty) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(result = SwipeResult.Error) }
            }
        }
    }

    fun retry() {
        if (state.value.saving) return
        if (state.value.result == SwipeResult.StorageError) {
            mutableState.update { it.copy(result = SwipeResult.Loading) }
            observeRatings()
        } else {
            loadJob?.cancel()
            if (state.value.result == SwipeResult.Empty) {
                offset = 0
                hasMore = true
            }
            loadPage()
        }
    }

    fun rate(value: RatingValue) {
        val movie = (state.value.result as? SwipeResult.Card)?.movie ?: return
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            try {
                ratings.rate(movie, value)
                // Advance only after the transaction succeeds, including before Room emits.
                ratedIds = ratedIds.orEmpty() + movie.id
                queue.removeAll { it.id == movie.id }
                mutableState.update { it.copy(saving = false) }
                showNext()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, message = dev.master.lumio.R.string.rating_error) }
            }
        }
    }

    fun undo() {
        val id = state.value.undoId ?: return
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            try {
                val restored = ratings.undo(id)
                mutableState.update { it.copy(saving = false, message = if (restored) dev.master.lumio.R.string.rating_undone else null) }
                if (restored) {
                    // Wait for the persisted ids after undo; a restored previous rating stays excluded.
                    ratedIds = ratings.ratedIds().first()
                    resetQueue()
                    loadPage()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, message = dev.master.lumio.R.string.rating_error) }
            }
        }
    }

    fun messageShown() { mutableState.update { it.copy(message = null) } }
}

/** Physical directions, independent of language direction; upward/short drags never rate. */
fun swipeChoice(x: Float, y: Float, threshold: Float): RatingValue? = when {
    kotlin.math.abs(x) >= threshold && kotlin.math.abs(x) > kotlin.math.abs(y) ->
        if (x > 0) RatingValue.LIKE else RatingValue.DISLIKE
    y >= threshold && y > kotlin.math.abs(x) -> RatingValue.UNSEEN
    else -> null
}
