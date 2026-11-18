import SwiftUI

@main
struct LumioApp: App {
    private let catalog = CatalogClient.configured()

    var body: some Scene {
        WindowGroup {
            LumioShell(catalog: catalog)
        }
    }
}
