[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Preflight', 'Finalize', 'ContractTest', 'RuntimeBlockedContractTest', 'PartialFinalizerContractTest')]
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
    }
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
            $threshold.notEvaluatedCount = 94
            $threshold.notEvaluatedThresholds = 94
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
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($QualificationOnlyEnabled) { 'NOT_EVALUATED' } else { 'BLOCKED' })
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
    Write-Manifest
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
        $sourcePath = Join-Path $ProjectRoot $criteria.sourceDocument
        $criteriaHash = Get-Sha256 -Path $sourcePath
        Add-Check 'criteria-version' $CriteriaVersion ([string]$criteria.criteriaVersion) ($criteria.criteriaVersion -eq $CriteriaVersion)
        Add-Check 'criteria-source-hash' ([string]$criteria.sourceDocumentSha256) $criteriaHash ($criteriaHash -eq $criteria.sourceDocumentSha256)
        foreach ($contract in @('qdr7-capacity-artifacts.schema.json', 'qdr7-capacity-artifact-registry.json', 'qdr7-capacity-scenario-registry.json', 'qdr7-capacity-exit-codes.json', 'qdr7-capacity-secret-patterns.json')) {
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
        Add-Check 'logical-cpu' '>=16' ([string]$logicalCpu) ($logicalCpu -ge 16)
        Add-Check 'available-memory' '>=17179869184 bytes' ([string]$availableMemoryBytes) ($availableMemoryBytes -ge 17179869184L)

        $dockerVersionResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('version', '--format', '{{.Server.Version}}')
        $dockerVersion = $dockerVersionResult.text
        Add-Check 'docker-daemon' 'Docker Engine 29.x' $dockerVersion (($dockerVersionResult.exitCode -eq 0) -and ($dockerVersion -match '^29\.'))
        $dockerMemoryResult = Invoke-NativeCommand -Executable 'docker' -Arguments @('info', '--format', '{{.MemTotal}}')
        $dockerMemoryText = $dockerMemoryResult.text
        $dockerMemory = 0L
        [long]::TryParse($dockerMemoryText, [ref]$dockerMemory) | Out-Null
        Add-Check 'docker-memory' '>=17179869184 bytes' ([string]$dockerMemory) ($dockerMemoryResult.exitCode -eq 0 -and $dockerMemory -ge 17179869184L)
        $imageInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('image', 'inspect', 'postgres:17')
        Add-Check 'postgres-image' 'cached postgres:17' $(if ($imageInspect.exitCode -eq 0) { 'cached' } else { 'missing' }) ($imageInspect.exitCode -eq 0)

        $containerName = "dh-qdr7-capacity-$RunId"
        $volumeName = "dh-qdr7-capacity-$RunId"
        $containerInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('container', 'inspect', $containerName)
        Add-Check 'container-isolation' 'absent before run' $(if ($containerInspect.exitCode -eq 0) { 'exists' } else { 'absent' }) ($containerInspect.exitCode -ne 0)
        $volumeInspect = Invoke-NativeCommand -Executable 'docker' -Arguments @('volume', 'inspect', $volumeName)
        Add-Check 'volume-isolation' 'absent before run' $(if ($volumeInspect.exitCode -eq 0) { 'exists' } else { 'absent' }) ($volumeInspect.exitCode -ne 0)
        $port = Get-FreeLoopbackPort
        Add-Check 'loopback-port' 'available dynamic loopback port' ([string]$port) ($port -gt 0)

        if ($blockers.Count -gt 0) {
            Write-BlockedPreflight -CommitSha $commitSha -StartedAt $started -Checks $checks.ToArray() -Blockers $blockers.ToArray()
            exit 10
        }

        $finished = Get-UtcTimestamp
        $environment = New-Artifact -Scenario 'environment' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $finished
        $environment.os = [ordered]@{ caption = $os.Caption; version = $os.Version; architecture = $env:PROCESSOR_ARCHITECTURE }
        $environment.logicalCpu = $logicalCpu
        $environment.availableMemoryBytes = $availableMemoryBytes
        $environment.totalPhysicalMemoryBytes = [long]$computer.TotalPhysicalMemory
        $environment.javaVersion = $javaVersion
        $environment.mavenVersion = $mavenVersion
        $environment.dockerVersion = $dockerVersion
        $environment.dockerMemoryBytes = $dockerMemory
        $environment.postgresImage = 'postgres:17'
        $environment.testcontainersVersion = '1.20.4'
        $environment.criteriaSourceSha256 = $criteriaHash
        $environment.powerShellExecutable = $ResolvedPowerShellIdentity
        Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment

        $preflight = New-Artifact -Scenario 'environment-preflight' -Status 'PASS' -CommitSha $commitSha -StartedAt $started -FinishedAt $finished
        $preflight.checks = $checks.ToArray()
        $preflight.blockerCode = $null
        $preflight.blockers = @()
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
        foreach ($field in @('schemaVersion', 'runId', 'commitSha', 'scenario', 'status', 'startedAtUtc', 'finishedAtUtc', 'durationMs', 'seed', 'unitSystem', 'missingValues', 'criteriaVersion')) {
            if ($null -eq $artifact.PSObject.Properties[$field]) {
                return $false
            }
        }
        return ($artifact.schemaVersion -eq $SchemaVersion -and $artifact.runId -eq $RunId -and $artifact.commitSha -eq $CommitSha -and $artifact.criteriaVersion -eq $CriteriaVersion -and $artifact.seed -eq 7 -and @('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN', 'NOT_FORMAL') -contains $artifact.status)
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
    foreach ($line in Get-Content -LiteralPath $manifestPath) {
        $parts = $line -split '  ', 2
        if ($parts.Count -ne 2) {
            $mismatches++
            continue
        }
        $target = [IO.Path]::GetFullPath((Join-Path $EvidenceRoot $parts[1]))
        if (-not $target.StartsWith($EvidenceRoot, [StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $target -PathType Leaf) -or (Get-Sha256 -Path $target) -ne $parts[0]) {
            $mismatches++
        }
    }
    return $mismatches
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
    $threshold.notEvaluatedCount = 89
    $threshold.notEvaluatedThresholds = 89
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
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($ImplementationValidationEnabled -or $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($baseExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $baseExit })
    Set-ObjectProperty -Target $summary -Name 'qualificationVerdict' -Value $(if (-not $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($baseExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $baseExit })
    Set-ObjectProperty -Target $summary -Name 'artifactValidationFindings' -Value @()
    Set-ObjectProperty -Target $summary -Name 'secretFindingCount' -Value 0
    Set-ObjectProperty -Target $summary -Name 'teardown' -Value $registry.teardown
    Set-ObjectProperty -Target $summary -Name 'scenarioStatuses' -Value $ledger['scenarios']
    $summary.finishedAtUtc = $completedAt
    Write-JsonFile -Path $summaryPath -Value $summary
    Write-Utf8File -Path $exitPath -Content "$baseExit`n"

    $secretFindings = Invoke-SecretScan -CommitSha $commitSha
    $findings = New-Object Collections.Generic.List[string]
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
        if ($baseExit -eq 0 -and -not $implementationValidationPassed -and ($threshold.status -ne 'PASS' -or [int]$threshold.comparisonCount -ne 94 -or [int]$threshold.notEvaluatedCount -ne 0)) {
            $findings.Add('THRESHOLD_COMPLETE_CONTRACT_INVALID')
        }
    }

    if ($QualificationOnlyEnabled -and $baseExit -eq 0) {
        if ($summary.status -ne 'NOT_FORMAL' -or $summary.reasonCode -ne 'QUALIFICATION_ONLY' -or $summary.capacityAcceptanceExecuted -ne $false -or $summary.formalAcceptanceVerdict -ne 'NOT_EVALUATED' -or $summary.qualificationVerdict -ne 'PASS') {
            $findings.Add('QUALIFICATION_SUMMARY_CONTRACT_INVALID')
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
    Set-ObjectProperty -Target $summary -Name 'formalAcceptanceVerdict' -Value $(if ($ImplementationValidationEnabled -or $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($finalExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $finalExit })
    Set-ObjectProperty -Target $summary -Name 'qualificationVerdict' -Value $(if (-not $QualificationOnlyEnabled) { 'NOT_EVALUATED' } elseif ($finalExit -eq 0) { 'PASS' } else { Get-StatusForExitCode -ExitCode $finalExit })
    Set-ObjectProperty -Target $summary -Name 'artifactValidationFindings' -Value $findings.ToArray()
    Set-ObjectProperty -Target $summary -Name 'secretFindingCount' -Value $secretFindings
    $summary.finishedAtUtc = $summary.completedAt
    Write-JsonFile -Path $summaryPath -Value $summary
    Write-Utf8File -Path $exitPath -Content "$finalExit`n"
    Write-Manifest

    $manifestMismatches = Get-ManifestMismatchCount
    if ($manifestMismatches -gt 0) {
        $finalExit = 80
        $summary.artifactVerdict = 'BLOCKED'
        $summary.internalExitCode = 80
        $summary.exitCode = 80
        $summary.status = 'BLOCKED'
        $summary.finalStatus = 'BLOCKED'
        $summary.reasonCode = 'ARTIFACT_VALIDATION_BLOCKED'
        if ($QualificationOnlyEnabled) {
            $summary.qualificationVerdict = 'BLOCKED'
        }
        $summary.artifactValidationFindings = @($summary.artifactValidationFindings) + @("MANIFEST_MISMATCH_COUNT:$manifestMismatches")
        $summary.completedAt = Get-UtcTimestamp
        $summary.finishedAtUtc = $summary.completedAt
        Write-JsonFile -Path $summaryPath -Value $summary
        Write-Utf8File -Path $exitPath -Content "80`n"
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
else {
    if ($RunId -notmatch '^[0-9]{8}T[0-9]{6}Z$' -or $Seed -ne 7 -or -not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        exit 10
    }
    Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json') | Out-Null
    Read-JsonFile -Path (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | Out-Null
    exit 0
}
