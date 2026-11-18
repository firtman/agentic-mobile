import SwiftUI
import SwiftData

@main
struct LumioApp: App {
    private let catalog = CatalogClient.configured()
    @State private var ratings = Self.makeRatings()

    var body: some Scene {
        WindowGroup {
            LumioShell(catalog: catalog, ratings: ratings)
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
