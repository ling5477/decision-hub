[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Preflight', 'Finalize', 'ContractTest', 'RuntimeBlockedContractTest', 'PartialFinalizerContractTest', 'FormalPacketGateContractTest', 'ManifestContractTest')]
    [string]$Phase,

    [Parameter(Mandatory = $true)]
    [string]$RunId,

    [Parameter(Mandatory = $true)]
    [int]$Seed,

    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot,

    [string]$PowerShellExecutable = 'AUTO',

    [ValidateSet('true', 'false')]
    [string]$ImplementationValidation = 'false',

    [ValidateSet('true', 'false')]
    [string]$QualificationOnly = 'false',

    [switch]$PowerShellResolved
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$SchemaVersion = 'qdr7-capacity-1'
$CriteriaVersion = 'qdr7-capacity-criteria-1'
$UnitSystem = 'milliseconds-bytes-requestsPerSecond-percent-rfc3339-utc'
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
$QualificationOnlyEnabled = $QualificationOnly -eq 'true'
$EvidenceDirectoryName = $(if ($QualificationOnlyEnabled) { 'qdr7-capacity-qualification' } else { 'qdr7-capacity-acceptance' })
$EvidenceBase = [IO.Path]::GetFullPath((Join-Path $ProjectRoot "target\$EvidenceDirectoryName"))
$EvidenceRoot = [IO.Path]::GetFullPath((Join-Path $EvidenceBase $RunId))
$ConfigRoot = Join-Path $ProjectRoot 'config\qdr7-capacity'
$RegistryPath = Join-Path $EvidenceRoot 'resource-registry.json'
$StopMarker = Join-Path $EvidenceRoot 'sampler.stop'
$Utf8NoBom = New-Object Text.UTF8Encoding($false)
$ResolvedPowerShellIdentity = [ordered]@{ name = 'UNRESOLVED'; pathSha256 = $null }
$ImplementationValidationEnabled = $ImplementationValidation -eq 'true'
$HarnessVersion = 'qdr12-capacity-harness-1'
$EvidenceBinding = [ordered]@{
    attemptId = $RunId
    candidateSha = ('0' * 40)
    candidateTree = ('0' * 40)
    profileId = 'qdr7-capacity-acceptance'
    profileVersion = $CriteriaVersion
    scenarioSetHash = ('0' * 64)
    thresholdSetHash = ('0' * 64)
    environmentManifestHash = ('0' * 64)
    harnessVersion = $HarnessVersion
    harnessHash = ('0' * 64)
    generatedAt = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
}

function Get-UtcTimestamp {
    return [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
}

function ConvertTo-UtcTimestamp {
    param([object]$Value)
    if ($null -eq $Value) {
        return Get-UtcTimestamp
    }
    if ($Value -is [DateTime]) {
        return ([DateTime]$Value).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
    }
    $styles = [Globalization.DateTimeStyles]::AssumeUniversal -bor [Globalization.DateTimeStyles]::AdjustToUniversal
    $parsed = [DateTime]::Parse([string]$Value, [Globalization.CultureInfo]::InvariantCulture, $styles)
    return $parsed.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
}

function Write-Utf8File {
    param([string]$Path, [string]$Content)
    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        [IO.Directory]::CreateDirectory($parent) | Out-Null
    }
    [IO.File]::WriteAllText($Path, $Content, $Utf8NoBom)
}

function Write-JsonFile {
    param([string]$Path, [object]$Value)
    Write-Utf8File -Path $Path -Content (($Value | ConvertTo-Json -Depth 30) + "`n")
}

function Read-JsonFile {
    param([string]$Path)
    # Windows PowerShell 5.1 treats UTF-8 without a BOM as ANSI, which can invalidate JSON.
    return ([IO.File]::ReadAllText($Path, $Utf8NoBom) | ConvertFrom-Json)
}

function New-Artifact {
    param(
        [string]$Scenario,
        [string]$Status,
        [string]$CommitSha,
        [string]$StartedAt,
        [string]$FinishedAt
    )
    $started = [DateTime]::Parse($StartedAt, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::AdjustToUniversal)
    $finished = [DateTime]::Parse($FinishedAt, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::AdjustToUniversal)
    return [ordered]@{
        schemaVersion = $SchemaVersion
        runId = $RunId
        commitSha = $CommitSha
        scenario = $Scenario
        status = $Status
        startedAtUtc = $StartedAt
        finishedAtUtc = $FinishedAt
        durationMs = [Math]::Max(0, [long]($finished - $started).TotalMilliseconds)
        seed = $Seed
        unitSystem = $UnitSystem
        missingValues = @()
        criteriaVersion = $CriteriaVersion
        attemptId = $EvidenceBinding.attemptId
        candidateSha = $EvidenceBinding.candidateSha
        candidateTree = $EvidenceBinding.candidateTree
        profileId = $EvidenceBinding.profileId
        profileVersion = $EvidenceBinding.profileVersion
        scenarioSetHash = $EvidenceBinding.scenarioSetHash
        thresholdSetHash = $EvidenceBinding.thresholdSetHash
        environmentManifestHash = $EvidenceBinding.environmentManifestHash
        harnessVersion = $EvidenceBinding.harnessVersion
        harnessHash = $EvidenceBinding.harnessHash
        generatedAt = $EvidenceBinding.generatedAt
    }
}

function Get-ExecutionMode {
    if ($ImplementationValidationEnabled) {
        return 'IMPLEMENTATION_VALIDATION'
    }
    if ($QualificationOnlyEnabled) {
        return 'QUALIFICATION'
    }
    return 'FORMAL'
}

function Set-ObjectProperty {
    param([object]$Target, [string]$Name, [object]$Value)
    if ($Target -is [Collections.IDictionary]) {
        $Target[$Name] = $Value
    }
    else {
        $Target | Add-Member -NotePropertyName $Name -NotePropertyValue $Value -Force
    }
}

function Set-SummaryContract {
    param(
        [object]$Summary,
        [string]$StartedAt,
        [string]$CompletedAt,
        [string]$Status,
        [int]$InternalExitCode,
        [int]$MandatoryScenarioCount,
        [int]$ExecutedScenarioCount,
        [string]$CorrectnessVerdict,
        [string]$ThresholdVerdict,
        [string]$RegressionVerdict,
        [string]$QualityVerdict,
        [string]$ArtifactVerdict,
        [string]$SecretVerdict,
        [string]$TeardownVerdict,
        [string]$ReasonCode
    )
    # PowerShell 7 converts ISO JSON strings to DateTime; restore the frozen .fffZ form.
    Set-ObjectProperty -Target $Summary -Name 'startedAtUtc' -Value (ConvertTo-UtcTimestamp -Value $StartedAt)
    Set-ObjectProperty -Target $Summary -Name 'finishedAtUtc' -Value (ConvertTo-UtcTimestamp -Value $CompletedAt)
    Set-ObjectProperty -Target $Summary -Name 'startedAt' -Value $StartedAt
    Set-ObjectProperty -Target $Summary -Name 'completedAt' -Value $CompletedAt
    Set-ObjectProperty -Target $Summary -Name 'status' -Value $Status
    Set-ObjectProperty -Target $Summary -Name 'internalExitCode' -Value $InternalExitCode
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenarioCount' -Value $MandatoryScenarioCount
    foreach ($field in @('startedScenarioCount', 'partialScenarioCount', 'completedScenarioCount', 'passedScenarioCount', 'failedScenarioCount', 'blockedScenarioCount')) {
        if ($null -eq $Summary.PSObject.Properties[$field] -and -not ($Summary -is [Collections.IDictionary] -and $Summary.Contains($field))) {
            Set-ObjectProperty -Target $Summary -Name $field -Value 0
        }
    }
    if ($null -eq $Summary.PSObject.Properties['notStartedScenarioCount'] -and -not ($Summary -is [Collections.IDictionary] -and $Summary.Contains('notStartedScenarioCount'))) {
        Set-ObjectProperty -Target $Summary -Name 'notStartedScenarioCount' -Value $MandatoryScenarioCount
    }
    Set-ObjectProperty -Target $Summary -Name 'executedScenarioCount' -Value $ExecutedScenarioCount
    Set-ObjectProperty -Target $Summary -Name 'correctnessVerdict' -Value $CorrectnessVerdict
    Set-ObjectProperty -Target $Summary -Name 'thresholdVerdict' -Value $ThresholdVerdict
    Set-ObjectProperty -Target $Summary -Name 'regressionVerdict' -Value $RegressionVerdict
    Set-ObjectProperty -Target $Summary -Name 'qualityVerdict' -Value $QualityVerdict
    Set-ObjectProperty -Target $Summary -Name 'artifactVerdict' -Value $ArtifactVerdict
    Set-ObjectProperty -Target $Summary -Name 'secretVerdict' -Value $SecretVerdict
    Set-ObjectProperty -Target $Summary -Name 'teardownVerdict' -Value $TeardownVerdict
    Set-ObjectProperty -Target $Summary -Name 'reasonCode' -Value $ReasonCode
    if ($null -eq $Summary.PSObject.Properties['capacityAcceptanceExecuted'] -and -not ($Summary -is [Collections.IDictionary] -and $Summary.Contains('capacityAcceptanceExecuted'))) {
        Set-ObjectProperty -Target $Summary -Name 'capacityAcceptanceExecuted' -Value $false
    }
    if ($null -eq $Summary.PSObject.Properties['formalAcceptanceVerdict'] -and -not ($Summary -is [Collections.IDictionary] -and $Summary.Contains('formalAcceptanceVerdict'))) {
        Set-ObjectProperty -Target $Summary -Name 'formalAcceptanceVerdict' -Value 'NOT_EVALUATED'
    }
    if ($null -eq $Summary.PSObject.Properties['qualificationVerdict'] -and -not ($Summary -is [Collections.IDictionary] -and $Summary.Contains('qualificationVerdict'))) {
        Set-ObjectProperty -Target $Summary -Name 'qualificationVerdict' -Value 'NOT_EVALUATED'
    }
    Set-ObjectProperty -Target $Summary -Name 'finalStatus' -Value $Status
    Set-ObjectProperty -Target $Summary -Name 'exitCode' -Value $InternalExitCode
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenariosTotal' -Value $MandatoryScenarioCount
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenariosExecuted' -Value $ExecutedScenarioCount
}

function Get-ScenarioRoundsRequired {
    param([string]$ScenarioId)
    if ($ScenarioId -eq 'rate-matrix') {
        return 15
    }
    if ($ScenarioId -in @('cold-start-quota', 'tenant-environment-isolation', 'nonce-race', 'tenant-scoped-cleanup', 'postgres-same-pool-recovery', 'spring-context-restart', 'postgres-persistent-volume-restart')) {
        return 3
    }
    return 1
}

function Get-ScenarioArtifactRefs {
    param([string]$ScenarioId)
    switch ($ScenarioId) {
        'actual-wiring' { return @('actual-wiring.json') }
        'rate-matrix' { return @('rate-matrix.csv', 'rate-summary.json') }
        'cold-start-quota' { return @('quota-atomicity.json') }
        'tenant-environment-isolation' { return @('tenant-isolation.json') }
        'canonical-source-fail-closed' { return @('tenant-isolation.json') }
        'nonce-race' { return @('nonce-race.json') }
        'idempotency-lifecycle' { return @('idempotency-lifecycle.json') }
        'tenant-scoped-cleanup' { return @('cleanup-timeline.csv', 'cleanup-summary.json') }
        'postgres-hikari-contention' { return @('postgres-hikari-series.csv', 'postgres-hikari-summary.json') }
        'postgres-same-pool-recovery' { return @('recovery-timeline.csv', 'recovery-summary.json', 'restart-results.json') }
        'spring-context-restart' { return @('restart-results.json') }
        'postgres-persistent-volume-restart' { return @('restart-results.json') }
        'post-recovery-concurrency' { return @('recovery-timeline.csv', 'post-recovery-summary.json') }
        'full-regression' { return @('full-regression.log', 'full-regression-summary.json') }
        'quality-gate' { return @('quality.log', 'quality-summary.json') }
        default { return @() }
    }
}

function New-ScenarioLedgerRow {
    param([string]$ScenarioId)
    return [ordered]@{
        scenarioId = $ScenarioId
        mandatory = $true
        executionState = 'NOT_STARTED'
        verdict = 'NOT_EVALUATED'
        startedAt = $null
        completedAt = $null
        roundsRequired = Get-ScenarioRoundsRequired -ScenarioId $ScenarioId
        roundsStarted = 0
        roundsCompleted = 0
        measurementsCaptured = 0
        comparisonsExecuted = 0
        reasonCode = 'NOT_STARTED'
        artifactRefs = @(Get-ScenarioArtifactRefs -ScenarioId $ScenarioId)
    }
}

function Get-NormalizedScenarioLedger {
    param([string]$CommitSha, [string]$StartedAt)
    $scenarioRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')
    $ledgerPath = Join-Path $EvidenceRoot 'scenario-ledger.json'
    if (Test-Path -LiteralPath $ledgerPath -PathType Leaf) {
        $artifact = Read-JsonFile -Path $ledgerPath
    }
    else {
        $artifact = New-Artifact -Scenario 'scenario-ledger' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt (Get-UtcTimestamp)
        Set-ObjectProperty -Target $artifact -Name 'scenarios' -Value @()
    }

    $existingRows = @($artifact.scenarios)
    $integrityFindings = New-Object Collections.Generic.List[string]
    $knownScenarioIds = @($scenarioRegistry.fixedOrder | ForEach-Object { [string]$_ })
    foreach ($scenarioId in $knownScenarioIds) {
        if (@($existingRows | Where-Object { $_.scenarioId -eq $scenarioId }).Count -gt 1) {
            $integrityFindings.Add("SCENARIO_CARDINALITY_INVALID:$scenarioId")
        }
    }
    foreach ($row in $existingRows) {
        if ([string]$row.scenarioId -notin $knownScenarioIds) {
            $integrityFindings.Add("ORPHAN_SCENARIO_RESULT:$($row.scenarioId)")
        }
    }
    $normalizedRows = New-Object Collections.Generic.List[object]
    foreach ($scenarioId in $scenarioRegistry.fixedOrder) {
        $row = @($existingRows | Where-Object { $_.scenarioId -eq $scenarioId } | Select-Object -First 1)
        if ($row.Count -eq 0) {
            $current = New-ScenarioLedgerRow -ScenarioId ([string]$scenarioId)
        }
        else {
            $current = $row[0]
            foreach ($field in @('mandatory', 'executionState', 'verdict', 'startedAt', 'completedAt', 'roundsRequired', 'roundsStarted', 'roundsCompleted', 'measurementsCaptured', 'comparisonsExecuted', 'reasonCode', 'artifactRefs')) {
                if ($null -eq $current.PSObject.Properties[$field]) {
                    $defaults = New-ScenarioLedgerRow -ScenarioId ([string]$scenarioId)
                    Set-ObjectProperty -Target $current -Name $field -Value $defaults[$field]
                }
            }
        }
        if ($current.executionState -eq 'STARTED') {
            $current.executionState = 'PARTIAL'
            if ($current.verdict -eq 'NOT_EVALUATED') {
                $current.verdict = 'BLOCKED'
            }
            if ([string]::IsNullOrWhiteSpace([string]$current.reasonCode) -or $current.reasonCode -eq 'SCENARIO_STARTED') {
                $current.reasonCode = 'HARNESS_INTERRUPTED_AFTER_SCENARIO_START'
            }
            $current.completedAt = Get-UtcTimestamp
        }
        $normalizedRows.Add($current)
    }

    Set-ObjectProperty -Target $artifact -Name 'scenarios' -Value $normalizedRows.ToArray()
    Set-ObjectProperty -Target $artifact -Name 'finishedAtUtc' -Value (Get-UtcTimestamp)
    $partialCount = @($normalizedRows | Where-Object { $_.executionState -eq 'PARTIAL' }).Count
    $completedCount = @($normalizedRows | Where-Object { $_.executionState -eq 'COMPLETED' }).Count
    $passedCount = @($normalizedRows | Where-Object { $_.verdict -eq 'PASS' }).Count
    $failedCount = @($normalizedRows | Where-Object { $_.verdict -eq 'FAIL' }).Count
    $blockedCount = @($normalizedRows | Where-Object { $_.verdict -eq 'BLOCKED' }).Count
    $notStartedCount = @($normalizedRows | Where-Object { $_.executionState -eq 'NOT_STARTED' }).Count
    $ledgerStatus = $(if ($failedCount -gt 0) { 'FAIL' } elseif ($passedCount -eq [int]$scenarioRegistry.mandatoryCount) { 'PASS' } else { 'BLOCKED' })
    Set-ObjectProperty -Target $artifact -Name 'status' -Value $ledgerStatus
    Write-JsonFile -Path $ledgerPath -Value $artifact

    return [ordered]@{
        artifact = $artifact
        scenarios = $normalizedRows.ToArray()
        startedScenarioCount = $partialCount + $completedCount
        partialScenarioCount = $partialCount
        completedScenarioCount = $completedCount
        passedScenarioCount = $passedCount
        failedScenarioCount = $failedCount
        blockedScenarioCount = $blockedCount
        notStartedScenarioCount = $notStartedCount
        executedScenarioCount = $partialCount + $completedCount
        integrityFindings = $integrityFindings.ToArray()
    }
}

function Set-SummaryScenarioCounts {
    param([object]$Summary, [object]$Ledger)
    foreach ($field in @('startedScenarioCount', 'partialScenarioCount', 'completedScenarioCount', 'passedScenarioCount', 'failedScenarioCount', 'blockedScenarioCount', 'notStartedScenarioCount', 'executedScenarioCount')) {
        Set-ObjectProperty -Target $Summary -Name $field -Value ([int]$Ledger[$field])
    }
}

function Write-MissingScenarioArtifacts {
    param(
        [string]$CommitSha,
        [string]$StartedAt,
        [string]$Status,
        [string]$ReasonCode
    )
    $finished = Get-UtcTimestamp
    $artifactRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json')
    foreach ($name in $artifactRegistry.mandatory) {
        $path = Join-Path $EvidenceRoot $name
        if ((Test-Path -LiteralPath $path -PathType Leaf) -or $name -in @('capacity-acceptance-summary.json', 'harness-exit-code.txt', 'secret-scan.json', 'sha256-manifest.txt')) {
            continue
        }
        if ($name -eq 'threshold-comparison.json') {
            $threshold = New-Artifact -Scenario 'threshold-comparison' -Status $Status -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
            $threshold.comparisons = @()
            $threshold.comparisonCount = 0
            $threshold.comparisonsExecuted = 0
            $threshold.passedCount = 0
            $threshold.failedCount = 0
            $threshold.blockedCount = 0
            $threshold.coveredThresholdLeaves = 0
            $threshold.declaredThresholdLeaves = 41
            $threshold.notEvaluatedCount = 99
            $threshold.notEvaluatedThresholds = 99
            $threshold.reason = $(if ($QualificationOnlyEnabled) { 'QUALIFICATION_SCENARIO_NOT_EXECUTED' } else { 'FORMAL_SCENARIO_NOT_EXECUTED' })
            $threshold.missingValues = @('scenarioMeasurements')
            Write-JsonFile -Path $path -Value $threshold
        }
        elseif ($name.EndsWith('.json')) {
            $placeholder = New-Artifact -Scenario ([IO.Path]::GetFileNameWithoutExtension($name)) -Status $Status -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
            $placeholder.reason = $ReasonCode
            $placeholder.missingValues = @('scenarioExecution')
            Write-JsonFile -Path $path -Value $placeholder
        }
        elseif ($name.EndsWith('.csv')) {
            Write-Utf8File -Path $path -Content "schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,missingReason`n"
        }
        elseif ($name.EndsWith('.log')) {
            Write-Utf8File -Path $path -Content "$Status / $ReasonCode`n"
        }
    }
}

function Get-Sha256 {
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

function Get-GitValue {
    param([string[]]$Arguments)
    $output = & git -C $ProjectRoot @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git command failed: $($Arguments -join ' ')"
    }
    return (($output | Out-String).Trim())
}

function Get-GitPathList {
    param([string[]]$Arguments)
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git -C $ProjectRoot @Arguments 2>$null)
        $gitExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($gitExitCode -ne 0) {
        throw "git path command failed: $($Arguments -join ' ')"
    }
    return @($output | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Invoke-NativeCommand {
    param([string]$Executable, [string[]]$Arguments)
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& $Executable @Arguments 2>&1)
        $nativeExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
    return [ordered]@{
        exitCode = $nativeExitCode
        text = (($output | Out-String).Trim())
    }
}

function Get-FreeLoopbackPort {
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    }
    finally {
        $listener.Stop()
    }
}

