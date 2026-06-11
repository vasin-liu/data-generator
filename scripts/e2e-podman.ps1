# Podman-based E2E: package service, run container, execute Playwright smoke tests.
param(
    [switch]$SkipBuild,
    [switch]$KeepContainer,
    [switch]$SkipDistributedSplit,
    [string]$ImageTag = 'dg-e2e:local',
    [string]$ContainerName = 'dg-e2e',
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

Write-Step "Checking Podman"
podman info | Out-Null

if (-not $SkipBuild) {
    Write-Step "Building service package (frontend + assembly)"
    Push-Location $RepoRoot
    try {
        # Build console static assets first; service embeds ../console-web/target/console-dist (not a Maven dependency).
        $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-console-web -DskipTests package
        if ($code -ne 0) { throw "Console web build failed with exit code $code" }
        $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-service -am -DskipTests package
        if ($code -ne 0) { throw "Maven package failed with exit code $code" }
    } finally {
        Pop-Location
    }
}

Write-Step "Building container image $ImageTag"
Push-Location $ServiceDir
try {
    podman build -t $ImageTag -f Containerfile .
    if ($LASTEXITCODE -ne 0) { throw "podman build failed" }
} finally {
    Pop-Location
}

Write-Step "Starting container $ContainerName on port $HostPort"
podman rm -f $ContainerName 2>$null | Out-Null
podman run -d --name $ContainerName -p "${HostPort}:9876" $ImageTag | Out-Null
if ($LASTEXITCODE -ne 0) { throw "podman run failed" }

try {
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "Running Playwright E2E"
    Push-Location $WebDir
    try {
        if (-not (Test-Path 'node_modules')) {
            npm install
        }
        npx playwright install chromium
        $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
        $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
        npm run e2e
        if ($LASTEXITCODE -ne 0) { throw "Playwright E2E failed" }
    } finally {
        Pop-Location
    }


    Write-Step "Restarting container for RBAC E2E profile"
    podman rm -f $ContainerName 2>$null | Out-Null
    podman run -d --name $ContainerName -p "${HostPort}:9876" -e DG_SPRING_PROFILES_ACTIVE=e2e-rbac $ImageTag | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "podman run failed for RBAC profile" }
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "Running Playwright RBAC E2E"
    Push-Location $WebDir
    try {
        $env:DG_E2E_RBAC = 'true'
        npx playwright test e2e/specs/rbac.console.spec.ts e2e/specs/rbac.ui.spec.ts
        if ($LASTEXITCODE -ne 0) { throw "Playwright RBAC E2E failed" }
    } finally {
        Remove-Item Env:DG_E2E_RBAC -ErrorAction SilentlyContinue
        Pop-Location
    }

    Write-Step "Restarting container for distributed staging E2E profile"
    podman rm -f $ContainerName 2>$null | Out-Null
    podman run -d --name $ContainerName -p "${HostPort}:9876" -e DG_SPRING_PROFILES_ACTIVE=e2e-distributed $ImageTag | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "podman run failed for distributed profile" }
    Wait-Health "http://127.0.0.1:${HostPort}/healthz"

    Write-Step "REST: distributed metrics smoke"
    $distMetricsUri = "http://127.0.0.1:${HostPort}/api/console/distributed/metrics"
    $distMetrics = Invoke-RestMethod -Method GET -Uri $distMetricsUri
    if (-not $distMetrics.data.distributedEnabled -or -not $distMetrics.data.workerEnabled) {
        throw "Distributed metrics expected enabled worker on e2e-distributed profile"
    }
    Write-Host "[OK] Distributed metrics: enabled=$($distMetrics.data.distributedEnabled) worker=$($distMetrics.data.workerEnabled)"

    Write-Step "Running Playwright distributed E2E"
    Push-Location $WebDir
    try {
        $env:DG_E2E_DISTRIBUTED = 'true'
        $env:DG_E2E_BASE_URL = "http://127.0.0.1:${HostPort}/console/"
        $env:DG_E2E_API_URL = "http://127.0.0.1:${HostPort}"
        npx playwright test e2e/specs/distributed.spec.ts
        if ($LASTEXITCODE -ne 0) { throw "Playwright distributed E2E failed" }
    } finally {
        Remove-Item Env:DG_E2E_DISTRIBUTED -ErrorAction SilentlyContinue
        Pop-Location
    }

    if (-not $SkipDistributedSplit) {
        Write-Step "Dual-JVM distributed staging E2E (coordinator + worker)"
        podman rm -f $ContainerName 2>$null | Out-Null
        $splitArgs = @{
            SkipBuild = $true
            ImageTag  = $ImageTag
        }
        if ($KeepContainer) {
            $splitArgs.KeepContainers = $true
        }
        & (Join-Path $PSScriptRoot 'e2e-distributed-podman.ps1') @splitArgs
        if ($LASTEXITCODE -ne 0) { throw "Dual-JVM distributed E2E failed" }
    }

    Write-Host ""
    Write-Host "[SUCCESS] Podman E2E completed." -ForegroundColor Green
} finally {
    if (-not $KeepContainer) {
        Write-Step "Stopping container $ContainerName"
        podman rm -f $ContainerName 2>$null | Out-Null
        podman rm -f dg-dist-coordinator dg-dist-worker 2>$null | Out-Null
    } else {
        Write-Host "Container kept running: podman logs -f $ContainerName"
        Write-Host "Dual-JVM containers (if started): podman logs -f dg-dist-coordinator | podman logs -f dg-dist-worker"
    }
}
