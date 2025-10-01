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
	@echo "  build-all               - Build both Android and iOS debug builds"
	@echo "  run-all                 - Build, install, and run on both Android emulator and iOS simulator"
else
	@echo "  run-ios-simulator       - [macOS only] Build, install, and run on iOS simulator"
	@echo "  build-all               - [macOS only] Build both Android and iOS debug builds"
	@echo "  run-all                 - [macOS only] Build, install, and run on both platforms"
endif
	@echo "  run-android-remote-emu  - [Linux only] Build and run on remote Android emulator"
	@echo "  run-remote-all          - [Linux only] Build and run on both remote iOS and Android"
	@echo "  run-ios-remotesim       - [Linux only] Build and run on remote iOS simulator"
	@echo ""
	@echo "LOG COMMANDS:"
	@echo "  logs-help               - Show detailed log reading help"
	@echo "  logs-android-search     - Show Android search logs"
	@echo "  logs-ios-search         - Show iOS search logs"
	@echo "  logs-android-data       - Show Android data import logs"
	@echo "  logs-ios-data           - Show iOS data import logs"
	@echo "  logs-android-crash      - Show Android crash/error logs"
	@echo "  logs-ios-crash          - Show iOS crash/error logs"
	@echo "  logs-follow-android CONCEPT='search' - Follow Android logs live"
	@echo "  logs-follow-ios CONCEPT='di'         - Follow iOS logs live"
ifeq ($(UNAME_S),Linux)
	@echo "  logs-remote-android     - [Linux only] View Android logs on remote device"
	@echo "  logs-remote-ios         - [Linux only] View iOS logs on remote device"
	@echo "  logs-remote-data        - [Linux only] Check data import logs on both platforms"
else
	@echo "  logs-remote-android     - [Linux only] View Android logs on remote device"
	@echo "  logs-remote-ios         - [Linux only] View iOS logs on remote device"
	@echo "  logs-remote-data        - [Linux only] Check data import logs on both platforms"
endif
	@echo ""
	@echo "DATABASE COMMANDS:"
	@echo "  db-android              - Extract Android database and open in DB Browser"
	@echo "  db-ios                  - Extract iOS database and open in DB Browser"
	@echo "  clean-db                - Delete databases on both platforms (force schema recreation)"
	@echo "  clean-db-android        - Delete Android database only"
	@echo "  clean-db-ios            - Delete iOS database only"
	@echo ""
	@echo "CACHE COMMANDS:"
	@echo "  clean-cache             - Delete app caches on both platforms"
	@echo "  clean-cache-android     - Delete Android app cache only"
	@echo "  clean-cache-ios         - Delete iOS app cache only"
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

# Linux-only commands for remote development
ifeq ($(UNAME_S),Linux)
.PHONY: run-ios-remotesim
run-ios-remotesim:
	@echo "🍎 Building and running iOS app on remote simulator..."
	$(GRADLEW) iosRemoteRunSimulator
	@echo "✅ iOS app launched on remote simulator successfully!"

.PHONY: run-android-remote-emu
run-android-remote-emu:
	@echo "🤖 Building and running Android app on remote emulator..."
	$(GRADLEW) androidRemoteRunEmulator
	@echo "✅ Android app launched on remote emulator successfully!"

.PHONY: run-remote-all
run-remote-all:
	@echo "🚀 Building and running app on both remote iOS and Android..."
	@echo ""
	@echo "📱 Step 1/2: Running iOS simulator..."
	$(MAKE) run-ios-remotesim
	@echo ""
	@echo "🤖 Step 2/2: Running Android emulator..."
	$(MAKE) run-android-remote-emu
	@echo ""
	@echo "✅ App successfully launched on both remote iOS and Android platforms!"
endif

# =============================================================================
# LOCAL BUILD ALL / RUN ALL COMMANDS (macOS only)
# =============================================================================

ifeq ($(UNAME_S),Darwin)
.PHONY: build-all
build-all:
	@echo "🔨 Building both Android and iOS debug builds..."
	@echo ""
	@echo "🤖 Step 1/2: Building Android debug APK..."
	$(MAKE) build-debug-android
	@echo ""
	@echo "🍎 Step 2/2: Building iOS debug framework..."
	$(MAKE) build-debug-ios
	@echo ""
	@echo "✅ Both Android and iOS debug builds completed successfully!"

.PHONY: run-all
run-all:
	@echo "🚀 Building and running app on both Android emulator and iOS simulator..."
	@echo ""
	@echo "🤖 Step 1/2: Running Android emulator..."
	$(MAKE) run-android-emulator
	@echo ""
	@echo "🍎 Step 2/2: Running iOS simulator..."
	$(MAKE) run-ios-simulator
	@echo ""
	@echo "✅ App successfully launched on both Android emulator and iOS simulator!"
else
.PHONY: build-all
build-all:
	@echo "❌ build-all requires macOS for iOS builds"
	@echo "💡 Use 'make build-debug-android' for Android-only builds"

.PHONY: run-all
run-all:
	@echo "❌ run-all requires macOS for iOS builds"
	@echo "💡 Use 'make run-android-emulator' for Android-only testing"
	@echo "💡 Use 'make run-remote-all' for remote iOS testing from Linux"
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

.PHONY: logs-android-data
logs-android-data:
	@./scripts/readlogs -a -d data

.PHONY: logs-ios-data
logs-ios-data:
	@./scripts/readlogs -i -d data

.PHONY: logs-android-crash
logs-android-crash:
	@./scripts/readlogs -a -e $(CONCEPT)

.PHONY: logs-ios-crash
logs-ios-crash:
	@./scripts/readlogs -i -e $(CONCEPT)

# Linux-only remote logging commands
ifeq ($(UNAME_S),Linux)
.PHONY: logs-remote-android
logs-remote-android:
	@./scripts/remote-logs android $(or $(CONCEPT),all)

.PHONY: logs-remote-ios
logs-remote-ios:
	@./scripts/remote-logs ios $(or $(CONCEPT),all)

.PHONY: logs-remote-data
logs-remote-data:
	@echo "🔍 Checking remote data import logs on both platforms..."
	@echo ""
	@echo "📱 Step 1/2: iOS data logs..."
	@./scripts/remote-logs ios data
	@echo ""
	@echo "🤖 Step 2/2: Android data logs..."
	@./scripts/remote-logs android data
endif

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

# =============================================================================
# DATABASE COMMANDS
# =============================================================================

.PHONY: db-android
db-android:
	@echo "🗄️ Extracting Android database..."
	./scripts/get-db.sh android

.PHONY: db-ios
db-ios:
	@echo "🗄️ Extracting iOS database..."
	./scripts/get-db.sh ios

.PHONY: clean-db
clean-db:
	@echo "🧹 Cleaning databases on both platforms..."
	./scripts/clean-db.sh all

.PHONY: clean-db-android
clean-db-android:
	@echo "🧹 Cleaning Android database..."
	./scripts/clean-db.sh android

.PHONY: clean-db-ios
clean-db-ios:
	@echo "🧹 Cleaning iOS database..."
	./scripts/clean-db.sh ios

# =============================================================================
# CACHE COMMANDS
# =============================================================================

.PHONY: clean-cache
clean-cache:
	@echo "🧹 Cleaning app caches on both platforms..."
	./scripts/clean-cache.sh all

.PHONY: clean-cache-android
clean-cache-android:
	@echo "🧹 Cleaning Android app cache..."
	./scripts/clean-cache.sh android

.PHONY: clean-cache-ios
clean-cache-ios:
	@echo "🧹 Cleaning iOS app cache..."
	./scripts/clean-cache.sh ios