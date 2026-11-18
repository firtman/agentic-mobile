import SwiftUI
import SwiftData

enum LumioRoute: Hashable {
    case movie(String)
}

private enum Destination: String, CaseIterable, Identifiable {
    case explore, swipe, genie, arcade

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .explore: "Explore"
        case .swipe: "Swipe"
        case .genie: "Genie"
        case .arcade: "Arcade"
        }
    }

    var icon: String {
        switch self {
        case .explore: "compass"
        case .swipe: "layers-2"
        case .genie: "sparkles"
        case .arcade: "gamepad-2"
        }
    }
}

struct LumioShell: View {
    let catalog: CatalogClient
    let ratings: RatingsStore
    @State private var explore: ExploreModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.horizontalSizeClass) private var width
    @State private var selection: Destination = .explore
    @State private var paths: [Destination: NavigationPath] = [:]
    @State private var showingSettings = false
    @State private var sidebarVisibility: NavigationSplitViewVisibility = .all

    init(catalog: CatalogClient, ratings: RatingsStore) {
        self.catalog = catalog
        self.ratings = ratings
        _explore = State(initialValue: ExploreModel(
            catalog: catalog,
            savedSort: UserDefaults.standard.string(forKey: "explore.sort")
        ))
    }

    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        Group {
            if width == .regular {
                NavigationSplitView(columnVisibility: $sidebarVisibility) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(Destination.allCases) { destination in
                                destinationButton(destination, sidebar: true)
                            }
                        }
                        .padding()
                    }
                    .background(palette.canvas)
                    .navigationTitle("Lumio")
                } detail: {
                    navigation(for: selection)
                }
                .navigationSplitViewStyle(.balanced)
            } else {
                navigation(for: selection)
                    .safeAreaInset(edge: .bottom, spacing: 0) {
                        if paths[selection, default: NavigationPath()].isEmpty {
                            compactNavigation
                        }
                    }
            }
        }
        .tint(palette.accent)
        .sheet(isPresented: $showingSettings) {
            NavigationStack {
                screen(title: "Settings", icon: "settings-2") {}
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Close") { showingSettings = false }
                                .accessibilityIdentifier("settings-close")
                        }
                    }
            }
        }
    }

    private var compactDestinations: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Destination.allCases) { destination in
                destinationButton(destination, sidebar: false)
            }
        }
    }

    @ViewBuilder
    private var compactNavigation: some View {
        if #available(iOS 26, *) {
            compactDestinations
                .padding(.horizontal, 6)
                .padding(.vertical, 5)
                .glassEffect(.regular, in: .rect(cornerRadius: 30))
                .padding(.horizontal, 14)
                .padding(.vertical, 5)
        } else {
            compactDestinations
                .padding(.vertical, 4)
                .background(palette.raised)
                .overlay(alignment: .top) { Rectangle().fill(palette.divider).frame(height: 1) }
        }
    }

    private func navigation(for destination: Destination) -> some View {
        NavigationStack(path: Binding(
            get: { paths[destination, default: NavigationPath()] },
            set: { paths[destination] = $0 }
        )) {
            Group {
                if destination == .explore {
                    ExploreScreen(model: explore)
                        .navigationTitle(destination.title)
                        .navigationBarTitleDisplayMode(.inline)
                        .editorialNavigationBar(canvas: palette.canvas)
                } else {
                    screen(title: destination.title, icon: destination.icon) {}
                }
            }
            .toolbar {
                settingsToolbar
            }
            .navigationDestination(for: LumioRoute.self) { route in
                switch route {
                case .movie(let id):
                    MovieDetailsScreen(movieID: id, catalog: catalog, ratings: ratings)
                        .toolbar { settingsToolbar }
                }
            }
        }
        .id(destination)
    }

    private var settingsToolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                showingSettings = true
            } label: {
                LucideIcon(name: "settings-2")
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Settings")
            .accessibilityIdentifier("settings")
        }
    }

    private func screen<Content: View>(
        title: LocalizedStringKey,
        icon: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                LucideIcon(name: icon)
                    .foregroundStyle(palette.accent)
                Text(title)
                    .editorialFont(.title)
                    .foregroundStyle(palette.primary)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("screen-title")
                content()
            }
            .frame(maxWidth: 560, alignment: .leading)
            .padding(24)
            .frame(maxWidth: .infinity)
        }
        .background(palette.canvas)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .editorialNavigationBar(canvas: palette.canvas)
    }

    private func destinationButton(_ destination: Destination, sidebar: Bool) -> some View {
        Button {
            selection = destination
        } label: {
            let layout = sidebar ? AnyLayout(HStackLayout(spacing: 10)) : AnyLayout(VStackLayout(spacing: 2))
            layout {
                LucideIcon(name: destination.icon)
                    .foregroundStyle(selection == destination ? palette.ink : palette.primary)
                    .frame(width: 48, height: 32)
                    .background(selection == destination ? palette.coral : .clear,
                                in: RoundedRectangle(cornerRadius: 8))
                Text(destination.title)
                    .editorialFont(.navigation)
                    .foregroundStyle(palette.primary)
                    .multilineTextAlignment(sidebar ? .leading : .center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, minHeight: 48, alignment: sidebar ? .leading : .center)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selection == destination ? .isSelected : [])
        .accessibilityIdentifier("destination-\(destination.rawValue)")
    }
}

#Preview {
    LumioShell(catalog: .configured(), ratings: RatingsStore(
        configuration: ModelConfiguration(isStoredInMemoryOnly: true)
    ))
}
