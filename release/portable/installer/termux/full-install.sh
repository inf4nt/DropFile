#!/usr/bin/env sh
set -e

REQUIRED_JAVA_VERSION=25

REAL_SCRIPT_PATH=$(realpath "$0" 2>/dev/null || readlink -f "$0")
SCRIPT_DIR=$(dirname "$REAL_SCRIPT_PATH")

echo "=== Running Full Install ==="
echo "Warning! System Java $REQUIRED_JAVA_VERSION (openjdk-$REQUIRED_JAVA_VERSION) will be installed now."
echo "If you already have another default Java version installed, it might be overridden."

printf "Are you sure you want to proceed? (y/n): "
read answer

if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
    echo ""
    echo "You declined the system Java installation."
    echo "Perhaps a compatible Java version is already installed."
    echo "If so, try running the command:"
    echo "  sh ./nano-install.sh"
    echo "It will configure the application without downloading any packages."
    exit 0
fi

echo ""
echo "Downloading and installing Java $REQUIRED_JAVA_VERSION..."
pkg update -y
pkg install -y "openjdk-$REQUIRED_JAVA_VERSION"

JAVA_BIN_PATH=$(dpkg -L "openjdk-$REQUIRED_JAVA_VERSION" 2>/dev/null | grep '/bin/java$' | head -n 1)

if [ -n "$JAVA_BIN_PATH" ] && [ -x "$JAVA_BIN_PATH" ]; then
    JAVA_25_BIN_DIR=$(dirname "$JAVA_BIN_PATH")
    JAVA_25_HOME=$(dirname "$JAVA_25_BIN_DIR")

    export JAVA_HOME="$JAVA_25_HOME"
    export PATH="$JAVA_25_BIN_DIR:$PATH"

    PREFIX_PATH=${PREFIX:-"/data/data/com.termux/files/usr"}
    ln -sf "$JAVA_BIN_PATH" "$PREFIX_PATH/bin/java"
    if [ -x "$JAVA_25_BIN_DIR/javac" ]; then
        ln -sf "$JAVA_25_BIN_DIR/javac" "$PREFIX_PATH/bin/javac"
    fi
fi

echo "✅ System packages installed and environment updated."
echo "Proceeding to environment configuration..."
echo ""

sh "$SCRIPT_DIR/nano-install.sh"