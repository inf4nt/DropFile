@echo off

set "BIN_DIR=%~dp0"
for %%I in ("%BIN_DIR%..") do set "APPLICATION_HOME=%%~fI"

set "JAR_PATH=%APPLICATION_HOME%\jars\dropfile-daemon.jar"
set "LOG_PATH=%APPLICATION_HOME%\logs"
set "SPRING_APPLICATION_PROPERTIES_PATH=%APPLICATION_HOME%\conf\dropfile-daemon.application.properties"

IF NOT DEFINED DROPFILE_DAEMON_APPLICATION_HOME_DIRECTORY (
    SET "DROPFILE_DAEMON_APPLICATION_HOME_DIRECTORY=%APPLICATION_HOME%"
)

java ^
         -Dlogging.path="%LOG_PATH%" ^
         -XX:ActiveProcessorCount=1 ^
         -Xmx256m -Xms64m ^
         -jar "%JAR_PATH%" ^
         "--spring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" ^
         "--dropfile.daemon.application-home.directory=%DROPFILE_DAEMON_APPLICATION_HOME_DIRECTORY%" ^
         %*