#!/bin/bash

# Script to delete SQLite database from Android or iOS to force schema recreation
# Usage: ./scripts/clean-db.sh [android|ios|all]

set -e

# Configuration
APP_BUNDLE_ID="com.grateful.deadly"
APP_PACKAGE_NAME="com.grateful.deadly"
DB_NAME="deadly.db"

# Parse arguments
PLATFORM="$1"

if [ $# -eq 0 ]; then
    PLATFORM="all"
fi

if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios" ] && [ "$PLATFORM" != "all" ]; then
    echo "❌ Invalid platform: $PLATFORM"
    echo "Usage: ./scripts/clean-db.sh <android|ios|all>"
    exit 1
fi

clean_android_db() {
    echo "🧹 Cleaning Android database..."

    # Check if adb is available
    if ! command -v adb >/dev/null 2>&1; then
        echo "⚠️  adb not found. Skipping Android database cleanup"
        return 1
    fi

    # Check if device/emulator is connected
    DEVICE_COUNT=$(adb devices | grep -v "List of devices attached" | grep -c "device$" || echo "0")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "⚠️  No Android device/emulator connected. Skipping Android database cleanup"
        return 1
    fi

    echo "📱 Found Android device/emulator"

    # Database paths
    DB_REMOTE_PATH="databases/$DB_NAME"

    # Remove database files using run-as for proper permissions
    echo "🗑️  Removing database files..."

    # Remove main database file
    if adb shell "run-as $APP_PACKAGE_NAME test -f $DB_REMOTE_PATH" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME rm $DB_REMOTE_PATH" && echo "✅ Deleted main database: $DB_NAME"
    else
        echo "ℹ️  Main database file not found (already clean)"
    fi

    # Remove WAL file if exists
    if adb shell "run-as $APP_PACKAGE_NAME test -f ${DB_REMOTE_PATH}-wal" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME rm ${DB_REMOTE_PATH}-wal" && echo "✅ Deleted WAL file: ${DB_NAME}-wal"
    fi

    # Remove SHM file if exists
    if adb shell "run-as $APP_PACKAGE_NAME test -f ${DB_REMOTE_PATH}-shm" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME rm ${DB_REMOTE_PATH}-shm" && echo "✅ Deleted SHM file: ${DB_NAME}-shm"
    fi

    echo "✅ Android database cleanup completed!"
}

clean_ios_db() {
    echo "🧹 Cleaning iOS database..."

    # Check if we're on macOS
    if [ "$(uname)" != "Darwin" ]; then
        echo "⚠️  iOS database cleanup only available on macOS. Skipping..."
        return 1
    fi

    # Find the running simulator
    DEVICE_UUID=$(xcrun simctl list devices | grep "(Booted)" | head -1 | grep -o '[A-F0-9-]\{36\}' || echo "")

    if [ -z "$DEVICE_UUID" ]; then
        echo "⚠️  No booted iOS Simulator found. Skipping iOS database cleanup"
        return 1
    fi

    echo "📱 Found booted simulator: $DEVICE_UUID"

    # Find the app's data directory
    APP_DATA_DIR=$(xcrun simctl get_app_container "$DEVICE_UUID" "$APP_BUNDLE_ID" data 2>/dev/null || echo "")

    if [ -z "$APP_DATA_DIR" ]; then
        echo "⚠️  App not installed or data directory not found. Skipping iOS database cleanup"
        return 1
    fi

    echo "📂 App data directory: $APP_DATA_DIR"

    # The database is in Library/Application Support/databases/
    DB_PATH="$APP_DATA_DIR/Library/Application Support/databases/$DB_NAME"

    echo "🗑️  Removing database files..."

    # Remove main database file
    if [ -f "$DB_PATH" ]; then
        rm "$DB_PATH" && echo "✅ Deleted main database: $DB_NAME"
    else
        echo "ℹ️  Main database file not found (already clean)"
    fi

    # Remove WAL file if exists
    if [ -f "${DB_PATH}-wal" ]; then
        rm "${DB_PATH}-wal" && echo "✅ Deleted WAL file: ${DB_NAME}-wal"
    fi

    # Remove SHM file if exists
    if [ -f "${DB_PATH}-shm" ]; then
        rm "${DB_PATH}-shm" && echo "✅ Deleted SHM file: ${DB_NAME}-shm"
    fi

    echo "✅ iOS database cleanup completed!"
}

echo "🗄️ Cleaning database files to force schema recreation..."
echo ""

if [ "$PLATFORM" = "android" ] || [ "$PLATFORM" = "all" ]; then
    clean_android_db || true
fi

if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
    echo ""
    clean_ios_db || true
fi

echo ""
echo "🎉 Database cleanup completed!"
echo ""
echo "💡 Next steps:"
echo "   1. Restart the app to recreate database with new schema"
echo "   2. Run data sync to re-import show data"
echo "   3. Test new features (like RecentShows functionality)"