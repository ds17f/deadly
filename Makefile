# Makefile for Kotlin Multiplatform Mobile (KMM) project
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
	@echo "Available targets:"
	@echo "  android-debug   - Build Android debug APK"
	@echo "  android-release - Build Android release APK"
	@echo "  android-device  - Build and install Android app to connected device"
	@echo "  android-run     - Build and run Android app in emulator"
ifeq ($(UNAME_S),Darwin)
	@echo "  ios-sim         - Build and launch iOS app in simulator"
	@echo "  ios-device      - Build iOS framework for connected device"
else
	@echo "  ios-sim         - [macOS only] Build and launch iOS app in simulator"
	@echo "  ios-device      - [macOS only] Build iOS framework for connected device"
endif
	@echo "  clean           - Clean all build outputs"
	@echo "  all             - Build Android debug and iOS simulator (if on macOS)"
	@echo "  help            - Show this help message"

# Android targets (work on all platforms)
.PHONY: android-debug
android-debug:
	@echo "🤖 Building Android debug APK..."
	$(GRADLEW) :composeApp:assembleDebug
	@echo "✅ Android debug APK built successfully!"
	@echo "📱 APK location: composeApp/build/outputs/apk/debug/"

.PHONY: android-release
android-release:
	@echo "🤖 Building Android release APK..."
	$(GRADLEW) :composeApp:assembleRelease
	@echo "✅ Android release APK built successfully!"
	@echo "📱 APK location: composeApp/build/outputs/apk/release/"

.PHONY: android-device
android-device:
	@echo "🤖 Building and installing Android app to connected device..."
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
	@echo "🚀 Launching app on device..."
	@if adb shell am start -n "com.grateful.deadly/com.grateful.deadly.MainActivity"; then \
		echo "✅ Android app installed and launched successfully!"; \
	else \
		echo "❌ Failed to launch Android app"; \
		exit 1; \
	fi

.PHONY: android-run
android-run:
	@echo "🤖 Building and installing Android app..."
	@echo "🔍 Checking for running emulator or connected device..."
	@DEVICE_COUNT=$$(adb devices | grep -c "device$$\|emulator"); \
	if [ $$DEVICE_COUNT -eq 0 ]; then \
		echo "⚠️  No Android device or emulator detected"; \
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
		echo "✅ Found $$DEVICE_COUNT connected device(s)"; \
	fi
	@echo "📱 Installing app... (logs: logs/gradle-install.log)"
	@mkdir -p logs
	$(GRADLEW) :composeApp:installDebug > logs/gradle-install.log 2>&1 || (echo "❌ Install failed - check logs/gradle-install.log"; exit 1)
	@echo "🚀 Launching app on device/emulator..."
	@if adb shell am start -n "com.grateful.deadly/com.grateful.deadly.MainActivity"; then \
		echo "✅ Android app launched successfully!"; \
	else \
		echo "❌ Failed to launch Android app"; \
		exit 1; \
	fi

# iOS targets (macOS only)
ifeq ($(UNAME_S),Darwin)
.PHONY: ios-sim
ios-sim:
	@echo "🍎 Building Compose framework for iOS simulator..."
	@echo "🔍 Detected architecture: $(UNAME_M)"
	@echo "🔨 Building iOS simulator framework..."
	$(GRADLEW) :composeApp:linkDebugFrameworkIosSimulatorArm64
	@echo "🚀 Building iOS app for simulator..."
	@SIMULATOR=$$(xcrun simctl list devices | grep "iPhone" | grep -v "iPad" | head -1 | sed 's/.*iPhone \([^(]*\).*/iPhone \1/' | sed 's/ *$$//' || echo "iPhone 16"); \
	echo "📱 Using iPhone simulator: $$SIMULATOR"; \
	mkdir -p iosApp/build; \
	cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination "platform=iOS Simulator,name=$$SIMULATOR" -configuration Debug -derivedDataPath ./build build
	@echo "📱 Opening iPhone Simulator and launching app..."
	@SIMULATOR=$$(xcrun simctl list devices | grep "iPhone" | grep -v "iPad" | head -1 | sed 's/.*iPhone \([^(]*\).*/iPhone \1/' | sed 's/ *$$//' || echo "iPhone 16"); \
	xcrun simctl boot "$$SIMULATOR" 2>/dev/null || true; \
	open -a Simulator; \
	sleep 3; \
	APP_PATH=$$(find iosApp/build -name "*.app" -type d | head -1); \
	if [ -n "$$APP_PATH" ] && [ -d "$$APP_PATH" ]; then \
		echo "📱 Installing app: $$APP_PATH"; \
		xcrun simctl install booted "$$APP_PATH" && \
		BUNDLE_ID=$$(/usr/libexec/PlistBuddy -c "Print CFBundleIdentifier" "$$APP_PATH/Info.plist"); \
		echo "🚀 Launching app with bundle ID: $$BUNDLE_ID"; \
		xcrun simctl launch booted "$$BUNDLE_ID" && \
		echo "✅ iOS simulator build and launch completed!"; \
	else \
		echo "❌ App build not found - check Xcode build output"; \
		find iosApp/build -name "*.app" -type d 2>/dev/null || echo "No .app files found"; \
		exit 1; \
	fi

.PHONY: ios-device
ios-device:
	@echo "🍎 Building Compose framework for iOS device..."
	@echo "🔨 Building iOS device framework..."
	$(GRADLEW) :composeApp:linkDebugFrameworkIosArm64
	@echo "📱 Building iOS app for device..."
	cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS' -configuration Debug build
	@echo "✅ iOS device build completed!"
	@echo "📋 Note: Use Xcode to deploy to your connected device"

else
# Non-macOS iOS targets
.PHONY: ios-sim ios-device
ios-sim ios-device:
	@echo "⚠️  iOS builds are only supported on macOS"
	@echo "💡 Current OS: $(UNAME_S)"
	@echo "🍎 Please run this command on a Mac to build for iOS"
endif

# Clean target (works on all platforms)
.PHONY: clean
clean:
	@echo "🧹 Cleaning all build outputs..."
	$(GRADLEW) clean
	@echo "✅ Clean completed!"

# Build all target
.PHONY: all
all: android-run
ifeq ($(UNAME_S),Darwin)
	@$(MAKE) ios-sim
else
	@echo "ℹ️  Skipping iOS build (not on macOS)"
endif
	@echo "🎉 All builds completed!"

# Utility targets for development
.PHONY: tasks
tasks:
	@echo "📋 Available Gradle tasks:"
	$(GRADLEW) tasks

.PHONY: dependencies
dependencies:
	@echo "📦 Project dependencies:"
	$(GRADLEW) :composeApp:dependencies

# Debugging targets
.PHONY: gradle-version
gradle-version:
	@echo "🔧 Gradle version:"
	$(GRADLEW) --version

.PHONY: project-info
project-info:
	@echo "📊 Project information:"
	@echo "OS: $(UNAME_S)"
	@echo "Architecture: $(UNAME_M)"
	@echo "Gradle wrapper: $(GRADLEW)"
	$(GRADLEW) projects