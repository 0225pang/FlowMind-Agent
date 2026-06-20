param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$runtimeDir = Join-Path $ProjectRoot ".runtime"
$chromeDir = Join-Path $runtimeDir "chrome"
$zipPath = Join-Path $runtimeDir "chrome-for-testing-win64.zip"
$extractDir = Join-Path $runtimeDir "chrome-for-testing-extract"
$chromeExe = Join-Path $chromeDir "chrome.exe"

if ((Test-Path $chromeExe) -and -not $Force) {
    Write-Host "Chrome already exists: $chromeExe"
    Write-Host "Use -Force to download and overwrite files."
    exit 0
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
New-Item -ItemType Directory -Force -Path $chromeDir | Out-Null

$metadataUrl = "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json"
Write-Host "Fetching Chrome for Testing metadata..."
$metadata = Invoke-RestMethod -Uri $metadataUrl -TimeoutSec 60

$download = $metadata.channels.Stable.downloads.chrome |
    Where-Object { $_.platform -eq "win64" } |
    Select-Object -First 1

if (-not $download -or -not $download.url) {
    throw "Cannot find Stable win64 Chrome for Testing download URL."
}

Write-Host "Downloading: $($download.url)"
Invoke-WebRequest -Uri $download.url -OutFile $zipPath -TimeoutSec 600

if (Test-Path $extractDir) {
    Remove-Item -LiteralPath $extractDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $extractDir | Out-Null

Write-Host "Extracting to: $extractDir"
Expand-Archive -LiteralPath $zipPath -DestinationPath $extractDir -Force

$downloadedChrome = Get-ChildItem -LiteralPath $extractDir -Recurse -Filter "chrome.exe" |
    Select-Object -First 1

if (-not $downloadedChrome) {
    throw "Downloaded archive does not contain chrome.exe."
}

$sourceDir = Split-Path $downloadedChrome.FullName -Parent
Write-Host "Installing Chrome runtime to: $chromeDir"
Get-ChildItem -LiteralPath $sourceDir -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $chromeDir -Recurse -Force
}

if (-not (Test-Path $chromeExe)) {
    throw "Chrome install failed: $chromeExe was not found."
}

$versionOutput = & $chromeExe --version
Write-Host "Chrome runtime is ready:"
Write-Host "  $chromeExe"
Write-Host "  $versionOutput"
