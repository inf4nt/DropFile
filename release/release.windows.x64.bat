@echo off
set RELEASE_BUILD_IMAGE=dropfile-release-windows-x64

docker build -t %RELEASE_BUILD_IMAGE% --file Dockerfile.release.windows.x64 ../.

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

docker run --rm -v "%cd%:/out" %RELEASE_BUILD_IMAGE%

echo [SUCCESS] Done!
pause
