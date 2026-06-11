# REST helpers for C2 coordinator + worker staging smoke (enqueue on coordinator, execute on worker).

function Wait-DistributedWorkerHealth {
    param(
        [Parameter(Mandatory)][string]$ContainerName,
        [int]$TimeoutSec = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = podman exec $ContainerName curl -sf http://127.0.0.1:9876/healthz 2>$null | ConvertFrom-Json
            if ($resp.opcode -eq 0) {
                return
            }
        } catch {
            # worker JVM still booting
        }
        Start-Sleep -Seconds 3
    }
    throw "Worker health check timed out in container $ContainerName"
}

function Wait-DistributedHealth {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [int]$TimeoutSec = 180
    )
    $healthUri = "$($BaseUrl.TrimEnd('/'))/healthz"
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-RestMethod -Method GET -Uri $healthUri -TimeoutSec 5
            if ($resp.opcode -eq 0) {
                return
            }
        } catch {
            # retry
        }
        Start-Sleep -Seconds 3
    }
    throw "Health check timed out: $healthUri"
}

function Invoke-DistributedApi {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][string]$Path,
        $Body
    )
    $uri = "$($BaseUrl.TrimEnd('/'))$Path"
    $params = @{
        Method      = $Method
        Uri         = $uri
        ContentType = 'application/json'
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 30)
    }
    return Invoke-RestMethod @params
}

function Get-DistributedApiData {
    param([Parameter(Mandatory)]$Envelope)
    if (-not $Envelope.success) {
        $msg = if ($Envelope.message) { $Envelope.message } else { 'API returned success=false' }
        throw $msg
    }
    return $Envelope.data
}

function New-DistributedPublishedTemplate {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [string]$ScenarioId = 'GF-A',
        [string]$TemplateName = "staging-dist-$(Get-Date -Format 'yyyyMMddHHmmss')"
    )
    $scaffold = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path "/api/templates/scenarios/$ScenarioId/scaffold"
    $payload = Get-DistributedApiData $scaffold
    $draft = $payload.draft
    if (-not $draft) {
        throw "Scenario scaffold $ScenarioId returned no draft"
    }
    $draft.name = $TemplateName

    $created = Invoke-DistributedApi -Method POST -BaseUrl $BaseUrl -Path '/api/templates' -Body $draft
    $saved = Get-DistributedApiData $created
    $templateId = $saved.templateId
    if (-not $templateId) {
        throw 'Template create did not return templateId'
    }

    Invoke-DistributedApi -Method POST -BaseUrl $BaseUrl -Path "/api/templates/$templateId/publish" | Out-Null
    return [pscustomobject]@{
        TemplateId   = [int64]$templateId
        TemplateName = $TemplateName
    }
}

function Start-DistributedTemplateRun {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$TemplateId
    )
    $start = Invoke-DistributedApi -Method POST -BaseUrl $BaseUrl -Path "/task/run/$TemplateId"
    if (-not $start.success) {
        $msg = if ($start.message) { $start.message } else { 'task run failed' }
        throw $msg
    }
    # TaskController returns the start summary in the envelope message (data is null).
    $message = $start.message
    if ($message -notmatch 'instanceId=(\d+)') {
        throw "Unable to parse instanceId from run response: $message"
    }
    return [int64]$Matches[1]
}

function Get-DistributedJobDetail {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$InstanceId
    )
    $detail = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path "/api/jobs/$InstanceId"
    return Get-DistributedApiData $detail
}

function Wait-DistributedJobSuccess {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$InstanceId,
        [int]$TimeoutSec = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $data = Get-DistributedJobDetail -BaseUrl $BaseUrl -InstanceId $InstanceId
        $status = $data.execution.status
        if ($status -eq 'SUCCESS') {
            return $data
        }
        if ($status -in @('FAILED', 'CANCELLED')) {
            throw "Job $InstanceId ended with status $status"
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for job $InstanceId to reach SUCCESS"
}

function Wait-DistributedJobExecutionStatus {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$InstanceId,
        [Parameter(Mandatory)][string]$Status,
        [int]$TimeoutSec = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $data = Get-DistributedJobDetail -BaseUrl $BaseUrl -InstanceId $InstanceId
        if ($data.execution.status -eq $Status) {
            return $data
        }
        if ($data.execution.status -in @('FAILED', 'CANCELLED', 'SUCCESS') -and $data.execution.status -ne $Status) {
            throw "Job $InstanceId reached $($data.execution.status) before expected $Status"
        }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for job $InstanceId execution status $Status"
}

function Wait-DistributedJobTerminalFailure {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$InstanceId,
        [int]$ExpectedAttempts = 3,
        [int]$TimeoutSec = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $data = Get-DistributedJobDetail -BaseUrl $BaseUrl -InstanceId $InstanceId
        if ($data.execution.status -eq 'FAILED' -and $data.distributedJob.status -eq 'FAILED') {
            if ($data.distributedJob.attempts -ne $ExpectedAttempts) {
                throw "Expected distributedJob.attempts=$ExpectedAttempts, got $($data.distributedJob.attempts)"
            }
            return $data
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for job $InstanceId to reach terminal FAILED with $ExpectedAttempts attempts"
}

function Publish-DistributedDraftTemplate {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)]$Draft
    )
    $created = Invoke-DistributedApi -Method POST -BaseUrl $BaseUrl -Path '/api/templates' -Body $Draft
    $saved = Get-DistributedApiData $created
    $templateId = $saved.templateId
    if (-not $templateId) {
        throw 'Template create did not return templateId'
    }
    Invoke-DistributedApi -Method POST -BaseUrl $BaseUrl -Path "/api/templates/$templateId/publish" | Out-Null
    return [int64]$templateId
}

