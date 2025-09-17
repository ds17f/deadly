# Makefile for Kotlin Multiplatform Mobile (KMM) project
# Consistent naming pattern: action-platform-target
# Works on macOS, Linux, and Windows (with Java and Android SDK installed)

# Detect OS
UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)

# Define gradlew command based on OS
ifeq ($(OS),Windows_NT)
    GRADLEW = gradlew.bat
else
    GRADLEW = ./gradlew
endif

# Default target
.DEFAULT_GOAL := help

# Help target - shows available commands
.PHONY: help
help:
	@echo "KMM Build System - Consistent Command Pattern"
	@echo ""
	@echo "BUILD COMMANDS:"
	@echo "  build-debug-android     - Build Android debug APK"
	@echo "  build-release-android   - Build Android release APK"
ifeq ($(UNAME_S),Darwin)
	@echo "  build-debug-ios         - Build iOS debug framework"
	@echo "  build-release-ios       - Build iOS release framework"
else
	@echo "  build-debug-ios         - [macOS only] Build iOS debug framework"
	@echo "  build-release-ios       - [macOS only] Build iOS release framework"
endif
	@echo ""
	@echo "INSTALL COMMANDS:"
	@echo "  install-android-device  - Install to connected Android device"
	@echo "  install-android-emulator - Install to Android emulator (auto-start if needed)"
ifeq ($(UNAME_S),Darwin)
	@echo "  install-ios-simulator   - Install to iOS simulator"
	@echo "  install-ios-device      - Install to connected iOS device"
else
	@echo "  install-ios-simulator   - [macOS only] Install to iOS simulator"
	@echo "  install-ios-device      - [macOS only] Install to connected iOS device"
endif
	@echo ""
	@echo "RUN COMMANDS:"
	@echo "  run-android-emulator    - Build, install, and run on Android emulator"
	@echo "  run-android-device      - Build, install, and run on Android device"
ifeq ($(UNAME_S),Darwin)
	@echo "  run-ios-simulator       - Build, install, and run on iOS simulator"
else
	@echo "  run-ios-simulator       - [macOS only] Build, install, and run on iOS simulator"
	@echo "  run-ios-remotesim       - [Linux only] Build and run on remote iOS simulator"
endif
	@echo ""
	@echo "LOG COMMANDS:"
	@echo "  logs-help               - Show detailed log reading help"
	@echo "  logs-android-search     - Show Android search logs"
	@echo "  logs-ios-search         - Show iOS search logs"
	@echo "  logs-follow-android CONCEPT='search' - Follow Android logs live"
	@echo "  logs-follow-ios CONCEPT='di'         - Follow iOS logs live"
	@echo ""
	@echo "UTILITY COMMANDS:"
	@echo "  clean                   - Clean all build outputs"
	@echo "  clean-android          - Clean Android build outputs only"
	@echo "  clean-ios              - Clean iOS build outputs only"

# =============================================================================
# BUILD COMMANDS
# =============================================================================

.PHONY: build-debug-android
build-debug-android:
	@echo "🤖 Building Android debug APK..."
	$(GRADLEW) :composeApp:assembleDebug
	@echo "✅ Android debug APK built successfully!"
	@echo "📱 APK location: composeApp/build/outputs/apk/debug/"

.PHONY: build-release-android
build-release-android:
	@echo "🤖 Building Android release APK..."
	$(GRADLEW) :composeApp:assembleRelease
	@echo "✅ Android release APK built successfully!"
	@echo "📱 APK location: composeApp/build/outputs/apk/release/"

ifeq ($(UNAME_S),Darwin)
.PHONY: build-debug-ios
build-debug-ios:
	@echo "🍎 Building iOS debug framework..."
	@echo "🔍 Detected architecture: $(UNAME_M)"
	$(GRADLEW) :composeApp:linkDebugFrameworkIosSimulatorArm64
	@echo "✅ iOS debug framework built successfully!"

.PHONY: build-release-ios
build-release-ios:
	@echo "🍎 Building iOS release framework..."
	$(GRADLEW) :composeApp:linkReleaseFrameworkIosSimulatorArm64
	@echo "✅ iOS release framework built successfully!"
