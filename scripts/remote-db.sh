#!/bin/bash

# remote-db - Extract database from remote device and download to local machine
# Usage: remote-db [android|ios]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "Usage: remote-db [android|ios]"
    echo ""
    echo "Examples:"
    echo "  remote-db android"
    echo "  remote-db ios"
    exit 1
fi

PLATFORM="$1"

# Validate platform
if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios" ]; then
    echo "❌ Error: Platform must be 'android' or 'ios'"
    exit 1
fi

echo "🗄️  Extracting $PLATFORM database from remote device..."
echo ""

# Sync code to remote
sync_to_remote

# Execute db extraction on remote (without opening DB Browser remotely)
echo "📱 Extracting database on remote Mac..."
exec_remote "./scripts/get-db.sh $PLATFORM --no-open"

# Download database files from remote
echo "📥 Downloading database files to local machine..."
mkdir -p sqliteDbs/$PLATFORM

# Download main database file
if ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/sqliteDbs/$PLATFORM/deadly.db"; then
    download_from_remote "sqliteDbs/$PLATFORM/deadly.db" "sqliteDbs/$PLATFORM/deadly.db"
    echo "✅ Downloaded main database"
else
    echo "❌ Remote database file not found"
    exit 1
fi

# Download WAL file if exists
if ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/sqliteDbs/$PLATFORM/deadly.db-wal"; then
    download_from_remote "sqliteDbs/$PLATFORM/deadly.db-wal" "sqliteDbs/$PLATFORM/deadly.db-wal"
    echo "✅ Downloaded WAL file"
fi

# Download SHM file if exists
if ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/sqliteDbs/$PLATFORM/deadly.db-shm"; then
    download_from_remote "sqliteDbs/$PLATFORM/deadly.db-shm" "sqliteDbs/$PLATFORM/deadly.db-shm"
    echo "✅ Downloaded SHM file"
fi

echo ""
echo "🎉 Database downloaded successfully!"
echo "📍 Location: sqliteDbs/$PLATFORM/deadly.db"

# Get database size
DB_SIZE=$(ls -lh "sqliteDbs/$PLATFORM/deadly.db" | awk '{print $5}')
echo "📏 Size: $DB_SIZE"

# Show basic info if sqlite3 is available
if command -v sqlite3 >/dev/null 2>&1; then
    echo ""
    echo "📊 Database tables:"
    sqlite3 "sqliteDbs/$PLATFORM/deadly.db" ".tables" | tr ' ' '\n' | sort

    echo ""
    echo "📈 Row counts:"
    sqlite3 "sqliteDbs/$PLATFORM/deadly.db" "
        SELECT 'Show', COUNT(*) FROM Show
        UNION ALL
        SELECT 'Recording', COUNT(*) FROM Recording
        ORDER BY 1;
    " 2>/dev/null || echo "Could not get row counts"
fi

# Try to open with DB Browser
echo ""
if command -v sqlitebrowser >/dev/null 2>&1; then
    echo "🔧 Opening in DB Browser for SQLite..."
    sqlitebrowser "sqliteDbs/$PLATFORM/deadly.db" &
    echo "✅ Opened database in DB Browser"
else
    echo "💡 Install DB Browser for SQLite to view: https://sqlitebrowser.org/"
    echo "   Or use: sqlite3 sqliteDbs/$PLATFORM/deadly.db"
fi
