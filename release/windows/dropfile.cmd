@echo off

set "BIN_DIR=%~dp0"
for %%I in ("%BIN_DIR%..") do set "APPLICATION_HOME=%%~fI"

set "JAR_PATH=%APPLICATION_HOME%\jars\dropfile-cli.jar"
set "JAVA_PATH=%APPLICATION_HOME%\runtime\bin\java.exe"
set "SPRING_APPLICATION_PROPERTIES_PATH=%APPLICATION_HOME%\conf\dropfile-cli.application.properties"

IF NOT DEFINED DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY (
    SET "DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY=%APPLICATION_HOME%\conf"
)

IF NOT DEFINED DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY (
    SET "DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY=%APPLICATION_HOME%\conf"
)

"%JAVA_PATH%" ^
        -XX:ActiveProcessorCount=1 ^
        -Xmx128m -Xms64m ^
        -jar "%JAR_PATH%" ^
        "--spring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" ^
        "--dropfile.daemon.daemon-secrets.directory=%DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY%" ^
        "--dropfile.daemon.installation-seed.directory=%DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY%" ^
        %*
