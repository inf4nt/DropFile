#!/bin/sh
set -e

REAL_SCRIPT_PATH=$(realpath "$0" 2>/dev/null || readlink -f "$0")
SCRIPT_DIR=$(dirname "$REAL_SCRIPT_PATH")
cd "$SCRIPT_DIR"

SHARE_MOUNT=dropfile-daemon-share-mount
SHARE_DIRECTORY=$HOME/$SHARE_MOUNT

export SHARE_DIRECTORY

docker compose down
