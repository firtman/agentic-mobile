import CoreText
import SwiftUI

struct EditorialPalette {
    private let dark: Bool

    init(_ scheme: ColorScheme) {
        dark = scheme == .dark
    }

    var canvas: Color { color(0xF7F2E8, 0x1D1C1A) }
    var raised: Color { color(0xFFFCF7, 0x292723) }
    var subtle: Color { color(0xEEE7DC, 0x34312C) }
    var primary: Color { color(0x242320, 0xF7F2E8) }
    var secondary: Color { color(0x69635C, 0xBDB4A8) }
    var accent: Color { color(0xA83F30, 0xF47B65) }
    var coral: Color { color(0xF47B65, 0xF47B65) }
    var ink: Color { color(0x242320, 0x242320) }
    var outline: Color { color(0x8C8378, 0x8C8378) }
    var divider: Color { color(0xD9CFC0, 0x49443D) }
    var success: Color { color(0x3C6750, 0x9CC8AA) }
    var error: Color { color(0xA73838, 0xF3A09B) }
    var warning: Color { color(0x785A18, 0xE4C47D) }

    private func color(_ light: UInt32, _ night: UInt32) -> Color {
        let rgb = dark ? night : light
        return Color(
            red: Double((rgb >> 16) & 255) / 255,
            green: Double((rgb >> 8) & 255) / 255,
            blue: Double(rgb & 255) / 255
        )
    }
}

enum EditorialTextStyle {
    case title, body, control, navigation

    var size: CGFloat {
        switch self {
        case .title: 30
        case .body: 17
        case .control: 16
        case .navigation: 13
        }
    }

    var relativeTo: Font.TextStyle {
        switch self {
        case .title: .title
        case .body, .control: .body
        case .navigation: .footnote
        }
    }

    private static let fraunces = loadFont("fraunces_variable")
    private static let dmSans = loadFont("dm_sans_variable")

    func font(at size: CGFloat) -> Font {
        guard let source = self == .title ? Self.fraunces : Self.dmSans else {
            return .system(size: size, weight: self == .title ? .semibold : .regular,
                           design: self == .title ? .serif : .default)
        }
        let font = CTFontCreateWithGraphicsFont(source, size, nil, nil)
        let weight = switch self {
        case .title, .control: 600.0
        case .body: 400.0
        case .navigation: 500.0
        }
        var descriptor = CTFontCopyFontDescriptor(font)
        let weightTag = 0x77676874 as CFIndex // wght
        descriptor = CTFontDescriptorCreateCopyWithVariation(descriptor, weightTag as CFNumber, weight)
        return Font(CTFontCreateCopyWithAttributes(font, size, nil, descriptor))
    }

    private static func loadFont(_ name: String) -> CGFont? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "ttf"),
              let provider = CGDataProvider(url: url as CFURL) else { return nil }
        return CGFont(provider)
    }
}

private struct EditorialFont: ViewModifier {
    let style: EditorialTextStyle
    @ScaledMetric private var size: CGFloat

    init(style: EditorialTextStyle) {
        self.style = style
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: style.relativeTo)
    }

    func body(content: Content) -> some View {
        content.font(style.font(at: size))
    }
}

extension View {
    func editorialFont(_ style: EditorialTextStyle) -> some View {
        modifier(EditorialFont(style: style))
    }
}

struct LucideIcon: View {
    let name: String

    var body: some View {
        Image(name)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: 24, height: 24)
            .accessibilityHidden(true)
    }
}
