#!/usr/bin/env sh
set -e

WORK_DIR=$PWD
PROJECT_DIRECTORY_NAME=dropfile
PROJECT_DIRECTORY=$WORK_DIR/$PROJECT_DIRECTORY_NAME
PROJECT_DIRECTORY_CLI=$PROJECT_DIRECTORY/dropfile-cli
PROJECT_DIRECTORY_DAEMON=$PROJECT_DIRECTORY/dropfile-daemon
PROJECT_DIRECTORY_BIN=$PROJECT_DIRECTORY/bin
PROJECT_DIRECTORY_RUNTIME=$PROJECT_DIRECTORY/runtime
FINAL_ARCHIVE_NAME=dropfile-windows.tar.gz

echo "Cleaning $PROJECT_DIRECTORY"
rm -rf $PROJECT_DIRECTORY

echo "Building project"
M2_HOME_WINDOWS=$(cmd.exe /c "echo %M2_HOME%" | tr -d '\r')
M2_HOME_WINDOWS_WSL=$(wslpath $M2_HOME_WINDOWS)
echo "Maven is calling $M2_HOME_WINDOWS_WSL"
$M2_HOME_WINDOWS_WSL/bin/mvn clean install -f ../

echo "Preparing $PROJECT_DIRECTORY"
mkdir $PROJECT_DIRECTORY
mkdir $PROJECT_DIRECTORY_BIN
mkdir $PROJECT_DIRECTORY_CLI
mkdir $PROJECT_DIRECTORY_DAEMON

echo "Copying jar files"
cp ../dropfile-cli/target/dropfile-cli*.jar $PROJECT_DIRECTORY_CLI/dropfile-cli.jar
cp ../dropfile-daemon/target/dropfile-daemon*.jar $PROJECT_DIRECTORY_DAEMON/dropfile-daemon.jar

echo "Creating WSL files for dropfile and dropfile-daemon"
cp windows/dropfile $PROJECT_DIRECTORY_BIN

echo "Creating .cmd files for dropfile and dropfile-daemon"
cp windows/dropfile.cmd $PROJECT_DIRECTORY_BIN
cp windows/dropfile-daemon.cmd $PROJECT_DIRECTORY_BIN

echo "Copying README.md"
cp windows/README.md $PROJECT_DIRECTORY/README.md

echo "Preparing, copying JAVA_HOME to runtime"
JAVA_HOME_WINDOWS=$(cmd.exe /c "echo %JAVA_HOME%" | tr -d '\r')
WSL_JAVA_HOME_WINDOWS=$(wslpath $JAVA_HOME_WINDOWS)
echo $WSL_JAVA_HOME_WINDOWS

cp -r $WSL_JAVA_HOME_WINDOWS $PROJECT_DIRECTORY_RUNTIME

sed -i 's/\r$//' $PROJECT_DIRECTORY_BIN/*

echo "Preparing dropfile-windows.tar.gz"
tar -czf $FINAL_ARCHIVE_NAME $PROJECT_DIRECTORY_NAME

echo "Cleaning $PROJECT_DIRECTORY"
rm -rf $PROJECT_DIRECTORY