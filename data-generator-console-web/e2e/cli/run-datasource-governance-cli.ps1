# Playwright CLI automation for Phase 7 UAT — datasource governance HEALTHY list + DEGRADED detail (D-28).
param(
    [string]$BaseUrl = $(if ($env:DG_E2E_BASE_URL) { $env:DG_E2E_BASE_URL } else { 'http://127.0.0.1:9876/console/' }),
    [string]$ApiUrl = $(if ($env:DG_E2E_API_URL) { $env:DG_E2E_API_URL } else { 'http://127.0.0.1:9876' }),
    [string]$Session = 'dg-phase7-governance'
)

$ErrorActionPreference = 'Stop'
$WebDir = Split-Path -Parent $PSScriptRoot
Push-Location $WebDir
try {
    if (-not (Test-Path 'node_modules')) {
        npm install
    }
    npx playwright install chromium

    $consoleUrl = $BaseUrl.TrimEnd('/') + '/datasources'
    $sessionArgs = @("-s=$Session")
    $apiBase = $ApiUrl.TrimEnd('/')
    $degradedName = "e2e-cli-degraded-$(Get-Date -Format 'yyyyMMddHHmmss')"
    $healthyName = "e2e-cli-healthy-$(Get-Date -Format 'yyyyMMddHHmmss')"
    $snapshotDir = Join-Path $WebDir 'e2e/snapshots/governance'
    if (-not (Test-Path $snapshotDir)) {
        New-Item -ItemType Directory -Path $snapshotDir -Force | Out-Null
    }

    function Invoke-CliEval([string]$Expression) {
        return (& npx playwright-cli @sessionArgs eval $Expression 2>&1 | Out-String)
    }

    function Assert-CliTrue([string]$Output, [string]$Label) {
        if ($Output -notmatch 'Result\s+true') {
            throw "$Label failed (output: $Output)"
        }
    }

    Write-Host "==> REST API seed HEALTHY + DEGRADED catalog rows" -ForegroundColor Cyan
    $healthyForm = @{
        name            = $healthyName
        url             = "jdbc:h2:mem:$healthyName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        username        = 'sa'
        password        = ''
        driverClassName = 'org.h2.Driver'
    }
    $healthySave = Invoke-RestMethod -Method POST -Uri "$apiBase/api/datasources" -Body $healthyForm
    if (-not $healthySave.success) { throw "Healthy save failed: $($healthySave.message)" }

    $degradedForm = @{
        name            = $degradedName
        url             = "jdbc:h2:mem:$degradedName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        username        = 'sa'
        password        = ''
        driverClassName = 'org.h2.Driver'
    }
    $degradedSave = Invoke-RestMethod -Method POST -Uri "$apiBase/api/datasources" -Body $degradedForm
    if (-not $degradedSave.success) { throw "Degraded seed save failed: $($degradedSave.message)" }
    $brokenForm = @{
        name            = $degradedName
        url             = "jdbc:h2:mem:$degradedName-broken;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        username        = 'sa'
        password        = ''
        driverClassName = 'com.example.NonexistentDriver'
    }
    $brokenSave = Invoke-RestMethod -Method POST -Uri "$apiBase/api/datasources" -Body $brokenForm
    if (-not $brokenSave.success) { throw "Degraded reload save failed: $($brokenSave.message)" }

    Write-Host "==> playwright-cli open $consoleUrl" -ForegroundColor Cyan
    & npx playwright-cli @sessionArgs open $consoleUrl | Out-String | Write-Host
    if ($LASTEXITCODE -ne 0) { throw 'playwright-cli open failed' }

    Write-Host "==> playwright-cli eval: HEALTHY badge visible in catalog" -ForegroundColor Cyan
    $healthyBadge = Invoke-CliEval "() => { const t = document.querySelector('[data-testid=""datasources-catalog-table""]')?.textContent || ''; return t.includes('$healthyName') && (/HEALTHY|健康/i.test(t)); }"
    Assert-CliTrue $healthyBadge 'HEALTHY catalog badge'

    Write-Host "==> playwright-cli snapshot: HEALTHY list baseline" -ForegroundColor Cyan
    $healthyShot = Join-Path $snapshotDir 'datasource-list-healthy-cli.png'
    & npx playwright-cli @sessionArgs screenshot --filename=$healthyShot | Out-String | Write-Host
    if ($LASTEXITCODE -ne 0) { throw 'playwright-cli HEALTHY screenshot failed' }

    Write-Host "==> playwright-cli eval: DEGRADED badge visible" -ForegroundColor Cyan
    $degradedBadge = Invoke-CliEval "() => { const t = document.querySelector('[data-testid=""datasources-catalog-table""]')?.textContent || ''; return t.includes('$degradedName') && (/DEGRADED|降级/i.test(t)); }"
    Assert-CliTrue $degradedBadge 'DEGRADED catalog badge'

    Write-Host "==> playwright-cli eval: open DEGRADED detail drawer" -ForegroundColor Cyan
    $openDetail = Invoke-CliEval "() => { const rows = [...document.querySelectorAll('[data-testid=""datasources-catalog-table""] button')]; const btn = rows.find(b => (b.textContent || '').match(/detail|详情/i)); if (btn) { btn.click(); return true; } return false; }"
    Assert-CliTrue $openDetail 'open catalog detail drawer'

    Write-Host "==> playwright-cli snapshot: DEGRADED detail panel" -ForegroundColor Cyan
    $degradedShot = Join-Path $snapshotDir 'datasource-detail-degraded-cli.png'
    & npx playwright-cli @sessionArgs screenshot --filename=$degradedShot | Out-String | Write-Host
    if ($LASTEXITCODE -ne 0) { throw 'playwright-cli DEGRADED screenshot failed' }

    Write-Host "[OK] playwright-cli governance snapshot checks passed." -ForegroundColor Green
    Write-Host "Artifacts: $healthyShot, $degradedShot"
} finally {
    try {
        & npx playwright-cli "-s=$Session" close 2>$null | Out-Null
    } catch {
        # ignore cleanup errors
    }
    Pop-Location
}
