package dev.master.lumio.domain

import dev.master.lumio.data.Movie
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PosterPuzzleTest {
    private fun movie(id: String, url: String = "", title: String = id) = Movie(id, title, posterUrl = url)

    @Test fun shuffledBoardsAreUnsolvedPermutations() {
        repeat(500) { seed ->
            val board = PuzzleBoard.shuffled(Random(seed))
            assertEquals((0..8).toSet(), board.tiles.toSet())
            assertFalse(board.complete)
            assertNull(board.selected)
            assertEquals(0, board.moves)
        }
    }

    @Test fun selectingCancellingAndSwappingOnlyTwoPositions() {
        val board = PuzzleBoard(listOf(1, 0, 2, 3, 4, 5, 6, 7, 8))
        assertEquals(board, board.choose(-1))
        assertEquals(board, board.choose(9))
        val selected = board.choose(0)
        assertEquals(0, selected.selected)
        assertEquals(board, selected.choose(0))
        val solved = selected.choose(1)
        assertEquals((0..8).toList(), solved.tiles)
        assertEquals(1, solved.moves)
        assertNull(solved.selected)
        assertEquals(9, solved.correct)
        assertTrue(solved.complete)
        assertEquals(solved, solved.choose(0))
    }

    @Test fun arbitraryPermutationsCanBeSolvedBySwapping() {
        repeat(100) { seed ->
            var board = PuzzleBoard.shuffled(Random(seed))
            while (!board.complete) {
                val position = board.tiles.indices.first { board.tiles[it] != it }
                val target = board.tiles.indexOf(position)
                val before = board
                board = board.choose(position).choose(target)
                assertEquals(before.moves + 1, board.moves)
                assertEquals(before.tiles[position], board.tiles[target])
                assertEquals(before.tiles[target], board.tiles[position])
                assertEquals(before.tiles.indices.filter { it != position && it != target }.map { before.tiles[it] },
                    board.tiles.indices.filter { it != position && it != target }.map { board.tiles[it] })
            }
            assertEquals(9, board.correct)
        }
    }

    @Test fun everyDifficultySupportsDragAndTapAndCanBeSolved() {
        for (size in listOf(3, 4, 6)) repeat(30) { seed ->
            var board = PuzzleBoard.shuffled(Random(seed), size)
            assertEquals(size, board.dimension)
            assertEquals(size * size, board.tiles.size)
            assertFalse(board.complete)
            assertEquals(board, board.swap(-1, 0))
            assertEquals(board, board.swap(0, size * size))
            assertEquals(board, board.swap(0, 0))
            assertEquals(board.choose(0).choose(1), board.swap(0, 1))
            while (!board.complete) {
                val position = board.tiles.indices.first { board.tiles[it] != it }
                val previous = board
                board = board.swap(position, board.tiles.indexOf(position))
                assertEquals(previous.moves + 1, board.moves)
            }
            assertEquals(size * size, board.correct)
            assertEquals(board, board.swap(0, 1))
        }
    }

    @Test fun posterUrlRequiresValidHttpHostAndIsNeverRewritten() {
        val valid = listOf("https://example.org/path?a=1", "http://example.org:8080/poster.png", "HTTPS://example.org/p")
        valid.forEach { assertEquals(it, suppliedPosterUrl(movie("m", it))) }
        listOf("", "file:///tmp/a", "ftp://example.org/a", "https:///path", "https://", "https://bad host/a", "//example.org/a")
            .forEach { assertNull(suppliedPosterUrl(movie("m", it))) }
    }

    @Test fun selectionPrefersSuppliedPostersAndAvoidsPreviousWhenPossible() {
        val first = movie("one", "https://example.org/one")
        val second = movie("two", "http://example.org/two")
        val movies = listOf(movie(" ", "https://example.org/blank"), movie("no-title", "https://example.org/x", " "),
            first, first.copy(title = "Duplicate"), second, movie("fallback"))
        repeat(20) { seed -> assertEquals(second, selectPuzzleMovie(movies, "one", Random(seed))) }
        assertEquals(first, selectPuzzleMovie(listOf(first), "one", Random(1)))
    }

    @Test fun selectionRejectsMissingPostersAndHandlesEmpty() {
        val first = movie("one")
        val second = movie("two", "not a url")
        assertNull(selectPuzzleMovie(listOf(first, second), "one", Random(3)))
        assertNull(selectPuzzleMovie(listOf(first), "one", Random(3)))
        assertNull(selectPuzzleMovie(listOf(movie(" ", title = "Film"), movie("id", title = " ")), null, Random(3)))
        assertNull(selectPuzzleMovie(emptyList(), null, Random(3)))
    }
}
