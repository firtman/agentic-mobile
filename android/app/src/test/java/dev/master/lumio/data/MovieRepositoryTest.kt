package dev.master.lumio.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class MovieRepositoryTest {
    private val server = MockWebServer().also { it.start() }
    private val repository = OnlineMovieRepository(ApiClient.create(server.url("/").toString()))
    @After fun tearDown() = server.shutdown()

    @Test fun loadsMovieById() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"x","title":"Film","runtime":0,"director":"","cast":[]}"""))
        val movie = repository.movie("x")
        assertEquals("Film", movie.title)
        assertEquals(0, movie.runtime)
        assertEquals("/movies/x", server.takeRequest().requestUrl!!.encodedPath)
    }

    @Test fun notFoundIsDistinctFromOtherFailures() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"Movie not found"}"""))
        assertThrows(MovieNotFoundException::class.java) { runBlocking { repository.movie("gone") } }
        server.enqueue(MockResponse().setResponseCode(500))
        val error = assertThrows(HttpException::class.java) { runBlocking { repository.movie("x") } }
        assertEquals(500, error.code())
    }
}
