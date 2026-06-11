# Phase C2 distributed staging smoke helper.
# Pre-flight: runs embedded integration tests. Optional: REST checks when service is up.
param(
    [string]$CoordinatorBaseUrl = "",
    [string]$WorkerMetricsUrl = "",
    [switch]$SkipMaven,
    [switch]$SkipRest
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "Pre-flight: distributed integration tests (AC-1..AC-6)"
if (-not $SkipMaven) {
    Push-Location $RepoRoot
    try {
        & "$RepoRoot\mvnw-jdk25.ps1" -pl "data-generator-service" -am test `
            "-Dtest=DistributedJob*IntegrationTests,DistributedJobServiceTests,DistributedSplitRoleIntegrationTests,ConsoleDistributedControllerTest" `
            "-Dsurefire.failIfNoSpecifiedTests=false"
        if ($LASTEXITCODE -ne 0) {
            throw "Maven distributed tests failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    Write-Host "Maven pre-flight: PASS" -ForegroundColor Green
} else {
    Write-Host "Maven pre-flight: SKIPPED" -ForegroundColor Yellow
}

if (-not $SkipRest -and $CoordinatorBaseUrl) {
    Write-Step "REST: coordinator metrics"
    $metricsUri = "$CoordinatorBaseUrl.TrimEnd('/')/api/console/distributed/metrics"
    $metrics = Invoke-RestMethod -Method GET -Uri $metricsUri
    $metrics | ConvertTo-Json -Depth 6 | Write-Host

    Write-Step "Manual staging steps (see docs/staging-distributed-deployment.md)"
    Write-Host @"
1. Start Coordinator: DataGeneratorApplication --spring.profiles.active=distributed-coordinator
2. Start Worker(s): DataGeneratorWorkerApplication --spring.profiles.active=distributed-worker
3. POST /task/run/{templateId} on coordinator (V2 published template)
4. Confirm worker logs show lease + SUCCESS; Console job detail shows distributedJob
5. Optional cancel-before-run and long-run heartbeat on staging DB
"@ -ForegroundColor Yellow
} elseif (-not $SkipRest) {
    Write-Host "REST checks skipped (pass -CoordinatorBaseUrl http://host:port)." -ForegroundColor Yellow
}

if ($WorkerMetricsUrl) {
    Write-Step "REST: worker metrics endpoint"
    Invoke-RestMethod -Method GET -Uri $WorkerMetricsUrl | ConvertTo-Json -Depth 6 | Write-Host
}

Write-Step "Done"
Write-Host "Full runbook: docs/staging-distributed-deployment.md" -ForegroundColor Green
