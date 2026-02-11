# Mobile Architecture Guide

This document describes the architecture patterns used in both Android and iOS applications.

## Overview

Both platforms follow **MVVM with Clean Architecture** principles:

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │   View/Screen   │◄───│   ViewModel     │                 │
│  │  (UI Components)│    │  (State Holder) │                 │
│  └─────────────────┘    └────────┬────────┘                 │
└──────────────────────────────────┼──────────────────────────┘
                                   │
┌──────────────────────────────────┼──────────────────────────┐
│                       Domain Layer                           │
│                    ┌─────────────▼────────┐                 │
│                    │      Use Cases       │                 │
│                    │  (Business Logic)    │                 │
│                    └─────────────┬────────┘                 │
└──────────────────────────────────┼──────────────────────────┘
                                   │
┌──────────────────────────────────┼──────────────────────────┐
│                        Data Layer                            │
│  ┌─────────────────┐    ┌───────▼─────────┐                 │
│  │  Remote Source  │◄───│   Repository    │───►│Local Source│
│  │    (API)        │    │                 │    │   (DB)     │
│  └─────────────────┘    └─────────────────┘    └────────────┘
└─────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### Presentation Layer
- **Views/Screens**: Pure UI rendering, receives state, sends actions
- **ViewModels**: Holds UI state, processes user actions, calls use cases

### Domain Layer
- **Use Cases**: Encapsulate business logic, orchestrate data operations
- **Domain Models**: Pure data classes representing business entities

### Data Layer
- **Repositories**: Abstract data sources, handle caching strategy
- **Remote Sources**: API clients, network requests
- **Local Sources**: Database access, file storage

## Android Implementation

### Directory Structure
```
app/src/main/java/com/example/app/
├── di/                          # Dependency Injection
│   ├── AppModule.kt             # App-level dependencies
│   ├── NetworkModule.kt         # Retrofit, OkHttp
│   └── DatabaseModule.kt        # Room database
│
├── data/
│   ├── remote/
│   │   ├── api/                 # Retrofit service interfaces
│   │   └── dto/                 # Data Transfer Objects
│   ├── local/
│   │   ├── dao/                 # Room DAOs
│   │   └── entity/              # Room entities
│   └── repository/              # Repository implementations
│
├── domain/
│   ├── model/                   # Domain models
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # Use case classes
│
└── ui/
    ├── theme/                   # Material theme
    ├── components/              # Reusable composables
    ├── navigation/              # Navigation setup
    └── screens/
        └── feature/
            ├── FeatureScreen.kt
            ├── FeatureViewModel.kt
            └── FeatureState.kt
```

### Dependency Injection with Hilt

```kotlin
// AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideUserRepository(
        api: UserApi,
        dao: UserDao
    ): UserRepository {
        return UserRepositoryImpl(api, dao)
    }
}

// Usage in ViewModel
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    // ...
}
```

### State Management

```kotlin
// Immutable state
data class FeatureState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<Item> = emptyList()
)

// Actions as sealed interface
sealed interface FeatureAction {
    data object Refresh : FeatureAction
    data class ItemClicked(val id: String) : FeatureAction
    data class DeleteItem(val id: String) : FeatureAction
}

// ViewModel
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: FeatureUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(FeatureState())
    val state: StateFlow<FeatureState> = _state.asStateFlow()
    
    fun onAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.Refresh -> refresh()
            is FeatureAction.ItemClicked -> navigateToDetail(action.id)
            is FeatureAction.DeleteItem -> deleteItem(action.id)
        }
    }
    
    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            useCase.getItems()
                .onSuccess { items ->
                    _state.update { it.copy(isLoading = false, items = items) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
```

## iOS Implementation

### Directory Structure
```
App/Sources/
├── App/
│   ├── AppApp.swift             # @main entry point
│   ├── AppState.swift           # Global app state
│   └── ContentView.swift        # Root view
│
├── Core/
│   ├── DI/
│   │   └── DependencyContainer.swift
│   ├── Network/
│   │   ├── APIClient.swift
│   │   └── Endpoints.swift
│   ├── Data/
│   │   ├── DTOs/
│   │   └── Repositories/
│   └── Domain/
│       ├── Models/
│       └── UseCases/
│
├── Features/
│   └── Feature/
│       ├── FeatureView.swift
│       ├── FeatureViewModel.swift
│       └── FeatureModels.swift
│
└── UI/
    ├── Theme/
    ├── Components/
    └── Navigation/
```

### Dependency Injection

