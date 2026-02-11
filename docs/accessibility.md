# Accessibility Guide

This document outlines accessibility requirements and implementation patterns for Android and iOS applications.

## Overview

Accessibility ensures your app is usable by everyone, including people with disabilities. Both platforms provide robust accessibility frameworks that must be properly implemented.

## Core Principles

1. **Perceivable**: Information must be presentable to users in ways they can perceive
2. **Operable**: UI components must be operable by all users
3. **Understandable**: Information and operation must be understandable
4. **Robust**: Content must be robust enough for assistive technologies

## Minimum Requirements

| Requirement | Android | iOS |
|-------------|---------|-----|
| Touch Target Size | 48x48 dp | 44x44 pt |
| Color Contrast Ratio | 4.5:1 (text), 3:1 (large text) | 4.5:1 (text), 3:1 (large text) |
| Screen Reader Support | TalkBack | VoiceOver |
| Font Scaling | Up to 200% | Dynamic Type (all sizes) |
| Keyboard Navigation | Full support | Full support |

## Screen Reader Support

### Android (TalkBack)

#### Content Descriptions
```kotlin
// Images
Image(
    painter = painterResource(R.drawable.profile),
    contentDescription = "User profile picture"
)

// Decorative images (no announcement)
Image(
    painter = painterResource(R.drawable.decorative),
    contentDescription = null
)

// Icons with actions
IconButton(onClick = { /* delete */ }) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete item"
    )
}
```

#### Semantic Properties
```kotlin
// Headings
Text(
    text = "Section Title",
    modifier = Modifier.semantics { heading() }
)

// Buttons with state
Button(
    onClick = { },
    modifier = Modifier.semantics {
        stateDescription = if (isSelected) "Selected" else "Not selected"
    }
) {
    Text("Option")
}

// Custom actions
Box(
    modifier = Modifier.semantics {
        customActions = listOf(
            CustomAccessibilityAction("Delete") { delete(); true },
            CustomAccessibilityAction("Edit") { edit(); true }
        )
    }
)

// Live regions (announce changes)
Text(
    text = "Items in cart: $count",
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
)
```

#### Grouping Related Elements
```kotlin
// Merge child semantics into parent
Row(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "John Doe, Software Engineer, Online"
    }
) {
    Image(/* avatar */)
    Column {
        Text("John Doe")
        Text("Software Engineer")
    }
    Icon(/* online indicator */)
}
```

### iOS (VoiceOver)

#### Accessibility Labels
```swift
// Images
Image("profile")
    .accessibilityLabel("User profile picture")

// Decorative images
Image("decorative")
    .accessibilityHidden(true)

// Buttons
Button(action: { /* delete */ }) {
    Image(systemName: "trash")
}
.accessibilityLabel("Delete item")
```

#### Accessibility Traits
```swift
// Headers
Text("Section Title")
    .accessibilityAddTraits(.isHeader)

// Buttons with state
Button(action: { }) {
    Text("Option")
}
.accessibilityAddTraits(isSelected ? .isSelected : [])

// Images that are buttons
Image(systemName: "gear")
    .accessibilityAddTraits(.isButton)
```

#### Grouping Elements
```swift
// Combine elements into single announcement
HStack {
    Image("avatar")
    VStack {
        Text("John Doe")
        Text("Software Engineer")
    }
    Image(systemName: "circle.fill")
}
.accessibilityElement(children: .combine)
.accessibilityLabel("John Doe, Software Engineer, Online")
```

#### Custom Actions
```swift
Text("Item")
    .accessibilityAction(named: "Delete") {
        deleteItem()
    }
    .accessibilityAction(named: "Edit") {
        editItem()
    }
```

## Touch Target Size

### Android
```kotlin
// Minimum 48dp touch target
IconButton(
    onClick = { },
    modifier = Modifier.size(48.dp)  // Explicit size
) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        modifier = Modifier.size(24.dp)  // Visual size can be smaller
    )
}

// Using minimumInteractiveComponentSize
Text(
    text = "Clickable",
    modifier = Modifier
        .clickable { }
        .minimumInteractiveComponentSize()
)
```

### iOS
```swift
// Minimum 44pt touch target
Button(action: { }) {
    Image(systemName: "xmark")
        .frame(width: 44, height: 44)
}

// Increase hit area without changing visual
Button(action: { }) {
    Text("Small text")
}
.frame(minWidth: 44, minHeight: 44)
```

## Color and Contrast

### Guidelines
- **Normal text**: 4.5:1 contrast ratio minimum
- **Large text** (18pt+ or 14pt bold): 3:1 contrast ratio minimum
- **Icons and graphics**: 3:1 contrast ratio minimum
- Don't rely solely on color to convey information

### Android
```kotlin
// Use semantic colors
Text(
    text = "Error message",
    color = MaterialTheme.colorScheme.error
)

// Add icon alongside color
Row {
    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error
    )
    Text("Error: Invalid input")
}
```

### iOS
```swift
// Use semantic colors
Text("Error message")
    .foregroundStyle(.red)

// Add icon alongside color
HStack {
    Image(systemName: "exclamationmark.circle")
    Text("Error: Invalid input")
}
.foregroundStyle(.red)
```

## Dynamic Type / Font Scaling

### Android
```kotlin
// Use sp units for text (scales with system settings)
Text(
    text = "Body text",
    fontSize = 16.sp  // Will scale
)

// Test with different font scales in preview
@Preview(fontScale = 2.0f)
@Composable
fun LargeFontPreview() {
    MyScreen()
}

// Handle text overflow
Text(
    text = longText,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

### iOS
```swift
// Use built-in text styles (automatically scale)
Text("Body text")
    .font(.body)

