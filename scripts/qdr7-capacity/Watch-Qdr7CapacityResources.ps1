[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string]$RunId,
    [Parameter(Mandatory = $true)] [string]$CommitSha,
    [Parameter(Mandatory = $true)] [string]$EvidenceRoot,
    [Parameter(Mandatory = $true)] [string]$ContainerName,
    [Parameter(Mandatory = $true)] [int]$MavenPid,
    [Parameter(Mandatory = $true)] [string]$StopMarker
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$Utf8NoBom = New-Object Text.UTF8Encoding($false)
$JvmPath = Join-Path $EvidenceRoot 'jvm-series.csv'
$DockerPath = Join-Path $EvidenceRoot 'docker-series.csv'
$PhasePath = Join-Path $EvidenceRoot 'resource-sampling-phase.txt'
$CompletionPath = Join-Path $EvidenceRoot 'resource-sampler-completion.json'
$Started = [Diagnostics.Stopwatch]::StartNew()

function Write-Line {
    param([string]$Path, [string]$Line)
    [IO.File]::AppendAllText($Path, $Line + "`n", $Utf8NoBom)
}

function Get-UtcTimestamp {
    return [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
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

function ConvertTo-ByteCount {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -notmatch '^(?<number>[0-9]+(?:\.[0-9]+)?)\s*(?<unit>B|kB|KB|KiB|MB|MiB|GB|GiB|TB|TiB)$') {
        return $null
    }
    $factor = switch ($Matches['unit']) {
        'B' { 1L }
        'kB' { 1000L }
        'KB' { 1000L }
        'KiB' { 1024L }
        'MB' { 1000000L }
        'MiB' { 1048576L }
        'GB' { 1000000000L }
        'GiB' { 1073741824L }
        'TB' { 1000000000000L }
        'TiB' { 1099511627776L }
    }
    return [long][Math]::Round(([double]$Matches['number']) * $factor, 0, [MidpointRounding]::AwayFromZero)
}

function Get-SamplingPhase {
    if (-not (Test-Path -LiteralPath $PhasePath -PathType Leaf)) {
        return 'HARNESS_BASELINE'
    }
    $phase = ([IO.File]::ReadAllText($PhasePath, $Utf8NoBom)).Trim()
    if ($phase -notmatch '^[A-Z][A-Z0-9_]{2,63}$') {
        return 'INVALID_PHASE'
    }
    return $phase
}

function Get-JavaProcessRows {
    param([string]$Timestamp, [long]$Elapsed, [string]$Phase)
    $rows = New-Object Collections.Generic.List[string]
    $processTable = @(Get-CimInstance Win32_Process | Select-Object ProcessId, ParentProcessId, Name, CommandLine)
    $descendants = New-Object 'Collections.Generic.HashSet[int]'
    [void]$descendants.Add($MavenPid)
    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($candidate in $processTable) {
            if ($descendants.Contains([int]$candidate.ParentProcessId) -and -not $descendants.Contains([int]$candidate.ProcessId)) {
                [void]$descendants.Add([int]$candidate.ProcessId)
                $changed = $true
            }
        }
    }

    foreach ($candidate in $processTable) {
        $pidValue = [int]$candidate.ProcessId
        if (-not $descendants.Contains($pidValue)) {
            continue
        }
        $role = $null
        $commandLine = [string]$candidate.CommandLine
        if ($pidValue -eq $MavenPid -or $commandLine -match 'org\.codehaus\.plexus\.classworlds\.launcher\.Launcher') {
            $role = 'MAVEN'
        }
        elseif ($commandLine -match 'surefirebooter') {
            $role = 'SUREFIRE'
        }
        if ($null -eq $role) {
            continue
        }
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }
        $workingSet = [long]$process.WorkingSet64
        $threads = $process.Threads.Count
        $parentHash = Get-Sha256Text -Value ([string]$candidate.ParentProcessId)
        $rows.Add("qdr7-capacity-1,$RunId,$CommitSha,jvm-resource-sampling,$Timestamp,$Elapsed,$Phase,$role,$pidValue,$parentHash,,,,,,,$workingSet,$threads,")
    }
    return $rows.ToArray()
}

[IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
[IO.File]::WriteAllText($JvmPath, 'schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,processRole,pid,parentPidHash,cpuPercent,heapUsedBytes,heapCommittedBytes,heapMaxBytes,nonHeapBytes,nativeMemoryBytes,workingSetBytes,threadCount,missingReason' + "`n", $Utf8NoBom)
[IO.File]::WriteAllText($DockerPath, 'schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,containerHash,cpuPercent,memoryBytes,hostAvailableBytes,missingReason' + "`n", $Utf8NoBom)

while (-not (Test-Path -LiteralPath $StopMarker)) {
    $timestamp = Get-UtcTimestamp
    $elapsed = $Started.ElapsedMilliseconds
    $phase = Get-SamplingPhase
    $hostAvailable = ''
    try {
        $hostAvailable = [long](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory * 1024L
    }
    catch {
        $hostAvailable = ''
    }

    try {
        $jvmRows = @(Get-JavaProcessRows -Timestamp $timestamp -Elapsed $elapsed -Phase $phase)
        if ($jvmRows.Count -eq 0) {
            Write-Line -Path $JvmPath -Line "qdr7-capacity-1,$RunId,$CommitSha,jvm-resource-sampling,$timestamp,$elapsed,$phase,UNAVAILABLE,$MavenPid,,,,,,,,,,REQUIRED_JVM_PROCESS_UNAVAILABLE"
        }
        else {
            foreach ($row in $jvmRows) {
                Write-Line -Path $JvmPath -Line $row
            }
        }
    }
    catch {
        Write-Line -Path $JvmPath -Line "qdr7-capacity-1,$RunId,$CommitSha,jvm-resource-sampling,$timestamp,$elapsed,$phase,UNAVAILABLE,$MavenPid,,,,,,,,,,JVM_SAMPLER_FAILURE"
    }

    $containerHash = ''
    $dockerMemory = ''
    $dockerCpu = ''
    $missing = 'CONTAINER_NOT_STARTED'
    try {
        $containerId = (& docker container inspect --format '{{.Id}}' $ContainerName 2>$null | Out-String).Trim()
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($containerId)) {
            $containerHash = Get-Sha256Text -Value $containerId
            $stats = (& docker stats --no-stream --format '{{.CPUPerc}}|{{.MemUsage}}' $ContainerName 2>$null | Out-String).Trim()
            if ($LASTEXITCODE -eq 0 -and $stats -match '^(?<cpu>[0-9.]+)%\|(?<memory>[^/]+)\s*/') {
                $dockerCpu = $Matches['cpu']
                $normalized = ConvertTo-ByteCount -Value $Matches['memory'].Trim()
                if ($null -ne $normalized -and $normalized -gt 0L -and -not [string]::IsNullOrWhiteSpace([string]$hostAvailable)) {
                    $dockerMemory = [string]$normalized
                    $missing = ''
                }
                else {
                    $missing = 'DOCKER_OR_HOST_MEMORY_UNAVAILABLE'
                }
            }
            else {
                $missing = 'DOCKER_STATS_UNAVAILABLE'
            }
        }
    }
    catch {
        $missing = 'DOCKER_TEMPORARILY_UNREADABLE'
    }
    Write-Line -Path $DockerPath -Line "qdr7-capacity-1,$RunId,$CommitSha,docker-host-resource-sampling,$timestamp,$elapsed,$phase,$containerHash,$dockerCpu,$dockerMemory,$hostAvailable,$missing"
    Start-Sleep -Seconds 1
}

$finishedAt = Get-UtcTimestamp
$jvmRows = @(Import-Csv -LiteralPath $JvmPath)
$dockerRows = @(Import-Csv -LiteralPath $DockerPath)
$jvmCriticalMissing = @(
    $jvmRows | Where-Object {
        $_.phase -in @('HARNESS_BASELINE', 'FULL_REGRESSION', 'POST_REGRESSION') -and
        -not [string]::IsNullOrWhiteSpace([string]$_.missingReason)
    }
).Count
$dockerCriticalMissing = @(
    $dockerRows | Where-Object {
        $_.phase -in @('FULL_REGRESSION', 'POST_REGRESSION') -and
        -not [string]::IsNullOrWhiteSpace([string]$_.missingReason)
    }
).Count
$completionStatus = $(
    if ($jvmRows.Count -gt 0 -and $dockerRows.Count -gt 0 -and
        $jvmCriticalMissing -eq 0 -and $dockerCriticalMissing -eq 0) {
        'COMPLETED'
    }
    else {
        'FAILED'
    }
)
$completion = [ordered]@{
    schemaVersion = 'qdr7-capacity-resource-sampler-completion-2'
    runId = $RunId
    commitSha = $CommitSha
    status = $completionStatus
    startedAtUtc = [DateTime]::UtcNow.Subtract($Started.Elapsed).ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
    finishedAtUtc = $finishedAt
    elapsedMs = [long]$Started.ElapsedMilliseconds
    jvmRowCount = $jvmRows.Count
    dockerRowCount = $dockerRows.Count
    jvmCriticalMissingCount = $jvmCriticalMissing
    dockerCriticalMissingCount = $dockerCriticalMissing
    mavenRowCount = @($jvmRows | Where-Object { $_.processRole -eq 'MAVEN' }).Count
    surefireRowCount = @($jvmRows | Where-Object { $_.processRole -eq 'SUREFIRE' }).Count
    fullRegressionMavenSampleCount = @($jvmRows | Where-Object { $_.phase -eq 'FULL_REGRESSION' -and $_.processRole -eq 'MAVEN' } | Select-Object -ExpandProperty elapsedMs -Unique).Count
    fullRegressionSurefireSampleCount = @($jvmRows | Where-Object { $_.phase -eq 'FULL_REGRESSION' -and $_.processRole -eq 'SUREFIRE' } | Select-Object -ExpandProperty elapsedMs -Unique).Count
    jvmLastElapsedMs = $(if ($jvmRows.Count -gt 0) { [long]$jvmRows[-1].elapsedMs } else { -1L })
    dockerLastElapsedMs = $(if ($dockerRows.Count -gt 0) { [long]$dockerRows[-1].elapsedMs } else { -1L })
    jvmSeriesSha256 = Get-Sha256File -Path $JvmPath
    dockerSeriesSha256 = Get-Sha256File -Path $DockerPath
}
$completionTemp = $CompletionPath + '.tmp'
[IO.File]::WriteAllText($completionTemp, ($completion | ConvertTo-Json -Depth 10) + "`n", $Utf8NoBom)
Move-Item -LiteralPath $completionTemp -Destination $CompletionPath -Force
