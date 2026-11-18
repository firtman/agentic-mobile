package dev.master.lumio.ui.genie

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.master.lumio.R
import dev.master.lumio.data.Movie
import dev.master.lumio.domain.*
import dev.master.lumio.ui.components.MoviePoster
import dev.master.lumio.ui.theme.LumioTheme

@Composable
fun GenieScreen(state: GenieState, retry: () -> Unit, openDetails: (String) -> Unit,
                startSwiping: () -> Unit, openHistory: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val activeId = (state as? GenieState.Content)?.recommendations?.let { picks ->
        picks.firstOrNull { it.movie.id == selectedId }?.movie?.id ?: picks.firstOrNull()?.movie?.id
    }
    LaunchedEffect(activeId) { listState.scrollToItem(0) }
    val pane = stringResource(R.string.genie_recommendations)
    LazyColumn(Modifier.fillMaxSize().semantics { paneTitle = pane }, state = listState,
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.lucide_sparkles), null, tint = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.genie_heading), style = MaterialTheme.typography.headlineLarge)
                }
                Text(stringResource(R.string.genie_intro), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = openHistory,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(painterResource(R.drawable.lucide_list_checks), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.history))
                }
            }
        }
        when (state) {
            GenieState.Loading -> item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.loading_recommendations), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            GenieState.GetStarted -> item {
                GenieNotice(R.string.genie_get_started, R.string.genie_get_started_hint, startSwiping)
            }
            GenieState.NoMatches -> item {
                GenieNotice(R.string.genie_no_matches, R.string.genie_no_matches_hint, startSwiping)
            }
            GenieState.NetworkError, GenieState.StorageError -> item {
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(if (state == GenieState.StorageError) R.string.choices_load_error else R.string.connection_error),
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = retry) { Text(stringResource(R.string.retry)) }
                }
            }
            is GenieState.Content -> {
                item { Text(stringResource(R.string.genie_selection), Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium) }
                val index = state.recommendations.indexOfFirst { it.movie.id == selectedId }.coerceAtLeast(0)
                val recommendation = state.recommendations.getOrNull(index)
                if (recommendation != null) {
                    item {
                        RecommendationCard(recommendation) { openDetails(recommendation.movie.id) }
                    }
                    item {
                        Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.genie_position, index + 1, state.recommendations.size),
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            if (index < state.recommendations.lastIndex) {
                                Button(onClick = { selectedId = state.recommendations[index + 1].movie.id }) {
                                    Text(stringResource(R.string.genie_next))
                                }
                            } else {
                                Text(stringResource(R.string.genie_end))
                                OutlinedButton(onClick = { selectedId = state.recommendations.first().movie.id }) {
                                    Text(stringResource(R.string.genie_restart))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun GenieNotice(title: Int, hint: Int, startSwiping: () -> Unit) {
    Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(hint), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = startSwiping) { Text(stringResource(R.string.start_swiping)) }
    }
}

@Composable private fun RecommendationCard(recommendation: Recommendation, open: () -> Unit) {
    val movie = recommendation.movie
    val detailsLabel = stringResource(R.string.history_details_label, movie.title)
    Surface(Modifier.widthIn(max = 680.dp).fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoviePoster(movie, Modifier.width(120.dp).aspectRatio(2f / 3f), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineSmall)
                    if (movie.year > 0) Text(movie.year.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (movie.genres.isNotEmpty()) Text(movie.genres.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(stringResource(R.string.genie_why), style = MaterialTheme.typography.titleMedium)
            // Up to three strongest positive contributions; dislikes are included in the net ranking.
            recommendation.matches.take(3).forEach { match ->
                Text(stringResource(when (match.kind) {
                    MatchKind.GENRE -> R.string.genie_reason_genre
                    MatchKind.DIRECTOR -> R.string.genie_reason_director
                    MatchKind.ACTOR -> R.string.genie_reason_actor
                }, match.name), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = open, modifier = Modifier.semantics { contentDescription = detailsLabel },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.open_details))
            }
        }
    }
}

private val previewRecommendation = Recommendation(Movie("fictional", "A Place Beyond the Moon", 2024,
    genres = listOf("Drama")), 1, listOf(TasteMatch(MatchKind.GENRE, "Drama", 1)))
@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable private fun GenieLightPreview() {
    LumioTheme(false) { Surface { GenieScreen(GenieState.Content(listOf(previewRecommendation)), {}, {}, {}, {}) } }
}
@Preview(showBackground = true, widthDp = 400, heightDp = 850, fontScale = 2f)
@Composable private fun GenieDarkLargePreview() {
    LumioTheme(true) { Surface { GenieScreen(GenieState.GetStarted, {}, {}, {}, {}) } }
}
