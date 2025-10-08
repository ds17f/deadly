#!/bin/bash

# remote-deploy - Execute deployment commands on remote Mac
# Usage: remote-deploy [deploy-type]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "Usage: remote-deploy [deploy-type]"
    echo ""
    echo "Deploy types:"
    echo "  testing-android  - Upload to Play Store Internal Testing"
    echo "  testing-ios      - Upload to TestFlight"
    exit 1
fi

DEPLOY_TYPE="$1"

# Validate deploy type
if [ "$DEPLOY_TYPE" != "testing-android" ] && [ "$DEPLOY_TYPE" != "testing-ios" ]; then
    echo "❌ Error: Invalid deploy type: $DEPLOY_TYPE"
    echo "Valid types: testing-android, testing-ios"
    exit 1
fi

echo "🚀 Deploying $DEPLOY_TYPE from remote Mac..."
echo ""

# Sync code to remote (includes any local changes)
sync_to_remote

# Sync secrets directory (contains signing keys, service account keys, etc.)
echo "🔐 Syncing secrets to remote host..."
if [ -d ".secrets" ]; then
    rsync -az .secrets/ "$REMOTE_HOST:$REMOTE_DIR/.secrets/"
    echo "✅ Secrets synced"
else
    echo "⚠️  Warning: .secrets directory not found locally"
    echo "   Deployment will use secrets already on remote Mac (if any)"
fi

# Execute deployment on remote
echo ""
echo "📱 Executing deployment on remote Mac..."
echo "⚠️  This may take several minutes..."
exec_remote "make deploy-$DEPLOY_TYPE"

echo ""
echo "✅ Remote deployment completed successfully!"

# Provide next steps based on deploy type
if [ "$DEPLOY_TYPE" = "testing-android" ]; then
    echo ""
    echo "🎉 App uploaded to Play Store Internal Testing!"
    echo "💡 Testers will receive update notification automatically"
    echo "📱 Check status: https://play.google.com/console"
elif [ "$DEPLOY_TYPE" = "testing-ios" ]; then
    echo ""
    echo "🎉 App uploaded to TestFlight!"
    echo "💡 Processing may take 5-10 minutes before testers can install"
    echo "📱 Check status: https://appstoreconnect.apple.com"
fi
