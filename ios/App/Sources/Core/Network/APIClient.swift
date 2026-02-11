import Foundation

/// API client for network requests
actor APIClient {
    static let shared = APIClient()
    
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    
    private var baseURL: URL {
        #if DEBUG
        URL(string: "https://api.dev.example.com")!
        #else
        URL(string: "https://api.example.com")!
        #endif
    }
    
    init(session: URLSession = .shared) {
        self.session = session
        
        self.decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        
        self.encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.keyEncodingStrategy = .convertToSnakeCase
    }
    
    // MARK: - Public Methods
    
    func get<T: Decodable>(_ path: String, queryItems: [URLQueryItem]? = nil) async throws -> T {
        let request = try buildRequest(method: "GET", path: path, queryItems: queryItems)
        return try await execute(request)
    }
    
    func post<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        var request = try buildRequest(method: "POST", path: path)
        request.httpBody = try encoder.encode(body)
        return try await execute(request)
    }
    
    func put<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        var request = try buildRequest(method: "PUT", path: path)
        request.httpBody = try encoder.encode(body)
        return try await execute(request)
    }
    
    func delete(_ path: String) async throws {
        let request = try buildRequest(method: "DELETE", path: path)
        let (_, response) = try await session.data(for: request)
        try validateResponse(response)
    }
    
    // MARK: - Private Methods
    
    private func buildRequest(
        method: String,
        path: String,
        queryItems: [URLQueryItem]? = nil
    ) throws -> URLRequest {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: true)
        components?.queryItems = queryItems
        
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add auth token if available
        // if let token = await AuthManager.shared.accessToken {
        //     request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        // }
        
        return request
    }
    
    private func execute<T: Decodable>(_ request: URLRequest) async throws -> T {
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            return
        case 401:
            throw APIError.unauthorized
        case 404:
            throw APIError.notFound
        case 400...499:
            throw APIError.clientError(httpResponse.statusCode)
        case 500...599:
            throw APIError.serverError(httpResponse.statusCode)
        default:
            throw APIError.unknown(httpResponse.statusCode)
        }
    }
}

// MARK: - API Errors

enum APIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case unauthorized
    case notFound
    case clientError(Int)
    case serverError(Int)
    case unknown(Int)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .unauthorized:
            return "Please log in to continue"
        case .notFound:
            return "Resource not found"
        case .clientError(let code):
            return "Request error (code: \(code))"
        case .serverError(let code):
            return "Server error (code: \(code))"
        case .unknown(let code):
            return "Unknown error (code: \(code))"
        }
    }
}
