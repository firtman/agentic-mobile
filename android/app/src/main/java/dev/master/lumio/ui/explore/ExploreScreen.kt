package dev.master.lumio.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import dev.master.lumio.R
import dev.master.lumio.data.*
import dev.master.lumio.ui.components.*
import dev.master.lumio.ui.theme.*

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onSearch: (String) -> Unit,
    onGenre: (String?) -> Unit,
    onSort: (MovieSort) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryGenres: () -> Unit,
    onReset: () -> Unit,
    onOpenMovie: (Movie) -> Unit,
) {
    val gridState = rememberSaveable(state.filters, saver = LazyGridState.Saver) { LazyGridState() }
    var attribution by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val content = state.result as? ExploreResult.Content
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.5f) 220.dp else 150.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize().testTag("explore_grid").semantics { testTagsAsResourceId = true },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.explore_heading), style = MaterialTheme.typography.displayLarge)
        }
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = state.filters.query, onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth().testTag("explore_search"),
                label = { Text(stringResource(R.string.search_movies)) },
                leadingIcon = { Icon(painterResource(R.drawable.lucide_search), null) },
                trailingIcon = { if (state.filters.query.isNotEmpty()) IconButton(onClick = { onSearch("") }) {
                    Icon(painterResource(R.drawable.lucide_x),stringResource(R.string.clear_search))
                } },
                singleLine = true, shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        item(key = "genres", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { GenreChip(stringResource(R.string.all_genres),state.filters.genre == null) { onGenre(null) } }
                    rowItems(state.genres, key = { it }) { genre -> GenreChip(genre,state.filters.genre == genre) { onGenre(genre) } }
                }
                if (state.genresLoading) LinearProgressIndicator(Modifier.fillMaxWidth(),color = MaterialTheme.colorScheme.secondary)
                if (state.genresError) {
                    Text(stringResource(R.string.genres_error),style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetryGenres,colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                        Text(stringResource(R.string.retry_genres))
                    }
                }
            }
        }
        if (content != null) {
            item(key = "spotlight-label", span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel(stringResource(R.string.spotlight))
            }
            item(key = "spotlight", span = { GridItemSpan(maxLineSpan) }) {
                content.movies.first().let { movie -> SpotlightCard(movie,{ onOpenMovie(movie) },Modifier.fillMaxWidth()) }
            }
        }
        item(key = "catalog-controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.explore_catalog),style = MaterialTheme.typography.headlineMedium)
                Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
                    SortMenu(state.filters.sort,onSort)
                    IconButton(onClick = { keyboard?.hide(); onRefresh() }, modifier = Modifier.testTag("explore_refresh")) {
                        Icon(painterResource(R.drawable.lucide_rotate_ccw),stringResource(R.string.refresh_movies))
                    }
                }
                if (content != null) Text(stringResource(R.string.catalog_count,content.total),style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        when (val result = state.result) {
            ExploreResult.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.loading_movies),modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            ExploreResult.Error -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(painterResource(R.drawable.lucide_wifi_off),null,tint = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.connection_error),style = MaterialTheme.typography.bodyLarge,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = onRefresh,modifier = Modifier.testTag("explore_retry")) { Text(stringResource(R.string.retry)) }
                }
            }
            ExploreResult.Empty -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.no_movies),style = MaterialTheme.typography.headlineMedium,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(stringResource(R.string.no_movies_hint),style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { keyboard?.hide(); onReset() }) { Text(stringResource(R.string.reset_filters)) }
                }
            }
            is ExploreResult.Content -> {
                items(result.movies, key = { "movie:" + it.id },contentType = { "movie" }) { movie -> MovieGridCard(movie,{ onOpenMovie(movie) },Modifier.fillMaxWidth()) }
                item(key = "pagination", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth(),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.loaded_count,result.movies.size,result.total),style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (result.pageError) Text(stringResource(R.string.page_error),color = MaterialTheme.colorScheme.error,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        when {
                            result.loadingMore -> {
                                CircularProgressIndicator(Modifier.size(24.dp),color = MaterialTheme.colorScheme.secondary)
                                Text(stringResource(R.string.loading_more))
                            }
                            result.nextOffset != null -> OutlinedButton(onClick = onLoadMore,modifier = Modifier.testTag("explore_load_more"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
                                Text(stringResource(if(result.pageError) R.string.retry else R.string.load_more))
                            }
                        }
                    }
                }
            }
        }
        item(key = "attribution", span = { GridItemSpan(maxLineSpan) }) {
            TextButton(onClick = { attribution = true },colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.source_attribution))
            }
        }
    }
    if (attribution) AttributionDialog { attribution = false }
}

