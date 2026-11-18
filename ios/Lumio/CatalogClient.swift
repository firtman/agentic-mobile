import Foundation

struct CatalogClient: Sendable {
    let baseURL: URL
    private let session: URLSession

    init(baseURL: URL, session: URLSession? = nil) {
        self.baseURL = baseURL
        if let session {
            self.session = session
        } else {
            let configuration = URLSessionConfiguration.ephemeral
            configuration.urlCache = nil
            configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
            self.session = URLSession(configuration: configuration)
        }
    }

    static func configured() -> Self {
        let configured = Bundle.main.object(forInfoDictionaryKey: "LumioAPIBaseURL") as? String
        #if DEBUG
        let value = ProcessInfo.processInfo.environment["LUMIO_API_BASE_URL"] ?? configured
        let allowedSchemes = ["http", "https"]
        #else
        let value = configured
        let allowedSchemes = ["https"]
        #endif
        if let value, let url = URL(string: value),
           let scheme = url.scheme, allowedSchemes.contains(scheme), url.host != nil {
            return Self(baseURL: url)
        }
        return Self(baseURL: URL(string: "https://movie-api-delta-six.vercel.app")!)
    }

    func health() async throws -> CatalogHealth {
        try await get("health")
    }

    func movies(query: [URLQueryItem] = []) async throws -> MoviePage {
        try await get("movies", query: query)
    }

    func movie(id: String) async throws -> Movie {
        try await get("movies", id: id)
    }

    func genres() async throws -> CatalogGenres {
        try await get("genres")
    }

    func catalog() async throws -> Catalog {
        try await get("catalog")
    }

    private func get<Value: Decodable>(
        _ route: String,
        id: String? = nil,
        query: [URLQueryItem] = []
    ) async throws -> Value {
        let endpoint = baseURL.appendingPathComponent(route)
        guard var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false) else {
            throw URLError(.badURL)
        }
        if let id {
            let segmentCharacters = CharacterSet(charactersIn:
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
            guard let segment = id.addingPercentEncoding(withAllowedCharacters: segmentCharacters),
                  !segment.isEmpty, segment != ".", segment != ".." else {
                throw URLError(.badURL)
            }
            components.percentEncodedPath += "/" + segment
        }
        if !query.isEmpty {
            components.queryItems = query
        }
        guard let url = components.url else { throw URLError(.badURL) }
        let (data, response) = try await session.data(from: url)
        guard let response = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200..<300).contains(response.statusCode) else {
            throw CatalogHTTPError(statusCode: response.statusCode)
        }
        #if !DEBUG
        guard response.url?.scheme == "https" else { throw URLError(.secureConnectionFailed) }
        #endif
        return try JSONDecoder().decode(Value.self, from: data)
    }
}

struct CatalogHTTPError: Error, Equatable {
    let statusCode: Int
}

struct CatalogHealth: Decodable, Equatable {
    let status: String
    let movies: Int
}

struct Movie: Decodable, Identifiable, Sendable {
    let id: String
    let title: String
    let year: Int?
    let synopsis: String?
    let genres: [String]?
    let director: String?
    let cast: [String]?
    let runtime: Int?
    let rating: Double?
    let posterUrl: String?
    let backdropUrl: String?
    let sourceUrl: String?
    let releaseDate: String?
    let voteCount: Int?
    let popularity: Double?
    let originalLanguage: String?
    let datasetSourceUrl: String?
}

struct MoviePage: Decodable, Sendable {
    let movies: [Movie]
    let total: Int
    let limit: Int
    let offset: Int
}

struct CatalogGenres: Decodable, Sendable {
    let genres: [String]
    let languages: [String]
}

struct TriviaQuestion: Decodable, Identifiable, Sendable {
    let id: String
    let movieId: String
    let kind: String
    let question: String
    let options: [String]
    let answerIndex: Int
}

struct Catalog: Decodable, Sendable {
    let movies: [Movie]
    let trivia: [TriviaQuestion]
}
