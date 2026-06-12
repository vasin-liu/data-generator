# AI P2 — cost tracing, run report AI metrics, optional live Ollama IT.
param(
    [switch]$SkipLive,
    [switch]$SkipPlaywright,
    [string]$PlaywrightBaseUrl = 'http://127.0.0.1:9876',
    [string]$PlaywrightApiUrl = 'http://127.0.0.1:9876'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "AI P2 — Maven slice"
$testList = @(
    'AiSourceFactoryTests',
    'RunReportCollectorTests',
    'OllamaAiRuntimeBridgeTests'
)
if (-not $SkipLive) {
    $testList += 'OllamaAiRuntimeBridgeLiveIT'
}
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl "data-generator-service,data-generator-calcite" -am `
    "-Dtest=$($testList -join ',')" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "AI P2 Maven tests failed" }

Write-Step "AI P1 regression slice"
& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1') -SkipPlaywright
if ($LASTEXITCODE -ne 0) { throw "AI P1 regression failed" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] AI P2 Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

& (Join-Path $PSScriptRoot 'verify-ai-p1.ps1') `
    -PlaywrightBaseUrl $PlaywrightBaseUrl `
    -PlaywrightApiUrl $PlaywrightApiUrl
if ($LASTEXITCODE -ne 0) { throw "AI P1 Playwright regression failed" }

Write-Host ""
Write-Host "[SUCCESS] AI P2 verification passed." -ForegroundColor Green
