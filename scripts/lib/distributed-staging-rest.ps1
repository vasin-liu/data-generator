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

function Wait-DistributedJobSuccess {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][int64]$InstanceId,
        [int]$TimeoutSec = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $detail = Invoke-DistributedApi -Method GET -BaseUrl $BaseUrl -Path "/api/jobs/$InstanceId"
        $data = Get-DistributedApiData $detail
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
