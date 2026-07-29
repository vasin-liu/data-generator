# DIST-01 primary proof: host OS coordinator + worker JVMs, POST /task/run, dual SUCCESS poll.
param(
    [switch]$SkipBuild,
    [switch]$SkipMavenPreflight,
    [switch]$KeepWorkDir,
    [int]$HostPort = 9876,
    [int]$TimeoutSec = 300
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$ServiceDir = Join-Path $RepoRoot 'data-generator-service'
$WorkerId = 'host-worker-1'
$WorkerPort = 9877
$CoordinatorProfiles = 'distributed-staging,distributed-coordinator'
$WorkerProfiles = 'distributed-staging,distributed-worker'
$SharedH2Url = 'jdbc:h2:file:./db/distributed-staging;MODE=PostgreSQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1'
$CommonSpringArgs = @(
    '--spring.config.additional-location=file:./application-dist-verify.yaml'
)
$CoordinatorSpringArgs = $CommonSpringArgs
$WorkerSpringArgs = @(
    '--data.generator.distributed.poll-delay-ms=1000',
    '--data.generator.distributed.lease-seconds=60',
    "--data.generator.distributed.worker-id=$WorkerId",
    "--server.port=$WorkerPort"
) + $CommonSpringArgs

. (Join-Path $PSScriptRoot 'lib\repo-maven.ps1')
. (Join-Path $PSScriptRoot 'lib\distributed-host-jvm.ps1')
. (Join-Path $PSScriptRoot 'lib\distributed-staging-rest.ps1')

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-PortInUse {
    param([int]$Port)
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
        $listener.Start()
        $listener.Stop()
        return $false
    } catch {
        return $true
    }
}

function Wait-DistributedWorkerReady {
    param(
        [Parameter(Mandatory)][int]$Port,
        [int]$TimeoutSec = 300
    )
    Wait-DistributedHostHealth -Port $Port -TimeoutSec $TimeoutSec
    # Allow worker poller thread to start after Spring context is up.
    Start-Sleep -Seconds 5
}

if (-not $SkipMavenPreflight) {
    Write-Step 'Maven preflight: embedded distributed integration tests (D-08 guard)'
    $mvnw = Join-Path $RepoRoot 'mvnw-jdk25.ps1'
    & $mvnw -pl 'data-generator-service' -am test `
        '-Dtest=DistributedJob*IntegrationTests,DistributedJobServiceTests,DistributedSplitRoleIntegrationTests,ConsoleDistributedControllerTest' `
        '-Dsurefire.failIfNoSpecifiedTests=false'
    if ($LASTEXITCODE -ne 0) {
        throw "Maven distributed preflight failed with exit code $LASTEXITCODE"
    }
    Write-Host 'Maven preflight: PASS' -ForegroundColor Green
}

if (Test-PortInUse -Port $HostPort) {
    throw "Port $HostPort is in use. Free the port or pass -HostPort with a free value."
}

$workDir = Join-Path $env:TEMP ("dg-dist-verify-" + [guid]::NewGuid().ToString('n'))
$dbDir = Join-Path $workDir 'db'
New-Item -ItemType Directory -Path $dbDir -Force | Out-Null

$overrideYaml = @"
spring:
  datasource:
    dynamic:
      datasource:
        data-generator:
          url: $SharedH2Url
"@
Set-Content -LiteralPath (Join-Path $workDir 'application-dist-verify.yaml') -Value $overrideYaml -Encoding utf8

$coordinatorJvm = $null
$workerJvm = $null
$baseUrl = "http://127.0.0.1:$HostPort"
$origLocation = Get-Location

$forceBuild = -not $SkipBuild.IsPresent
$classpath = Build-DistributedServiceClasspath -RepoRoot $RepoRoot -ServiceDir $ServiceDir -ForceBuild:$forceBuild

try {
    Set-Location $workDir
    Write-Step "Isolated work dir: $workDir (shared file H2 under ./db/)"

    Write-Step "Starting coordinator ($CoordinatorProfiles) on port $HostPort"
    $coordinatorJvm = Start-DistributedHostJvm `
        -RepoRoot $RepoRoot `
        -ServiceDir $ServiceDir `
        -WorkingDirectory $workDir `
        -MainClass 'org.gensokyo.data.DataGeneratorApplication' `
        -SpringProfilesActive $CoordinatorProfiles `
        -Classpath $classpath `
        -ServerPort $HostPort `
        -ExtraSpringArgs $CoordinatorSpringArgs `
        -LogFile (Join-Path $workDir 'coordinator.log')

    Wait-DistributedHostHealth -Port $HostPort

    Write-Step "Starting worker ($WorkerProfiles, worker-id=$WorkerId)"
    $workerJvm = Start-DistributedHostJvm `
        -RepoRoot $RepoRoot `
        -ServiceDir $ServiceDir `
        -WorkingDirectory $workDir `
        -MainClass 'org.gensokyo.data.DataGeneratorWorkerApplication' `
        -SpringProfilesActive $WorkerProfiles `
        -Classpath $classpath `
        -ExtraSpringArgs $WorkerSpringArgs `
        -LogFile (Join-Path $workDir 'worker.log')

    Write-Step "Waiting for worker health (port $WorkerPort)"
    Wait-DistributedWorkerReady -Port $WorkerPort

    Write-Step 'Seed published minimal iterator/SQL/console template (D-06)'
    $template = New-DistributedMinimalIteratorConsoleTemplate -BaseUrl $baseUrl

    Write-Step "Enqueue via POST /task/run/$($template.TemplateId) (D-05)"
    $instanceId = Start-DistributedTemplateRun -BaseUrl $baseUrl -TemplateId $template.TemplateId

    Write-Step "Poll dual SUCCESS (execution + distributedJob, timeout=${TimeoutSec}s)"
    $detail = Wait-DistributedDualSuccess -BaseUrl $baseUrl -InstanceId $instanceId -TimeoutSec $TimeoutSec

    $distributedJobId = $detail.distributedJob.jobId
    Write-Host "[SUCCESS] Multi-JVM worker E2E: instanceId=$instanceId distributedJobId=$distributedJobId workerId=$($detail.distributedJob.workerId) templateId=$($template.TemplateId)" -ForegroundColor Green
    exit 0
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    if ($coordinatorJvm -and (Test-Path -LiteralPath $coordinatorJvm.LogFile)) {
        Write-Host "--- coordinator log tail ---" -ForegroundColor Yellow
        Get-Content -LiteralPath $coordinatorJvm.LogFile -Tail 40 -ErrorAction SilentlyContinue | Write-Host
    }
    if ($workerJvm -and (Test-Path -LiteralPath $workerJvm.LogFile)) {
        Write-Host "--- worker log tail ---" -ForegroundColor Yellow
        Get-Content -LiteralPath $workerJvm.LogFile -Tail 40 -ErrorAction SilentlyContinue | Write-Host
    }
    exit 1
} finally {
    Set-Location $origLocation
    if ($workerJvm) {
        Stop-DistributedHostJvm -JvmInfo $workerJvm
    }
    if ($coordinatorJvm) {
        Stop-DistributedHostJvm -JvmInfo $coordinatorJvm
    }
    if (-not $KeepWorkDir) {
        if (Test-Path -LiteralPath $workDir) {
            Remove-Item -LiteralPath $workDir -Recurse -Force -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "Kept work dir: $workDir" -ForegroundColor Yellow
    }
}
