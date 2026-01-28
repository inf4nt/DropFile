WORK_DIR=$PWD
JARS_DIR=$WORK_DIR/jars
STAGING_DIR=$WORK_DIR/staging
FINAL_DESTINATION=$WORK_DIR/dropfile-windows
FINAL_ARCHIVE=dropfile-windows.tar.gz

echo 'Installing project and its dependencies'
mvn clean install -f ../

echo "Cleaning files: $FINAL_ARCHIVE"
rm -rf $FINAL_ARCHIVE

echo "Cleaning directories: $FINAL_DESTINATION"
rm -rf $FINAL_DESTINATION

echo "Cleaning directories: $JARS_DIR"
rm -rf $JARS_DIR

echo "Cleaning directories: $STAGING_DIR"
rm -rf $STAGING_DIR

echo "Creating $FINAL_DESTINATION directories"
mkdir $FINAL_DESTINATION
mkdir $FINAL_DESTINATION/dropfile-cli
mkdir $FINAL_DESTINATION/dropfile-daemon

echo "Creating $JARS_DIR directories"
mkdir $JARS_DIR
mkdir $JARS_DIR/dropfile-cli
mkdir $JARS_DIR/dropfile-daemon
mkdir $JARS_DIR/dropfile-installer

echo "Creating $STAGING_DIR directories"
mkdir $STAGING_DIR

echo "Copying jars to $JARS_DIR"
cp ../dropfile-cli/target/dropfile-cli*.jar $JARS_DIR/dropfile-cli/dropfile-cli.jar
cp ../dropfile-daemon/target/dropfile-daemon*.jar $JARS_DIR/dropfile-daemon/dropfile-daemon.jar
cp ../dropfile-installer/target/dropfile-installer*.jar $JARS_DIR/dropfile-installer/dropfile-installer.jar

echo "Copying jars to $FINAL_DESTINATION"
cp $JARS_DIR/dropfile-cli/dropfile-cli.jar $FINAL_DESTINATION/dropfile-cli/dropfile-cli.jar
cp $JARS_DIR/dropfile-daemon/dropfile-daemon.jar $FINAL_DESTINATION/dropfile-daemon/dropfile-daemon.jar

echo "Copying windows .bat files to $FINAL_DESTINATION"
cp $WORK_DIR/windows/dropfile-cli.bat $FINAL_DESTINATION/dropfile-cli/dropfile-cli.bat
cp $WORK_DIR/windows/dropfile-daemon.bat $FINAL_DESTINATION/dropfile-daemon/dropfile-daemon.bat

echo "Executing jpackage against dropfile-installer.jar"
JAVA_HOME_WIN=$(cmd.exe /c "echo %JAVA_HOME%" | tr -d '\r')

jpackage.exe --name dropfile-installer \
      --input jars/dropfile-installer/ \
      --main-jar dropfile-installer.jar \
      --dest staging \
      --main-class com.evolution.dropfile.installer.Installer \
      --app-version 0.0.1 \
      --type app-image \
      --java-options "--enable-preview" \
      --verbose \
      --win-console \
      --runtime-image "$JAVA_HOME_WIN"

echo "Copying runtime"
cp -r $STAGING_DIR/dropfile-installer/runtime $FINAL_DESTINATION/

echo "Copying installer app"
cp -r $STAGING_DIR/dropfile-installer/app $FINAL_DESTINATION/

echo "Copying installer jar"
cp -r $STAGING_DIR/dropfile-installer/dropfile-installer.exe $FINAL_DESTINATION/

echo "Preparing dropfile-windows.tar.gz"
tar -czf $FINAL_ARCHIVE dropfile-windows

echo "Cleaning directories: $JARS_DIR"
rm -rf $JARS_DIR

echo "Cleaning directories: $STAGING_DIR"
rm -rf $STAGING_DIR

echo "Cleaning directories: $FINAL_DESTINATION"
rm -rf $FINAL_DESTINATION