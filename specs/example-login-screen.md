# Feature: Login Screen

> Example mobile spec demonstrating UI component, form validation, and authentication flow

## 1. Problem Statement

Users need to authenticate to access the app's features. Currently, there's no login screen, so users cannot access personalized content or secure functionality.

## 2. Goals

- G1: Provide a native login screen following platform design guidelines
- G2: Validate user input with clear error feedback
- G3: Securely transmit credentials to the authentication API
- G4: Store session tokens securely using platform keychain/keystore
- G5: Support accessibility requirements (VoiceOver/TalkBack)

## 3. Non-Goals

- NG1: Social authentication (OAuth with Google/Apple) - separate spec
- NG2: Biometric authentication (Face ID/Touch ID) - future enhancement
- NG3: Password reset flow - separate spec
- NG4: Account creation/registration - separate spec
- NG5: "Remember me" functionality - future enhancement

## 4. Functional Requirements

### R1: Screen Layout

#### Android (Jetpack Compose)
- Material 3 design system
- Text fields with outlined style
- Primary button with loading state
- Logo/branding at top
- Support landscape and portrait orientations
- Keyboard-aware layout (content scrolls when keyboard appears)

#### iOS (SwiftUI)
- Human Interface Guidelines compliance
- Native text field styles
- Primary button with activity indicator
- Logo/branding at top
- Support all device sizes (iPhone SE to Pro Max)
- Keyboard avoidance built-in

### R2: Email Field
- Text input with email keyboard type
- Placeholder: "Email"
- Auto-capitalization disabled
- Autocorrect disabled
- Clear button when focused
- Error state styling when validation fails

### R3: Password Field
- Secure text entry (masked characters)
- Placeholder: "Password"
- Toggle button to show/hide password
- Clear button when focused
- Error state styling when validation fails

### R4: Login Button
- Full-width primary button
- Text: "Log In"
- Disabled state when form is invalid
- Loading state during API call (spinner replaces text)
- Prevents double-submission

### R5: Form Validation
- Email: Required, valid email format (regex pattern)
- Password: Required, minimum 8 characters
- Validation runs on:
  - Field blur (when user leaves field)
  - Form submission attempt
- Error messages appear below respective fields

### R6: Authentication Flow
- On submit: Call POST /api/auth/login
- Request body: `{ "email": string, "password": string }`
- Success (200): Store tokens, navigate to home screen
- Unauthorized (401): Show "Invalid email or password"
- Network error: Show "Unable to connect. Please try again."
- Server error (5xx): Show "Something went wrong. Please try again."

### R7: Token Storage
- Store access token in secure storage:
  - Android: EncryptedSharedPreferences or Android Keystore
  - iOS: Keychain with kSecAttrAccessibleWhenUnlockedThisDeviceOnly
- Store refresh token separately with same security level
- Clear tokens on logout

### R8: Accessibility
- All interactive elements have accessibility labels
- Form fields announce their purpose and error state
- Loading state is announced to screen readers
- Minimum touch target size: 44x44 points (iOS) / 48x48 dp (Android)
- Color contrast ratio minimum 4.5:1 for text

## 5. Acceptance Criteria

### Form Display

#### A1: Screen renders correctly
- GIVEN: User launches app without authentication
- WHEN: Login screen is displayed
- THEN: Screen shows logo, email field, password field, and login button
- AND: Login button is disabled (form is empty)

#### A2: Responsive layout
- GIVEN: Login screen is displayed
- WHEN: Device is rotated or screen size changes
- THEN: Layout adapts appropriately
- AND: All elements remain visible and usable

### Email Validation

#### A3: Empty email rejected
- GIVEN: Email field is empty
- WHEN: User attempts to submit or leaves field
- THEN: Error message "Email is required" appears below field
- AND: Field shows error styling

#### A4: Invalid email format rejected
- GIVEN: Email field contains "notanemail"
- WHEN: User attempts to submit or leaves field
- THEN: Error message "Please enter a valid email" appears
- AND: Field shows error styling

#### A5: Valid email accepted
- GIVEN: Email field contains "user@example.com"
- WHEN: User leaves field or submits
- THEN: No error message shown
- AND: Field shows normal styling

### Password Validation

#### A6: Empty password rejected
- GIVEN: Password field is empty
- WHEN: User attempts to submit or leaves field
- THEN: Error message "Password is required" appears
- AND: Field shows error styling

#### A7: Short password rejected
- GIVEN: Password field contains "abc123" (7 characters)
- WHEN: User attempts to submit or leaves field
- THEN: Error message "Password must be at least 8 characters" appears
- AND: Field shows error styling

#### A8: Valid password accepted
- GIVEN: Password field contains "securepassword123"
- WHEN: User leaves field or submits
- THEN: No error message shown
- AND: Field shows normal styling

