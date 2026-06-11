# Pack 3 — execution reliability: sink retry, partial-success metrics, streaming scenario IT.
param(
    [switch]$SkipPlaywright,
    [string]$PlaywrightBaseUrl = 'http://127.0.0.1:9876',
    [string]$PlaywrightApiUrl = 'http://127.0.0.1:9876'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$WebDir = Join-Path $RepoRoot 'data-generator-console-web'
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "Pack 3 execution reliability — Maven slice"
$testList = @(
    'SinkRetryPolicyTests',
    'RunReportCollectorTests',
    'V2ScenarioTemplateIT',
    'V2ScenarioCatalogServiceTest'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "Execution reliability Maven tests failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] Execution reliability Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

if (-not (Test-Path -LiteralPath (Join-Path $WebDir 'node_modules'))) {
    Write-Step "Installing console web dependencies"
    Push-Location $WebDir
    try {
        npm ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci failed" }
    } finally {
        Pop-Location
    }
}

Write-Step "Playwright — execution-reliability.spec.ts (requires running console on $PlaywrightApiUrl)"
Push-Location $WebDir
try {
    $env:DG_E2E_BASE_URL = "$PlaywrightBaseUrl/console/"
    $env:DG_E2E_API_URL = $PlaywrightApiUrl
    npx playwright test e2e/specs/execution-reliability.spec.ts
    if ($LASTEXITCODE -ne 0) { throw "Playwright execution-reliability E2E failed" }
} finally {
    Remove-Item Env:DG_E2E_BASE_URL -ErrorAction SilentlyContinue
    Remove-Item Env:DG_E2E_API_URL -ErrorAction SilentlyContinue
    Pop-Location
}

Write-Host ""
Write-Host "[SUCCESS] Pack 3 execution reliability verification passed." -ForegroundColor Green
