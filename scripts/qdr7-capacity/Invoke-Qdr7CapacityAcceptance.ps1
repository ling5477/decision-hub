[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Preflight', 'Finalize', 'ContractTest', 'RuntimeBlockedContractTest')]
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

    [switch]$PowerShellResolved
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$SchemaVersion = 'qdr7-capacity-1'
$CriteriaVersion = 'qdr7-capacity-criteria-1'
$UnitSystem = 'milliseconds-bytes-requestsPerSecond-percent-rfc3339-utc'
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
$EvidenceBase = [IO.Path]::GetFullPath((Join-Path $ProjectRoot 'target\qdr7-capacity-acceptance'))
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
    Set-ObjectProperty -Target $Summary -Name 'startedAt' -Value $StartedAt
    Set-ObjectProperty -Target $Summary -Name 'completedAt' -Value $CompletedAt
    Set-ObjectProperty -Target $Summary -Name 'status' -Value $Status
    Set-ObjectProperty -Target $Summary -Name 'internalExitCode' -Value $InternalExitCode
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenarioCount' -Value $MandatoryScenarioCount
    Set-ObjectProperty -Target $Summary -Name 'executedScenarioCount' -Value $ExecutedScenarioCount
    Set-ObjectProperty -Target $Summary -Name 'correctnessVerdict' -Value $CorrectnessVerdict
    Set-ObjectProperty -Target $Summary -Name 'thresholdVerdict' -Value $ThresholdVerdict
    Set-ObjectProperty -Target $Summary -Name 'regressionVerdict' -Value $RegressionVerdict
    Set-ObjectProperty -Target $Summary -Name 'qualityVerdict' -Value $QualityVerdict
    Set-ObjectProperty -Target $Summary -Name 'artifactVerdict' -Value $ArtifactVerdict
    Set-ObjectProperty -Target $Summary -Name 'secretVerdict' -Value $SecretVerdict
    Set-ObjectProperty -Target $Summary -Name 'teardownVerdict' -Value $TeardownVerdict
    Set-ObjectProperty -Target $Summary -Name 'reasonCode' -Value $ReasonCode
    Set-ObjectProperty -Target $Summary -Name 'finalStatus' -Value $Status
    Set-ObjectProperty -Target $Summary -Name 'exitCode' -Value $InternalExitCode
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenariosTotal' -Value $MandatoryScenarioCount
    Set-ObjectProperty -Target $Summary -Name 'mandatoryScenariosExecuted' -Value $ExecutedScenarioCount
}

