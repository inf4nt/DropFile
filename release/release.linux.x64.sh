#!/bin/sh
set -e

REAL_SCRIPT_PATH=$(realpath "$0" 2>/dev/null || readlink -f "$0")
SCRIPT_DIR=$(dirname "$REAL_SCRIPT_PATH")
cd "$SCRIPT_DIR"

IMAGE_NAME="dropfile-release-linux-x64"
DOCKERFILE_NAME="Dockerfile.release.linux.x64"
ARCHIVE_NAME_INPUT="dropfile.tar.gz"
ARCHIVE_NAME_OUTPUT="dropfile-linux-x64.tar.gz"

docker build -t "$IMAGE_NAME" --file "$DOCKERFILE_NAME" ..

docker create --name extractor "$IMAGE_NAME"
docker cp extractor:"/$ARCHIVE_NAME_INPUT" "./$ARCHIVE_NAME_OUTPUT"
docker rm extractor
