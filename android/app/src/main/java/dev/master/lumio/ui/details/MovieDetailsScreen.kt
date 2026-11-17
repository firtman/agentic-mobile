package dev.master.lumio.ui.details

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.master.lumio.R
import dev.master.lumio.data.Movie
import dev.master.lumio.data.ratings.RatingValue
import dev.master.lumio.ui.components.MoviePoster
import dev.master.lumio.ui.components.MovieSourceLinks
import dev.master.lumio.ui.theme.*
import java.text.NumberFormat

@Composable
fun MovieDetailsScreen(
    state: MovieDetailsUiState,
    onRate: (RatingValue) -> Unit,
    onUndo: (Long) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onMessageShown: (Long) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message
    val savedLabels = mapOf(
        RatingValue.LIKE to stringResource(R.string.rating_saved_like),
        RatingValue.DISLIKE to stringResource(R.string.rating_saved_dislike),
        RatingValue.UNSEEN to stringResource(R.string.rating_saved_unseen),
    )
    val undoLabel = stringResource(R.string.undo)
    val undoneLabel = stringResource(R.string.rating_undone)
    val failedLabel = stringResource(R.string.rating_error)
    LaunchedEffect(message?.id) {
        if (message == null) return@LaunchedEffect
        // Clear the message only after the snackbar closes: clearing it changes this effect's key.
        when (message) {
            is DetailsMessage.Saved -> {
                val result = snackbar.showSnackbar(savedLabels.getValue(message.value), undoLabel, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) onUndo(message.undoId)
            }
            is DetailsMessage.Undone -> snackbar.showSnackbar(undoneLabel)
            is DetailsMessage.SaveFailed -> snackbar.showSnackbar(failedLabel, duration = SnackbarDuration.Long)
        }
        onMessageShown(message.id)
    }
    val paneLabel = stringResource(R.string.movie_details)
    Box(Modifier.fillMaxSize().semantics { paneTitle = paneLabel }) {
        when (val result = state.result) {
            DetailsResult.Loading -> StatusColumn {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.loading_movie), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            DetailsResult.Error -> StatusColumn {
                Icon(painterResource(R.drawable.lucide_wifi_off), null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.connection_error), style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Button(onClick = onRetry, modifier = Modifier.testTag("details_retry")) { Text(stringResource(R.string.retry)) }
            }
            DetailsResult.NotFound -> StatusColumn {
                Text(stringResource(R.string.movie_not_found), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            is DetailsResult.Content -> DetailsContent(result.movie, state.rating, onRate)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { data ->
            Snackbar(data, shape = RoundedCornerShape(8.dp), containerColor = Ink, contentColor = Paper, actionColor = Coral)
        }
    }
}

@Composable private fun StatusColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable private fun DetailsContent(movie: Movie, rating: RatingValue?, onRate: (RatingValue) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val wide = maxWidth >= 720.dp
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("details_content")
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .padding(bottom = 72.dp)) {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                MoviePoster(movie, Modifier.width(300.dp).aspectRatio(2f / 3f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    TitleBlock(movie, TextAlign.Start)
                    RatingButtons(rating, onRate)
                    InfoSections(movie)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                MoviePoster(movie, Modifier.widthIn(max = 300.dp).fillMaxWidth(0.72f).aspectRatio(2f / 3f).align(Alignment.CenterHorizontally))
                TitleBlock(movie, TextAlign.Center)
                RatingButtons(rating, onRate)
                InfoSections(movie)
            }
        }
    }
    }
}

@Composable private fun TitleBlock(movie: Movie, align: TextAlign) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val meta = listOfNotNull(
            movie.year.takeIf { it > 0 }?.toString(),
            movie.runtime.takeIf { it > 0 }?.let { stringResource(R.string.runtime_minutes, it) },
            movie.genres.takeIf { it.isNotEmpty() }?.joinToString(", "),
        )
        if (meta.isNotEmpty()) Text(meta.joinToString(" · ").uppercase(), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align, modifier = Modifier.fillMaxWidth())
        Text(movie.title, style = MaterialTheme.typography.displayLarge, textAlign = align,
            modifier = Modifier.fillMaxWidth().semantics { heading() })
    }
}

