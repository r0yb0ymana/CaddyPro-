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

    // MARK: - Memory Management (Task 14)

    /// Shot recorder for tracking shots
    lazy var shotRecorder: ShotRecorder = {
        ShotRecorder(repository: navCaddyRepository)
    }()

    /// Pattern aggregator for miss pattern analysis
    lazy var missPatternAggregator: MissPatternAggregator = {
        MissPatternAggregator(repository: navCaddyRepository)
    }()

    /// Miss pattern store - main interface for miss pattern operations
    lazy var missPatternStore: MissPatternStore = {
        MissPatternStore(repository: navCaddyRepository)
    }()

    // MARK: - Session Context (Task 15)

    /// Session context manager for conversation continuity
    lazy var sessionContextManager: SessionContextManager = {
        SessionContextManager()
    }()

    // MARK: - Analytics (Task 22)

    /// Analytics service for observability and debugging
    lazy var analytics: NavCaddyAnalytics = {
        #if DEBUG
        // Use console analytics in debug builds
        ConsoleAnalytics(enablePIIRedaction: true, verbose: true)
        #else
        // Use no-op analytics in release builds (maximum privacy)
        NoOpAnalytics()
        #endif
    }()

    // MARK: - Offline Mode (Task 24)

    /// Network monitor for connectivity status
    lazy var networkMonitor: NetworkMonitor = {
        NetworkMonitor()
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
