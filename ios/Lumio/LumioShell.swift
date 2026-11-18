import SwiftUI

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
    @Environment(\.colorScheme) private var scheme
    @Environment(\.horizontalSizeClass) private var width
    @State private var selection: Destination = .explore
    @State private var paths: [Destination: NavigationPath] = [:]
    @State private var showingSettings = false
    @State private var sidebarVisibility: NavigationSplitViewVisibility = .all

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
                        HStack(alignment: .top, spacing: 0) {
                            ForEach(Destination.allCases) { destination in
                                destinationButton(destination, sidebar: false)
                            }
                        }
                        .padding(.vertical, 4)
                        .background(palette.raised)
                        .overlay(alignment: .top) { Rectangle().fill(palette.divider).frame(height: 1) }
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

    private func navigation(for destination: Destination) -> some View {
        NavigationStack(path: Binding(
            get: { paths[destination, default: NavigationPath()] },
            set: { paths[destination] = $0 }
        )) {
            screen(title: destination.title, icon: destination.icon) {
                if destination == .explore {
                    CatalogConnection(catalog: catalog)
                }
            }
            .toolbar {
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
        }
        .id(destination)
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
        .toolbarBackground(palette.canvas, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
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
    LumioShell(catalog: .configured())
}
