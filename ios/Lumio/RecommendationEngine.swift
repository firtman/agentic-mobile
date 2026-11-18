import Foundation

struct TastePreference: Sendable {
    let movieID: String
    let choice: RatingChoice
    let genres: [String]
    let director: String?
    let cast: [String]
}

struct TasteMatch: Sendable {
    enum Kind: Int, Sendable {
        case genre, director, actor

        var weight: Int {
            switch self {
            case .genre: 1
            case .director: 3
            case .actor: 2
            }
        }
    }

    let kind: Kind
    let name: String
    let contribution: Int
}

struct Recommendation: Sendable {
    let movie: Movie
    let score: Int
    let matches: [TasteMatch]
}

enum RecommendationEngine {
    private struct Feature: Hashable {
        let kind: TasteMatch.Kind
        let normalizedName: String
    }

    static func rank(_ movies: [Movie], using preferences: [TastePreference]) throws -> [Recommendation] {
        let excluded = Set(preferences.map(\.movieID))
        var affinities: [Feature: Int] = [:]
        for preference in preferences where preference.choice != .unseen {
            try Task.checkCancellation()
            let direction = preference.choice == .like ? 1 : -1
            for feature in features(preference.genres, preference.director, preference.cast).keys {
                affinities[feature, default: 0] += direction * feature.kind.weight
            }
        }
        guard affinities.values.contains(where: { $0 > 0 }) else { return [] }

        var seen = Set<String>()
        var results: [Recommendation] = []
        for movie in movies {
            try Task.checkCancellation()
            guard seen.insert(movie.id).inserted, !excluded.contains(movie.id) else { continue }
            var score = 0
            var positive: [TasteMatch] = []
            for (feature, name) in features(movie.genres ?? [], movie.director, movie.cast ?? []) {
                let contribution = affinities[feature, default: 0]
                score += contribution
                if contribution > 0 {
                    positive.append(TasteMatch(kind: feature.kind, name: name, contribution: contribution))
                }
            }
            guard score > 0 else { continue }
            positive.sort {
                if $0.contribution != $1.contribution { return $0.contribution > $1.contribution }
                if $0.kind != $1.kind { return $0.kind.rawValue < $1.kind.rawValue }
                return $0.name.lowercased() < $1.name.lowercased()
            }
            results.append(Recommendation(movie: movie, score: score, matches: Array(positive.prefix(3))))
        }
        try Task.checkCancellation()
        results.sort { lhs, rhs in
            if lhs.score != rhs.score { return lhs.score > rhs.score }
            let left = popularity(lhs.movie)
            let right = popularity(rhs.movie)
            if left != right { return left > right }
            let leftTitle = normalizedTitle(lhs.movie)
            let rightTitle = normalizedTitle(rhs.movie)
            if leftTitle != rightTitle { return leftTitle < rightTitle }
            return lhs.movie.id < rhs.movie.id
        }
        return Array(results.prefix(30))
    }

    private static func features(_ genres: [String], _ director: String?, _ cast: [String]) -> [Feature: String] {
        var result: [Feature: String] = [:]
        func add(_ name: String, kind: TasteMatch.Kind) {
            let display = name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !display.isEmpty else { return }
            let key = Feature(kind: kind, normalizedName: display.lowercased())
            if result[key] == nil { result[key] = display }
        }
        for genre in genres { add(genre, kind: .genre) }
        if let director { add(director, kind: .director) }
        for actor in cast { add(actor, kind: .actor) }
        return result
    }

    private static func popularity(_ movie: Movie) -> Double {
        guard let value = movie.popularity, value.isFinite else { return 0 }
        return value
    }

    private static func normalizedTitle(_ movie: Movie) -> String {
        movie.title.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}
