# Podman C2 staging: coordinator + worker JVMs sharing an H2 file volume, REST enqueue smoke, optional Playwright.
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright,
    [switch]$SkipP2,
    [switch]$KeepContainers,
    [string]$ImageTag = 'dg-e2e:local',
    [string]$CoordinatorName = 'dg-dist-coordinator',
    [string]$WorkerName = 'dg-dist-worker',
    [string]$Worker2Name = 'dg-dist-worker-2',
    [string]$DbVolume = 'dg-distributed-db',
    [int]$HostPort = 9876
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$ServiceDir = Join-Path $RepoRoot 'data-generator-service'
$WebDir = Join-Path $RepoRoot 'data-generator-console-web'
. (Join-Path $PSScriptRoot 'lib\repo-maven.ps1')
. (Join-Path $PSScriptRoot 'lib\distributed-staging-rest.ps1')

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
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

Write-Step "Building container image $ImageTag"
Push-Location $ServiceDir
try {
    podman build -t $ImageTag -f Containerfile .
    if ($LASTEXITCODE -ne 0) { throw "podman build failed" }
} finally {
    Pop-Location
}

Write-Step "Preparing shared DB volume $DbVolume"
podman volume exists $DbVolume 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    podman volume create $DbVolume | Out-Null
}

function Stop-DistributedContainers {
    podman rm -f $CoordinatorName 2>$null | Out-Null
    podman rm -f $WorkerName 2>$null | Out-Null
    podman rm -f $Worker2Name 2>$null | Out-Null
}

Stop-DistributedContainers

$coordinatorProfiles = 'distributed-staging,distributed-coordinator'
$workerProfiles = 'distributed-staging,distributed-worker'
$workerSpringArgs = '--data.generator.distributed.lease-seconds=10 --data.generator.distributed.heartbeat-interval-ms=3600000 --data.generator.distributed.poll-delay-ms=1000'
$baseUrl = "http://127.0.0.1:${HostPort}"

try {
    Write-Step "Starting coordinator $CoordinatorName ($coordinatorProfiles)"
    podman run -d `
        --name $CoordinatorName `
        -p "${HostPort}:9876" `
        -v "${DbVolume}:/opt/data-generator-service/db:Z" `
        -e DG_DAEMON=0 `
        -e DG_SERVICE_ROLE=coordinator `
        -e DG_SPRING_PROFILES_ACTIVE=$coordinatorProfiles `
        $ImageTag | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "podman run failed for coordinator" }

    Wait-DistributedHealth -BaseUrl $baseUrl

    Write-Step "Starting worker $WorkerName ($workerProfiles)"
    Start-DistributedWorkerContainer `
        -ContainerName $WorkerName `
        -ImageTag $ImageTag `
        -DbVolume $DbVolume `
        -WorkerProfiles $workerProfiles `
        -WorkerId 'podman-worker-1' `
        -WorkerSpringArgs $workerSpringArgs

    $smoke = Invoke-DistributedEnqueueSmoke -BaseUrl $baseUrl

    if (-not $SkipP2) {
        Write-Step 'C2 P2 drills (AC-4 lease recovery + AC-6 requeue)'
        Invoke-DistributedLeaseRecoverySmoke `
            -BaseUrl $baseUrl `
            -WorkerName $WorkerName `
            -Worker2Name $Worker2Name `
            -ImageTag $ImageTag `
            -DbVolume $DbVolume `
            -WorkerProfiles $workerProfiles `
            -WorkerSpringArgs $workerSpringArgs | Out-Null
        Invoke-DistributedRequeueSmoke -BaseUrl $baseUrl -ExpectedAttempts 3 | Out-Null
    }

    if (-not $SkipPlaywright) {
        Write-Step "Running Playwright distributed job detail E2E"
        Push-Location $WebDir
        try {
            if (-not (Test-Path 'node_modules')) {
                npm install
            }
            npx playwright install chromium
            $env:DG_E2E_DISTRIBUTED_SPLIT = 'true'
            $env:DG_E2E_BASE_URL = "$baseUrl/console/"
            $env:DG_E2E_API_URL = $baseUrl
            $env:DG_E2E_JOB_INSTANCE_ID = "$($smoke.InstanceId)"
            npx playwright test e2e/specs/distributed-job-detail.spec.ts e2e/specs/distributed.spec.ts
            if ($LASTEXITCODE -ne 0) { throw "Playwright distributed split E2E failed" }
        } finally {
            Remove-Item Env:DG_E2E_DISTRIBUTED_SPLIT -ErrorAction SilentlyContinue
            Remove-Item Env:DG_E2E_JOB_INSTANCE_ID -ErrorAction SilentlyContinue
            Pop-Location
        }
    }

    Write-Host ""
    Write-Host "[SUCCESS] Distributed Podman staging completed." -ForegroundColor Green
    Write-Host "  instanceId=$($smoke.InstanceId) worker=$($smoke.WorkerId)" -ForegroundColor Green
} finally {
    if (-not $KeepContainers) {
        Write-Step "Stopping distributed containers"
        Stop-DistributedContainers
    } else {
        Write-Host "Containers kept: podman logs -f $CoordinatorName | podman logs -f $WorkerName | podman logs -f $Worker2Name"
    }
}
