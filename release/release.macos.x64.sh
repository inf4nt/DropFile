RELEASE_BUILD_IMAGE=dropfile-release-macos-x64

docker build -t $RELEASE_BUILD_IMAGE --file Dockerfile.release.macos.x64 ../.

docker run --rm -v "$(pwd):/out" $RELEASE_BUILD_IMAGE