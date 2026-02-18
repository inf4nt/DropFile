@echo off
set DROPFILE_HOME=%~dp0..
"%DROPFILE_HOME%\runtime\bin\java.exe" -XX:ActiveProcessorCount=1 -Xmx256m -Xms256m  -jar "%DROPFILE_HOME%\dropfile-daemon\dropfile-daemon.jar" %*
