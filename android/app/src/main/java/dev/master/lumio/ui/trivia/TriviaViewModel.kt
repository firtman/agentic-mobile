package dev.master.lumio.ui.trivia

import android.os.SystemClock
import dev.master.lumio.ui.arcade.GameCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.master.lumio.data.Catalog
import dev.master.lumio.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

sealed interface TriviaState {
    data object Ready : TriviaState
    data object Loading : TriviaState
    data object Error : TriviaState
    data object Unavailable : TriviaState
    data class Playing(val round: TriviaRound) : TriviaState
}

class TriviaViewModel(
    private val catalog: suspend () -> Catalog,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val random: Random = Random.Default,
    private val questionCount: Int = 5,
    private val secondsPerQuestion: Int = 20,
) : ViewModel() {
    private val cues = MutableSharedFlow<GameCue>(extraBufferCapacity = 4)
    val sounds = cues.asSharedFlow()
    private val mutableState = MutableStateFlow<TriviaState>(TriviaState.Ready)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var tickJob: Job? = null
    private var visible = false
    private var lastTick = 0L

    fun start() {
        if (state.value is TriviaState.Playing || state.value == TriviaState.Loading) return
        load()
    }

    fun playAgain() { load() }
    fun retry() { load() }

    private fun load() {
        loadJob?.cancel()
        tickJob?.cancel()
        mutableState.value = TriviaState.Loading
        loadJob = viewModelScope.launch {
            try {
                val questions = selectTrivia(catalog().trivia, questionCount, random)
                mutableState.value = if (questions.size < questionCount) TriviaState.Unavailable
                    else TriviaState.Playing(TriviaRound(questions, secondsPerQuestion, paused = !visible))
                lastTick = clock()
                if (visible && mutableState.value is TriviaState.Playing) ticker()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = TriviaState.Error
            }
        }
    }

    /** Active rounds require explicit Resume after navigation, backgrounding or recreation. */
    fun setVisible(value: Boolean) {
        if (visible == value) return
        if (!value) pause()
        visible = value
        lastTick = clock()
    }

    fun pause() {
        syncClock()
        transform { if (it.phase == TriviaPhase.RESULTS) it else it.copy(paused = true) }
        tickJob?.cancel()
    }

    fun resume() {
        if (!visible) return
        lastTick = clock()
        transform { it.copy(paused = false) }
        ticker()
    }

    fun answer(option: Int) { syncClock(); transform { it.answer(option) } }
    fun fiftyFifty() { syncClock(); transform { it.fiftyFifty(random) } }
    fun hold() { syncClock(); transform { if (it.phase == TriviaPhase.FEEDBACK && !it.paused) it.copy(holdFeedback = true) else it } }
    fun next() { syncClock(); transform { it.next() }; lastTick = clock() }

    private fun transform(action: (TriviaRound) -> TriviaRound) {
        val old = (state.value as? TriviaState.Playing)?.round ?: return
        val next = action(old)
        mutableState.value = TriviaState.Playing(next)
        if (old.phase == TriviaPhase.QUESTION && next.phase == TriviaPhase.FEEDBACK) {
            cues.tryEmit(if (next.selected == next.question.answerIndex) GameCue.CORRECT else GameCue.INCORRECT)
        }
    }

    private fun syncClock() {
        val now = clock()
        val elapsed = (now - lastTick).coerceAtLeast(0)
        lastTick = now
        if (visible) transform { it.elapse(elapsed) }
    }

    private fun ticker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive && visible) {
                val round = (state.value as? TriviaState.Playing)?.round ?: break
                if (round.paused || round.phase == TriviaPhase.RESULTS) break
                delay(100)
                syncClock()
            }
        }
    }
}