private data class RatingOption(val value: RatingValue, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)
private val ratingOptions = listOf(
    RatingOption(RatingValue.DISLIKE, R.string.dislike, R.drawable.lucide_thumbs_down),
    RatingOption(RatingValue.UNSEEN, R.string.unseen, R.drawable.lucide_eye_off),
    RatingOption(RatingValue.LIKE, R.string.like, R.drawable.lucide_thumbs_up),
)

// Buttons stay enabled while saving so TalkBack focus is stable; the ViewModel ignores taps until the save ends.
@Composable private fun RatingButtons(rating: RatingValue?, onRate: (RatingValue) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.your_rating), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ratingOptions.forEach { option ->
                val selected = rating == option.value
                Column(
                    Modifier.weight(1f).fillMaxHeight().heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Coral else MaterialTheme.colorScheme.surfaceContainer)
                        .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton) { onRate(option.value) }
                        .testTag("rate_" + option.value.name.lowercase())
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    val tint = if (selected) Ink else MaterialTheme.colorScheme.onSurface
                    Icon(painterResource(option.icon), null, Modifier.size(24.dp), tint = tint)
                    Text(stringResource(option.label), style = MaterialTheme.typography.labelLarge, color = tint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable private fun InfoSections(movie: Movie) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (movie.synopsis.isNotBlank()) {
            Text(stringResource(R.string.synopsis), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(movie.synopsis, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text(stringResource(R.string.movie_information), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        val rows = buildList {
            if (movie.year > 0) add(R.string.release_year to movie.year.toString())
            if (movie.genres.isNotEmpty()) add(R.string.genres to movie.genres.joinToString(", "))
            if (movie.runtime > 0) add(R.string.runtime to stringResource(R.string.runtime_minutes, movie.runtime))
            if (movie.director.isNotBlank()) add(R.string.director to movie.director)
            if (movie.cast.isNotEmpty()) add(R.string.cast to movie.cast.joinToString(", "))
            if (movie.rating > 0) {
                val score = NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(movie.rating)
                add(R.string.source_rating to if (movie.voteCount > 0) {
                    pluralStringResource(R.plurals.source_rating_votes, movie.voteCount, score, movie.voteCount)
                } else stringResource(R.string.source_rating_value, score))
            }
        }
        if (rows.isEmpty()) {
            Text(stringResource(R.string.no_movie_information), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else rows.forEach { (label, value) ->
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
        MovieSourceLinks(movie)
    }
}

private val previewMovie = Movie(
    id = "preview-details", title = "The Quiet Observatory", year = 2024,
    synopsis = "A fictional placeholder synopsis used only for design previews.",
    genres = listOf("Drama", "Mystery"), runtime = 112,
)
@Preview(widthDp = 412, heightDp = 1200) @Composable private fun DetailsLightPreview() {
    LumioTheme(false) { Surface { MovieDetailsScreen(MovieDetailsUiState(DetailsResult.Content(previewMovie), RatingValue.LIKE), {}, {}, {}, {}, {}) } }
}
@Preview(widthDp = 412, heightDp = 1400, fontScale = 2f) @Composable private fun DetailsDarkLargePreview() {
    LumioTheme(true) { Surface { MovieDetailsScreen(MovieDetailsUiState(DetailsResult.Content(previewMovie.copy(synopsis = "")), RatingValue.UNSEEN), {}, {}, {}, {}, {}) } }
}
@Preview(widthDp = 412, heightDp = 600) @Composable private fun DetailsErrorPreview() {
    LumioTheme(false) { Surface { MovieDetailsScreen(MovieDetailsUiState(DetailsResult.Error), {}, {}, {}, {}, {}) } }
}
