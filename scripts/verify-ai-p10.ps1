# AI P10 — tenant-scoped AI quotas and quota notification webhooks.
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

Write-Step "AI P10 — Maven slice"
$testList = @(
    'AiQuotaServiceIntegrationTests',
    'AiQuotaWebhookNotifierTests',
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
if ($code -ne 0) { throw "AI P10 Maven tests failed" }

Write-Step "Console web build"
Push-Location (Join-Path $RepoRoot 'data-generator-console-web')
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Console web build failed" }
} finally {
    Pop-Location
}

Write-Step "AI P9 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p9.ps1') -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P9 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P10 Maven + web verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1')
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P10 verification passed." -ForegroundColor Green
