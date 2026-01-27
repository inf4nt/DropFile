@echo off

jpackage --name dropfile ^
         --input ../dropfile-cli/target/ ^
         --main-jar dropfile-cli-0.0.1-SNAPSHOT.jar ^
         --main-class org.springframework.boot.loader.launch.JarLauncher ^
         --app-version 0.0.1 ^
         --type app-image ^
         --java-options "--enable-preview" ^
         --verbose ^
         --win-console ^
         --runtime-image "%JAVA_HOME%"

jpackage --name dropfiled ^
      --input ../dropfile-daemon/target/ ^
      --main-jar dropfile-daemon-0.0.1-SNAPSHOT.jar ^
      --main-class org.springframework.boot.loader.launch.JarLauncher ^
      --app-version 0.0.1 ^
      --type app-image ^
      --java-options "--enable-preview" ^
      --verbose ^
      --win-console ^
      --runtime-image "%JAVA_HOME%"

echo.
echo Done!
pause
