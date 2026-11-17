package dev.master.lumio.data

import retrofit2.HttpException

/** Thrown when the catalog has no movie for the requested id. */
class MovieNotFoundException(id: String) : Exception("Movie not found: $id")

interface MovieRepository {
    suspend fun movie(id: String): Movie
}
class OnlineMovieRepository(private val api: CatalogApi) : MovieRepository {
    override suspend fun movie(id: String): Movie = try {
        api.movie(id)
    } catch (e: HttpException) {
        if (e.code() == 404) throw MovieNotFoundException(id) else throw e
    }
}