endif

# =============================================================================
# INSTALL COMMANDS  
# =============================================================================

.PHONY: install-android-device
install-android-device:
	@echo "🤖 Installing Android app to connected device..."
	@echo "🔍 Checking for connected device..."
	@DEVICE_COUNT=$$(adb devices | grep -c "device$$"); \
	if [ $$DEVICE_COUNT -eq 0 ]; then \
		echo "❌ No Android device detected"; \
		echo "💡 Please connect an Android device with USB debugging enabled"; \
		exit 1; \
	else \
		echo "✅ Found $$DEVICE_COUNT connected device(s)"; \
	fi
	@echo "📱 Installing app..."
	@mkdir -p logs
	$(GRADLEW) :composeApp:installDebug > logs/gradle-install.log 2>&1 || (echo "❌ Install failed - check logs/gradle-install.log"; exit 1)
	@echo "✅ Android app installed to device successfully!"

.PHONY: install-android-emulator
install-android-emulator:
	@echo "🤖 Installing Android app to emulator..."
	@echo "🔍 Checking for running emulator..."
	@EMULATOR_COUNT=$$(adb devices | grep -c "emulator"); \
	if [ $$EMULATOR_COUNT -eq 0 ]; then \
		echo "⚠️  No Android emulator detected"; \
		echo "💡 Starting Android emulator (if available)..."; \
		AVD=$$(emulator -list-avds | head -1); \
		if [ -n "$$AVD" ]; then \
			echo "🚀 Starting AVD: $$AVD"; \
			mkdir -p logs; \
			emulator -avd "$$AVD" -no-snapshot -no-boot-anim > logs/emulator.log 2>&1 & \
			echo "⏳ Waiting for emulator to boot (timeout: 2 minutes)... (logs: logs/emulator.log)"; \
			timeout 120 adb wait-for-device > logs/adb.log 2>&1 || (echo "❌ Emulator boot timeout - check logs/emulator.log"; exit 1); \
			echo "⏳ Waiting for emulator to be fully ready..."; \
			sleep 15; \
			adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1" || (echo "⚠️  Emulator may not be fully booted"); \
		else \
			echo "❌ No AVDs found - please create one in Android Studio"; \
			exit 1; \
		fi \
	else \
		echo "✅ Found $$EMULATOR_COUNT running emulator(s)"; \
	fi
	@echo "📱 Installing app... (logs: logs/gradle-install.log)"
	@mkdir -p logs
	$(GRADLEW) :composeApp:installDebug > logs/gradle-install.log 2>&1 || (echo "❌ Install failed - check logs/gradle-install.log"; exit 1)
	@echo "✅ Android app installed to emulator successfully!"

ifeq ($(UNAME_S),Darwin)
.PHONY: install-ios-simulator
install-ios-simulator: build-debug-ios
	@echo "🍎 Installing iOS app to simulator..."
	@SIMULATOR=$$(xcrun simctl list devices | grep "iPhone" | grep -v "iPad" | head -1 | sed 's/.*iPhone \([^(]*\).*/iPhone \1/' | sed 's/ *$$//' || echo "iPhone 16"); \
	echo "📱 Using iPhone simulator: $$SIMULATOR"; \
	mkdir -p iosApp/build; \
	cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination "platform=iOS Simulator,name=$$SIMULATOR" -configuration Debug -derivedDataPath ./build build
	@echo "📱 Installing app to simulator..."
	@xcrun simctl boot "$$(xcrun simctl list devices | grep "iPhone" | grep -v "iPad" | head -1 | sed 's/.*iPhone \([^(]*\).*/iPhone \1/' | sed 's/ *$$//' || echo "iPhone 16")" 2>/dev/null || true
	@APP_PATH=$$(find iosApp/build -name "*.app" -type d | head -1); \
	if [ -n "$$APP_PATH" ] && [ -d "$$APP_PATH" ]; then \
		echo "📱 Installing app: $$APP_PATH"; \
		xcrun simctl install booted "$$APP_PATH"; \
		echo "✅ iOS app installed to simulator successfully!"; \
	else \
		echo "❌ App build not found"; \
		exit 1; \
	fi

