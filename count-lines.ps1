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

# New document-oriented statistics output.
# This block intentionally exits before the legacy detailed module/extension output below.
$scaleExcludeDirs = @(
    ".git", ".gocache", ".runtime", ".xhs-chrome-profile", ".gradle",
    ".idea", ".vscode", ".venv", "__pycache__", "node_modules",
    "target", "build", "dist", "out", ".pytest_cache"
)

$scaleSourceExt = @(
    ".java", ".vue", ".ts", ".js", ".css", ".scss", ".html",
    ".py", ".sql", ".xml", ".yml", ".yaml", ".gradle",
    ".properties", ".json", ".ps1", ".sh", ".cmd", ".bat",
    ".go", ".kt", ".kts", ".md"
)

function Get-ScaleRelativePath {
    param([System.IO.FileInfo]$File)
    return $File.FullName.Substring($root.Path.Length).TrimStart("\", "/")
}

function Test-ScaleExcludedFile {
    param([System.IO.FileInfo]$File)
    if ($IncludeGenerated) { return $false }
    $parts = (Get-ScaleRelativePath $File) -split "[\\/]"
    foreach ($part in $parts) {
        if ($scaleExcludeDirs -contains $part) { return $true }
    }
    return $false
}

function Test-ScaleUnderPath {
    param(
        [System.IO.FileInfo]$File,
        [string]$Prefix
    )
    $relative = (Get-ScaleRelativePath $File).Replace("\", "/")
    return $relative.StartsWith($Prefix.Trim("/").Replace("\", "/") + "/")
}

function Count-ScaleLines {
    param([System.IO.FileInfo[]]$Files)
    $sum = 0
    foreach ($file in $Files) {
        try {
            $sum += (Get-Content -LiteralPath $file.FullName -ErrorAction Stop | Measure-Object -Line).Lines
        } catch {
            Write-Warning "Skip unreadable file: $($file.FullName)"
        }
    }
    return [int]$sum
}

function Format-ScaleApprox {
    param([int]$Number)
    return "~{0:N0} 行" -f $Number
}

function Join-ScaleNonEmpty {
    param([string[]]$Items)
    return (($Items | Where-Object { $_ -and $_.Trim().Length -gt 0 }) -join " + ")
}

function Get-ScaleExtLines {
    param(
        [System.IO.FileInfo[]]$Files,
        [string[]]$Extensions
    )
    $items = @()
    foreach ($ext in $Extensions) {
        $matched = @($Files | Where-Object { $_.Extension.ToLowerInvariant() -eq $ext })
        if ($matched.Count -gt 0) {
            $items += [pscustomobject]@{
                extension = $ext
                files     = $matched.Count
                lines     = Count-ScaleLines $matched
            }
        }
    }
    return $items
}

function Get-ScaleModuleCount {
    $pom = Join-Path $root.Path "backend/pom.xml"
    if (-not (Test-Path $pom)) { return 0 }
    $matches = Select-String -LiteralPath $pom -Pattern "<module>" -AllMatches
    return ($matches.Matches | Measure-Object).Count
}

function New-ScaleRow {
    param(
        [string]$Category,
        [int]$Lines,
        [string]$Method,
        [int]$Files
    )
    return [pscustomobject]@{
        category = $Category
        displayLines = Format-ScaleApprox $Lines
        method = $Method
        files = $Files
        lines = $Lines
    }
}

$trackedScaleFiles = @()
try {
    Push-Location $root
    $trackedScaleFiles = @(git ls-files 2>$null | ForEach-Object {
        $full = Join-Path $root.Path $_
        if (Test-Path $full) { Get-Item -LiteralPath $full }
    } | Where-Object { -not (Test-ScaleExcludedFile $_) })
} catch {
    $trackedScaleFiles = @()
} finally {
    Pop-Location
}

$allScaleFiles = @(Get-ChildItem -Path $root -Recurse -File | Where-Object { -not (Test-ScaleExcludedFile $_) })
$scaleFiles = if ($trackedScaleFiles.Count -gt 0) { $trackedScaleFiles } else { $allScaleFiles }
$sourceScaleFiles = @($scaleFiles | Where-Object { $scaleSourceExt -contains $_.Extension.ToLowerInvariant() })

$frontendScaleFiles = @($sourceScaleFiles | Where-Object {
    (Test-ScaleUnderPath $_ "frontend") -and
    (@(".vue", ".ts", ".js", ".css", ".scss", ".html") -contains $_.Extension.ToLowerInvariant())
})
$javaScaleFiles = @($sourceScaleFiles | Where-Object {
    (Test-ScaleUnderPath $_ "backend") -and $_.Extension.ToLowerInvariant() -eq ".java"
})
$desktopScaleFiles = @($sourceScaleFiles | Where-Object {
    (Test-ScaleUnderPath $_ "desktop_fronted") -and $_.Extension.ToLowerInvariant() -eq ".py"
})
$androidScaleFiles = @($sourceScaleFiles | Where-Object {
    (Test-ScaleUnderPath $_ "app") -and $_.Extension.ToLowerInvariant() -eq ".java"
})
$sqlScaleFiles = @($sourceScaleFiles | Where-Object { $_.Extension.ToLowerInvariant() -eq ".sql" })
$markdownScaleFiles = @($sourceScaleFiles | Where-Object { $_.Extension.ToLowerInvariant() -eq ".md" })
$configScaleFiles = @($sourceScaleFiles | Where-Object {
    @(".xml", ".yml", ".yaml", ".gradle", ".properties", ".json", ".ps1", ".sh", ".cmd", ".bat", ".kts") -contains $_.Extension.ToLowerInvariant()
})
$testScaleFiles = @($sourceScaleFiles | Where-Object {
    $relative = (Get-ScaleRelativePath $_).Replace("\", "/")
    $relative -like "test/*" -or
    $relative -like "backend/*/src/test/*" -or
    $relative -like "frontend/*test*" -or
    $_.Name -match "(?i)(test|spec)"
})

$frontendExtStats = Get-ScaleExtLines $frontendScaleFiles @(".vue", ".ts", ".js", ".css", ".scss", ".html")
$frontendMethod = if ($frontendExtStats.Count -gt 0) {
    Join-ScaleNonEmpty ($frontendExtStats | ForEach-Object {
        $name = switch ($_.extension) {
            ".vue" { "Vue" }
            ".ts" { "TypeScript" }
            ".js" { "JavaScript" }
            ".css" { "CSS" }
            ".scss" { "SCSS" }
            ".html" { "HTML" }
            default { $_.extension }
        }
        "{0} {1:N0}" -f $name, $_.lines
    })
} else {
    "frontend 目录下 Vue/TS/JS/CSS/HTML 文件"
}

$desktopCoreNames = @("views.py", "widgets.py", "api.py", "styles.py", "fallback_data.py", "tk_app.py", "run.py", "run_tk.py")
$desktopNamed = @($desktopScaleFiles | Where-Object { $desktopCoreNames -contains $_.Name })
$desktopMethod = if ($desktopNamed.Count -gt 0) {
    (Join-ScaleNonEmpty ($desktopNamed | Sort-Object Name | ForEach-Object { $_.Name })) + " 等 Python 文件"
} else {
    "desktop_fronted 目录下 Python 源文件"
}

$androidMethod = if ($androidScaleFiles.Count -gt 0) {
    (Join-ScaleNonEmpty ($androidScaleFiles | Sort-Object Name | Select-Object -ExpandProperty Name -Unique)) + " 等 Android Java 文件"
} else {
    "app 目录下 Android Java 文件"
}

$sqlMethod = if ($sqlScaleFiles.Count -gt 0) {
    (Join-ScaleNonEmpty ($sqlScaleFiles | Sort-Object Name | Select-Object -ExpandProperty Name -Unique)) + " 等 SQL 脚本"
} else {
    "backend/sql 及各模块 SQL 脚本"
}

$scaleRows = @()
$scaleRows += New-ScaleRow "前端代码量（Vue/TS/JS/CSS）" (Count-ScaleLines $frontendScaleFiles) $frontendMethod $frontendScaleFiles.Count
$scaleRows += New-ScaleRow "服务端代码量（Java）" (Count-ScaleLines $javaScaleFiles) ("{0} 个 Java 源文件，涵盖 {1} 个 Maven 模块" -f $javaScaleFiles.Count, (Get-ScaleModuleCount)) $javaScaleFiles.Count
$scaleRows += New-ScaleRow "桌面客户端代码量（Python）" (Count-ScaleLines $desktopScaleFiles) $desktopMethod $desktopScaleFiles.Count
$scaleRows += New-ScaleRow "移动端代码量（Android Java）" (Count-ScaleLines $androidScaleFiles) $androidMethod $androidScaleFiles.Count
$scaleRows += New-ScaleRow "数据库脚本（SQL）" (Count-ScaleLines $sqlScaleFiles) $sqlMethod $sqlScaleFiles.Count
$scaleRows += New-ScaleRow "文档与 Markdown" (Count-ScaleLines $markdownScaleFiles) "所有 .md 文件（不含依赖、构建产物和运行缓存）" $markdownScaleFiles.Count
$scaleRows += New-ScaleRow "配置与脚本（XML/YAML/Gradle/PS1/Sh）" (Count-ScaleLines $configScaleFiles) "pom.xml、application.yml、build.gradle、.ps1、.sh、JSON 等配置和脚本" $configScaleFiles.Count
if ($testScaleFiles.Count -gt 0) {
    $scaleRows += New-ScaleRow "测试与验证代码" (Count-ScaleLines $testScaleFiles) "test 目录、src/test 目录以及 *test* / *spec* 文件" $testScaleFiles.Count
}
$scaleTotalLines = Count-ScaleLines $sourceScaleFiles
$scaleSourceName = if ($trackedScaleFiles.Count -gt 0) { "git ls-files" } else { "Get-ChildItem" }
$scaleRows += New-ScaleRow "整个系统总代码量" $scaleTotalLines ("使用 {0} 统计已纳入当前工程的源文件，排除 node_modules、target、build、dist、.runtime 等目录（{1} 个文件）" -f $scaleSourceName, $sourceScaleFiles.Count) $sourceScaleFiles.Count

$scaleResult = [pscustomobject]@{
    root = $root.Path
    includeGenerated = [bool]$IncludeGenerated
    fileSource = $scaleSourceName
    excludedDirs = if ($IncludeGenerated) { @() } else { $scaleExcludeDirs }
    rows = $scaleRows
    totalFiles = $sourceScaleFiles.Count
    totalLines = $scaleTotalLines
}

if ($Json) {
    $scaleResult | ConvertTo-Json -Depth 8
    exit 0
}

Write-Host ""
Write-Host "12.5  系统规模" -ForegroundColor Cyan
Write-Host ""
Write-Host "| 类别 | 代码量 | 统计方式 |"
Write-Host "|---|---:|---|"
foreach ($row in $scaleRows) {
    Write-Host ("| {0} | {1} | {2} |" -f $row.category, $row.displayLines, $row.method)
}
Write-Host ""
Write-Host "统计说明：" -ForegroundColor Cyan
Write-Host ("- 统计根目录：{0}" -f $root.Path)
Write-Host ("- 文件来源：{0}" -f $scaleSourceName)
if (-not $IncludeGenerated) {
    Write-Host ("- 默认排除目录：{0}" -f ($scaleExcludeDirs -join ", "))
}
Write-Host "- 代码量为物理行数，空行和注释均计入；适合课程设计文档中的系统规模说明。"
Write-Host ""
exit 0

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