```swift
// DependencyContainer.swift
@MainActor
final class DependencyContainer {
    static let shared = DependencyContainer()
    
    // Services
    lazy var apiClient = APIClient.shared
    
    // Repositories
    lazy var userRepository: UserRepository = {
        UserRepositoryImpl(apiClient: apiClient)
    }()
    
    // Use Cases
    lazy var getUserUseCase: GetUserUseCase = {
        GetUserUseCase(repository: userRepository)
    }()
}

// Environment-based injection
private struct DependencyKey: EnvironmentKey {
    static let defaultValue = DependencyContainer.shared
}

extension EnvironmentValues {
    var dependencies: DependencyContainer {
        get { self[DependencyKey.self] }
        set { self[DependencyKey.self] = newValue }
    }
}
```

### State Management

```swift
// State struct
struct FeatureState {
    var isLoading = false
    var error: String?
    var items: [Item] = []
}

// Actions enum
enum FeatureAction {
    case refresh
    case itemTapped(id: String)
    case deleteItem(id: String)
}

// ViewModel with @Observable (iOS 17+)
@Observable
@MainActor
final class FeatureViewModel {
    private(set) var state = FeatureState()
    
    private let useCase: FeatureUseCase
    
    init(useCase: FeatureUseCase = DependencyContainer.shared.featureUseCase) {
        self.useCase = useCase
    }
    
    func handle(_ action: FeatureAction) {
        switch action {
        case .refresh:
            refresh()
        case .itemTapped(let id):
            navigateToDetail(id)
        case .deleteItem(let id):
            deleteItem(id)
        }
    }
    
    private func refresh() {
        Task {
            state.isLoading = true
            state.error = nil
            
            do {
                state.items = try await useCase.getItems()
            } catch {
                state.error = error.localizedDescription
            }
            
            state.isLoading = false
        }
    }
}
```

## Data Flow

### Unidirectional Data Flow (UDF)

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│    ┌─────────┐     Action     ┌─────────────┐             │
│    │  View   │ ───────────────► │  ViewModel │             │
│    │         │                 │             │             │
│    │         │ ◄─────────────── │             │             │
│    └─────────┘     State       └──────┬──────┘             │
│                                       │                    │
│                                       │ Call               │
│                                       ▼                    │
│                               ┌─────────────┐              │
│                               │  Use Case   │              │
│                               └──────┬──────┘              │
│                                       │                    │
│                                       ▼                    │
│                               ┌─────────────┐              │
│                               │ Repository  │              │
│                               └─────────────┘              │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

1. **View** renders current **State**
2. User interaction triggers **Action**
3. **ViewModel** receives Action, updates State
4. State change triggers View re-render

## Error Handling

### Android
```kotlin
// Result wrapper
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

// Usage in repository
suspend fun getUser(id: String): Result<User> {
    return try {
        Result.Success(api.getUser(id).toDomain())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

### iOS
```swift
// Use Swift's native error handling
func getUser(id: String) async throws -> User {
    let response = try await apiClient.get("/users/\(id)")
    return response.toDomain()
}

// In ViewModel
do {
    let user = try await useCase.getUser(id: id)
    state.user = user
} catch {
    state.error = error.localizedDescription
}
```

## Testing

### Android
```kotlin
@Test
fun `when refresh succeeds then state has items`() = runTest {
    // Given
    val items = listOf(Item("1", "Test"))
    coEvery { useCase.getItems() } returns Result.Success(items)
    
    // When
    viewModel.onAction(FeatureAction.Refresh)
    
    // Then
    viewModel.state.test {
        val state = awaitItem()
        assertThat(state.items).isEqualTo(items)
        assertThat(state.isLoading).isFalse()
    }
}
```

### iOS
```swift
func testRefreshSuccessUpdatesState() async {
    // Given
    let items = [Item(id: "1", name: "Test")]
    mockUseCase.itemsToReturn = items
    let viewModel = FeatureViewModel(useCase: mockUseCase)
    
    // When
    await viewModel.handle(.refresh)
    
    // Then
    XCTAssertEqual(viewModel.state.items, items)
    XCTAssertFalse(viewModel.state.isLoading)
}
```

## Best Practices

1. **Keep Views Dumb**: Views only render state and forward actions
2. **Immutable State**: State classes are immutable, create new instances on change
3. **Single Source of Truth**: ViewModel owns the state
4. **Testable Use Cases**: Business logic is isolated and testable
5. **Repository Pattern**: Abstract data sources behind interfaces
6. **Error Handling**: Always handle and display errors gracefully
7. **Loading States**: Show loading indicators for async operations

## Resources

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [SwiftUI Data Flow](https://developer.apple.com/documentation/swiftui/model-data)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
