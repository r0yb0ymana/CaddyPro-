import SwiftUI
import Combine

/// Global application state
@MainActor
final class AppState: ObservableObject {
    @Published var isLoading = false
    @Published var currentUser: User?
    @Published var isAuthenticated = false
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Setup observers
        $currentUser
            .map { $0 != nil }
            .assign(to: &$isAuthenticated)
    }
}

// MARK: - User Model
struct User: Identifiable, Codable, Equatable {
    let id: String
    let email: String
    let name: String
    let createdAt: Date
}
