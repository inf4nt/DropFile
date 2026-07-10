#!/usr/bin/env sh
set -e

APPLICATION_NAME=dropfile
REQUIRED_JAVA_VERSION=25

REAL_SCRIPT_PATH=$(realpath "$0" 2>/dev/null || readlink -f "$0")
APPLICATION_HOME=$(cd "$(dirname -- "$REAL_SCRIPT_PATH")/../.." && pwd)

echo "=== Running Nano Install ==="

if ! command -v java >/dev/null 2>&1; then
    echo "❌ Error: Java not found in the system!"
    echo "Please run full-install.sh for a full installation."
    exit 1
fi

CURRENT_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$CURRENT_VERSION" = "1" ]; then
    CURRENT_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $2}')
fi

if [ -n "$CURRENT_VERSION" ] && [ "$CURRENT_VERSION" -lt "$REQUIRED_JAVA_VERSION" ]; then
    echo "❌ Error: The installed Java version ($CURRENT_VERSION) is too old."
    echo "Java $REQUIRED_JAVA_VERSION or newer is required. Run full-install.sh to update."
    exit 1
fi

echo "✅ Compatible Java found (version $CURRENT_VERSION)."

LOCAL_BIN="$HOME/.local/bin"
mkdir -p "$LOCAL_BIN"
ln -sf "$APPLICATION_HOME/bin/$APPLICATION_NAME" "$LOCAL_BIN/$APPLICATION_NAME"
echo "✅ Symlink created: $LOCAL_BIN/$APPLICATION_NAME"

BASHRC="$HOME/.bashrc"
if [ ! -f "$BASHRC" ]; then
    echo "📝 File $BASHRC not found. Creating a new one..."
    touch "$BASHRC"
fi

if ! grep -q '\.local/bin' "$BASHRC" 2>/dev/null; then
    echo "" >> "$BASHRC"
    echo "# Added by $APPLICATION_NAME installer" >> "$BASHRC"
    echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$BASHRC"
    echo "✅ Folder ~/.local/bin added to $BASHRC."

    echo "=== Installation complete! ==="
    echo ""
    echo "💡 To start using '$APPLICATION_NAME', please restart Termux."
    echo ""
else
    echo "✅ Environment already configured."
    echo "=== Installation complete! ==="
    echo ""
    echo "You can run: $APPLICATION_NAME"
    echo ""
fi