function Invoke-NativeCommandWithTimeout {
    param(
        [string]$Executable,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [int]$TimeoutSeconds
    )
    $stdoutPath = Join-Path $WorkingDirectory ('native-' + [Guid]::NewGuid().ToString('N') + '.stdout')
    $stderrPath = Join-Path $WorkingDirectory ('native-' + [Guid]::NewGuid().ToString('N') + '.stderr')
    $process = $null
    try {
        $process = Start-Process -FilePath $Executable -ArgumentList $Arguments -PassThru -NoNewWindow -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill()
            $process.WaitForExit()
            return [ordered]@{ exitCode = -1; text = ''; timedOut = $true }
        }
        # Refresh the Process object after redirected streams have reached EOF.
        $process.WaitForExit()
        $stdout = $(if (Test-Path -LiteralPath $stdoutPath -PathType Leaf) { [IO.File]::ReadAllText($stdoutPath, $Utf8NoBom) } else { '' })
        $stderr = $(if (Test-Path -LiteralPath $stderrPath -PathType Leaf) { [IO.File]::ReadAllText($stderrPath, $Utf8NoBom) } else { '' })
        return [ordered]@{
            exitCode = $process.ExitCode
            text = (($stdout + $(if ([string]::IsNullOrWhiteSpace($stderr)) { '' } else { "`n$stderr" })).Trim())
            timedOut = $false
        }
    }
    finally {
        if ($null -ne $process) {
            $process.Dispose()
        }
        foreach ($path in @($stdoutPath, $stderrPath)) {
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                Remove-Item -LiteralPath $path -Force
            }
        }
    }
}

function Get-HarnessHash {
    $paths = @(
        'scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1',
        'scripts/qdr7-capacity/Watch-Qdr7CapacityResources.ps1',
        'config/qdr7-capacity/qdr7-capacity-artifact-registry.json',
        'config/qdr7-capacity/qdr7-capacity-artifacts.schema.json',
        'config/qdr7-capacity/qdr7-capacity-environment-admission.json',
        'config/qdr7-capacity/qdr7-capacity-scenario-registry.json',
        'config/qdr7-capacity/qdr7-capacity-threshold-consumers.json',
        'config/qdr7-capacity/qdr7-capacity-thresholds.json',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityArtifactSupport.java',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityEnvironmentAdmission.java',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityEvidenceFinalizer.java',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityResourceEvidence.java',
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityRuntimeSafetyProbe.java'
    )
    $rows = New-Object Collections.Generic.List[string]
    foreach ($relative in $paths) {
        $path = Join-Path $ProjectRoot $relative
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "harness hash input missing: $relative"
        }
        $rows.Add("$relative=$(Get-Sha256 -Path $path)")
    }
    return Get-Sha256Text -Value (($rows -join "`n") + "`n")
}

function Get-AdvertisedDevSha {
    $result = Invoke-NativeCommand -Executable 'git' -Arguments @('-C', $ProjectRoot, 'ls-remote', '--heads', 'origin', 'refs/heads/dev')
    if ($result.exitCode -ne 0) {
        return ''
    }
    $match = [regex]::Match($result.text, '^(?<sha>[a-f0-9]{40})\s+')
    return $(if ($match.Success) { $match.Groups['sha'].Value } else { '' })
}

function Set-EvidenceBinding {
    param(
        [string]$CandidateSha,
        [string]$CandidateTree,
        [string]$ProfileId,
        [string]$ProfileVersion,
        [string]$ScenarioSetHash,
        [string]$ThresholdSetHash,
        [string]$EnvironmentManifestHash,
        [string]$HarnessHash
    )
    $EvidenceBinding.attemptId = $RunId
    $EvidenceBinding.candidateSha = $CandidateSha
    $EvidenceBinding.candidateTree = $CandidateTree
    $EvidenceBinding.profileId = $ProfileId
    $EvidenceBinding.profileVersion = $ProfileVersion
    $EvidenceBinding.scenarioSetHash = $ScenarioSetHash
    $EvidenceBinding.thresholdSetHash = $ThresholdSetHash
    $EvidenceBinding.environmentManifestHash = $EnvironmentManifestHash
    $EvidenceBinding.harnessVersion = $HarnessVersion
    $EvidenceBinding.harnessHash = $HarnessHash
    $EvidenceBinding.generatedAt = Get-UtcTimestamp
}

function Add-BindingToObject {
    param([object]$Target)
    foreach ($field in $EvidenceBinding.Keys) {
        Set-ObjectProperty -Target $Target -Name $field -Value $EvidenceBinding[$field]
    }
}

function Write-ExecutionManifest {
    param(
        [string]$CommitSha,
        [string]$OriginSha,
        [string]$AdvertisedSha,
        [string]$StartedAt,
        [string]$FinishedAt,
        [string]$Status
    )
    $manifest = New-Artifact -Scenario 'capacity-execution-manifest' -Status $Status -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $FinishedAt
    $manifest.originSha = $OriginSha
    $manifest.advertisedSha = $AdvertisedSha
    $manifest.formalCapacityExecuted = $false
    $manifest.formalPacketPrepared = (Get-ExecutionMode) -eq 'FORMAL'
    $manifest.executionMode = Get-ExecutionMode
    $manifest.scenarioDispatchState = 'PENDING'
    $manifest.expectedMandatoryScenarios = 15
    $manifest.expectedThresholdLeaves = 41
    $manifest.expectedThresholdComparisons = 99
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-execution-manifest.json') -Value $manifest
}

function Get-ProcessParentId {
    param([int]$ProcessId)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId"
    if ($null -eq $process) {
        throw "cannot resolve parent process for PID $ProcessId"
    }
    return [int]$process.ParentProcessId
}

function Test-ImplementationValidationPath {
    param([string]$Path)
    $normalized = $Path.Replace('\', '/')
    if ($normalized -in @('.gitattributes', 'AGENTS.md', 'CLAUDE.md', 'README.md', 'pom.xml', 'dh-bom/pom.xml', 'dh-app/pom.xml')) {
        return $true
    }
    foreach ($prefix in @(
        'dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/',
        'dh-app/src/test/resources/qdr7-capacity/',
        'scripts/qdr7-capacity/',
        'config/qdr7-capacity/',
        'docs/current/'
    )) {
        if ($normalized.StartsWith($prefix, [StringComparison]::Ordinal)) {
            return $true
        }
    }
    return $false
}

function Write-BlockedPreflight {
    param(
        [string]$CommitSha,
        [string]$StartedAt,
        [object[]]$Checks,
        [string[]]$Blockers
    )
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
    $finished = Get-UtcTimestamp
    $environment = New-Artifact -Scenario 'environment' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $environment.safeSummary = 'environment preflight did not satisfy the frozen baseline'
    $criteriaPath = Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json'
    $criteria = Read-JsonFile -Path $criteriaPath
    $criteriaSourcePath = Join-Path $ProjectRoot $criteria.sourceDocument
    $environment.criteriaSourceSha256 = $(if (Test-Path -LiteralPath $criteriaSourcePath -PathType Leaf) { Get-Sha256 -Path $criteriaSourcePath } else { $null })
    $environment.powerShellExecutable = $ResolvedPowerShellIdentity
    $environment.missingValues = @('environmentBaseline')
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment
    $preflight = New-Artifact -Scenario 'environment-preflight' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $preflight.checks = $Checks
    $preflight.blockerCode = 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'
    $preflight.blockers = $Blockers
    $preflight.powerShellExecutable = $ResolvedPowerShellIdentity
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'preflight.json') -Value $preflight

    $resourceRegistry = New-Artifact -Scenario 'resource-registry' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $resourceRegistry.containerName = "dh-qdr7-capacity-$RunId"
    $resourceRegistry.volumeName = "dh-qdr7-capacity-$RunId"
    $resourceRegistry.samplerPid = $null
    $resourceRegistry.resourcesCreated = $false
    $resourceRegistry.teardown = [ordered]@{
        sampler = 'NOT_STARTED'
        container = 'REMOVED_OR_ABSENT'
        volume = 'REMOVED_OR_ABSENT'
        residual = 'NONE'
    }
    Write-JsonFile -Path $RegistryPath -Value $resourceRegistry

    $scenarioRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')
    $ledger = Get-NormalizedScenarioLedger -CommitSha $CommitSha -StartedAt $StartedAt
    $summary = New-Artifact -Scenario 'capacity-acceptance' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $summary.finalStatus = 'BLOCKED'
    $summary.exitCode = 10
    $summary.firstBlocker = 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'
    $summary.findings = $Blockers
    $summary.capacityAcceptanceExecuted = $false
    $summary.mandatoryScenariosExecuted = 0
    $summary.mandatoryScenariosTotal = [int]$scenarioRegistry.mandatoryCount
    $summary.scenarioStatuses = @($scenarioRegistry.fixedOrder | ForEach-Object { [ordered]@{ scenarioId = $_; status = 'NOT_RUN'; mandatory = $true } })
    $summary.artifactValidationFindings = @()
    $summary.secretFindingCount = 0
    $summary.teardown = $resourceRegistry.teardown
    Set-SummaryContract -Summary $summary -StartedAt $StartedAt -CompletedAt $finished -Status 'BLOCKED' -InternalExitCode 10 -MandatoryScenarioCount ([int]$scenarioRegistry.mandatoryCount) -ExecutedScenarioCount 0 -CorrectnessVerdict 'BLOCKED' -ThresholdVerdict 'BLOCKED' -RegressionVerdict 'BLOCKED' -QualityVerdict 'BLOCKED' -ArtifactVerdict 'PASS' -SecretVerdict 'PASS' -TeardownVerdict 'PASS' -ReasonCode 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'
    Set-SummaryScenarioCounts -Summary $summary -Ledger $ledger
    Set-ObjectProperty -Target $summary -Name 'capacityAcceptanceExecuted' -Value $false
    $nonFormalMode = $ImplementationValidationEnabled -or $QualificationOnlyEnabled
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($nonFormalMode) { 'NOT_EVALUATED' } else { 'BLOCKED' })
    Set-ObjectProperty -Target $summary -Name 'qualificationVerdict' -Value $(if ($QualificationOnlyEnabled) { 'BLOCKED' } else { 'NOT_EVALUATED' })
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-acceptance-summary.json') -Value $summary
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'harness-exit-code.txt') -Content "10`n"
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'commands.txt') -Content ("1 | $StartedAt | . | qdr7 capacity preflight | BLOCKED`n")

    Write-MissingScenarioArtifacts -CommitSha $CommitSha -StartedAt $StartedAt -Status 'BLOCKED' -ReasonCode 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'

    $secretFindings = Invoke-SecretScan -CommitSha $CommitSha
    if ($secretFindings -gt 0) {
        $summary.status = 'FAIL'
        $summary.finalStatus = 'FAIL'
        $summary.exitCode = 90
        $summary.internalExitCode = 90
        $summary.secretVerdict = 'FAIL'
        $summary.secretFindingCount = $secretFindings
        Write-Utf8File -Path (Join-Path $EvidenceRoot 'harness-exit-code.txt') -Content "90`n"
    }
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-acceptance-summary.json') -Value $summary
    Write-ArtifactInventory -CommitSha $CommitSha -StartedAt $StartedAt
    Write-CapacityFinalVerdict -CommitSha $CommitSha -StartedAt $StartedAt -Verdict $(if ($nonFormalMode) { 'NOT_EVALUATED' } else { 'BLOCKED' }) -ReasonCode 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED' -IntegrityFindings @()
    Write-Manifest
}

function Test-FullSha256Digest {
    param([string]$Value)
    return -not [string]::IsNullOrWhiteSpace($Value) -and $Value -match '^sha256:[a-f0-9]{64}$'
}

function Get-DigestHex {
    param([string]$Value)
    if (-not (Test-FullSha256Digest -Value $Value)) {
        return ''
    }
    return $Value.Substring(7)
}

function Test-ImageIdentityEquality {
    param(
        [string]$LeftKind,
        [string]$LeftDigest,
        [string]$LeftMediaType,
        [string]$RightKind,
        [string]$RightDigest,
        [string]$RightMediaType
    )
    $knownKinds = @('OCI_INDEX_DIGEST', 'PLATFORM_MANIFEST_DIGEST', 'CONFIG_DIGEST', 'REPO_DIGEST')
    return $LeftKind -in $knownKinds -and
        $RightKind -in $knownKinds -and
        $LeftKind -eq $RightKind -and
        (Test-FullSha256Digest -Value $LeftDigest) -and
        $LeftDigest -eq $RightDigest -and
        -not [string]::IsNullOrWhiteSpace($LeftMediaType) -and
        $LeftMediaType -eq $RightMediaType
}

$PostgresIdentityBlockerTaxonomy = @(
    'POSTGRES_INDEX_DIGEST_INVALID',
    'POSTGRES_INDEX_MEDIA_TYPE_INVALID',
    'POSTGRES_PLATFORM_NOT_FOUND',
    'POSTGRES_PLATFORM_AMBIGUOUS',
    'POSTGRES_PLATFORM_MANIFEST_MISMATCH',
    'POSTGRES_CONFIG_DIGEST_MISMATCH',
    'POSTGRES_REPO_DIGEST_MEMBERSHIP_MISSING',
    'POSTGRES_LOCAL_IDENTITY_UNKNOWN',
    'POSTGRES_EXECUTED_IDENTITY_UNKNOWN',
    'POSTGRES_EXECUTED_PLATFORM_MISMATCH',
    'POSTGRES_IDENTITY_DOMAIN_MISMATCH',
    'POSTGRES_IDENTITY_RESOLUTION_FAILED'
)

