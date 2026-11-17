package dev.master.lumio.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

class CatalogStreamConverterTest {
    private class TrackingBody(content: String) : ResponseBody() {
        var closed = false
        private val trackingSource = object : ForwardingSource(Buffer().writeUtf8(content)) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()
        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = -1
        override fun source(): BufferedSource = trackingSource
    }

    private val converter = CatalogStreamConverter(Json { ignoreUnknownKeys = true })
        .responseBodyConverter(Catalog::class.java, emptyArray(), Retrofit.Builder().baseUrl("http://localhost/").build())!!

    @Test fun closesBodyAfterSuccess() {
        val body = TrackingBody("""{"movies":[],"trivia":[]}""")
        assertEquals(Catalog(emptyList(), emptyList()), converter.convert(body))
        assertTrue(body.closed)
    }

    @Test fun closesBodyAfterMalformedJson() {
        val body = TrackingBody("""{"movies":[""")
        try {
            converter.convert(body)
            fail("Expected malformed JSON failure")
        } catch (_: IllegalArgumentException) {
            assertTrue(body.closed)
        }
    }
}
