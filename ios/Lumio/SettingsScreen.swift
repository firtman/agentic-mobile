import SwiftUI

enum AppAppearance: String, CaseIterable {
    case system, light, dark

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }

    var title: LocalizedStringKey {
        switch self {
        case .system: "System"
        case .light: "Light"
        case .dark: "Dark"
        }
    }
}

enum SettingKey {
    static let appearance = "settings.appearance"
    static let gameSound = "settings.gameSound"
}

struct SettingsScreen: View {
    @Binding var appearance: AppAppearance
    @AppStorage(SettingKey.gameSound) private var gameSound = true
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = EditorialPalette(scheme)
        Form {
            Section("Appearance") {
                ForEach(AppAppearance.allCases, id: \.self) { option in
                    Button {
                        appearance = option
                    } label: {
                        HStack {
                            Text(option.title)
                            Spacer()
                            if appearance == option {
                                LucideIcon(name: "check")
                                    .foregroundStyle(palette.accent)
                                    .accessibilityHidden(true)
                            }
                        }
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(appearance == option ? .isSelected : [])
                    .accessibilityIdentifier("appearance-\(option.rawValue)")
                }
            }
            .listRowBackground(palette.raised)

            Section {
                Toggle("Game sound", isOn: $gameSound)
                    .accessibilityIdentifier("game-sound")
            }
            .listRowBackground(palette.raised)

            Section("Credits and licenses") {
                notice("Catalog sources", file: "catalog_sources")
                notice("Fraunces", file: "fraunces_ofl")
                notice("DM Sans", file: "dm_sans_ofl")
                notice("Lucide", file: "lucide")
            }
            .listRowBackground(palette.raised)
        }
        .editorialFont(.body)
        .foregroundStyle(palette.primary)
        .scrollContentBackground(.hidden)
        .background(palette.canvas)
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func notice(_ title: LocalizedStringKey, file: String) -> some View {
        NavigationLink {
            NoticeScreen(title: title, file: file)
        } label: {
            Text(title)
        }
        .accessibilityIdentifier("notice-\(file)")
    }
}

private struct NoticeScreen: View {
    let title: LocalizedStringKey
    let file: String
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let url = Bundle.main.url(forResource: file, withExtension: "txt"),
                   let contents = try? String(contentsOf: url, encoding: .utf8) {
                    ForEach(Array(contents.components(separatedBy: "\n\n").enumerated()), id: \.offset) { _, paragraph in
                        if file == "catalog_sources" {
                            Text((try? AttributedString(markdown: paragraph)) ?? AttributedString(paragraph))
                        } else {
                            Text(verbatim: paragraph)
                        }
                    }
                } else {
                    Text("This notice is unavailable.")
                }
            }
            .textSelection(.enabled)
            .editorialFont(.body)
            .frame(maxWidth: 640, alignment: .leading)
            .padding()
            .frame(maxWidth: .infinity)
        }
        .background(EditorialPalette(scheme).canvas)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
