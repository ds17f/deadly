# KMM Compatibility Guide

This guide covers common compatibility issues and best practices when developing Kotlin Multiplatform Mobile (KMM) applications.

## Platform-Specific API Pitfalls

### ❌ Common Mistakes

#### Time and Date Handling
```kotlin
// ❌ JVM-only - Will fail on iOS
val timestamp = System.currentTimeMillis()
val date = Date()
val calendar = Calendar.getInstance()

// ✅ Multiplatform solution
import kotlinx.datetime.Clock
val timestamp = Clock.System.now().toEpochMilliseconds()
val instant = Clock.System.now()
val duration = (endTime - startTime).inWholeMilliseconds
```

#### String Formatting
```kotlin
// ❌ Not available in KMP
val formatted = "%.2f".format(value)
val text = String.format("Value: %d", number)

// ✅ Multiplatform alternatives
val formatted = "${(value * 100).toInt() / 100.0}"
val rounded = kotlin.math.round(value * 10) / 10.0
val text = "Value: $number"
```

#### Collections and Utilities
```kotlin
// ❌ JVM-specific classes
import java.util.UUID
import java.util.Collections
import java.util.concurrent.*

// ✅ Use Kotlin stdlib or multiplatform libraries
// For UUID: Use a multiplatform UUID library
// For collections: Use Kotlin stdlib collections
// For concurrency: Use kotlinx.coroutines
```

#### File I/O and System Operations
```kotlin
// ❌ JVM-specific
import java.io.File
import java.nio.file.Files
System.getProperty("user.home")

// ✅ Multiplatform file handling
import okio.FileSystem
import okio.Path.Companion.toPath
// Or use expect/actual declarations for platform-specific file operations
```

## Recommended Multiplatform Dependencies

### Essential Libraries
```kotlin
// Date/Time
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

// JSON Serialization  
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Networking
implementation("io.ktor:ktor-client-core:2.3.6")
// Platform-specific engines:
implementation("io.ktor:ktor-client-okhttp:2.3.6") // Android
implementation("io.ktor:ktor-client-darwin:2.3.6") // iOS

// File I/O
implementation("com.squareup.okio:okio:3.9.0")

// Database
implementation("app.cash.sqldelight:runtime:2.0.2")
// Platform-specific drivers:
implementation("app.cash.sqldelight:android-driver:2.0.2") // Android  
implementation("app.cash.sqldelight:native-driver:2.0.2") // iOS

// Settings/Preferences
implementation("com.russhwolf:multiplatform-settings:1.1.1")

// Dependency Injection
implementation("io.insert-koin:koin-core:3.5.6")
```

### Platform-Specific Dependencies
```kotlin
// In commonMain - shared interfaces
expect class PlatformSpecificClass()
expect fun platformSpecificFunction(): String

// In androidMain - Android implementation
actual class PlatformSpecificClass actual constructor() {
    // Android-specific implementation
}
actual fun platformSpecificFunction(): String = "Android"

// In iosMain - iOS implementation  
actual class PlatformSpecificClass actual constructor() {
    // iOS-specific implementation
}
actual fun platformSpecificFunction(): String = "iOS"
```

## Common Compilation Errors and Fixes

### "Unresolved reference: System"
```kotlin
// ❌ Problem
val time = System.currentTimeMillis()

// ✅ Solution
import kotlinx.datetime.Clock
val time = Clock.System.now().toEpochMilliseconds()
```

### "Unresolved reference: format"
```kotlin
// ❌ Problem  
val text = "%.1f★".format(rating)

// ✅ Solution
val rounded = kotlin.math.round(rating * 10) / 10.0
val text = "$rounded★"
```

### "Expected class/interface/object"
This usually means you're using a platform-specific class in commonMain.
```kotlin
// ❌ Problem - using JVM Date in commonMain
import java.util.Date
val date = Date()

// ✅ Solution - use kotlinx-datetime
import kotlinx.datetime.Clock
val instant = Clock.System.now()
```

