#FROM ubuntu:22.04
#WORKDIR /test
#CMD ["sh", "-c", "touch test.txt"]





FROM ubuntu:24.04 AS build

RUN apt update && apt upgrade -y \
    && apt install curl unzip tar -y \
    && rm -rf /var/lib/apt/lists/*

ARG BUILD_WORKDIR=/build
ARG PROJECT_WORKDIR=/project

ARG JDK_MAJOR_VERION=21
ARG JDK_VERSION=21.0.9
ARG JDK_LINUX_DOWNLOAD_URL="https://download.oracle.com/java/"$JDK_MAJOR_VERION"/archive/jdk-"$JDK_VERSION"_linux-x64_bin.tar.gz"
ARG JDK_ARCHIVE="jdk.tar.gz"
ARG JDK_LOCATION_DIRECTORY=$BUILD_WORKDIR
ARG JAVA_HOME=$JDK_LOCATION_DIRECTORY/jdk-$JDK_VERSION

ARG M2_MAJOR_VERSION=3
ARG M2_VERSION=3.9.12
ARG M2_DOWNLOAD_URL="https://dlcdn.apache.org/maven/maven-"$M2_MAJOR_VERSION"/"$M2_VERSION"/binaries/apache-maven-"$M2_VERSION"-bin.tar.gz"
ARG M2_ARCHIVE="apache-maven.tar.gz"
ARG M2_LOCATION_DIRECTORY=$BUILD_WORKDIR
ARG M2_HOME=$M2_LOCATION_DIRECTORY/apache-maven-$M2_VERSION

WORKDIR $BUILD_WORKDIR

RUN curl -o $JDK_ARCHIVE $JDK_LINUX_DOWNLOAD_URL \
    && tar -xzf $JDK_ARCHIVE -C $JDK_LOCATION_DIRECTORY \
    && rm $JDK_ARCHIVE

RUN curl -o $M2_ARCHIVE $M2_DOWNLOAD_URL \
    && tar -xzf $M2_ARCHIVE -C $M2_LOCATION_DIRECTORY \
    && rm $M2_ARCHIVE

RUN ln -s $JAVA_HOME/bin/java /usr/bin/java
RUN ln -s $M2_HOME/bin/mvn /usr/bin/mvn

WORKDIR $PROJECT_WORKDIR

COPY pom.xml $PROJECT_WORKDIR/pom.xml

COPY /dropfile-common/pom.xml $PROJECT_WORKDIR/dropfile-common/pom.xml
COPY /dropfile-store/pom.xml $PROJECT_WORKDIR/dropfile-store/pom.xml
COPY /dropfile-cli/pom.xml $PROJECT_WORKDIR/dropfile-cli/pom.xml
COPY /dropfile-daemon/pom.xml $PROJECT_WORKDIR/dropfile-daemon/pom.xml

# RUN mvn package -f $PROJECT_WORKDIR

COPY /dropfile-common/src $PROJECT_WORKDIR/dropfile-common/src
COPY /dropfile-store/src $PROJECT_WORKDIR/dropfile-store/src
COPY /dropfile-cli/src $PROJECT_WORKDIR/dropfile-cli/src
COPY /dropfile-daemon/src $PROJECT_WORKDIR/dropfile-daemon/src

RUN mvn clean install -DskipTests -f $PROJECT_WORKDIR

FROM ubuntu:24.04 AS release

RUN apt update && apt upgrade -y \
    && apt install curl unzip tar -y \
    && rm -rf /var/lib/apt/lists/*

ARG RELEASE_WORKDIR="release"

ARG JDK_MAJOR_VERION=21
ARG JDK_VERSION=21.0.9
ARG JDK_DOWNLOAD_URL="https://download.oracle.com/java/$JDK_MAJOR_VERION/archive/jdk-"$JDK_VERSION"_windows-x64_bin.zip"
ARG JDK_ARCHIVE="jdk.tar.gz"
ARG JDK_LOCATION_DIRECTORY=$RELEASE_WORKDIR
ARG JAVA_HOME=$JDK_LOCATION_DIRECTORY/jdk-$JDK_VERSION

RUN mkdir /$RELEASE_WORKDIR \
    && mkdir /$RELEASE_WORKDIR/dropfile-windows \
    && mkdir /$RELEASE_WORKDIR/dropfile-windows/bin \
    && mkdir /$RELEASE_WORKDIR/dropfile-windows/dropfile-cli \
    && mkdir /$RELEASE_WORKDIR/dropfile-windows/dropfile-daemon \
    && mkdir /$RELEASE_WORKDIR/dropfile-windows/runtime

COPY --from=build /project/dropfile-cli/target/*.jar $RELEASE_WORKDIR/dropfile-windows/dropfile-cli/dropfile-cli.jar
COPY --from=build /project/dropfile-daemon/target/*.jar $RELEASE_WORKDIR/dropfile-windows/dropfile-daemon/dropfile-daemon.jar

RUN curl -o $JDK_ARCHIVE $JDK_DOWNLOAD_URL \
    && unzip $JDK_ARCHIVE -d $JDK_LOCATION_DIRECTORY \
    && rm $JDK_ARCHIVE

RUN cp -r $JAVA_HOME/. "/$RELEASE_WORKDIR/dropfile-windows/runtime"

COPY release/windows/ $RELEASE_WORKDIR/dropfile-windows/bin

RUN cp -r $RELEASE_WORKDIR/dropfile-windows/. dropfile

RUN tar -czf $RELEASE_WORKDIR/dropfile-windows.tar.gz dropfile

RUN mkdir -p out

ENTRYPOINT ["cp", "/release/dropfile-windows.tar.gz", "/out/dropfile-windows.tar.gz"]
