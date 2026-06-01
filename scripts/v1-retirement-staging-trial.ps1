# V1 retirement staging trial helper (M2 ops).
# Verifies CI guardrails and documents the v1-execution.enabled=false trial on staging.
param(
    [string]$BaseUrl = "http://localhost:9876",
    [long]$V1TemplateId = 0,
    [switch]$SkipMaven
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

Write-Step "CI slice: V1 execution flag and migration guards"
if (-not $SkipMaven) {
    Push-Location $RepoRoot
    try {
        & "$RepoRoot\mvnw-jdk25.ps1" -pl data-generator-service -am `
            "-Dtest=TaskControllerV1ExecutionFlagTests,BuiltinClasspathTemplateRegressionTests,MigrationPromoteServiceTests" `
            "-Dsurefire.failIfNoSpecifiedTests=false" test
        if ($LASTEXITCODE -ne 0) {
            throw "Maven V1 retirement slice failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    Write-Host "Maven slice: PASS" -ForegroundColor Green
}

Write-Step "Staging configuration trial"
Write-Host @"
On staging data-generator-service set:

  data:
    generator:
      v1-execution:
        enabled: false

Then validate:
- V2 templates run via /task/run/{id}
- V1-only templates return clear error (no silent fallback)
- Migration signoff complete per docs/migration/staging-readiness-checklist.md
"@ -ForegroundColor Yellow

if ($V1TemplateId -gt 0 -and $BaseUrl) {
    Write-Step "Optional REST probe: V1 template run (expect failure when disabled)"
    $uri = "$BaseUrl.TrimEnd('/')/task/runById/$V1TemplateId"
    try {
        $response = Invoke-RestMethod -Method GET -Uri $uri
        $response | ConvertTo-Json -Depth 4 | Write-Host
        if ($response.success -eq $true) {
            Write-Warning "Run succeeded; confirm v1-execution.enabled is false on target environment."
        } else {
            Write-Host "Expected rejection when V1 disabled: $($response.message)" -ForegroundColor Green
        }
    } catch {
        Write-Host "HTTP error (may be expected): $_" -ForegroundColor Yellow
    }
}

Write-Step "Related docs"
Write-Host "- docs/migration/staging-readiness-checklist.md"
Write-Host "- docs/migration/staging-runbook.md"
Write-Host "- docs/superpowers/plans/2026-05-21-v1-retirement-deferred-ops.md"