function Resolve-PostgresImageIdentityContract {
    param([object]$Admission)

    $blockers = New-Object Collections.Generic.List[string]
    function Add-IdentityBlocker {
        param([string]$Code)
        if ($Code -notin $PostgresIdentityBlockerTaxonomy) {
            throw "unknown PostgreSQL image identity blocker: $Code"
        }
        if (-not $blockers.Contains($Code)) {
            $blockers.Add($Code)
        }
    }

    $requiredReference = [string]$Admission.requiredImageReference
    $requiredIndexDigest = [string]$Admission.requiredIndexDigest
    $requiredIndexMediaType = [string]$Admission.requiredIndexMediaType
    $expectedManifestDigest = [string]$Admission.expectedPlatformManifestDigest
    $expectedManifestMediaType = [string]$Admission.expectedPlatformManifestMediaType
    $expectedConfigDigest = [string]$Admission.expectedPlatformConfigDigest
    $targetOs = [string]$Admission.targetPlatform.os
    $targetArchitecture = [string]$Admission.targetPlatform.architecture
    $targetVariant = [string]$Admission.targetPlatform.variant
    $repoDigests = @()
    $localObservedDigest = ''
    $localObservedKind = 'UNKNOWN'
    $localObservedMediaType = ''
    $resolvedManifestDigest = ''
    $resolvedManifestMediaType = ''
    $resolvedConfigDigest = ''
    $resolutionSource = 'UNRESOLVED'

    if ([string]$Admission.identityContractId -ne 'POSTGRES_IMAGE_IDENTITY_CONTRACT_V2' -or [int]$Admission.identityContractVersion -ne 2) {
        Add-IdentityBlocker 'POSTGRES_IDENTITY_DOMAIN_MISMATCH'
    }
    if (-not (Test-FullSha256Digest -Value $requiredIndexDigest) -or $requiredReference -ne "postgres@$requiredIndexDigest") {
        Add-IdentityBlocker 'POSTGRES_INDEX_DIGEST_INVALID'
    }
    if ($requiredIndexMediaType -ne 'application/vnd.oci.image.index.v1+json') {
        Add-IdentityBlocker 'POSTGRES_INDEX_MEDIA_TYPE_INVALID'
    }
    if (-not (Test-FullSha256Digest -Value $expectedManifestDigest) -or $expectedManifestMediaType -ne 'application/vnd.oci.image.manifest.v1+json') {
        Add-IdentityBlocker 'POSTGRES_PLATFORM_MANIFEST_MISMATCH'
    }
    if (-not (Test-FullSha256Digest -Value $expectedConfigDigest)) {
        Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
    }
    if ($targetOs -ne 'linux' -or $targetArchitecture -ne 'amd64' -or $targetVariant -ne '') {
        Add-IdentityBlocker 'POSTGRES_PLATFORM_NOT_FOUND'
    }

    $identityRoot = [IO.Path]::GetFullPath((Join-Path $EvidenceRoot ('.postgres-identity-' + [Guid]::NewGuid().ToString('N'))))
    $expectedIdentityPrefix = [IO.Path]::GetFullPath($EvidenceRoot) + [IO.Path]::DirectorySeparatorChar
    if (-not $identityRoot.StartsWith($expectedIdentityPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'postgres identity temporary path escaped the evidence root'
    }
    $archivePath = Join-Path $identityRoot 'image.tar'
    $extractRoot = Join-Path $identityRoot 'extract'

    try {
        [IO.Directory]::CreateDirectory($extractRoot) | Out-Null
        $rootInspectResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('image', 'inspect', '--format', '{{json .}}', $requiredReference)
        if ($rootInspectResult.exitCode -ne 0) {
            Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
        }
        else {
            $rootInspect = $rootInspectResult.text | ConvertFrom-Json
            $repoDigests = @($rootInspect.RepoDigests | ForEach-Object { [string]$_ })
            $localObservedDigest = [string]$rootInspect.Id
            if ($null -ne $rootInspect.Descriptor) {
                $localObservedMediaType = [string]$rootInspect.Descriptor.mediaType
                if (Test-ImageIdentityEquality -LeftKind 'OCI_INDEX_DIGEST' -LeftDigest ([string]$rootInspect.Descriptor.digest) -LeftMediaType $localObservedMediaType -RightKind 'OCI_INDEX_DIGEST' -RightDigest $requiredIndexDigest -RightMediaType $requiredIndexMediaType) {
                    $localObservedDigest = [string]$rootInspect.Descriptor.digest
                    $localObservedKind = 'OCI_INDEX_DIGEST'
                }
                else {
                    Add-IdentityBlocker 'POSTGRES_IDENTITY_DOMAIN_MISMATCH'
                }
            }
        }

        if (-not (@($repoDigests) -contains $requiredReference)) {
            Add-IdentityBlocker 'POSTGRES_REPO_DIGEST_MEMBERSHIP_MISSING'
        }

        $saveResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('image', 'save', '--output', $archivePath, $requiredReference)
        if ($saveResult.exitCode -ne 0 -or -not (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
            Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
        }
        else {
            $archiveListResult = Invoke-NativeCommand -Executable 'tar' -Arguments @('-tf', $archivePath)
            if ($archiveListResult.exitCode -ne 0) {
                Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
            }
            else {
                $archiveEntries = @($archiveListResult.text -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
                $indexBlobEntry = 'blobs/sha256/' + (Get-DigestHex -Value $requiredIndexDigest)
                if ($archiveEntries -contains 'index.json' -and $archiveEntries -contains $indexBlobEntry) {
                    $extractIndex = Invoke-NativeCommand -Executable 'tar' -Arguments @('-xf', $archivePath, '-C', $extractRoot, $indexBlobEntry)
                    $indexBlobPath = Join-Path $extractRoot ($indexBlobEntry -replace '/', [IO.Path]::DirectorySeparatorChar)
                    if ($extractIndex.exitCode -ne 0 -or -not (Test-Path -LiteralPath $indexBlobPath -PathType Leaf) -or (Get-Sha256 -Path $indexBlobPath) -ne (Get-DigestHex -Value $requiredIndexDigest)) {
                        Add-IdentityBlocker 'POSTGRES_INDEX_DIGEST_INVALID'
                    }
                    else {
                        $indexDocument = Read-JsonFile -Path $indexBlobPath
                        if ([string]$indexDocument.mediaType -ne $requiredIndexMediaType) {
                            Add-IdentityBlocker 'POSTGRES_INDEX_MEDIA_TYPE_INVALID'
                        }
                        $eligible = @($indexDocument.manifests | Where-Object {
                            [string]$_.platform.os -eq $targetOs -and
                            [string]$_.platform.architecture -eq $targetArchitecture -and
                            [string]$_.platform.variant -eq $targetVariant
                        })
                        if ($eligible.Count -eq 0) {
                            Add-IdentityBlocker 'POSTGRES_PLATFORM_NOT_FOUND'
                        }
                        elseif ($eligible.Count -ne 1) {
                            Add-IdentityBlocker 'POSTGRES_PLATFORM_AMBIGUOUS'
                        }
                        else {
                            $platformDescriptor = $eligible[0]
                            $resolvedManifestDigest = [string]$platformDescriptor.digest
                            $resolvedManifestMediaType = [string]$platformDescriptor.mediaType
                            if ($resolvedManifestDigest -ne $expectedManifestDigest -or $resolvedManifestMediaType -ne $expectedManifestMediaType) {
                                Add-IdentityBlocker 'POSTGRES_PLATFORM_MANIFEST_MISMATCH'
                            }
                            $manifestBlobEntry = 'blobs/sha256/' + (Get-DigestHex -Value $resolvedManifestDigest)
                            $extractManifest = Invoke-NativeCommand -Executable 'tar' -Arguments @('-xf', $archivePath, '-C', $extractRoot, $manifestBlobEntry)
                            $manifestBlobPath = Join-Path $extractRoot ($manifestBlobEntry -replace '/', [IO.Path]::DirectorySeparatorChar)
                            if ($extractManifest.exitCode -ne 0 -or -not (Test-Path -LiteralPath $manifestBlobPath -PathType Leaf) -or (Get-Sha256 -Path $manifestBlobPath) -ne (Get-DigestHex -Value $resolvedManifestDigest)) {
                                Add-IdentityBlocker 'POSTGRES_PLATFORM_MANIFEST_MISMATCH'
                            }
                            else {
                                $manifestDocument = Read-JsonFile -Path $manifestBlobPath
                                if ([string]$manifestDocument.mediaType -ne $expectedManifestMediaType -or [string]$manifestDocument.config.mediaType -ne 'application/vnd.oci.image.config.v1+json') {
                                    Add-IdentityBlocker 'POSTGRES_PLATFORM_MANIFEST_MISMATCH'
                                }
                                $resolvedConfigDigest = [string]$manifestDocument.config.digest
                                if ($resolvedConfigDigest -ne $expectedConfigDigest) {
                                    Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                }
                                $configBlobEntry = 'blobs/sha256/' + (Get-DigestHex -Value $resolvedConfigDigest)
                                $extractConfig = Invoke-NativeCommand -Executable 'tar' -Arguments @('-xf', $archivePath, '-C', $extractRoot, $configBlobEntry)
                                $configBlobPath = Join-Path $extractRoot ($configBlobEntry -replace '/', [IO.Path]::DirectorySeparatorChar)
                                if ($extractConfig.exitCode -ne 0 -or -not (Test-Path -LiteralPath $configBlobPath -PathType Leaf) -or (Get-Sha256 -Path $configBlobPath) -ne (Get-DigestHex -Value $resolvedConfigDigest)) {
                                    Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                }
                                else {
                                    $configDocument = Read-JsonFile -Path $configBlobPath
                                    if ([string]$configDocument.os -ne $targetOs -or [string]$configDocument.architecture -ne $targetArchitecture) {
                                        Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                    }
                                }
                                $resolutionSource = 'LOCAL_OCI_ARCHIVE'
                            }
                        }
                    }
                }
                elseif ($archiveEntries -contains 'manifest.json') {
                    $extractManifestIndex = Invoke-NativeCommand -Executable 'tar' -Arguments @('-xf', $archivePath, '-C', $extractRoot, 'manifest.json')
                    $manifestIndexPath = Join-Path $extractRoot 'manifest.json'
                    if ($extractManifestIndex.exitCode -ne 0 -or -not (Test-Path -LiteralPath $manifestIndexPath -PathType Leaf)) {
                        Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
                    }
                    else {
                        $dockerArchiveManifest = @(Read-JsonFile -Path $manifestIndexPath)
                        if ($dockerArchiveManifest.Count -ne 1) {
                            Add-IdentityBlocker 'POSTGRES_PLATFORM_AMBIGUOUS'
                        }
                        else {
                            $configEntry = [string]$dockerArchiveManifest[0].Config
                            if ($configEntry -notmatch '^(?:blobs/sha256/)?(?<digest>[a-f0-9]{64})(?:\.json)?$') {
                                Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                            }
                            else {
                                $resolvedConfigDigest = 'sha256:' + $Matches.digest
                                $extractConfig = Invoke-NativeCommand -Executable 'tar' -Arguments @('-xf', $archivePath, '-C', $extractRoot, $configEntry)
                                $configBlobPath = Join-Path $extractRoot ($configEntry -replace '/', [IO.Path]::DirectorySeparatorChar)
                                if ($extractConfig.exitCode -ne 0 -or -not (Test-Path -LiteralPath $configBlobPath -PathType Leaf) -or (Get-Sha256 -Path $configBlobPath) -ne $Matches.digest -or $resolvedConfigDigest -ne $expectedConfigDigest) {
                                    Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                }
                                else {
                                    $configDocument = Read-JsonFile -Path $configBlobPath
                                    if ([string]$configDocument.os -ne $targetOs -or [string]$configDocument.architecture -ne $targetArchitecture) {
                                        Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                    }
                                }
                                $remoteManifestResult = Invoke-NativeCommandWithTimeout -Executable 'docker' -Arguments @('manifest', 'inspect', '--verbose', $requiredReference) -WorkingDirectory $identityRoot -TimeoutSeconds 60
                                if ($remoteManifestResult.timedOut -or $remoteManifestResult.exitCode -ne 0 -or [string]::IsNullOrWhiteSpace($remoteManifestResult.text)) {
                                    Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
                                }
                                else {
                                    $remoteManifestRecords = @($remoteManifestResult.text | ConvertFrom-Json)
                                    $eligibleRemoteManifests = @($remoteManifestRecords | Where-Object {
                                        [string]$_.Descriptor.platform.os -eq $targetOs -and
                                        [string]$_.Descriptor.platform.architecture -eq $targetArchitecture -and
                                        [string]$_.Descriptor.platform.variant -eq $targetVariant
                                    })
                                    if ($eligibleRemoteManifests.Count -eq 0) {
                                        Add-IdentityBlocker 'POSTGRES_PLATFORM_NOT_FOUND'
                                    }
                                    elseif ($eligibleRemoteManifests.Count -ne 1) {
                                        Add-IdentityBlocker 'POSTGRES_PLATFORM_AMBIGUOUS'
                                    }
                                    else {
                                        $remoteManifest = $eligibleRemoteManifests[0]
                                        $remoteManifestDigest = [string]$remoteManifest.Descriptor.digest
                                        $remoteManifestMediaType = [string]$remoteManifest.Descriptor.mediaType
                                        $remoteConfigDigest = [string]$remoteManifest.OCIManifest.config.digest
                                        $remoteConfigMediaType = [string]$remoteManifest.OCIManifest.config.mediaType
                                        if ($remoteManifestDigest -ne $expectedManifestDigest -or
                                            $remoteManifestMediaType -ne $expectedManifestMediaType -or
                                            [string]$remoteManifest.OCIManifest.mediaType -ne $expectedManifestMediaType) {
                                            Add-IdentityBlocker 'POSTGRES_PLATFORM_MANIFEST_MISMATCH'
                                        }
                                        elseif ($remoteConfigDigest -ne $expectedConfigDigest -or
                                            $remoteConfigMediaType -ne 'application/vnd.oci.image.config.v1+json' -or
                                            $remoteConfigDigest -ne $resolvedConfigDigest) {
                                            Add-IdentityBlocker 'POSTGRES_CONFIG_DIGEST_MISMATCH'
                                        }
                                        else {
                                            $resolvedManifestDigest = $remoteManifestDigest
                                            $resolvedManifestMediaType = $remoteManifestMediaType
                                        }
                                    }
                                }
                                if ($localObservedDigest -eq $expectedConfigDigest) {
                                    $localObservedKind = 'CONFIG_DIGEST'
                                    $localObservedMediaType = 'application/vnd.oci.image.config.v1+json'
                                }
                                else {
                                    Add-IdentityBlocker 'POSTGRES_LOCAL_IDENTITY_UNKNOWN'
                                }
                                $resolutionSource = 'PINNED_REGISTRY_DESCRIPTOR_AND_LOCAL_DOCKER_ARCHIVE_CONFIG'
                            }
                        }
                    }
                }
                else {
                    Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
                }
            }
        }
    }
    catch {
        Add-IdentityBlocker 'POSTGRES_IDENTITY_RESOLUTION_FAILED'
    }
    finally {
        if (Test-Path -LiteralPath $identityRoot) {
            Remove-Item -LiteralPath $identityRoot -Recurse -Force
        }
    }

    if ($localObservedKind -eq 'UNKNOWN') {
        Add-IdentityBlocker 'POSTGRES_LOCAL_IDENTITY_UNKNOWN'
    }

    return [ordered]@{
        identityContractId = [string]$Admission.identityContractId
        identityContractVersion = [int]$Admission.identityContractVersion
        requiredImageReference = $requiredReference
        requiredIndexDigest = $requiredIndexDigest
        requiredIndexMediaType = $requiredIndexMediaType
        targetPlatform = [ordered]@{ os = $targetOs; architecture = $targetArchitecture; variant = $targetVariant }
        resolvedPlatformManifestDigest = $resolvedManifestDigest
        resolvedPlatformManifestMediaType = $resolvedManifestMediaType
        resolvedPlatformConfigDigest = $resolvedConfigDigest
        repoDigests = @($repoDigests)
        repoDigestMembership = $(if (@($repoDigests) -contains $requiredReference) { 'PASS' } else { 'FAIL' })
        localObservedIdentity = [ordered]@{ digest = $localObservedDigest; kind = $localObservedKind; mediaType = $localObservedMediaType }
        resolutionSource = $resolutionSource
        blockers = $blockers.ToArray()
        passed = $blockers.Count -eq 0
    }
}

function Invoke-Preflight {
    $started = Get-UtcTimestamp
    if (-not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'runId resolved outside the evidence base'
    }
    if (Test-Path -LiteralPath $EvidenceRoot) {
        throw "duplicate runId evidence root already exists: $RunId"
    }
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null

    $checks = New-Object Collections.Generic.List[object]
    $blockers = New-Object Collections.Generic.List[string]
    $commitSha = '0000000000000000000000000000000000000000'

    function Add-Check {
        param([string]$Id, [string]$Expected, [string]$Actual, [bool]$Passed)
        $checks.Add([ordered]@{ id = $Id; expected = $Expected; actual = $Actual; status = $(if ($Passed) { 'PASS' } else { 'BLOCKED' }) })
        if (-not $Passed) {
            $blockers.Add($Id)
        }
    }

    try {
        $runIdValid = $RunId -match '^[0-9]{8}T[0-9]{6}Z$'
        if ($runIdValid) {
            try {
                [DateTime]::ParseExact($RunId, 'yyyyMMddTHHmmssZ', [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::AssumeUniversal) | Out-Null
            }
            catch {
                $runIdValid = $false
            }
        }
        Add-Check 'run-id' 'UTC yyyyMMddTHHmmssZ' $RunId $runIdValid
        Add-Check 'seed' '7' ([string]$Seed) ($Seed -eq 7)
        Add-Check 'powershell-executable' 'resolved command name and path hash' ("$($ResolvedPowerShellIdentity.name) / $($ResolvedPowerShellIdentity.pathSha256)") ($ResolvedPowerShellIdentity.name -ne 'UNRESOLVED' -and $ResolvedPowerShellIdentity.pathSha256 -match '^[a-f0-9]{64}$')

        $branch = Get-GitValue -Arguments @('branch', '--show-current')
        $commitSha = Get-GitValue -Arguments @('rev-parse', 'HEAD')
        $candidateTree = Get-GitValue -Arguments @('rev-parse', 'HEAD^{tree}')
        $originSha = Get-GitValue -Arguments @('rev-parse', 'origin/dev')
        $advertisedSha = Get-AdvertisedDevSha
        $trackedChanges = @(Get-GitPathList -Arguments @('diff', '--name-only'))
        $untrackedChanges = @(Get-GitPathList -Arguments @('ls-files', '--others', '--exclude-standard'))
        $statusPaths = @(
            $trackedChanges
            $untrackedChanges
        ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
        $status = $statusPaths -join "`n"
        $stagedPaths = @(Get-GitPathList -Arguments @('diff', '--cached', '--name-only'))
        $staged = $stagedPaths -join "`n"
        Add-Check 'git-branch' 'dev' $branch ($branch -eq 'dev')
        Add-Check 'git-head' '40-character SHA-1' $commitSha ($commitSha -match '^[a-f0-9]{40}$')
        Add-Check 'git-tree' '40-character tree SHA-1' $candidateTree ($candidateTree -match '^[a-f0-9]{40}$')
        Add-Check 'git-origin-binding' $commitSha $originSha ($originSha -eq $commitSha)
        Add-Check 'git-advertised-binding' $commitSha $advertisedSha ($advertisedSha -eq $commitSha)
        $writeScopeEnabled = $ImplementationValidationEnabled -or $QualificationOnlyEnabled
        $writeScopeValid = $writeScopeEnabled -and @($statusPaths | Where-Object { -not (Test-ImplementationValidationPath -Path $_) }).Count -eq 0
        $worktreeValid = [string]::IsNullOrWhiteSpace($status) -or $writeScopeValid
        $stagedWriteScopeValid = $writeScopeEnabled -and @($stagedPaths | Where-Object { -not (Test-ImplementationValidationPath -Path $_) }).Count -eq 0
        $stagedValid = [string]::IsNullOrWhiteSpace($staged) -or $stagedWriteScopeValid
        $scopeLabel = $(if ($QualificationOnlyEnabled) { 'qualification write allowlist only' } else { 'implementation-validation write allowlist only' })
        Add-Check 'git-worktree' $(if ($writeScopeEnabled) { 'clean or harness write allowlist only' } else { 'clean' }) $(if ([string]::IsNullOrWhiteSpace($status)) { 'clean' } elseif ($writeScopeValid) { $scopeLabel } else { 'dirty outside allowed scope' }) $worktreeValid
        Add-Check 'git-staged' $(if ($writeScopeEnabled) { 'empty or harness write allowlist only' } else { 'empty' }) $(if ([string]::IsNullOrWhiteSpace($staged)) { 'empty' } elseif ($stagedWriteScopeValid) { $scopeLabel } else { 'staged outside allowed scope' }) $stagedValid

        $criteriaPath = Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json'
        $criteria = Read-JsonFile -Path $criteriaPath
        $admissionPath = Join-Path $ConfigRoot 'qdr7-capacity-environment-admission.json'
        $admission = Read-JsonFile -Path $admissionPath
        $sourcePath = Join-Path $ProjectRoot $criteria.sourceDocument
        $criteriaHash = Get-Sha256 -Path $sourcePath
        $scenarioSetHash = Get-Sha256 -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')
        $thresholdSetHash = Get-Sha256 -Path $criteriaPath
        $harnessHash = Get-HarnessHash
        Set-EvidenceBinding -CandidateSha $commitSha -CandidateTree $candidateTree -ProfileId ([string]$admission.profileId) -ProfileVersion ([string]$admission.profileVersion) -ScenarioSetHash $scenarioSetHash -ThresholdSetHash $thresholdSetHash -EnvironmentManifestHash ('0' * 64) -HarnessHash $harnessHash
        Add-Check 'criteria-version' $CriteriaVersion ([string]$criteria.criteriaVersion) ($criteria.criteriaVersion -eq $CriteriaVersion)
        Add-Check 'criteria-source-hash' ([string]$criteria.sourceDocumentSha256) $criteriaHash ($criteriaHash -eq $criteria.sourceDocumentSha256)
        foreach ($contract in @('qdr7-capacity-artifacts.schema.json', 'qdr7-capacity-artifact-registry.json', 'qdr7-capacity-scenario-registry.json', 'qdr7-capacity-threshold-consumers.json', 'qdr7-capacity-environment-admission.json', 'qdr7-capacity-exit-codes.json', 'qdr7-capacity-secret-patterns.json')) {
            $contractPath = Join-Path $ConfigRoot $contract
            $contractValid = $false
            try {
                Read-JsonFile -Path $contractPath | Out-Null
                $contractValid = $true
            }
            catch {
                $contractValid = $false
            }
            Add-Check "contract-$contract" 'parseable JSON' $(if ($contractValid) { 'parseable' } else { 'invalid' }) $contractValid
        }

        $javaResult = Invoke-NativeCommand -Executable 'java' -Arguments @('-version')
        $javaVersionText = $javaResult.text
        $javaMatch = [regex]::Match($javaVersionText, 'version "(?<version>\d+(?:\.\d+)*)')
        $javaVersion = $(if ($javaMatch.Success) { $javaMatch.Groups['version'].Value } else { 'unknown' })
        Add-Check 'java-version' '21.x' $javaVersion ($javaResult.exitCode -eq 0 -and $javaVersion -match '^21(?:\.|$)')
        $mavenResult = Invoke-NativeCommand -Executable 'mvn' -Arguments @('-version')
        $mavenVersionText = $mavenResult.text
        $mavenMatch = [regex]::Match($mavenVersionText, 'Apache Maven (?<version>\d+\.\d+\.\d+)')
        $mavenVersion = $(if ($mavenMatch.Success) { $mavenMatch.Groups['version'].Value } else { 'unknown' })
        Add-Check 'maven-version' '3.9.x' $mavenVersion ($mavenResult.exitCode -eq 0 -and $mavenVersion -match '^3\.9\.')
        Add-Check 'maven-opts' 'unset' $(if ([string]::IsNullOrEmpty($env:MAVEN_OPTS)) { 'unset' } else { 'set' }) ([string]::IsNullOrEmpty($env:MAVEN_OPTS))

        $os = Get-CimInstance Win32_OperatingSystem
        $computer = Get-CimInstance Win32_ComputerSystem
        $cpu = Get-CimInstance Win32_Processor
        $logicalCpu = [int](($cpu | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum)
        $availableMemoryBytes = [long]$os.FreePhysicalMemory * 1024L
        Add-Check 'os-family' 'Windows 11 x64' "$($os.Caption) / $env:PROCESSOR_ARCHITECTURE" (($os.Caption -match 'Windows 11') -and ($env:PROCESSOR_ARCHITECTURE -eq 'AMD64'))
        Add-Check 'logical-cpu' ">=$($admission.minimumLogicalCpu)" ([string]$logicalCpu) ($logicalCpu -ge [int]$admission.minimumLogicalCpu)
        Add-Check 'available-memory' ">=$($admission.minimumAvailableMemoryBytes) bytes" ([string]$availableMemoryBytes) ($availableMemoryBytes -ge [long]$admission.minimumAvailableMemoryBytes)

        $dockerVersionResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('version', '--format', '{{.Server.Version}}')
        $dockerVersion = $dockerVersionResult.text
        Add-Check 'docker-daemon' 'Docker Engine 29.x' $dockerVersion (($dockerVersionResult.exitCode -eq 0) -and ($dockerVersion -match '^29\.'))
        $dockerMemoryResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('info', '--format', '{{.MemTotal}}')
        $dockerMemoryText = $dockerMemoryResult.text
        $dockerMemory = 0L
        [long]::TryParse($dockerMemoryText, [ref]$dockerMemory) | Out-Null
        Add-Check 'docker-memory' ">=$($admission.minimumDockerMemoryBytes) bytes" ([string]$dockerMemory) ($dockerMemoryResult.exitCode -eq 0 -and $dockerMemory -ge [long]$admission.minimumDockerMemoryBytes)
        $postgresIdentity = Resolve-PostgresImageIdentityContract -Admission $admission
        $postgresImageReference = [string]$postgresIdentity.requiredImageReference
        $expectedImageId = [string]$postgresIdentity.resolvedPlatformConfigDigest
        $imageRepoDigests = @($postgresIdentity.repoDigests)
        $postgresImageDigestVerified = [string]$postgresIdentity.repoDigestMembership -eq 'PASS'
        $checks.Add([ordered]@{
            id = 'postgres-image-identity-v2'
            expected = 'explicit index -> platform manifest -> config + RepoDigest binding'
            actual = $(if ($postgresIdentity.passed) { [string]$postgresIdentity.resolutionSource } else { @($postgresIdentity.blockers) -join ',' })
            status = $(if ($postgresIdentity.passed) { 'PASS' } else { 'BLOCKED' })
        })
        foreach ($identityBlocker in @($postgresIdentity.blockers)) {
            if (-not $blockers.Contains([string]$identityBlocker)) {
                $blockers.Add([string]$identityBlocker)
            }
        }
        $dockerStorage = Invoke-NativeCommand -Executable 'docker' -Arguments @('info', '--format', '{{.Driver}}')

        $containerName = "dh-qdr7-capacity-$RunId"
        $volumeName = "dh-qdr7-capacity-$RunId"
        $containerInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('container', 'inspect', $containerName)
        Add-Check 'container-isolation' 'absent before run' $(if ($containerInspect.exitCode -eq 0) { 'exists' } else { 'absent' }) ($containerInspect.exitCode -ne 0)
        $volumeInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('volume', 'inspect', $volumeName)
        Add-Check 'volume-isolation' 'absent before run' $(if ($volumeInspect.exitCode -eq 0) { 'exists' } else { 'absent' }) ($volumeInspect.exitCode -ne 0)
        $port = Get-FreeLoopbackPort
        Add-Check 'loopback-port' 'available dynamic loopback port' ([string]$port) ($port -gt 0)

        $drive = New-Object IO.DriveInfo([IO.Path]::GetPathRoot($ProjectRoot))
        $driveLetter = $drive.Name.TrimEnd('\').TrimEnd(':')
        $volume = Get-Volume -DriveLetter $driveLetter
        $filesystemType = [string]$volume.FileSystemType
        $diskFreeBytes = [long]$drive.AvailableFreeSpace
        Add-Check 'disk-free' ">=$($admission.minimumDiskFreeBytes) bytes" ([string]$diskFreeBytes) ($diskFreeBytes -ge [long]$admission.minimumDiskFreeBytes)
        Add-Check 'filesystem-type' ($admission.requiredFilesystemTypes -join ',') $filesystemType ($filesystemType -in @($admission.requiredFilesystemTypes))

        $cpuSamples = New-Object Collections.Generic.List[double]
        for ($sample = 0; $sample -lt [int]$admission.backgroundObservationSeconds; $sample++) {
            $cpuLoad = [double](Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
            $cpuSamples.Add($cpuLoad)
            if ($sample -lt ([int]$admission.backgroundObservationSeconds - 1)) {
                Start-Sleep -Milliseconds ([int]$admission.backgroundSampleIntervalMilliseconds)
            }
        }
        $backgroundAverageCpu = [double]($cpuSamples | Measure-Object -Average).Average
        $backgroundPeakCpu = [double]($cpuSamples | Measure-Object -Maximum).Maximum
        Add-Check 'background-average-cpu' "<=$($admission.maximumBackgroundAverageCpuPercent)%" ([string]$backgroundAverageCpu) ($backgroundAverageCpu -le [double]$admission.maximumBackgroundAverageCpuPercent)
        Add-Check 'background-peak-cpu' "<=$($admission.maximumBackgroundPeakCpuPercent)%" ([string]$backgroundPeakCpu) ($backgroundPeakCpu -le [double]$admission.maximumBackgroundPeakCpuPercent)

        $timeService = Get-Service -Name W32Time -ErrorAction SilentlyContinue
        $timeStatus = Invoke-NativeCommand -Executable 'w32tm.exe' -Arguments @('/query', '/status', '/verbose')
        $offsetMatch = [regex]::Match($timeStatus.text, '(?:Phase Offset|相位偏移)\s*:\s*(?<seconds>[+-]?\d+(?:\.\d+)?)s', [Text.RegularExpressions.RegexOptions]::IgnoreCase)
        $clockOffsetMilliseconds = [double]::MaxValue
        if ($offsetMatch.Success) {
            $clockOffsetMilliseconds = [Math]::Abs([double]::Parse($offsetMatch.Groups['seconds'].Value, [Globalization.CultureInfo]::InvariantCulture) * 1000.0)
        }
        $clockSynchronized = $null -ne $timeService -and $timeService.Status -eq 'Running' -and $timeStatus.exitCode -eq 0 -and $offsetMatch.Success
        Add-Check 'clock-synchronized' 'W32Time running with measurable phase offset' $(if ($clockSynchronized) { "offset=$clockOffsetMilliseconds ms" } else { 'not synchronized or offset unavailable' }) $clockSynchronized
        Add-Check 'clock-offset' "<=$($admission.maximumClockOffsetMilliseconds) ms" ([string]$clockOffsetMilliseconds) ($clockOffsetMilliseconds -le [double]$admission.maximumClockOffsetMilliseconds)

        $credentialVariablesPresent = @($admission.credentialEnvironmentVariableNames | Where-Object { -not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable([string]$_)) } | ForEach-Object { [string]$_ })
        $proxyVariablesPresent = @($admission.proxyEnvironmentVariableNames | Where-Object { -not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable([string]$_)) } | ForEach-Object { [string]$_ })
        Add-Check 'credential-environment' 'no named credential variables present' ($credentialVariablesPresent -join ',') ($credentialVariablesPresent.Count -eq 0)
        Add-Check 'proxy-environment' 'no external proxy variables present' ($proxyVariablesPresent -join ',') ($proxyVariablesPresent.Count -eq 0)
        $networkIsolation = Get-NetworkIsolationSnapshot -AllowedPatterns @($admission.allowedActiveNetworkAdapterNamePatterns)
        Add-Check 'network-isolation' ([string]$admission.networkPolicyEvidenceType) $(if ($networkIsolation.networkIsolationVerified) { 'verified' } else { 'unverified' }) $networkIsolation.networkIsolationVerified

        $environmentManifest = New-Artifact -Scenario 'capacity-environment-manifest' -Status 'NOT_QUALIFIED' -CommitSha $commitSha -StartedAt $started -FinishedAt (Get-UtcTimestamp)
        $environmentManifest.originSha = $originSha
        $environmentManifest.advertisedSha = $advertisedSha
        $environmentManifest.worktreeClean = [string]::IsNullOrWhiteSpace($status)
        $environmentManifest.stagedEmpty = [string]::IsNullOrWhiteSpace($staged)
        $environmentManifest.untrackedTechnicalCount = @($untrackedChanges).Count
        $environmentManifest.javaMajor = $(if ($javaVersion -match '^(?<major>\d+)') { [int]$Matches.major } else { 0 })
        $environmentManifest.mavenVersion = $mavenVersion
        $environmentManifest.powerShellPathHash = $ResolvedPowerShellIdentity.pathSha256
        $environmentManifest.dockerDaemonAvailable = $dockerVersionResult.exitCode -eq 0
        $environmentManifest.dockerHealthy = $dockerVersionResult.exitCode -eq 0 -and $dockerMemoryResult.exitCode -eq 0
        $environmentManifest.dockerStorageDriver = $dockerStorage.text
        $environmentManifest.dockerMemoryBytes = $dockerMemory
        $environmentManifest.minimumDockerMemoryBytes = [long]$admission.minimumDockerMemoryBytes
        $environmentManifest.identityContractId = [string]$postgresIdentity.identityContractId
        $environmentManifest.identityContractVersion = [int]$postgresIdentity.identityContractVersion
        $environmentManifest.requiredImageReference = [string]$postgresIdentity.requiredImageReference
        $environmentManifest.requiredIndexDigest = [string]$postgresIdentity.requiredIndexDigest
        $environmentManifest.requiredIndexMediaType = [string]$postgresIdentity.requiredIndexMediaType
        $environmentManifest.targetPlatform = $postgresIdentity.targetPlatform
        $environmentManifest.resolvedPlatformManifestDigest = [string]$postgresIdentity.resolvedPlatformManifestDigest
        $environmentManifest.resolvedPlatformManifestMediaType = [string]$postgresIdentity.resolvedPlatformManifestMediaType
        $environmentManifest.resolvedPlatformConfigDigest = [string]$postgresIdentity.resolvedPlatformConfigDigest
        $environmentManifest.repoDigestMembership = [string]$postgresIdentity.repoDigestMembership
        $environmentManifest.localObservedIdentity = $postgresIdentity.localObservedIdentity
        $environmentManifest.executedObservedIdentity = [ordered]@{ digest = ''; kind = 'UNKNOWN'; mediaType = '' }
        $environmentManifest.executedPlatformManifestDigest = ''
        $environmentManifest.executedConfigDigest = ''
        $environmentManifest.immutableBindingResult = 'PENDING_EXECUTED_IDENTITY'
        $environmentManifest.identityBlockers = @($postgresIdentity.blockers)
        $environmentManifest.identityResolutionSource = [string]$postgresIdentity.resolutionSource
        $environmentManifest.postgresImageAvailable = [bool]$postgresIdentity.passed
        $environmentManifest.postgresImageId = [string]$postgresIdentity.localObservedIdentity.digest
        $environmentManifest.postgresImageIdentityDomain = [string]$postgresIdentity.localObservedIdentity.kind
        $environmentManifest.postgresExpectedCanonicalImageId = $expectedImageId
        $environmentManifest.postgresExpectedRepoDigests = @($imageRepoDigests)
        $environmentManifest.postgresImageReference = $postgresImageReference
        $environmentManifest.postgresImageDigestVerified = $postgresImageDigestVerified
        $environmentManifest.postgresExecutedImageReference = ''
        $environmentManifest.postgresExecutedImageId = ''
        $environmentManifest.legacyPostgresIdentityFields = [ordered]@{ status = 'DEPRECATED_NOT_USED_FOR_V2_DECISION'; decisionContract = 'POSTGRES_IMAGE_IDENTITY_CONTRACT_V2' }
        $environmentManifest.testcontainersViable = $false
        $environmentManifest.postgresMajor = 0
        $environmentManifest.logicalCpu = $logicalCpu
        $environmentManifest.minimumLogicalCpu = [int]$admission.minimumLogicalCpu
        $environmentManifest.availableMemoryBytes = $availableMemoryBytes
        $environmentManifest.minimumAvailableMemoryBytes = [long]$admission.minimumAvailableMemoryBytes
        $environmentManifest.diskFreeBytes = $diskFreeBytes
        $environmentManifest.filesystemWritable = $true
        $environmentManifest.filesystemType = $filesystemType
        $environmentManifest.clockSynchronized = $clockSynchronized
        $environmentManifest.clockOffsetMilliseconds = $clockOffsetMilliseconds
        $environmentManifest.loopbackPortAvailable = $port -gt 0
        $environmentManifest.networkPolicy = $(if ($networkIsolation.networkIsolationVerified) { [string]$admission.networkPolicy } else { 'UNVERIFIED' })
        $environmentManifest.networkPolicyEvidenceType = [string]$admission.networkPolicyEvidenceType
        $environmentManifest.networkIsolationVerified = [bool]$networkIsolation.networkIsolationVerified
        $environmentManifest.disallowedNetworkAdapterCount = [int]$networkIsolation.disallowedNetworkAdapterCount
        $environmentManifest.activeNetworkAdapterSetHash = [string]$networkIsolation.activeNetworkAdapterSetHash
        $environmentManifest.credentialVariablesPresent = $credentialVariablesPresent
        $environmentManifest.proxyVariablesPresent = $proxyVariablesPresent
        $environmentManifest.backgroundAverageCpuPercent = $backgroundAverageCpu
        $environmentManifest.backgroundPeakCpuPercent = $backgroundPeakCpu
        $environmentManifest.attemptContainerAbsent = $containerInspect.exitCode -ne 0
        $environmentManifest.attemptVolumeAbsent = $volumeInspect.exitCode -ne 0
        $environmentManifestHash = Get-EnvironmentManifestContentHash -Manifest $environmentManifest
        Set-EvidenceBinding -CandidateSha $commitSha -CandidateTree $candidateTree -ProfileId ([string]$admission.profileId) -ProfileVersion ([string]$admission.profileVersion) -ScenarioSetHash $scenarioSetHash -ThresholdSetHash $thresholdSetHash -EnvironmentManifestHash $environmentManifestHash -HarnessHash $harnessHash
        Add-BindingToObject -Target $environmentManifest
        Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-environment-manifest.json') -Value $environmentManifest
        Write-ExecutionManifest -CommitSha $commitSha -OriginSha $originSha -AdvertisedSha $advertisedSha -StartedAt $started -FinishedAt (Get-UtcTimestamp) -Status 'PASS'

        $executionBlockers = @($blockers)
        if ($ImplementationValidationEnabled) {
            $executionBlockers = @(
                $executionBlockers | Where-Object {
                    $_ -notin @('clock-synchronized', 'clock-offset', 'network-isolation')
                }
            )
        }
        if ($executionBlockers.Count -gt 0) {
            Write-BlockedPreflight -CommitSha $commitSha -StartedAt $started -Checks $checks.ToArray() -Blockers $executionBlockers
            exit 10
        }

        $finished = Get-UtcTimestamp
        $preflightStatus = $(if ($ImplementationValidationEnabled) { 'NOT_FORMAL' } else { 'PASS' })
        $environment = New-Artifact -Scenario 'environment' -Status $preflightStatus -CommitSha $commitSha -StartedAt $started -FinishedAt $finished
        $environment.os = [ordered]@{ caption = $os.Caption; version = $os.Version; architecture = $env:PROCESSOR_ARCHITECTURE }
        $environment.logicalCpu = $logicalCpu
        $environment.availableMemoryBytes = $availableMemoryBytes
        $environment.totalPhysicalMemoryBytes = [long]$computer.TotalPhysicalMemory
        $environment.javaVersion = $javaVersion
        $environment.mavenVersion = $mavenVersion
        $environment.dockerVersion = $dockerVersion
        $environment.dockerMemoryBytes = $dockerMemory
        $environment.postgresImage = $postgresImageReference
        $environment.testcontainersVersion = '1.20.4'
        $environment.criteriaSourceSha256 = $criteriaHash
        $environment.powerShellExecutable = $ResolvedPowerShellIdentity
        Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment

        $preflight = New-Artifact -Scenario 'environment-preflight' -Status $preflightStatus -CommitSha $commitSha -StartedAt $started -FinishedAt $finished
        $preflight.checks = $checks.ToArray()
        $preflight.blockerCode = $(if ($ImplementationValidationEnabled -and $blockers.Count -gt 0) { 'IMPLEMENTATION_VALIDATION_FORMAL_ENVIRONMENT_NOT_QUALIFIED' } else { $null })
        $preflight.blockers = $(if ($ImplementationValidationEnabled) { $blockers.ToArray() } else { @() })
        Write-JsonFile -Path (Join-Path $EvidenceRoot 'preflight.json') -Value $preflight

        $mavenPid = Get-ProcessParentId -ProcessId $PID
        $registry = New-Artifact -Scenario 'resource-registry' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $finished
        $registry.containerName = $containerName
        $registry.volumeName = $volumeName
        $registry.databaseName = "qdr7_capacity_$($RunId.ToLowerInvariant())"
        $registry.tenantHash = Get-Sha256Text -Value "qdr7-capacity-$RunId"
        $registry.environmentHash = Get-Sha256Text -Value "QDR7_CAPACITY_$RunId"
        $registry.loopbackPort = $port
        $registry.mavenPid = $mavenPid
        $registry.samplerPid = $null
        $registry.samplerStartedAtUtc = $null
        $registry.containerOwnership = 'JUNIT_TESTCONTAINERS'
        $registry.implementationValidation = $ImplementationValidationEnabled
        $registry.qualificationOnly = $QualificationOnlyEnabled
        $registry.postgresImageId = $expectedImageId
        $registry.postgresImageReference = $postgresImageReference
        $registry.identityContractId = [string]$postgresIdentity.identityContractId
        $registry.identityContractVersion = [int]$postgresIdentity.identityContractVersion
        $registry.expectedPlatformManifestDigest = [string]$postgresIdentity.resolvedPlatformManifestDigest
        $registry.expectedPlatformConfigDigest = [string]$postgresIdentity.resolvedPlatformConfigDigest
        $registry.localObservedIdentity = $postgresIdentity.localObservedIdentity
        $registry.identityResolutionSource = [string]$postgresIdentity.resolutionSource
        $registry.teardown = [ordered]@{ sampler = 'PENDING'; container = 'PENDING'; volume = 'PENDING'; residual = 'PENDING' }
        Write-JsonFile -Path $RegistryPath -Value $registry

        $commands = @(
            '1 | ' + $started + ' | . | mvn -ntp -Pqdr7-capacity-acceptance -Dqdr7.runId=<UTC_RUN_ID> -Dqdr7.seed=7' + $(if ($QualificationOnlyEnabled) { ' -Dqdr7.qualificationOnly=true' } else { '' }) + ' verify | STARTED',
            '2 | ' + $finished + ' | dh-app | pre-integration-test PowerShell preflight | PASS'
        )
        Write-Utf8File -Path (Join-Path $EvidenceRoot 'commands.txt') -Content (($commands -join "`n") + "`n")

        $samplerScript = Join-Path $ProjectRoot 'scripts\qdr7-capacity\Watch-Qdr7CapacityResources.ps1'
        $samplerExecutable = (Get-Process -Id $PID).Path
        $samplerArguments = @('-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $samplerScript, '-RunId', $RunId, '-CommitSha', $commitSha, '-EvidenceRoot', $EvidenceRoot, '-ContainerName', $containerName, '-MavenPid', [string]$mavenPid, '-StopMarker', $StopMarker)
        $sampler = Start-Process -FilePath $samplerExecutable -ArgumentList $samplerArguments -PassThru -WindowStyle Hidden
        $registry.samplerPid = $sampler.Id
        $registry.samplerStartedAtUtc = Get-UtcTimestamp
        Write-JsonFile -Path $RegistryPath -Value $registry
        exit 0
    }
    catch {
        if (-not ($blockers -contains 'unexpected-preflight-failure')) {
            $blockers.Add('unexpected-preflight-failure')
        }
        $checks.Add([ordered]@{
            id = 'unexpected-preflight-failure'
            expected = 'no unexpected exception'
            actual = "$($_.Exception.GetType().Name): $($_.Exception.Message)"
            status = 'BLOCKED'
        })
        Write-BlockedPreflight -CommitSha $commitSha -StartedAt $started -Checks $checks.ToArray() -Blockers $blockers.ToArray()
        exit 10
    }
}

function Get-Sha256Text {
    param([string]$Value)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return (($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($Value)) | ForEach-Object { $_.ToString('x2') }) -join '')
    }
    finally {
        $sha.Dispose()
    }
}

function Resolve-Qdr7PowerShellExecutable {
    param([string]$RequestedExecutable)

    $candidates = New-Object Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace($RequestedExecutable) -and $RequestedExecutable -ne 'AUTO') {
        $candidates.Add($RequestedExecutable)
    }
    else {
        $candidates.Add('pwsh.exe')
        $candidates.Add('pwsh')
        $candidates.Add('powershell.exe')
    }

    foreach ($candidate in $candidates) {
        $command = Get-Command -Name $candidate -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -ne $command -and -not [string]::IsNullOrWhiteSpace($command.Path) -and (Test-Path -LiteralPath $command.Path -PathType Leaf)) {
            return [ordered]@{
                name = [IO.Path]::GetFileName($command.Path)
                path = $command.Path
                pathSha256 = Get-Sha256Text -Value ([IO.Path]::GetFullPath($command.Path).ToLowerInvariant())
            }
        }
    }
    return $null
}

function Enter-Qdr7ResolvedPowerShell {
    $resolved = Resolve-Qdr7PowerShellExecutable -RequestedExecutable $PowerShellExecutable
    if ($null -eq $resolved) {
        if ($RunId -match '^[0-9]{8}T[0-9]{6}Z$' -and $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase) -and -not (Test-Path -LiteralPath $EvidenceRoot)) {
            $commitSha = '0000000000000000000000000000000000000000'
            try {
                $commitSha = Get-GitValue -Arguments @('rev-parse', 'HEAD')
            }
            catch {
                # Keep the zero SHA so the blocked resolver artifact remains path-safe.
            }
            $started = Get-UtcTimestamp
            $checks = @([ordered]@{
                id = 'powershell-executable'
                expected = 'explicit executable or AUTO resolver candidate'
                actual = 'UNRESOLVED'
                status = 'BLOCKED'
            })
            Write-BlockedPreflight -CommitSha $commitSha -StartedAt $started -Checks $checks -Blockers @('powershell-executable')
        }
        exit 10
    }

    $script:ResolvedPowerShellIdentity = [ordered]@{ name = $resolved.name; pathSha256 = $resolved.pathSha256 }
    $currentExecutable = (Get-Process -Id $PID).Path
    $sameExecutable = -not [string]::IsNullOrWhiteSpace($currentExecutable) -and ([IO.Path]::GetFullPath($currentExecutable) -eq [IO.Path]::GetFullPath($resolved.path))
    if (-not $PowerShellResolved -and -not $sameExecutable) {
        $forwardArguments = @(
            '-NoProfile',
            '-NonInteractive',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            $PSCommandPath,
            '-Phase',
            $Phase,
            '-RunId',
            $RunId,
            '-Seed',
            [string]$Seed,
            '-ProjectRoot',
            $ProjectRoot,
            '-PowerShellExecutable',
            $resolved.name,
            '-ImplementationValidation',
            $ImplementationValidation,
            '-QualificationOnly',
            $QualificationOnly,
            '-PowerShellResolved'
        )
        $resolvedPath = [string]$resolved.path
        & $resolvedPath @forwardArguments
        exit $LASTEXITCODE
    }
}

function Test-CommonJsonArtifact {
    param([string]$Path, [string]$CommitSha)
    try {
        $artifact = Read-JsonFile -Path $Path
        foreach ($field in @('schemaVersion', 'runId', 'commitSha', 'scenario', 'status', 'startedAtUtc', 'finishedAtUtc', 'durationMs', 'seed', 'unitSystem', 'missingValues', 'criteriaVersion', 'attemptId', 'candidateSha', 'candidateTree', 'profileId', 'profileVersion', 'scenarioSetHash', 'thresholdSetHash', 'environmentManifestHash', 'harnessVersion', 'harnessHash', 'generatedAt')) {
            if ($null -eq $artifact.PSObject.Properties[$field]) {
                return $false
            }
        }
        return ($artifact.schemaVersion -eq $SchemaVersion -and $artifact.runId -eq $RunId -and $artifact.commitSha -eq $CommitSha -and $artifact.criteriaVersion -eq $CriteriaVersion -and $artifact.seed -eq 7 -and @('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN', 'NOT_FORMAL', 'QUALIFIED', 'NOT_QUALIFIED') -contains $artifact.status)
    }
    catch {
        return $false
    }
}

function Stop-RegisteredResources {
    param([object]$Registry, [string]$CommitSha)
    # ConvertFrom-Json may return DateTime; persist registry timestamps in frozen .fffZ form.
    $Registry.startedAtUtc = ConvertTo-UtcTimestamp -Value $Registry.startedAtUtc
    if ($null -ne $Registry.PSObject.Properties['samplerStartedAtUtc'] -and $null -ne $Registry.samplerStartedAtUtc) {
        $Registry.samplerStartedAtUtc = ConvertTo-UtcTimestamp -Value $Registry.samplerStartedAtUtc
    }
    $findings = New-Object Collections.Generic.List[string]
    Write-Utf8File -Path $StopMarker -Content "stop`n"
    if ($null -ne $Registry.samplerPid) {
        $samplerProcess = Get-Process -Id ([int]$Registry.samplerPid) -ErrorAction SilentlyContinue
        if ($null -ne $samplerProcess) {
            try {
                Wait-Process -Id $samplerProcess.Id -Timeout 10 -ErrorAction Stop
            }
            catch {
                Stop-Process -Id $samplerProcess.Id -Force -ErrorAction SilentlyContinue
            }
        }
    }
    $Registry.teardown.sampler = 'STOPPED'

    if ($null -ne $Registry.PSObject.Properties['contractOnly'] -and $Registry.contractOnly) {
        $Registry.teardown.container = 'REMOVED_OR_ABSENT'
        $Registry.teardown.volume = 'REMOVED_OR_ABSENT'
        $Registry.teardown.residual = 'NONE'
        $Registry.finishedAtUtc = Get-UtcTimestamp
        Write-JsonFile -Path $RegistryPath -Value $Registry
        return @()
    }

    $expected = "dh-qdr7-capacity-$RunId"
    if ($Registry.containerName -ne $expected -or $Registry.volumeName -ne $expected) {
        $findings.Add('RESOURCE_REGISTRY_NAME_MISMATCH')
        return $findings.ToArray()
    }
    $containerInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('container', 'inspect', [string]$Registry.containerName)
    if ($containerInspect.exitCode -eq 0) {
        $containerRemove = Invoke-NativeCommand -Executable 'docker' -Arguments @('container', 'rm', '--force', [string]$Registry.containerName)
        if ($containerRemove.exitCode -ne 0) {
            $findings.Add('REGISTERED_CONTAINER_TEARDOWN_FAILED')
        }
    }
    $Registry.teardown.container = $(if ($findings -contains 'REGISTERED_CONTAINER_TEARDOWN_FAILED') { 'FAILED' } else { 'REMOVED_OR_ABSENT' })
    $volumeInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('volume', 'inspect', [string]$Registry.volumeName)
    if ($volumeInspect.exitCode -eq 0) {
        $volumeRemove = Invoke-NativeCommand -Executable 'docker' -Arguments @('volume', 'rm', [string]$Registry.volumeName)
        if ($volumeRemove.exitCode -ne 0) {
            $findings.Add('REGISTERED_VOLUME_TEARDOWN_FAILED')
        }
    }
    $Registry.teardown.volume = $(if ($findings -contains 'REGISTERED_VOLUME_TEARDOWN_FAILED') { 'FAILED' } else { 'REMOVED_OR_ABSENT' })
    $containerResidualInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('container', 'inspect', [string]$Registry.containerName)
    $containerResidual = $containerResidualInspect.exitCode -eq 0
    $volumeResidualInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('volume', 'inspect', [string]$Registry.volumeName)
    $volumeResidual = $volumeResidualInspect.exitCode -eq 0
    $Registry.teardown.residual = $(if ($containerResidual -or $volumeResidual) { 'FAILED' } else { 'NONE' })
    if ($containerResidual -or $volumeResidual) {
        $findings.Add('RUN_ID_RESOURCE_RESIDUAL')
    }
    $Registry.finishedAtUtc = Get-UtcTimestamp
    Write-JsonFile -Path $RegistryPath -Value $Registry
    return $findings.ToArray()
}

function Invoke-SecretScan {
    param([string]$CommitSha)
    $started = Get-UtcTimestamp
    $patternsConfig = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-secret-patterns.json')
    $findings = New-Object Collections.Generic.List[object]
    $files = @(Get-ChildItem -LiteralPath $EvidenceRoot -File | Where-Object { $_.Name -notin @('secret-scan.json', 'sha256-manifest.txt') -and $_.Extension -in @('.json', '.csv', '.txt', '.log') })
    foreach ($file in $files) {
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath $file.FullName) {
            $lineNumber++
            foreach ($pattern in $patternsConfig.patterns) {
                if ($line -match $pattern.regex) {
                    $findings.Add([ordered]@{ patternId = $pattern.id; path = $file.Name; line = $lineNumber })
                }
            }
        }
    }
    $finished = Get-UtcTimestamp
    $artifact = New-Artifact -Scenario 'secret-scan' -Status $(if ($findings.Count -eq 0) { 'PASS' } else { 'FAIL' }) -CommitSha $CommitSha -StartedAt $started -FinishedAt $finished
    $artifact.patternsVersion = $patternsConfig.patternsVersion
    $artifact.scannedFiles = $files.Count
    $artifact.findingCount = $findings.Count
    $artifact.findings = $findings.ToArray()
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'secret-scan.json') -Value $artifact
    return $findings.Count
}

