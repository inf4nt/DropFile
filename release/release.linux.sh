DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-linux

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.linux ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE