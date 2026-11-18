package dev.master.lumio.ui.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExploreResult {
    data object Loading : ExploreResult
    data object Empty : ExploreResult
    data object Error : ExploreResult
    data class Content(
        val movies: List<Movie>, val total: Int, val nextOffset: Int?,
        val loadingMore: Boolean = false, val pageError: Boolean = false,
    ) : ExploreResult
}
data class ExploreUiState(
    val filters: ExploreFilters = ExploreFilters(),
    val genres: List<String> = emptyList(),
    val genresLoading: Boolean = true, val genresError: Boolean = false,
    val result: ExploreResult = ExploreResult.Loading,
)

class ExploreViewModel(
    private val repository: ExploreRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val restored = ExploreFilters(
        query = savedState["query"] ?: "",
        genre = savedState["genre"],
        sort = MovieSort.entries.find { it.name == savedState.get<String>("sort") } ?: MovieSort.YEAR_DESC,
    )
    private val mutableState = MutableStateFlow(ExploreUiState(filters = restored))
    val state = mutableState.asStateFlow()
    private var pageJob: Job? = null
    private var genreJob: Job? = null
    private var generation = 0

    init { retryGenres(); reload() }

    fun search(query: String) {
        if (query == state.value.filters.query) return
        setFilters(state.value.filters.copy(query = query), debounce = true)
    }
    fun selectGenre(genre: String?) {
        if (genre == state.value.filters.genre) return
        setFilters(state.value.filters.copy(genre = genre))
    }
    fun sort(sort: MovieSort) {
        if (sort == state.value.filters.sort) return
        setFilters(state.value.filters.copy(sort = sort))
    }
    fun resetFilters() = setFilters(ExploreFilters())
    private fun setFilters(filters: ExploreFilters, debounce: Boolean = false) {
        savedState["query"] = filters.query
        savedState["genre"] = filters.genre
        savedState["sort"] = filters.sort.name
        mutableState.update { it.copy(filters = filters) }
        reload(debounce)
    }
    fun refresh() {
        if (state.value.genresError) retryGenres()
        reload()
    }
    private fun reload(debounce: Boolean = false) {
        val currentGeneration = ++generation
        pageJob?.cancel()
        val filters = state.value.filters
        mutableState.update { it.copy(result = ExploreResult.Loading) }
        pageJob = viewModelScope.launch {
            if (debounce) delay(300)
            try {
                val page = repository.movies(filters, 0)
                if (currentGeneration != generation) return@launch
                val result = if (page.movies.isEmpty()) ExploreResult.Empty else ExploreResult.Content(
                    movies = page.movies.distinctBy { it.id }, total = page.total,
                    nextOffset = nextOffset(page),
                )
                mutableState.update { it.copy(result = result) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) {
                if (currentGeneration == generation) mutableState.update { it.copy(result = ExploreResult.Error) }
            }
        }
    }
    fun loadMore() {
        val content = state.value.result as? ExploreResult.Content ?: return
        val offset = content.nextOffset ?: return
        if (content.loadingMore) return
        val currentGeneration = generation
        val filters = state.value.filters
        mutableState.update { it.copy(result = content.copy(loadingMore = true, pageError = false)) }
        pageJob = viewModelScope.launch {
            try {
                val page = repository.movies(filters, offset)
                if (currentGeneration != generation) return@launch
                mutableState.update { it.copy(result = ExploreResult.Content(
                    movies = (content.movies + page.movies).distinctBy { movie -> movie.id },
                    total = page.total, nextOffset = nextOffset(page),
                )) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) {
                if (currentGeneration == generation) mutableState.update {
                    it.copy(result = content.copy(loadingMore = false, pageError = true))
                }
            }
        }
    }
    fun retryGenres() {
        if (genreJob?.isActive == true) return
        mutableState.update { it.copy(genresLoading = true, genresError = false) }
        genreJob = viewModelScope.launch {
            try {
                val genres = repository.genres().filter { it.isNotBlank() }.distinct()
                mutableState.update { it.copy(genres = genres, genresLoading = false, genresError = false) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(genresLoading = false, genresError = true) }
            }
        }
    }
    // Empty pages end pagination even if a changing server total still promises more.
    private fun nextOffset(page: MoviePage): Int? =
        if (page.movies.isNotEmpty() && page.hasMore) page.offset + page.movies.size else null
}
