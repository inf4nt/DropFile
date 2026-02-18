RELEASE_BUILD_IMAGE=dropfile-release-windows-x64

docker build -t $RELEASE_BUILD_IMAGE --file Dockerfile.release.windows.x64 ../.

docker run --rm -v "$(pwd):/out" $RELEASE_BUILD_IMAGE