$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot "target\healthfirst-pims.jar"
$jpackage = Get-Command jpackage.exe -ErrorAction SilentlyContinue

if (-not (Test-Path $jarPath)) {
    throw "Build the application first with 'mvn clean package'. Missing: $jarPath"
}
if ($null -eq $jpackage) {
    throw "jpackage was not found. Install a JDK 17+ and add its bin folder to PATH."
}

$staging = Join-Path $env:TEMP "healthfirst-pims-jpackage"
$output = Join-Path $projectRoot "dist"
New-Item -ItemType Directory -Force -Path $staging, $output | Out-Null
Copy-Item -Force $jarPath (Join-Path $staging "healthfirst-pims.jar")

& $jpackage.Source `
    --type exe `
    --name HealthFirstPIMS `
    --app-version 1.0.0 `
    --input $staging `
    --main-jar healthfirst-pims.jar `
    --main-class com.healthfirst.pims.PimsApp `
    --dest $output `
    --win-menu `
    --win-shortcut

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE. Windows EXE packaging also requires WiX Toolset 3.0."
}
Write-Host "Installer created in $output"