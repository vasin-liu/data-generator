# Runs Java unit/integration tests for the operator console backend slice.
param(
    [switch]$IncludeWebBuild
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

Push-Location $RepoRoot
try {
    if ($IncludeWebBuild) {
        Write-Host "==> Building console web (embeds into service test classpath)" -ForegroundColor Cyan
        $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-console-web -DskipTests package
        if ($code -ne 0) { throw "Console web build failed" }
    }

    Write-Host "==> Console backend unit tests" -ForegroundColor Cyan
    $testList = @(
        'HealthControllerTest',
        'ConsoleWebEndpointIT',
        'ConsoleStaticResourceIT',
        'ConsoleRuntimeControllerTest',
        'ConsoleTemplateControllerTest',
        'ConsoleDataSourceControllerTest',
        'ConsoleJobControllerTest',
        'ConsoleScheduleControllerTest',
        'ConsoleAuditControllerTest',
        'ConsoleScenarioCatalogControllerTest',
        'V2ScenarioCatalogServiceTest',
        'ConsoleAuthorizationFilterTest',
        'ConsoleDistributedControllerTest',
        'ConsoleUploadControllerTest',
        'TemplateObjectMapperFactoryTests',
        'ConsoleRoleTests',
        'TaskScheduleServiceTests'
    ) -join ','
    $code = Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-service -am `
        "-Dtest=$testList" `
        '-Dsurefire.failIfNoSpecifiedTests=false' `
        test
    if ($code -ne 0) { throw "Console unit tests failed" }

    Write-Host ""
    Write-Host "[SUCCESS] Console unit tests passed." -ForegroundColor Green
} finally {
    Pop-Location
}
