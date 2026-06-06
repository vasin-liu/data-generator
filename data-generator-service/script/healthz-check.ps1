# HTTP liveness probe for Windows Task Scheduler and manual checks.

$BinDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $BinDir 'lib\common.ps1')
Dg-InitPaths $BinDir
Dg-LoadConfig
Dg-HealthCheck
