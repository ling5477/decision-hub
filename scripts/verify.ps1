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

function Assert-TextContains([string]$path, [string]$expected) {
  if (-not (Test-Path $path)) { throw ("Missing required file: {0}" -f $path) }
  $txt = Get-Content -Raw -Encoding UTF8 $path
  if (-not $txt.Contains($expected)) {
    throw ("Missing required marker in {0}: {1}" -f $path, $expected)
  }
}

try {
  # Fast-fail guards for current Decision Hub workflow files.
  Assert-NoBom "AGENTS.md"
  foreach ($p in @(
      "README.md",
      "docs/current/README.md",
      "docs/current/STATUS.md",
      "docs/current/WORK_ORDER.md",
      "docs/current/CODEX_PROJECT_INSTRUCTIONS.md",
      "docs/current/TESTING.md",
      "docs/current/ARCHIVE_INDEX.md"
    )) {
    Assert-NoBom $p
  }

  # Current authority guard: docs/current is the current factsource.
  Assert-TextContains "docs/current/STATUS.md" "STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED"
  Assert-TextContains "docs/current/STATUS.md" "STAGE_QDR_4_TAG: PENDING"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_STAGE_QDR_5_PLAN_NOW: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_REAL_HTTP: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_REAL_PROVIDER: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_AGENT_PHASE: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_LANGGRAPH_RUNTIME: NO"
  Assert-TextContains "docs/current/STATUS.md" "ALLOW_LIVE: NO"
  Assert-TextContains "docs/current/WORK_ORDER.md" "DH-STAGE-QDR-4-TAG-CLOSE"
  Assert-TextContains "docs/current/CODEX_PROJECT_INSTRUCTIONS.md" "docs/current"

  # Historical docs/codex files are JSON sanity checks only, not current workflow authority.
  foreach ($p in @(
      "docs/codex/plans/_active/STATUS.json",
      "docs/codex/PLAN_QUEUE.json",
      "docs/codex/PLAN_CURRENT_POINTER.json"
    )) {
    try {
      Assert-NoBom $p
      Assert-JsonParsable $p
    } catch {
      Write-Warning ("Non-blocking historical docs/codex issue: {0} -> {1}" -f $p, $_.Exception.Message)
    }
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