function New-DistributedSlowPublishedTemplate {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [string]$TemplateName = "staging-dist-slow-$(Get-Date -Format 'yyyyMMddHHmmss')"
    )
    $scaffold = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path '/api/templates/scenarios/GF-A/scaffold'
    $payload = Get-DistributedApiData $scaffold
    $draft = $payload.draft
    if (-not $draft) {
        throw 'GF-A scaffold returned no draft'
    }
    $draft.name = $TemplateName
    # Large iterator keeps the worker busy long enough to kill mid-run (JS Thread.sleep is sandbox-blocked).
    $draft.sources.seed.iterator.to = 400000
    if ($draft.sources.seed.PSObject.Properties['materializationPolicy']) {
        $draft.sources.seed.PSObject.Properties.Remove('materializationPolicy')
    }
    $templateId = Publish-DistributedDraftTemplate -BaseUrl $BaseUrl -Draft $draft
    return [pscustomobject]@{
        TemplateId   = $templateId
        TemplateName = $TemplateName
    }
}

function New-DistributedFailingPublishedTemplate {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [string]$TemplateName = "staging-dist-fail-$(Get-Date -Format 'yyyyMMddHHmmss')"
    )
    $scaffold = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path '/api/templates/scenarios/GF-A/scaffold'
    $payload = Get-DistributedApiData $scaffold
    $draft = $payload.draft
    if (-not $draft) {
        throw 'GF-A scaffold returned no draft'
    }
    $draft.name = $TemplateName
    $draft.transform = @{
        type = 'sql'
        sql  = 'SELECT * FROM __dist_staging_fail_missing__'
    }
    $templateId = Publish-DistributedDraftTemplate -BaseUrl $BaseUrl -Draft $draft
    return [pscustomobject]@{
        TemplateId   = $templateId
        TemplateName = $TemplateName
    }
}

function Start-DistributedWorkerContainer {
    param(
        [Parameter(Mandatory)][string]$ContainerName,
        [Parameter(Mandatory)][string]$ImageTag,
        [Parameter(Mandatory)][string]$DbVolume,
        [Parameter(Mandatory)][string]$WorkerProfiles,
        [Parameter(Mandatory)][string]$WorkerId,
        [string]$WorkerSpringArgs = ''
    )
    podman rm -f $ContainerName 2>$null | Out-Null
    $envArgs = @(
        '-e', 'DG_DAEMON=0',
        '-e', 'DG_SERVICE_ROLE=worker',
        '-e', "DG_SPRING_PROFILES_ACTIVE=$WorkerProfiles"
    )
    if ($WorkerSpringArgs) {
        $envArgs += '-e'
        $envArgs += "DG_SPRING_ARGS=$WorkerSpringArgs --data.generator.distributed.worker-id=$WorkerId"
    } else {
        $envArgs += '-e'
        $envArgs += "DG_SPRING_ARGS=--data.generator.distributed.worker-id=$WorkerId"
    }
    podman run -d `
        --name $ContainerName `
        -v "${DbVolume}:/opt/data-generator-service/db:Z" `
        @envArgs `
        --entrypoint bash `
        $ImageTag `
        bin/run-worker.sh start 0 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "podman run failed for worker $ContainerName"
    }
    Wait-DistributedWorkerHealth -ContainerName $ContainerName
}

