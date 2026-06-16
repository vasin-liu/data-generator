# AI P5 — provider rate limits for remote AI runtime bridges.
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

Write-Step "AI P5 — Maven slice"
$testList = @(
    'AiRateLimiterTests',
    'OllamaAiRuntimeBridgeTests',
    'OpenAiCompatibleRuntimeBridgeTests',
    'Pf4jRuntimeConfigTests'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "AI P5 Maven tests failed" }

Write-Step "AI P4 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p4.ps1') -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P4 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P5 Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1')
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P5 verification passed." -ForegroundColor Green
