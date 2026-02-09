$ErrorActionPreference = "Stop"

# ===== Encoding Hardening (UTF-8) =====
try { & chcp 65001 > $null } catch { }

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom
# ===== Encoding Hardening End =====


$startTime = Get-Date
Write-Host "== Decision Hub Verify =="
Write-Host ("Start: {0}" -f $startTime.ToString("yyyy-MM-dd HH:mm:ss"))

function Assert-NoBom([string]$path) {
  if (-not (Test-Path $path)) { return }
  $bytes = [System.IO.File]::ReadAllBytes($path)
  if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    throw ("BOM detected: {0} (please re-save as UTF-8 without BOM)" -f $path)
  }
}

function Assert-JsonParsable([string]$path) {
  if (-not (Test-Path $path)) { return }
  $txt = Get-Content -Raw -Encoding UTF8 $path
  try { $null = $txt | ConvertFrom-Json } catch { throw ("Invalid JSON: {0} -> {1}" -f $path, $_.Exception.Message) }
}

try {
  # Fast-fail guards for Codex workflow files
  Assert-NoBom "AGENTS.md"
  Assert-NoBom "docs/codex/WORK_ORDER.md"
  Assert-NoBom "docs/codex/PLAN_QUEUE.json"
  Assert-NoBom "docs/codex/PLAN_CURRENT_POINTER.json"
  Assert-JsonParsable "docs/codex/PLAN_QUEUE.json"
  Assert-JsonParsable "docs/codex/PLAN_CURRENT_POINTER.json"
  # NOTE: activePlanId may change; STATUS parse is best-effort
  $ptr = (Get-Content -Raw -Encoding UTF8 "docs/codex/PLAN_CURRENT_POINTER.json" | ConvertFrom-Json)
  if ($ptr.activePlanId) {
    $statusPath = "docs/codex/plans/{0}/STATUS.json" -f $ptr.activePlanId
    Assert-NoBom $statusPath
    Assert-JsonParsable $statusPath
  }

  mvn verify
  if ($LASTEXITCODE -ne 0) { throw "mvn verify failed with exit code $LASTEXITCODE" }
  $endTime = Get-Date
  $duration = New-TimeSpan -Start $startTime -End $endTime
  Write-Host ("End:   {0}" -f $endTime.ToString("yyyy-MM-dd HH:mm:ss"))
  Write-Host ("Cost:  {0:mm\:ss}" -f $duration)
  Write-Host "== VERIFY PASS =="
  exit 0
}
catch {
  $endTime = Get-Date
  $duration = New-TimeSpan -Start $startTime -End $endTime
  Write-Error ("Verify failed: {0}" -f $_.Exception.Message)
  Write-Host ("End:   {0}" -f $endTime.ToString("yyyy-MM-dd HH:mm:ss"))
  Write-Host ("Cost:  {0:mm\:ss}" -f $duration)
  Write-Host "== VERIFY FAIL =="
  exit 1
}
