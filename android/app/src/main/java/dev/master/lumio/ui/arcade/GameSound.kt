package dev.master.lumio.ui.arcade

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

enum class GameCue { CORRECT, INCORRECT, SWAP, COMPLETE }

/** No replay: restored games, recompositions and returning from Settings stay silent. */
@Composable fun GameSound(events: Flow<GameCue>, enabled: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(events, enabled, lifecycle) {
        if (enabled) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Audio service failures must never prevent playing a game.
            val generator = try { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }
                catch (_: RuntimeException) { null }
            try {
                events.collect { cue ->
                    try {
                        generator?.startTone(when (cue) {
                            GameCue.CORRECT -> ToneGenerator.TONE_PROP_ACK
                            GameCue.INCORRECT -> ToneGenerator.TONE_PROP_NACK
                            GameCue.SWAP -> ToneGenerator.TONE_PROP_BEEP
                            GameCue.COMPLETE -> ToneGenerator.TONE_PROP_ACK
                        }, if (cue == GameCue.COMPLETE) 220 else 100)
                    } catch (_: RuntimeException) { /* Sound is optional. */ }
                }
            } finally { generator?.release() }
        }
    }
}
