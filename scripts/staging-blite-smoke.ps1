# B-lite staging smoke: RBAC unit/integration slice + optional live staging runtime check.
param(
    [string]$BaseUrl = "",
    [switch]$SkipMaven
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "B-lite verification (RBAC + publish gate unit slice)"
if (-not $SkipMaven) {
    & (Join-Path $RepoRoot 'scripts\verify-console-unit.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host "Skipping Maven (-SkipMaven)" -ForegroundColor Yellow
}

if ($BaseUrl) {
    Write-Step "REST: staging runtime flags on $BaseUrl"
    $headers = @{ 'X-Console-Role' = 'VIEWER' }
    $runtimeUri = "$($BaseUrl.TrimEnd('/'))/api/console/runtime"
    $runtime = Invoke-RestMethod -Method GET -Uri $runtimeUri -Headers $headers
    if (-not $runtime.data.consoleSecurityEnabled) {
        throw "Expected consoleSecurityEnabled=true on staging runtime"
    }
    Write-Host "[OK] consoleSecurityEnabled=true roles=$($runtime.data.consoleRoles -join ',')" -ForegroundColor Green

    Write-Step "REST: missing role header is rejected"
    try {
        Invoke-RestMethod -Method GET -Uri "$($BaseUrl.TrimEnd('/'))/api/templates/scenarios" -ErrorAction Stop | Out-Null
        throw "Expected 403 without X-Console-Role"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 403) {
            throw
        }
    }
    Write-Host "[OK] RBAC rejects unauthenticated console API" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Live staging check skipped (pass -BaseUrl http://host:port after starting with profile staging)." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[SUCCESS] B-lite staging smoke completed." -ForegroundColor Green
