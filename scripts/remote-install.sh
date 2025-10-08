#!/bin/bash

# remote-install - Install app to physical device connected to remote Mac
# Usage: remote-install [android-device|ios-device]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "Usage: remote-install [android-device|ios-device]"
    echo ""
    echo "Examples:"
    echo "  remote-install android-device"
    echo "  remote-install ios-device"
    exit 1
fi

DEVICE_TYPE="$1"

# Validate device type
if [ "$DEVICE_TYPE" != "android-device" ] && [ "$DEVICE_TYPE" != "ios-device" ]; then
    echo "❌ Error: Device type must be 'android-device' or 'ios-device'"
    exit 1
fi

echo "📱 Installing app to $DEVICE_TYPE connected to remote Mac..."
echo ""

# Sync code to remote
sync_to_remote

# Execute install on remote
echo "📱 Installing on remote Mac..."
exec_remote "make install-$DEVICE_TYPE"

echo ""
echo "✅ Remote install completed successfully!"
echo "💡 Device is connected to remote Mac - app should be installed"
