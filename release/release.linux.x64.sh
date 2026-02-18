RELEASE_BUILD_IMAGE=dropfile-release-linux-x64

docker build -t $RELEASE_BUILD_IMAGE --file Dockerfile.release.linux.x64 ../.

docker run --rm -v "$(pwd):/out" $RELEASE_BUILD_IMAGE