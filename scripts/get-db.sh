#!/bin/bash

# Script to extract SQLite database from Android or iOS for inspection
# Usage: ./scripts/get-db.sh [android|ios-simulator|ios-device] [--no-open]

set -e

# Configuration
APP_BUNDLE_ID="com.grateful.deadly"
APP_PACKAGE_NAME="com.grateful.deadly"
DB_NAME="deadly.db"
DEFAULT_OUTPUT="./sqliteDbs"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "❌ Platform required"
    echo "Usage: ./scripts/get-db.sh <android|ios-simulator|ios-device> [--no-open]"
    exit 1
fi

PLATFORM="$1"
OPEN_DB=true

# Check for --no-open flag
if [ $# -gt 1 ] && [ "$2" = "--no-open" ]; then
    OPEN_DB=false
fi

# Support legacy "ios" argument (defaults to simulator)
if [ "$PLATFORM" = "ios" ]; then
    PLATFORM="ios-simulator"
fi

if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios-simulator" ] && [ "$PLATFORM" != "ios-device" ]; then
    echo "❌ Invalid platform: $PLATFORM"
    echo "Usage: ./scripts/get-db.sh <android|ios-simulator|ios-device> [--no-open]"
    exit 1
fi

OUTPUT_DIR="$DEFAULT_OUTPUT/$PLATFORM"

echo "🔍 Extracting latest $PLATFORM database..."

# Create output directory and clean any existing files to ensure we get the latest
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR/$DB_NAME" "$OUTPUT_DIR/${DB_NAME}-wal" "$OUTPUT_DIR/${DB_NAME}-shm" 2>/dev/null || true
echo "🧹 Cleared old database files to ensure fresh extraction"

if [ "$PLATFORM" = "android" ]; then
    # Android database extraction
    echo "📱 Extracting Android database..."

    # Check if adb is available
    if ! command -v adb >/dev/null 2>&1; then
        echo "❌ adb not found. Please install Android SDK platform tools"
        exit 1
    fi

    # Check if device/emulator is connected
    DEVICE_COUNT=$(adb devices | grep -v "List of devices attached" | grep -c "device$" || echo "0")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "❌ No Android device/emulator connected"
        echo "Please start an Android emulator or connect a device"
        exit 1
    fi

    echo "📂 Found Android device/emulator"

    # Extract database files from Android using run-as for proper permissions
    DB_REMOTE_PATH="databases/$DB_NAME"

    # Copy database files to accessible location and pull them
    TEMP_DIR="/data/local/tmp"

    # Copy main database file
    if adb shell "run-as $APP_PACKAGE_NAME test -f $DB_REMOTE_PATH" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME cat $DB_REMOTE_PATH > $TEMP_DIR/$DB_NAME"
        adb pull "$TEMP_DIR/$DB_NAME" "$OUTPUT_DIR/" && echo "✅ Copied main database: $DB_NAME"
        adb shell "rm $TEMP_DIR/$DB_NAME" 2>/dev/null || true
    else
        echo "❌ Database file not found at: $DB_REMOTE_PATH"
        echo "Make sure the app has created the database"
        exit 1
    fi

    # Copy WAL file if exists
    if adb shell "run-as $APP_PACKAGE_NAME test -f ${DB_REMOTE_PATH}-wal" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME cat ${DB_REMOTE_PATH}-wal > $TEMP_DIR/${DB_NAME}-wal"
        adb pull "$TEMP_DIR/${DB_NAME}-wal" "$OUTPUT_DIR/" && echo "✅ Copied WAL file: ${DB_NAME}-wal"
        adb shell "rm $TEMP_DIR/${DB_NAME}-wal" 2>/dev/null || true
    fi

    # Copy SHM file if exists
    if adb shell "run-as $APP_PACKAGE_NAME test -f ${DB_REMOTE_PATH}-shm" 2>/dev/null; then
        adb shell "run-as $APP_PACKAGE_NAME cat ${DB_REMOTE_PATH}-shm > $TEMP_DIR/${DB_NAME}-shm"
        adb pull "$TEMP_DIR/${DB_NAME}-shm" "$OUTPUT_DIR/" && echo "✅ Copied SHM file: ${DB_NAME}-shm"
        adb shell "rm $TEMP_DIR/${DB_NAME}-shm" 2>/dev/null || true
    fi

elif [ "$PLATFORM" = "ios-simulator" ]; then
    # iOS Simulator database extraction
    echo "📱 Extracting iOS Simulator database..."

    # Find the running simulator
    DEVICE_UUID=$(xcrun simctl list devices | grep "(Booted)" | head -1 | grep -o '[A-F0-9-]\{36\}')

    if [ -z "$DEVICE_UUID" ]; then
        echo "❌ No booted iOS Simulator found"
        echo "Please start the iOS Simulator first"
        exit 1
    fi

    echo "📂 Found booted simulator: $DEVICE_UUID"

    # Find the app's data directory
    APP_DATA_DIR=$(xcrun simctl get_app_container "$DEVICE_UUID" "$APP_BUNDLE_ID" data 2>/dev/null || echo "")

    if [ -z "$APP_DATA_DIR" ]; then
        echo "❌ App not installed or data directory not found"
        echo "Please make sure the app is installed on the simulator"
        exit 1
    fi

    echo "📂 App data directory: $APP_DATA_DIR"

    # The database is in Library/Application Support/databases/
    DB_PATH="$APP_DATA_DIR/Library/Application Support/databases/$DB_NAME"

    if [ ! -f "$DB_PATH" ]; then
        echo "❌ Database file not found at: $DB_PATH"
        echo "Expected location: Library/Application Support/databases/$DB_NAME"
        echo ""
        echo "Available files in app directory:"
        find "$APP_DATA_DIR" -name "*.db" -o -name "*.sqlite" -o -name "*.sqlite3" 2>/dev/null | head -10
        exit 1
    fi

    # Copy database files (including WAL and SHM files if they exist)
    echo "📋 Copying database files..."

    cp "$DB_PATH" "$OUTPUT_DIR/" && echo "✅ Copied main database: $DB_NAME"

    # Copy WAL file if exists
    if [ -f "${DB_PATH}-wal" ]; then
        cp "${DB_PATH}-wal" "$OUTPUT_DIR/" && echo "✅ Copied WAL file: ${DB_NAME}-wal"
    fi

    # Copy SHM file if exists
    if [ -f "${DB_PATH}-shm" ]; then
        cp "${DB_PATH}-shm" "$OUTPUT_DIR/" && echo "✅ Copied SHM file: ${DB_NAME}-shm"
    fi

elif [ "$PLATFORM" = "ios-device" ]; then
    # iOS Physical Device database extraction using xcrun devicectl
    echo "📱 Extracting iOS Device database..."

    # Check if xcrun and devicectl are available
    if ! command -v xcrun >/dev/null 2>&1; then
        echo "❌ Xcode command line tools not found"
        echo "Install with: xcode-select --install"
        exit 1
    fi

    # Get connected device using devicectl
    DEVICE_INFO=$(xcrun devicectl list devices 2>/dev/null | grep "available (paired)" | head -1)

    if [ -z "$DEVICE_INFO" ]; then
        echo "❌ No paired iOS device found"
        echo "Please connect and trust an iOS device via USB"
        exit 1
    fi

    # Extract device identifier (3rd column)
    DEVICE_ID=$(echo "$DEVICE_INFO" | awk '{print $3}')
    DEVICE_NAME=$(echo "$DEVICE_INFO" | awk '{print $1}')

    echo "📂 Found device: $DEVICE_NAME ($DEVICE_ID)"

    # Copy database files from device using devicectl
    echo "📋 Copying database files from device..."

    DB_SOURCE="Library/Application Support/databases/$DB_NAME"

    # Copy main database file
    if xcrun devicectl device copy from \
        --device "$DEVICE_ID" \
        --source "$DB_SOURCE" \
        --destination "$OUTPUT_DIR/$DB_NAME" \
        --domain-type appDataContainer \
        --domain-identifier "$APP_BUNDLE_ID" \
        --quiet 2>/dev/null; then
        echo "✅ Copied main database: $DB_NAME"
    else
        echo "❌ Database file not found at: $DB_SOURCE"
        echo ""
        echo "Make sure:"
        echo "  1. The app is installed on the device"
        echo "  2. The app has been launched and created data"
        echo "  3. The device is unlocked and trusted"
        exit 1
    fi

    # Copy WAL file if exists
    if xcrun devicectl device copy from \
        --device "$DEVICE_ID" \
        --source "${DB_SOURCE}-wal" \
        --destination "$OUTPUT_DIR/${DB_NAME}-wal" \
        --domain-type appDataContainer \
        --domain-identifier "$APP_BUNDLE_ID" \
        --quiet 2>/dev/null; then
        echo "✅ Copied WAL file: ${DB_NAME}-wal"
    fi

    # Copy SHM file if exists
    if xcrun devicectl device copy from \
        --device "$DEVICE_ID" \
        --source "${DB_SOURCE}-shm" \
        --destination "$OUTPUT_DIR/${DB_NAME}-shm" \
        --domain-type appDataContainer \
        --domain-identifier "$APP_BUNDLE_ID" \
        --quiet 2>/dev/null; then
        echo "✅ Copied SHM file: ${DB_NAME}-shm"
    fi
fi

# Get database info
DB_SIZE=$(ls -lh "$OUTPUT_DIR/$DB_NAME" | awk '{print $5}')
echo ""
echo "🎉 Database extracted successfully!"
echo "📍 Location: $OUTPUT_DIR/$DB_NAME"
echo "📏 Size: $DB_SIZE"

# Show some basic info about the database
if command -v sqlite3 >/dev/null 2>&1; then
    echo ""
    echo "📊 Database tables:"
    sqlite3 "$OUTPUT_DIR/$DB_NAME" ".tables" | tr ' ' '\n' | sort

    echo ""
    echo "📈 Row counts:"
    sqlite3 "$OUTPUT_DIR/$DB_NAME" "
        SELECT 'Show', COUNT(*) FROM Show
        UNION ALL
        SELECT 'Recording', COUNT(*) FROM Recording
        ORDER BY 1;
    " 2>/dev/null || echo "Could not get row counts (tables may not exist yet)"
else
    echo ""
    echo "💡 Install sqlite3 to see database info: brew install sqlite"
fi

if [ "$OPEN_DB" = true ]; then
    echo ""
    echo "🔧 Opening with DB Browser for SQLite..."

    # Try to open with DB Browser for SQLite
    if [ -d "/Applications/DB Browser for SQLite.app" ]; then
        open -a "DB Browser for SQLite" "$OUTPUT_DIR/$DB_NAME"
        echo "✅ Opened $PLATFORM database in DB Browser for SQLite"
    elif command -v sqlitebrowser >/dev/null 2>&1; then
        sqlitebrowser "$OUTPUT_DIR/$DB_NAME" &
        echo "✅ Opened $PLATFORM database in DB Browser for SQLite"
    else
        echo "❌ DB Browser for SQLite not found"
        echo ""
        echo "🔧 Alternative ways to open:"
        echo "   - Install DB Browser: https://sqlitebrowser.org/"
        echo "   - TablePlus: https://tableplus.com/"
        echo "   - Command line: sqlite3 '$OUTPUT_DIR/$DB_NAME'"
    fi
fi