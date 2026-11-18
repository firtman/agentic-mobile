import Foundation

/// Owns a single process-memory catalog shared by the features that need the full data set.
actor CatalogRepository {
    private let client: CatalogClient
    private var cached: Catalog?
    private var inFlight: Task<Catalog, Error>?

    init(client: CatalogClient) {
        self.client = client
    }

    func load() async throws -> Catalog {
        if let cached { return cached }
        if let inFlight { return try await inFlight.value }

        // A departing screen can discard its work while another screen still needs this load.
        let request = Task { try await client.catalog() }
        inFlight = request
        defer { inFlight = nil }
        let catalog = try await request.value
        cached = catalog
        return catalog
    }
}
