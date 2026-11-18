import XCTest
@testable import Lumio

final class CatalogClientTests: XCTestCase {
    func testHealthDecodesSuccessfulResponse() async throws {
        let health = try await client().health()
        XCTAssertEqual(health, CatalogHealth(status: "ok", movies: 2))
    }

    func testMovieIDIsSentAsOnePathSegment() async throws {
        let result = try await client().movie(id: "a/b?c")
        XCTAssertEqual(result.id, "a/b?c")
        XCTAssertNil(result.posterUrl)
    }

    func testNonSuccessStatusIsReportedBeforeDecoding() async throws {
        do {
            _ = try await client().genres()
            XCTFail("Expected an HTTP error")
        } catch let error as CatalogHTTPError {
            XCTAssertEqual(error.statusCode, 503)
        }
    }

    private func client() -> CatalogClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [CatalogStub.self]
        return CatalogClient(
            baseURL: URL(string: "https://catalog.example")!,
            session: URLSession(configuration: configuration)
        )
    }
}

private final class CatalogStub: URLProtocol {
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url,
              let path = URLComponents(url: url, resolvingAgainstBaseURL: false)?.percentEncodedPath
        else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }
        let (status, body) = switch path {
        case "/health": (200, #"{"status":"ok","movies":2}"#)
        case "/movies/a%2Fb%3Fc": (200, #"{"id":"a/b?c","title":"Fixture"}"#)
        default: (503, "{}")
        }
        let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