function Invoke-DistributedLeaseRecoverySmoke {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][string]$WorkerName,
        [Parameter(Mandatory)][string]$Worker2Name,
        [Parameter(Mandatory)][string]$ImageTag,
        [Parameter(Mandatory)][string]$DbVolume,
        [Parameter(Mandatory)][string]$WorkerProfiles,
        [Parameter(Mandatory)][string]$WorkerSpringArgs,
        [string]$RecoveryWorkerId = 'podman-worker-2'
    )
    Write-Host '==> REST AC-4: slow job, kill worker, lease recovery' -ForegroundColor Cyan
    $template = New-DistributedSlowPublishedTemplate -BaseUrl $BaseUrl
    $instanceId = Start-DistributedTemplateRun -BaseUrl $BaseUrl -TemplateId $template.TemplateId

    $runningDeadline = (Get-Date).AddSeconds(90)
    $sawRunning = $false
    while ((Get-Date) -lt $runningDeadline) {
        $data = Get-DistributedJobDetail -BaseUrl $BaseUrl -InstanceId $instanceId
        if ($data.execution.status -eq 'SUCCESS') {
            throw "Slow job $instanceId completed on worker $($data.distributedJob.workerId) before AC-4 kill window"
        }
        if ($data.execution.status -eq 'RUNNING') {
            $sawRunning = $true
            break
        }
        if ($data.execution.status -in @('FAILED', 'CANCELLED')) {
            throw "Slow job $instanceId ended with $($data.execution.status) before RUNNING"
        }
        Start-Sleep -Milliseconds 250
    }
    if (-not $sawRunning) {
        throw "Timed out waiting for slow job $instanceId to reach RUNNING"
    }

    Write-Host "==> AC-4: stopping worker $WorkerName mid-run" -ForegroundColor Cyan
    podman stop -t 2 $WorkerName 2>$null | Out-Null
    podman rm -f $WorkerName 2>$null | Out-Null

    # Lease is short in P2 profile; allow expiry before replacement worker polls.
    Start-Sleep -Seconds 12

    Write-Host "==> AC-4: starting replacement worker $Worker2Name" -ForegroundColor Cyan
    Start-DistributedWorkerContainer `
        -ContainerName $Worker2Name `
        -ImageTag $ImageTag `
        -DbVolume $DbVolume `
        -WorkerProfiles $WorkerProfiles `
        -WorkerId $RecoveryWorkerId `
        -WorkerSpringArgs $WorkerSpringArgs

    $detail = Wait-DistributedJobSuccess -BaseUrl $BaseUrl -InstanceId $instanceId -TimeoutSec 180
    if ($detail.distributedJob.attempts -lt 2) {
        throw "Expected distributedJob.attempts >= 2 after lease recovery, got $($detail.distributedJob.attempts)"
    }
    if ($detail.distributedJob.workerId -ne $RecoveryWorkerId) {
        throw "Expected recovery worker $RecoveryWorkerId, got $($detail.distributedJob.workerId)"
    }
    Write-Host "[OK] AC-4 lease recovery: instanceId=$instanceId attempts=$($detail.distributedJob.attempts) worker=$($detail.distributedJob.workerId)" -ForegroundColor Green
    return $detail
}

function Invoke-DistributedRequeueSmoke {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [int]$ExpectedAttempts = 3
    )
    Write-Host '==> REST AC-6: failing template requeues until max-attempts' -ForegroundColor Cyan
    $template = New-DistributedFailingPublishedTemplate -BaseUrl $BaseUrl
    $instanceId = Start-DistributedTemplateRun -BaseUrl $BaseUrl -TemplateId $template.TemplateId
    $detail = Wait-DistributedJobTerminalFailure -BaseUrl $BaseUrl -InstanceId $instanceId -ExpectedAttempts $ExpectedAttempts -TimeoutSec 180
    Write-Host "[OK] AC-6 requeue terminal failure: instanceId=$instanceId attempts=$($detail.distributedJob.attempts)" -ForegroundColor Green
    return $detail
}

function Invoke-DistributedEnqueueSmoke {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [string]$ScenarioId = 'GF-A'
    )
    Write-Host "==> REST: create + publish scenario $ScenarioId" -ForegroundColor Cyan
    $template = New-DistributedPublishedTemplate -BaseUrl $BaseUrl -ScenarioId $ScenarioId

    Write-Host "==> REST: enqueue on coordinator (templateId=$($template.TemplateId))" -ForegroundColor Cyan
    $instanceId = Start-DistributedTemplateRun -BaseUrl $BaseUrl -TemplateId $template.TemplateId

    Write-Host "==> REST: wait for worker SUCCESS (instanceId=$instanceId)" -ForegroundColor Cyan
    $detail = Wait-DistributedJobSuccess -BaseUrl $BaseUrl -InstanceId $instanceId

    if (-not $detail.distributedJob) {
        throw 'Job detail missing distributedJob row'
    }
    if ($detail.distributedJob.status -ne 'SUCCESS') {
        throw "Expected distributedJob.status SUCCESS, got $($detail.distributedJob.status)"
    }
    if (-not $detail.distributedJob.workerId) {
        throw 'Expected distributedJob.workerId to be set after SUCCESS'
    }

    $metrics = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path '/api/console/distributed/metrics'
    $metricsData = Get-DistributedApiData $metrics
    if (-not $metricsData.distributedEnabled) {
        throw 'Coordinator metrics report distributedEnabled=false'
    }

    Write-Host "[OK] Distributed enqueue smoke: instanceId=$instanceId worker=$($detail.distributedJob.workerId)" -ForegroundColor Green
    return [pscustomobject]@{
        TemplateId = $template.TemplateId
        InstanceId = $instanceId
        WorkerId   = $detail.distributedJob.workerId
        Detail     = $detail
    }
}