function Write-Manifest {
    $manifestPath = Join-Path $EvidenceRoot 'sha256-manifest.txt'
    $lines = New-Object Collections.Generic.List[string]
    $files = @(Get-ChildItem -LiteralPath $EvidenceRoot -File | Where-Object { $_.FullName -ne $manifestPath })
    $names = New-Object Collections.Generic.List[string]
    foreach ($file in $files) { $names.Add($file.Name) }
    $names.Sort([StringComparer]::Ordinal)
    foreach ($name in $names) {
        $file = Get-Item -LiteralPath (Join-Path $EvidenceRoot $name)
        $lines.Add((Get-Sha256 -Path $file.FullName) + '  ' + $file.Name)
    }
    Write-Utf8File -Path $manifestPath -Content (($lines -join "`n") + "`n")
}

function Get-ManifestMismatchCount {
    $manifestPath = Join-Path $EvidenceRoot 'sha256-manifest.txt'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        return 1
    }
    $mismatches = 0
    $manifestNames = New-Object Collections.Generic.List[string]
    $uniqueNames = New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    foreach ($line in Get-Content -LiteralPath $manifestPath) {
        $parts = $line -split '  ', 2
        if ($parts.Count -ne 2 -or $parts[0] -notmatch '^[a-f0-9]{64}$') {
            $mismatches++
            continue
        }
        $name = [string]$parts[1]
        if ([string]::IsNullOrWhiteSpace($name) -or [IO.Path]::IsPathRooted($name) -or [IO.Path]::GetFileName($name) -ne $name -or $name -eq 'sha256-manifest.txt') {
            $mismatches++
            continue
        }
        $manifestNames.Add($name)
        if (-not $uniqueNames.Add($name)) {
            $mismatches++
        }
        $target = [IO.Path]::GetFullPath((Join-Path $EvidenceRoot $parts[1]))
        if (-not (Test-Path -LiteralPath $target -PathType Leaf) -or (Get-Sha256 -Path $target) -ne $parts[0]) {
            $mismatches++
        }
    }
    $sortedManifestNames = New-Object Collections.Generic.List[string]
    foreach ($name in $manifestNames) { $sortedManifestNames.Add($name) }
    $sortedManifestNames.Sort([StringComparer]::Ordinal)
    for ($index = 0; $index -lt $manifestNames.Count; $index++) {
        if ($manifestNames[$index] -cne $sortedManifestNames[$index]) {
            $mismatches++
            break
        }
    }
    $currentNames = New-Object Collections.Generic.List[string]
    foreach ($file in @(Get-ChildItem -LiteralPath $EvidenceRoot -File | Where-Object { $_.FullName -ne $manifestPath })) {
        $currentNames.Add($file.Name)
    }
    if (-not $uniqueNames.SetEquals($currentNames)) {
        $mismatches++
    }
    return $mismatches
}

