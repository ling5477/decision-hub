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
        [string]$RequestedExecutable,
        [string]$QualificationOnly = 'false'
    )
    & $CurrentExecutable -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $EntryPoint -Phase ContractTest -RunId $RunId -Seed $Seed -ProjectRoot $ProjectRoot -PowerShellExecutable $RequestedExecutable -QualificationOnly $QualificationOnly
    return $LASTEXITCODE
}

function Invoke-RuntimeBlockedContract {
    param([string]$RunId)
    & $CurrentExecutable -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $EntryPoint -Phase RuntimeBlockedContractTest -RunId $RunId -Seed 7 -ProjectRoot $ProjectRoot -PowerShellExecutable $CurrentExecutableName -ImplementationValidation false
    return $LASTEXITCODE
}

function Invoke-PartialFinalizerContract {
    param([string]$RunId)
    & $CurrentExecutable -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $EntryPoint -Phase PartialFinalizerContractTest -RunId $RunId -Seed 7 -ProjectRoot $ProjectRoot -PowerShellExecutable $CurrentExecutableName -ImplementationValidation false -QualificationOnly false
    return $LASTEXITCODE
}

function Get-UniqueRunId {
    param([bool]$QualificationOnly = $false)
    $candidate = [DateTime]::UtcNow
    if ($CurrentExecutableName -like 'pwsh*') {
        $candidate = $candidate.AddMinutes(1)
    }
    do {
        $runId = $candidate.ToString('yyyyMMddTHHmmssZ', [Globalization.CultureInfo]::InvariantCulture)
        $evidenceDirectory = $(if ($QualificationOnly) { 'qdr7-capacity-qualification' } else { 'qdr7-capacity-acceptance' })
        $evidenceRoot = Join-Path $ProjectRoot "target\$evidenceDirectory\$runId"
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
    param(
        [string]$BlockedRoot,
        [string]$ExpectedRunId,
        [int]$ExpectedExit,
        [string]$ExpectedReason,
        [string]$ExpectedThresholdReason = 'FORMAL_SCENARIO_NOT_EXECUTED'
    )
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
    Assert-Equal 0 ([int]$summary.startedScenarioCount) 'blocked started scenario count'
    Assert-Equal 0 ([int]$summary.partialScenarioCount) 'blocked partial scenario count'
    Assert-Equal 0 ([int]$summary.completedScenarioCount) 'blocked completed scenario count'
    Assert-Equal 0 ([int]$summary.passedScenarioCount) 'blocked passed scenario count'
    Assert-Equal 0 ([int]$summary.failedScenarioCount) 'blocked failed scenario count'
    Assert-Equal 0 ([int]$summary.blockedScenarioCount) 'blocked ledger verdict count'
    Assert-Equal 15 ([int]$summary.notStartedScenarioCount) 'blocked not-started scenario count'
    Assert-Equal 'BLOCKED' $summary.correctnessVerdict 'blocked correctness verdict'
    Assert-Equal 'BLOCKED' $summary.thresholdVerdict 'blocked threshold verdict'
    Assert-Equal 'BLOCKED' $summary.regressionVerdict 'blocked regression verdict'
    Assert-Equal 'BLOCKED' $summary.qualityVerdict 'blocked quality verdict'
    Assert-Equal 'PASS' $summary.artifactVerdict 'blocked artifact verdict'
    Assert-Equal 'PASS' $summary.secretVerdict 'blocked secret verdict'
    Assert-Equal 'PASS' $summary.teardownVerdict 'blocked teardown verdict'
    Assert-Equal $ExpectedReason $summary.reasonCode 'blocked reason code'
    Assert-Equal $ExpectedRunId $summary.runId 'blocked runId transmission'
    Assert-Equal $false ([bool]$summary.capacityAcceptanceExecuted) 'blocked capacity execution flag'
    foreach ($field in @('startedAtUtc', 'finishedAtUtc', 'startedAt', 'completedAt')) {
        Assert-Rfc3339UtcJsonField -Json $summaryRaw -Field $field -Contract 'blocked summary timestamp'
    }

    $thresholdRaw = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'threshold-comparison.json')
    $threshold = $thresholdRaw | ConvertFrom-Json
    Assert-Equal 'BLOCKED' $threshold.status 'blocked threshold status'
    Assert-Equal $ExpectedThresholdReason $threshold.reason 'blocked threshold reason'
    Assert-Equal 0 @($threshold.comparisons).Count 'blocked threshold comparison count'
    Assert-Equal 94 ([int]$threshold.notEvaluatedCount) 'blocked not-evaluated threshold count'
    foreach ($field in @('startedAtUtc', 'finishedAtUtc')) {
        Assert-Rfc3339UtcJsonField -Json $thresholdRaw -Field $field -Contract 'blocked threshold timestamp'
    }

    $secretScan = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'secret-scan.json') | ConvertFrom-Json
    Assert-Equal 0 ([int]$secretScan.findingCount) 'blocked secret scan'

    $ledger = Get-Content -Raw -LiteralPath (Join-Path $BlockedRoot 'scenario-ledger.json') | ConvertFrom-Json
    Assert-Equal 15 @($ledger.scenarios).Count 'blocked scenario ledger size'
    Assert-Equal 15 @($ledger.scenarios | Where-Object { $_.executionState -eq 'NOT_STARTED' }).Count 'blocked ledger not-started state'

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

function Assert-PartialFinalizerContract {
    param([string]$PartialRoot, [string]$ExpectedRunId)
    $summary = Get-Content -Raw -LiteralPath (Join-Path $PartialRoot 'capacity-acceptance-summary.json') | ConvertFrom-Json
    Assert-Equal 'BLOCKED' $summary.status 'partial finalizer status'
    Assert-Equal 100 ([int]$summary.internalExitCode) 'partial finalizer exit'
    Assert-Equal 2 ([int]$summary.startedScenarioCount) 'partial started count'
    Assert-Equal 1 ([int]$summary.partialScenarioCount) 'partial state count'
    Assert-Equal 1 ([int]$summary.completedScenarioCount) 'partial completed count'
    Assert-Equal 1 ([int]$summary.passedScenarioCount) 'partial passed count'
    Assert-Equal 0 ([int]$summary.failedScenarioCount) 'partial failed count'
    Assert-Equal 1 ([int]$summary.blockedScenarioCount) 'partial blocked count'
    Assert-Equal 13 ([int]$summary.notStartedScenarioCount) 'partial not-started count'
    Assert-Equal 2 ([int]$summary.executedScenarioCount) 'partial executed count'
    Assert-Equal $ExpectedRunId $summary.runId 'partial runId'
    Assert-Equal 'PASS' $summary.artifactVerdict 'partial artifact finalization'
    Assert-Equal 'PASS' $summary.secretVerdict 'partial secret scan'
    Assert-Equal 'PASS' $summary.teardownVerdict 'partial teardown'

    $ledger = Get-Content -Raw -LiteralPath (Join-Path $PartialRoot 'scenario-ledger.json') | ConvertFrom-Json
    $rate = @($ledger.scenarios | Where-Object { $_.scenarioId -eq 'rate-matrix' })
    Assert-Equal 1 $rate.Count 'partial rate ledger uniqueness'
    Assert-Equal 'PARTIAL' $rate[0].executionState 'started to partial normalization'
    Assert-Equal 'BLOCKED' $rate[0].verdict 'partial verdict normalization'
    Assert-Equal 2 ([int]$rate[0].roundsStarted) 'partial rounds started'
    Assert-Equal 1 ([int]$rate[0].roundsCompleted) 'partial rounds completed'

    $threshold = Get-Content -Raw -LiteralPath (Join-Path $PartialRoot 'threshold-comparison.json') | ConvertFrom-Json
    Assert-Equal 5 @($threshold.comparisons).Count 'partial comparisons preserved'
    Assert-Equal 5 ([int]$threshold.passedCount) 'partial passed comparisons preserved'
    Assert-Equal 89 ([int]$threshold.notEvaluatedCount) 'partial not-evaluated comparisons'
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

$qualificationBlockedRunId = Get-UniqueRunId -QualificationOnly $true
Assert-Equal 10 (Invoke-Contract -RunId $qualificationBlockedRunId -Seed '7' -RequestedExecutable '__qdr7_missing_executable__' -QualificationOnly 'true') 'qualification missing executable rejection'
$qualificationBlockedRoot = Join-Path $ProjectRoot "target\qdr7-capacity-qualification\$qualificationBlockedRunId"
Assert-BlockedArtifactContract -BlockedRoot $qualificationBlockedRoot -ExpectedRunId $qualificationBlockedRunId -ExpectedExit 10 -ExpectedReason 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED' -ExpectedThresholdReason 'QUALIFICATION_SCENARIO_NOT_EXECUTED'
$qualificationBlockedSummary = Get-Content -Raw -LiteralPath (Join-Path $qualificationBlockedRoot 'capacity-acceptance-summary.json') | ConvertFrom-Json
Assert-Equal 'NOT_EVALUATED' $qualificationBlockedSummary.formalAcceptanceVerdict 'qualification formal verdict isolation'
Assert-Equal 'BLOCKED' $qualificationBlockedSummary.qualificationVerdict 'qualification blocked verdict'

$runtimeBlockedRunId = Get-UniqueRunId
Assert-Equal 20 (Invoke-RuntimeBlockedContract -RunId $runtimeBlockedRunId) 'runtime startup blocked classification'
$runtimeBlockedRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$runtimeBlockedRunId"
Assert-BlockedArtifactContract -BlockedRoot $runtimeBlockedRoot -ExpectedRunId $runtimeBlockedRunId -ExpectedExit 20 -ExpectedReason 'APPLICATION_CONTEXT_STARTUP_BLOCKED'

$partialRunId = Get-UniqueRunId
Assert-Equal 100 (Invoke-PartialFinalizerContract -RunId $partialRunId) 'partial finalizer classification'
$partialRoot = Join-Path $ProjectRoot "target\qdr7-capacity-acceptance\$partialRunId"
Assert-PartialFinalizerContract -PartialRoot $partialRoot -ExpectedRunId $partialRunId

Write-Output 'QDR7_CAPACITY_RUNTIME_BINDING_CONTRACT=PASS'
Write-Output "POWERSHELL_EXECUTABLE_NAME=$CurrentExecutableName"
Write-Output "BLOCKED_CONTRACT_RUN_ID=$blockedRunId"
Write-Output "RUNTIME_BLOCKED_CONTRACT_RUN_ID=$runtimeBlockedRunId"
Write-Output "QUALIFICATION_BLOCKED_CONTRACT_RUN_ID=$qualificationBlockedRunId"
Write-Output "PARTIAL_FINALIZER_CONTRACT_RUN_ID=$partialRunId"
