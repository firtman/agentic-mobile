package dev.master.lumio.data

/** Stable API sort values; display labels live in localized UI resources. */
enum class MovieSort(val field: String, val order: String) {
    TITLE_ASC("title", "asc"), TITLE_DESC("title", "desc"),
    YEAR_DESC("year", "desc"), YEAR_ASC("year", "asc"),
    RATING_DESC("rating", "desc"), POPULARITY_DESC("popularity", "desc"),
}
data class ExploreFilters(
    val query: String = "", val genre: String? = null,
    val sort: MovieSort = MovieSort.TITLE_ASC,
)
interface ExploreRepository {
    suspend fun genres(): List<String>
    suspend fun movies(filters: ExploreFilters, offset: Int): MoviePage
}
class OnlineExploreRepository(private val api: CatalogApi) : ExploreRepository {
    override suspend fun genres(): List<String> = api.genres().genres
    override suspend fun movies(filters: ExploreFilters, offset: Int): MoviePage = api.movies(
        query = filters.query.trim().takeIf { it.isNotEmpty() },
        genre = filters.genre, sort = filters.sort.field, order = filters.sort.order,
        limit = 24, offset = offset,
    )
}
