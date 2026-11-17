package dev.master.lumio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import dev.master.lumio.R
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.master.lumio.data.Movie
import dev.master.lumio.ui.theme.LumioTheme
import java.net.URI

/** Decorative artwork; the surrounding card supplies the movie title to accessibility services. */
@Composable
fun MoviePoster(movie: Movie, modifier: Modifier = Modifier) {
    val url = remember(movie.posterUrl) {
        runCatching { URI(movie.posterUrl) }.getOrNull()?.let { uri ->
            movie.posterUrl.takeIf {
                (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
                    !uri.host.isNullOrBlank()
            }
        }
    }
    var loaded by remember(movie.id, url) { mutableStateOf(false) }
    val fallbackColor = if ((movie.id.hashCode() and 1) == 0) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fallbackColor)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (url != null) {
            // AsyncImage resolves the requested image size from the layout constraints.
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onLoading = { loaded = false },
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun MovieGridCard(movie: Movie, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MoviePoster(movie, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Text(movie.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        if (movie.year > 0) {
            Text(movie.year.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MovieSourceLinks(movie)
    }
}

@Composable private fun MovieSourceLinks(movie: Movie) {
    val uriHandler = LocalUriHandler.current
    listOf(movie.sourceUrl to R.string.view_source, movie.datasetSourceUrl to R.string.dataset_source)
        .filter { (url,_) -> runCatching { URI(url) }.getOrNull()?.let {
            (it.scheme == "https" || it.scheme == "http") && !it.host.isNullOrBlank()
        } == true }
        .distinctBy { it.first }.forEach { (url,label) ->
            val description = stringResource(R.string.movie_source_label,stringResource(label),movie.title)
            TextButton(onClick = { uriHandler.openUri(url) },
                modifier = Modifier.semantics { contentDescription = description },
                contentPadding = PaddingValues(horizontal = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(painterResource(R.drawable.lucide_external_link),null,Modifier.size(16.dp))
                Text(stringResource(label),modifier = Modifier.padding(start = 6.dp),style = MaterialTheme.typography.bodyMedium)
            }
        }
}

@Composable
fun SpotlightCard(movie: Movie, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MoviePoster(movie, Modifier.width(100.dp).aspectRatio(2f / 3f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(movie.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                if (movie.year > 0) {
                    Text(movie.year.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (movie.genres.isNotEmpty()) {
                    Text(movie.genres.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val previewMovie = Movie(id = "fictional-1", title = "A Place Beyond the Moon", year = 2024, genres = listOf("Drama", "Adventure"))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MovieCardsLightPreview() {
    LumioTheme(dark = false) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SpotlightCard(previewMovie, Modifier.fillMaxWidth())
            MovieGridCard(previewMovie, Modifier.width(150.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, fontScale = 1.6f)
@Composable
private fun MovieCardsDarkLargeTextPreview() {
    LumioTheme(dark = true) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SpotlightCard(previewMovie, Modifier.fillMaxWidth())
            MovieGridCard(previewMovie, Modifier.width(150.dp))
        }
    }
}
