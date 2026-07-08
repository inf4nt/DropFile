@echo off
cd /d "%~dp0"

set "IMAGE_NAME=dropfile-release-windows-x64"
set "DOCKERFILE_NAME=Dockerfile.release.windows.x64"
set "ARCHIVE_NAME_INPUT=dropfile.tar.gz"
set "ARCHIVE_NAME_OUTPUT=dropfile-windows-x64.tar.gz"

rem
docker build -t %IMAGE_NAME% --file %DOCKERFILE_NAME% ..
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

rem
docker create --name extractor %IMAGE_NAME%
docker cp extractor:/%ARCHIVE_NAME_INPUT% ./%ARCHIVE_NAME_OUTPUT%

rem
set "CP_ERROR=%ERRORLEVEL%"

rem
docker rm extractor

rem
if %CP_ERROR% NEQ 0 (
    echo [ERROR] Extraction failed!
    pause
    exit /b %CP_ERROR%
)

pause
