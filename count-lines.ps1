<#
cd "H:\Babycode\FlowMind Agent\flowmind-agent"
powershell -ExecutionPolicy Bypass -File .\count-lines.ps1

powershell -ExecutionPolicy Bypass -File .\count-lines.ps1 -IncludeDocs

powershell -ExecutionPolicy Bypass -File .\count-lines.ps1 -Json
#>
param(
    [switch]$IncludeDocs,
    [switch]$IncludeGenerated,
    [switch]$Json
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path $PSScriptRoot

$excludeDirs = @(
    ".git",
    "node_modules",
    "target",
    "build",
    "dist",
    ".gradle",
    "__pycache__",
    ".idea",
    ".vscode"
)

$codeExt = @(
    ".java",
    ".vue",
    ".ts",
    ".js",
    ".css",
    ".scss",
    ".html",
    ".xml",
    ".yml",
    ".yaml",
    ".sql",
    ".gradle",
    ".properties",
    ".json",
    ".py",
    ".go"
)

$docExt = @(".md")
$specialCodeNames = @("pom.xml", "package.json", "vite.config.ts", "settings.gradle", "build.gradle", "requirements.txt")

function Test-IsExcludedFile {
    param([System.IO.FileInfo]$File)

    if ($IncludeGenerated) {
        return $false
    }

    $relative = $File.FullName.Substring($root.Path.Length).TrimStart("\", "/")
    $parts = $relative -split "[\\/]"
    foreach ($part in $parts) {
        if ($excludeDirs -contains $part) {
            return $true
        }
    }
    return $false
}

function Get-PartName {
    param([System.IO.FileInfo]$File)

    # Test directories must be checked before general module dirs
    if ($File.FullName -like "*\test\backend\*") { return "test-backend" }
    if ($File.FullName -like "*\test\frontend\*") { return "test-frontend" }
    if ($File.FullName -like "*\test\*") { return "test-other" }
    if ($File.FullName -like "*\backend\*") { return "backend" }
    if ($File.FullName -like "*\frontend\*") { return "frontend" }
    if ($File.FullName -like "*\desktop_fronted\*") { return "desktop_fronted" }
    if ($File.FullName -like "*\Desktop_fronted\*") { return "desktop_fronted" }
    if ($File.FullName -like "*\app\*") { return "android-app" }
    if ($File.FullName -like "*\docs\*") { return "docs-code" }
    if ($File.FullName -like "*\agent-capabilities\*") { return "agent-capabilities" }
    if ($File.FullName -like "*\skills\*") { return "skills" }
    if ($File.FullName -like "*\tools\*") { return "tools" }
    return "root"
}

function Count-Lines {
    param([System.IO.FileInfo[]]$Files)

    $sum = 0
    foreach ($file in $Files) {
        try {
            $sum += (Get-Content -LiteralPath $file.FullName -ErrorAction Stop | Measure-Object -Line).Lines
        } catch {
            Write-Warning "Skip unreadable file: $($file.FullName)"
        }
    }
    return $sum
}

$allFiles = Get-ChildItem -Path $root -Recurse -File | Where-Object { -not (Test-IsExcludedFile $_) }

$codeFiles = $allFiles | Where-Object {
    ($codeExt -contains $_.Extension.ToLower()) -or ($specialCodeNames -contains $_.Name)
}

$docFiles = $allFiles | Where-Object {
    $docExt -contains $_.Extension.ToLower()
}

$partStats = $codeFiles |
    Group-Object { Get-PartName $_ } |
    Sort-Object Name |
    ForEach-Object {
        [pscustomobject]@{
            part  = $_.Name
            files = $_.Count
            lines = Count-Lines $_.Group
        }
    }

$extensionStats = $codeFiles |
    Group-Object { if ($_.Extension) { $_.Extension.ToLower() } else { "(none)" } } |
    Sort-Object Name |
    ForEach-Object {
        [pscustomobject]@{
            extension = $_.Name
            files     = $_.Count
            lines     = Count-Lines $_.Group
        }
    }

$codeTotal = [pscustomobject]@{
    scope = "source-code"
    files = $codeFiles.Count
    lines = Count-Lines $codeFiles
}

$docTotal = [pscustomobject]@{
    scope = "markdown-docs"
    files = $docFiles.Count
    lines = Count-Lines $docFiles
}

$result = [pscustomobject]@{
    root              = $root.Path
    includeDocs       = [bool]$IncludeDocs
    includeGenerated  = [bool]$IncludeGenerated
    excludedDirs      = if ($IncludeGenerated) { @() } else { $excludeDirs }
    codeTotal         = $codeTotal
    markdownTotal     = $docTotal
    combinedLineCount = if ($IncludeDocs) { $codeTotal.lines + $docTotal.lines } else { $codeTotal.lines }
    parts             = $partStats
    extensions        = $extensionStats
}

if ($Json) {
    $result | ConvertTo-Json -Depth 8
    exit 0
}

Write-Host ""
Write-Host "FlowMind Agent Code Line Statistics" -ForegroundColor Cyan
Write-Host "Root: $($root.Path)"
Write-Host ""
Write-Host ("Source code:    {0,6} files, {1,8} lines" -f $codeTotal.files, $codeTotal.lines) -ForegroundColor Green
Write-Host ("Markdown docs:  {0,6} files, {1,8} lines" -f $docTotal.files, $docTotal.lines) -ForegroundColor Green

# Test stats
$testParts = $partStats | Where-Object { $_.part -like "test-*" }
$testBackend = ($testParts | Where-Object { $_.part -eq "test-backend" } | Select-Object -First 1)
$testFrontend = ($testParts | Where-Object { $_.part -eq "test-frontend" } | Select-Object -First 1)
$testBackendLines = if ($testBackend) { $testBackend.lines } else { 0 }
$testFrontendLines = if ($testFrontend) { $testFrontend.lines } else { 0 }
$testTotal = $testBackendLines + $testFrontendLines

Write-Host ("Tests backend:  {0,6} files, {1,8} lines" -f ($testBackend.files -as [int]), $testBackendLines) -ForegroundColor Yellow
Write-Host ("Tests frontend: {0,6} files, {1,8} lines" -f ($testFrontend.files -as [int]), $testFrontendLines) -ForegroundColor Yellow
Write-Host ("Tests combined: {0,6} files, {1,8} lines" -f (($testParts | Measure-Object -Property files -Sum).Sum), $testTotal) -ForegroundColor Yellow

if ($docTotal.lines -gt 0) {
    $nonTestCode = $codeTotal.lines - $testTotal
    if (-not $IncludeDocs) {
        Write-Host ("Non-test code:  {0,6} files, {1,8} lines" -f ($codeTotal.files - ($testParts | Measure-Object -Property files -Sum).Sum), $nonTestCode) -ForegroundColor Green
    }
}

if ($IncludeDocs) {
    Write-Host ("Combined:              {0,8} lines" -f $result.combinedLineCount) -ForegroundColor Yellow
}
Write-Host ""

Write-Host "By module:" -ForegroundColor Cyan
$partStats | Format-Table -AutoSize

Write-Host "By extension:" -ForegroundColor Cyan
$extensionStats | Format-Table -AutoSize

Write-Host "Options:" -ForegroundColor DarkGray
Write-Host "  .\count-lines.ps1              Count source code only" -ForegroundColor DarkGray
Write-Host "  .\count-lines.ps1 -IncludeDocs Include Markdown docs in combined total" -ForegroundColor DarkGray
Write-Host "  .\count-lines.ps1 -Json        Output machine-readable JSON" -ForegroundColor DarkGray
Write-Host ""
