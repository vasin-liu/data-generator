# Unified CI-oriented harness: matrix-linked Maven tests + optional Playwright E2E.
param(
    [switch]$IncludeE2e,
    [switch]$UsePodman,
    [switch]$UseLocalService,
    [string]$MatrixFile = '.planning/test-matrix.yaml'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')
. (Join-Path $PSScriptRoot 'lib/test-matrix-summary.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Get-MatrixRows([string]$Path) {
    $yaml = Get-Content -Raw $Path
    return (Parse-MatrixRows $yaml)
}

Write-Step "Harness verify — embedded fast path (matrix-linked Maven tests)"

$matrixPath = if ([System.IO.Path]::IsPathRooted($MatrixFile)) { $MatrixFile } else { Join-Path $RepoRoot $MatrixFile }
if (-not (Test-Path $matrixPath)) { throw "Matrix file not found: $matrixPath" }

$rows = Get-MatrixRows $matrixPath
$mavenByModule = @{}
$allMavenClasses = New-Object System.Collections.Generic.List[string]

foreach ($row in $rows) {
    $links = @($row.linked_tests)
    $mavenLinks = $links | Where-Object { $_ -and $_ -notmatch 'e2e/specs/' }
    if ($mavenLinks.Count -eq 0) { continue }
    $module = $row.owner_module
    if (-not $module) { $module = 'data-generator-service' }
    if (-not $mavenByModule.ContainsKey($module)) { $mavenByModule[$module] = New-Object System.Collections.Generic.List[string] }
    foreach ($cls in $mavenLinks) {
        if ($mavenByModule[$module] -notcontains $cls) { [void]$mavenByModule[$module].Add($cls) }
        if ($allMavenClasses -notcontains $cls) { [void]$allMavenClasses.Add($cls) }
    }
}

$mavenExit = 0
if ($allMavenClasses.Count -gt 0) {
    $modules = ($mavenByModule.Keys | Sort-Object) -join ','
    $testList = ($allMavenClasses | Sort-Object) -join ','
    Write-Step "Running linked Maven tests: $testList"
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $mvnArgs = @('-pl', $modules, '-am', "-Dtest=$testList", '-Dsurefire.failIfNoSpecifiedTests=false', 'test')
        $mvnw = Join-Path $RepoRoot 'mvnw.cmd'
        $settings = Join-Path $RepoRoot '.mvn/settings-jdk25.xml'
        $jdk25Helper = Join-Path $RepoRoot 'mvnw-jdk25.ps1'
        if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath $jdk25Helper)) {
            # Reuse local JDK 25 path from helper without inheriting its Stop-on-stderr behavior.
            $helperText = Get-Content -Raw $jdk25Helper
            if ($helperText -match '\$jdkHome\s*=\s*"([^"]+)"') {
                $env:JAVA_HOME = $Matches[1]
                $env:Path = "$env:JAVA_HOME\bin;$env:Path"
            }
        }
        & $mvnw '-s' $settings @mvnArgs 2>&1 | ForEach-Object { Write-Host $_ }
        $mavenExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prevEap
    }
} else {
    Write-Host "No Maven linked_tests in matrix — skipping Surefire slice." -ForegroundColor Yellow
}

$summary = New-TestMatrixSummary -MatrixFile $matrixPath -RepoRoot $RepoRoot
Write-Host "Summary written to target/test-matrix-summary.json (rows=$($summary.rows.Count))" -ForegroundColor Green

$linkedFailed = $false
foreach ($entry in $summary.rows) {
    foreach ($lr in @($entry.linkedResults)) {
        if ($lr.outcome -eq 'failed') { $linkedFailed = $true }
    }
}

if ($IncludeE2e) {
    Write-Step "Optional Playwright E2E (-IncludeE2e)"
    if ($UseLocalService) {
        $webDir = Join-Path $RepoRoot 'data-generator-console-web'
        Push-Location $webDir
        try {
            npx playwright test e2e/specs/template-workflow.spec.ts e2e/specs/job-trigger.spec.ts
            if ($LASTEXITCODE -ne 0) { throw 'Playwright E2E failed (local service)' }
        } finally { Pop-Location }
    } else {
        $e2eArgs = @{}
        if ($UsePodman) { $e2eArgs.UsePodman = $true }
        & (Join-Path $RepoRoot 'scripts\e2e-podman.ps1') @e2eArgs
        if ($LASTEXITCODE -ne 0) { throw 'Playwright E2E failed (Podman)' }
    }
}

if ($mavenExit -ne 0 -or $linkedFailed) {
    Write-Host "Harness failed: mavenExit=$mavenExit linkedFailed=$linkedFailed" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] Harness verification passed." -ForegroundColor Green
exit 0
