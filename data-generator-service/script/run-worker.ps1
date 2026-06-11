# Start/stop the distributed worker process.

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CommandArgs
)

$env:DG_SERVICE_ROLE = 'worker'
if (-not $env:DG_SPRING_PROFILES_ACTIVE) { $env:DG_SPRING_PROFILES_ACTIVE = 'distributed-worker' }

$BinDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $BinDir 'lib\common.ps1')
Dg-InitPaths $BinDir
Dg-LoadConfig
Dg-Main -Args $CommandArgs
