import Foundation
import SwiftData

/// Simple dependency injection container
@MainActor
final class DependencyContainer {
    static let shared = DependencyContainer()

    // MARK: - Services

    lazy var apiClient: APIClient = {
        APIClient.shared
    }()

    // MARK: - Data

    lazy var navCaddyModelContainer: ModelContainer = {
        do {
            return try NavCaddyDataContainer.create()
        } catch {
            fatalError("Failed to create NavCaddy ModelContainer: \(error)")
        }
    }()

    // MARK: - Repositories

    lazy var navCaddyRepository: NavCaddyRepository = {
        NavCaddyRepositoryImpl(modelContainer: navCaddyModelContainer)
    }()

    // MARK: - Navigation

    /// Navigation coordinator for NavCaddy system
    lazy var navigationCoordinator: NavigationCoordinator = {
        NavigationCoordinator()
    }()

    /// Deep link builder
    lazy var deepLinkBuilder: DeepLinkBuilder = {
        DeepLinkBuilder()
    }()

    /// Navigation executor
    lazy var navigationExecutor: NavigationExecutor = {
        NavigationExecutor(coordinator: navigationCoordinator, deepLinkBuilder: deepLinkBuilder)
    }()

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
