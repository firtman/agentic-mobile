import ImageIO
import SwiftUI

private actor PosterImages {
    static let shared = PosterImages()
    private let cache = NSCache<NSURL, CGImage>()
    private let session: URLSession = {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.urlCache = nil
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: configuration)
    }()

    private init() {
        cache.totalCostLimit = 24 * 1024 * 1024
    }

    func image(from url: URL) async throws -> CGImage {
        if let image = cache.object(forKey: url as NSURL) { return image }
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
              let source = CGImageSourceCreateWithData(data as CFData, nil),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                  kCGImageSourceCreateThumbnailFromImageAlways: true,
                  kCGImageSourceCreateThumbnailWithTransform: true,
                  kCGImageSourceThumbnailMaxPixelSize: 720
              ] as CFDictionary)
        else { throw URLError(.cannotDecodeContentData) }
        try Task.checkCancellation()
        cache.setObject(image, forKey: url as NSURL, cost: image.bytesPerRow * image.height)
        return image
    }
}

func suppliedWebURL(_ value: String?) -> URL? {
    guard let value, let url = URL(string: value), url.host != nil,
          let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http"
    else { return nil }
    return url
}

struct MoviePoster: View {
    let movie: Movie
    let aspect: CGFloat
    @State private var image: CGImage?

    var body: some View {
        let url = suppliedWebURL(movie.posterUrl)
        Color.clear
            .aspectRatio(aspect, contentMode: .fit)
            .overlay {
                if let image {
                    GeometryReader { geometry in
                        Image(decorative: image, scale: 1)
                            .resizable()
                            .scaledToFill()
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .clipped()
                    }
                } else {
                    fallback
                }
            }
            .clipped()
            .accessibilityHidden(true)
            .task(id: url) {
                image = nil
                guard let url, let loaded = try? await PosterImages.shared.image(from: url),
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
        let index = movie.id.utf8.reduce(0) { ($0 * 31 + Int($1)) % tones.count }
        return ZStack {
            tones[index]
            Text(movie.title)
                .editorialFont(.movieTitle)
                .foregroundStyle(EditorialPalette(.dark).primary)
                .multilineTextAlignment(.center)
                .lineLimit(4)
                .padding(8)
        }
    }
}
