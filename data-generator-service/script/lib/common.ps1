# Shared runtime helpers for Data Generator packaged deployments (Windows PowerShell).
# Dot-sourced by run.ps1, run-worker.ps1, healthz-check.ps1, keepalive.ps1.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Dg-Now { Get-Date -Format 'yyyy-MM-dd HH:mm:ss' }

function Dg-Log {
    param(
        [Parameter(Mandatory)][string]$Level,
        [Parameter(ValueFromRemainingArguments = $true)][string[]]$Message
    )
    $text = ($Message -join ' ')
    $timestamp = Dg-Now
    Write-Host "[$timestamp] [$Level] $text"
}

function Dg-LogInfo([string]$Message) { Dg-Log -Level 'INFO' $Message }
function Dg-LogWarn([string]$Message) { Dg-Log -Level 'WARN' $Message }
function Dg-LogError([string]$Message) { Dg-Log -Level 'ERROR' $Message }

function Dg-EnsureDir([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Dg-InitPaths([string]$BinDir) {
    $script:BinDir = (Resolve-Path -LiteralPath $BinDir).Path
    $script:RootDir = (Resolve-Path -LiteralPath (Join-Path $script:BinDir '..')).Path
    if (-not $env:DG_LOG_DIR) { $script:LogDir = Join-Path $script:RootDir 'logs' } else { $script:LogDir = $env:DG_LOG_DIR }
    $script:ConfDir = Join-Path $script:RootDir 'conf'
    $script:LibDir = Join-Path $script:RootDir 'lib'
    if (-not $env:DG_JVMDUMP_DIR) { $script:JvmDumpDir = Join-Path $script:RootDir 'jvmdump' } else { $script:JvmDumpDir = $env:DG_JVMDUMP_DIR }
    if (-not $env:DG_SERVICE_NAME) { $script:ServiceName = 'data-generator-service' } else { $script:ServiceName = $env:DG_SERVICE_NAME }
    if (-not $env:DG_SERVICE_ROLE) { $script:ServiceRole = 'coordinator' } else { $script:ServiceRole = $env:DG_SERVICE_ROLE }
    if (-not $env:DG_LOG_FILE) { $script:LogFile = Join-Path $script:LogDir "$($script:ServiceName).log" } else { $script:LogFile = $env:DG_LOG_FILE }
    if (-not $env:DG_PID_FILE) { $script:PidFile = Join-Path $script:RootDir "$($script:ServiceName).pid" } else { $script:PidFile = $env:DG_PID_FILE }
    if (-not $env:DG_SERVER_PORT) { $script:ServerPort = '9876' } else { $script:ServerPort = $env:DG_SERVER_PORT }
    if (-not $env:DG_HEALTH_URL) { $script:HealthUrl = "http://127.0.0.1:$($script:ServerPort)/healthz" } else { $script:HealthUrl = $env:DG_HEALTH_URL }
    if (-not $env:DG_START_WAIT_SEC) { $script:StartWaitSec = 5 } else { $script:StartWaitSec = [int]$env:DG_START_WAIT_SEC }
    if (-not $env:DG_STOP_TIMEOUT_SEC) { $script:StopTimeoutSec = 30 } else { $script:StopTimeoutSec = [int]$env:DG_STOP_TIMEOUT_SEC }
    if (-not $env:DG_DAEMON) { $script:Daemon = '1' } else { $script:Daemon = $env:DG_DAEMON }
    if (-not $env:DG_JAVA_MIN_VERSION) { $script:JavaMinVersion = 25 } else { $script:JavaMinVersion = [int]$env:DG_JAVA_MIN_VERSION }
    if (-not $env:DG_MAIN_CLASS) { $script:MainClass = 'org.gensokyo.data.DataGeneratorWorkerApplication' } else { $script:MainClass = $env:DG_MAIN_CLASS }
    if (-not $env:DG_HEAP_MIN) { $script:HeapMin = '512m' } else { $script:HeapMin = $env:DG_HEAP_MIN }
    if (-not $env:DG_HEAP_MAX) { $script:HeapMax = '1g' } else { $script:HeapMax = $env:DG_HEAP_MAX }
    Dg-ApplyRoleDefaults
}

function Dg-ApplyRoleDefaults {
    if ($script:ServiceRole -eq 'worker') {
        if ($env:DG_WORKER_SERVICE_NAME) {
            $script:ServiceName = $env:DG_WORKER_SERVICE_NAME
        } elseif ($script:ServiceName -eq 'data-generator-service') {
            $script:ServiceName = 'data-generator-worker'
        }
        if (-not $env:DG_LOG_FILE) { $script:LogFile = Join-Path $script:LogDir "$($script:ServiceName).log" }
        if (-not $env:DG_PID_FILE) { $script:PidFile = Join-Path $script:RootDir "$($script:ServiceName).pid" }
    }
}

function Dg-LoadConfig {
    $envFile = Join-Path $script:RootDir 'conf\service.env'
    if (-not (Test-Path -LiteralPath $envFile)) { return }
    Dg-LogInfo "Loading configuration: $envFile"
    Get-Content -LiteralPath $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -le 0) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if ($value.StartsWith('"') -and $value.EndsWith('"')) { $value = $value.Substring(1, $value.Length - 2) }
        [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
    Dg-InitPaths $script:BinDir
    Dg-ApplyRoleDefaults
}

function Dg-JavaMajorVersion([string]$JavaExe) {
    $output = & $JavaExe -version 2>&1 | Out-String
    if ($output -match 'version "([^"]+)"') {
        $ver = $Matches[1]
        $parts = $ver.Split('.')
        if ($parts[0] -eq '1') { return [int]$parts[1] }
        return [int]$parts[0]
    }
    return 0
}

function Dg-ResolveJava {
    $candidates = @()
    if ($env:DG_JAVA_HOME) { $candidates += (Join-Path $env:DG_JAVA_HOME 'bin\java.exe') }
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $pathJava = $null
    try { $pathJava = (Get-Command java -ErrorAction Stop).Source } catch { }
    if ($pathJava) { $candidates += $pathJava }

    foreach ($java in $candidates | Select-Object -Unique) {
        if (-not (Test-Path -LiteralPath $java)) { continue }
        $major = Dg-JavaMajorVersion $java
        if ($major -ge $script:JavaMinVersion) {
            $script:JavaCmd = $java
            $verLine = (& $java -version 2>&1 | Select-Object -First 1)
            Dg-LogInfo "Java: $verLine"
            return
        }
        Dg-LogWarn "Skipping Java $java (version $major < $($script:JavaMinVersion))"
    }
    throw "JDK $($script:JavaMinVersion)+ required. Set DG_JAVA_HOME in conf\service.env (see service.env.example)."
}

function Dg-FindAppJar {
    $pattern = Join-Path $script:BinDir "$($script:ServiceName)-*.jar"
    $jar = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue | Sort-Object Name | Select-Object -Last 1
    if (-not $jar) { throw "Application JAR not found in $($script:BinDir) (pattern: $pattern)" }
    $script:AppJar = $jar.FullName
    Dg-LogInfo "Application JAR: $($script:AppJar)"
}

function Dg-BuildSpringArgs {
    $args = @("--spring.config.location=$($script:ConfDir)\")
    if ($env:DG_SPRING_PROFILES_ACTIVE) { $args += "--spring.profiles.active=$($env:DG_SPRING_PROFILES_ACTIVE)" }
    $logback = Join-Path $script:ConfDir 'logback-spring.xml'
    if (Test-Path -LiteralPath $logback) { $args += "--logging.config=$logback" }
    if ($script:ServerPort) { $args += "--server.port=$($script:ServerPort)" }
    if ($env:DG_SPRING_ARGS) { $args += ($env:DG_SPRING_ARGS -split '\s+') }
    return ,$args
}

function Dg-BuildJvmArgs {
    $args = @()
    if ($script:Daemon -eq '0') {
        $args += '-XX:MaxRAMPercentage=95.0'
    } else {
        $args += @("-Xms$($script:HeapMin)", "-Xmx$($script:HeapMax)", '-XX:MetaspaceSize=256m', '-XX:MaxMetaspaceSize=256m')
    }
    $args += @('-XX:+UseG1GC', '-XX:+HeapDumpOnOutOfMemoryError', "-XX:HeapDumpPath=$(Join-Path $script:JvmDumpDir "$($script:ServiceName).hprof")")
    if ($env:DG_JVM_OPTS) { $args += ($env:DG_JVM_OPTS -split '\s+') }
    return ,$args
}

function Dg-ReadPid {
    if (-not (Test-Path -LiteralPath $script:PidFile)) { return $null }
    $text = (Get-Content -LiteralPath $script:PidFile -Raw).Trim()
    if ($text -match '^\d+$') { return [int]$text }
    return $null
}

function Dg-IsRunning([Nullable[int]]$Pid) {
    if (-not $Pid) { return $false }
    return $null -ne (Get-Process -Id $Pid -ErrorAction SilentlyContinue)
}

function Dg-ServiceStatus {
    $pidVal = Dg-ReadPid
    if (Dg-IsRunning $pidVal) {
        Dg-LogInfo "$($script:ServiceName) is running (pid=$pidVal, role=$($script:ServiceRole))"
        Dg-LogInfo "Log file: $($script:LogFile)"
        Dg-LogInfo "Health URL: $($script:HealthUrl)"
        return
    }
    if ($pidVal) {
        Dg-LogWarn "Removing stale PID file: $($script:PidFile) (pid=$pidVal)"
        Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue
    }
    Dg-LogInfo "$($script:ServiceName) is not running"
    exit 1
}

function Dg-HealthCheckQuiet {
    try {
        $resp = Invoke-WebRequest -Uri $script:HealthUrl -UseBasicParsing -TimeoutSec 8
        return ($resp.StatusCode -eq 200)
    } catch {
        return $false
    }
}

function Dg-HealthCheck {
    Dg-LogInfo "Checking $($script:HealthUrl) ..."
    try {
        $resp = Invoke-WebRequest -Uri $script:HealthUrl -UseBasicParsing -TimeoutSec 10
    } catch {
        Dg-LogError "Health check failed: $($_.Exception.Message)"
        exit 1
    }
    if ($resp.StatusCode -ne 200) {
        Dg-LogError "Health check failed: HTTP $($resp.StatusCode)"
        exit 1
    }
    if ($resp.Content -notmatch '"opcode"\s*:\s*0') {
        Dg-LogError "Health check failed: response missing opcode=0"
        Dg-LogError "Body: $($resp.Content)"
        exit 1
    }
    Dg-LogInfo "Health check OK: $($resp.Content)"
}

function Dg-ServiceStart {
    $pidVal = Dg-ReadPid
    if (Dg-IsRunning $pidVal) {
        Dg-LogInfo "$($script:ServiceName) already running (pid=$pidVal)"
        return
    }
    if ($pidVal) { Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue }

    Dg-EnsureDir $script:LogDir
    Dg-EnsureDir $script:JvmDumpDir
    Dg-ResolveJava
    Dg-FindAppJar
    $jvm = Dg-BuildJvmArgs
    $spring = Dg-BuildSpringArgs

    if ($script:ServiceRole -eq 'worker') {
        $libGlob = Join-Path $script:LibDir '*.jar'
        $cp = "$($script:ConfDir);$libGlob;$($script:AppJar)"
        Dg-LogInfo "Launch mode: classpath main $($script:MainClass)"
        $cmdArgs = @($jvm) + @('-cp', $cp, $script:MainClass) + $spring
    } else {
        Dg-LogInfo 'Launch mode: java -jar'
        $cmdArgs = @($jvm) + @('-jar', $script:AppJar) + $spring
    }

    if ($script:Daemon -eq '0') {
        Dg-LogInfo "Starting $($script:ServiceName) in foreground (role=$($script:ServiceRole)) ..."
        Dg-LogInfo 'Press Ctrl+C to stop.'
        & $script:JavaCmd @cmdArgs
        return
    }

    Dg-LogInfo "Starting $($script:ServiceName) in background (role=$($script:ServiceRole)) ..."
    Dg-LogInfo "Stdout/stderr -> $($script:LogFile)"
    $proc = Start-Process -FilePath $script:JavaCmd -ArgumentList $cmdArgs -RedirectStandardOutput $script:LogFile -RedirectStandardError $script:LogFile -PassThru -WindowStyle Hidden
    Set-Content -LiteralPath $script:PidFile -Value $proc.Id -NoNewline
    Dg-LogInfo "Started with pid=$($proc.Id)"

    for ($i = 0; $i -lt $script:StartWaitSec; $i++) {
        Start-Sleep -Seconds 1
        if (Dg-HealthCheckQuiet) {
            Dg-LogInfo "Health check passed: $($script:HealthUrl)"
            return
        }
    }
    Dg-LogWarn "Process started but health check not yet OK ($($script:HealthUrl))"
    Dg-LogWarn "Tail logs: Get-Content -Wait $($script:LogFile)"
}

function Dg-ServiceStop {
    $pidVal = Dg-ReadPid
    if (-not (Dg-IsRunning $pidVal)) {
        Dg-LogInfo "$($script:ServiceName) is not running"
        Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue
        return
    }
    Dg-LogInfo "Stopping $($script:ServiceName) (pid=$pidVal) ..."
    Stop-Process -Id $pidVal -ErrorAction SilentlyContinue
    $waited = 0
    while ((Dg-IsRunning $pidVal) -and ($waited -lt $script:StopTimeoutSec)) {
        Start-Sleep -Seconds 1
        $waited++
    }
    if (Dg-IsRunning $pidVal) {
        Dg-LogWarn "Graceful stop timed out; forcing stop"
        Stop-Process -Id $pidVal -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue
    Dg-LogInfo "$($script:ServiceName) stopped"
}

function Dg-ServiceRestart {
    Dg-ServiceStop
    Dg-ServiceStart
}

function Dg-PrintUsage {
    @"

Data Generator runtime script (Windows)
  Package root : $($script:RootDir)
  Service      : $($script:ServiceName) ($($script:ServiceRole))
  Config file  : $(Join-Path $script:RootDir 'conf\service.env')

Commands:
  start [0|1]   Start service (1=background default, 0=foreground)
  stop          Stop service
  restart       Restart service
  status        Show running state
  health        Run HTTP health check
  help          Show this help

"@
}

function Dg-Main {
    param([string[]]$CommandArgs)
    $action = if ($CommandArgs.Count -gt 0) { $CommandArgs[0].ToLowerInvariant() } else { 'help' }
    if ($CommandArgs.Count -gt 1 -and ($CommandArgs[1] -eq '0' -or $CommandArgs[1] -eq '1')) {
        $env:DG_DAEMON = $CommandArgs[1]
    }
    if ($env:DG_FOREGROUND -eq '1') { $env:DG_DAEMON = '0' }

    switch ($action) {
        'start' { Dg-ServiceStart }
        'stop' { Dg-ServiceStop }
        'restart' { Dg-ServiceRestart }
        'status' { Dg-ServiceStatus }
        'health' { Dg-HealthCheck }
        'help' { Dg-PrintUsage }
        default {
            Dg-LogError "Unknown command: $action"
            Dg-PrintUsage
            exit 1
        }
    }
}
