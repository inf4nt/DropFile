cd %~dp0

@echo off
set IMAGE_NAME=dropfile-release-windows-x64
set DOCKERFILE_NAME=Dockerfile.release.windows.x64
set ARCHIVE_NAME_INPUT=dropfile.tar.gz
set ARCHIVE_NAME_OUTPUT=dropfile-windows-x64.tar.gz

docker build -t %IMAGE_NAME% --file %DOCKERFILE_NAME% ../.

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

docker create --name extractor %IMAGE_NAME%
docker cp extractor:/%ARCHIVE_NAME_INPUT% ./%ARCHIVE_NAME_OUTPUT%
docker rm extractor

pause
