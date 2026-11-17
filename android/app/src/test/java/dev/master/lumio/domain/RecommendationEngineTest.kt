package dev.master.lumio.domain

import dev.master.lumio.data.Movie
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class RecommendationEngineTest {
    private val engine = RecommendationEngine()
    private fun film(id: String, genres: List<String> = emptyList(), director: String = "", cast: List<String> = emptyList(), popularity: Double = 0.0, title: String = id) =
        Movie(id, title, genres = genres, director = director, cast = cast, popularity = popularity)

    @Test fun weightsAndPositiveReasonsReflectActualCandidateFeatures() {
        val liked = film("liked", listOf("Drama"), "Director", listOf("Actor"))
        val candidate = film("candidate", listOf("Drama"), "Director", listOf("Actor"))
        val result = engine.recommend(listOf(candidate), listOf(TastePreference(liked, 1))).single()
        assertEquals(6L, result.score)
        assertEquals(setOf(TasteMatch(MatchKind.GENRE, "Drama", 1), TasteMatch(MatchKind.DIRECTOR, "Director", 3), TasteMatch(MatchKind.ACTOR, "Actor", 2)), result.matches.toSet())
    }

    @Test fun affinitiesSumAcrossLikesAndDislikesAndExcludeNonpositiveScores() {
        val preferences = listOf(
            TastePreference(film("a", listOf("Drama"), "D", listOf("A")), 1),
            TastePreference(film("b", listOf("Drama"), cast = listOf("A")), 1),
            TastePreference(film("c", listOf("Drama"), "D"), -1),
            TastePreference(film("d", cast = listOf("A")), -1),
            TastePreference(film("e", listOf("Horror")), -1),
        )
        val result = engine.recommend(listOf(film("positive", listOf("Drama", "Horror"), cast = listOf("A")), film("zero", director = "D"), film("negative", listOf("Horror"))), preferences)
        assertEquals(listOf("positive"), result.map { it.movie.id })
        assertEquals(2L, result.single().score)
        assertEquals(setOf(TasteMatch(MatchKind.GENRE, "Drama", 1), TasteMatch(MatchKind.ACTOR, "A", 2)), result.single().matches.toSet())
    }

    @Test fun unseenDoesNotContributeAndAllSavedFilmsAreExcluded() {
        val liked = film("liked", listOf("Drama"))
        val disliked = film("disliked", listOf("Horror"))
        val unseen = film("unseen", listOf("Comedy"))
        val preferences = listOf(TastePreference(liked, 1), TastePreference(disliked, -1), TastePreference(unseen, 0))
        val results = engine.recommend(listOf(liked, disliked, unseen, film("comedy", listOf("Comedy")), film("drama", listOf("Drama"))), preferences)
        assertEquals(listOf("drama"), results.map { it.movie.id })
    }

    @Test fun normalizationDeduplicatesFeaturesAndIgnoresBlanksUsingRootLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val liked = film("liked", listOf("  INDIE ", "indie", " "), " ", listOf(" ACTOR ", "actor", ""))
            val candidate = film("candidate", listOf(" Indie ", "INDIE", ""), cast = listOf(" Actor ", "actor", " "))
            val result = engine.recommend(listOf(candidate, candidate.copy(title = "duplicate")), listOf(TastePreference(liked, 1))).single()
            assertEquals(3L, result.score)
            assertEquals(setOf(TasteMatch(MatchKind.GENRE, "Indie", 1), TasteMatch(MatchKind.ACTOR, "Actor", 2)), result.matches.toSet())
            assertEquals("candidate", result.movie.title)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun identicalNamesInDifferentFeatureTypesDoNotCrossMatch() {
        val preferences = listOf(TastePreference(film("liked", listOf("Shared")), 1))
        val results = engine.recommend(listOf(film("director", director = "Shared"), film("actor", cast = listOf("Shared")), film("genre", listOf("Shared"))), preferences)
        assertEquals(listOf("genre"), results.map { it.movie.id })
    }

    @Test fun orderingAndLimitsAreDeterministic() {
        val preferences = listOf(TastePreference(film("liked", listOf("Drama"), director = "D"), 1))
        val candidates = listOf(film("b", listOf("Drama"), popularity = 5.0, title = "Same"), film("a", listOf("Drama"), popularity = 5.0, title = "same"), film("z", listOf("Drama"), popularity = 5.0, title = "Alpha"), film("high", listOf("Drama"), popularity = 9.0), film("score", director = "D"))
        assertEquals(listOf("score", "high", "z", "a", "b"), engine.recommend(candidates, preferences).map { it.movie.id })
        assertEquals(listOf("score", "high"), engine.recommend(candidates, preferences, 2).map { it.movie.id })
        assertTrue(engine.recommend(candidates, preferences, 0).isEmpty())
        val many = (1..40).map { film("id-$it", listOf("Drama")) }
        assertEquals(30, engine.recommend(many, preferences).size)
    }
}
