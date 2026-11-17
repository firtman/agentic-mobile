package dev.master.lumio.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/** Decode the large catalog directly from the response stream, closing it on failure too. */
internal class CatalogStreamConverter(private val json: Json) : Converter.Factory() {
    @OptIn(ExperimentalSerializationApi::class)
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        if (type != Catalog::class.java) return null
        return Converter<ResponseBody, Catalog> { body ->
            body.use { json.decodeFromStream<Catalog>(it.byteStream()) }
        }
    }
}
