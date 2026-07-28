# Phase 13 UAT - Dameng live IT: opt-in JDBC upsert idempotency proof (DIAL-01).
# Fail-closed: exits non-zero with usage when the opt-in flag or connection env vars are missing
# (D-16) - an unconfigured run must never look like a passed UAT. See the Dameng live IT recipe in
# docs/template-v2-jdbc-sink-guide.md for the full setup and PASS/FAIL semantics. This wrapper runs
# only the opt-in ChunkedPipelineDamengUpsertIT slice; it is not part of the P0 merge gate (DIAL-03
# stays deferred) and is intentionally excluded from scripts/verify-harness.ps1.
param()

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

$requiredVars = @('DG_DM_IT', 'DG_DM_JDBC_URL', 'DG_DM_USER', 'DG_DM_PASSWORD')
$missing = @()
if ($env:DG_DM_IT -ne 'true') { $missing += 'DG_DM_IT (must equal "true")' }
if ([string]::IsNullOrWhiteSpace($env:DG_DM_JDBC_URL)) { $missing += 'DG_DM_JDBC_URL' }
if ([string]::IsNullOrWhiteSpace($env:DG_DM_USER)) { $missing += 'DG_DM_USER' }
if ([string]::IsNullOrWhiteSpace($env:DG_DM_PASSWORD)) { $missing += 'DG_DM_PASSWORD' }

if ($missing.Count -gt 0) {
    # Never echo the JDBC URL or password value here - only variable names (T-13-01).
    Write-Host "Dameng live IT is not configured. Missing/invalid:" -ForegroundColor Yellow
    foreach ($name in $missing) { Write-Host "  - $name" -ForegroundColor Yellow }
    Write-Host ""
    Write-Host "Required: $($requiredVars -join ', ')" -ForegroundColor Yellow
    Write-Host "See the Dameng live IT recipe: docs/template-v2-jdbc-sink-guide.md (Dameng live IT section)." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "==> Phase 13 UAT - Dameng live IT (ChunkedPipelineDamengUpsertIT)" -ForegroundColor Cyan

# Mirror the opt-in flag as a system property alongside the env var so the DamengTestSupport gate
# is satisfied regardless of how the calling shell exports environment variables to the Maven JVM.
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl data-generator-calcite -am `
    '-Ddm.it=true' `
    '-Dtest=ChunkedPipelineDamengUpsertIT' `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "Dameng live IT failed with exit code $code" }

Write-Host ""
Write-Host "[SUCCESS] Dameng live IT passed (chunked upsert idempotency)." -ForegroundColor Green
