@echo off
title CampusCycle - Desktop Application
cd /d "%~dp0"
echo ===================================================
echo     Starting CampusCycle Desktop Application...
echo ===================================================

REM Set Java Home to the installed OpenJDK runtime
if exist "E:\Downloads\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe" (
    set "JAVA_HOME=E:\Downloads\IntelliJ IDEA 2026.2.1\jbr"
    set "PATH=E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin;%JAVA_HOME%\bin;%PATH%"
)

echo Working Directory: %CD%
echo Using JAVA_HOME: %JAVA_HOME%

call "E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" javafx:run

pause
