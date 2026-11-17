package dev.master.lumio.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class CatalogApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: CatalogApi

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        api = ApiClient.create(server.url("/").toString())
    }

    @After fun tearDown() = server.shutdown()

    @Test fun pagedQueryAndMissingMetadata() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"movies":[{"id":"1","title":"Film","futureField":true}],"total":3,"limit":1,"offset":1,"unknown":42}"""))
        val page = api.movies(query = "a & b", genre = "Science Fiction", sort = "year", order = "desc", limit = 1, offset = 1)
        val request = server.takeRequest()
        assertEquals("/movies", request.requestUrl!!.encodedPath)
        assertEquals("a & b", request.requestUrl!!.queryParameter("q"))
        assertEquals("Science Fiction", request.requestUrl!!.queryParameter("genre"))
        assertEquals("year", request.requestUrl!!.queryParameter("sort"))
        assertEquals("desc", request.requestUrl!!.queryParameter("order"))
        assertEquals("1", request.requestUrl!!.queryParameter("limit"))
        assertEquals("1", request.requestUrl!!.queryParameter("offset"))
        assertTrue(page.hasMore)
        assertEquals(0, page.movies.single().runtime)
        assertEquals(0.0, page.movies.single().rating, 0.0)
        assertEquals("", page.movies.single().director)
        assertTrue(page.movies.single().cast.isEmpty())
        assertFalse(page.copy(offset = 2).hasMore)
    }

    @Test fun detailIdIsEncodedAsOnePathSegment() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"a/b ?","title":"Film"}"""))
        assertEquals("a/b ?", api.movie("a/b ?").id)
        assertEquals("/movies/a%2Fb%20%3F", server.takeRequest().requestUrl!!.encodedPath)
    }

    @Test fun catalogDecodesMoviesAndTriviaThroughApi() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"movies":[{"id":"m1","title":"Film","newField":1}],"trivia":[{"id":"t1","movieId":"m1","kind":"director","question":"Who?","options":["A","B"],"answerIndex":1}],"newField":true}"""))
        val catalog = api.catalog()
        assertEquals("/catalog", server.takeRequest().path)
        assertEquals("Film", catalog.movies.single().title)
        assertEquals(1, catalog.trivia.single().answerIndex)
        assertEquals("m1", catalog.trivia.single().movieId)
    }

    @Test fun httpErrorsArePropagated() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            api.health()
            fail("Expected HTTP failure")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }
    }
}
