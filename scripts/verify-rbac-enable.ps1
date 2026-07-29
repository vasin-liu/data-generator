# Phase 16 SEC-01 — RBAC enable-path Maven slice + optional Playwright (Playwright leg in plan 16-03).
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "Phase 16 SEC-01 — RBAC enable-path (Maven slice)"

$testList = @(
    'ConsoleSecurityDefaultOffIT',
    'ConsoleAuthorizationIntegrationIT',
    'ConsoleAuthorizationFilterTest',
    'ConsoleUdfAuthorizationFilterTest'
) -join ','

$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $code = Invoke-RepoMaven -RepoRoot $RepoRoot `
        -pl data-generator-service -am `
        "-Dtest=$testList" `
        '-Dsurefire.failIfNoSpecifiedTests=false' `
        test
} finally {
    $ErrorActionPreference = $prevEap
}
if ($code -ne 0) { throw "SEC-01 RBAC Maven slice failed with exit code $code" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] SEC-01 RBAC enable-path Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

Write-Host "[INFO] Playwright/Podman RBAC E2E leg ships in plan 16-03; Maven slice passed." -ForegroundColor Yellow
if ($SkipBuild) {
    Write-Host "[INFO] -SkipBuild reserved for plan 16-03 Podman build; no-op in wave 1." -ForegroundColor Yellow
}
Write-Host "[SUCCESS] SEC-01 RBAC enable-path Maven verification passed." -ForegroundColor Green
exit 0
