WORK_DIR=$PWD
PROJECT_DIRECTORY_NAME=dropfile
PROJECT_DIRECTORY=$WORK_DIR/$PROJECT_DIRECTORY_NAME
PROJECT_DIRECTORY_CLI=$PROJECT_DIRECTORY/dropfile-cli
PROJECT_DIRECTORY_DAEMON=$PROJECT_DIRECTORY/dropfile-daemon
PROJECT_DIRECTORY_BIN=$PROJECT_DIRECTORY/bin
PROJECT_DIRECTORY_RUNTIME=$PROJECT_DIRECTORY/runtime
FINAL_ARCHIVE_NAME=dropfile-windows.tar.gz

echo "Clearing $PROJECT_DIRECTORY"
rm -rf $PROJECT_DIRECTORY

echo "Building project"
mvn clean install -f ../

echo "Preparing $PROJECT_DIRECTORY"
mkdir $PROJECT_DIRECTORY
mkdir $PROJECT_DIRECTORY_BIN
mkdir $PROJECT_DIRECTORY_CLI
mkdir $PROJECT_DIRECTORY_DAEMON

echo "Copying jar files"
cp ../dropfile-cli/target/dropfile-cli*.jar $PROJECT_DIRECTORY_CLI/dropfile-cli.jar
cp ../dropfile-daemon/target/dropfile-daemon*.jar $PROJECT_DIRECTORY_DAEMON/dropfile-daemon.jar

echo "Copying windows .bat files"
cp windows/dropfile.bat $PROJECT_DIRECTORY_BIN/dropfile.bat
cp windows/dropfile-daemon.bat $PROJECT_DIRECTORY_BIN/dropfile-daemon.bat

echo "Copying README.md"
cp windows/README.md $PROJECT_DIRECTORY/README.md

echo "Preparing, copying JAVA_HOME to runtime"
JAVA_HOME_WINDOWS=$(cmd.exe /c "echo %JAVA_HOME%" | tr -d '\r')
WSL_JAVA_HOME_WINDOWS=$(wslpath $JAVA_HOME_WINDOWS)
echo $WSL_JAVA_HOME_WINDOWS

cp -r $WSL_JAVA_HOME_WINDOWS $PROJECT_DIRECTORY_RUNTIME

echo "Preparing tar.gz"
echo "Preparing dropfile-windows.tar.gz"
tar -czf $FINAL_ARCHIVE_NAME $PROJECT_DIRECTORY_NAME

echo "Clearing $PROJECT_DIRECTORY"
rm -rf $PROJECT_DIRECTORY