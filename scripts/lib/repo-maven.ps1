# Cross-platform Maven wrapper for local Windows (mvnw-jdk25) and CI (JAVA_HOME from setup-java).
function Invoke-RepoMaven {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$MavenArgs
    )

    $settings = Join-Path $RepoRoot '.mvn/settings-jdk25.xml'
    $jdk25Helper = Join-Path $RepoRoot 'mvnw-jdk25.ps1'
    $useLocalJdk25 = -not $env:JAVA_HOME -and (Test-Path -LiteralPath $jdk25Helper) -and -not ($IsLinux -or $IsMacOS)

    if ($useLocalJdk25) {
        & $jdk25Helper @MavenArgs 2>&1 | ForEach-Object { Write-Host $_ }
        $exitCode = $LASTEXITCODE
        return $exitCode
    }

    $mvnw = if ($IsLinux -or $IsMacOS) {
        Join-Path $RepoRoot 'mvnw'
    } else {
        Join-Path $RepoRoot 'mvnw.cmd'
    }

    & $mvnw '-s' $settings @MavenArgs 2>&1 | ForEach-Object { Write-Host $_ }
    $exitCode = $LASTEXITCODE
    return $exitCode
}
