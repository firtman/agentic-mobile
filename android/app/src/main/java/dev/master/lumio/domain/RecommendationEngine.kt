package dev.master.lumio.domain

import dev.master.lumio.data.Movie
import java.util.Locale

/** +1 = like, -1 = dislike, 0 = unseen (excluded, never contributes to taste). */
data class TastePreference(val movie: Movie, val direction: Int) {
    init { require(direction in -1..1) }
}
enum class MatchKind { GENRE, DIRECTOR, ACTOR }
data class TasteMatch(val kind: MatchKind, val name: String, val contribution: Long)
data class Recommendation(val movie: Movie, val score: Long, val matches: List<TasteMatch>)

/** Pure deterministic scoring. No source ratings, random facts, or network access. */
class RecommendationEngine {
    fun recommend(movies: List<Movie>, preferences: List<TastePreference>, limit: Int = 30): List<Recommendation> {
        require(limit >= 0)
        if (limit == 0) return emptyList()
        val excluded = preferences.map { it.movie.id }.toSet()
        val affinities = mutableMapOf<Pair<MatchKind, String>, Long>()
        preferences.forEach { preference ->
            if (preference.direction != 0) features(preference.movie).forEach { (key, _) ->
                val weight = when (key.first) {
                    MatchKind.GENRE -> 1L
                    MatchKind.DIRECTOR -> 3L
                    MatchKind.ACTOR -> 2L
                }
                affinities[key] = affinities.getOrDefault(key, 0) + weight * preference.direction
            }
        }
        return movies.asSequence().distinctBy { it.id }.filter { it.id !in excluded }.mapNotNull { movie ->
            val matches = features(movie).mapNotNull { (key, name) ->
                affinities[key]?.takeIf { it != 0L }?.let { TasteMatch(key.first, name, it) }
            }
            val score = matches.sumOf { it.contribution }
            if (score <= 0) null else Recommendation(movie, score,
                matches.filter { it.contribution > 0 }.sortedWith(
                    compareByDescending<TasteMatch> { it.contribution }.thenBy { it.kind }.thenBy { normalized(it.name) }))
        }.sortedWith(compareByDescending<Recommendation> { it.score }
            .thenByDescending { it.movie.popularity.takeIf(Double::isFinite) ?: 0.0 }
            .thenBy { normalized(it.movie.title) }.thenBy { it.movie.id })
            .take(limit).toList()
    }

    /** Ignore blank metadata and count a repeated feature only once per movie. */
    private fun features(movie: Movie): Map<Pair<MatchKind, String>, String> = buildMap {
        fun add(kind: MatchKind, name: String) {
            val display = name.trim()
            if (display.isNotEmpty()) putIfAbsent(kind to normalized(display), display)
        }
        movie.genres.forEach { add(MatchKind.GENRE, it) }
        add(MatchKind.DIRECTOR, movie.director)
        movie.cast.forEach { add(MatchKind.ACTOR, it) }
    }
    private fun normalized(value: String) = value.trim().lowercase(Locale.ROOT)
}
