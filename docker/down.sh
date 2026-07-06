SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_DIR

SHARE_MOUNT=dropfile-daemon-share-mount
SHARE_DIRECTORY=$HOME/$SHARE_MOUNT

export SHARE_DIRECTORY

docker compose down
