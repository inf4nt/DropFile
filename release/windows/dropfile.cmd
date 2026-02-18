@echo off
set DROPFILE_HOME=%~dp0..
"%DROPFILE_HOME%\runtime\bin\java.exe" -XX:ActiveProcessorCount=1 -Xmx128m -Xms128m -jar "%DROPFILE_HOME%\dropfile-cli\dropfile-cli.jar" %*
