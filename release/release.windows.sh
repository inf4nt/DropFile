DROPFILE_RELEASE_BUILD_IMAGE=dropfile-release-build

docker build -t $DROPFILE_RELEASE_BUILD_IMAGE --file Dockerfile.release.windows ../.

docker run --rm -v "$(pwd):/out" $DROPFILE_RELEASE_BUILD_IMAGE