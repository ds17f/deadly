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
	@echo "  build-release-signed-android - Build signed Android release APK"
	@echo "  build-bundle-android    - Build signed Android App Bundle (AAB)"
	@echo "  deploy-playstore-internal - Build and upload AAB to Play Store Internal Testing"
ifeq ($(UNAME_S),Darwin)
	@echo "  build-debug-ios         - Build iOS debug framework"
	@echo "  build-release-ios       - Build signed iOS release IPA"
	@echo "  deploy-testflight       - Build and upload iOS release to TestFlight"
else
	@echo "  build-debug-ios         - [macOS only] Build iOS debug framework"
	@echo "  build-release-ios       - [macOS only] Build signed iOS release IPA"
	@echo "  release-ios-testflight  - [macOS only] Build and upload to TestFlight"
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
	@echo "  run-android-device-signed - Build signed release, install, and run on device"
ifeq ($(UNAME_S),Darwin)
	@echo "  run-ios-simulator       - Build, install, and run on iOS simulator"
	@echo "  run-ios-device          - Build, install, and run on connected iOS device"
	@echo "  build-all               - Build both Android and iOS debug builds"
	@echo "  run-all                 - Build, install, and run on both Android emulator and iOS simulator"
else
	@echo "  run-ios-simulator       - [macOS only] Build, install, and run on iOS simulator"
	@echo "  run-ios-device          - [macOS only] Build, install, and run on iOS device"
	@echo "  build-all               - [macOS only] Build both Android and iOS debug builds"
	@echo "  run-all                 - [macOS only] Build, install, and run on both platforms"
endif
	@echo ""
	@echo "REMOTE COMMANDS (Linux only - execute on remote Mac):"
	@echo "  remote-run-ios-simulator       - Build and run iOS on remote simulator"
	@echo "  remote-run-android-emulator    - Build and run Android on remote emulator"
	@echo "  remote-run-all                 - Build and run on both remote platforms"
	@echo "  remote-run-ios-device          - Run on iOS device connected to remote Mac"
	@echo "  remote-run-android-device      - Run on Android device connected to remote Mac"
	@echo "  remote-run-android-device-signed - Run signed build on remote Android device"
	@echo "  remote-build-debug-android     - Build Android debug APK on remote Mac"
	@echo "  remote-build-release-android   - Build Android release APK on remote Mac"
	@echo "  remote-build-release-signed-android - Build signed release APK on remote Mac"
	@echo "  remote-build-bundle-android    - Build Android App Bundle on remote Mac"
	@echo "  remote-build-debug-ios         - Build iOS debug framework on remote Mac"
	@echo "  remote-build-release-ios       - Build iOS release IPA on remote Mac"
	@echo "  remote-build-all               - Build both platforms on remote Mac"
	@echo "  remote-deploy-testing-android  - Upload to Play Store Internal Testing from remote"
	@echo "  remote-deploy-testing-ios      - Upload to TestFlight from remote"
	@echo "  remote-db-android              - Extract Android DB from remote and download"
	@echo "  remote-db-ios                  - Extract iOS DB from remote and download"
	@echo "  remote-clean-db-android        - Clean Android DB on remote device"
	@echo "  remote-clean-db-ios            - Clean iOS DB on remote device"
	@echo "  remote-clean-db                - Clean DBs on both remote platforms"
	@echo "  remote-clean-cache-android     - Clean Android cache on remote device"
	@echo "  remote-clean-cache-ios         - Clean iOS cache on remote device"
	@echo "  remote-clean-cache             - Clean cache on both remote platforms"
	@echo "  remote-install-android-device  - Install to Android device on remote Mac"
	@echo "  remote-install-ios-device      - Install to iOS device on remote Mac"
	@echo "  remote-logs-android            - View Android logs from remote device"
	@echo "  remote-logs-ios                - View iOS logs from remote device"
	@echo "  remote-logs-data               - View data import logs from both remote platforms"
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
	@echo "RELEASE COMMANDS:"
	@echo "  release                 - Auto-determine version from commits and create release"
	@echo "  release-version VERSION=x.y.z - Create release with specific version"
	@echo "  release-dry-run         - Preview what release would do without making changes"
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

.PHONY: build-release-signed-android
build-release-signed-android:
	@echo "🤖 Building signed Android release APK..."
	@if [ ! -f .secrets/keystore.properties ]; then \
		echo "❌ Error: .secrets/keystore.properties not found"; \
		echo "💡 Please configure .secrets/keystore.properties with your keystore details"; \
		exit 1; \
	fi
	$(GRADLEW) :composeApp:assembleRelease
	@echo "✅ Signed Android release APK built successfully!"
	@echo "📱 APK location: composeApp/build/outputs/apk/release/"

