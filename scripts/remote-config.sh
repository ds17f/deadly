#!/bin/bash

# Shared configuration for remote development scripts
# Source this file in any script that needs to interact with the remote Mac

# Remote machine configuration
export REMOTE_HOST="dsilbergleithcu@worklaptop.local"
export REMOTE_DIR="~/AndroidStudioProjects/Deadly"

# Rsync options for syncing code to remote
# Uses --delete to keep remote in sync with local, but protects key directories:
#   .git              - Git metadata (prevents overwriting remote git config/remotes)
#   .secrets          - Signing keys and credentials (managed separately, lives on remote Mac)
#   .gradle           - Gradle cache (regenerated on remote)
#   build             - Build artifacts (regenerated on remote)
#   .idea             - IDE config (may differ between machines)
#   logs              - Log files (local-only)
export RSYNC_OPTS="-az --delete --exclude '.git' --exclude '.secrets' --exclude '.gradle' --exclude 'build' --exclude '.idea' --exclude 'logs'"

# Common function to sync code to remote
sync_to_remote() {
    echo "🔄 Syncing code to remote host..."
    rsync $RSYNC_OPTS ./ "$REMOTE_HOST:$REMOTE_DIR"
}

# Sync secrets from local to remote (optional, for when secrets are updated locally)
# Use with caution - only call when you want to push local secrets to remote
sync_secrets_to_remote() {
    echo "🔐 Syncing .secrets to remote host..."
    if [ -d ".secrets" ]; then
        rsync -az .secrets/ "$REMOTE_HOST:$REMOTE_DIR/.secrets/"
        echo "✅ Secrets synced to remote"
    else
        echo "⚠️  .secrets directory not found locally"
        return 1
    fi
}

# Pull secrets from remote to local (useful for getting remote Mac's signing keys)
# WARNING: This will overwrite local .secrets!
sync_secrets_from_remote() {
    echo "🔐 Pulling .secrets from remote host..."
    echo "⚠️  This will overwrite your local .secrets directory!"
    read -p "Continue? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rsync -az "$REMOTE_HOST:$REMOTE_DIR/.secrets/" .secrets/
        echo "✅ Secrets pulled from remote"
    else
        echo "❌ Cancelled"
        return 1
    fi
}

# Common function to execute remote command
exec_remote() {
    local cmd="$1"
    ssh "$REMOTE_HOST" "zsh -l -c 'cd $REMOTE_DIR && $cmd'"
}

# Common function to execute remote command with Android SDK
exec_remote_android() {
    local cmd="$1"
    ssh "$REMOTE_HOST" "zsh -l -c 'export ANDROID_HOME=~/Library/Android/sdk && cd $REMOTE_DIR && $cmd'"
}

# Common function to execute remote command for iOS builds with keychain unlock
# iOS code signing requires keychain access which can fail in SSH sessions
exec_remote_ios() {
    local cmd="$1"
    echo "🔐 iOS code signing requires keychain access..."
    echo -n "Enter password for $REMOTE_HOST to unlock keychain: "
    read -s KEYCHAIN_PASSWORD
    echo ""  # New line after hidden password input

    ssh "$REMOTE_HOST" "zsh -l -c '
        cd $REMOTE_DIR
        echo \"🔓 Unlocking keychain...\"
        security unlock-keychain -p \"$KEYCHAIN_PASSWORD\" ~/Library/Keychains/login.keychain-db 2>/dev/null || \
        security unlock-keychain -p \"$KEYCHAIN_PASSWORD\" ~/Library/Keychains/login.keychain 2>/dev/null || \
        echo \"⚠️  Keychain unlock failed - build may fail\"
        $cmd
    '"
}

# Common function to download file from remote
download_from_remote() {
    local remote_path="$1"
    local local_path="$2"
    scp "$REMOTE_HOST:$REMOTE_DIR/$remote_path" "$local_path"
}

# Common function to download directory from remote
download_dir_from_remote() {
    local remote_path="$1"
    local local_path="$2"
    rsync -az "$REMOTE_HOST:$REMOTE_DIR/$remote_path/" "$local_path/"
}
