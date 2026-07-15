[CmdletBinding()]
param()

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$ProjectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$EntryPoint = Join-Path $PSScriptRoot 'Invoke-Qdr7CapacityAcceptance.ps1'
$CurrentExecutable = (Get-Process -Id $PID).Path
$CurrentExecutableName = [IO.Path]::GetFileName($CurrentExecutable)

function Invoke-Contract {
    param(
        [string]$RunId,
        [string]$Seed,
        [string]$RequestedExecutable
    )
    & $CurrentExecutable -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $EntryPoint -Phase ContractTest -RunId $RunId -Seed $Seed -ProjectRoot $ProjectRoot -PowerShellExecutable $RequestedExecutable
    return $LASTEXITCODE
}

function Get-UniqueRunId {
    $candidate = [DateTime]::UtcNow
    if ($CurrentExecutableName -like 'pwsh*') {
        $candidate = $candidate.AddMinutes(1)
    }
    do {
        $runId = $candidate.ToString('yyyyMMddTHHmmssZ', [Globalization.CultureInfo]::InvariantCulture)
        $evidenceRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$runId"
        $candidate = $candidate.AddSeconds(1)
    } while (Test-Path -LiteralPath $evidenceRoot)
    return $runId
}

function Assert-Equal {
    param([object]$Expected, [object]$Actual, [string]$Contract)
    if ($Expected -ne $Actual) {
        throw "$Contract expected=$Expected actual=$Actual"
    }
}

function Get-Sha256File {
    param([string]$Path)
    $stream = [IO.File]::OpenRead($Path)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return (($sha.ComputeHash($stream) | ForEach-Object { $_.ToString('x2') }) -join '')
    }
    finally {
        $sha.Dispose()
        $stream.Dispose()
    }
}

$validRunId = '20260715T150000Z'
Assert-Equal 0 (Invoke-Contract -RunId $validRunId -Seed '7' -RequestedExecutable $CurrentExecutableName) 'explicit executable binding'
Assert-Equal 0 (Invoke-Contract -RunId $validRunId -Seed '7' -RequestedExecutable 'AUTO') 'AUTO executable binding'
Assert-Equal 10 (Invoke-Contract -RunId '../unsafe' -Seed '7' -RequestedExecutable $CurrentExecutableName) 'invalid runId rejection'
Assert-Equal 10 (Invoke-Contract -RunId $validRunId -Seed '8' -RequestedExecutable $CurrentExecutableName) 'invalid seed rejection'

$blockedRunId = Get-UniqueRunId
Assert-Equal 10 (Invoke-Contract -RunId $blockedRunId -Seed '7' -RequestedExecutable '__qdr7_missing_executable__') 'missing executable rejection'
$blockedRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$blockedRunId"
$artifactRegistry = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot 'config\qdr7-capacity\qdr7-capacity-artifact-registry.json') | ConvertFrom-Json
foreach ($name in $artifactRegistry.mandatory) {
    if (-not (Test-Path -LiteralPath (Join-Path $blockedRoot $name) -PathType Leaf)) {
        throw "blocked artifact missing: $name"
    }
}

$summary = Get-Content -Raw -LiteralPath (Join-Path $blockedRoot 'capacity-acceptance-summary.json') | ConvertFrom-Json
Assert-Equal 'BLOCKED' $summary.finalStatus 'blocked final status'
Assert-Equal 10 ([int]$summary.exitCode) 'blocked internal exit'
Assert-Equal 0 ([int]$summary.mandatoryScenariosExecuted) 'mandatory scenario non-execution'
Assert-Equal 15 ([int]$summary.mandatoryScenariosTotal) 'mandatory scenario count'
Assert-Equal $blockedRunId $summary.runId 'blocked runId transmission'
Assert-Equal 7 ([int]$summary.seed) 'blocked seed transmission'

$secretScan = Get-Content -Raw -LiteralPath (Join-Path $blockedRoot 'secret-scan.json') | ConvertFrom-Json
Assert-Equal 0 ([int]$secretScan.findingCount) 'blocked secret scan'

$manifestMismatch = 0
foreach ($line in Get-Content -LiteralPath (Join-Path $blockedRoot 'sha256-manifest.txt')) {
    $parts = $line -split '  ', 2
    if ($parts.Count -ne 2) {
        $manifestMismatch++
        continue
    }
    $target = Join-Path $blockedRoot $parts[1]
    if (-not (Test-Path -LiteralPath $target -PathType Leaf) -or (Get-Sha256File -Path $target) -ne $parts[0]) {
        $manifestMismatch++
    }
}
Assert-Equal 0 $manifestMismatch 'blocked manifest integrity'

Write-Output 'QDR7_CAPACITY_RUNTIME_BINDING_CONTRACT=PASS'
Write-Output "POWERSHELL_EXECUTABLE_NAME=$CurrentExecutableName"
Write-Output "BLOCKED_CONTRACT_RUN_ID=$blockedRunId"
