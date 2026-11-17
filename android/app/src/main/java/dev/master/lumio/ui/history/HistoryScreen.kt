package dev.master.lumio.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.master.lumio.R
import dev.master.lumio.data.ratings.*
import dev.master.lumio.ui.components.MoviePoster
import dev.master.lumio.ui.theme.*
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(state: HistoryUiState, rate: (String, RatingValue) -> Unit, undo: (String) -> Unit,
                  retry: () -> Unit, openDetails: (String) -> Unit, messageShown: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingValue by rememberSaveable { mutableStateOf(RatingValue.LIKE.name) }
    val rows = (state.result as? HistoryResult.Content)?.choices.orEmpty()
    val editing = rows.find { it.rating.movieId == editingId }
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); messageShown() }
    }
    val title = stringResource(R.string.history)
    Box(Modifier.fillMaxSize().semantics { paneTitle = title }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.history_intro), Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (state.result) {
                HistoryResult.Loading -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Text(stringResource(R.string.loading_choices))
                    }
                }
                HistoryResult.Error -> item {
                    Column(Modifier.widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.choices_load_error), color = MaterialTheme.colorScheme.error)
                        Button(onClick = retry) { Text(stringResource(R.string.retry)) }
                    }
                }
                HistoryResult.Empty -> item {
                    Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.history_empty_hint), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is HistoryResult.Content -> items(rows, key = { it.rating.movieId }) { choice ->
                    ChoiceCard(choice, state.saving, { openDetails(choice.rating.movieId) }, {
                        editingId = choice.rating.movieId
                        pendingValue = choice.rating.value.name
                    }, { undo(choice.rating.movieId) })
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { data ->
            Snackbar(data, shape = RoundedCornerShape(8.dp), containerColor = Ink, contentColor = Paper, actionColor = Coral)
        }
    }
    if (editing != null) {
        AlertDialog(onDismissRequest = { editingId = null },
            title = { Text(editing.rating.title, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(Modifier.selectableGroup()) {
                    RatingValue.entries.forEach { value ->
                        val selected = pendingValue == value.name
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { pendingValue = value.name })
                            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(ratingLabel(value)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    rate(editing.rating.movieId, RatingValue.valueOf(pendingValue))
                    editingId = null
                }, enabled = !state.saving && pendingValue != editing.rating.value.name,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                    Text(stringResource(R.string.save_choice))
                }
            },
            dismissButton = { TextButton(onClick = { editingId = null },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable private fun ChoiceCard(choice: HistoryChoice, saving: Boolean, open: () -> Unit, change: () -> Unit, undo: () -> Unit) {
    val rating = choice.rating
    val detailsLabel = stringResource(R.string.history_details_label, rating.title)
    val changeLabel = stringResource(R.string.change_choice_label, rating.title)
    val undoLabel = stringResource(R.string.undo_choice_label, rating.title)
    Surface(Modifier.widthIn(max = 680.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoviePoster(rating.snapshotMovie(), Modifier.width(72.dp).aspectRatio(2f / 3f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rating.title, style = MaterialTheme.typography.headlineSmall)
                    if (rating.year > 0) Text(rating.year.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(ratingLabel(rating.value)), style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(rating.updatedAt)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = open, modifier = Modifier.semantics { contentDescription = detailsLabel },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.open_details))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = change, enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .semantics { contentDescription = changeLabel },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(stringResource(R.string.change_choice))
                }
                OutlinedButton(onClick = undo, enabled = !saving && choice.undoId != null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = undoLabel },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Icon(painterResource(R.drawable.lucide_undo_2), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.undo))
                }
            }
        }
    }
}
private fun ratingLabel(value: RatingValue) = when (value) {
    RatingValue.LIKE -> R.string.like
    RatingValue.DISLIKE -> R.string.dislike
    RatingValue.UNSEEN -> R.string.unseen
}

private val previewChoice = HistoryChoice(RatingEntity("preview", RatingValue.UNSEEN, 0, "A Place Beyond the Moon",
    2024, "", listOf("Drama"), "", emptyList()), 1)
@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable private fun HistoryLightPreview() {
    LumioTheme(false) { Surface { HistoryScreen(HistoryUiState(HistoryResult.Content(listOf(previewChoice))), { _, _ -> }, {}, {}, {}, {}) } }
}
@Preview(showBackground = true, widthDp = 400, heightDp = 800, fontScale = 2f)
@Composable private fun HistoryDarkLargePreview() {
    LumioTheme(true) { Surface { HistoryScreen(HistoryUiState(HistoryResult.Content(listOf(previewChoice))), { _, _ -> }, {}, {}, {}, {}) } }
}
