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
$Started = [Diagnostics.Stopwatch]::StartNew()

function Write-Line {
    param([string]$Path, [string]$Line)
    [IO.File]::AppendAllText($Path, $Line + "`n", $Utf8NoBom)
}

function Get-UtcTimestamp {
    return [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ', [Globalization.CultureInfo]::InvariantCulture)
}

[IO.Directory]::CreateDirectory($EvidenceRoot) | Out-Null
[IO.File]::WriteAllText($JvmPath, 'schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,pid,parentPidHash,cpuPercent,heapUsedBytes,heapCommittedBytes,heapMaxBytes,nonHeapBytes,nativeMemoryBytes,workingSetBytes,threadCount,missingReason' + "`n", $Utf8NoBom)
[IO.File]::WriteAllText($DockerPath, 'schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,containerHash,cpuPercent,memoryBytes,hostAvailableBytes,missingReason' + "`n", $Utf8NoBom)

while (-not (Test-Path -LiteralPath $StopMarker)) {
    $timestamp = Get-UtcTimestamp
    $elapsed = $Started.ElapsedMilliseconds
    $hostAvailable = ''
    try {
        $hostAvailable = [long](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory * 1024L
    }
    catch {
        $hostAvailable = ''
    }

    $jvm = Get-Process -Id $MavenPid -ErrorAction SilentlyContinue
    if ($null -eq $jvm) {
        Write-Line -Path $JvmPath -Line "qdr7-capacity-1,$RunId,$CommitSha,jvm-resource-sampling,$timestamp,$elapsed,$MavenPid,,,,,,,,,,PID_EXITED"
    }
    else {
        $workingSet = [long]$jvm.WorkingSet64
        $threads = $jvm.Threads.Count
        Write-Line -Path $JvmPath -Line "qdr7-capacity-1,$RunId,$CommitSha,jvm-resource-sampling,$timestamp,$elapsed,$MavenPid,,,,,,,,,$workingSet,$threads,JVM_HEAP_NOT_AVAILABLE_FROM_HOST_SAMPLER"
    }

    $containerHash = ''
    $dockerMemory = ''
    $dockerCpu = ''
    $missing = 'CONTAINER_NOT_STARTED'
    try {
        $containerId = (& docker container inspect --format '{{.Id}}' $ContainerName 2>$null | Out-String).Trim()
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($containerId)) {
            $sha = [Security.Cryptography.SHA256]::Create()
            try {
                $containerHash = (($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($containerId)) | ForEach-Object { $_.ToString('x2') }) -join '')
            }
            finally {
                $sha.Dispose()
            }
            $stats = (& docker stats --no-stream --format '{{.CPUPerc}}|{{.MemUsage}}' $ContainerName 2>$null | Out-String).Trim()
            if ($LASTEXITCODE -eq 0 -and $stats -match '^(?<cpu>[0-9.]+)%\|(?<memory>[^/]+)\s*/') {
                $dockerCpu = $Matches['cpu']
                $dockerMemory = ''
                $missing = 'DOCKER_MEMORY_REQUIRES_BYTE_NORMALIZATION'
            }
            else {
                $missing = 'DOCKER_STATS_UNAVAILABLE'
            }
        }
    }
    catch {
        $missing = 'DOCKER_TEMPORARILY_UNREADABLE'
    }
    Write-Line -Path $DockerPath -Line "qdr7-capacity-1,$RunId,$CommitSha,docker-host-resource-sampling,$timestamp,$elapsed,$containerHash,$dockerCpu,$dockerMemory,$hostAvailable,$missing"
    Start-Sleep -Seconds 1
}
