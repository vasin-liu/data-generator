# Host OS coordinator/worker JVM lifecycle for multi-JVM distributed staging (DIST-01).

. (Join-Path $PSScriptRoot 'repo-maven.ps1')

function Get-DistributedJdkHome {
    param(
        [Parameter(Mandatory)][string]$RepoRoot
    )
    $jdk25Helper = Join-Path $RepoRoot 'mvnw-jdk25.ps1'
    if (Test-Path -LiteralPath $jdk25Helper) {
        $content = Get-Content -LiteralPath $jdk25Helper -Raw
        if ($content -match '\$jdkHome\s*=\s*"([^"]+)"') {
            $candidate = $Matches[1]
            if (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe')) {
                return $candidate
            }
        }
    }
    $prevEap = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $javaMajor = Get-RepoJavaMajorVersion -JavaHome $env:JAVA_HOME
    } finally {
        $ErrorActionPreference = $prevEap
    }
    if ($javaMajor -ge 25) {
        return $env:JAVA_HOME
    }
    throw "JDK 25 not found. Configure mvnw-jdk25.ps1 or set JAVA_HOME to JDK 25."
}

function Build-DistributedServiceClasspath {
    param(
        [Parameter(Mandatory)][string]$RepoRoot,
        [Parameter(Mandatory)][string]$ServiceDir,
        [switch]$ForceBuild
    )
    $classesDir = Join-Path $ServiceDir 'target\classes'
    $depsDir = Join-Path $ServiceDir 'target\dependency'
    $mainClassFile = Join-Path $classesDir 'org\gensokyo\data\DataGeneratorApplication.class'

    $needBuild = $ForceBuild.IsPresent
    if (-not $needBuild) {
        if (-not (Test-Path -LiteralPath $mainClassFile)) { $needBuild = $true }
        if (-not (Test-Path -LiteralPath $depsDir)) { $needBuild = $true }
        elseif (-not (Get-ChildItem -LiteralPath $depsDir -Filter '*.jar' -ErrorAction SilentlyContinue)) {
            $needBuild = $true
        }
    }

    if ($needBuild) {
        Write-Host 'Building data-generator-service classpath (package + dependency:copy-dependencies)...' -ForegroundColor Cyan
        $mvnw = Join-Path $RepoRoot 'mvnw-jdk25.ps1'
        if (-not (Test-Path -LiteralPath $mvnw)) {
            throw "Missing Maven wrapper helper: $mvnw"
        }
        $buildArgs = @(
            '-pl', 'data-generator-service',
            '-am',
            '-DskipTests',
            '-Dskip.console.frontend=true',
            '-Dassembly.skipAssembly=true',
            'package',
            'dependency:copy-dependencies',
            '-DoutputDirectory=target/dependency',
            '-DincludeScope=runtime'
        )
        Push-Location $RepoRoot
        try {
            & $mvnw @buildArgs
            if ($LASTEXITCODE -ne 0) {
                throw "Maven classpath build failed with exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }

    if (-not (Test-Path -LiteralPath $classesDir)) {
        throw "Missing $classesDir after build"
    }
    if (-not (Test-Path -LiteralPath $depsDir)) {
        throw "Missing $depsDir after dependency:copy-dependencies"
    }

    return "$classesDir;$depsDir\*"
}

function Start-DistributedHostJvm {
    param(
        [Parameter(Mandatory)][string]$RepoRoot,
        [Parameter(Mandatory)][string]$ServiceDir,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$MainClass,
        [Parameter(Mandatory)][string]$SpringProfilesActive,
        [Parameter(Mandatory)][string]$Classpath,
        [int]$ServerPort = 0,
        [string[]]$ExtraSpringArgs = @(),
        [string[]]$JavaOpts = @('-Dh2.bindAddress=127.0.0.1'),
        [Parameter(Mandatory)][string]$LogFile,
        [string]$JdkHome = ''
    )
    if (-not $JdkHome) {
        $JdkHome = Get-DistributedJdkHome -RepoRoot $RepoRoot
    }
    $javaExe = Join-Path $JdkHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe)) {
        throw "java.exe not found at $javaExe"
    }

    $logDir = Split-Path -Parent $LogFile
    if ($logDir -and -not (Test-Path -LiteralPath $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }

    $javaArgs = @()
    foreach ($opt in $JavaOpts) {
        if ($opt) { $javaArgs += $opt }
    }
    $javaArgs += @(
        '-cp', $Classpath,
        $MainClass,
        "--spring.profiles.active=$SpringProfilesActive"
    )
    if ($ServerPort -gt 0) {
        $javaArgs += "--server.port=$ServerPort"
    }
    foreach ($arg in $ExtraSpringArgs) {
        if ($arg) { $javaArgs += $arg }
    }

    Write-Host "Starting $MainClass (profiles=$SpringProfilesActive, cwd=$WorkingDirectory)..." -ForegroundColor Cyan
    $proc = Start-Process `
        -FilePath $javaExe `
        -ArgumentList $javaArgs `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $LogFile `
        -RedirectStandardError ($LogFile + '.err') `
        -PassThru `
        -NoNewWindow

    return [pscustomobject]@{
        Process  = $proc
        LogFile  = $LogFile
        Pid      = $proc.Id
        JdkHome  = $JdkHome
        MainClass = $MainClass
    }
}

function Stop-DistributedHostJvm {
    param(
        $JvmInfo,
        [int]$GraceSec = 5
    )
    if (-not $JvmInfo -or -not $JvmInfo.Process) {
        return
    }
    $proc = $JvmInfo.Process
    if ($proc.HasExited) {
        return
    }
    try {
        $proc.CloseMainWindow() | Out-Null
        if (-not $proc.WaitForExit($GraceSec * 1000)) {
            $proc.Kill()
            $proc.WaitForExit(5000) | Out-Null
        }
    } catch {
        if (-not $proc.HasExited) {
            $proc.Kill()
        }
    }
}

function Wait-DistributedHostHealth {
    param(
        [Parameter(Mandatory)][int]$Port,
        [int]$TimeoutSec = 300
    )
    $healthUri = "http://127.0.0.1:$Port/healthz"
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-RestMethod -Method GET -Uri $healthUri -TimeoutSec 5
            if ($resp.opcode -eq 0) {
                return
            }
        } catch {
            # JVM still booting
        }
        Start-Sleep -Seconds 3
    }
    throw "Health check timed out: $healthUri"
}
