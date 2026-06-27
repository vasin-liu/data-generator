# Phase 7 UAT — hot-reload isolation: focused Maven IT slice + governance E2E hot-reload scenario.
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright,
    [switch]$KeepContainer,
    [string]$ImageTag = 'dg-phase7-hot-reload-uat:local',
    [string]$ContainerName = 'dg-phase7-hot-reload-uat',
    [int]$HostPort = 9877
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
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

Write-Step "Phase 7 UAT — Hot-reload isolation (Maven + optional Playwright)"

Write-Step "Maven slice — HotReloadTests + ConnectionSnapshotIT"
$testList = @(
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
if ($code -ne 0) { throw "Phase 7 hot-reload Maven tests failed with exit code $code" }

if ($SkipPlaywright) {
    Write-Host "[SUCCESS] Phase 7 hot-reload Maven verification passed (-SkipPlaywright)." -ForegroundColor Green
    exit 0
}

$ServiceDir = Join-Path $RepoRoot 'data-generator-service'

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

Write-Step "Creating container $ContainerName on port $HostPort"
podman rm -f $ContainerName 2>$null | Out-Null
podman run -d --name $ContainerName -p "${HostPort}:9876" -e DG_SPRING_PROFILES_ACTIVE=e2e $ImageTag | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'podman run failed' }

try {
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "Playwright E2E — hot-reload isolation scenario only"
    Push-Location $WebDir
    try {
        if (-not (Test-Path 'node_modules')) {
            npm install
        }
        npx playwright install chromium
        $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
        $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
        npx playwright test e2e/specs/datasource-governance.spec.ts -g "hot-reload isolation"
        if ($LASTEXITCODE -ne 0) { throw 'Playwright hot-reload E2E failed' }
    } finally {
        Remove-Item Env:DG_E2E_BASE_URL -ErrorAction SilentlyContinue
        Remove-Item Env:DG_E2E_API_URL -ErrorAction SilentlyContinue
        Pop-Location
    }

    Write-Host ""
    Write-Host "[SUCCESS] Phase 7 hot-reload UAT automation passed." -ForegroundColor Green
} finally {
    if (-not $KeepContainer) {
        Write-Step "Stopping container $ContainerName"
        podman rm -f $ContainerName 2>$null | Out-Null
    }
}
