#!/bin/bash

# Shared configuration for remote development scripts
# Source this file in any script that needs to interact with the remote Mac

# Remote machine configuration
export REMOTE_HOST="dsilbergleithcu@worklaptop.local"
export REMOTE_DIR="~/AndroidStudioProjects/Deadly"

# Rsync options for syncing code to remote
export RSYNC_OPTS="-az --delete --exclude '.gradle' --exclude 'build'"

# Common function to sync code to remote
sync_to_remote() {
    echo "🔄 Syncing code to remote host..."
    rsync $RSYNC_OPTS ./ "$REMOTE_HOST:$REMOTE_DIR"
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
