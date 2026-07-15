[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Preflight', 'Finalize', 'ContractTest')]
    [string]$Phase,

    [Parameter(Mandatory = $true)]
    [string]$RunId,

    [Parameter(Mandatory = $true)]
    [int]$Seed,

    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot
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

function Get-UtcTimestamp {
    return [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
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

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-GitValue {
    param([string[]]$Arguments)
    $output = & git -C $ProjectRoot @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git command failed: $($Arguments -join ' ')"
    }
    return (($output | Out-String).Trim())
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
    $environment.criteriaSourceSha256 = $null
    $environment.missingValues = @('environmentBaseline')
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'environment.json') -Value $environment
    $preflight = New-Artifact -Scenario 'environment-preflight' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $preflight.checks = $Checks
    $preflight.blockerCode = 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'
    $preflight.blockers = $Blockers
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'preflight.json') -Value $preflight
    $summary = New-Artifact -Scenario 'capacity-acceptance' -Status 'BLOCKED' -CommitSha $CommitSha -StartedAt $StartedAt -FinishedAt $finished
    $summary.finalStatus = 'BLOCKED'
    $summary.exitCode = 10
    $summary.firstBlocker = 'ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED'
    $summary.findings = $Blockers
    $summary.capacityAcceptanceExecuted = $false
    Write-JsonFile -Path (Join-Path $EvidenceRoot 'capacity-acceptance-summary.json') -Value $summary
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'harness-exit-code.txt') -Content "10`n"
    Write-Utf8File -Path (Join-Path $EvidenceRoot 'commands.txt') -Content ("1 | $StartedAt | . | qdr7 capacity preflight | BLOCKED`n")
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

        $branch = Get-GitValue -Arguments @('branch', '--show-current')
        $commitSha = Get-GitValue -Arguments @('rev-parse', 'HEAD')
        $status = Get-GitValue -Arguments @('status', '--porcelain=v1', '--untracked-files=all')
        $staged = Get-GitValue -Arguments @('diff', '--cached', '--name-only')
        Add-Check 'git-branch' 'dev' $branch ($branch -eq 'dev')
        Add-Check 'git-head' '40-character SHA-1' $commitSha ($commitSha -match '^[a-f0-9]{40}$')
        Add-Check 'git-worktree' 'clean' $(if ([string]::IsNullOrWhiteSpace($status)) { 'clean' } else { 'dirty' }) ([string]::IsNullOrWhiteSpace($status))
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

        $javaVersionText = (& java -version 2>&1 | Out-String).Trim()
        $javaMatch = [regex]::Match($javaVersionText, 'version "(?<version>\d+(?:\.\d+)*)')
        $javaVersion = $(if ($javaMatch.Success) { $javaMatch.Groups['version'].Value } else { 'unknown' })
        Add-Check 'java-version' '21.x' $javaVersion ($javaVersion -match '^21(?:\.|$)')
        $mavenVersionText = (& mvn -version 2>&1 | Out-String).Trim()
        $mavenMatch = [regex]::Match($mavenVersionText, 'Apache Maven (?<version>\d+\.\d+\.\d+)')
        $mavenVersion = $(if ($mavenMatch.Success) { $mavenMatch.Groups['version'].Value } else { 'unknown' })
        Add-Check 'maven-version' '3.9.x' $mavenVersion ($mavenVersion -match '^3\.9\.')
        Add-Check 'maven-opts' 'unset' $(if ([string]::IsNullOrEmpty($env:MAVEN_OPTS)) { 'unset' } else { 'set' }) ([string]::IsNullOrEmpty($env:MAVEN_OPTS))

        $os = Get-CimInstance Win32_OperatingSystem
        $computer = Get-CimInstance Win32_ComputerSystem
        $cpu = Get-CimInstance Win32_Processor
        $logicalCpu = [int](($cpu | Measure-Object -Property NumberOfLogicalProcessors -Sum).Sum)
        $availableMemoryBytes = [long]$os.FreePhysicalMemory * 1024L
        Add-Check 'os-family' 'Windows 11 x64' "$($os.Caption) / $env:PROCESSOR_ARCHITECTURE" (($os.Caption -match 'Windows 11') -and ($env:PROCESSOR_ARCHITECTURE -eq 'AMD64'))
        Add-Check 'logical-cpu' '>=16' ([string]$logicalCpu) ($logicalCpu -ge 16)
        Add-Check 'available-memory' '>=17179869184 bytes' ([string]$availableMemoryBytes) ($availableMemoryBytes -ge 17179869184L)

        $dockerVersion = (& docker version --format '{{.Server.Version}}' 2>&1 | Out-String).Trim()
        Add-Check 'docker-daemon' 'Docker Engine 29.x' $dockerVersion (($LASTEXITCODE -eq 0) -and ($dockerVersion -match '^29\.'))
        $dockerMemoryText = (& docker info --format '{{.MemTotal}}' 2>&1 | Out-String).Trim()
        $dockerMemory = 0L
        [long]::TryParse($dockerMemoryText, [ref]$dockerMemory) | Out-Null
        Add-Check 'docker-memory' '>=17179869184 bytes' ([string]$dockerMemory) ($dockerMemory -ge 17179869184L)
        & docker image inspect 'postgres:17' *> $null
        Add-Check 'postgres-image' 'cached postgres:17' $(if ($LASTEXITCODE -eq 0) { 'cached' } else { 'missing' }) ($LASTEXITCODE -eq 0)

        $containerName = "dh-qdr7-capacity-$RunId"
        $volumeName = "dh-qdr7-capacity-$RunId"
        & docker container inspect $containerName *> $null
        Add-Check 'container-isolation' 'absent before run' $(if ($LASTEXITCODE -eq 0) { 'exists' } else { 'absent' }) ($LASTEXITCODE -ne 0)
        & docker volume inspect $volumeName *> $null
        Add-Check 'volume-isolation' 'absent before run' $(if ($LASTEXITCODE -eq 0) { 'exists' } else { 'absent' }) ($LASTEXITCODE -ne 0)
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

    $expected = "dh-qdr7-capacity-$RunId"
    if ($Registry.containerName -ne $expected -or $Registry.volumeName -ne $expected) {
        $findings.Add('RESOURCE_REGISTRY_NAME_MISMATCH')
        return $findings.ToArray()
    }
    & docker container inspect $Registry.containerName *> $null
    if ($LASTEXITCODE -eq 0) {
        & docker container rm --force $Registry.containerName *> $null
        if ($LASTEXITCODE -ne 0) {
            $findings.Add('REGISTERED_CONTAINER_TEARDOWN_FAILED')
        }
    }
    $Registry.teardown.container = $(if ($findings -contains 'REGISTERED_CONTAINER_TEARDOWN_FAILED') { 'FAILED' } else { 'REMOVED_OR_ABSENT' })
    & docker volume inspect $Registry.volumeName *> $null
    if ($LASTEXITCODE -eq 0) {
        & docker volume rm $Registry.volumeName *> $null
        if ($LASTEXITCODE -ne 0) {
            $findings.Add('REGISTERED_VOLUME_TEARDOWN_FAILED')
        }
    }
    $Registry.teardown.volume = $(if ($findings -contains 'REGISTERED_VOLUME_TEARDOWN_FAILED') { 'FAILED' } else { 'REMOVED_OR_ABSENT' })
    & docker container inspect $Registry.containerName *> $null
    $containerResidual = $LASTEXITCODE -eq 0
    & docker volume inspect $Registry.volumeName *> $null
    $volumeResidual = $LASTEXITCODE -eq 0
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
    $files = @(Get-ChildItem -LiteralPath $EvidenceRoot -File | Where-Object { $_.Name -ne 'secret-scan.json' -and $_.Extension -in @('.json', '.csv', '.txt', '.log') })
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

