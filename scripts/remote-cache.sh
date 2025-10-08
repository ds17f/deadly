#!/bin/bash

# remote-cache - Clean app cache on remote device
# Usage: remote-cache [android|ios|all]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
PLATFORM="${1:-all}"

# Validate platform
if [ "$PLATFORM" != "android" ] && [ "$PLATFORM" != "ios" ] && [ "$PLATFORM" != "all" ]; then
    echo "❌ Error: Platform must be 'android', 'ios', or 'all'"
    echo "Usage: remote-cache [android|ios|all]"
    exit 1
fi

echo "🧹 Cleaning $PLATFORM cache on remote device..."
echo ""

# Sync code to remote
sync_to_remote

# Execute cache cleanup on remote
echo "📱 Cleaning cache on remote Mac..."
if [ "$PLATFORM" = "all" ]; then
    exec_remote "make clean-cache"
else
    exec_remote "make clean-cache-$PLATFORM"
fi

echo ""
echo "✅ Remote cache cleanup completed!"
