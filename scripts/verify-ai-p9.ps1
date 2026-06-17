# AI P9 — scoped AI quotas (provider/template) and quota alert audit hooks.
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

Write-Step "AI P9 — Maven slice"
$testList = @(
    'AiQuotaServiceIntegrationTests',
    'OllamaAiRuntimeBridgeTests',
    'OpenAiCompatibleRuntimeBridgeTests',
    'ConsoleAiCatalogControllerTest',
    'Pf4jRuntimeConfigTests'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "AI P9 Maven tests failed" }

Write-Step "Console web build"
Push-Location (Join-Path $RepoRoot 'data-generator-console-web')
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Console web build failed" }
} finally {
    Pop-Location
}

Write-Step "AI P8 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p8.ps1') -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P8 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P9 Maven + web verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1')
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P9 verification passed." -ForegroundColor Green