.PHONY: install-ios-device
install-ios-device: build-debug-ios
	@echo "🍎 Installing iOS app to connected device..."
	@echo "💡 Note: This requires proper iOS development setup (certificates, provisioning profiles)"
	@echo "🔍 Building for device..."
	$(GRADLEW) :composeApp:linkDebugFrameworkIosArm64
	@echo "✅ iOS device installation prepared!"
	@echo "💡 Use Xcode to install to device or configure automatic deployment"
endif

# =============================================================================
# RUN COMMANDS (Build + Install + Launch)
# =============================================================================

.PHONY: run-android-device
run-android-device: install-android-device
	@echo "🚀 Launching Android app on device..."
	@if adb shell am start -n "com.grateful.deadly/com.grateful.deadly.MainActivity"; then \
		echo "✅ Android app launched on device successfully!"; \
	else \
		echo "❌ Failed to launch Android app"; \
		exit 1; \
	fi

.PHONY: run-android-emulator
run-android-emulator: install-android-emulator
	@echo "🚀 Launching Android app on emulator..."
	@if adb shell am start -n "com.grateful.deadly/com.grateful.deadly.MainActivity"; then \
		echo "✅ Android app launched on emulator successfully!"; \
	else \
		echo "❌ Failed to launch Android app"; \
		exit 1; \
	fi

ifeq ($(UNAME_S),Darwin)
.PHONY: run-ios-simulator
run-ios-simulator: install-ios-simulator
	@echo "🚀 Launching iOS app on simulator..."
	@open -a Simulator
	@sleep 3
	@APP_PATH=$$(find iosApp/build -name "*.app" -type d | head -1); \
	BUNDLE_ID=$$(/usr/libexec/PlistBuddy -c "Print CFBundleIdentifier" "$$APP_PATH/Info.plist"); \
	echo "🚀 Launching app with bundle ID: $$BUNDLE_ID"; \
	xcrun simctl launch booted "$$BUNDLE_ID" && \
	echo "✅ iOS app launched on simulator successfully!"
endif

# Linux-only commands for remote iOS development
ifeq ($(UNAME_S),Linux)
.PHONY: run-ios-remotesim
run-ios-remotesim:
	@echo "🍎 Building and running iOS app on remote simulator..."
	$(GRADLEW) iosRemoteRunSimulator
	@echo "✅ iOS app launched on remote simulator successfully!"
endif

# =============================================================================
# LOG COMMANDS
# =============================================================================

.PHONY: logs-help
logs-help:
	@./scripts/readlogs -h

.PHONY: logs-android-search
logs-android-search:
	@./scripts/readlogs -a -d search

.PHONY: logs-ios-search
logs-ios-search:
	@./scripts/readlogs -i -d search

.PHONY: logs-follow-android
logs-follow-android:
	@./scripts/readlogs -a -f $(CONCEPT)

.PHONY: logs-follow-ios
logs-follow-ios:
	@./scripts/readlogs -i -f $(CONCEPT)

# =============================================================================
# CLEAN COMMANDS
# =============================================================================

.PHONY: clean
clean:
	@echo "🧹 Cleaning all build outputs..."
	$(GRADLEW) clean
	@rm -rf iosApp/build 2>/dev/null || true
	@rm -rf logs 2>/dev/null || true
	@echo "✅ Clean completed!"

.PHONY: clean-android
clean-android:
	@echo "🧹 Cleaning Android build outputs..."
	$(GRADLEW) :composeApp:clean
	@echo "✅ Android clean completed!"

.PHONY: clean-ios
clean-ios:
	@echo "🧹 Cleaning iOS build outputs..."
	@rm -rf iosApp/build 2>/dev/null || true
	@echo "✅ iOS clean completed!"

# =============================================================================
# UTILITY TARGETS
# =============================================================================

.PHONY: tasks
tasks:
	@echo "📋 Available Gradle tasks:"
	$(GRADLEW) tasks

.PHONY: dependencies
dependencies:
	@echo "📦 Project dependencies:"
	$(GRADLEW) :composeApp:dependencies

.PHONY: gradle-version
gradle-version:
	@echo "📋 Gradle version information:"
	$(GRADLEW) --version