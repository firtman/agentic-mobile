import Foundation
import Observation
import SwiftData

enum RatingChoice: String, CaseIterable, Identifiable {
    case like, dislike, unseen

    var id: Self { self }
}

@Model
final class SavedRating {
    @Attribute(.unique) var movieID: String
    var choice: String
    var updatedAt: Date
    var title: String
    var year: Int?
    var posterURL: String?
    var genres: [String]
    var director: String?
    var cast: [String]

    init(movie: Movie, choice: RatingChoice, updatedAt: Date) {
        movieID = movie.id
        self.choice = choice.rawValue
        self.updatedAt = updatedAt
        title = movie.title
        year = movie.year
        posterURL = movie.posterUrl
        genres = movie.genres ?? []
        director = movie.director
        cast = movie.cast ?? []
    }

    func update(movie: Movie, choice: RatingChoice, updatedAt: Date) {
        self.choice = choice.rawValue
        self.updatedAt = updatedAt
        title = movie.title
        year = movie.year
        posterURL = movie.posterUrl
        genres = movie.genres ?? []
        director = movie.director
        cast = movie.cast ?? []
    }
}

@Model
final class RatingUndoEntry {
    @Attribute(.unique) var id: UUID
    var sequence: Int
    var movieID: String
    var previousChoice: String?
    var previousUpdatedAt: Date?

    init(movieID: String, sequence: Int, previous: SavedRating?) {
        id = UUID()
        self.sequence = sequence
        self.movieID = movieID
        previousChoice = previous?.choice
        previousUpdatedAt = previous?.updatedAt
    }
}

@MainActor
@Observable
final class RatingsStore {
    @ObservationIgnored private var container: ModelContainer?
    @ObservationIgnored private let configuration: ModelConfiguration
    @ObservationIgnored private let now: () -> Date
    private static let undoLimit = 200

    /// Advances after every committed change so screens kept in memory can refresh their view of the ledger.
    private(set) var revision = 0

    init(
        configuration: ModelConfiguration = ModelConfiguration("Ratings", cloudKitDatabase: .none),
        now: @escaping () -> Date = Date.init
    ) {
        self.configuration = configuration
        self.now = now
    }

    func choice(for movieID: String) throws -> RatingChoice? {
        let context = try makeContext()
        return try rating(for: movieID, in: context).flatMap { RatingChoice(rawValue: $0.choice) }
    }

    func ratedIDs() throws -> Set<String> {
        let context = try makeContext()
        return Set(try context.fetch(FetchDescriptor<SavedRating>()).map(\.movieID))
    }

    /// The token for the newest remaining change, shared by every global Undo control.
    func latestUndoID() throws -> UUID? {
        let context = try makeContext()
        var newest = FetchDescriptor<RatingUndoEntry>(sortBy: [SortDescriptor(\.sequence, order: .reverse)])
        newest.fetchLimit = 1
        return try context.fetch(newest).first?.id
    }

    /// Returns a token for the committed change. Repeating a choice leaves the ledger unchanged.
    func rate(_ movie: Movie, as choice: RatingChoice) throws -> UUID? {
        let context = try makeContext()
        let current = try rating(for: movie.id, in: context)
        guard current?.choice != choice.rawValue else { return nil }

        let entries = try context.fetch(FetchDescriptor<RatingUndoEntry>(
            sortBy: [SortDescriptor(\.sequence, order: .reverse)]
        ))
        let entry = RatingUndoEntry(
            movieID: movie.id,
            sequence: (entries.first?.sequence ?? 0) + 1,
            previous: current
        )

        let timestamp = now()
        if let current {
            current.update(movie: movie, choice: choice, updatedAt: timestamp)
        } else {
            context.insert(SavedRating(movie: movie, choice: choice, updatedAt: timestamp))
        }
        context.insert(entry)
        for expired in entries.dropFirst(Self.undoLimit - 1) {
            context.delete(expired)
        }
        try context.save()
        revision += 1
        return entry.id
    }

    /// A stale token cannot undo a more recent choice, even if it was made for another film.
    func undoLatest(expectedID: UUID) throws -> Bool {
        let context = try makeContext()
        var newest = FetchDescriptor<RatingUndoEntry>(sortBy: [SortDescriptor(\.sequence, order: .reverse)])
        newest.fetchLimit = 1
        guard let entry = try context.fetch(newest).first, entry.id == expectedID,
              let rating = try rating(for: entry.movieID, in: context)
        else { return false }

        if let previous = entry.previousChoice, let timestamp = entry.previousUpdatedAt {
            rating.choice = previous
            rating.updatedAt = timestamp
        } else {
            context.delete(rating)
        }
        context.delete(entry)
        try context.save()
        revision += 1
        return true
    }

    private func makeContext() throws -> ModelContext {
        if container == nil {
            container = try ModelContainer(for: SavedRating.self, RatingUndoEntry.self, configurations: configuration)
        }
        guard let container else {
            throw CocoaError(.persistentStoreOpen)
        }
        // A failed write discards the entire context, including its pending ledger changes.
        let context = ModelContext(container)
        context.autosaveEnabled = false
        return context
    }

    private func rating(for movieID: String, in context: ModelContext) throws -> SavedRating? {
        var request = FetchDescriptor<SavedRating>(predicate: #Predicate { $0.movieID == movieID })
        request.fetchLimit = 1
        return try context.fetch(request).first
    }
}
