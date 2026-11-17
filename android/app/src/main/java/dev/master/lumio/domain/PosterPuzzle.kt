package dev.master.lumio.domain

import dev.master.lumio.data.Movie
import java.net.URI
import kotlin.random.Random

/** Tap any two positions to swap; every permutation is solvable (this is not a sliding puzzle). */
data class PuzzleBoard(val tiles: List<Int>, val selected: Int? = null, val moves: Int = 0) {
    init { require(tiles.size == 9 && tiles.toSet() == (0..8).toSet()); require(selected == null || selected in 0..8) }
    val correct: Int get() = tiles.withIndex().count { it.index == it.value }
    val complete: Boolean get() = correct == 9
    fun choose(position: Int): PuzzleBoard {
        if (position !in tiles.indices || complete) return this
        val first = selected ?: return copy(selected = position)
        if (first == position) return copy(selected = null)
        val swapped = tiles.toMutableList().apply { this[first] = tiles[position]; this[position] = tiles[first] }
        return copy(tiles = swapped, selected = null, moves = moves + 1)
    }
    companion object {
        fun shuffled(random: Random): PuzzleBoard {
            val tiles = (0..8).shuffled(random).toMutableList()
            // Never serve an already-completed puzzle, even if the random permutation is identity.
            if (tiles == (0..8).toList()) { tiles[0] = 1; tiles[1] = 0 }
            return PuzzleBoard(tiles)
        }
    }
}

fun suppliedPosterUrl(movie: Movie): String? = runCatching { URI(movie.posterUrl) }.getOrNull()?.let { uri ->
    movie.posterUrl.takeIf { (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank() }
}

fun selectPuzzleMovie(movies: List<Movie>, previousId: String?, random: Random): Movie? {
    val candidates = movies.filter { it.id.isNotBlank() && it.title.isNotBlank() && suppliedPosterUrl(it) != null }
        .distinctBy { it.id }
    val different = candidates.filter { it.id != previousId }
    return different.ifEmpty { candidates }.randomOrNull(random)
}
