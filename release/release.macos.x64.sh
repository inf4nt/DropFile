SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_DIR

IMAGE_NAME=dropfile-release-macos-x64
DOCKERFILE_NAME=Dockerfile.release.macos.x64
ARCHIVE_NAME_INPUT=dropfile.tar.gz
ARCHIVE_NAME_OUTPUT=dropfile-macos-x64.tar.gz

docker build -t $IMAGE_NAME --file $DOCKERFILE_NAME ../.

docker create --name extractor $IMAGE_NAME
docker cp extractor:/$ARCHIVE_NAME_INPUT ./$ARCHIVE_NAME_OUTPUT
docker rm extractor
