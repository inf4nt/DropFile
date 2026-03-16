@echo off

cd /d "%DROPFILE_HOME%"

set JAR_PATH=%DROPFILE_HOME%\dropfile-cli\dropfile-cli.jar
set JAVA_PATH=%DROPFILE_HOME%\runtime\bin\java.exe
set SPRING_APPLICATION_PROPERTIES_PATH=%DROPFILE_HOME%\conf\dropfile-cli.application.properties

"%JAVA_PATH%" -XX:ActiveProcessorCount=1 -Xmx128m -Xms128m -jar "%JAR_PATH%" "--spring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" %*
