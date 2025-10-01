#!/bin/bash

# Script to delete app caches from Android or iOS
# Usage: ./scripts/clean-cache.sh [android|ios|all]

set -e

# Configuration
APP_BUNDLE_ID="com.grateful.deadly.Deadly"
APP_PACKAGE_NAME="com.grateful.deadly"

# Parse arguments
PLATFORM="$1"

if [ $# -eq 0 ]; then
    PLATFORM="all"
fi

if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios" ] && [ "$PLATFORM" != "all" ]; then
    echo "❌ Invalid platform: $PLATFORM"
    echo "Usage: ./scripts/clean-cache.sh <android|ios|all>"
    exit 1
fi

clean_android_cache() {
    echo "🧹 Cleaning Android cache..."

    # Check if adb is available
    if ! command -v adb >/dev/null 2>&1; then
        echo "⚠️  adb not found. Skipping Android cache cleanup"
        return 1
    fi

    # Check if device/emulator is connected
    DEVICE_COUNT=$(adb devices | grep -v "List of devices attached" | grep -c "device$" || echo "0")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "⚠️  No Android device/emulator connected. Skipping Android cache cleanup"
        return 1
    fi

    echo "📱 Found Android device/emulator"

    # Cache directory path
    CACHE_DIR="cache"

    echo "🗑️  Removing cache directory..."

    # Remove entire cache directory using run-as for proper permissions
    if adb shell "run-as $APP_PACKAGE_NAME test -d $CACHE_DIR" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME rm -rf $CACHE_DIR" && echo "✅ Deleted cache directory"
    else
        echo "ℹ️  Cache directory not found (already clean)"
    fi

    # Recreate cache directory
    adb shell "run-as $APP_PACKAGE_NAME mkdir -p $CACHE_DIR" && echo "✅ Recreated empty cache directory"

    echo "✅ Android cache cleanup completed!"
}

clean_ios_cache() {
    echo "🧹 Cleaning iOS cache..."

    # Check if we're on macOS
    if [ "$(uname)" != "Darwin" ]; then
        echo "⚠️  iOS cache cleanup only available on macOS. Skipping..."
        return 1
    fi

    # Find the running simulator
    DEVICE_UUID=$(xcrun simctl list devices | grep "(Booted)" | head -1 | grep -o '[A-F0-9-]\{36\}' || echo "")

    if [ -z "$DEVICE_UUID" ]; then
        echo "⚠️  No booted iOS Simulator found. Skipping iOS cache cleanup"
        return 1
    fi

    echo "📱 Found booted simulator: $DEVICE_UUID"

    # Find the app's data directory
    APP_DATA_DIR=$(xcrun simctl get_app_container "$DEVICE_UUID" "$APP_BUNDLE_ID" data 2>/dev/null || echo "")

    if [ -z "$APP_DATA_DIR" ]; then
        echo "⚠️  App not installed or data directory not found. Skipping iOS cache cleanup"
        return 1
    fi

    echo "📂 App data directory: $APP_DATA_DIR"

    # iOS cache is in Library/Caches/
    CACHE_PATH="$APP_DATA_DIR/Library/Caches"

    echo "🗑️  Removing cache directory..."

    # Remove cache directory
    if [ -d "$CACHE_PATH" ]; then
        rm -rf "$CACHE_PATH" && echo "✅ Deleted cache directory"
    else
        echo "ℹ️  Cache directory not found (already clean)"
    fi

    # Recreate cache directory
    mkdir -p "$CACHE_PATH" && echo "✅ Recreated empty cache directory"

    echo "✅ iOS cache cleanup completed!"
}

echo "🗑️  Cleaning app caches..."
echo ""

if [ "$PLATFORM" = "android" ] || [ "$PLATFORM" = "all" ]; then
    clean_android_cache || true
fi

if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
    echo ""
    clean_ios_cache || true
fi

echo ""
echo "🎉 Cache cleanup completed!"
echo ""
echo "💡 Next steps:"
echo "   1. Restart the app to test cache re-population"
echo "   2. Navigate between shows to verify prefetching works"
echo "   3. Check logs for 'Cache HIT' vs 'Cache MISS' messages"
