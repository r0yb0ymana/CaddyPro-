import Foundation

/// Simple dependency injection container
@MainActor
final class DependencyContainer {
    static let shared = DependencyContainer()
    
    // MARK: - Services
    
    lazy var apiClient: APIClient = {
        APIClient.shared
    }()
    
    // MARK: - Repositories
    
    // lazy var userRepository: UserRepository = {
    //     UserRepositoryImpl(apiClient: apiClient)
    // }()
    
    // MARK: - Use Cases
    
    // lazy var loginUseCase: LoginUseCase = {
    //     LoginUseCase(userRepository: userRepository)
    // }()
    
    private init() {}
}

// MARK: - Environment Key

private struct DependencyContainerKey: EnvironmentKey {
    static let defaultValue = DependencyContainer.shared
}

extension EnvironmentValues {
    var dependencies: DependencyContainer {
        get { self[DependencyContainerKey.self] }
        set { self[DependencyContainerKey.self] = newValue }
    }
}