function Invoke-ManifestContractTest {
    if ($RunId -notmatch '^[0-9]{8}T[0-9]{6}Z$' -or $Seed -ne 7 -or -not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        exit 10
    }
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
    foreach ($name in @('a.txt', 'b.txt', 'c.txt', 'sha256-manifest.txt')) {
        [IO.File]::Delete((Join-Path $EvidenceRoot $name))
    }
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'a.txt') -Content "a`n"
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'b.txt') -Content "b`n"
    Write-Manifest
    if ((Get-ManifestMismatchCount) -ne 0) { exit 1 }

    $manifestPath = Join-Path $EvidenceRoot 'sha256-manifest.txt'
    $original = @(Get-Content -LiteralPath $manifestPath)
    Write-Utf8File -Path $manifestPath -Content ($original[0] + "`n")
    if ((Get-ManifestMismatchCount) -eq 0) { exit 2 }

    Write-Utf8File -Path $manifestPath -Content (($original[0], $original[0], $original[1] -join "`n") + "`n")
    if ((Get-ManifestMismatchCount) -eq 0) { exit 3 }

    Write-Manifest
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'c.txt') -Content "c`n"
    if ((Get-ManifestMismatchCount) -eq 0) { exit 4 }

    Write-Output 'QDR7_CAPACITY_MANIFEST_CONTRACT=PASS'
    exit 0
}

function Get-EnvironmentManifestContentHash {
    param([object]$Manifest)
    $hashSource = $Manifest | ConvertTo-Json -Depth 30 -Compress | ConvertFrom-Json
    $hashSource.PSObject.Properties.Remove('environmentManifestHash')
    return Get-Sha256Text -Value ($hashSource | ConvertTo-Json -Depth 30 -Compress)
}

function Get-NetworkIsolationSnapshot {
    param([string[]]$AllowedPatterns)
    $activeAdapters = @()
    $disallowed = @()
    $inspectionAvailable = $false
    try {
        if ($null -eq (Get-Command -Name 'Get-NetAdapter' -ErrorAction SilentlyContinue)) {
            throw 'Get-NetAdapter unavailable'
        }
        $activeAdapters = @(Get-NetAdapter -IncludeHidden -ErrorAction Stop | Where-Object { $_.Status -eq 'Up' })
        foreach ($adapter in $activeAdapters) {
            $allowed = $false
            foreach ($pattern in $AllowedPatterns) {
                if ([string]$adapter.Name -match $pattern -or [string]$adapter.InterfaceDescription -match $pattern) {
                    $allowed = $true
                    break
                }
            }
            if (-not $allowed) {
                $disallowed += $adapter
            }
        }
        $inspectionAvailable = $true
    }
    catch {
        $activeAdapters = @()
        $disallowed = @()
    }
    $adapterFacts = @($activeAdapters | ForEach-Object { "$($_.ifIndex)|$($_.Name)|$($_.InterfaceDescription)" } | Sort-Object)
    return [ordered]@{
        networkIsolationVerified = $inspectionAvailable -and $disallowed.Count -eq 0
        disallowedNetworkAdapterCount = $(if ($inspectionAvailable) { $disallowed.Count } else { -1 })
        activeNetworkAdapterSetHash = Get-Sha256Text -Value ($adapterFacts | ConvertTo-Json -Compress)
    }
}

function Write-ArtifactInventory {
    param([string]$CommitSha, [string]$StartedAt)
    $excluded = @(
        'artifact-inventory.json',
        'capacity-final-verdict.json',
        'capacity-acceptance-summary.json',
        'harness-exit-code.txt',
        'sha256-manifest.txt'
    )
    $rows = New-Object Collections.Generic.List[object]
    foreach ($file in @(Get-ChildItem -LiteralPath $EvidenceRoot -File | Where-Object { $_.Name -notin $excluded } | Sort-Object -Property Name)) {
        $rows.Add([ordered]@{
            path = $file.Name
            sha256 = Get-Sha256 -Path $file.FullName
            bytes = [long]$file.Length
        })
    }
    $artifact = New-Artifact -Scenario 'artifact-inventory' -Status 'PASS' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt (Get-UtcTimestamp)
    $artifact.artifacts = $rows.ToArray()
    $artifact.artifactCount = $rows.Count
    $artifact.excludedDerivedArtifacts = $excluded
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'artifact-inventory.json') -Value $artifact
}

function Write-CapacityFinalVerdict {
    param(
        [string]$CommitSha,
        [string]$StartedAt,
        [string]$Verdict,
        [string]$ReasonCode,
        [string[]]$IntegrityFindings
    )
    $status = $(if ($Verdict -eq 'PASS_WITHIN_FROZEN_PROFILE') { 'PASS' } elseif ($Verdict -eq 'INVALID') { 'INVALID' } elseif ($Verdict -eq 'FAIL') { 'FAIL' } else { 'BLOCKED' })
    $artifact = New-Artifact -Scenario 'capacity-final-verdict' -Status $status -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt (Get-UtcTimestamp)
    $artifact.verdict = $Verdict
    $artifact.reasonCode = $ReasonCode
    $artifact.integrityFindings = $IntegrityFindings
    $artifact.formalCapacityExecuted = ($Verdict -eq 'PASS_WITHIN_FROZEN_PROFILE' -and (Get-ExecutionMode) -eq 'FORMAL')
    $artifact.expectedMandatoryScenarios = 15
    $artifact.expectedThresholdLeaves = 41
    $artifact.expectedThresholdComparisons = 99
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-final-verdict.json') -Value $artifact
}

function Get-ThresholdValueByPath {
    param([object]$ThresholdProfile, [string]$ThresholdPath)
    $current = $ThresholdProfile
    foreach ($segment in $ThresholdPath.Split('.')) {
        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            throw "threshold path not found: $ThresholdPath"
        }
        $current = $property.Value
    }
    return [decimal]$current
}

