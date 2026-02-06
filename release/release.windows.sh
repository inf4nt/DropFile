DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-windows

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.windows ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE