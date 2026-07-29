# Phase 16 SEC-01 — RBAC enable-path: Maven slice (primary) + optional Podman Playwright RBAC specs (D-06, D-08).
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright,
    [switch]$KeepContainer,
    [string]$ImageTag = 'dg-rbac-enable-verify:local',
    [string]$ContainerName = 'dg-rbac-enable-verify',
    [int]$HostPort = 9876
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

Write-Step "Creating container $ContainerName on port $HostPort (profile: e2e-rbac)"
podman rm -f $ContainerName 2>$null | Out-Null
podman run -d --name $ContainerName -p "${HostPort}:9876" `
    -e DG_SPRING_PROFILES_ACTIVE=e2e-rbac `
    $ImageTag | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'podman run failed for e2e-rbac profile' }

try {
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "Playwright RBAC E2E — rbac.console.spec.ts + rbac.ui.spec.ts"
    Push-Location $WebDir
    try {
        if (-not (Test-Path 'node_modules')) {
            npm install
        }
        npx playwright install chromium
        $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
        $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
        $env:DG_E2E_RBAC = 'true'
        npx playwright test e2e/specs/rbac.console.spec.ts e2e/specs/rbac.ui.spec.ts
        if ($LASTEXITCODE -ne 0) { throw 'Playwright RBAC E2E failed' }
    } finally {
        Remove-Item Env:DG_E2E_BASE_URL -ErrorAction SilentlyContinue
        Remove-Item Env:DG_E2E_API_URL -ErrorAction SilentlyContinue
        Remove-Item Env:DG_E2E_RBAC -ErrorAction SilentlyContinue
        Pop-Location
    }

    Write-Host ""
    Write-Host "[SUCCESS] SEC-01 RBAC enable-path verification passed (Maven + Playwright)." -ForegroundColor Green
    Write-Host "Podman image: $ImageTag"
    Write-Host "Container: $ContainerName (port $HostPort, profile e2e-rbac)"
} finally {
    if (-not $KeepContainer) {
        Write-Step "Stopping container $ContainerName"
        podman rm -f $ContainerName 2>$null | Out-Null
    } else {
        Write-Host "Container kept running: podman logs -f $ContainerName"
        Write-Host "Image retained in Podman: podman images $ImageTag"
    }
}
