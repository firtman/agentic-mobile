import SwiftUI

struct CatalogConnection: View {
    let catalog: CatalogClient
    @Environment(\.colorScheme) private var scheme
    @State private var status: Status = .loading
    @State private var attempt = 0

    private enum Status {
        case loading, connected, failed
    }

    var body: some View {
        let palette = EditorialPalette(scheme)
        VStack(alignment: .leading, spacing: 12) {
            switch status {
            case .loading:
                ProgressView("Connecting to the catalog…")
            case .connected:
                Label {
                    Text("Catalog connected")
                } icon: {
                    LucideIcon(name: "check")
                }
                .foregroundStyle(palette.success)
                .accessibilityIdentifier("catalog-connected")
            case .failed:
                Label {
                    Text("Couldn’t connect to the catalog.")
                } icon: {
                    LucideIcon(name: "wifi-off")
                }
                .foregroundStyle(palette.error)
                Button {
                    attempt += 1
                } label: {
                    Text("Retry")
                        .editorialFont(.control)
                        .foregroundStyle(palette.ink)
                        .padding(.horizontal, 16)
                        .frame(minHeight: 44)
                        .background(palette.coral, in: RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("catalog-retry")
            }
        }
        .editorialFont(.body)
        .task(id: attempt) {
            status = .loading
            do {
                let health = try await catalog.health()
                try Task.checkCancellation()
                status = health.status == "ok" ? .connected : .failed
            } catch {
                if !Task.isCancelled { status = .failed }
            }
        }
    }
}
