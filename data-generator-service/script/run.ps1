# Start/stop the main Data Generator coordinator (API + console) process.

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CommandArgs
)

$BinDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $BinDir 'lib\common.ps1')
Dg-InitPaths $BinDir
Dg-LoadConfig
Dg-Main -Args $CommandArgs
