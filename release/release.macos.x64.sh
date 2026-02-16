DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-macos-x64

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.macos.x64 ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE