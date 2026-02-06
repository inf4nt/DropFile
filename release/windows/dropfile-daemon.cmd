@echo off
set DROPFILE_HOME=%~dp0..
"%DROPFILE_HOME%\runtime\bin\java.exe" -jar "%DROPFILE_HOME%\dropfile-daemon\dropfile-daemon.jar" %*
