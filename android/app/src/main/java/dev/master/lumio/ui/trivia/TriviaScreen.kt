package dev.master.lumio.ui.trivia

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.layout.ContentScale
import dev.master.lumio.ui.theme.LocalLumioDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    val scroll = rememberScrollState()
    val round = (state as? TriviaState.Playing)?.round
    LaunchedEffect(round?.index, round?.phase, round?.paused) {
        if (round?.phase != TriviaPhase.FEEDBACK || round.paused) scroll.scrollTo(0)
    }
    LaunchedEffect(round?.phase, scroll.maxValue) {
        if (round?.phase == TriviaPhase.FEEDBACK && !round.paused) scroll.animateScrollTo(scroll.maxValue)
    }
    val pane = stringResource(R.string.trivia)
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp).semantics { paneTitle = pane },
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            TriviaState.Ready -> {
                TriviaArtwork()
                Text(stringResource(R.string.trivia_welcome), style = MaterialTheme.typography.displaySmall)
                Text(stringResource(R.string.trivia_welcome_hint), style = MaterialTheme.typography.bodyLarge)
                TriviaTicket {
                    Text(stringResource(R.string.trivia_ticket_rules), style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(painterResource(R.drawable.lucide_play), null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.trivia_lets_play))
                }
            }
            TriviaState.Loading -> {
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
                    TriviaArtwork()
                    Text(stringResource(if (round.correctCount == round.questions.size) R.string.trivia_perfect else R.string.trivia_finish_cheer),
                        color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.round_complete), style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    val animatedScore by animateIntAsState(round.score, label = "score")
                    TriviaTicket {
                        Text(stringResource(R.string.trivia_points, animatedScore), style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.trivia_correct_total, round.correctCount, round.questions.size))
                    }
                    Text(stringResource(R.string.trivia_correct_total, round.correctCount, round.questions.size))
                    Text(stringResource(R.string.trivia_best_streak, round.bestStreak))
                    Button(onClick = again) { Text(stringResource(R.string.play_again)) }
                    OutlinedButton(onClick = back) { Text(stringResource(R.string.back_to_arcade)) }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.trivia_question_count, round.index + 1, round.questions.size), style = MaterialTheme.typography.titleMedium)
                        if (!round.paused) OutlinedButton(onClick = fifty,
                            enabled = !round.fiftyUsed && round.phase == TriviaPhase.QUESTION) {
                            Text(stringResource(if (round.fiftyUsed) R.string.fifty_used else R.string.fifty_fifty))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(round.questions.size) { index ->
                            Surface(Modifier.weight(1f).height(6.dp), shape = RoundedCornerShape(3.dp),
                                color = if (index <= round.index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {}
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.trivia_points, round.score), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.trivia_streak, round.streak), color = MaterialTheme.colorScheme.secondary)
                    }
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
                        TriviaTicket {
                            Text(stringResource(R.string.trivia_ticket_label), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary)
                            AnimatedContent(targetState = round.index, label = "question") { index ->
                                Text(round.questions[index].question, style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            }
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
                                val animatedAccent by animateColorAsState(accent, label = "answer feedback")
                                OutlinedButton(onClick = { answer(index) }, enabled = !feedback,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).animateContentSize(),
                                    shape = RoundedCornerShape(16.dp), border = BorderStroke(if (correct || wrong) 2.dp else 1.dp, animatedAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = accent)) {
                                    Text("${('A'.code + index).toChar()}", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(16.dp))
                                    Text(option, Modifier.weight(1f))
                                    if (correct || wrong) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(painterResource(if (correct) R.drawable.lucide_check else R.drawable.lucide_x),
                                            stringResource(if (correct) R.string.trivia_correct else R.string.trivia_incorrect), Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(visible = feedback) {
                          Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val correct = round.selected == round.question.answerIndex
                            Text(stringResource(when {
                                round.selected == null -> R.string.trivia_timeout
                                correct -> R.string.trivia_correct
                                else -> R.string.trivia_incorrect
                            }), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            Text(stringResource(if (correct) R.string.trivia_correct_cheer else R.string.trivia_try_cheer),
                                style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.trivia_answer, round.question.options[round.question.answerIndex]))
                            Text(stringResource(R.string.trivia_earned, round.earned))
                            Text(stringResource(if (round.holdFeedback) R.string.reveal_held else R.string.reveal_countdown,
                                ((round.feedbackMs + 999) / 1000).toInt()))
                            OutlinedButton(onClick = hold, enabled = !round.holdFeedback,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                                Text(stringResource(R.string.keep_reading))
                            }
                            Button(onClick = next, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                                Text(stringResource(if (round.index == round.questions.lastIndex) R.string.see_results else R.string.next_question))
                            }
                          }
                        }
                    }
                }
            }
        }
    }
}

/** Editorial ticket silhouette, inspired by the saved Golden Ticket direction. */
private val TicketShape = GenericShape { size, _ ->
    val notch = size.width * 0.035f
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height / 2 - notch)
    quadraticTo(size.width - notch * 2, size.height / 2, size.width, size.height / 2 + notch)
    lineTo(size.width, size.height); lineTo(0f, size.height); lineTo(0f, size.height / 2 + notch)
    quadraticTo(notch * 2, size.height / 2, 0f, size.height / 2 - notch)
    close()
}

@Composable private fun TriviaTicket(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().animateContentSize(), shape = TicketShape,
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable private fun TriviaArtwork() {
    Image(painterResource(R.drawable.trivia_ticket), null,
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
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
