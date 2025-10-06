#!/bin/bash

# Update iOS app version from version.properties
# This script is called as a build phase in Xcode

set -e

# Path to version.properties (one level up from iosApp)
VERSION_PROPS="../version.properties"

if [ ! -f "$VERSION_PROPS" ]; then
    echo "error: version.properties not found at $VERSION_PROPS"
    exit 1
fi

# Read version from properties file
VERSION_NAME=$(grep 'VERSION_NAME=' "$VERSION_PROPS" | cut -d'=' -f2)
VERSION_CODE=$(grep 'VERSION_CODE=' "$VERSION_PROPS" | cut -d'=' -f2)

if [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
    echo "error: Could not read version from $VERSION_PROPS"
    exit 1
fi

# Update Info.plist using PlistBuddy
PLIST="${BUILT_PRODUCTS_DIR}/${INFOPLIST_PATH}"

if [ -f "$PLIST" ]; then
    /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $VERSION_NAME" "$PLIST"
    /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $VERSION_CODE" "$PLIST"
    echo "Updated iOS version to $VERSION_NAME ($VERSION_CODE)"
else
    echo "warning: Info.plist not found at $PLIST, version not updated"
fi
