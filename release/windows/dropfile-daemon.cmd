@echo off

cd /d "%DROPFILE_HOME%"

set JAR_PATH=%DROPFILE_HOME%\jars\dropfile-daemon.jar
set JAVA_PATH=%DROPFILE_HOME%\runtime\bin\java.exe
set LOG_PATH=%DROPFILE_HOME%\logs
set SPRING_APPLICATION_PROPERTIES_PATH=%DROPFILE_HOME%\conf\dropfile-daemon.application.properties

"%JAVA_PATH%" ^
        "-Dlogging.path=%LOG_PATH%" ^
         -XX:ActiveProcessorCount=1 ^
         -Xmx256m -Xms64m ^
         -jar "%JAR_PATH%" ^
         "--spring.config.location=file:%SPRING_APPLICATION_PROPERTIES_PATH%" ^
         %*
