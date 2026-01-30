import SwiftUI

// MARK: - Integration Example for NavCaddy Navigation System
//
// This file demonstrates how to integrate the navigation system into your app.
// DO NOT COMPILE THIS FILE - It's for reference only.
//
// Spec: R3 (Routing Orchestrator) - Deep link navigation integration
//
// Usage:
// 1. Update ContentView to use NavigationStack with NavigationCoordinator
// 2. Create destination views for each NavCaddyDestination case
// 3. Use NavigationExecutor in your ViewModel to process routing results

#if false // Prevent compilation - this is example code only

// MARK: - Step 1: ContentView Integration

struct ContentView: View {
    @Environment(\.dependencies) var dependencies

    var body: some View {
        // Access the shared navigation coordinator
        let coordinator = dependencies.navigationCoordinator

        NavigationStack(path: Binding(
            get: { coordinator.path },
            set: { _ in } // Read-only binding, navigation controlled by coordinator
        )) {
            HomeView()
                .navigationDestination(for: NavCaddyDestination.self) { destination in
                    destinationView(for: destination)
                }
        }
        .environment(\.dependencies, dependencies)
    }

    @ViewBuilder
    private func destinationView(for destination: NavCaddyDestination) -> some View {
        switch destination {
        // MARK: Caddy Module
        case .clubAdjustment(let club):
            ClubAdjustmentView(club: club)

        case .shotRecommendation(let yardage, let club, let lie, let wind):
            ShotRecommendationView(
                yardage: yardage,
                club: club,
                lie: lie,
                wind: wind
            )

        case .weatherCheck(let location):
            WeatherCheckView(location: location)

        case .courseInfo(let courseId):
            CourseInfoView(courseId: courseId)

        case .roundStart(let courseId):
            RoundStartView(preselectedCourseId: courseId)

        case .scoreEntry(let hole):
            ScoreEntryView(hole: hole)

        case .roundEnd:
            RoundEndView()

        // MARK: Coach Module
        case .drillScreen(let drillType, let focusClub):
            DrillScreenView(drillType: drillType, focusClub: focusClub)

        case .statsLookup(let statType, let dateRange):
            StatsLookupView(statType: statType, dateRange: dateRange)

        case .patternQuery(let club, let pressureContext):
            PatternQueryView(club: club, pressureContext: pressureContext)

        // MARK: Recovery Module
        case .recoveryOverview:
            RecoveryOverviewView()

        case .recoveryDetail(let date):
            RecoveryDetailView(date: date)

        // MARK: Settings Module
        case .equipmentInfo(let clubToEdit):
            EquipmentInfoView(clubToEdit: clubToEdit)

        case .settings(let section):
            SettingsView(section: section)

        case .help:
            HelpView()
        }
    }
}

// MARK: - Step 2: ViewModel Integration

@Observable
@MainActor
final class ConversationViewModel {
    private let routingOrchestrator: RoutingOrchestrator
    private let navigationExecutor: NavigationExecutor

    // State
    private(set) var messages: [Message] = []
    private(set) var isProcessing = false
    private(set) var currentAlert: AlertInfo?

    init(dependencies: DependencyContainer) {
        // Initialize from dependency container
        self.routingOrchestrator = dependencies.routingOrchestrator
        self.navigationExecutor = dependencies.navigationExecutor
    }

    // MARK: Main Flow

    func handleUserInput(_ input: String) async {
        isProcessing = true
        defer { isProcessing = false }

        // Step 1: Normalize input (from Task 7)
        // let normalizedInput = inputNormalizer.normalize(input)

        // Step 2: Classify intent (from Task 6)
        // let classificationResult = await intentClassifier.classify(normalizedInput)

        // For this example, assume we have a classification result
        let classificationResult = ClassificationResult.route(
            intent: ParsedIntent(
                intentType: .clubAdjustment,
                confidence: 0.92,
                entities: ExtractedEntities(club: "7-iron")
            ),
            target: RoutingTarget(
                module: .caddy,
                screen: "club_adjustment",
                parameters: ["club": "7-iron"]
            )
        )

        // Step 3: Route the intent (from Task 10)
        let routingResult = await routingOrchestrator.route(classificationResult)

        // Step 4: Execute navigation (THIS IS THE NEW PART FROM TASK 12)
        let action = navigationExecutor.execute(routingResult)

        // Step 5: Handle the navigation action in UI
        handleNavigationAction(action)
    }

