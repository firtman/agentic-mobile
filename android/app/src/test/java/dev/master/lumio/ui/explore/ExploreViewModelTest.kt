package dev.master.lumio.ui.explore

import androidx.lifecycle.SavedStateHandle
import dev.master.lumio.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun page(offset: Int, total: Int, vararg ids: String) =
        MoviePage(ids.map { Movie(id = it, title = it) }, total, 24, offset)
    private class Repository : ExploreRepository {
        val calls = mutableListOf<Pair<ExploreFilters, Int>>()
        var load: suspend (ExploreFilters, Int) -> MoviePage = { _, offset -> MoviePage(emptyList(), 0, 24, offset) }
        var loadGenres: suspend () -> List<String> = { listOf("Drama") }
        override suspend fun genres() = loadGenres()
        override suspend fun movies(filters: ExploreFilters, offset: Int): MoviePage {
            calls += filters to offset
            return load(filters, offset)
        }
    }
    private fun ExploreViewModel.content() = state.value.result as ExploreResult.Content

    @Test fun initialGenreAndPageFailuresAreIndependentAndRetryable() = runTest(dispatcher) {
        val repository = Repository().apply {
            loadGenres = { throw IllegalStateException() }
            load = { _, offset -> page(offset, 1, "a") }
        }
        val vm = ExploreViewModel(repository, SavedStateHandle())
        runCurrent()
        assertTrue(vm.state.value.genresError)
        assertEquals(listOf("a"), vm.content().movies.map { it.id })
        repository.loadGenres = { listOf("Drama", "", "Drama") }
        repository.load = { _, _ -> throw IllegalStateException() }
        vm.refresh(); runCurrent()
        assertEquals(listOf("Drama"), vm.state.value.genres)
        assertEquals(ExploreResult.Error, vm.state.value.result)
        repository.load = { _, offset -> page(offset, 1, "b") }
        vm.refresh(); runCurrent()
        assertEquals(listOf("b"), vm.content().movies.map { it.id })
    }

    @Test fun debounceUsesLatestQueryAndStaleResultCannotOverwriteFilters() = runTest(dispatcher) {
        val pending = CompletableDeferred<MoviePage>()
        val repository = Repository().apply {
            load = { filters, offset -> if (filters.query == "old") withContext(NonCancellable) { pending.await() } else page(offset, 1, filters.query.ifEmpty { "initial" }) }
        }
        val vm = ExploreViewModel(repository, SavedStateHandle())
        runCurrent()
        vm.search("old"); advanceTimeBy(300); runCurrent()
        vm.search("new"); advanceTimeBy(100); vm.search("newest")
        advanceTimeBy(300); runCurrent()
        assertEquals("newest", vm.content().movies.single().id)
        pending.complete(page(0, 1, "stale")); runCurrent()
        assertEquals("newest", vm.content().movies.single().id)
        assertEquals(listOf("", "old", "newest"), repository.calls.map { it.first.query })
    }

    @Test fun filterChangesResetOffsetAndSavedStateRestoresFilters() = runTest(dispatcher) {
        val repository = Repository().apply { load = { _, offset -> page(offset, 50, "a") } }
        val saved = SavedStateHandle()
        val vm = ExploreViewModel(repository, saved)
        runCurrent(); vm.loadMore(); runCurrent()
        vm.selectGenre("Drama"); runCurrent()
        vm.sort(MovieSort.YEAR_DESC); runCurrent()
        vm.search("film"); advanceTimeBy(300); runCurrent()
        assertEquals(listOf(0, 1, 0, 0, 0), repository.calls.map { it.second })
        assertEquals("Drama", repository.calls.last().first.genre)
        assertEquals(MovieSort.YEAR_DESC, repository.calls.last().first.sort)
        val restored = ExploreViewModel(repository, saved)
        runCurrent()
        assertEquals(ExploreFilters(query = "film", genre = "Drama", sort = MovieSort.YEAR_DESC), restored.state.value.filters)
        assertEquals(restored.state.value.filters, repository.calls.last().first)
    }

    @Test fun pagingGuardsDuplicatesRetriesAndStopsOnEmptyPage() = runTest(dispatcher) {
        val gate = CompletableDeferred<MoviePage>()
        var attempt = 0
        val repository = Repository().apply {
            load = { _, offset -> when (offset) {
                0 -> page(0, 100, "a", "b")
                2 -> if (++attempt == 1) gate.await() else page(2, 100, "b", "c")
                else -> page(offset, 100)
            } }
        }
        val vm = ExploreViewModel(repository, SavedStateHandle())
        runCurrent(); vm.loadMore(); vm.loadMore(); runCurrent()
        assertEquals(listOf(0, 2), repository.calls.map { it.second })
        assertTrue(vm.content().loadingMore)
        gate.completeExceptionally(IllegalStateException()); runCurrent()
        assertTrue(vm.content().pageError)
        assertEquals(listOf("a", "b"), vm.content().movies.map { it.id })
        vm.loadMore(); runCurrent()
        assertEquals(listOf("a", "b", "c"), vm.content().movies.map { it.id })
        assertEquals(4, vm.content().nextOffset ?: -1)
        vm.loadMore(); runCurrent()
        assertNull(vm.content().nextOffset)
        vm.loadMore(); runCurrent()
        assertEquals(listOf(0, 2, 2, 4), repository.calls.map { it.second })
    }
}
