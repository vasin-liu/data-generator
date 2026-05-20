# Migration workbench staging helper (calls REST APIs on data-generator-service).
param(
    [string]$BaseUrl = "http://localhost:9876/template",
    [long]$TemplateId = 0,
    [string]$InventoryId = "",
    [string]$Filter = "pending_signoff",
    [string]$ApprovedBy = "",
    [ValidateSet("summary", "backlog", "signoff-status", "refresh", "batch-compare", "analyze", "draft", "compare", "promote", "signoff", "workflow")]
    [string]$Action = "summary"
)

$ErrorActionPreference = "Stop"

function Invoke-MigrationApi {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [string]$Body = $null
    )
    $uri = "$BaseUrl$Path"
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json" -Body $Body
    }
    return Invoke-RestMethod -Method $Method -Uri $uri
}

switch ($Action) {
    "summary" {
        Invoke-MigrationApi -Path "/migration/summary" | ConvertTo-Json -Depth 6
    }
    "backlog" {
        Invoke-MigrationApi -Path "/migration/backlog?filter=$Filter" | ConvertTo-Json -Depth 8
    }
    "signoff-status" {
        Invoke-MigrationApi -Path "/migration/signoff-status" | ConvertTo-Json -Depth 6
    }
    "refresh" {
        Invoke-MigrationApi -Method POST -Path "/migration/inventory/refresh" | ConvertTo-Json -Depth 6
    }
    "batch-compare" {
        $body = '{"refreshInventoryFirst":true,"maxTemplates":50,"skipCompatibilityOnly":true}'
        Invoke-MigrationApi -Method POST -Path "/migration/compare/batch" -Body $body | ConvertTo-Json -Depth 8
    }
    "analyze" {
        if ($TemplateId -le 0) { throw "TemplateId required for analyze" }
        Invoke-MigrationApi -Path "/migration/analyze/$TemplateId" | ConvertTo-Json -Depth 8
    }
    "draft" {
        if ($TemplateId -le 0) { throw "TemplateId required for draft" }
        Invoke-MigrationApi -Method POST -Path "/migration/draft/$TemplateId" | ConvertTo-Json -Depth 8
    }
    "compare" {
        if ($TemplateId -le 0) { throw "TemplateId required for compare" }
        Invoke-MigrationApi -Method POST -Path "/migration/compare/$TemplateId" -Body '{"sampleSize":500}' | ConvertTo-Json -Depth 8
    }
    "promote" {
        if ($TemplateId -le 0) { throw "TemplateId required for promote" }
        Invoke-MigrationApi -Method POST -Path "/migration/promote/$TemplateId" | ConvertTo-Json -Depth 8
    }
    "signoff" {
        if ([string]::IsNullOrWhiteSpace($InventoryId)) { throw "InventoryId required for signoff" }
        $body = @{ approved = $true; approvedBy = $ApprovedBy } | ConvertTo-Json
        Invoke-MigrationApi -Method POST -Path "/migration/inventory/$InventoryId/signoff" -Body $body | ConvertTo-Json -Depth 8
    }
    "workflow" {
        if ($TemplateId -le 0) { throw "TemplateId required for workflow" }
        Write-Host "=== analyze ===" -ForegroundColor Cyan
        Invoke-MigrationApi -Path "/migration/analyze/$TemplateId" | ConvertTo-Json -Depth 6
        Write-Host "=== draft ===" -ForegroundColor Cyan
        Invoke-MigrationApi -Method POST -Path "/migration/draft/$TemplateId" | ConvertTo-Json -Depth 4
        Write-Host "=== compare ===" -ForegroundColor Cyan
        Invoke-MigrationApi -Method POST -Path "/migration/compare/$TemplateId" -Body '{"sampleSize":500}' | ConvertTo-Json -Depth 8
        Write-Host "Review report path and classification before promote." -ForegroundColor Yellow
    }
}
