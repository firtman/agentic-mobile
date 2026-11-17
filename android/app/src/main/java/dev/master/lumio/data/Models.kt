package dev.master.lumio.data

import kotlinx.serialization.Serializable

@Serializable data class Health(val status: String, val movies: Int)
@Serializable data class Movie(
    val id: String, val title: String, val year: Int = 0,
    val synopsis: String = "", val genres: List<String> = emptyList(),
    val director: String = "", val cast: List<String> = emptyList(),
    val runtime: Int = 0, val rating: Double = 0.0,
    val posterUrl: String = "", val backdropUrl: String = "",
    val sourceUrl: String = "", val releaseDate: String = "",
    val voteCount: Int = 0, val popularity: Double = 0.0,
    val originalLanguage: String = "", val datasetSourceUrl: String = "",
)
@Serializable data class MoviePage(val movies: List<Movie>, val total: Int, val limit: Int, val offset: Int) {
    val hasMore: Boolean get() = offset.toLong() + movies.size < total
}
@Serializable data class Genres(val genres: List<String>, val languages: List<String>)
@Serializable data class TriviaQuestion(
    val id: String, val movieId: String, val kind: String,
    val question: String, val options: List<String>, val answerIndex: Int,
)
@Serializable data class Catalog(val movies: List<Movie>, val trivia: List<TriviaQuestion>)