#### A9: Password visibility toggle
- GIVEN: Password field contains text
- WHEN: User taps show/hide toggle
- THEN: Password visibility toggles between masked and plain text
- AND: Toggle icon updates accordingly

### Form Submission

#### A10: Valid form enables button
- GIVEN: Email is "user@example.com" and password is "password123"
- WHEN: Both fields pass validation
- THEN: Login button becomes enabled

#### A11: Submission shows loading state
- GIVEN: Valid credentials entered
- WHEN: User taps login button
- THEN: Button shows loading indicator
- AND: Button is disabled to prevent double-tap
- AND: Form fields are disabled during request

#### A12: Successful login navigates
- GIVEN: Valid credentials for existing user
- WHEN: API returns 200 with tokens
- THEN: Tokens are stored in secure storage
- AND: User is navigated to home screen
- AND: Login screen is removed from navigation stack

#### A13: Invalid credentials show error
- GIVEN: Email/password combination is incorrect
- WHEN: API returns 401
- THEN: Error message "Invalid email or password" appears
- AND: Loading state is dismissed
- AND: Form fields are re-enabled
- AND: Password field is cleared

#### A14: Network error shows message
- GIVEN: Device has no network connection
- WHEN: User attempts to login
- THEN: Error message "Unable to connect. Please try again." appears
- AND: Loading state is dismissed
- AND: Retry is possible

#### A15: Server error shows message
- GIVEN: API returns 500 error
- WHEN: Response is received
- THEN: Error message "Something went wrong. Please try again." appears
- AND: Loading state is dismissed

### Security

#### A16: Tokens stored securely
- GIVEN: Login succeeds with tokens
- WHEN: Tokens are persisted
- THEN: Access token is stored in platform-secure storage
- AND: Tokens are not accessible to other apps
- AND: Tokens are not included in device backups

#### A17: Password not logged
- GIVEN: Any login attempt
- WHEN: Request is made
- THEN: Password is never logged to console or analytics
- AND: Password is transmitted over HTTPS only

### Accessibility

#### A18: Screen reader support
- GIVEN: VoiceOver (iOS) or TalkBack (Android) is enabled
- WHEN: User navigates login screen
- THEN: All fields and buttons announce their purpose
- AND: Error messages are announced when they appear
- AND: Loading state is announced

#### A19: Touch targets meet minimum size
- GIVEN: Login screen is displayed
- WHEN: Measuring interactive elements
- THEN: All touch targets are at least 44x44 pt (iOS) / 48x48 dp (Android)

## 6. Constraints & Invariants

### C1: Platform Guidelines
- Android: Follow Material 3 design guidelines
- iOS: Follow Human Interface Guidelines
- Use native components, not cross-platform abstractions

### C2: API Contract
```json
// POST /api/auth/login
// Request
{
  "email": "string",
  "password": "string"
}

// Response 200
{
  "access_token": "string (JWT)",
  "refresh_token": "string",
  "expires_in": "number (seconds)",
  "user": {
    "id": "string",
    "email": "string",
    "name": "string"
  }
}

// Response 401
{
  "error": "invalid_credentials",
  "message": "Invalid email or password"
}
```

### C3: Email Validation Regex
```
^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
```

### C4: Security Requirements
- All API calls over HTTPS
- Token storage uses platform keychain/keystore
- No sensitive data in logs
- Password fields use secure text entry
- Clear password from memory after submission

### C5: Performance
- Screen renders within 100ms of navigation
- Form validation is instant (< 16ms)
- API timeout: 30 seconds
- Loading indicator appears within 100ms of tap

## 7. Open Questions

- Q1: Should we support "Sign in with Apple" on iOS?
  - **Decision:** Not in this spec. Create separate spec for social auth.

- Q2: What's the token expiration policy?
  - **Decision:** Access token: 15 minutes, Refresh token: 7 days. Handled by auth service.

- Q3: Should password requirements be shown upfront?
  - **Decision:** No, show error only when validation fails. Keep UI clean.

## 8. Data Model

### LoginState (ViewModel State)
```kotlin
// Android
data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

```swift
// iOS
struct LoginState {
    var email: String = ""
    var password: String = ""
    var emailError: String?
    var passwordError: String?
    var isPasswordVisible: Bool = false
    var isLoading: Bool = false
    var error: String?
}
```

## 9. Testing Strategy

### Unit Tests
- Email validation logic
- Password validation logic
- State management (ViewModel)
- Token storage operations

### UI Tests
- Form submission happy path
- Validation error display
- Loading state visibility
- Navigation after success
- Accessibility compliance

### Integration Tests
- Full login flow with mock API
- Error handling scenarios
- Token persistence verification
