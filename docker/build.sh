CLI_IMAGE_NAME=dropfile-cli

docker build -t $CLI_IMAGE_NAME --file Dockerfile.cli ../.

DAEMON_IMAGE_NAME=dropfile-daemon

docker build -t $DAEMON_IMAGE_NAME --file Dockerfile.daemon ../.