.PHONY: build-bundle-android
build-bundle-android:
	@echo "🤖 Building signed Android App Bundle (AAB)..."
	@if [ ! -f .secrets/keystore.properties ]; then \
		echo "❌ Error: .secrets/keystore.properties not found"; \
		echo "💡 Please configure .secrets/keystore.properties with your keystore details"; \
		exit 1; \
	fi
	$(GRADLEW) :composeApp:bundleRelease
	@echo "✅ Signed Android App Bundle built successfully!"
	@echo "📦 AAB location: composeApp/build/outputs/bundle/release/"

ifeq ($(UNAME_S),Darwin)
.PHONY: build-debug-ios
build-debug-ios:
	@echo "🍎 Building iOS debug framework..."
	@echo "🔍 Detected architecture: $(UNAME_M)"
	$(GRADLEW) :composeApp:linkDebugFrameworkIosSimulatorArm64
	@echo "✅ iOS debug framework built successfully!"

.PHONY: build-release-ios
build-release-ios:
	@echo "🍎 Building iOS release IPA..."
	@if ! command -v fastlane >/dev/null 2>&1; then \
		echo "❌ fastlane not found"; \
		echo "💡 Install with: brew install fastlane"; \
		exit 1; \
	fi
	@echo "🔍 Building KMM framework for device..."
	$(GRADLEW) :composeApp:linkReleaseFrameworkIosArm64
	@echo "📦 Building and signing release IPA with fastlane..."
	cd iosApp && fastlane build_release
	@echo "✅ iOS release IPA built successfully!"
	@echo "📱 IPA location: iosApp/build/Deadly.ipa"

.PHONY: deploy-testing-ios
deploy-testing-ios:
	@echo "🍎 Building and uploading to TestFlight..."
	@if ! command -v fastlane >/dev/null 2>&1; then \
		echo "❌ fastlane not found"; \
		echo "💡 Install with: brew install fastlane"; \
		exit 1; \
	fi
	@echo "🔍 Building KMM framework for device..."
	$(GRADLEW) :composeApp:linkReleaseFrameworkIosArm64
	@echo "📦 Building, signing, and uploading to TestFlight..."
	cd iosApp && fastlane deploy_testflight
	@echo "✅ iOS app uploaded to TestFlight successfully!"

# Aliases for backwards compatibility
.PHONY: deploy-testflight
deploy-testflight: deploy-testing-ios

.PHONY: release-ios-testflight
release-ios-testflight: deploy-testing-ios
endif

# =============================================================================
# PLAY STORE DEPLOYMENT
# =============================================================================

.PHONY: deploy-testing-android
deploy-testing-android:
	@echo "🤖 Building and uploading to Play Store Internal Testing..."
	@if ! command -v fastlane >/dev/null 2>&1; then \
		echo "❌ fastlane not found"; \
		echo "💡 Install with: gem install fastlane"; \
		exit 1; \
	fi
	@if [ ! -f .secrets/thedeadly-app-f48493c2a133.json ]; then \
		echo "❌ Error: Play Store service account key not found"; \
		echo "💡 Please ensure .secrets/thedeadly-app-f48493c2a133.json exists"; \
		exit 1; \
	fi
	@echo "📦 Building and uploading to Play Store..."
	cd android && fastlane deploy_testing
	@echo "✅ Android AAB uploaded to Play Store Internal Testing - available to testers immediately!"

# Alias for backwards compatibility
.PHONY: deploy-playstore-internal
deploy-playstore-internal: deploy-testing-android

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
install-ios-device:
	@echo "🍎 Installing iOS app to connected device..."
	@echo "💡 Note: This requires proper iOS development setup (certificates, provisioning profiles)"
	@echo "🔍 Building framework for device..."
	$(GRADLEW) :composeApp:linkDebugFrameworkIosArm64
	@echo "🔍 Building app for device..."
	@DEVICE_ID=$$(xcrun xctrace list devices 2>&1 | grep "iPhone" | grep -v "Simulator" | head -1 | grep -o '[0-9A-F]\{8\}-[0-9A-F]\{16\}' | head -1); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "❌ No iOS device detected"; \
		echo "💡 Please connect an iOS device and ensure it's trusted"; \
		echo "💡 Available devices:"; \
		xcrun xctrace list devices 2>&1 | grep "iPhone" | grep -v "Simulator"; \
		exit 1; \
	fi; \
	echo "📱 Found device ID: $$DEVICE_ID"; \
	cd iosApp && xcodebuild \
		-project iosApp.xcodeproj \
		-scheme iosApp \
		-configuration Debug \
		-destination "id=$$DEVICE_ID" \
		-derivedDataPath ./build \
		CODE_SIGN_STYLE=Manual \
		clean build; \
	echo "📱 Installing app to device..."; \
	APP_PATH=$$(find ./build/Build/Products -name "Deadly.app" -type d | head -1); \
	if [ -z "$$APP_PATH" ]; then \
		echo "❌ App bundle not found"; \
		echo "Searched in: ./build/Build/Products"; \
		exit 1; \
	fi; \
	echo "📦 Found app at: $$APP_PATH"; \
	xcrun devicectl device install app --device $$DEVICE_ID "$$APP_PATH"
	@echo "✅ iOS app installed to device successfully!"
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

