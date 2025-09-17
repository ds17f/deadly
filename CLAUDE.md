# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform Mobile (KMM)** project called "Deadly" that targets Android and iOS platforms using Compose Multiplatform for shared UI. The project uses Gradle with version catalogs and follows the standard KMM structure.

**Key Details:**
- Package: `com.grateful.deadly`
- Framework name: `ComposeApp` (for iOS)
- Bundle ID: `com.grateful.deadly.Deadly` (iOS app gets project name suffix)
- Target platforms: Android + iOS (arm64 + simulator)

## Build Commands

**Use the Makefile for all build operations** - it handles cross-platform compatibility and provides clean output with proper error handling:

```bash
# See all available commands
make help

# Local Android development
make build-debug-android       # Build debug APK
make install-android-device    # Install to connected Android device
make install-android-emulator  # Install to Android emulator
make run-android-emulator      # Build, install, and run on Android emulator
make run-android-device        # Build, install, and run on Android device

# Local iOS development (macOS only)
make build-debug-ios           # Build iOS debug framework
make install-ios-simulator     # Install to iOS simulator
make run-ios-simulator         # Build, install, and run on iOS simulator

# Remote development (Linux only - for testing on remote Mac)
make run-ios-remotesim         # Build and run iOS on remote Mac simulator
make run-android-remote-emu    # Build and run Android on remote Mac emulator
make run-remote-all           # Build and run on both remote iOS and Android

# Utilities
make clean                    # Clean all build artifacts
```

**Raw Gradle commands** (if needed):
```bash
./gradlew :composeApp:assembleDebug              # Android debug APK
./gradlew :composeApp:installDebug               # Install to connected Android device
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # iOS simulator framework
./gradlew :composeApp:linkDebugFrameworkIosArm64           # iOS device framework
```

## Architecture

**Multiplatform Source Structure:**
- `composeApp/src/commonMain/` - Shared Kotlin code and Compose UI
- `composeApp/src/androidMain/` - Android-specific implementations  
- `composeApp/src/iosMain/` - iOS-specific implementations
- `composeApp/src/commonTest/` - Shared test code

**Platform Integration:**
- **Android**: Entry point in `MainActivity.kt`, standard Android app structure
- **iOS**: Xcode project in `iosApp/` with SwiftUI wrapper, Kotlin framework integration via `MainViewController.kt`

**Key Architecture Points:**
- `Platform.kt` interface with platform-specific implementations for OS detection
- `Greeting.kt` demonstrates shared business logic
- `App.kt` contains the main Compose UI that runs on both platforms
- iOS app depends on the `ComposeApp.framework` built by Gradle
- Static framework configuration for iOS to avoid dynamic linking issues

## Development Workflow

**Adding Platform-Specific Code:**
- Add to appropriate `*Main` source set
- Use expect/actual declarations for platform-specific APIs
- iOS-specific Swift code goes in `iosApp/iosApp/`

**Build Artifacts:**
- Android APKs: `composeApp/build/outputs/apk/`
- iOS framework: `composeApp/build/bin/`
- iOS app: Built via Xcode to `iosApp/build/`

**Debugging:**
- Makefile logs all build noise to `logs/` directory
- Use `--info --stacktrace` with Gradle for detailed error information
- Check `logs/emulator.log`, `logs/gradle-install.log` for Android issues

## KMM Compatibility Guidelines

**⚠️ Critical**: Avoid platform-specific APIs in `commonMain` source sets.

**Quick Reference:**
- ❌ `System.currentTimeMillis()` → ✅ `Clock.System.now()` (kotlinx-datetime)
- ❌ `String.format()` → ✅ `kotlin.math.round()` + string templates  
- ❌ `java.util.*` → ✅ `kotlinx.*` libraries
- ❌ `Date()`, `Calendar` → ✅ `kotlinx-datetime` types

**For comprehensive compatibility guidance, troubleshooting, and examples:**
📖 **See [docs/KMM_COMPATIBILITY.md](docs/KMM_COMPATIBILITY.md)**

## Current Implementation Status

### ✅ Completed Features
- **Cross-platform Navigation System**: Expect/actual pattern with `DeadlyNavHost` abstraction
- **AppScaffold with TopBar/BottomBar**: Feature-colocated bar configurations following V2 patterns
- **SearchScreen**: Complete search interface with QR scanner, browse sections, and discovery
- **SearchResultsScreen**: Full search results UI with V2 patterns (TopBar, sections, cards, pin indicators)
- **Cross-platform AppIcon System**: Material Symbols (Android) + SF Symbols (iOS)
- **Remote Development Workflow**: Linux→Mac remote builds for both iOS and Android
- **5-Tab Bottom Navigation**: Home, Search, Library, Collections, Settings (with placeholder screens)

### 🔧 Architecture Highlights
- **expect/actual Navigation**: `DeadlyNavHost` abstracts platform differences
- **Feature-colocated Configuration**: Each feature defines its own bar configuration
- **Shared ViewModel Pattern**: SearchViewModel works across SearchScreen and SearchResultsScreen
- **V2 Design Patterns**: Pin indicators, LibraryV2-style cards, integrated search headers
- **Cross-platform Icon System**: Unified `AppIcon.Render()` API for both platforms

### 🚀 Development Workflow
- **Local Development**: Standard Android/iOS development on respective platforms
- **Remote Testing**: Complete Linux→Mac workflow for testing both platforms remotely
- **Cross-platform Builds**: Single codebase builds and runs on Android and iOS