    // MARK: Navigation Action Handling

    private func handleNavigationAction(_ action: NavigationAction) {
        switch action {
        case .navigated(let destination, let intent):
            // Navigation was successful
            // Optionally add a message to the conversation
            addMessage("Taking you to \(destination.description)", isUser: false)

            print("[Navigation] Navigated to: \(destination.description)")
            print("[Navigation] Intent: \(intent.intentType.rawValue)")

        case .showResponse(let response, let intent):
            // Show response without navigation
            addMessage(response, isUser: false)

            print("[Navigation] Showing response (no navigation)")
            print("[Navigation] Intent: \(intent.intentType.rawValue)")

        case .showPrerequisitePrompt(let message, let prerequisites, let intent):
            // Show alert with missing prerequisites
            currentAlert = AlertInfo(
                title: "Setup Required",
                message: message,
                actions: [
                    AlertAction(title: "Set Up Now", style: .default) { [weak self] in
                        self?.handlePrerequisiteSetup(prerequisites)
                    },
                    AlertAction(title: "Cancel", style: .cancel)
                ]
            )

            print("[Navigation] Prerequisites missing: \(prerequisites.map { $0.description })")

        case .requestConfirmation(let message, let intent):
            // Show confirmation dialog
            currentAlert = AlertInfo(
                title: "Confirm",
                message: message,
                actions: [
                    AlertAction(title: "Yes", style: .default) { [weak self] in
                        self?.confirmIntent(intent)
                    },
                    AlertAction(title: "No", style: .cancel)
                ]
            )

            print("[Navigation] Requesting confirmation for: \(intent.intentType.rawValue)")

        case .showError(let message, let intent):
            // Show error message
            addMessage(message, isUser: false, isError: true)

            print("[Navigation] Error: \(message)")
        }
    }

    // MARK: Helper Methods

    private func addMessage(_ text: String, isUser: Bool, isError: Bool = false) {
        let message = Message(
            text: text,
            isUser: isUser,
            isError: isError,
            timestamp: Date()
        )
        messages.append(message)
    }

    private func handlePrerequisiteSetup(_ prerequisites: [Prerequisite]) {
        // Navigate to setup flow for missing prerequisites
        for prerequisite in prerequisites {
            switch prerequisite {
            case .recoveryData:
                // Navigate to recovery setup
                let destination = NavCaddyDestination.recoveryOverview
                navigationExecutor.navigationCoordinator.navigate(to: destination)

            case .bagConfigured:
                // Navigate to equipment setup
                let destination = NavCaddyDestination.equipmentInfo(clubToEdit: nil)
                navigationExecutor.navigationCoordinator.navigate(to: destination)

            case .roundActive:
                // Navigate to round start
                let destination = NavCaddyDestination.roundStart(courseId: nil)
                navigationExecutor.navigationCoordinator.navigate(to: destination)

            case .courseSelected:
                // Navigate to course selection (not yet implemented)
                addMessage("Please select a course to continue.", isUser: false)
            }
        }
    }

    private func confirmIntent(_ intent: ParsedIntent) async {
        // Re-process with forced confidence
        let highConfidenceIntent = ParsedIntent(
            id: intent.id,
            intentType: intent.intentType,
            confidence: 1.0, // Force high confidence
            entities: intent.entities,
            userGoal: intent.userGoal,
            routingTarget: intent.routingTarget
        )

        // Re-route with high confidence
        if let target = intent.routingTarget {
            let result = RoutingResult.navigate(target: target, intent: highConfidenceIntent)
            let action = navigationExecutor.execute(result)
            handleNavigationAction(action)
        }
    }
}