## Best Practices

### 1. Dependency Organization
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Only multiplatform libraries here
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
        }
        
        androidMain.dependencies {
            // Android-specific implementations
            implementation("io.ktor:ktor-client-okhttp:2.3.6")
        }
        
        iosMain.dependencies {
            // iOS-specific implementations  
            implementation("io.ktor:ktor-client-darwin:2.3.6")
        }
    }
}
```

### 2. Testing Multiplatform Code
```kotlin
// In commonTest
class MultiplatformTest {
    @Test
    fun testCommonLogic() {
        val result = Clock.System.now()
        assertNotNull(result)
    }
}
```

### 3. Gradual Migration
When converting existing Android code to KMM:
1. Start with data models and business logic
2. Replace platform-specific APIs incrementally  
3. Use expect/actual for truly platform-specific needs
4. Test on both platforms frequently

### 4. IDE Configuration
- Use Android Studio with KMM plugin for best experience
- Enable "Type-safe project accessors" in settings.gradle.kts
- Configure proper source set recognition

## Troubleshooting

### Build Fails on iOS but Works on Android
1. Check for JVM-specific imports in commonMain
2. Verify all dependencies are multiplatform-compatible
3. Look for `System.*`, `java.*`, or `android.*` usage

### "Could not infer iOS target architectures"
1. Ensure you're building the framework first: `./gradlew linkDebugFrameworkIosSimulatorArm64`
2. Use proper Xcode integration (avoid direct embedAndSign tasks)
3. Check iOS deployment target compatibility

### Performance Issues
1. iOS builds are typically slower - use configuration cache
2. Enable Gradle parallel builds in gradle.properties
3. Use `--no-daemon` for CI builds to avoid memory issues

## iOS-Specific Integration Issues

### Koin Dependency Injection Initialization
When integrating Koin DI in an iOS KMM project, the initialization must happen **before** any Compose components try to inject dependencies.

#### ❌ Problem: iOS App Crash on Startup
```
Fatal error: [Koin] No definition found for class:'SearchViewModel' and qualifier:'null'
```

**Root Cause**: The App() composable tries to inject dependencies via Koin before Koin is initialized.

#### ✅ Solution: Initialize Koin in SwiftUI
```swift
// In iOS ContentView.swift
import SwiftUI
import ComposeApp

struct ContentView: View {
    init() {
        // Initialize Koin DI before any Compose UI is created
        IOSKoinInitKt.doInitKoin()
    }
    
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
```

**Key Points**:
- Swift uses `doInitKoin()` not `initKoin()` - Kotlin/Native function naming conventions
- Must be called in `init()` of the SwiftUI view that hosts the Compose content
- Ensures Koin is ready before any `@Composable` functions execute

#### Function Name Resolution
Kotlin functions exported to iOS follow specific naming conventions:
```kotlin
// Kotlin: initKoin()
// Swift: doInitKoin() or IOSKoinInitKt.doInitKoin()

// Top-level functions are often prefixed with "do" in Swift
// Check generated framework headers in Xcode for exact names
```

### iOS UIKit Interoperability
When using iOS UIKit components in Compose Multiplatform:

#### ExperimentalForeignApi Annotation Required
```kotlin
// In iosMain source sets
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppIcon.Render(size: Dp) {
    UIKitView<UIImageView>(
        factory = {
            val config = UIImageSymbolConfiguration
                .configurationWithPointSize(size.value.toDouble())
            // ...
        }
    )
}
```

**Why Needed**: Kotlin/Native's C interop APIs are experimental and require explicit opt-in.

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [kotlinx.datetime](https://github.com/Kotlin/kotlinx-datetime)
- [Ktor Client](https://ktor.io/docs/getting-started-ktor-client.html)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Koin for KMM](https://insert-koin.io/docs/setup/multiplatform/)