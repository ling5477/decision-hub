# DH Shadow report proof hash P1 remediation evidence（attempt-02）

```text
task=NQ_DH_SHADOW_REPORT_PROOF_HASH_P1_REMEDIATION
pass=DH_ONLY
finding=DH-JAVA-REVIEW-001-R1
finding_state=REMEDIATED / PENDING_INDEPENDENT_REVIEW
candidate_base_head=42fa15a2b373786d515796f910eb078233929159
attempt_01_report_hash_claim=INVALIDATED_BY_INDEPENDENT_REVIEW
attempt_01_immutable=YES
attempt_01_sha256=ccca05ffbf6a50602a6adcf216f7266b339d64815c9dbcf727222d6d439212da
report_proof_algorithm=shadow-report-proof-v1
report_proof_sha256=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd
baseline_deterministic_content_sha256=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd
remote_ci=REMOTE_CI_NOT_RUN
```

## 1. Forward-only说明

`DH-JAVA-REVIEW-001.attempt-01.md` 中的 `c37e2e41...` normalized report hash无法由仓库代码独立复现，该 claim已由独立审查否定。attempt-01保持原字节与原 SHA-256，不回写、不声称历史 claim原本正确。本 attempt-02只提供向前修复证据。

## 2. Existing deterministic hash audit

现有 `shadow-baseline.json.deterministic_content_sha256`已经表达稳定 finding projection：

```text
included finding fields = rule_id, path, classification, fingerprint
finding identity = rule_id + path + fingerprint
excluded top-level volatility = generated_at_utc, report_artifact, baseline head, platform/runtime fields
excluded display/derived fields = symbol_or_line, summary, severity, architecture_scope,
                                  is_in_baseline, is_current_task, checker/ruleset/configuration metadata
```

因此本轮不创建第二套近义 baseline hash，不修改 scanner report schema，也不修改 baseline finding projection。`shadow-report-proof-v1`是现有 `deterministic_content_sha256`的可执行 report proof合同。

## 3. Canonical report proof contract

```text
algorithm version = shadow-report-proof-v1
input report schema = 2.0.0 or 3.0.0
required array = violations
required count = current_violation_count (2.0.0) / current_count (3.0.0)
included fields/order = rule_id, path, classification, fingerprint
finding ordering = InvariantCulture(rule_id, path, fingerprint, classification)
                   + ordinal tie-breaker
path = repository-relative, '/' separator, no empty/dot/dot-dot segment
JSON = compact array of ordered four-field objects, no trailing newline
encoding = strict UTF-8 without BOM
input EOL = CRLF normalized to LF; bare CR rejected
digest = SHA-256 lowercase hex
duplicate identity = fail closed
invalid schema/count/field/path/JSON/UTF-8 = fail closed
```

Top-level时间和输出路径不进入 proof；finding四字段任一变化、finding增删都会改变 proof。

## 4. Exact reproduction command

```powershell
pwsh -NoProfile -File scripts/java-standard/invoke-java-shadow-scan.ps1 `
  -OutputPath artifacts/java-shadow/shadow-report.json

pwsh -NoProfile -File scripts/java-standard/get-shadow-report-proof-hash.ps1 `
  -ReportPath artifacts/java-shadow/shadow-report.json
```

Expected stable output：

```text
SHADOW_REPORT_PROOF_RESULT=PASS
REPORT_PROOF_ALGORITHM=shadow-report-proof-v1
REPORT_PROOF_SCHEMA=3.0.0
REPORT_PROOF_FINDING_COUNT=11
REPORT_PROOF_SHA256=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd
```

## 5. Permanent regression

```text
same report twice = SAME_HASH
LF vs CRLF = SAME_HASH
generated_at_utc changed = SAME_HASH
report_artifact changed = SAME_HASH
finding order changed = SAME_HASH
schema 2.0.0 vs 3.0.0 equivalent projection = SAME_HASH
rule_id/path/classification/fingerprint mutation = HASH_CHANGED
finding added/removed = HASH_CHANGED
invalid schema = REPORT_PROOF_INVALID / EXIT_2
duplicate finding identity = REPORT_PROOF_INVALID / EXIT_2
```

永久入口：`scripts/java-standard/tests/Test-ShadowReportProofHash.ps1`。

## 6. Windows validation

```text
runtime=PowerShell 7.6.5 / Windows
canonical_contract_test=PASS / EXIT_0
report_proof_contract_test=PASS / EXIT_0
governance_verifier=PASS / EXIT_0
shadow_run_1=VIOLATION_FOUND / EXIT_0 / 11_FINDINGS
shadow_run_2=VIOLATION_FOUND / EXIT_0 / 11_FINDINGS
raw_report_sha256_run_1=6c28a3364551948be814e791d7e66d9c9c6dce9a26ac3f398b4c139e6a8f08c2
raw_report_sha256_run_2=02d154e86d3ea6640dc35cb13dc1e2646f1ee302f3c6d898951756012316c7ce
proof_run_1=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd / EXIT_0
proof_run_2=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd / EXIT_0
raw_reports_differ_because_output_path_differs=YES
proof_hash_equal=YES
```

## 7. Linux/LF validation

真实 WSL `/tmp` disposable Git checkout；不是 Windows bind mount checkout。10个 canonical configuration inputs为 `w/lf`，候选 proof脚本也为 LF-only。

```text
runtime=PowerShell 7.6.4 / Linux / VERIFIED_ARCHIVE_SHA256
canonical_contract_test=PASS / EXIT_0
report_proof_contract_test=PASS / EXIT_0
governance_verifier=PASS / EXIT_0
shadow_run_1=VIOLATION_FOUND / EXIT_0 / 11_FINDINGS
shadow_run_2=VIOLATION_FOUND / EXIT_0 / 11_FINDINGS
raw_report_sha256_run_1=c5f04a04eb8e3b6fc8be3da29cc05f6007c2efd87ce5670e643412daf3c2e524
raw_report_sha256_run_2=0f59f9a7443fd26bf0cfd308f3f9aef194ae604854afa1ab2907d58771804c37
proof_run_1=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd / EXIT_0
proof_run_2=e82bedceb09991fefce8736628c9b87c807c78a9bdd078682e34d3ffe49f4dfd / EXIT_0
windows_linux_proof_hash=EQUAL
```

## 8. Remaining scope

```text
DH-JAVA-REVIEW-001-R1=REMEDIATED / PENDING_INDEPENDENT_REVIEW
REMOTE_CI_NOT_RUN
NOT_ADDRESSED=NQ-JAVA-REVIEW-002,NQ-JAVA-REVIEW-003,NQ-JAVA-REVIEW-004,
              CROSS-JAVA-REVIEW-001,CROSS-JAVA-REVIEW-002
```

本 attempt不关闭 finding，不声明整个 Java Engineering review通过，不授权 merge。