// Custom fonts with scaling
Text("Custom")
    .font(.custom("MyFont", size: 16, relativeTo: .body))

// Test with environment modifier
MyView()
    .environment(\.sizeCategory, .accessibilityExtraExtraExtraLarge)

// Handle text that might truncate
Text(longText)
    .lineLimit(2)
    .truncationMode(.tail)
```

## Keyboard Navigation (iOS)

```swift
// Focus management
@FocusState private var focusedField: Field?

enum Field {
    case email
    case password
}

var body: some View {
    VStack {
        TextField("Email", text: $email)
            .focused($focusedField, equals: .email)
            .submitLabel(.next)
            .onSubmit { focusedField = .password }
        
        SecureField("Password", text: $password)
            .focused($focusedField, equals: .password)
            .submitLabel(.done)
            .onSubmit { login() }
    }
}
```

## Motion and Animation

### Reduce Motion Support

#### Android
```kotlin
@Composable
fun AnimatedContent() {
    val reduceMotion = LocalReduceMotion.current
    
    val animationSpec = if (reduceMotion) {
        snap<Float>()  // No animation
    } else {
        tween<Float>(durationMillis = 300)
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(animationSpec),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(animationSpec)
    ) {
        Content()
    }
}
```

#### iOS
```swift
struct AnimatedContent: View {
    @Environment(\.accessibilityReduceMotion) var reduceMotion
    
    var body: some View {
        Content()
            .animation(reduceMotion ? nil : .easeInOut, value: someValue)
    }
}
```

## Form Accessibility

### Android
```kotlin
Column {
    // Associate label with field
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email address") },
        modifier = Modifier.semantics {
            error = emailError  // Announce error
        },
        isError = emailError != null
    )
    
    // Error message
    emailError?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            }
        )
    }
}
```

### iOS
```swift
VStack(alignment: .leading) {
    TextField("Email address", text: $email)
        .accessibilityLabel("Email address")
        .accessibilityValue(email.isEmpty ? "Empty" : email)
    
    if let error = emailError {
        Text(error)
            .foregroundStyle(.red)
            .accessibilityLabel("Error: \(error)")
    }
}
```

## Testing Accessibility

### Android

#### Using Accessibility Scanner
1. Install Accessibility Scanner from Play Store
2. Enable in Settings > Accessibility
3. Run scan on your app
4. Review and fix issues

#### Compose Testing
```kotlin
@Test
fun checkAccessibility() {
    composeTestRule.setContent {
        MyScreen()
    }
    
    // Check content description exists
    composeTestRule
        .onNodeWithContentDescription("Profile picture")
        .assertExists()
    
    // Check minimum touch target
    composeTestRule
        .onNodeWithContentDescription("Delete")
        .assertTouchHeightIsAtLeast(48.dp)
        .assertTouchWidthIsAtLeast(48.dp)
}
```

### iOS

#### Using Accessibility Inspector
1. Open Xcode > Developer Tools > Accessibility Inspector
2. Connect device or simulator
3. Inspect elements and audit

#### XCUITest
```swift
func testAccessibility() {
    let app = XCUIApplication()
    app.launch()
    
    // Check element exists and is accessible
    XCTAssertTrue(app.images["Profile picture"].exists)
    
    // Check button is accessible
    let deleteButton = app.buttons["Delete"]
    XCTAssertTrue(deleteButton.isHittable)
}
```

## Accessibility Checklist

### Per Screen
- [ ] All images have appropriate content descriptions
- [ ] All buttons and interactive elements are labeled
- [ ] Headings are marked as headings
- [ ] Reading order is logical
- [ ] Focus order follows visual order
- [ ] Touch targets are at least 44/48 pt/dp
- [ ] Color is not sole means of conveying information
- [ ] Contrast ratios meet minimums

### Per Form
- [ ] Labels are associated with inputs
- [ ] Error messages are announced
- [ ] Required fields are indicated
- [ ] Keyboard navigation works
- [ ] Form submission is accessible

### App-Wide
- [ ] Respects system font size settings
- [ ] Respects reduce motion setting
- [ ] Works with screen reader enabled
- [ ] Works with keyboard only (iOS)
- [ ] No time-sensitive interactions without alternatives

## Spec Template Addition

When writing specs for mobile features, include an accessibility section:

```markdown
## 8. Accessibility Requirements

### Screen Reader
- [ ] All interactive elements have labels
- [ ] Images have descriptions (or hidden if decorative)
- [ ] Headings marked appropriately
- [ ] Reading order is logical

### Visual
- [ ] Touch targets minimum 44/48 pt/dp
- [ ] Color contrast meets WCAG AA (4.5:1)
- [ ] Information not conveyed by color alone

### Motion
- [ ] Animations respect reduce motion setting

### Testing
- [ ] Test with TalkBack (Android)
- [ ] Test with VoiceOver (iOS)
- [ ] Test with large fonts (200%)
```

## Resources

- [Android Accessibility](https://developer.android.com/guide/topics/ui/accessibility)
- [iOS Accessibility](https://developer.apple.com/accessibility/)
- [WCAG Guidelines](https://www.w3.org/WAI/standards-guidelines/wcag/)
- [Material Design Accessibility](https://m3.material.io/foundations/accessible-design)
- [Apple HIG Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
