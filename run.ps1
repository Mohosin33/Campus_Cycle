# CampusCycle - PowerShell Launcher
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ($scriptDir) { Set-Location $scriptDir }

Write-Host "🚲 Launching CampusCycle Application..." -ForegroundColor Cyan
Write-Host "Directory: $(Get-Location)" -ForegroundColor Gray

$env:JAVA_HOME = "E:\Downloads\IntelliJ IDEA 2026.2.1\jbr"
$mvnPath = "E:\Downloads\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"

& $mvnPath javafx:run
