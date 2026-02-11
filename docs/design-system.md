# Design System Guide

This document outlines the design system and UI component standards for both Android and iOS platforms.

## Overview

Both platforms should maintain visual consistency while respecting platform-specific design guidelines:
- **Android**: Material 3 Design System
- **iOS**: Human Interface Guidelines

## Color System

### Semantic Colors

Define colors by their purpose, not their value:

| Semantic Name | Android (Material 3) | iOS |
|---------------|---------------------|-----|
| Primary | `MaterialTheme.colorScheme.primary` | `Color.accentColor` |
| On Primary | `MaterialTheme.colorScheme.onPrimary` | `.white` |
| Background | `MaterialTheme.colorScheme.background` | `Color(.systemBackground)` |
| Surface | `MaterialTheme.colorScheme.surface` | `Color(.secondarySystemBackground)` |
| Error | `MaterialTheme.colorScheme.error` | `Color.red` |
| On Error | `MaterialTheme.colorScheme.onError` | `.white` |

### Android Theme Setup

```kotlin
// Color.kt
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_background = Color(0xFFFFFBFE)
// ... more colors

val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
val md_theme_dark_background = Color(0xFF1C1B1F)
// ... more colors

// Theme.kt
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    background = md_theme_light_background,
    // ...
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    background = md_theme_dark_background,
    // ...
)
```

### iOS Theme Setup

```swift
// AppTheme.swift
enum AppTheme {
    enum Colors {
        static let primary = Color.accentColor
        static let background = Color(.systemBackground)
        static let surface = Color(.secondarySystemBackground)
        static let error = Color.red
        static let success = Color.green
    }
}

// Supports automatic dark mode via system colors
```

## Typography

### Android (Material 3 Type Scale)

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

### iOS (Dynamic Type)

```swift
// Use built-in text styles for Dynamic Type support
Text("Headline")
    .font(.headline)

Text("Body text")
    .font(.body)

Text("Caption")
    .font(.caption)

// Custom fonts with Dynamic Type
Text("Custom")
    .font(.custom("MyFont", size: 16, relativeTo: .body))
```

## Spacing

### Consistent Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `xxs` | 4 | Minimal padding |
| `xs` | 8 | Tight spacing |
| `sm` | 12 | Compact spacing |
| `md` | 16 | Standard spacing |
| `lg` | 24 | Comfortable spacing |
| `xl` | 32 | Section spacing |
| `xxl` | 48 | Large sections |

### Android
```kotlin
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

### iOS
```swift
enum Spacing {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 8
    static let sm: CGFloat = 12
    static let md: CGFloat = 16
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
    static let xxl: CGFloat = 48
}
```

## Component Library

### Buttons

#### Primary Button

**Android:**
```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}
```

**iOS:**
```swift
struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var isLoading: Bool = false
    var isEnabled: Bool = true
    
    var body: some View {
        Button(action: action) {
            HStack {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                }
                Text(title)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(!isEnabled || isLoading)
    }
}
```

### Text Fields

**Android:**
```kotlin
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
```

**iOS:**
```swift
struct AppTextField: View {
    let label: String
    @Binding var text: String
    var error: String?
    var keyboardType: UIKeyboardType = .default
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            TextField(label, text: $text)
                .keyboardType(keyboardType)
                .textFieldStyle(.roundedBorder)
            
            if let error {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
    }
}
```

### Cards

**Android:**
```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
```

**iOS:**
```swift
struct AppCard<Content: View>: View {
    let content: () -> Content
    
    var body: some View {
        content()
            .padding(Spacing.md)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
```

### Loading States

**Android:**
```kotlin
@Composable
fun LoadingView(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

**iOS:**
```swift
struct LoadingView: View {
    var message: String?
    
    var body: some View {
        VStack(spacing: Spacing.md) {
            ProgressView()
            if let message {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
```

### Error States

**Android:**
```kotlin
@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        onRetry?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = it) {
                Text("Retry")
            }
        }
    }
}
```

**iOS:**
```swift
struct ErrorView: View {
    let message: String
    var onRetry: (() -> Void)?
    
    var body: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(.red)
            
            Text(message)
                .multilineTextAlignment(.center)
            
            if let onRetry {
                Button("Retry", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }
}
```

## Icons

### Android
Use Material Icons:
```kotlin
Icon(
    imageVector = Icons.Filled.Home,
    contentDescription = "Home"
)

// Extended icons
Icon(
    imageVector = Icons.Outlined.AccountCircle,
    contentDescription = "Profile"
)
```

### iOS
Use SF Symbols:
```swift
Image(systemName: "house.fill")
    .accessibilityLabel("Home")

Image(systemName: "person.circle")
    .accessibilityLabel("Profile")
```

## Responsive Design

### Android
```kotlin
@Composable
fun ResponsiveLayout(
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val isCompact = maxWidth < 600.dp
        val isExpanded = maxWidth >= 840.dp
        
        if (isCompact) {
            // Phone layout
        } else if (isExpanded) {
            // Tablet/desktop layout
        } else {
            // Medium layout
        }
    }
}
```

### iOS
```swift
struct ResponsiveLayout<Content: View>: View {
    @Environment(\.horizontalSizeClass) var sizeClass
    let content: () -> Content
    
    var body: some View {
        if sizeClass == .compact {
            // iPhone layout
        } else {
            // iPad layout
        }
    }
}
```

## Animation

### Android
```kotlin
// Animate visibility
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically()
) {
    Content()
}

// Animate values
val alpha by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0.5f
)
```

### iOS
```swift
// Animate visibility
withAnimation(.easeInOut) {
    isVisible.toggle()
}

// Animate values
Text("Hello")
    .opacity(isSelected ? 1 : 0.5)
    .animation(.easeInOut, value: isSelected)
```

## Dark Mode

### Android
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### iOS
Use semantic colors that automatically adapt:
```swift
// These automatically support dark mode
Color(.systemBackground)
Color(.label)
Color(.secondaryLabel)
Color(.tertiarySystemBackground)
```

## Resources

- [Material 3 Design](https://m3.material.io/)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)
- [SF Symbols](https://developer.apple.com/sf-symbols/)
- [Material Icons](https://fonts.google.com/icons)