function Invoke-Finalize {
    if (-not (Test-Path -LiteralPath $RegistryPath)) {
        exit 80
    }
    $registry = Get-Content -Raw -LiteralPath $RegistryPath | ConvertFrom-Json
    $commitSha = [string]$registry.commitSha
    $teardownFindings = @(Stop-RegisteredResources -Registry $registry -CommitSha $commitSha)
    $registry = Get-Content -Raw -LiteralPath $RegistryPath | ConvertFrom-Json
    $artifactRegistry = Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | ConvertFrom-Json
    $findings = New-Object Collections.Generic.List[string]
    foreach ($finding in $teardownFindings) {
        $findings.Add([string]$finding)
    }

    foreach ($name in $artifactRegistry.mandatory) {
        if ($name -in @('secret-scan.json', 'sha256-manifest.txt')) {
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

    $secretFindings = Invoke-SecretScan -CommitSha $commitSha

    $existingExit = 0
    $exitPath = Join-Path $EvidenceRoot 'harness-exit-code.txt'
    if (Test-Path -LiteralPath $exitPath) {
        [int]::TryParse((Get-Content -Raw -LiteralPath $exitPath).Trim(), [ref]$existingExit) | Out-Null
    }
    else {
        $findings.Add('HARNESS_EXIT_CODE_MISSING')
    }
    $finalExit = $existingExit
    if ($findings.Count -gt 0) {
        $finalExit = 80
    }
    if ($secretFindings -gt 0) {
        $finalExit = 90
    }

    $summaryPath = Join-Path $EvidenceRoot 'capacity-acceptance-summary.json'
    if (Test-Path -LiteralPath $summaryPath) {
        $summary = Get-Content -Raw -LiteralPath $summaryPath | ConvertFrom-Json
    }
    else {
        $now = Get-UtcTimestamp
        $summary = New-Artifact -Scenario 'capacity-acceptance' -Status 'BLOCKED' -CommitSha $commitSha -StartedAt $now -FinishedAt $now
    }
    $summary.status = $(if ($finalExit -eq 0) { 'PASS' } elseif ($finalExit -in @(40, 50, 60, 70, 90)) { 'FAIL' } else { 'BLOCKED' })
    $summary | Add-Member -NotePropertyName finalStatus -NotePropertyValue $summary.status -Force
    $summary | Add-Member -NotePropertyName exitCode -NotePropertyValue $finalExit -Force
    $summary | Add-Member -NotePropertyName artifactValidationFindings -NotePropertyValue $findings.ToArray() -Force
    $summary | Add-Member -NotePropertyName secretFindingCount -NotePropertyValue $secretFindings -Force
    $summary | Add-Member -NotePropertyName teardown -NotePropertyValue $registry.teardown -Force
    $summary | Add-Member -NotePropertyName capacityAcceptanceExecuted -NotePropertyValue $true -Force
    $summary.finishedAtUtc = Get-UtcTimestamp
    Write-JsonFile -Path $summaryPath -Value $summary
    Write-Utf8File -Path $exitPath -Content "$finalExit`n"
    Write-Manifest
    if (-not (Test-Path -LiteralPath (Join-Path $EvidenceRoot 'sha256-manifest.txt'))) {
        exit 80
    }
    exit $finalExit
}

if ($Phase -eq 'Preflight') {
    Invoke-Preflight
}
elseif ($Phase -eq 'Finalize') {
    Invoke-Finalize
}
else {
    if ($RunId -notmatch '^[0-9]{8}T[0-9]{6}Z$' -or $Seed -ne 7 -or -not $EvidenceRoot.StartsWith($EvidenceBase, [StringComparison]::OrdinalIgnoreCase)) {
        exit 10
    }
    Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-thresholds.json') | ConvertFrom-Json | Out-Null
    Get-Content -Raw -LiteralPath (Join-Path $ConfigRoot 'qdr7-capacity-artifact-registry.json') | ConvertFrom-Json | Out-Null
    exit 0
}
