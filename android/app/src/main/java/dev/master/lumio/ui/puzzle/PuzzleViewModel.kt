package dev.master.lumio.ui.puzzle

import dev.master.lumio.ui.arcade.GameCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.Catalog
import dev.master.lumio.data.Movie
import dev.master.lumio.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

enum class PuzzlePoster { LOADING, READY, FAILED }
data class PuzzleSession(val id: Long, val movie: Movie, val board: PuzzleBoard, val poster: PuzzlePoster = PuzzlePoster.LOADING, val reference: Boolean = false)
sealed interface PuzzleState {
    data object Ready : PuzzleState
    data object Loading : PuzzleState
    data object Error : PuzzleState
    data object Unavailable : PuzzleState
    data class Playing(val session: PuzzleSession) : PuzzleState
}
class PuzzleViewModel(private val catalog: suspend () -> Catalog, private val random: Random = Random.Default) : ViewModel() {
    private val cues = MutableSharedFlow<GameCue>(extraBufferCapacity = 4)
    val sounds = cues.asSharedFlow()
    private val mutableState = MutableStateFlow<PuzzleState>(PuzzleState.Ready)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var previousId: String? = null
    private var dimension = 3
    private var nextId = 0L
    private val failedPosters = mutableSetOf<String>()
    // Validated once per catalog instance; Next puzzle then avoids re-parsing every poster URL.
    private var candidateSource: List<Movie>? = null
    private var candidates: List<Movie> = emptyList()

    fun start() { if (state.value == PuzzleState.Ready) load() }
    fun retry() { failedPosters.clear(); load() }
    fun nextPuzzle() { load() }

    private fun load() {
        loadJob?.cancel()
        mutableState.value = PuzzleState.Loading
        loadJob = viewModelScope.launch {
            try {
                val movies = catalog().movies
                if (candidateSource !== movies) { candidates = puzzleCandidates(movies); candidateSource = movies }
                val movie = pickPuzzleMovie(candidates.filter { it.id !in failedPosters }, previousId, random)
                if (movie == null) mutableState.value = PuzzleState.Unavailable
                else {
                    previousId = movie.id
                    mutableState.value = PuzzleState.Playing(PuzzleSession(++nextId, movie, PuzzleBoard.shuffled(random, dimension)))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = PuzzleState.Error }
        }
    }

    fun choose(position: Int) {
        val old = (state.value as? PuzzleState.Playing)?.session ?: return
        if (old.reference || old.poster != PuzzlePoster.READY) return
        val board = old.board.choose(position)
        mutableState.value = PuzzleState.Playing(old.copy(board = board))
        if (board.moves > old.board.moves) cues.tryEmit(if (board.complete) GameCue.COMPLETE else GameCue.SWAP)
    }

    fun swap(first: Int, second: Int) {
        val old = (state.value as? PuzzleState.Playing)?.session ?: return
        if (old.reference || old.poster != PuzzlePoster.READY) return
        val board = old.board.swap(first, second)
        mutableState.value = PuzzleState.Playing(old.copy(board = board))
        if (board.moves > old.board.moves) cues.tryEmit(if (board.complete) GameCue.COMPLETE else GameCue.SWAP)
    }

    fun difficulty(size: Int) {
        if (size !in listOf(3, 4, 6) || size == dimension) return
        dimension = size
        mutableState.update { if (it is PuzzleState.Playing)
            it.copy(session = it.session.copy(board = PuzzleBoard.shuffled(random, dimension), reference = false)) else it }
    }

    fun toggleReference() {
        mutableState.update { if (it is PuzzleState.Playing && it.session.poster == PuzzlePoster.READY && !it.session.board.complete)
            it.copy(session = it.session.copy(reference = !it.session.reference, board = it.session.board.copy(selected = null))) else it }
    }

    /** Failed artwork is never playable and is skipped by Next puzzle. */
    fun posterFailed(id: Long) {
        val current = (state.value as? PuzzleState.Playing)?.session ?: return
        if (current.id != id) return
        failedPosters += current.movie.id
        mutableState.value = PuzzleState.Playing(current.copy(poster = PuzzlePoster.FAILED, reference = false, board = current.board.copy(selected = null)))
    }

    fun posterLoaded(id: Long) {
        val current = (state.value as? PuzzleState.Playing)?.session ?: return
        if (current.id != id) return
        failedPosters -= current.movie.id
        mutableState.value = PuzzleState.Playing(current.copy(poster = PuzzlePoster.READY))
    }

    /** A new attempt id prevents a late result from the old request changing the retry. */
    fun retryPoster() {
        val current = (state.value as? PuzzleState.Playing)?.session ?: return
        if (current.poster != PuzzlePoster.FAILED) return
        mutableState.value = PuzzleState.Playing(current.copy(id = ++nextId, poster = PuzzlePoster.LOADING, reference = false))
    }
}
