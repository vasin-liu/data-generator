$ErrorActionPreference = "Stop"

$jdkHome = "E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1"

if (-not (Test-Path -LiteralPath $jdkHome)) {
    throw "Configured JDK 25 path does not exist: $jdkHome"
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& "$PSScriptRoot\mvnw.cmd" "-s" "$PSScriptRoot\.mvn\settings-jdk25.xml" @Args
exit $LASTEXITCODE
