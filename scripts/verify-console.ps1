# Fixed console verification: unit tests + frontend build + Podman UI/E2E automation.
param(
    [switch]$SkipBuild,
    [switch]$SkipUnit,
    [switch]$SkipE2e,
    [switch]$KeepContainer,
    [string]$ImageTag = 'dg-e2e:local',
    [string]$ContainerName = 'dg-e2e',
    [int]$HostPort = 9876
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$WebDir = Join-Path $RepoRoot 'data-generator-console-web'

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "Console verification pipeline"

if (-not $SkipUnit) {
    Write-Step "Phase 1/3 — Backend unit tests"
    $unitArgs = @()
    if (-not $SkipBuild) { $unitArgs += '-IncludeWebBuild' }
    & (Join-Path $RepoRoot 'scripts\verify-console-unit.ps1') @unitArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host "Skipping unit tests (-SkipUnit)" -ForegroundColor Yellow
}

Write-Step "Phase 2/3 — Frontend typecheck and production build"
Push-Location $WebDir
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
} finally {
    Pop-Location
}

if ($SkipE2e) {
    Write-Host "Skipping UI/E2E automation (-SkipE2e)" -ForegroundColor Yellow
    Write-Host "[SUCCESS] Console verification (unit + build) completed." -ForegroundColor Green
    exit 0
}

Write-Step "Phase 3/3 — Podman UI automation (Playwright E2E)"
$e2eArgs = @{
    ImageTag      = $ImageTag
    ContainerName = $ContainerName
    HostPort      = $HostPort
}
if ($SkipBuild) { $e2eArgs.SkipBuild = $true }
if ($KeepContainer) { $e2eArgs.KeepContainer = $true }

& (Join-Path $RepoRoot 'scripts\e2e-podman.ps1') @e2eArgs
exit $LASTEXITCODE
