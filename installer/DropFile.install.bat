@echo off

jpackage --name DropFileCli ^
         --input ../drop-file-cli/target/ ^
         --main-jar drop-file-cli-0.0.1-SNAPSHOT.jar ^
         --main-class org.springframework.boot.loader.launch.JarLauncher ^
         --app-version 0.0.1 ^
         --type app-image ^
         --java-options "--enable-preview" ^
         --verbose ^
         --win-console ^
         --runtime-image "%JAVA_HOME%"

jpackage --name DropFileDaemon ^
      --input ../drop-file-daemon/target/ ^
      --main-jar drop-file-daemon-0.0.1-SNAPSHOT.jar ^
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
