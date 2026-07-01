IMAGE_NAME=dropfile-daemon
SHARE_MOUNT=dropfile-daemon-share-mount

./build.sh

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

docker run --rm \
        -v dropfile-daemon-conf:/daemon-conf \
        --mount type=bind,source=$HOME/$SHARE_MOUNT,target=$HOME/$SHARE_MOUNT \
        -e DROPFILE_DAEMON_DAEMON-SECRETS_DIRECTORY=/daemon-conf \
        -e DROPFILE_DAEMON_INSTALLATION-SEED_DIRECTORY=/daemon-conf \
        -e DROPFILE_DAEMON_PORT=28282 \
        -p 28282:28282 \
        --name dropfile-daemon \
        $IMAGE_NAME
