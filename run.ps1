# CampusCycle - PowerShell Launcher
Write-Host "🚲 Launching CampusCycle Application..." -ForegroundColor Cyan

$env:JAVA_HOME = "E:\Downloads\IntelliJ IDEA 2026.2.1\jbr"
$mvnPath = "E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"

& $mvnPath javafx:run