function Get-ThresholdSemanticExpectation {
    param([object]$Consumer, [object]$ThresholdProfile)
    $path = [string]$Consumer.thresholdPath
    $operator = '<='
    $unit = 'milliseconds'
    $sourceArtifact = ''
    $artifactField = ''
    switch ([string]$Consumer.scenario) {
        'rate-matrix' {
            $operator = $(if ($path.EndsWith('.throughputMin')) { '>=' } else { '<=' })
            $unit = $(if ($path.EndsWith('.throughputMin')) { 'operations/second' } else { 'milliseconds' })
            $sourceArtifact = 'rate-summary.json'
            $field = $(
                if ($path.EndsWith('.throughputMin')) { 'throughput' }
                elseif ($path.EndsWith('.p50MaxMs')) { 'p50Ms' }
                elseif ($path.EndsWith('.p95MaxMs')) { 'p95Ms' }
                elseif ($path.EndsWith('.p99MaxMs')) { 'p99Ms' }
                elseif ($path.EndsWith('.latencyMaxMs')) { 'maxMs' }
                else { throw "unsupported rate threshold: $path" }
            )
            $artifactField = "rate-summary.json#rounds[concurrency,round].$field"
        }
        'tenant-scoped-cleanup' {
            $sourceArtifact = 'cleanup-summary.json'
            $artifactField = 'cleanup-summary.json#scales[scale].durationMs'
        }
        'postgres-same-pool-recovery' {
            $sourceArtifact = 'recovery-timeline.csv'
            $field = $(
                if ($path.EndsWith('.database')) { 'databaseReadyMs' }
                elseif ($path.EndsWith('.hikari')) { 'hikariReadyMs' }
                elseif ($path.EndsWith('.request')) { 'requestReadyMs' }
                elseif ($path.EndsWith('.samplingGap')) { 'samplingGapMs' }
                else { throw "unsupported recovery threshold: $path" }
            )
            $artifactField = "recovery-timeline.csv#rows[round].$field"
        }
        'postgres-hikari-contention' {
            $unit = $(if ($path.EndsWith('.acquireMs')) { 'milliseconds' } else { 'count' })
            $sourceArtifact = 'postgres-hikari-series.csv'
            $field = $(
                if ($path.EndsWith('.hikariPending')) { 'hikariPending' }
                elseif ($path.EndsWith('.acquireMs')) { 'acquireMs' }
                elseif ($path.EndsWith('.postgresWaiting')) { 'postgresWaiting' }
                elseif ($path.EndsWith('.postgresLockWaiting')) { 'postgresLockWaiting' }
                else { throw "unsupported contention threshold: $path" }
            )
            $artifactField = "postgres-hikari-series.csv#max($field)"
        }
        'full-regression' {
            $operator = $(if ($path.EndsWith('.minimumFreeMemoryBytes')) { '>=' } else { '<=' })
            $unit = $(if ($path.EndsWith('.durationMs')) { 'milliseconds' } else { 'bytes' })
            $sourceArtifact = 'resource-summary.json'
            $field = @{
                'numericThresholds.regressionMax.durationMs' = 'durationMs'
                'numericThresholds.regressionMax.mavenWorkingSetBytes' = 'mavenPeakWorkingSetBytes'
                'numericThresholds.regressionMax.surefireWorkingSetBytes' = 'surefirePeakAggregateWorkingSetBytes'
                'numericThresholds.regressionMax.dockerMemoryBytes' = 'dockerPeakMemoryBytes'
                'numericThresholds.regressionMax.minimumFreeMemoryBytes' = 'minimumHostAvailableBytes'
            }[$path]
            if ([string]::IsNullOrWhiteSpace([string]$field)) { throw "unsupported regression threshold: $path" }
            $artifactField = "resource-summary.json#$field"
        }
        default { throw "threshold consumer scenario unsupported: $($Consumer.scenario)" }
    }
    return [ordered]@{
        operator = $operator
        threshold = Get-ThresholdValueByPath -ThresholdProfile $ThresholdProfile -ThresholdPath $path
        unit = $unit
        sourceArtifact = $sourceArtifact
        artifactField = $artifactField
    }
}

function Test-ThresholdSemanticRow {
    param([object]$Row, [object]$Expectation, [string]$ThresholdPath, [decimal]$SourceObserved)
    $findings = New-Object Collections.Generic.List[string]
    if ([string]$Row.operator -ne [string]$Expectation.operator) {
        $findings.Add("THRESHOLD_OPERATOR_MISMATCH:$ThresholdPath")
    }
    $thresholdValid = $true
    try {
        $actualThreshold = [decimal]$Row.threshold
        if ($actualThreshold -ne [decimal]$Expectation.threshold) {
            $findings.Add("THRESHOLD_VALUE_MISMATCH:$ThresholdPath")
        }
    }
    catch {
        $thresholdValid = $false
        $findings.Add("THRESHOLD_VALUE_MISMATCH:$ThresholdPath")
    }
    if ([string]$Row.sourceArtifact -ne [string]$Expectation.sourceArtifact) {
        $findings.Add("THRESHOLD_SOURCE_ARTIFACT_MISMATCH:$ThresholdPath")
    }
    if ([string]$Row.unit -ne [string]$Expectation.unit) {
        $findings.Add("THRESHOLD_UNIT_MISMATCH:$ThresholdPath")
    }
    $observedValid = $true
    try {
        $observedDouble = [double]$Row.observed
        $observedValid = -not [double]::IsNaN($observedDouble) -and -not [double]::IsInfinity($observedDouble)
        $observed = [decimal]$observedDouble
    }
    catch {
        $observedValid = $false
    }
    if (-not $observedValid) {
        $findings.Add("THRESHOLD_OBSERVED_INVALID:$ThresholdPath")
    }
    elseif ($observed -ne $SourceObserved) {
        $findings.Add("THRESHOLD_SOURCE_OBSERVED_MISMATCH:$ThresholdPath")
    }
    if ($observedValid -and $thresholdValid -and $findings.Count -eq 0) {
        $comparisonPassed = $(if ($Expectation.operator -eq '>=') { $observed -ge [decimal]$Expectation.threshold } else { $observed -le [decimal]$Expectation.threshold })
        $expectedStatus = $(if ($comparisonPassed) { 'PASS' } else { 'FAIL' })
        if ([string]$Row.status -ne $expectedStatus) {
            $findings.Add("THRESHOLD_STATUS_SEMANTICS_MISMATCH:$ThresholdPath")
        }
    }
    return $findings.ToArray()
}

function Get-StrictJsonDecimal {
    param([object]$Value)
    if ($null -eq $Value -or $Value -is [bool] -or $Value -is [string]) {
        throw 'JSON source metric is not numeric'
    }
    $asDouble = [double]$Value
    if ([double]::IsNaN($asDouble) -or [double]::IsInfinity($asDouble)) {
        throw 'JSON source metric is not finite'
    }
    return [decimal]$Value
}

function Get-ThresholdObservedFromSource {
    param([object]$Row, [object]$Consumer, [object]$Expectation)
    $path = Join-Path $EvidenceRoot ([string]$Expectation.sourceArtifact)
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "threshold source artifact missing: $($Expectation.sourceArtifact)"
    }
    $criterionId = [string]$Row.criterionId
    $thresholdPath = [string]$Consumer.thresholdPath
    switch ([string]$Consumer.scenario) {
        'rate-matrix' {
            if ($criterionId -notmatch '^rate\.c(?<concurrency>[0-9]+)\.r(?<round>[0-9]+)\.(?<metric>throughput|p50|p95|p99|max)$') {
                throw 'rate criterion id invalid'
            }
            $criterionConcurrency = [int]$Matches['concurrency']
            $criterionRound = [int]$Matches['round']
            $criterionMetric = [string]$Matches['metric']
            if ($thresholdPath -notmatch '^numericThresholds\.rate\.(?<thresholdConcurrency>[0-9]+)\.' -or [int]$Matches['thresholdConcurrency'] -ne $criterionConcurrency) {
                throw 'rate criterion threshold binding mismatch'
            }
            $source = Read-JsonFile -Path $path
            $rows = @($source.rounds | Where-Object { [int]$_.concurrency -eq $criterionConcurrency -and [int]$_.round -eq $criterionRound })
            if ($rows.Count -ne 1) { throw 'rate source row cardinality invalid' }
            $field = @{ throughput = 'throughput'; p50 = 'p50Ms'; p95 = 'p95Ms'; p99 = 'p99Ms'; max = 'maxMs' }[$criterionMetric]
            return Get-StrictJsonDecimal -Value $rows[0].$field
        }
        'tenant-scoped-cleanup' {
            if ($criterionId -notmatch '^cleanup\.(?<scale>[0-9]+)\.duration$' -or -not $thresholdPath.EndsWith(".$($Matches['scale'])")) {
                throw 'cleanup criterion threshold binding mismatch'
            }
            $source = Read-JsonFile -Path $path
            $rows = @($source.scales | Where-Object { [int]$_.scale -eq [int]$Matches['scale'] })
            if ($rows.Count -ne 1) { throw 'cleanup source row cardinality invalid' }
            return Get-StrictJsonDecimal -Value $rows[0].durationMs
        }
        'postgres-same-pool-recovery' {
            if ($criterionId -notmatch '^recovery\.(?<metric>database|hikari|request|samplingGap)\.r(?<round>[0-9]+)$') {
                throw 'recovery criterion id invalid'
            }
            $expectedSuffix = @{ database = '.database'; hikari = '.hikari'; request = '.request'; samplingGap = '.samplingGap' }[$Matches['metric']]
            if (-not $thresholdPath.EndsWith($expectedSuffix)) { throw 'recovery criterion threshold binding mismatch' }
            $rows = @(Import-Csv -LiteralPath $path | Where-Object { [int]$_.round -eq [int]$Matches['round'] })
            if ($rows.Count -ne 1) { throw 'recovery source row cardinality invalid' }
            $field = @{ database = 'databaseReadyMs'; hikari = 'hikariReadyMs'; request = 'requestReadyMs'; samplingGap = 'samplingGapMs' }[$Matches['metric']]
            return [decimal]$rows[0].$field
        }
        'postgres-hikari-contention' {
            if ($criterionId -notmatch '^contention\.(?<metric>hikariPending|acquire|postgresWaiting|lockWaiting)$') {
                throw 'contention criterion id invalid'
            }
            $expectedSuffix = @{ hikariPending = '.hikariPending'; acquire = '.acquireMs'; postgresWaiting = '.postgresWaiting'; lockWaiting = '.postgresLockWaiting' }[$Matches['metric']]
            if (-not $thresholdPath.EndsWith($expectedSuffix)) { throw 'contention criterion threshold binding mismatch' }
            $field = @{ hikariPending = 'hikariPending'; acquire = 'acquireMs'; postgresWaiting = 'postgresWaiting'; lockWaiting = 'postgresLockWaiting' }[$Matches['metric']]
            $values = @(Import-Csv -LiteralPath $path | ForEach-Object { [decimal]$_.$field })
            if ($values.Count -eq 0) { throw 'contention source has no rows' }
            return [decimal](($values | Measure-Object -Maximum).Maximum)
        }
        'full-regression' {
            $expectedCriterion = @{
                'numericThresholds.regressionMax.durationMs' = 'regression.duration'
                'numericThresholds.regressionMax.mavenWorkingSetBytes' = 'regression.mavenWorkingSet'
                'numericThresholds.regressionMax.surefireWorkingSetBytes' = 'regression.surefireWorkingSet'
                'numericThresholds.regressionMax.dockerMemoryBytes' = 'regression.dockerMemory'
                'numericThresholds.regressionMax.minimumFreeMemoryBytes' = 'regression.minimumFreeMemory'
            }[$thresholdPath]
            if ($criterionId -ne $expectedCriterion) { throw 'regression criterion threshold binding mismatch' }
            $source = Read-JsonFile -Path $path
            $field = ([string]$Expectation.artifactField).Substring(([string]$Expectation.artifactField).IndexOf('#') + 1)
            return Get-StrictJsonDecimal -Value $source.$field
        }
        default { throw "threshold source scenario unsupported: $($Consumer.scenario)" }
    }
}

function Test-FormalEvidencePacket {
    param([string]$CommitSha)
    $findings = New-Object Collections.Generic.List[string]
    $executionPath = Join-Path $EvidenceRoot 'capacity-execution-manifest.json'
    if (-not (Test-Path -LiteralPath $executionPath -PathType Leaf)) {
        return @('EXECUTION_MANIFEST_MISSING')
    }
    $execution = Read-JsonFile -Path $executionPath
    $bindingFields = @('attemptId', 'candidateSha', 'candidateTree', 'profileId', 'profileVersion', 'scenarioSetHash', 'thresholdSetHash', 'environmentManifestHash', 'harnessVersion', 'harnessHash')
    $artifactRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json')
    foreach ($name in @($artifactRegistry.mandatory | Where-Object { $_.EndsWith('.json') })) {
        $path = Join-Path $EvidenceRoot $name
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            $findings.Add("BINDING_ARTIFACT_MISSING:$name")
            continue
        }
        $artifact = Read-JsonFile -Path $path
        foreach ($field in $bindingFields) {
            if ([string]$artifact.$field -ne [string]$execution.$field) {
                $findings.Add("BINDING_MISMATCH:${name}:$field")
            }
        }
    }
    $environmentManifest = Read-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-environment-manifest.json')
    try {
        if ((Get-EnvironmentManifestContentHash -Manifest $environmentManifest) -ne [string]$execution.environmentManifestHash) {
            $findings.Add('ENVIRONMENT_MANIFEST_HASH_MISMATCH')
        }
    }
    catch {
        $findings.Add('ENVIRONMENT_MANIFEST_HASH_UNREADABLE')
    }
    $currentHead = Get-GitValue -Arguments @('rev-parse', 'HEAD')
    $currentTree = Get-GitValue -Arguments @('rev-parse', 'HEAD^{tree}')
    if ($execution.candidateSha -ne $currentHead -or $CommitSha -ne $currentHead) {
        $findings.Add('FINALIZER_HEAD_MISMATCH')
    }
    if ($execution.candidateTree -ne $currentTree) {
        $findings.Add('FINALIZER_TREE_MISMATCH')
    }
    $trackedChanges = @(Get-GitPathList -Arguments @('diff', '--name-only'))
    $untrackedChanges = @(Get-GitPathList -Arguments @('ls-files', '--others', '--exclude-standard'))
    if (@($trackedChanges + $untrackedChanges | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count -gt 0) {
        $findings.Add('FINALIZER_WORKTREE_DIRTY')
    }
    $stagedChanges = @(Get-GitPathList -Arguments @('diff', '--cached', '--name-only'))
    if (@($stagedChanges | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count -gt 0) {
        $findings.Add('FINALIZER_STAGED_DIRTY')
    }
    if ((Get-Sha256 -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')) -ne [string]$execution.scenarioSetHash) {
        $findings.Add('FINALIZER_SCENARIO_SET_HASH_MISMATCH')
    }
    if ((Get-Sha256 -Path (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json')) -ne [string]$execution.thresholdSetHash) {
        $findings.Add('FINALIZER_THRESHOLD_SET_HASH_MISMATCH')
    }
    if ((Get-HarnessHash) -ne [string]$execution.harnessHash) {
        $findings.Add('FINALIZER_HARNESS_HASH_MISMATCH')
    }
    if ($execution.candidateSha -ne $CommitSha -or $execution.candidateTree -ne $currentTree -or $execution.profileId -ne 'qdr7-capacity-acceptance' -or $execution.profileVersion -ne $CriteriaVersion) {
        $findings.Add('CANDIDATE_OR_PROFILE_BINDING_MISMATCH')
    }

    $scenarioRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')
    $ledger = Read-JsonFile -Path (Join-Path $EvidenceRoot 'scenario-ledger.json')
    foreach ($scenarioId in $scenarioRegistry.fixedOrder) {
        if (@($ledger.scenarios | Where-Object { $_.scenarioId -eq $scenarioId }).Count -ne 1) {
            $findings.Add("SCENARIO_CARDINALITY_INVALID:$scenarioId")
        }
    }
    $scenarioIds = @($ledger.scenarios | ForEach-Object { [string]$_.scenarioId })
    if (@($scenarioIds | Sort-Object -Unique).Count -ne @($scenarioIds).Count) {
        $findings.Add('DUPLICATE_SCENARIO_RESULT')
    }

    $consumerMatrix = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-threshold-consumers.json')
    $thresholdProfile = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json')
    $threshold = Read-JsonFile -Path (Join-Path $EvidenceRoot 'threshold-comparison.json')
    foreach ($consumer in $consumerMatrix.consumers) {
        $rows = @($threshold.comparisons | Where-Object { $_.thresholdPath -eq $consumer.thresholdPath })
        if ($rows.Count -ne [int]$consumer.expectedExecutions) {
            $findings.Add("THRESHOLD_EXECUTION_COUNT_MISMATCH:$($consumer.thresholdPath)")
        }
        try {
            $expectation = Get-ThresholdSemanticExpectation -Consumer $consumer -ThresholdProfile $thresholdProfile
            if ([string]$consumer.artifactField -ne [string]$expectation.artifactField) {
                $findings.Add("THRESHOLD_ARTIFACT_FIELD_MISMATCH:$($consumer.thresholdPath)")
            }
            if ([string]$expectation.sourceArtifact -notin @($artifactRegistry.mandatory)) {
                $findings.Add("THRESHOLD_SOURCE_ARTIFACT_UNSEALED:$($consumer.thresholdPath)")
            }
            foreach ($row in $rows) {
                try {
                    $sourceObserved = Get-ThresholdObservedFromSource -Row $row -Consumer $consumer -Expectation $expectation
                }
                catch {
                    $findings.Add("THRESHOLD_SOURCE_OBSERVED_UNREADABLE:$($consumer.thresholdPath):$($row.criterionId)")
                    continue
                }
                foreach ($finding in @(Test-ThresholdSemanticRow -Row $row -Expectation $expectation -ThresholdPath ([string]$consumer.thresholdPath) -SourceObserved $sourceObserved)) {
                    $findings.Add([string]$finding)
                }
            }
        }
        catch {
            $findings.Add("THRESHOLD_EXPECTATION_UNREADABLE:$($consumer.thresholdPath)")
        }
    }
    $knownPaths = @($consumerMatrix.consumers | ForEach-Object { [string]$_.thresholdPath })
    foreach ($row in $threshold.comparisons) {
        if ([string]$row.thresholdPath -notin $knownPaths) {
            $findings.Add("ORPHAN_THRESHOLD:$($row.thresholdPath)")
        }
    }
    $criterionIds = @($threshold.comparisons | ForEach-Object { [string]$_.criterionId })
    if (@($criterionIds | Sort-Object -Unique).Count -ne @($criterionIds).Count) {
        $findings.Add('DUPLICATE_THRESHOLD_RESULT')
    }
    if (@($threshold.comparisons).Count -ne 99 -or @($knownPaths | Sort-Object -Unique).Count -ne 41) {
        $findings.Add('THRESHOLD_SET_INCOMPLETE')
    }
    foreach ($finding in @(Get-ThresholdAggregateFindings -Threshold $threshold)) {
        $findings.Add([string]$finding)
    }

    $samplerCompletionPath = Join-Path $EvidenceRoot 'resource-sampler-completion.json'
    if (-not (Test-Path -LiteralPath $samplerCompletionPath -PathType Leaf)) {
        $findings.Add('RESOURCE_SAMPLER_COMPLETION_INVALID')
    }
    else {
        try {
            $samplerCompletion = Read-JsonFile -Path $samplerCompletionPath
            if ($samplerCompletion.schemaVersion -ne 'qdr7-capacity-resource-sampler-completion-2' -or
                $samplerCompletion.status -ne 'COMPLETED' -or
                $samplerCompletion.runId -ne $RunId -or
                $samplerCompletion.commitSha -ne $currentHead -or
                [long]$samplerCompletion.elapsedMs -le 0L -or
                [int]$samplerCompletion.jvmRowCount -le 0 -or
                [int]$samplerCompletion.dockerRowCount -le 0 -or
                [int]$samplerCompletion.mavenRowCount -le 0 -or
                [int]$samplerCompletion.surefireRowCount -le 0 -or
                [int]$samplerCompletion.fullRegressionMavenSampleCount -lt 2 -or
                [int]$samplerCompletion.fullRegressionSurefireSampleCount -lt 2 -or
                [long]$samplerCompletion.jvmLastElapsedMs -le 0L -or
                [long]$samplerCompletion.dockerLastElapsedMs -le 0L -or
                [int]$samplerCompletion.jvmCriticalMissingCount -ne 0 -or
                [int]$samplerCompletion.dockerCriticalMissingCount -ne 0) {
                $findings.Add('RESOURCE_SAMPLER_COMPLETION_INVALID')
            }
            $jvmSeriesHash = Get-Sha256 -Path (Join-Path $EvidenceRoot 'jvm-series.csv')
            $dockerSeriesHash = Get-Sha256 -Path (Join-Path $EvidenceRoot 'docker-series.csv')
            if ([string]$samplerCompletion.jvmSeriesSha256 -ne $jvmSeriesHash -or
                [string]$samplerCompletion.dockerSeriesSha256 -ne $dockerSeriesHash) {
                $findings.Add('RESOURCE_SAMPLER_RAW_HASH_MISMATCH')
            }
            $resourceSummary = Read-JsonFile -Path (Join-Path $EvidenceRoot 'resource-summary.json')
            if ([string]$resourceSummary.jvmSeriesSha256 -ne $jvmSeriesHash -or
                [string]$resourceSummary.dockerSeriesSha256 -ne $dockerSeriesHash -or
                [string]$resourceSummary.samplerCompletionSchemaVersion -ne [string]$samplerCompletion.schemaVersion -or
                [long]$resourceSummary.samplerJvmLastElapsedMs -ne [long]$samplerCompletion.jvmLastElapsedMs -or
                [long]$resourceSummary.samplerDockerLastElapsedMs -ne [long]$samplerCompletion.dockerLastElapsedMs) {
                $findings.Add('RESOURCE_SUMMARY_RAW_BINDING_MISMATCH')
            }
        }
        catch {
            $findings.Add('RESOURCE_SAMPLER_COMPLETION_INVALID')
        }
    }

    $inventory = Read-JsonFile -Path (Join-Path $EvidenceRoot 'artifact-inventory.json')
    $inventoryPaths = @($inventory.artifacts | ForEach-Object { [string]$_.path })
    if (@($inventoryPaths | Sort-Object -Unique).Count -ne @($inventoryPaths).Count) {
        $findings.Add('DUPLICATE_ARTIFACT_INVENTORY_ENTRY')
    }
    foreach ($row in $inventory.artifacts) {
        $path = [IO.Path]::GetFullPath((Join-Path $EvidenceRoot ([string]$row.path)))
        if (-not $path.StartsWith($EvidenceRoot, [StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Sha256 -Path $path) -ne [string]$row.sha256) {
            $findings.Add("ARTIFACT_INVENTORY_HASH_MISMATCH:$($row.path)")
        }
    }
    return $findings.ToArray()
}

function Test-FormalPacketPreparationGate {
    param([object]$ExecutionManifest)
    if ($null -eq $ExecutionManifest.PSObject.Properties['formalPacketPrepared']) {
        return @('FORMAL_PACKET_PREPARED_MISSING')
    }
    if ($ExecutionManifest.formalPacketPrepared -isnot [bool]) {
        return @('FORMAL_PACKET_PREPARED_TYPE_INVALID')
    }
    if ($ExecutionManifest.formalPacketPrepared -ne $true) {
        return @('FORMAL_PACKET_NOT_PREPARED')
    }
    return @()
}

function Invoke-FormalPacketGateContractTest {
    $missing = [pscustomobject]@{}
    $falseValue = [pscustomobject]@{ formalPacketPrepared = $false }
    $wrongType = [pscustomobject]@{ formalPacketPrepared = 'true' }
    $trueValue = [pscustomobject]@{ formalPacketPrepared = $true }
    if (@(Test-FormalPacketPreparationGate -ExecutionManifest $missing) -notcontains 'FORMAL_PACKET_PREPARED_MISSING' -or
        @(Test-FormalPacketPreparationGate -ExecutionManifest $falseValue) -notcontains 'FORMAL_PACKET_NOT_PREPARED' -or
        @(Test-FormalPacketPreparationGate -ExecutionManifest $wrongType) -notcontains 'FORMAL_PACKET_PREPARED_TYPE_INVALID' -or
        @(Test-FormalPacketPreparationGate -ExecutionManifest $trueValue).Count -ne 0) {
        Write-Error 'formal packet gate contract failed'
        exit 80
    }
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'rate-summary.json') -Value ([ordered]@{ rounds = @([ordered]@{ concurrency = 1; round = 1; throughput = 12.5; p50Ms = 2; p95Ms = 3; p99Ms = 4; maxMs = 5 }) })
    $rateConsumer = [pscustomobject]@{ scenario = 'rate-matrix'; thresholdPath = 'numericThresholds.rate.1.throughputMin' }
    $rateExpectation = [pscustomobject]@{ sourceArtifact = 'rate-summary.json'; artifactField = 'rate-summary.json#rounds[concurrency,round].throughput' }
    $rateRow = [pscustomobject]@{ criterionId = 'rate.c1.r1.throughput' }
    if ((Get-ThresholdObservedFromSource -Row $rateRow -Consumer $rateConsumer -Expectation $rateExpectation) -ne [decimal]12.5) {
        Write-Error 'rate source binding contract failed'
        exit 80
    }
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'cleanup-summary.json') -Value ([ordered]@{ scales = @([ordered]@{ scale = 10; durationMs = 21 }) })
    $cleanupObserved = Get-ThresholdObservedFromSource -Row ([pscustomobject]@{ criterionId = 'cleanup.10.duration' }) -Consumer ([pscustomobject]@{ scenario = 'tenant-scoped-cleanup'; thresholdPath = 'numericThresholds.cleanupMaxMs.10' }) -Expectation ([pscustomobject]@{ sourceArtifact = 'cleanup-summary.json'; artifactField = 'cleanup-summary.json#scales[scale].durationMs' })
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'recovery-timeline.csv') -Content "round,databaseReadyMs,hikariReadyMs,requestReadyMs,samplingGapMs`n1,31,32,33,34`n"
    $recoveryObserved = Get-ThresholdObservedFromSource -Row ([pscustomobject]@{ criterionId = 'recovery.database.r1' }) -Consumer ([pscustomobject]@{ scenario = 'postgres-same-pool-recovery'; thresholdPath = 'numericThresholds.recoveryMaxMs.database' }) -Expectation ([pscustomobject]@{ sourceArtifact = 'recovery-timeline.csv'; artifactField = 'recovery-timeline.csv#rows[round].databaseReadyMs' })
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'postgres-hikari-series.csv') -Content "hikariPending,acquireMs,postgresWaiting,postgresLockWaiting`n1,10,2,0`n3,20,1,1`n"
    $contentionObserved = Get-ThresholdObservedFromSource -Row ([pscustomobject]@{ criterionId = 'contention.hikariPending' }) -Consumer ([pscustomobject]@{ scenario = 'postgres-hikari-contention'; thresholdPath = 'numericThresholds.contentionMax.hikariPending' }) -Expectation ([pscustomobject]@{ sourceArtifact = 'postgres-hikari-series.csv'; artifactField = 'postgres-hikari-series.csv#max(hikariPending)' })
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'resource-summary.json') -Value ([ordered]@{ durationMs = 41 })
    $resourceObserved = Get-ThresholdObservedFromSource -Row ([pscustomobject]@{ criterionId = 'regression.duration' }) -Consumer ([pscustomobject]@{ scenario = 'full-regression'; thresholdPath = 'numericThresholds.regressionMax.durationMs' }) -Expectation ([pscustomobject]@{ sourceArtifact = 'resource-summary.json'; artifactField = 'resource-summary.json#durationMs' })
    if ($cleanupObserved -ne 21 -or $recoveryObserved -ne 31 -or $contentionObserved -ne 3 -or $resourceObserved -ne 41) {
        Write-Error 'threshold source binding contract failed'
        exit 80
    }
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'resource-summary.json') -Value ([ordered]@{ durationMs = '41' })
    $nonnumericRejected = $false
    try {
        [void](Get-ThresholdObservedFromSource -Row ([pscustomobject]@{ criterionId = 'regression.duration' }) -Consumer ([pscustomobject]@{ scenario = 'full-regression'; thresholdPath = 'numericThresholds.regressionMax.durationMs' }) -Expectation ([pscustomobject]@{ sourceArtifact = 'resource-summary.json'; artifactField = 'resource-summary.json#durationMs' }))
    }
    catch {
        $nonnumericRejected = $true
    }
    if (-not $nonnumericRejected) {
        Write-Error 'nonnumeric source metric was accepted'
        exit 80
    }
    exit 0
}

function Get-ThresholdAggregateFindings {
    param([object]$Threshold, [bool]$AllowNotRun = $false)
    $findings = New-Object Collections.Generic.List[string]
    $rows = @((Get-ObjectPropertyValue -Target $Threshold -Name 'comparisons' -DefaultValue @()))
    $passed = @($rows | Where-Object { $_.status -eq 'PASS' }).Count
    $failed = @($rows | Where-Object { $_.status -eq 'FAIL' }).Count
    $blocked = @($rows | Where-Object { $_.status -eq 'BLOCKED' }).Count
    $unknown = @($rows | Where-Object { $_.status -notin @('PASS', 'FAIL', 'BLOCKED') }).Count
    if ($unknown -gt 0) {
        $findings.Add('THRESHOLD_ROW_STATUS_INVALID')
    }
    $comparisonCount = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'comparisonCount' -DefaultValue -1)
    $comparisonsExecuted = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'comparisonsExecuted' -DefaultValue -1)
    $passedCount = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'passedCount' -DefaultValue -1)
    $failedCount = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'failedCount' -DefaultValue -1)
    $blockedCount = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'blockedCount' -DefaultValue -1)
    $notEvaluatedCount = [int](Get-ObjectPropertyValue -Target $Threshold -Name 'notEvaluatedCount' -DefaultValue -1)
    if ($comparisonCount -ne $rows.Count -or
        $comparisonsExecuted -ne $rows.Count -or
        $passedCount -ne $passed -or
        $failedCount -ne $failed -or
        $blockedCount -ne $blocked -or
        $notEvaluatedCount -lt 0) {
        $findings.Add('THRESHOLD_STATUS_AGGREGATE_MISMATCH')
    }
    $notRunContract = $AllowNotRun -and
        $rows.Count -eq 0 -and
        $comparisonCount -eq 0 -and
        $comparisonsExecuted -eq 0 -and
        $passedCount -eq 0 -and
        $failedCount -eq 0 -and
        $blockedCount -eq 0 -and
        $notEvaluatedCount -eq 99 -and
        [int](Get-ObjectPropertyValue -Target $Threshold -Name 'notEvaluatedThresholds' -DefaultValue -1) -eq 99 -and
        [int](Get-ObjectPropertyValue -Target $Threshold -Name 'declaredThresholdLeaves' -DefaultValue -1) -eq 41 -and
        [string](Get-ObjectPropertyValue -Target $Threshold -Name 'reason' -DefaultValue '') -eq 'FORMAL_SCENARIO_NOT_EXECUTED'
    $expectedStatus = $(if ($notRunContract) { 'NOT_RUN' } elseif ($failed -gt 0) { 'FAIL' } elseif ($blocked -gt 0 -or $notEvaluatedCount -ne 0) { 'BLOCKED' } else { 'PASS' })
    $recordedStatus = [string](Get-ObjectPropertyValue -Target $Threshold -Name 'status' -DefaultValue '')
    if ($recordedStatus -ne $expectedStatus) {
        $findings.Add('THRESHOLD_TOP_LEVEL_STATUS_MISMATCH')
    }
    return $findings.ToArray()
}

function Get-ObjectPropertyValue {
    param([object]$Target, [string]$Name, [object]$DefaultValue)
    if ($Target -is [Collections.IDictionary] -and $Target.Contains($Name)) {
        return $Target[$Name]
    }
    $property = $Target.PSObject.Properties[$Name]
    if ($null -ne $property) {
        return $property.Value
    }
    return $DefaultValue
}

function Get-ScenarioVerdictFromLedger {
    param([object]$Ledger, [string]$ScenarioId)
    $rows = @($Ledger['scenarios'] | Where-Object { $_.scenarioId -eq $ScenarioId })
    if ($rows.Count -ne 1 -or $rows[0].verdict -eq 'NOT_EVALUATED') {
        return 'BLOCKED'
    }
    return [string]$rows[0].verdict
}

function Get-StatusForExitCode {
    param([int]$ExitCode)
    if ($ExitCode -eq 0) {
        return 'PASS'
    }
    if ($ExitCode -eq 80) {
        return 'INVALID'
    }
    if ($ExitCode -in @(40, 50, 60, 70, 90)) {
        return 'FAIL'
    }
    return 'BLOCKED'
}

function Get-ReasonForExitCode {
    param([int]$ExitCode)
    switch ($ExitCode) {
        0 { return 'FORMAL_CAPACITY_ACCEPTANCE_COMPLETED' }
        10 { return 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED' }
        20 { return 'APPLICATION_CONTEXT_STARTUP_BLOCKED' }
        30 { return 'MANDATORY_SCENARIO_INCOMPLETE' }
        40 { return 'CORRECTNESS_INVARIANT_FAILED' }
        50 { return 'NUMERIC_THRESHOLD_FAILED' }
        60 { return 'FULL_REGRESSION_FAILED' }
        70 { return 'QUALITY_GATE_FAILED' }
        80 { return 'ARTIFACT_VALIDATION_BLOCKED' }
        90 { return 'SECRET_SCAN_FAILED' }
        100 { return 'UNEXPECTED_HARNESS_FAILURE' }
        default { return 'UNEXPECTED_HARNESS_FAILURE' }
    }
}

function Invoke-RuntimeBlockedContractTest {
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
    $commitSha = Get-GitValue -Arguments @('rev-parse', 'HEAD')
    $started = Get-UtcTimestamp
    $environment = New-Artifact -Scenario 'environment' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $environment.contractTest = $true
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment
    $preflight = New-Artifact -Scenario 'environment-preflight' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $preflight.checks = @()
    $preflight.blockers = @()
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'preflight.json') -Value $preflight
    $registry = New-Artifact -Scenario 'resource-registry' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $registry.containerName = "dh-qdr7-capacity-$RunId"
    $registry.volumeName = "dh-qdr7-capacity-$RunId"
    $registry.samplerPid = $null
    $registry.contractOnly = $true
    $registry.containerOwnership = 'JUNIT_TESTCONTAINERS'
    $registry.implementationValidation = $false
    $registry.teardown = [ordered]@{ sampler = 'NOT_STARTED'; container = 'PENDING'; volume = 'PENDING'; residual = 'PENDING' }
    Write-JsonFile -Path $RegistryPath -Value $registry
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'commands.txt') -Content "1 | $started | . | runtime blocked finalizer contract | STARTED`n"
    Invoke-Finalize
}

function Invoke-PartialFinalizerContractTest {
    [IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
    $commitSha = Get-GitValue -Arguments @('rev-parse', 'HEAD')
    $started = Get-UtcTimestamp
    $environment = New-Artifact -Scenario 'environment' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $environment.contractTest = $true
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment
    $preflight = New-Artifact -Scenario 'environment-preflight' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $preflight.checks = @()
    $preflight.blockers = @()
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'preflight.json') -Value $preflight
    $registry = New-Artifact -Scenario 'resource-registry' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $started
    $registry.containerName = "dh-qdr7-capacity-$RunId"
    $registry.volumeName = "dh-qdr7-capacity-$RunId"
    $registry.samplerPid = $null
    $registry.contractOnly = $true
    $registry.containerOwnership = 'JUNIT_TESTCONTAINERS'
    $registry.implementationValidation = $false
    $registry.qualificationOnly = $false
    $registry.teardown = [ordered]@{ sampler = 'NOT_STARTED'; container = 'PENDING'; volume = 'PENDING'; residual = 'PENDING' }
    Write-JsonFile -Path $RegistryPath -Value $registry

    $ledgerSnapshot = Get-NormalizedScenarioLedger -CommitSha $commitSha -StartedAt $started
    $ledgerArtifact = $ledgerSnapshot['artifact']
    $rows = @($ledgerArtifact.scenarios)
    $rows[0].executionState = 'COMPLETED'
    $rows[0].verdict = 'PASS'
    $rows[0].startedAt = $started
    $rows[0].completedAt = Get-UtcTimestamp
    $rows[0].roundsStarted = 1
    $rows[0].roundsCompleted = 1
    $rows[0].measurementsCaptured = 13
    $rows[0].reasonCode = 'PASS'
    $rows[1].executionState = 'STARTED'
    $rows[1].verdict = 'NOT_EVALUATED'
    $rows[1].startedAt = $started
    $rows[1].roundsStarted = 2
    $rows[1].roundsCompleted = 1
    $rows[1].measurementsCaptured = 100
    $rows[1].comparisonsExecuted = 5
    $rows[1].reasonCode = 'SCENARIO_STARTED'
    $ledgerArtifact.scenarios = $rows
    $ledgerArtifact.status = 'BLOCKED'
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'scenario-ledger.json') -Value $ledgerArtifact

    $threshold = New-Artifact -Scenario 'threshold-comparison' -Status 'BLOCKED' -CommitSha $commitSha -StartedAt $started -FinishedAt (Get-UtcTimestamp)
    $threshold.comparisons = @(1..5 | ForEach-Object { [ordered]@{ criterionId = "partial.$_"; status = 'PASS' } })
    $threshold.comparisonCount = 5
    $threshold.comparisonsExecuted = 5
    $threshold.passedCount = 5
    $threshold.failedCount = 0
    $threshold.blockedCount = 0
    $threshold.notEvaluatedCount = 94
    $threshold.notEvaluatedThresholds = 94
    $threshold.reason = 'PARTIAL_THRESHOLD_EVIDENCE'
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'threshold-comparison.json') -Value $threshold
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'commands.txt') -Content "1 | $started | . | partial finalizer contract | STARTED`n"
    Invoke-Finalize
}

