package dev.master.lumio.ui.trivia

import androidx.compose.foundation.BorderStroke
import dev.master.lumio.ui.theme.LocalLumioDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first
import dev.master.lumio.R
import dev.master.lumio.data.TriviaQuestion
import dev.master.lumio.domain.*
import dev.master.lumio.ui.theme.LumioTheme

/** Hide question contents while paused. Lifecycle visibility never resumes a paused game for the player. */
@Composable
fun TriviaScreen(state: TriviaState, start: () -> Unit, retry: () -> Unit, answer: (Int) -> Unit,
                 pause: () -> Unit, resume: () -> Unit, fifty: () -> Unit, hold: () -> Unit,
                 next: () -> Unit, again: () -> Unit, back: () -> Unit, visibility: (Boolean) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestVisibility by rememberUpdatedState(visibility)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestVisibility(true)
            if (event == Lifecycle.Event.ON_PAUSE) latestVisibility(false)
        }
        lifecycle.addObserver(observer)
        latestVisibility(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { latestVisibility(false); lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        start()
    }
    val pane = stringResource(R.string.trivia)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).semantics { paneTitle = pane },
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            TriviaState.Ready, TriviaState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.loading_trivia))
            }
            TriviaState.Error -> {
                Text(stringResource(R.string.connection_error), color = MaterialTheme.colorScheme.error)
                Button(onClick = retry) { Text(stringResource(R.string.retry)) }
            }
            TriviaState.Unavailable -> {
                Text(stringResource(R.string.no_trivia), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.no_trivia_hint))
                OutlinedButton(onClick = back) { Text(stringResource(R.string.back_to_arcade)) }
            }
            is TriviaState.Playing -> {
                val round = state.round
                if (round.phase == TriviaPhase.RESULTS) {
                    Text(stringResource(R.string.round_complete), style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(stringResource(R.string.trivia_points, round.score), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.trivia_correct_total, round.correctCount, round.questions.size))
                    Text(stringResource(R.string.trivia_best_streak, round.bestStreak))
                    Button(onClick = again) { Text(stringResource(R.string.play_again)) }
                    OutlinedButton(onClick = back) { Text(stringResource(R.string.back_to_arcade)) }
                } else {
                    Text(stringResource(R.string.trivia_question_count, round.index + 1, round.questions.size), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.trivia_points, round.score))
                    Text(stringResource(R.string.trivia_streak, round.streak))
                    if (round.paused) {
                        Text(stringResource(R.string.trivia_paused), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.trivia_pause_hint))
                        Button(onClick = resume) {
                            Icon(painterResource(R.drawable.lucide_play), null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.resume))
                        }
                        OutlinedButton(onClick = back) { Text(stringResource(R.string.back_to_arcade)) }
                    } else {
                        TextButton(onClick = pause, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(painterResource(R.drawable.lucide_pause), null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.pause))
                        }
                        if (round.phase == TriviaPhase.QUESTION) {
                            Text(stringResource(R.string.trivia_seconds, ((round.remainingMs + 999) / 1000).toInt()),
                                style = MaterialTheme.typography.headlineMedium)
                            LinearProgressIndicator(progress = { (round.remainingMs.toFloat() / (round.secondsPerQuestion * 1000)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant)
                        }
                        key(round.index) {
                            Text(round.question.question, style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                        val feedback = round.phase == TriviaPhase.FEEDBACK
                        round.question.options.forEachIndexed { index, option ->
                            if (feedback || index !in round.eliminated) {
                                val correct = feedback && index == round.question.answerIndex
                                val wrong = feedback && index == round.selected && !correct
                                val accent = when {
                                    correct -> if (LocalLumioDark.current) Color(0xFF9CC8AA) else Color(0xFF3C6750)
                                    wrong -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                OutlinedButton(onClick = { answer(index) }, enabled = !feedback,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    shape = RoundedCornerShape(8.dp), border = BorderStroke(if (correct || wrong) 2.dp else 1.dp, accent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = accent)) {
                                    Text("${('A'.code + index).toChar()} · $option", Modifier.fillMaxWidth())
                                }
                            }
                        }
                        if (!feedback) {
                            OutlinedButton(onClick = fifty, enabled = !round.fiftyUsed,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                                Text(stringResource(if (round.fiftyUsed) R.string.fifty_used else R.string.fifty_fifty))
                            }
                        } else {
                            val correct = round.selected == round.question.answerIndex
                            Text(stringResource(when {
                                round.selected == null -> R.string.trivia_timeout
                                correct -> R.string.trivia_correct
                                else -> R.string.trivia_incorrect
                            }), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            Text(stringResource(R.string.trivia_answer, round.question.options[round.question.answerIndex]))
                            Text(stringResource(R.string.trivia_earned, round.earned))
                            Text(stringResource(if (round.holdFeedback) R.string.reveal_held else R.string.reveal_countdown,
                                ((round.feedbackMs + 999) / 1000).toInt()))
                            OutlinedButton(onClick = hold, enabled = !round.holdFeedback,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                                Text(stringResource(R.string.keep_reading))
                            }
                            Button(onClick = next) { Text(stringResource(if (round.index == round.questions.lastIndex) R.string.see_results else R.string.next_question)) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable private fun TriviaLightPreview() {
    val round = TriviaRound(listOf(TriviaQuestion("preview", "fictional", "sample", "Which word describes a film’s written script?",
        listOf("Screenplay", "Storyboard", "Soundtrack", "Trailer"), 0)), 20)
    LumioTheme(false) { Surface { TriviaScreen(TriviaState.Playing(round), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
}
@Preview(showBackground = true, widthDp = 400, heightDp = 900, fontScale = 2f)
@Composable private fun TriviaDarkLargePreview() {
    LumioTheme(true) { Surface { TriviaScreen(TriviaState.Playing(TriviaRound(listOf(TriviaQuestion("preview", "fictional", "sample", "Sample?",
        listOf("A", "B", "C", "D"), 0)), 20, paused = true)), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
}
