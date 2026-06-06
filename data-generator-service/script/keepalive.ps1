# Periodic health check with optional auto-restart (Windows Task Scheduler).

param(
    [Parameter(Mandatory = $false)]
    [ValidateSet('check', 'register', 'unregister')]
    [string]$Action = 'check'
)

$BinDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $BinDir 'lib\common.ps1')
Dg-InitPaths $BinDir
Dg-LoadConfig

$maxFail = if ($env:DG_KEEPALIVE_MAX_FAIL) { [int]$env:DG_KEEPALIVE_MAX_FAIL } else { 3 }
$failFile = Join-Path $BinDir '.failcount'
$taskName = "DataGenerator-$($script:ServiceName)-Keepalive"
$psScript = Join-Path $BinDir 'keepalive.ps1'

function Get-FailCount {
    if (-not (Test-Path -LiteralPath $failFile)) { return 0 }
    $line = Get-Content -LiteralPath $failFile -Tail 1
    if ($line -match 'fail:\s*(\d+)') { return [int]$Matches[1] }
    return 0
}

function Write-FailCount([int]$Fail, [string]$RestartTag) {
    $entry = "[$(Get-Date -Format 'yyyyMMdd_HHmmss')]$RestartTag fail: $Fail"
    Add-Content -LiteralPath $failFile -Value $entry
    $lines = (Get-Content -LiteralPath $failFile).Count
    if ($lines -gt 3000) {
        Get-Content -LiteralPath $failFile -Tail 500 | Set-Content -LiteralPath $failFile
    }
}

switch ($Action) {
    'check' {
        if (Dg-HealthCheckQuiet) {
            Write-FailCount 0 ''
            exit 0
        }
        Dg-LogError "Health check failed: $($script:HealthUrl)"
        $fail = Get-FailCount + 1
        if ($fail -ge $maxFail) {
            Dg-LogWarn "Keepalive: $maxFail consecutive failures; restarting $($script:ServiceName)"
            Dg-ServiceStop
            Dg-ServiceStart
            Write-FailCount 0 '[RESTART]'
        } else {
            Write-FailCount $fail ''
        }
        exit 1
    }
    'register' {
        $actionArg = "-ExecutionPolicy Bypass -File `"$psScript`" -Action check"
        schtasks /Create /TN $taskName /TR "powershell.exe $actionArg" /SC MINUTE /MO 5 /F | Out-Null
        Dg-LogInfo "Registered scheduled task: $taskName (every 5 minutes)"
    }
    'unregister' {
        schtasks /Delete /TN $taskName /F 2>$null | Out-Null
        Dg-LogInfo "Removed scheduled task: $taskName"
    }
}
