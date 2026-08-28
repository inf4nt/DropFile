@echo off
setlocal

set "BIN_DIR=%~dp0"
for %%I in ("%BIN_DIR%..") do set "APPLICATION_HOME=%%~fI"

set "CLI_DIR=%APPLICATION_HOME%\jars\cli"
set "ORIGINAL_JAR=%CLI_DIR%\dropfile-cli.jar"
set "CDS_DIR=%CLI_DIR%\cds"
set "CDS_JAR=%CDS_DIR%\dropfile-cli.jar"
set "JSA_PATH=%CDS_DIR%\dropfile-cli.jsa"

set "SPRING_APPLICATION_PROPERTIES_PATH=%APPLICATION_HOME%\conf\dropfile-cli.application.properties"

IF NOT DEFINED DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY (
    SET "DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY=%APPLICATION_HOME%\conf"
)

IF NOT DEFINED DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY (
    SET "DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY=%APPLICATION_HOME%\conf"
)

IF NOT DEFINED DROPFILE_CLI_CPU_COUNT (
    SET "DROPFILE_CLI_CPU_COUNT=1"
)

IF NOT DEFINED DROPFILE_CLI_RAM_MB_XMX (
    SET "DROPFILE_CLI_RAM_MB_XMX=128"
)

IF NOT DEFINED DROPFILE_CLI_RAM_MB_XMS (
    SET "DROPFILE_CLI_RAM_MB_XMS=64"
)

IF NOT DEFINED DROPFILE_CLI_CDS_ENABLED (
    SET "DROPFILE_CLI_CDS_ENABLED=false"
)

set "TARGET_JAR=%ORIGINAL_JAR%"
set "CDS_JVM_OPTS="

if /I "%DROPFILE_CLI_CDS_ENABLED%"=="true" (
    if not exist "%JSA_PATH%" (
        echo [dropfile] First run detected. Optimizing application startup time...

        if exist "%CDS_DIR%" rmdir /s /q "%CDS_DIR%"

        java -Djarmode=tools -jar "%ORIGINAL_JAR%" extract --destination "%CDS_DIR%" >nul 2>&1

        java ^
                "-XX:ActiveProcessorCount=%DROPFILE_CLI_CPU_COUNT%" ^
                "-Xmx%DROPFILE_CLI_RAM_MB_XMX%m" ^
                "-Xms%DROPFILE_CLI_RAM_MB_XMS%m" ^
                "-XX:ArchiveClassesAtExit=%JSA_PATH%" ^
                "-Dspring.context.exit=on" ^
                "-Ddropfile.home=%APPLICATION_HOME%" ^
                "-Dspring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" ^
                "-Ddropfile.daemon.daemon-secrets.directory=%DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY%" ^
                "-Ddropfile.daemon.installation-seed.directory=%DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY%" ^
                -jar "%CDS_JAR%" >nul 2>&1

        echo [dropfile] Optimization completed successfully!
    )

    set "TARGET_JAR=%CDS_JAR%"
    set "CDS_JVM_OPTS="-XX:SharedArchiveFile=%JSA_PATH%""
)

java ^
        "-XX:ActiveProcessorCount=%DROPFILE_CLI_CPU_COUNT%" ^
        "-Xmx%DROPFILE_CLI_RAM_MB_XMX%m" ^
        "-Xms%DROPFILE_CLI_RAM_MB_XMS%m" ^
        %CDS_JVM_OPTS% ^
        "-Ddropfile.home=%APPLICATION_HOME%" ^
        "-Dspring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" ^
        "-Ddropfile.daemon.daemon-secrets.directory=%DROPFILE_DAEMON_DAEMON_SECRETS_DIRECTORY%" ^
        "-Ddropfile.daemon.installation-seed.directory=%DROPFILE_DAEMON_INSTALLATION_SEED_DIRECTORY%" ^
        -jar "%TARGET_JAR%" ^
        %*