.PHONY: run-android-device-signed
run-android-device-signed: build-release-signed-android
	@echo "🤖 Installing signed Android app to connected device..."
	@echo "🔍 Checking for connected device..."
	@DEVICE_COUNT=$$(adb devices | grep -c "device$$"); \
	if [ $$DEVICE_COUNT -eq 0 ]; then \
		echo "❌ No Android device detected"; \
		echo "💡 Please connect an Android device with USB debugging enabled"; \
		exit 1; \
	else \
		echo "✅ Found $$DEVICE_COUNT connected device(s)"; \
	fi
	@echo "📱 Installing signed release APK..."
	@APK_PATH=$$(find composeApp/build/outputs/apk/release -name "*.apk" | head -1); \
	if [ -z "$$APK_PATH" ]; then \
		echo "❌ No APK found in composeApp/build/outputs/apk/release/"; \
		exit 1; \
	fi; \
	adb install -r "$$APK_PATH"
	@echo "🚀 Launching Android app on device..."
	@if adb shell am start -n "com.grateful.deadly/com.grateful.deadly.MainActivity"; then \
		echo "✅ Signed Android app launched on device successfully!"; \
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

.PHONY: run-ios-device
run-ios-device: install-ios-device
	@echo "🚀 Launching iOS app on device..."
	@DEVICE_ID=$$(xcrun xctrace list devices 2>&1 | grep "iPhone" | grep -v "Simulator" | head -1 | grep -o '[0-9A-F]\{8\}-[0-9A-F]\{16\}' | head -1); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "⚠️  Could not detect device for launch"; \
		echo "💡 Please manually tap the app icon on your device"; \
	else \
		echo "📱 Killing existing app instance if running..."; \
		xcrun devicectl device process kill --device $$DEVICE_ID com.grateful.deadly.Deadly 2>/dev/null || true; \
		sleep 1; \
		echo "🚀 Launching app on device..."; \
		xcrun devicectl device process launch --device $$DEVICE_ID com.grateful.deadly.Deadly 2>/dev/null || \
		(echo "💡 Note: Auto-launch may require developer mode enabled on device"; \
		echo "💡 Please manually tap the Deadly app icon on your device"); \
	fi
	@echo "✅ iOS app deployment complete!"
endif

# Linux-only commands for remote development
ifeq ($(UNAME_S),Linux)
.PHONY: remote-run-ios-simulator
remote-run-ios-simulator:
	@echo "🍎 Building and running iOS app on remote simulator..."
	$(GRADLEW) iosRemoteRunSimulator
	@echo "✅ iOS app launched on remote simulator successfully!"

.PHONY: remote-run-android-emulator
remote-run-android-emulator:
	@echo "🤖 Building and running Android app on remote emulator..."
	$(GRADLEW) androidRemoteRunEmulator
	@echo "✅ Android app launched on remote emulator successfully!"

.PHONY: remote-run-all
remote-run-all:
	@echo "🚀 Building and running app on both remote iOS and Android..."
	@echo ""
	@echo "📱 Step 1/2: Running iOS simulator..."
	$(MAKE) remote-run-ios-simulator
	@echo ""
	@echo "🤖 Step 2/2: Running Android emulator..."
	$(MAKE) remote-run-android-emulator
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
.PHONY: remote-logs-android
remote-logs-android:
	@./scripts/remote-logs android $(or $(CONCEPT),all)

.PHONY: remote-logs-ios
remote-logs-ios:
	@./scripts/remote-logs ios $(or $(CONCEPT),all)

