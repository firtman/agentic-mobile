package dev.master.lumio.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ExploreRepositoryTest {
    @Test fun forwardsNormalizedFiltersAndPaging() = runTest {
        val calls = mutableListOf<List<Any?>>()
        val api = object : FakeCatalogApi() {
            override suspend fun genres() = Genres(listOf("Drama"), listOf("en"))
            override suspend fun catalog(): Catalog = error("unexpected catalog")
            override suspend fun movies(query: String?, genre: String?, sort: String, order: String, limit: Int, offset: Int): MoviePage {
                calls.add(listOf(query, genre, sort, order, limit, offset))
                return MoviePage(emptyList(), 0, limit, offset)
            }
        }
        val repository = OnlineExploreRepository(api)
        assertEquals(listOf("Drama"), repository.genres())
        repository.movies(ExploreFilters("  Alien  ", "Horror", MovieSort.YEAR_DESC), 48)
        repository.movies(ExploreFilters(query = "   "), 0)
        assertEquals(listOf("Alien", "Horror", "year", "desc", 24, 48), calls[0])
        assertEquals(listOf(null, null, "year", "desc", 24, 0), calls[1])
    }
}
