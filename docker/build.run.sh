SHARE_MOUNT=dropfile-daemon-share-mount

echo "⚠️ Warning: The following directory will be created: $HOME/$SHARE_MOUNT"
echo "This directory will be used as a shared mount between the host OS and Docker."
echo "⚠️ Place your files in this directory to make them accessible to the daemon."

read -p "Press y and Enter to continue: " answer

if [[ "$answer" == "y" ]]; then
    mkdir -p "$HOME/$SHARE_MOUNT"
    echo "✅ Directory successfully created."
else
    echo "❌ Directory creation canceled by user."
    exit 1
fi

export SHARE_MOUNT

./down.sh
docker compose build
docker compose up -d
