@echo off

set JAR_PATH=%DROPFILE_HOME%\dropfile-cli\dropfile-cli.jar
set JAVA_PATH=%DROPFILE_HOME%\runtime\bin\java.exe

"%JAVA_PATH%" -XX:ActiveProcessorCount=1 -Xmx128m -Xms128m -jar "%JAR_PATH%" %*