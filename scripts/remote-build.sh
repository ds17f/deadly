#!/bin/bash

# remote-build - Execute build commands on remote Mac and optionally download artifacts
# Usage: remote-build [build-type]

set -euo pipefail

# Get script directory and source shared config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/remote-config.sh"

# Parse arguments
if [ $# -eq 0 ]; then
    echo "Usage: remote-build [build-type]"
    echo ""
    echo "Build types:"
    echo "  debug-android"
    echo "  release-android"
    echo "  release-signed-android"
    echo "  bundle-android"
    echo "  debug-ios"
    echo "  release-ios"
    echo "  all"
    exit 1
fi

BUILD_TYPE="$1"

# Validate build type
VALID_TYPES=("debug-android" "release-android" "release-signed-android" "bundle-android" "debug-ios" "release-ios" "all")
if [[ ! " ${VALID_TYPES[@]} " =~ " ${BUILD_TYPE} " ]]; then
    echo "❌ Error: Invalid build type: $BUILD_TYPE"
    echo "Valid types: ${VALID_TYPES[*]}"
    exit 1
fi

echo "🔨 Building $BUILD_TYPE on remote Mac..."
echo ""

# Sync code to remote
sync_to_remote

# Execute build on remote
echo "📱 Executing build on remote Mac..."
exec_remote "make build-$BUILD_TYPE"

# Download artifacts based on build type
echo ""
echo "📥 Downloading build artifacts..."

case "$BUILD_TYPE" in
    debug-android)
        mkdir -p composeApp/build/outputs/apk/debug
        if ssh "$REMOTE_HOST" "test -d $REMOTE_DIR/composeApp/build/outputs/apk/debug"; then
            download_dir_from_remote "composeApp/build/outputs/apk/debug" "composeApp/build/outputs/apk/debug"
            echo "✅ Downloaded debug APK"
            echo "📍 Location: composeApp/build/outputs/apk/debug/"
        fi
        ;;
    release-android|release-signed-android)
        mkdir -p composeApp/build/outputs/apk/release
        if ssh "$REMOTE_HOST" "test -d $REMOTE_DIR/composeApp/build/outputs/apk/release"; then
            download_dir_from_remote "composeApp/build/outputs/apk/release" "composeApp/build/outputs/apk/release"
            echo "✅ Downloaded release APK"
            echo "📍 Location: composeApp/build/outputs/apk/release/"
        fi
        ;;
    bundle-android)
        mkdir -p composeApp/build/outputs/bundle/release
        if ssh "$REMOTE_HOST" "test -d $REMOTE_DIR/composeApp/build/outputs/bundle/release"; then
            download_dir_from_remote "composeApp/build/outputs/bundle/release" "composeApp/build/outputs/bundle/release"
            echo "✅ Downloaded release AAB"
            echo "📍 Location: composeApp/build/outputs/bundle/release/"
        fi
        ;;
    debug-ios)
        mkdir -p composeApp/build/bin
        if ssh "$REMOTE_HOST" "test -d $REMOTE_DIR/composeApp/build/bin"; then
            download_dir_from_remote "composeApp/build/bin" "composeApp/build/bin"
            echo "✅ Downloaded iOS debug framework"
            echo "📍 Location: composeApp/build/bin/"
        fi
        ;;
    release-ios)
        mkdir -p iosApp/build
        if ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/iosApp/build/Deadly.ipa"; then
            download_from_remote "iosApp/build/Deadly.ipa" "iosApp/build/Deadly.ipa"
            echo "✅ Downloaded iOS release IPA"
            echo "📍 Location: iosApp/build/Deadly.ipa"
        fi
        ;;
    all)
        echo "ℹ️  'all' builds don't produce downloadable artifacts"
        echo "   Use specific build types to download artifacts"
        ;;
esac

echo ""
echo "✅ Remote build completed successfully!"
