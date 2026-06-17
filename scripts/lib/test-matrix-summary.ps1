# Build target/test-matrix-summary.json from matrix rows and Surefire reports.
function New-TestMatrixSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MatrixFile,
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [string]$OutFile = 'target/test-matrix-summary.json'
    )

    $matrixPath = if ([System.IO.Path]::IsPathRooted($MatrixFile)) { $MatrixFile } else { Join-Path $RepoRoot $MatrixFile }
    $yaml = Get-Content -Raw $matrixPath
    $rows = Parse-MatrixRows $yaml

    $linkedByRow = @{}
    $mavenClasses = New-Object System.Collections.Generic.List[string]
    foreach ($row in $rows) {
        $tests = @($row.linked_tests)
        if ($tests.Count -eq 0) { continue }
        $mavenLinks = $tests | Where-Object { $_ -and $_ -notmatch 'e2e/specs/' }
        if ($mavenLinks.Count -gt 0) {
            $linkedByRow[$row.id] = @($mavenLinks)
            foreach ($cls in $mavenLinks) {
                if ($mavenClasses -notcontains $cls) { [void]$mavenClasses.Add($cls) }
            }
        }
    }

    $classResults = Get-SurefireClassResults -RepoRoot $RepoRoot -ClassNames $mavenClasses

    $summaryRows = New-Object System.Collections.Generic.List[hashtable]
    $totals = @{ covered = 0; partial = 0; pending = 0; skipped = 0 }

    foreach ($row in $rows) {
        $links = @($row.linked_tests)
        $mavenLinks = $links | Where-Object { $_ -and $_ -notmatch 'e2e/specs/' }
        $linkedResults = @()

        if ($mavenLinks.Count -eq 0) {
            $status = if ($row.status) { $row.status } else { 'pending' }
        } else {
            $passed = 0
            $failed = 0
            $skipped = 0
            $missing = 0
            foreach ($cls in $mavenLinks) {
                $outcome = $classResults[$cls]
                if (-not $outcome) {
                    $missing++
                    $linkedResults += @{ test = $cls; outcome = 'missing' }
                } elseif ($outcome -eq 'passed') {
                    $passed++
                    $linkedResults += @{ test = $cls; outcome = 'passed' }
                } elseif ($outcome -eq 'skipped') {
                    $skipped++
                    $linkedResults += @{ test = $cls; outcome = 'skipped' }
                } else {
                    $failed++
                    $linkedResults += @{ test = $cls; outcome = 'failed' }
                }
            }
            if ($failed -gt 0 -and $passed -eq 0) {
                $status = 'pending'
            } elseif ($skipped -gt 0 -and $passed -eq 0 -and $failed -eq 0) {
                $status = 'skipped-conditional'
            } elseif ($passed -eq $mavenLinks.Count) {
                $status = 'covered'
            } elseif ($passed -gt 0) {
                $status = 'partial'
            } elseif ($missing -eq $mavenLinks.Count) {
                $status = 'pending'
            } else {
                $status = 'pending'
            }
        }

        if ($totals.ContainsKey($status)) { $totals[$status]++ } else { $totals.pending++ }
        $summaryRows.Add(@{ id = $row.id; status = $status; linkedResults = $linkedResults })
    }

    $gitCommit = $null
    try { $gitCommit = (git -C $RepoRoot rev-parse HEAD 2>$null) } catch { }

    $summary = @{
        generatedAt = (Get-Date).ToUniversalTime().ToString('o')
        gitCommit   = $gitCommit
        totals      = $totals
        rows        = $summaryRows
    }

    $outPath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path $RepoRoot $OutFile }
    $outDir = Split-Path -Parent $outPath
    if ($outDir -and -not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
    $summary | ConvertTo-Json -Depth 6 | Set-Content -Path $outPath -Encoding UTF8
    return $summary
}

function Parse-MatrixRows([string]$YamlText) {
    $lines = $YamlText -split "`r?`n"
    $inRows = $false
    $rows = New-Object System.Collections.Generic.List[hashtable]
    $current = $null

    foreach ($line in $lines) {
        if ($line -match '^\s*rows:\s*$') { $inRows = $true; continue }
        if (-not $inRows) { continue }
        if ($line -match '^\s*-\s*id:\s*(.+)$') {
            if ($null -ne $current) { $rows.Add($current) }
            $current = @{ id = $Matches[1].Trim() }
            continue
        }
        if ($null -eq $current) { continue }
        if ($line -match '^\s*([a-z_]+):\s*(.*)$') {
            $key = $Matches[1]
            $raw = $Matches[2].Trim()
            if ($raw -eq '') { $current[$key] = @() }
            elseif ($raw -match '^\[(.*)\]$') {
                $inner = $Matches[1].Trim()
                if ($inner -eq '') { $current[$key] = @() }
                else { $current[$key] = $inner -split ',\s*' | ForEach-Object { $_.Trim() } }
            }
            else { $current[$key] = $raw }
        }
    }
    if ($null -ne $current) { $rows.Add($current) }
    return $rows
}

function Get-SurefireClassResults {
    param(
        [string]$RepoRoot,
        [string[]]$ClassNames
    )
    $results = @{}
    if ($ClassNames.Count -eq 0) { return $results }

    $reportFiles = Get-ChildItem -Path $RepoRoot -Recurse -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\target\\surefire-reports\\' }

    foreach ($file in $reportFiles) {
        try {
            [xml]$xml = Get-Content -Raw $file.FullName
            $className = $xml.testsuite.name
            if (-not $className) { continue }
            $simple = ($className -split '\.')[-1]
            if ($ClassNames -notcontains $simple) { continue }
            $failures = [int]$xml.testsuite.failures
            $errors = [int]$xml.testsuite.errors
            $skipped = [int]$xml.testsuite.skipped
            if ($failures -gt 0 -or $errors -gt 0) { $results[$simple] = 'failed' }
            elseif ($skipped -gt 0 -and ([int]$xml.testsuite.tests -eq $skipped)) { $results[$simple] = 'skipped' }
            else { $results[$simple] = 'passed' }
        } catch { }
    }
    return $results
}
