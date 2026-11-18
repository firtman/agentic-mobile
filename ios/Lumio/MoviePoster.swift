import ImageIO
import SwiftUI

/// Decoded posters only, held in memory. NSCache is thread-safe, so views can peek without an actor hop.
private final class PosterCache: @unchecked Sendable {
    static let sizes = [240, 480, 720]
    private let cache = NSCache<NSString, CGImage>()

    init(costLimit: Int) {
        cache.totalCostLimit = costLimit
    }

    func image(for url: URL, maxPixel: Int) -> CGImage? {
        // A larger decode already in memory serves a smaller request without another download.
        for size in Self.sizes where size >= maxPixel {
            if let image = cache.object(forKey: Self.key(url, size)) { return image }
        }
        return nil
    }

    func store(_ image: CGImage, for url: URL, maxPixel: Int) {
        cache.setObject(image, forKey: Self.key(url, maxPixel), cost: image.bytesPerRow * image.height)
    }

    static func key(_ url: URL, _ size: Int) -> NSString {
        "\(size) \(url.absoluteString)" as NSString
    }
}

actor PosterImages {
    static let shared = PosterImages()
    // NSCache still purges under memory pressure; nothing is written to disk.
    private let cache = PosterCache(costLimit: 96 * 1024 * 1024)
    private let session: URLSession = {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.urlCache = nil
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: configuration)
    }()
    private var loading: [NSString: Task<CGImage, Error>] = [:]

    /// The smallest decode size that still covers a poster shown `side` points long.
    static func pixelSize(forPoints side: CGFloat, scale: CGFloat) -> Int {
        PosterCache.sizes.first { CGFloat($0) >= side * scale } ?? 720
    }

    nonisolated func cached(from url: URL, maxPixel: Int = 720) -> CGImage? {
        cache.image(for: url, maxPixel: maxPixel)
    }

    func image(from url: URL, maxPixel: Int = 720) async throws -> CGImage {
        if let image = cache.image(for: url, maxPixel: maxPixel) { return image }
        let key = PosterCache.key(url, maxPixel)
        // One download and decode per poster, even when it is shown in two places at once.
        if let task = loading[key] { return try await task.value }
        let session = session, cache = cache
        let task = Task.detached(priority: .userInitiated) { () throws -> CGImage in
            let (data, response) = try await session.data(from: url)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
                  let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                      kCGImageSourceCreateThumbnailFromImageAlways: true,
                      kCGImageSourceCreateThumbnailWithTransform: true,
                      kCGImageSourceThumbnailMaxPixelSize: maxPixel
                  ] as CFDictionary)
            else { throw URLError(.cannotDecodeContentData) }
            // Finished work is kept even if the requesting view has gone away.
            cache.store(image, for: url, maxPixel: maxPixel)
            return image
        }
        loading[key] = task
        defer { loading[key] = nil }
        return try await task.value
    }
}

private struct PosterRequest: Equatable {
    let url: URL?
    let maxPixel: Int?
}

func suppliedWebURL(_ value: String?) -> URL? {
    guard let value, let url = URL(string: value), url.host != nil,
          let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http"
    else { return nil }
    return url
}

struct MoviePoster: View {
    let id: String
    let title: String
    let posterURL: String?
    let aspect: CGFloat
    let contentMode: ContentMode
    @State private var image: CGImage?
    @State private var maxPixel: Int?
    @Environment(\.displayScale) private var displayScale

    init(movie: Movie, aspect: CGFloat, contentMode: ContentMode = .fill) {
        self.init(id: movie.id, title: movie.title, posterURL: movie.posterUrl, aspect: aspect, contentMode: contentMode)
    }

    init(id: String, title: String, posterURL: String?, aspect: CGFloat, contentMode: ContentMode = .fill) {
        self.id = id
        self.title = title
        self.posterURL = posterURL
        self.aspect = aspect
        self.contentMode = contentMode
    }

    var body: some View {
        let url = suppliedWebURL(posterURL)
        Color.clear
            .aspectRatio(aspect, contentMode: .fit)
            .overlay {
                if let image {
                    GeometryReader { geometry in
                        Image(decorative: image, scale: 1)
                            .resizable()
                            .aspectRatio(contentMode: contentMode)
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .clipped()
                    }
                } else {
                    fallback
                }
            }
            .background(EditorialPalette(.dark).subtle)
            .clipped()
            .accessibilityHidden(true)
            .onGeometryChange(for: Int.self) { proxy in
                PosterImages.pixelSize(forPoints: max(proxy.size.width, proxy.size.height), scale: displayScale)
            } action: { maxPixel = $0 }
            .task(id: PosterRequest(url: url, maxPixel: maxPixel)) {
                guard let url, let maxPixel else {
                    image = nil
                    return
                }
                // A poster already in memory appears without a fallback frame.
                if let cached = PosterImages.shared.cached(from: url, maxPixel: maxPixel) {
                    image = cached
                    return
                }
                image = nil
                guard let loaded = try? await PosterImages.shared.image(from: url, maxPixel: maxPixel),
                      !Task.isCancelled else { return }
                image = loaded
            }
    }

    private var fallback: some View {
        let tones = [
            Color(red: 0.20, green: 0.24, blue: 0.20),
            Color(red: 0.28, green: 0.20, blue: 0.19),
            Color(red: 0.22, green: 0.22, blue: 0.30),
            Color(red: 0.28, green: 0.24, blue: 0.17)
        ]
        let index = id.utf8.reduce(0) { ($0 * 31 + Int($1)) % tones.count }
        return ZStack {
            tones[index]
            Text(title)
                .editorialFont(.movieTitle)
                .foregroundStyle(EditorialPalette(.dark).primary)
                .multilineTextAlignment(.center)
                .lineLimit(4)
                .padding(8)
        }
    }
}
