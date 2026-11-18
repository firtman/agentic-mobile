import SwiftUI
import SwiftData

@main
struct LumioApp: App {
    private let catalog: CatalogClient
    private let repository: CatalogRepository
    @State private var ratings = Self.makeRatings()

    init() {
        let catalog = CatalogClient.configured()
        self.catalog = catalog
        repository = CatalogRepository(client: catalog)
    }

    var body: some Scene {
        WindowGroup {
            LumioShell(catalog: catalog, repository: repository, ratings: ratings)
        }
    }

    private static func makeRatings() -> RatingsStore {
        #if DEBUG
        if ProcessInfo.processInfo.environment["LUMIO_EPHEMERAL_RATINGS"] == "1" {
            return RatingsStore(configuration: ModelConfiguration(isStoredInMemoryOnly: true))
        }
        #endif
        return RatingsStore()
    }
}
