#!/bin/bash

# remote-clean-db - Clean database on remote device
# Usage: remote-clean-db [android|ios|all]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
PLATFORM="${1:-all}"

# Validate platform
if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios" ] && [ "$PLATFORM" != "all" ]; then
    echo "❌ Error: Platform must be 'android', 'ios', or 'all'"
    echo "Usage: remote-clean-db [android|ios|all]"
    exit 1
fi

echo "🧹 Cleaning $PLATFORM database on remote device..."
echo ""

# Sync code to remote
sync_to_remote

# Execute database cleanup on remote
echo "📱 Cleaning database on remote Mac..."
if [ "$PLATFORM" = "all" ]; then
    exec_remote "make clean-db"
else
    exec_remote "make clean-db-$PLATFORM"
fi

echo ""
echo "✅ Remote database cleanup completed!"
echo ""
echo "💡 Next steps:"
echo "   1. Run 'make remote-run-ios-simulator' or 'make remote-run-android-emulator'"
echo "   2. App will recreate database with new schema"
