package dev.master.lumio.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import dev.master.lumio.R
import dev.master.lumio.domain.*
import dev.master.lumio.ui.theme.*
import kotlin.math.max

@Composable
fun PuzzleScreen(state: PuzzleState, start: () -> Unit, retry: () -> Unit, choose: (Int) -> Unit,
                 reference: () -> Unit, next: () -> Unit, posterFailed: (Long) -> Unit,
                 posterLoaded: (Long) -> Unit, retryPoster: () -> Unit,
                 details: (String) -> Unit, back: () -> Unit) {
    LaunchedEffect(Unit) { start() }
    val pane = stringResource(R.string.poster_puzzle)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).semantics { paneTitle = pane },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            PuzzleState.Ready, PuzzleState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.loading_puzzle))
            }
            PuzzleState.Error -> {
                Text(stringResource(R.string.connection_error), color = MaterialTheme.colorScheme.error)
                Button(onClick = retry) { Text(stringResource(R.string.retry)) }
            }
            PuzzleState.Unavailable -> {
                Text(stringResource(R.string.no_puzzle), style = MaterialTheme.typography.headlineMedium)
                Button(onClick = retry) { Text(stringResource(R.string.retry)) }
                OutlinedButton(onClick = back) { Text(stringResource(R.string.back_to_arcade)) }
            }
            is PuzzleState.Playing -> key(state.session.id) {
                val session = state.session
                val context = LocalContext.current
                val request = remember(session.movie.posterUrl, context) {
                    ImageRequest.Builder(context).data(session.movie.posterUrl)
                        .size(900, 1350).build()
                }
                // One decoded painter is shared by all nine tiles; global disk cache is disabled.
                val painter = rememberAsyncImagePainter(request)
                val imageState by painter.state.collectAsState()
                LaunchedEffect(imageState, session.id) {
                    when (imageState) {
                        is AsyncImagePainter.State.Error -> posterFailed(session.id)
                        is AsyncImagePainter.State.Success -> posterLoaded(session.id)
                        else -> Unit
                    }
                }
                val ready = session.poster == PuzzlePoster.READY && imageState is AsyncImagePainter.State.Success
                Text(session.movie.title, style = MaterialTheme.typography.headlineMedium)
                if (session.movie.year > 0) Text(session.movie.year.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.puzzle_instructions), Modifier.widthIn(max = 520.dp).fillMaxWidth())
                Text(stringResource(R.string.puzzle_progress, session.board.correct), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(R.string.puzzle_moves, session.board.moves))
                if (session.poster == PuzzlePoster.FAILED || imageState is AsyncImagePainter.State.Error) {
                    Text(stringResource(R.string.puzzle_poster_error), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = retryPoster) { Text(stringResource(R.string.retry)) }
                } else if (!ready) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.loading_poster))
                } else {
                    if (session.reference) {
                        val label = stringResource(R.string.puzzle_reference)
                        Canvas(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(2f / 3f).semantics { contentDescription = label }) {
                            drawPuzzleArtwork(painter, size)
                        }
                    } else PuzzleGrid(session, painter, choose)
                    if (session.board.complete) {
                        Text(stringResource(R.string.puzzle_complete), style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } else {
                        Text(stringResource(if (session.reference) R.string.puzzle_reference_hint else if (session.board.selected != null) R.string.puzzle_selected_hint else R.string.puzzle_select_hint),
                            Modifier.widthIn(max = 520.dp), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = reference,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                            Text(stringResource(if (session.reference) R.string.hide_reference else R.string.show_reference))
                        }
                    }
                }
                Button(onClick = next) { Text(stringResource(R.string.next_puzzle)) }
                TextButton(onClick = { details(session.movie.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                    Text(stringResource(R.string.open_details))
                }
                if (session.board.complete) OutlinedButton(onClick = back,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(stringResource(R.string.back_to_arcade))
                }
            }
        }
    }
}

@Composable private fun PuzzleGrid(session: PuzzleSession, painter: Painter, choose: (Int) -> Unit) {
    Column(Modifier.widthIn(max = 320.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { col ->
                    val position = row * 3 + col
                    val tile = session.board.tiles[position]
                    val selected = session.board.selected == position
                    val label = stringResource(R.string.puzzle_tile, tile + 1, position + 1)
                    val selectedText = stringResource(if (selected) R.string.selection_active else R.string.puzzle_not_selected)
                    Surface(modifier = Modifier.weight(1f).aspectRatio(2f / 3f)
                        .selectable(selected, enabled = !session.board.complete, role = Role.Button, onClick = { choose(position) })
                        .semantics { contentDescription = label; stateDescription = selectedText },
                        shape = RoundedCornerShape(4.dp), border = if (selected) BorderStroke(3.dp, Coral) else null) {
                        Box {
                            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                                val full = Size(size.width * 3, size.height * 3)
                                clipRect {
                                    translate(-(tile % 3) * size.width, -(tile / 3) * size.height) {
                                        drawPuzzleArtwork(painter, full)
                                    }
                                }
                            }
                            if (selected) Surface(Modifier.align(Alignment.TopEnd).padding(4.dp), color = Coral, shape = RoundedCornerShape(4.dp)) {
                                Icon(painterResource(R.drawable.lucide_check), null, Modifier.size(24.dp), tint = Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every tile samples the same full-image crop, with no bitmap copies or independently loaded URLs. */
private fun DrawScope.drawPuzzleArtwork(painter: Painter, full: Size) {
    val intrinsic = painter.intrinsicSize
    if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.width > 0 && intrinsic.height > 0) {
        val scale = max(full.width / intrinsic.width, full.height / intrinsic.height)
        val imageSize = Size(intrinsic.width * scale, intrinsic.height * scale)
        translate((full.width - imageSize.width) / 2, (full.height - imageSize.height) / 2) {
            with(painter) { draw(imageSize) }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable fun PuzzleLightPreview() {
    LumioTheme(false) { Surface { PuzzleScreen(PuzzleState.Unavailable, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
}
@Preview(showBackground = true, widthDp = 400, heightDp = 1000, fontScale = 2f)
@Composable fun PuzzleDarkLargePreview() {
    LumioTheme(true) { Surface { PuzzleScreen(PuzzleState.Error, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
}
