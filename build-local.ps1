param([string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$toolRoot = Join-Path (Split-Path $PSScriptRoot -Parent) '.local-tools'
$env:JAVA_HOME = (Get-Content (Join-Path $toolRoot 'jdk-path.txt') -Raw).Trim()
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle-home'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Push-Location $PSScriptRoot
try { & ./gradlew.bat @Tasks --no-daemon; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
finally { Pop-Location }
