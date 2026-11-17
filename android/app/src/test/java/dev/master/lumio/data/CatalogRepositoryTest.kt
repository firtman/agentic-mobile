package dev.master.lumio.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

abstract class FakeCatalogApi : CatalogApi {
    override suspend fun health(): Health = error("unexpected health")
    override suspend fun movies(query: String?, genre: String?, sort: String, order: String, limit: Int, offset: Int): MoviePage = error("unexpected movies")
    override suspend fun movie(id: String): Movie = error("unexpected movie")
    override suspend fun genres(): Genres = error("unexpected genres")
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepositoryTest {
    @Test fun lazyConcurrentRequestsShareOneSuccessfulLoad() = runTest {
        val gate = CompletableDeferred<Catalog>()
        var calls = 0
        val api = object : FakeCatalogApi() {
            override suspend fun catalog(): Catalog { calls++; return gate.await() }
        }
        val repository = CatalogRepository(api)
        assertEquals(0, calls)
        val first = async { repository.catalog() }
        val second = async { repository.catalog() }
        runCurrent()
        assertEquals(1, calls)
        val expected = Catalog(emptyList(), emptyList())
        gate.complete(expected)
        assertSame(expected, first.await())
        assertSame(expected, second.await())
        assertSame(expected, repository.catalog())
        assertEquals(1, calls)
    }

    @Test fun failedLoadCanBeRetried() = runTest {
        var calls = 0
        val expected = Catalog(emptyList(), emptyList())
        val repository = CatalogRepository(object : FakeCatalogApi() {
            override suspend fun catalog(): Catalog {
                calls++
                if (calls == 1) throw IllegalStateException("network")
                return expected
            }
        })
        try { repository.catalog(); fail("Expected failure") } catch (_: IllegalStateException) { }
        assertSame(expected, repository.catalog())
        assertEquals(2, calls)
    }
}
