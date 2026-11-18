package dev.master.lumio.domain

import dev.master.lumio.data.Movie
import java.net.URI
import kotlin.random.Random

/** Tap any two positions to swap; every permutation is solvable (this is not a sliding puzzle). */
data class PuzzleBoard(val tiles: List<Int>, val selected: Int? = null, val moves: Int = 0) {
    val dimension: Int get() = kotlin.math.sqrt(tiles.size.toDouble()).toInt()
    init { require(tiles.size in listOf(9, 16, 36) && tiles.toSet() == tiles.indices.toSet()); require(selected == null || selected in tiles.indices) }
    val correct: Int get() = tiles.withIndex().count { it.index == it.value }
    val complete: Boolean get() = correct == tiles.size
    fun choose(position: Int): PuzzleBoard {
        if (position !in tiles.indices || complete) return this
        val first = selected ?: return copy(selected = position)
        if (first == position) return copy(selected = null)
        return swap(first, position)
    }
    fun swap(first: Int, position: Int): PuzzleBoard {
        if (complete || first !in tiles.indices || position !in tiles.indices || first == position) return this
        val swapped = tiles.toMutableList().apply { this[first] = tiles[position]; this[position] = tiles[first] }
        return copy(tiles = swapped, selected = null, moves = moves + 1)
    }
    companion object {
        fun shuffled(random: Random, dimension: Int = 3): PuzzleBoard {
            require(dimension in listOf(3, 4, 6))
            val positions = (0 until dimension * dimension).toList()
            val tiles = positions.shuffled(random).toMutableList()
            // Never serve an already-completed puzzle, even if the random permutation is identity.
            if (tiles == positions) { tiles[0] = 1; tiles[1] = 0 }
            return PuzzleBoard(tiles)
        }
    }
}

fun suppliedPosterUrl(movie: Movie): String? = runCatching { URI(movie.posterUrl) }.getOrNull()?.let { uri ->
    movie.posterUrl.takeIf { (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank() }
}

/** Films that can be played: an id, a title and a supplied web poster URL. Validates every URL, so build it once. */
fun puzzleCandidates(movies: List<Movie>): List<Movie> =
    movies.filter { it.id.isNotBlank() && it.title.isNotBlank() && suppliedPosterUrl(it) != null }
        .distinctBy { it.id }

/** Picks from already-validated [candidates], avoiding an immediate repeat when another film exists. */
fun pickPuzzleMovie(candidates: List<Movie>, previousId: String?, random: Random): Movie? {
    val different = candidates.filter { it.id != previousId }
    return different.ifEmpty { candidates }.randomOrNull(random)
}

fun selectPuzzleMovie(movies: List<Movie>, previousId: String?, random: Random): Movie? =
    pickPuzzleMovie(puzzleCandidates(movies), previousId, random)
