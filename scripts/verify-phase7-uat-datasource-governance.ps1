# Phase 7 UAT — datasource governance: Maven IT slice + optional Podman Playwright + playwright-cli.
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright,
    [switch]$KeepContainer,
    [string]$ImageTag = 'dg-phase7-governance-uat:local',
    [string]$ContainerName = 'dg-phase7-governance-uat',
    [int]$HostPort = 9876,
    [string]$SpringProfiles = 'e2e,staging'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$ServiceDir = Join-Path $RepoRoot 'data-generator-service'
$WebDir = Join-Path $RepoRoot 'data-generator-console-web'
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Wait-Health([string]$Url, [int]$TimeoutSec = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -eq 200 -and $resp.Content -match '"opcode"\s*:\s*0') {
                Write-Host "[OK] Health check passed: $Url"
                return
            }
        } catch {
            # retry
        }
        Start-Sleep -Seconds 3
    }
    throw "Health check timed out: $Url"
}

Write-Step "Phase 7 UAT — Datasource governance (Maven + Playwright + playwright-cli)"

Write-Step "Maven slice — DatasourceGovernanceIT, audit, catalog test, hot-reload, snapshot IT"
$testList = @(
    'DatasourceGovernanceIT',
    'DatasourceAuditTests',
    'ConnectionCatalogTestTests',
    'HotReloadTests',
    'ConnectionSnapshotIT'
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
if ($code -ne 0) { throw "Phase 7 governance Maven tests failed with exit code $code" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] Phase 7 datasource governance Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

Write-Step "Checking Podman"
podman info | Out-Null

if (-not $SkipBuild) {
    Write-Step "Building service package (frontend + assembly)"
    Push-Location $RepoRoot
    try {
        $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-console-web -DskipTests package
        if ($code -ne 0) { throw "Console web build failed with exit code $code" }
        $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-service -am -DskipTests package
        if ($code -ne 0) { throw "Maven package failed with exit code $code" }
    } finally {
        Pop-Location
    }
}

Write-Step "Building Podman image $ImageTag"
Push-Location $ServiceDir
try {
    podman build -t $ImageTag -f Containerfile .
    if ($LASTEXITCODE -ne 0) { throw 'podman build failed' }
} finally {
    Pop-Location
}

Write-Step "Creating container $ContainerName on port $HostPort (profiles: $SpringProfiles)"
podman rm -f $ContainerName 2>$null | Out-Null
podman run -d --name $ContainerName -p "${HostPort}:9876" `
    -e "DG_SPRING_PROFILES_ACTIVE=$SpringProfiles" `
    $ImageTag | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'podman run failed' }

try {
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "Playwright E2E — datasource-governance.spec.ts"
    Push-Location $WebDir
    try {
        if (-not (Test-Path 'node_modules')) {
            npm install
        }
        npx playwright install chromium
        $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
        $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
        if ($SpringProfiles -match 'staging') {
            $env:DG_E2E_GOVERNANCE_STAGING = 'true'
        }
        npx playwright test e2e/specs/datasource-governance.spec.ts
        if ($LASTEXITCODE -ne 0) { throw 'Playwright E2E failed' }
    } finally {
        Remove-Item Env:DG_E2E_BASE_URL -ErrorAction SilentlyContinue
        Remove-Item Env:DG_E2E_API_URL -ErrorAction SilentlyContinue
        Remove-Item Env:DG_E2E_GOVERNANCE_STAGING -ErrorAction SilentlyContinue
        Pop-Location
    }

    Write-Step "playwright-cli governance snapshots"
    $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
    $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
    & (Join-Path $WebDir 'e2e/cli/run-datasource-governance-cli.ps1') -BaseUrl $env:DG_E2E_BASE_URL -ApiUrl $env:DG_E2E_API_URL
    if ($LASTEXITCODE -ne 0) { throw 'playwright-cli automation failed' }

    Write-Host ""
    Write-Host "[SUCCESS] Phase 7 datasource governance UAT automation passed." -ForegroundColor Green
    Write-Host "Podman image: $ImageTag"
    Write-Host "Container: $ContainerName (port $HostPort)"
} finally {
    if (-not $KeepContainer) {
        Write-Step "Stopping container $ContainerName"
        podman rm -f $ContainerName 2>$null | Out-Null
    } else {
        Write-Host "Container kept running: podman logs -f $ContainerName"
        Write-Host "Image retained in Podman: podman images $ImageTag"
    }
}