// MARK: - Supporting Types

struct Message: Identifiable {
    let id = UUID()
    let text: String
    let isUser: Bool
    let isError: Bool
    let timestamp: Date
}

struct AlertInfo: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let actions: [AlertAction]
}

struct AlertAction {
    enum Style {
        case `default`, cancel, destructive
    }

    let title: String
    let style: Style
    let action: (() -> Void)?

    init(title: String, style: Style, action: (() -> Void)? = nil) {
        self.title = title
        self.style = style
        self.action = action
    }
}

// MARK: - Step 3: Direct Navigation Examples

extension ConversationViewModel {
    /// Example: Navigate directly to club adjustment
    func navigateToClubAdjustment(club: String) {
        let destination = NavCaddyDestination.clubAdjustment(club: club)
        navigationExecutor.navigationCoordinator.navigate(to: destination)
    }

    /// Example: Navigate to recovery overview
    func navigateToRecoveryOverview() {
        let destination = NavCaddyDestination.recoveryOverview
        navigationExecutor.navigationCoordinator.navigate(to: destination)
    }

    /// Example: Navigate back
    func navigateBack() {
        navigationExecutor.navigateBack()
    }

    /// Example: Pop to root (return to home)
    func popToRoot() {
        navigationExecutor.popToRoot()
    }
}

// MARK: - Step 4: View Integration Example

struct ConversationView: View {
    @State private var viewModel: ConversationViewModel
    @Environment(\.dependencies) var dependencies

    init() {
        _viewModel = State(initialValue: ConversationViewModel(dependencies: .shared))
    }

    var body: some View {
        VStack {
            // Message list
            ScrollView {
                LazyVStack {
                    ForEach(viewModel.messages) { message in
                        MessageBubble(message: message)
                    }
                }
            }

            // Input bar
            HStack {
                TextField("Ask Bones...", text: $inputText)
                    .textFieldStyle(.roundedBorder)

                Button("Send") {
                    Task {
                        await viewModel.handleUserInput(inputText)
                        inputText = ""
                    }
                }
                .disabled(viewModel.isProcessing)
            }
            .padding()
        }
        .alert(item: $viewModel.currentAlert) { alertInfo in
            Alert(
                title: Text(alertInfo.title),
                message: Text(alertInfo.message),
                primaryButton: .default(Text(alertInfo.actions[0].title)) {
                    alertInfo.actions[0].action?()
                },
                secondaryButton: .cancel()
            )
        }
    }

    @State private var inputText = ""
}

struct MessageBubble: View {
    let message: Message

    var body: some View {
        HStack {
            if message.isUser { Spacer() }

            Text(message.text)
                .padding()
                .background(message.isError ? Color.red : (message.isUser ? Color.blue : Color.gray))
                .foregroundColor(.white)
                .cornerRadius(12)

            if !message.isUser { Spacer() }
        }
        .padding(.horizontal)
    }
}

// MARK: - Placeholder Views (to be implemented in Phase 5)

struct ClubAdjustmentView: View {
    let club: String
    var body: some View {
        VStack {
            Text("Club Adjustment")
                .font(.largeTitle)
            Text("Club: \(club)")
        }
    }
}

struct ShotRecommendationView: View {
    let yardage: Int?
    let club: String?
    let lie: String?
    let wind: String?

    var body: some View {
        VStack {
            Text("Shot Recommendation")
                .font(.largeTitle)
            if let y = yardage { Text("Yardage: \(y)") }
            if let c = club { Text("Club: \(c)") }
            if let l = lie { Text("Lie: \(l)") }
            if let w = wind { Text("Wind: \(w)") }
        }
    }
}

struct RecoveryOverviewView: View {
    var body: some View {
        Text("Recovery Overview")
            .font(.largeTitle)
    }
}

// ... (other placeholder views)

#endif // End example code
