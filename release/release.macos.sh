DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-macos

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.macos ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE