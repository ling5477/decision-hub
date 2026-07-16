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

function Invoke-RuntimeBlockedContract {
    param([string]$RunId)
    & $CurrentExecutable -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $EntryPoint -Phase RuntimeBlockedContractTest -RunId $RunId -Seed 7 -ProjectRoot $ProjectRoot -PowerShellExecutable $CurrentExecutableName -ImplementationValidation false
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

function Assert-Rfc3339UtcJsonField {
    param([string]$Json, [string]$Field, [string]$Contract)
    $pattern = '"' + [Regex]::Escape($Field) + '"\s*:\s*"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z"'
    if ($Json -notmatch $pattern) {
        throw "$Contract field=$Field must be serialized as RFC3339 UTC"
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

function Assert-BlockedArtifactContract {
    param([string]$BlockedRoot, [string]$ExpectedRunId, [int]$ExpectedExit, [string]$ExpectedReason)
    $artifactRegistry = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot 'config\qdr7-capacity\qdr7-capacity-artifact-registry.json') | ConvertFrom-Json
    foreach ($name in $artifactRegistry.mandatory) {
        if (-not (Test-Path -LiteralPath (Join-Path $BlockedRoot $name) -PathType Leaf)) {
            throw "blocked artifact missing: $name"
        }
    }

    $summaryRaw = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'capacity-acceptance-summary.json')
    $summary = $summaryRaw | ConvertFrom-Json
    foreach ($field in $artifactRegistry.summaryRequired) {
        if ($null -eq $summary.PSObject.Properties[$field]) {
            throw "blocked summary field missing: $field"
        }
    }
    Assert-Equal 'BLOCKED' $summary.status 'blocked status'
    Assert-Equal $ExpectedExit ([int]$summary.internalExitCode) 'blocked internal exit'
    Assert-Equal 0 ([int]$summary.executedScenarioCount) 'mandatory scenario non-execution'
    Assert-Equal 15 ([int]$summary.mandatoryScenarioCount) 'mandatory scenario count'
    Assert-Equal 'BLOCKED' $summary.correctnessVerdict 'blocked correctness verdict'
    Assert-Equal 'BLOCKED' $summary.thresholdVerdict 'blocked threshold verdict'
    Assert-Equal 'BLOCKED' $summary.regressionVerdict 'blocked regression verdict'
    Assert-Equal 'BLOCKED' $summary.qualityVerdict 'blocked quality verdict'
    Assert-Equal 'PASS' $summary.artifactVerdict 'blocked artifact verdict'
    Assert-Equal 'PASS' $summary.secretVerdict 'blocked secret verdict'
    Assert-Equal 'PASS' $summary.teardownVerdict 'blocked teardown verdict'
    Assert-Equal $ExpectedReason $summary.reasonCode 'blocked reason code'
    Assert-Equal $ExpectedRunId $summary.runId 'blocked runId transmission'
    foreach ($field in @('startedAtUtc', 'finishedAtUtc', 'startedAt', 'completedAt')) {
        Assert-Rfc3339UtcJsonField -Json $summaryRaw -Field $field -Contract 'blocked summary timestamp'
    }

    $thresholdRaw = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'threshold-comparison.json')
    $threshold = $thresholdRaw | ConvertFrom-Json
    Assert-Equal 'BLOCKED' $threshold.status 'blocked threshold status'
    Assert-Equal 'FORMAL_SCENARIO_NOT_EXECUTED' $threshold.reason 'blocked threshold reason'
    Assert-Equal 0 @($threshold.comparisons).Count 'blocked threshold comparison count'
    foreach ($field in @('startedAtUtc', 'finishedAtUtc')) {
        Assert-Rfc3339UtcJsonField -Json $thresholdRaw -Field $field -Contract 'blocked threshold timestamp'
    }

    $secretScan = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'secret-scan.json') | ConvertFrom-Json
    Assert-Equal 0 ([int]$secretScan.findingCount) 'blocked secret scan'

    $manifestEntries = 0
    $manifestMismatch = 0
    foreach ($line in Get-Content -LiteralPath (Join-Path $BlockedRoot 'sha256-manifest.txt')) {
        $manifestEntries++
        $parts = $line -split '  ', 2
        if ($parts.Count -ne 2) {
            $manifestMismatch++
            continue
        }
        $target = Join-Path $BlockedRoot $parts[1]
        if (-not (Test-Path -LiteralPath $target -PathType Leaf) -or (Get-Sha256File -Path $target) -ne $parts[0]) {
            $manifestMismatch++
        }
    }
    if ($manifestEntries -le 0) {
        throw 'blocked manifest must contain entries'
    }
    Assert-Equal 0 $manifestMismatch 'blocked manifest integrity'
}

$validRunId = '20260715T150000Z'
Assert-Equal 0 (Invoke-Contract -RunId $validRunId -Seed '7' -RequestedExecutable $CurrentExecutableName) 'explicit executable binding'
Assert-Equal 0 (Invoke-Contract -RunId $validRunId -Seed '7' -RequestedExecutable 'AUTO') 'AUTO executable binding'
Assert-Equal 10 (Invoke-Contract -RunId '../unsafe' -Seed '7' -RequestedExecutable $CurrentExecutableName) 'invalid runId rejection'
Assert-Equal 10 (Invoke-Contract -RunId $validRunId -Seed '8' -RequestedExecutable $CurrentExecutableName) 'invalid seed rejection'

$blockedRunId = Get-UniqueRunId
Assert-Equal 10 (Invoke-Contract -RunId $blockedRunId -Seed '7' -RequestedExecutable '__qdr7_missing_executable__') 'missing executable rejection'
$blockedRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$blockedRunId"
Assert-BlockedArtifactContract -BlockedRoot $blockedRoot -ExpectedRunId $blockedRunId -ExpectedExit 10 -ExpectedReason 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'

$runtimeBlockedRunId = Get-UniqueRunId
Assert-Equal 20 (Invoke-RuntimeBlockedContract -RunId $runtimeBlockedRunId) 'runtime startup blocked classification'
$runtimeBlockedRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$runtimeBlockedRunId"
Assert-BlockedArtifactContract -BlockedRoot $runtimeBlockedRoot -ExpectedRunId $runtimeBlockedRunId -ExpectedExit 20 -ExpectedReason 'APPLICATION_CONTEXT_STARTUP_BLOCKED'

Write-Output 'QDR7_CAPACITY_RUNTIME_BINDING_CONTRACT=PASS'
Write-Output "POWERSHELL_EXECUTABLE_NAME=$CurrentExecutableName"
Write-Output "BLOCKED_CONTRACT_RUN_ID=$blockedRunId"
Write-Output "RUNTIME_BLOCKED_CONTRACT_RUN_ID=$runtimeBlockedRunId"
