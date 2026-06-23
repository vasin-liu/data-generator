# Render docs/test-feature-matrix.md from .planning/test-matrix.yaml.
param(
    [string]$MatrixFile = '.planning/test-matrix.yaml',
    [string]$OutFile = 'docs/test-feature-matrix.md'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$MatrixPath = if ([System.IO.Path]::IsPathRooted($MatrixFile)) { $MatrixFile } else { Join-Path $RepoRoot $MatrixFile }
$OutPath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path $RepoRoot $OutFile }

function Write-Step([string]$Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Format-Cell([object]$Value) {
    if ($null -eq $Value) { return '' }
    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        return (($Value | ForEach-Object { "$_" }) -join ', ')
    }
    return "$Value"
}

function Parse-MatrixRows([string]$YamlText) {
    $lines = $YamlText -split "`r?`n"
    $inRows = $false
    $rows = New-Object System.Collections.Generic.List[hashtable]
    $current = $null

    foreach ($line in $lines) {
        if ($line -match '^\s*rows:\s*$') {
            $inRows = $true
            continue
        }
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
            if ($raw -eq '') {
                $current[$key] = @()
            } elseif ($raw -match '^\[(.*)\]$') {
                $inner = $Matches[1].Trim()
                if ($inner -eq '') {
                    $current[$key] = @()
                } else {
                    $current[$key] = $inner -split ',\s*' | ForEach-Object { $_.Trim() }
                }
            } else {
                $current[$key] = $raw
            }
        }
    }
    if ($null -ne $current) { $rows.Add($current) }
    return $rows
}

Write-Step "Rendering feature matrix documentation"

if (-not (Test-Path $MatrixPath)) { throw "Matrix file not found: $MatrixPath" }

$yaml = Get-Content -Raw $MatrixPath
$rows = Parse-MatrixRows $yaml
if ($rows.Count -eq 0) { throw 'No rows found in matrix YAML' }

$outDir = Split-Path -Parent $OutPath
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$banner = @"
<!-- GENERATED — do not edit by hand. -->
<!-- Source: .planning/test-matrix.yaml -->
<!-- Generator: scripts/generate-test-matrix-doc.ps1 -->

# Test feature matrix

This document is generated from ``.planning/test-matrix.yaml``. Edit the YAML source of truth and re-run ``scripts/generate-test-matrix-doc.ps1``.

| id | capability | adapter | test_types | owner_module | status | tier | linked_tests |
|----|------------|---------|------------|--------------|--------|------|--------------|
"@

$tableRows = foreach ($row in $rows) {
    $cells = @(
        (Format-Cell $row.id),
        (Format-Cell $row.capability),
        (Format-Cell $row.adapter),
        (Format-Cell $row.test_types),
        (Format-Cell $row.owner_module),
        (Format-Cell $row.status),
        (Format-Cell $row.tier),
        (Format-Cell $row.linked_tests)
    )
    '| ' + ($cells -join ' | ') + ' |'
}

$content = $banner + "`n" + ($tableRows -join "`n") + "`n"
Set-Content -Path $OutPath -Value $content -Encoding utf8NoBOM
Write-Host "Wrote $($rows.Count) rows to $OutPath"
