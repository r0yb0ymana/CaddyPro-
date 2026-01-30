import Foundation
import SwiftUI

/// Manages navigation state for the NavCaddy system.
///
/// Spec R3: Executes navigation as deep link with <100ms latency budget.
/// Observable class that coordinates programmatic navigation using SwiftUI NavigationStack.
@Observable
@MainActor
final class NavigationCoordinator {
    /// Navigation path for NavigationStack
    private(set) var path: NavigationPath

    /// Current destination (top of stack)
    var currentDestination: NavCaddyDestination? {
        // NavigationPath doesn't expose its contents directly in a type-safe way
        // Track separately for observability
        _destinationStack.last
    }

    /// Internal tracking of destination stack for debugging and state queries
    private var _destinationStack: [NavCaddyDestination] = []

    /// Initialization
    init() {
        self.path = NavigationPath()
    }

    /// Navigates to a destination by pushing it onto the stack
    ///
    /// - Parameter destination: The destination to navigate to
    func navigate(to destination: NavCaddyDestination) {
        path.append(destination)
        _destinationStack.append(destination)

        // Log navigation for debugging
        print("[NavigationCoordinator] Navigating to: \(destination.description)")
    }

    /// Navigates back by popping the current destination
    func navigateBack() {
        guard !_destinationStack.isEmpty else {
            print("[NavigationCoordinator] Cannot navigate back: stack is empty")
            return
        }

        path.removeLast()
        _destinationStack.removeLast()

        print("[NavigationCoordinator] Navigated back. Stack depth: \(_destinationStack.count)")
    }

    /// Pops to root by clearing the entire navigation stack
    func popToRoot() {
        guard !_destinationStack.isEmpty else {
            return
        }

        let count = _destinationStack.count
        path.removeLast(count)
        _destinationStack.removeAll()

        print("[NavigationCoordinator] Popped to root. Cleared \(count) destinations.")
    }

    /// Replaces the current destination with a new one
    ///
    /// - Parameter destination: The new destination
    func replace(with destination: NavCaddyDestination) {
        if !_destinationStack.isEmpty {
            path.removeLast()
            _destinationStack.removeLast()
        }

        path.append(destination)
        _destinationStack.append(destination)

        print("[NavigationCoordinator] Replaced destination with: \(destination.description)")
    }

    /// Returns the depth of the navigation stack
    var stackDepth: Int {
        _destinationStack.count
    }

    /// Returns whether the stack is empty
    var isEmpty: Bool {
        _destinationStack.isEmpty
    }

    /// Returns the full destination stack for debugging
    var destinationStack: [NavCaddyDestination] {
        _destinationStack
    }
}