function Write-MissingScenarioArtifacts {
    param(
        [string]$CommitSha,
        [string]$StartedAt,
        [string]$Status,
        [string]$ReasonCode
    )
    $finished = Get-UtcTimestamp
    $artifactRegistry = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | ConvertFrom-Json
    foreach ($name in $artifactRegistry.mandatory) {
        $path = Join-Path $EvidenceRoot $name
        if ((Test-Path -LiteralPath $path -PathType Leaf) -or $name -in @('capacity-acceptance-summary.json', 'harness-exit-code.txt', 'secret-scan.json', 'sha256-manifest.txt')) {
            continue
        }
        if ($name -eq 'threshold-comparison.json') {
            $threshold = New-Artifact -Scenario 'threshold-comparison' -Status $Status -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
            $threshold.comparisons = @()
            $threshold.comparisonCount = 0
            $threshold.failedCount = 0
            $threshold.blockedCount = $(if ($Status -eq 'BLOCKED') { 15 } else { 0 })
            $threshold.reason = 'FORMAL_SCENARIO_NOT_EXECUTED'
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
    if ($normalized -in @('AGENTS.md', 'CLAUDE.md', 'README.md', 'pom.xml', 'dh-app/pom.xml')) {
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
    $criteria = Get-Content -Raw -LiteralPath $criteriaPath | ConvertFrom-Json
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

    $scenarioRegistry = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json') | ConvertFrom-Json
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
        $staged = Get-GitValue -Arguments @('diff', '--cached', '--name-only')
        Add-Check 'git-branch' 'dev' $branch ($branch -eq 'dev')
        Add-Check 'git-head' '40-character SHA-1' $commitSha ($commitSha -match '^[a-f0-9]{40}$')
        $implementationScopeValid = $ImplementationValidationEnabled -and @($statusPaths | Where-Object { -not (Test-ImplementationValidationPath -Path $_) }).Count -eq 0
        $worktreeValid = [string]::IsNullOrWhiteSpace($status) -or $implementationScopeValid
        Add-Check 'git-worktree' $(if ($ImplementationValidationEnabled) { 'clean or implementation-validation write allowlist only' } else { 'clean' }) $(if ([string]::IsNullOrWhiteSpace($status)) { 'clean' } elseif ($implementationScopeValid) { 'implementation-validation allowlist only' } else { 'dirty outside allowed scope' }) $worktreeValid
        Add-Check 'git-staged' 'empty' $(if ([string]::IsNullOrWhiteSpace($staged)) { 'empty' } else { 'non-empty' }) ([string]::IsNullOrWhiteSpace($staged))

        $criteriaPath = Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json'
        $criteria = Get-Content -Raw -LiteralPath $criteriaPath | ConvertFrom-Json
        $sourcePath = Join-Path $ProjectRoot $criteria.sourceDocument
        $criteriaHash = Get-Sha256 -Path $sourcePath
        Add-Check 'criteria-version' $CriteriaVersion ([string]$criteria.criteriaVersion) ($criteria.criteriaVersion -eq $CriteriaVersion)
        Add-Check 'criteria-source-hash' ([string]$criteria.sourceDocumentSha256) $criteriaHash ($criteriaHash -eq $criteria.sourceDocumentSha256)
        foreach ($contract in @('qdr7-capacity-artifacts.schema.json', 'qdr7-capacity-artifact-registry.json', 'qdr7-capacity-scenario-registry.json', 'qdr7-capacity-exit-codes.json', 'qdr7-capacity-secret-patterns.json')) {
            $contractPath = Join-Path $ConfigRoot $contract
            $contractValid = $false
            try {
                Get-Content -Raw -LiteralPath $contractPath | ConvertFrom-Json | Out-Null
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
        $registry.teardown = [ordered]@{ sampler = 'PENDING'; container = 'PENDING'; volume = 'PENDING'; residual = 'PENDING' }
        Write-JsonFile -Path $RegistryPath -Value $registry

        $commands = @(
            '1 | ' + $started + ' | . | mvn -ntp -Pqdr7-capacity-acceptance -Dqdr7.runId=<UTC_RUN_ID> -Dqdr7.seed=7 verify | STARTED',
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
        $artifact = Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
        foreach ($field in @('schemaVersion', 'runId', 'commitSha', 'scenario', 'status', 'startedAtUtc', 'finishedAtUtc', 'durationMs', 'seed', 'unitSystem', 'missingValues', 'criteriaVersion')) {
            if ($null -eq $artifact.PSObject.Properties[$field]) {
                return $false
            }
        }
        return ($artifact.schemaVersion -eq $SchemaVersion -and $artifact.runId -eq $RunId -and $artifact.commitSha -eq $CommitSha -and $artifact.criteriaVersion -eq $CriteriaVersion -and $artifact.seed -eq 7 -and @('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN') -contains $artifact.status)
    }
    catch {
        return $false
    }
}

function Stop-RegisteredResources {
    param([object]$Registry, [string]$CommitSha)
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
    $patternsConfig = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-secret-patterns.json') | ConvertFrom-Json
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

function Invoke-Finalize {
    if (-not (Test-Path -LiteralPath $RegistryPath)) {
        exit 80
    }
    $registry = Get-Content -Raw -LiteralPath $RegistryPath | ConvertFrom-Json
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
    $registry = Get-Content -Raw -LiteralPath $RegistryPath | ConvertFrom-Json
    $artifactRegistry = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | ConvertFrom-Json
    $scenarioRegistry = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-scenario-registry.json') | ConvertFrom-Json
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

    $implementationArtifact = Join-Path $EvidenceRoot 'implementation-validation.json'
    $implementationValidationPassed = $ImplementationValidationEnabled -and (Test-Path -LiteralPath $implementationArtifact -PathType Leaf) -and $hasRecordedExit -and $existingExit -eq 0
    if ($implementationValidationPassed) {
        $baseExit = 0
        $reasonCode = 'IMPLEMENTATION_VALIDATION_ONLY'
        $placeholderStatus = 'NOT_RUN'
        $capacityStatus = 'NOT_RUN'
        $verdict = 'NOT_RUN'
    }
    elseif (-not $hasRecordedExit) {
        $baseExit = 20
        $reasonCode = 'APPLICATION_CONTEXT_STARTUP_BLOCKED'
        $placeholderStatus = 'BLOCKED'
        $capacityStatus = 'BLOCKED'
        $verdict = 'BLOCKED'
    }
    else {
        $baseExit = $existingExit
        $reasonCode = $(if ($baseExit -eq 0) { 'FORMAL_CAPACITY_ACCEPTANCE_COMPLETED' } elseif ($baseExit -eq 20) { 'APPLICATION_CONTEXT_STARTUP_BLOCKED' } else { 'FORMAL_CAPACITY_ACCEPTANCE_BLOCKED_OR_FAILED' })
        $placeholderStatus = $(if ($baseExit -eq 0) { 'NOT_RUN' } else { 'BLOCKED' })
        $capacityStatus = $(if ($baseExit -eq 0) { 'PASS' } elseif ($baseExit -in @(40, 50, 60, 70, 90)) { 'FAIL' } else { 'BLOCKED' })
        $verdict = $capacityStatus
    }

    if ($baseExit -ne 0 -or $implementationValidationPassed) {
        Write-MissingScenarioArtifacts -CommitSha $commitSha -StartedAt $startedAt -Status $placeholderStatus -ReasonCode $reasonCode
    }

    $executedScenarioCount = 0
    if ($capacityStatus -eq 'PASS') {
        $executedScenarioCount = $mandatoryCount
    }
    $summaryPath = Join-Path $EvidenceRoot 'capacity-acceptance-summary.json'
    if (Test-Path -LiteralPath $summaryPath) {
        $summary = Get-Content -Raw -LiteralPath $summaryPath | ConvertFrom-Json
    }
    else {
        $now = Get-UtcTimestamp
        $summary = New-Artifact -Scenario 'capacity-acceptance' -Status 'BLOCKED' -CommitSha $commitSha -StartedAt $now -FinishedAt $now
    }
    $completedAt = Get-UtcTimestamp
    $teardownVerdict = $(if ($teardownFindings.Count -eq 0 -and $registry.teardown.residual -eq 'NONE') { 'PASS' } else { 'BLOCKED' })
    Set-SummaryContract -Summary $summary -StartedAt $startedAt -CompletedAt $completedAt -Status $capacityStatus -InternalExitCode $baseExit -MandatoryScenarioCount $mandatoryCount -ExecutedScenarioCount $executedScenarioCount -CorrectnessVerdict $verdict -ThresholdVerdict $verdict -RegressionVerdict $verdict -QualityVerdict $verdict -ArtifactVerdict 'PENDING' -SecretVerdict 'PENDING' -TeardownVerdict $teardownVerdict -ReasonCode $reasonCode
    Set-ObjectProperty -Target $summary -Name 'firstBlocker' -Value $(if ($capacityStatus -eq 'PASS' -or $implementationValidationPassed) { $null } else { 'CAPACITY_HARNESS_RUNTIME_DEFECT' })
    Set-ObjectProperty -Target $summary -Name 'capacityAcceptanceExecuted' -Value ($capacityStatus -eq 'PASS')
    Set-ObjectProperty -Target $summary -Name 'artifactValidationFindings' -Value @()
    Set-ObjectProperty -Target $summary -Name 'secretFindingCount' -Value 0
    Set-ObjectProperty -Target $summary -Name 'teardown' -Value $registry.teardown
    Set-ObjectProperty -Target $summary -Name 'scenarioStatuses' -Value @($scenarioRegistry.fixedOrder | ForEach-Object { [ordered]@{ scenarioId = $_; status = $(if ($capacityStatus -eq 'PASS') { 'PASS' } else { 'NOT_RUN' }); mandatory = $true } })
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
    $summary = Get-Content -Raw -LiteralPath $summaryPath | ConvertFrom-Json
    foreach ($field in $artifactRegistry.summaryRequired) {
        if ($null -eq $summary.PSObject.Properties[$field]) {
            $findings.Add("SUMMARY_REQUIRED_FIELD_MISSING:$field")
        }
    }
    $thresholdPath = Join-Path $EvidenceRoot 'threshold-comparison.json'
    if ($baseExit -ne 0 -and (Test-Path -LiteralPath $thresholdPath -PathType Leaf)) {
        $threshold = Get-Content -Raw -LiteralPath $thresholdPath | ConvertFrom-Json
        if ($threshold.status -ne 'BLOCKED' -or $threshold.reason -ne 'FORMAL_SCENARIO_NOT_EXECUTED' -or @($threshold.comparisons).Count -ne 0) {
            $findings.Add('THRESHOLD_COMPARISON_BLOCKED_CONTRACT_INVALID')
        }
    }

    $finalExit = $baseExit
    if ($findings.Count -gt 0) {
        $finalExit = 80
    }
    if ($secretFindings -gt 0) {
        $finalExit = 90
    }
    $finalStatus = $(if ($implementationValidationPassed -and $finalExit -eq 0) { 'NOT_RUN' } elseif ($finalExit -eq 0) { 'PASS' } elseif ($finalExit -in @(40, 50, 60, 70, 90)) { 'FAIL' } else { 'BLOCKED' })
    Set-SummaryContract -Summary $summary -StartedAt $startedAt -CompletedAt (Get-UtcTimestamp) -Status $finalStatus -InternalExitCode $finalExit -MandatoryScenarioCount $mandatoryCount -ExecutedScenarioCount $executedScenarioCount -CorrectnessVerdict $(if ($implementationValidationPassed) { 'NOT_RUN' } else { $verdict }) -ThresholdVerdict $(if ($implementationValidationPassed) { 'NOT_RUN' } else { $verdict }) -RegressionVerdict $(if ($implementationValidationPassed) { 'NOT_RUN' } else { $verdict }) -QualityVerdict $(if ($implementationValidationPassed) { 'NOT_RUN' } else { $verdict }) -ArtifactVerdict $(if ($findings.Count -eq 0) { 'PASS' } else { 'BLOCKED' }) -SecretVerdict $(if ($secretFindings -eq 0) { 'PASS' } else { 'FAIL' }) -TeardownVerdict $teardownVerdict -ReasonCode $reasonCode
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
else {
    if ($RunId -notmatch '^[0-9]{8}T[0-9]{6}Z$' -or $Seed -ne 7 -or -not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        exit 10
    }
    Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json') | ConvertFrom-Json | Out-Null
    Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | ConvertFrom-Json | Out-Null
    exit 0
}