function Invoke-Finalize {
    if (-not (Test-Path -LiteralPath $RegistryPath)) {
        exit 80
    }
    $registry = Read-JsonFile -Path $RegistryPath
    $commitSha = [string]$registry.commitSha
    $modeBindingFindings = New-Object Collections.Generic.List[string]
    $recordedImplementationValidation = [bool](Get-ObjectPropertyValue -Target $registry -Name 'implementationValidation' -DefaultValue $false)
    $recordedQualificationOnly = [bool](Get-ObjectPropertyValue -Target $registry -Name 'qualificationOnly' -DefaultValue $false)
    $recordedMode = $(if ($recordedImplementationValidation) { 'IMPLEMENTATION_VALIDATION' } elseif ($recordedQualificationOnly) { 'QUALIFICATION' } else { 'FORMAL' })
    if (($recordedImplementationValidation -and $recordedQualificationOnly) -or $recordedMode -ne (Get-ExecutionMode)) {
        $modeBindingFindings.Add('EXECUTION_MODE_BINDING_MISMATCH')
    }
    $recordedExecutionPath = Join-Path $EvidenceRoot 'capacity-execution-manifest.json'
    if (Test-Path -LiteralPath $recordedExecutionPath -PathType Leaf) {
        $recordedExecution = Read-JsonFile -Path $recordedExecutionPath
        Set-EvidenceBinding -CandidateSha ([string]$recordedExecution.candidateSha) -CandidateTree ([string]$recordedExecution.candidateTree) -ProfileId ([string]$recordedExecution.profileId) -ProfileVersion ([string]$recordedExecution.profileVersion) -ScenarioSetHash ([string]$recordedExecution.scenarioSetHash) -ThresholdSetHash ([string]$recordedExecution.thresholdSetHash) -EnvironmentManifestHash ([string]$recordedExecution.environmentManifestHash) -HarnessHash ([string]$recordedExecution.harnessHash)
        if ([string](Get-ObjectPropertyValue -Target $recordedExecution -Name 'executionMode' -DefaultValue '') -ne $recordedMode) {
            $modeBindingFindings.Add('EXECUTION_MANIFEST_MODE_MISMATCH')
        }
    }
    $teardownFindings = New-Object Collections.Generic.List[string]
    try {
        foreach ($finding in @(Stop-RegisteredResources -Registry $registry -CommitSha $commitSha)) {
            $teardownFindings.Add([string]$finding)
        }
    }
    catch {
        $teardownFindings.Add('REGISTERED_RESOURCE_TEARDOWN_UNEXPECTED_FAILURE')
    }
    $registry = Read-JsonFile -Path $RegistryPath
    $artifactRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json')
    $scenarioRegistry = Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json')
    $mandatoryCount = [int]$scenarioRegistry.mandatoryCount
    $startedAt = ConvertTo-UtcTimestamp -Value $registry.startedAtUtc
    if ([string]::IsNullOrWhiteSpace($startedAt)) {
        $startedAt = Get-UtcTimestamp
    }
    $existingExit = 0
    $hasRecordedExit = $false
    $exitPath = Join-Path $EvidenceRoot 'harness-exit-code.txt'
    if (Test-Path -LiteralPath $exitPath) {
        $hasRecordedExit = [int]::TryParse((Get-Content -Raw -LiteralPath $exitPath).Trim(), [ref]$existingExit)
    }

    $ledger = Get-NormalizedScenarioLedger -CommitSha $commitSha -StartedAt $startedAt
    $summaryPath = Join-Path $EvidenceRoot 'capacity-acceptance-summary.json'
    if (Test-Path -LiteralPath $summaryPath) {
        $summary = Read-JsonFile -Path $summaryPath
    }
    else {
        $now = Get-UtcTimestamp
        $summary = New-Artifact -Scenario 'capacity-acceptance' -Status 'BLOCKED' -CommitSha $commitSha -StartedAt $now -FinishedAt $now
    }

    $implementationArtifact = Join-Path $EvidenceRoot 'implementation-validation.json'
    $implementationValidationPassed = $ImplementationValidationEnabled -and (Test-Path -LiteralPath $implementationArtifact -PathType Leaf) -and $hasRecordedExit -and $existingExit -eq 0
    $allScenariosPassed = [int]$ledger['completedScenarioCount'] -eq $mandatoryCount -and [int]$ledger['passedScenarioCount'] -eq $mandatoryCount
    if ($implementationValidationPassed) {
        $baseExit = 0
    }
    elseif (-not $hasRecordedExit) {
        $baseExit = $(if ([int]$ledger['executedScenarioCount'] -gt 0) { 100 } else { 20 })
    }
    else {
        $baseExit = $existingExit
    }
    if (-not $implementationValidationPassed -and $baseExit -eq 0 -and -not $allScenariosPassed) {
        $baseExit = 30
    }

    $reasonCode = [string](Get-ObjectPropertyValue -Target $summary -Name 'reasonCode' -DefaultValue '')
    if ([string]::IsNullOrWhiteSpace($reasonCode) -or $reasonCode -in @('FORMAL_CAPACITY_ACCEPTANCE_BLOCKED_OR_FAILED', 'HARNESS_RUNNING')) {
        $reasonCode = Get-ReasonForExitCode -ExitCode $baseExit
    }
    if ($implementationValidationPassed) {
        $reasonCode = 'IMPLEMENTATION_VALIDATION_ONLY'
    }
    elseif ($QualificationOnlyEnabled -and $baseExit -eq 0 -and $allScenariosPassed) {
        $reasonCode = 'QUALIFICATION_ONLY'
    }
    $placeholderStatus = $(if ($implementationValidationPassed) { 'NOT_RUN' } else { 'BLOCKED' })
    if ($baseExit -ne 0 -or $implementationValidationPassed) {
        Write-MissingScenarioArtifacts -CommitSha $commitSha -StartedAt $startedAt -Status $placeholderStatus -ReasonCode $reasonCode
    }

    $correctnessVerdict = $(if ($implementationValidationPassed) { 'NOT_RUN' } elseif ([int]$ledger['failedScenarioCount'] -gt 0) { 'FAIL' } elseif ($allScenariosPassed) { 'PASS' } else { 'BLOCKED' })
    $thresholdVerdict = 'BLOCKED'
    $thresholdPath = Join-Path $EvidenceRoot 'threshold-comparison.json'
    if ($implementationValidationPassed) {
        $thresholdVerdict = 'NOT_RUN'
    }
    elseif (Test-Path -LiteralPath $thresholdPath -PathType Leaf) {
        $thresholdArtifact = Read-JsonFile -Path $thresholdPath
        if ($thresholdArtifact.status -in @('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN')) {
            $thresholdVerdict = [string]$thresholdArtifact.status
        }
    }
    $regressionVerdict = $(if ($implementationValidationPassed) { 'NOT_RUN' } else { Get-ScenarioVerdictFromLedger -Ledger $ledger -ScenarioId 'full-regression' })
    $qualityVerdict = $(if ($implementationValidationPassed) { 'NOT_RUN' } else { Get-ScenarioVerdictFromLedger -Ledger $ledger -ScenarioId 'quality-gate' })
    if (-not $implementationValidationPassed -and $baseExit -eq 0 -and $thresholdVerdict -ne 'PASS') {
        $baseExit = 50
        $reasonCode = 'NUMERIC_THRESHOLD_FAILED'
    }
    if (-not $implementationValidationPassed -and $baseExit -eq 0 -and $regressionVerdict -ne 'PASS') {
        $baseExit = 60
        $reasonCode = 'FULL_REGRESSION_FAILED'
    }
    if (-not $implementationValidationPassed -and $baseExit -eq 0 -and $qualityVerdict -ne 'PASS') {
        $baseExit = 70
        $reasonCode = 'QUALITY_GATE_FAILED'
    }

    $capacityStatus = $(if ($implementationValidationPassed) { 'NOT_RUN' } elseif ($QualificationOnlyEnabled -and $baseExit -eq 0) { 'NOT_FORMAL' } else { Get-StatusForExitCode -ExitCode $baseExit })
    $completedAt = Get-UtcTimestamp
    $teardownVerdict = $(if ($teardownFindings.Count -eq 0 -and $registry.teardown.residual -eq 'NONE') { 'PASS' } else { 'BLOCKED' })
    Set-SummaryContract -Summary $summary -StartedAt $startedAt -CompletedAt $completedAt -Status $capacityStatus -InternalExitCode $baseExit -MandatoryScenarioCount $mandatoryCount -ExecutedScenarioCount ([int]$ledger['executedScenarioCount']) -CorrectnessVerdict $correctnessVerdict -ThresholdVerdict $thresholdVerdict -RegressionVerdict $regressionVerdict -QualityVerdict $qualityVerdict -ArtifactVerdict 'PENDING' -SecretVerdict 'PENDING' -TeardownVerdict $teardownVerdict -ReasonCode $reasonCode
    Set-SummaryScenarioCounts -Summary $summary -Ledger $ledger
    $firstBlocker = @($ledger['scenarios'] | Where-Object { $_.verdict -ne 'PASS' } | ForEach-Object { "$($_.scenarioId):$($_.reasonCode)" } | Select-Object -First 1)
    Set-ObjectProperty -Target $summary -Name 'firstBlocker' -Value $(if ($firstBlocker.Count -eq 0) { $null } else { $firstBlocker[0] })
    Set-ObjectProperty -Target $summary -Name 'capacityAcceptanceExecuted' -Value (-not $ImplementationValidationEnabled -and -not $QualificationOnlyEnabled -and $baseExit -eq 0 -and $allScenariosPassed)
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($ImplementationValidationEnabled -or $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($baseExit -eq 0) { 'PASS_WITHIN_FROZEN_PROFILE' } else { Get-StatusForExitCode -ExitCode $baseExit })
    Set-ObjectProperty -Target $summary -Name 'qualificationVerdict' -Value $(if (-not $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($baseExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $baseExit })
    Set-ObjectProperty -Target $summary -Name 'artifactValidationFindings' -Value @()
    Set-ObjectProperty -Target $summary -Name 'secretFindingCount' -Value 0
    Set-ObjectProperty -Target $summary -Name 'teardown' -Value $registry.teardown
    Set-ObjectProperty -Target $summary -Name 'scenarioStatuses' -Value $ledger['scenarios']
    $summary.finishedAtUtc = $completedAt
    Write-JsonFile -Path $summaryPath -Value $summary
    Write-Utf8File -Path $exitPath -Content "$baseExit`n"

    $secretFindings = Invoke-SecretScan -CommitSha $commitSha
    Write-ArtifactInventory -CommitSha $commitSha -StartedAt $startedAt
    Write-CapacityFinalVerdict -CommitSha $commitSha -StartedAt $startedAt -Verdict 'BLOCKED' -ReasonCode 'FINALIZER_PENDING' -IntegrityFindings @()
    $findings = New-Object Collections.Generic.List[string]
    foreach ($finding in @($modeBindingFindings)) {
        $findings.Add([string]$finding)
    }
    foreach ($finding in @($ledger['integrityFindings'])) {
        $findings.Add([string]$finding)
    }
    foreach ($finding in $teardownFindings) {
        $findings.Add([string]$finding)
    }
    foreach ($name in $artifactRegistry.mandatory) {
        if ($name -eq 'sha256-manifest.txt') {
            continue
        }
        $path = Join-Path $EvidenceRoot $name
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            $findings.Add("MANDATORY_ARTIFACT_MISSING:$name")
        }
        elseif ($name.EndsWith('.json') -and -not (Test-CommonJsonArtifact -Path $path -CommitSha $commitSha)) {
            $findings.Add("JSON_CONTRACT_INVALID:$name")
        }
        elseif ($name.EndsWith('.csv')) {
            $header = (Get-Content -LiteralPath $path -TotalCount 1)
            if (-not $header.StartsWith('schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs')) {
                $findings.Add("CSV_HEADER_INVALID:$name")
            }
        }
    }
    $summary = Read-JsonFile -Path $summaryPath
    foreach ($field in $artifactRegistry.summaryRequired) {
        if ($null -eq $summary.PSObject.Properties[$field]) {
            $findings.Add("SUMMARY_REQUIRED_FIELD_MISSING:$field")
        }
    }
    if (Test-Path -LiteralPath $thresholdPath -PathType Leaf) {
        $threshold = Read-JsonFile -Path $thresholdPath
        foreach ($field in @('comparisons', 'comparisonCount', 'comparisonsExecuted', 'passedCount', 'failedCount', 'blockedCount', 'notEvaluatedCount', 'reason')) {
            if ($null -eq $threshold.PSObject.Properties[$field]) {
                $findings.Add("THRESHOLD_REQUIRED_FIELD_MISSING:$field")
            }
        }
        if ($null -ne $threshold.PSObject.Properties['comparisons'] -and ([int]$threshold.comparisonCount -ne @($threshold.comparisons).Count -or [int]$threshold.comparisonsExecuted -ne @($threshold.comparisons).Count)) {
            $findings.Add('THRESHOLD_COMPARISON_COUNT_MISMATCH')
        }
        foreach ($finding in @(Get-ThresholdAggregateFindings -Threshold $threshold -AllowNotRun $implementationValidationPassed)) {
            $findings.Add([string]$finding)
        }
        if ($baseExit -eq 0 -and -not $implementationValidationPassed -and ($threshold.status -ne 'PASS' -or [int]$threshold.comparisonCount -ne 99 -or [int]$threshold.notEvaluatedCount -ne 0 -or [int]$threshold.coveredThresholdLeaves -ne 41)) {
            $findings.Add('THRESHOLD_COMPLETE_CONTRACT_INVALID')
        }
    }

    if ($QualificationOnlyEnabled -and $baseExit -eq 0) {
        if ($summary.status -ne 'NOT_FORMAL' -or $summary.reasonCode -ne 'QUALIFICATION_ONLY' -or $summary.capacityAcceptanceExecuted -ne $false -or $summary.formalAcceptanceVerdict -ne 'NOT_EVALUATED' -or $summary.qualificationVerdict -ne 'PASS') {
            $findings.Add('QUALIFICATION_SUMMARY_CONTRACT_INVALID')
        }
    }
    $executionManifest = Read-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-execution-manifest.json')
    if ($Phase -eq 'Finalize' -and (Get-ExecutionMode) -eq 'FORMAL') {
        foreach ($finding in @(Test-FormalPacketPreparationGate -ExecutionManifest $executionManifest)) {
            $findings.Add([string]$finding)
        }
        foreach ($finding in @(Test-FormalEvidencePacket -CommitSha $commitSha)) {
            $findings.Add([string]$finding)
        }
    }

    $finalExit = $baseExit
    if ($findings.Count -gt 0) {
        $finalExit = 80
    }
    if ($secretFindings -gt 0) {
        $finalExit = 90
    }
    $finalStatus = $(if ($implementationValidationPassed -and $finalExit -eq 0) { 'NOT_RUN' } elseif ($QualificationOnlyEnabled -and $finalExit -eq 0) { 'NOT_FORMAL' } else { Get-StatusForExitCode -ExitCode $finalExit })
    if ($finalExit -ne $baseExit) {
        $reasonCode = Get-ReasonForExitCode -ExitCode $finalExit
    }
    Set-SummaryContract -Summary $summary -StartedAt $startedAt -CompletedAt (Get-UtcTimestamp) -Status $finalStatus -InternalExitCode $finalExit -MandatoryScenarioCount $mandatoryCount -ExecutedScenarioCount ([int]$ledger['executedScenarioCount']) -CorrectnessVerdict $correctnessVerdict -ThresholdVerdict $thresholdVerdict -RegressionVerdict $regressionVerdict -QualityVerdict $qualityVerdict -ArtifactVerdict $(if ($findings.Count -eq 0) { 'PASS' } else { 'BLOCKED' }) -SecretVerdict $(if ($secretFindings -eq 0) { 'PASS' } else { 'FAIL' }) -TeardownVerdict $teardownVerdict -ReasonCode $reasonCode
    Set-SummaryScenarioCounts -Summary $summary -Ledger $ledger
    Set-ObjectProperty -Target $summary -Name 'capacityAcceptanceExecuted' -Value (-not $ImplementationValidationEnabled -and -not $QualificationOnlyEnabled -and $finalExit -eq 0 -and $allScenariosPassed)
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($ImplementationValidationEnabled -or $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($finalExit -eq 0) { 'PASS_WITHIN_FROZEN_PROFILE' } else { Get-StatusForExitCode -ExitCode $finalExit })
    Set-ObjectProperty -Target $summary -Name 'qualificationVerdict' -Value $(if (-not $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($finalExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $finalExit })
    Set-ObjectProperty -Target $summary -Name 'artifactValidationFindings' -Value $findings.ToArray()
    Set-ObjectProperty -Target $summary -Name 'secretFindingCount' -Value $secretFindings
    $summary.finishedAtUtc = $summary.completedAt
    Write-JsonFile -Path $summaryPath -Value $summary
    Write-Utf8File -Path $exitPath -Content "$finalExit`n"
    $packetVerdict = $(
        if ($ImplementationValidationEnabled -or $QualificationOnlyEnabled) { 'NOT_EVALUATED' }
        elseif ($finalExit -eq 80) { 'INVALID' }
        elseif ($finalExit -eq 0) { 'PASS_WITHIN_FROZEN_PROFILE' }
        elseif ($finalExit -in @(40, 50, 60, 70, 90)) { 'FAIL' }
        else { 'BLOCKED' }
    )
    Write-CapacityFinalVerdict -CommitSha $commitSha -StartedAt $startedAt -Verdict $packetVerdict -ReasonCode $reasonCode -IntegrityFindings $findings.ToArray()
    Write-Manifest

    $manifestMismatches = Get-ManifestMismatchCount
    if ($manifestMismatches -gt 0) {
        $finalExit = 80
        $summary.artifactVerdict = 'BLOCKED'
        $summary.internalExitCode = 80
        $summary.exitCode = 80
        $summary.status = 'INVALID'
        $summary.finalStatus = 'INVALID'
        $summary.reasonCode = 'ARTIFACT_VALIDATION_BLOCKED'
        if (-not $ImplementationValidationEnabled -and -not $QualificationOnlyEnabled) {
            $summary.formalAcceptanceVerdict = 'INVALID'
        }
        if ($QualificationOnlyEnabled) {
            $summary.qualificationVerdict = 'BLOCKED'
        }
        $summary.artifactValidationFindings = @($summary.artifactValidationFindings) + @("MANIFEST_MISMATCH_COUNT:$manifestMismatches")
        $summary.completedAt = Get-UtcTimestamp
        $summary.finishedAtUtc = $summary.completedAt
        Write-JsonFile -Path $summaryPath -Value $summary
        Write-Utf8File -Path $exitPath -Content "80`n"
        Write-CapacityFinalVerdict -CommitSha $commitSha -StartedAt $startedAt -Verdict 'INVALID' -ReasonCode 'ARTIFACT_VALIDATION_BLOCKED' -IntegrityFindings @($summary.artifactValidationFindings)
        Write-Manifest
        $manifestMismatches = Get-ManifestMismatchCount
    }
    if ($manifestMismatches -gt 0) { exit 80 }
    exit $finalExit
}

Enter-Qdr7ResolvedPowerShell

if ($Phase -eq 'Preflight') {
    Invoke-Preflight
}
elseif ($Phase -eq 'Finalize') {
    Invoke-Finalize
}
elseif ($Phase -eq 'RuntimeBlockedContractTest') {
    Invoke-RuntimeBlockedContractTest
}
elseif ($Phase -eq 'PartialFinalizerContractTest') {
    Invoke-PartialFinalizerContractTest
}
elseif ($Phase -eq 'FormalPacketGateContractTest') {
    Invoke-FormalPacketGateContractTest
}
elseif ($Phase -eq 'ManifestContractTest') {
    Invoke-ManifestContractTest
}
else {
    if ($RunId -notmatch '^[0-9]{8}T[0-9]{6}Z$' -or $Seed -ne 7 -or -not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        exit 10
    }
    Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json') | Out-Null
    Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | Out-Null
    exit 0
}
