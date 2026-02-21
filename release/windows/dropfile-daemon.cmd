@echo off

set JAR_PATH=%DROPFILE_HOME%\dropfile-daemon\dropfile-daemon.jar
set JAVA_PATH=%DROPFILE_HOME%\runtime\bin\java.exe
set LOG_PATH=%DROPFILE_HOME%\logs

"%JAVA_PATH%" "-Dlogging.path=%LOG_PATH%" -XX:ActiveProcessorCount=1 -Xmx256m -Xms256m -jar "%JAR_PATH%" %*
