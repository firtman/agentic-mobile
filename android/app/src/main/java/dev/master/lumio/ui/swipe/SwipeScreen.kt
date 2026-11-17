package dev.master.lumio.ui.swipe

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.master.lumio.R
import dev.master.lumio.data.Movie
import dev.master.lumio.data.ratings.RatingValue
import dev.master.lumio.ui.components.MoviePoster
import dev.master.lumio.ui.theme.LumioTheme

@Composable
fun SwipeScreen(state: SwipeUiState, rate: (RatingValue) -> Unit, undo: () -> Unit,
                retry: () -> Unit, openDetails: (Movie) -> Unit, messageShown: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); messageShown() }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.swipe_heading), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.swipe_intro), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = undo, enabled = state.undoId != null && !state.saving,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(painterResource(R.drawable.lucide_undo_2), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.undo))
                }
            }
            when (val result = state.result) {
                SwipeResult.Loading -> {
                    CircularProgressIndicator(Modifier.padding(24.dp))
                    Text(stringResource(R.string.loading_movies))
                }
                SwipeResult.Error, SwipeResult.StorageError -> {
                    Text(stringResource(if (result == SwipeResult.StorageError) R.string.choices_load_error else R.string.connection_error),
                        modifier = Modifier.widthIn(max = 520.dp), color = MaterialTheme.colorScheme.error)
                    Button(onClick = retry) { Text(stringResource(R.string.retry)) }
                }
                SwipeResult.Empty -> {
                    Text(stringResource(R.string.swipe_empty), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.swipe_empty_hint))
                    OutlinedButton(onClick = retry) { Text(stringResource(R.string.refresh_movies)) }
                }
                is SwipeResult.Card -> {
                    SwipeCard(result.movie, !state.saving, rate, Modifier.widthIn(max = 520.dp).fillMaxWidth())
                    Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(result.movie.title, style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        val metadata = listOfNotNull(result.movie.year.takeIf { it > 0 }?.toString(),
                            result.movie.genres.joinToString(" · ").takeIf { it.isNotEmpty() }).joinToString(" · ")
                        if (metadata.isNotEmpty()) Text(metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { openDetails(result.movie) }, enabled = !state.saving,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                            Text(stringResource(R.string.open_details))
                        }
                    }
                    Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(RatingValue.DISLIKE, RatingValue.UNSEEN, RatingValue.LIKE).forEach { value ->
                            OutlinedButton(onClick = { rate(value) }, enabled = !state.saving,
                                modifier = Modifier.weight(1f).heightIn(min = 80.dp),
                                contentPadding = PaddingValues(8.dp), shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (value == RatingValue.LIKE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (value == RatingValue.LIKE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(painterResource(choiceIcon(value)), null, Modifier.size(24.dp))
                                    Text(stringResource(choiceLabel(value)), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.swipe_hint), modifier = Modifier.widthIn(max = 520.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SwipeCard(movie: Movie, enabled: Boolean, rate: (RatingValue) -> Unit, modifier: Modifier) {
    var drag by remember(movie.id, enabled) { mutableStateOf(Offset.Zero) }
    val latestRate by rememberUpdatedState(rate)
    val reduceMotion = reducedMotion()
    val labels = RatingValue.entries.associateWith { stringResource(choiceLabel(it)) }
    val choice = swipeChoice(drag.x, drag.y, with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() })
    Box(modifier.semantics {
        contentDescription = movie.title
        customActions = if (enabled) RatingValue.entries.map { value ->
            CustomAccessibilityAction(labels.getValue(value)) { latestRate(value); true }
        } else emptyList()
    }.pointerInput(movie.id, enabled) {
        if (enabled) detectDragGestures(
            onDragStart = { drag = Offset.Zero },
            onDragCancel = { drag = Offset.Zero },
            onDragEnd = {
                val selected = swipeChoice(drag.x, drag.y, 72.dp.toPx())
                drag = Offset.Zero
                if (selected != null) latestRate(selected)
            },
            onDrag = { change, amount -> change.consume(); drag += amount },
        )
    }) {
        // No timed transitions or fling. With system animations disabled, even drag transforms stop.
        MoviePoster(movie, Modifier.fillMaxWidth().height(300.dp).graphicsLayer {
            translationX = if (reduceMotion) 0f else drag.x.coerceIn(-size.width / 3, size.width / 3)
            translationY = if (reduceMotion) 0f else drag.y.coerceIn(-size.height / 4, size.height / 4)
            rotationZ = if (reduceMotion) 0f else (drag.x / 50).coerceIn(-8f, 8f)
        })
        if (choice != null) Surface(Modifier.align(Alignment.TopCenter).padding(16.dp),
            color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
            Text(labels.getValue(choice), Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Observe the system setting, including changes made while the screen is visible. */
@Composable private fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = !ValueAnimator.areAnimatorsEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
private fun choiceLabel(value: RatingValue) = when (value) {
    RatingValue.LIKE -> R.string.like
    RatingValue.DISLIKE -> R.string.dislike
    RatingValue.UNSEEN -> R.string.unseen
}
private fun choiceIcon(value: RatingValue) = when (value) {
    RatingValue.LIKE -> R.drawable.lucide_thumbs_up
    RatingValue.DISLIKE -> R.drawable.lucide_thumbs_down
    RatingValue.UNSEEN -> R.drawable.lucide_eye_off
}

@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable private fun SwipePreview() {
    LumioTheme(false) { Surface { SwipeScreen(SwipeUiState(SwipeResult.Card(Movie("preview", "A Place Beyond the Moon", 2024))), {}, {}, {}, {}, {}) } }
}
@Preview(showBackground = true, widthDp = 400, heightDp = 850, fontScale = 2f)
@Composable private fun SwipeDarkLargePreview() {
    LumioTheme(true) { Surface { SwipeScreen(SwipeUiState(SwipeResult.Error), {}, {}, {}, {}, {}) } }
}
