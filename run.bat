@echo off
title CampusCycle - Desktop Application
echo Starting CampusCycle...

REM Set Java Home to the installed OpenJDK runtime
if exist "E:\Downloads\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe" (
    set "JAVA_HOME=E:\Downloads\IntelliJ IDEA 2026.2.1\jbr"
    set "PATH=E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin;%JAVA_HOME%\bin;%PATH%"
)

echo Using JAVA_HOME: %JAVA_HOME%
mvn clean javafx:run
pause
