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

# Android development
make android-debug    # Build debug APK
make android-run      # Build, install, and run in emulator/device
make android-release  # Build release APK

# iOS development (macOS only)
make ios-sim         # Build and run in iPhone simulator
make ios-device      # Build for connected iOS device

# Utilities  
make clean          # Clean all build artifacts
make all           # Build and run on all available platforms
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