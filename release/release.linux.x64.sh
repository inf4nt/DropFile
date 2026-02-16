DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-linux-x64

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.linux.x64 ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE