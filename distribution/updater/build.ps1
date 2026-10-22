$ErrorActionPreference = 'Stop'
$sourceDir = $PSScriptRoot
$repoDir = Split-Path (Split-Path $sourceDir -Parent) -Parent
$outputDir = Join-Path $repoDir 'build/player-updater'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$compiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
$exePath = Join-Path $outputDir 'JUGCRAFT.exe'
$sourcePath = Join-Path $sourceDir 'JugcraftUpdater.cs'
& $compiler /nologo /target:winexe /optimize+ /warnaserror+ /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Web.Extensions.dll /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll /reference:System.Management.dll "/out:$exePath" $sourcePath
if ($LASTEXITCODE -ne 0) { throw 'Updater compilation failed' }
Write-Output "Built $outputDir/JUGCRAFT.exe"
