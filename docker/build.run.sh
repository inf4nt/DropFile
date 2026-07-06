SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_DIR

SHARE_MOUNT=dropfile-daemon-share-mount

echo "ℹ️  A shared directory will be created at: $HOME/$SHARE_MOUNT"
echo "This directory enables file sharing between your Host OS and the Docker container."
echo "Simply place your files there to make them accessible to the daemon."
echo ""

read -p "Enable local file sharing? (y/n): " answer

if [[ "$answer" == "y" ]]; then
    SHARE_DIRECTORY=$HOME/$SHARE_MOUNT
    mkdir -p $SHARE_DIRECTORY
    echo "✅ Success: Shared directory created at $SHARE_DIRECTORY"
    echo "🚀 Starting application WITH file access..."

    export SHARE_DIRECTORY

    ./down.sh
    docker compose -f docker-compose.yaml build
    docker compose -f docker-compose.yaml up -d
elif [[ "$answer" == "n" ]]; then
    echo ""
    echo "💡 Local file sharing skipped by user."
    echo "ℹ️  The application will run in isolated mode (no local file access)."
    echo "🚀 Starting application WITHOUT file access..."

    ./down.sh
    docker compose -f docker-compose.no-mount.yaml build
    docker compose -f docker-compose.no-mount.yaml up -d
else
  exit 1
fi
