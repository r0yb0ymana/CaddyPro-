import Foundation

/// Represents the target destination for routing after intent classification.
///
/// Spec R3: Routing target includes module, screen, and parameters.
struct RoutingTarget: Codable, Hashable {
    let module: Module
    let screen: String
    let parameters: [String: String]

    init(
        module: Module,
        screen: String,
        parameters: [String: String] = [:]
    ) {
        self.module = module
        self.screen = screen
        self.parameters = parameters
    }
}

// Note: We use [String: String] instead of [String: Any] because:
// 1. It conforms to Codable automatically
// 2. Most parameters will be serializable strings anyway
// 3. Complex types can be JSON-encoded to strings if needed