@Composable private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected,onClick = onClick,label = { Text(label) },modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Coral,selectedLabelColor = Ink),
        border = BorderStroke(1.dp,if(selected) Coral else MaterialTheme.colorScheme.outline))
}
@Composable private fun SectionLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label,style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f),color = MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable private fun SortMenu(sort: MovieSort, onSort: (MovieSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true },modifier = Modifier.testTag("explore_sort"),shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
            Icon(painterResource(R.drawable.lucide_arrow_up_down),stringResource(R.string.sort),Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(sort.label()))
        }
        DropdownMenu(expanded = expanded,onDismissRequest = { expanded = false }) {
            MovieSort.entries.forEach { option -> DropdownMenuItem(
                text = { Text(stringResource(option.label()),color = if(option == sort) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface) },
                onClick = { expanded = false; onSort(option) },
                leadingIcon = { if(option == sort) Icon(painterResource(R.drawable.lucide_check),null) },
                modifier = Modifier.semantics { selected = option == sort },
            ) }
        }
    }
}
private fun MovieSort.label() = when(this) {
    MovieSort.TITLE_ASC -> R.string.sort_title_asc
    MovieSort.TITLE_DESC -> R.string.sort_title_desc
    MovieSort.YEAR_DESC -> R.string.sort_year_desc
    MovieSort.YEAR_ASC -> R.string.sort_year_asc
    MovieSort.RATING_DESC -> R.string.sort_rating_desc
    MovieSort.POPULARITY_DESC -> R.string.sort_popularity_desc
}
@Composable private fun AttributionDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(onDismissRequest = onDismiss,title = { Text(stringResource(R.string.source_attribution)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.catalog_sources_notice))
                listOf(
                    "Wikipedia / prust" to "https://github.com/prust/wikipedia-movie-data",
                    "CC BY-SA 4.0" to "https://creativecommons.org/licenses/by-sa/4.0/",
                    "CMU Movie Summary Corpus" to "https://www.cs.cmu.edu/~ark/personas/",
                    "9,000+ Movies Dataset" to "https://www.kaggle.com/datasets/disham993/9000-movies-dataset",
                ).forEach { (label,url) -> TextButton(onClick = { uriHandler.openUri(url) },colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) { Text(label) } }
            }
        }, confirmButton = {
            TextButton(onClick = onDismiss,colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) { Text(stringResource(R.string.close)) }
        })
}

@Preview(widthDp = 412,heightDp = 860) @Composable private fun ExploreLightPreview() { ExplorePreview(false) }
@Preview(widthDp = 412,heightDp = 860,fontScale = 1.6f) @Composable private fun ExploreDarkPreview() { ExplorePreview(true) }
@Composable private fun ExplorePreview(dark: Boolean) {
    LumioTheme(dark) { Surface {
        ExploreScreen(ExploreUiState(genres = listOf("Drama","Comedy"),genresLoading = false,result = ExploreResult.Content(
            listOf(Movie("preview-a","The Quiet Observatory",2024,genres = listOf("Drama")),Movie("preview-b","A Small Adventure",2023)),2,null)),{},{},{},{},{},{},{},{})
    } }
}
