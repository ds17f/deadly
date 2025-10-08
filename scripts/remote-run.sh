#!/bin/bash

# remote-run - Run app on physical device connected to remote Mac
# Usage: remote-run [android-device|ios-device|android-device-signed]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "Usage: remote-run [android-device|ios-device|android-device-signed]"
    echo ""
    echo "Examples:"
    echo "  remote-run android-device"
    echo "  remote-run ios-device"
    echo "  remote-run android-device-signed"
    exit 1
fi

RUN_TYPE="$1"

# Validate run type
if [ "$RUN_TYPE" != "android-device" ] && [ "$RUN_TYPE" != "ios-device" ] && [ "$RUN_TYPE" != "android-device-signed" ]; then
    echo "❌ Error: Run type must be 'android-device', 'ios-device', or 'android-device-signed'"
    exit 1
fi

echo "🚀 Running app on $RUN_TYPE connected to remote Mac..."
echo ""

# Sync code to remote
sync_to_remote

# For signed builds, sync secrets too
if [ "$RUN_TYPE" = "android-device-signed" ]; then
    echo "🔐 Syncing secrets for signed build..."
    if [ -d ".secrets" ]; then
        rsync -az .secrets/ "$REMOTE_HOST:$REMOTE_DIR/.secrets/"
        echo "✅ Secrets synced"
    else
        echo "⚠️  Warning: .secrets directory not found"
        echo "   Build will use secrets already on remote Mac (if any)"
    fi
    echo ""
fi

# Execute run on remote
echo "📱 Building, installing, and launching on remote Mac..."
exec_remote "make run-$RUN_TYPE"

echo ""
echo "✅ Remote run completed successfully!"
echo "💡 App should now be running on the device connected to remote Mac"
