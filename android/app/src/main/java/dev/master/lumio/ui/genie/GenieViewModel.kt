package dev.master.lumio.ui.genie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.Catalog
import dev.master.lumio.data.ratings.RatingValue
import dev.master.lumio.data.ratings.RatingsRepository
import dev.master.lumio.domain.Recommendation
import dev.master.lumio.domain.RecommendationEngine
import dev.master.lumio.domain.TastePreference
import dev.master.lumio.ui.history.snapshotMovie
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface GenieState {
    data object Loading : GenieState
    data object GetStarted : GenieState
    data object NoMatches : GenieState
    data object NetworkError : GenieState
    data object StorageError : GenieState
    data class Content(val recommendations: List<Recommendation>) : GenieState
}

private data class PreferenceLoad(val preferences: List<TastePreference>?)

@OptIn(ExperimentalCoroutinesApi::class)
class GenieViewModel(
    private val catalog: suspend () -> Catalog,
    private val ratings: RatingsRepository,
    private val scoringDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutableState = MutableStateFlow<GenieState>(GenieState.Loading)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private val engine = RecommendationEngine()

    init { retry() }

    fun retry() {
        job?.cancel()
        mutableState.value = GenieState.Loading
        job = viewModelScope.launch {
            ratings.history().map { rows -> PreferenceLoad(rows.map { row ->
                TastePreference(row.rating.snapshotMovie(), when (row.rating.value) {
                    RatingValue.LIKE -> 1
                    RatingValue.DISLIKE -> -1
                    RatingValue.UNSEEN -> 0
                })
            }) }.distinctUntilChanged().catch { e ->
                if (e is CancellationException) throw e
                emit(PreferenceLoad(null))
            }.collectLatest { input ->
                val preferences = input.preferences
                if (preferences == null) {
                    mutableState.value = GenieState.StorageError
                } else if (preferences.none { it.direction != 0 }) {
                    mutableState.value = GenieState.GetStarted
                } else {
                    mutableState.value = GenieState.Loading
                    try {
                        val movies = catalog().movies
                        val recommendations = withContext(scoringDispatcher) {
                            engine.recommend(movies, preferences)
                        }
                        mutableState.value = if (recommendations.isEmpty()) GenieState.NoMatches else GenieState.Content(recommendations)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutableState.value = GenieState.NetworkError
                    }
                }
            }
        }
    }
}