.PHONY: remote-logs-data
remote-logs-data:
	@echo "🔍 Checking remote data import logs on both platforms..."
	@echo ""
	@echo "📱 Step 1/2: iOS data logs..."
	@./scripts/remote-logs ios data
	@echo ""
	@echo "🤖 Step 2/2: Android data logs..."
	@./scripts/remote-logs android data

# =============================================================================
# REMOTE BUILD COMMANDS
# =============================================================================

.PHONY: remote-build-debug-android
remote-build-debug-android:
	@./scripts/remote-build.sh debug-android

.PHONY: remote-build-release-android
remote-build-release-android:
	@./scripts/remote-build.sh release-android

.PHONY: remote-build-release-signed-android
remote-build-release-signed-android:
	@./scripts/remote-build.sh release-signed-android

.PHONY: remote-build-bundle-android
remote-build-bundle-android:
	@./scripts/remote-build.sh bundle-android

.PHONY: remote-build-debug-ios
remote-build-debug-ios:
	@./scripts/remote-build.sh debug-ios

.PHONY: remote-build-release-ios
remote-build-release-ios:
	@./scripts/remote-build.sh release-ios

.PHONY: remote-build-all
remote-build-all:
	@./scripts/remote-build.sh all

# =============================================================================
# REMOTE DEPLOY COMMANDS
# =============================================================================

.PHONY: remote-deploy-testing-android
remote-deploy-testing-android:
	@./scripts/remote-deploy.sh testing-android

.PHONY: remote-deploy-testing-ios
remote-deploy-testing-ios:
	@./scripts/remote-deploy.sh testing-ios

# =============================================================================
# REMOTE DATABASE COMMANDS
# =============================================================================

.PHONY: remote-db-android
remote-db-android:
	@./scripts/remote-db.sh android

.PHONY: remote-db-ios
remote-db-ios:
	@./scripts/remote-db.sh ios

.PHONY: remote-clean-db-android
remote-clean-db-android:
	@./scripts/remote-clean-db.sh android

.PHONY: remote-clean-db-ios
remote-clean-db-ios:
	@./scripts/remote-clean-db.sh ios

.PHONY: remote-clean-db
remote-clean-db:
	@./scripts/remote-clean-db.sh all

# =============================================================================
# REMOTE CACHE COMMANDS
# =============================================================================

.PHONY: remote-clean-cache-android
remote-clean-cache-android:
	@./scripts/remote-cache.sh android

.PHONY: remote-clean-cache-ios
remote-clean-cache-ios:
	@./scripts/remote-cache.sh ios

.PHONY: remote-clean-cache
remote-clean-cache:
	@./scripts/remote-cache.sh all

# =============================================================================
# REMOTE DEVICE COMMANDS
# =============================================================================

.PHONY: remote-install-android-device
remote-install-android-device:
	@./scripts/remote-install.sh android-device

.PHONY: remote-install-ios-device
remote-install-ios-device:
	@./scripts/remote-install.sh ios-device

.PHONY: remote-run-android-device
remote-run-android-device:
	@./scripts/remote-run.sh android-device

.PHONY: remote-run-android-device-signed
remote-run-android-device-signed:
	@./scripts/remote-run.sh android-device-signed

.PHONY: remote-run-ios-device
remote-run-ios-device:
	@./scripts/remote-run.sh ios-device
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

# =============================================================================
# RELEASE COMMANDS
# =============================================================================

.PHONY: release
release:
	@echo "🚀 Creating release with automatic versioning..."
	@if [ ! -f scripts/release.sh ]; then \
		echo "❌ Error: scripts/release.sh not found"; \
		exit 1; \
	fi
	@./scripts/release.sh
	@echo "✅ Release created successfully!"
	@echo "💡 GitHub Actions will now build and publish the release"

.PHONY: release-version
release-version:
	@echo "🚀 Creating release with version $(VERSION)..."
	@if [ -z "$(VERSION)" ]; then \
		echo "❌ Error: VERSION not specified"; \
		echo "💡 Usage: make release-version VERSION=1.2.3"; \
		exit 1; \
	fi
	@if [ ! -f scripts/release.sh ]; then \
		echo "❌ Error: scripts/release.sh not found"; \
		exit 1; \
	fi
	@./scripts/release.sh $(VERSION)
	@echo "✅ Release $(VERSION) created successfully!"
	@echo "💡 GitHub Actions will now build and publish the release"

.PHONY: release-dry-run
release-dry-run:
	@echo "🧪 Running release dry run..."
	@if [ ! -f scripts/release.sh ]; then \
		echo "❌ Error: scripts/release.sh not found"; \
		exit 1; \
	fi
	@./scripts/release.sh --dry-run