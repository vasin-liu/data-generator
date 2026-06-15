# AI P3 — composite bridge routing, OpenAI-compatible provider, catalog entries.
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

Write-Step "AI P3 — Maven slice"
$testList = @(
    'CompositeAiRuntimeBridgeTests',
    'OpenAiCompatibleRuntimeBridgeTests',
    'OllamaAiRuntimeBridgeTests',
    'AiCatalogServiceTest',
    'Pf4jRuntimeConfigTests'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "AI P3 Maven tests failed" }

Write-Step "AI P2 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p2.ps1') -SkipLive -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P2 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P3 Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1')
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P3 verification passed." -ForegroundColor Green
