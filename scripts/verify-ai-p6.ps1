# AI P6 — richer AI cost pricing models (USD estimates from token usage).
param(
    [switch]$SkipPlaywright
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "AI P6 — Maven slice"
$testList = @(
    'AiPricingServiceTests',
    'AiUsageServiceTests',
    'RunReportCollectorTests',
    'ConsoleAiCatalogControllerTest'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-common/data-generator-core" -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "AI P6 Maven tests failed" }

Write-Step "Console web build"
Push-Location (Join-Path $RepoRoot 'data-generator-console-web')
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Console web build failed" }
} finally {
    Pop-Location
}

Write-Step "AI P5 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p5.ps1') -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P5 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P6 Maven + web verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1')
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P6 verification passed." -ForegroundColor Green
