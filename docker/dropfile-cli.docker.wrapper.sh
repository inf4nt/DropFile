#!/bin/sh

REAL_SCRIPT_PATH=$(realpath "$0" 2>/dev/null || readlink -f "$0")
SCRIPT_DIR=$(dirname "$REAL_SCRIPT_PATH")
cd "$SCRIPT_DIR"

docker exec -it dropfile-cli java -jar app.jar "$@"