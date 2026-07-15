# DH Stage-QDR-7 B2 Formal Capacity Acceptance Result

> task: `DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY`
> date: `2026-07-15`
> repository: `E:/Project/decision-hub`
> branch: `dev`
> baseline SHA: `1d97e8549fb20d00d26a2890800c730071104c6b`
> run ID: `20260715T140521Z`
> final verdict: `BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT`

## Follow-up（2026-07-15，非formal retry）

本文件以下原始formal结论保持不变。后续任务`DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-RUNTIME-BLOCKER`已关闭Maven/PowerShell binding缺陷；run `20260715T145836Z`仅作为binding validation进入preflight，并按冻结环境合同返回`BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED / internal exit 10 / 0 OF 15`。下一任务为`DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY-2`；不得把该follow-up改写为本文件formal run已通过。

## 1. 结论

```text
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
FORMAL_HARNESS: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT
ENVIRONMENT_PREFLIGHT: NOT_EXECUTED
MANDATORY_SCENARIOS: BLOCKED / 0 OF 15 EXECUTED
CORRECTNESS_INVARIANTS: BLOCKED / NOT_EXECUTED
NUMERIC_THRESHOLDS: BLOCKED / NOT_EXECUTED
POSTGRESQL_RECOVERY: BLOCKED / NOT_EXECUTED
FULL_REGRESSION: BLOCKED / FORMAL_REACTOR_INCOMPLETE
QUALITY_GATE: BLOCKED / NOT_REACHED
ARTIFACT_INTEGRITY: BLOCKED / FORMAL_ARTIFACT_ROOT_MISSING
SECRET_SCAN: BLOCKED / NOT_EXECUTED
B2_IMPLEMENTATION_STATUS: ACCEPTED
Stage-QDR-7 B3: NOT_ALLOWED
```

本轮不能形成capacity PASS或FAIL。Maven在`dh-app`的`qdr7-capacity-preflight`入口失败，错误为`The parameter 'executable' is missing or invalid`。PowerShell preflight未启动，Failsafe formal IT、finalizer、manifest和secret scan均未执行，因此没有语义internal exit code，也没有可作为正式结论的summary。

## 2. Scope与基线

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TASK_SCOPE_DESIGN_INVALID: NO
branch: dev
HEAD: 1d97e8549fb20d00d26a2890800c730071104c6b
worktree before task: clean
staged before task: empty
criteria hash: d015a48e92be91b9f6b0a5f73c358405924af15044c809e48d3034ed57973cab
criteria hash match: PASS
```

冻结criteria文件、machine-readable thresholds、POM、harness、Java、测试与config均未修改。

## 3. 执行记录

正式命令：

```powershell
mvn -ntp `
  -Pqdr7-capacity-acceptance `
  -Dqdr7.runId=20260715T140521Z `
  -Dqdr7.seed=7 `
  verify
```

```text
Maven exit code: 1
internal exit code: NOT_AVAILABLE
total time: 09:57 min
reactor: 17 SUCCESS / dh-app FAILURE / decision-hub SKIPPED
failure phase: dh-app / qdr7-capacity-preflight
failure code: CAPACITY_HARNESS_RUNTIME_DEFECT_BLOCKED
evidence root: target/qdr7-capacity-acceptance/20260715T140521Z / MISSING
```

第一次调用因PowerShell native argument未引用，使Maven收到`.seed=7`并在任何lifecycle前失败。该调用没有evidence root，不属于formal harness execution。修正引用后的上述命令是本轮唯一actual formal run；失败后未重跑。

## 4. Environment与代码现实

独立只读preflight在formal run前得到：

| Check | Result |
|---|---|
| Java | `21.0.9` / PASS |
| Maven | `3.9.12` / PASS |
| Docker | `29.6.1` / PASS |
| Docker memory | `24694095872` bytes / PASS |
| PostgreSQL image | cached `postgres:17` / PASS |
| logical CPU | `32` / PASS |
| host available memory | `15495634944` bytes / below frozen `17179869184` |
| `MAVEN_OPTS` | unset / PASS |

formal PowerShell preflight没有执行，因此正式`ENVIRONMENT_PREFLIGHT`只能记为`NOT_EXECUTED`，不能用独立检查替造internal exit `10`。即使executable绑定修复，当前内存样本仍预示后续formal preflight会按冻结合同BLOCKED；不得降低阈值。

profile binding现实：root profile定义`qdr7.powershell.executable=powershell`，`dh-app` profile的Exec引用该属性；实际reactor在`dh-app`解析为无有效executable。下一任务必须在独立修复范围内做effective-POM与actual formal preflight回归，本任务不修改POM。

## 5. Scenario与threshold结果

15个mandatory scenario全部`NOT_EXECUTED`：actual-wiring、rate matrix、cold-start quota、tenant/environment isolation、canonical source fail-closed、nonce race、idempotency lifecycle、tenant-scoped cleanup、PostgreSQL/Hikari contention、same-pool recovery、Spring Context restart、persistent-volume restart、post-recovery concurrency、full regression scenario与quality gate。

rate throughput、p50、p95、p99、max latency、cleanup duration/convergence、Hikari、PostgreSQL、recovery、JVM/Maven/Surefire、Docker与environment headroom均没有formal measurement，不得复用旧run替代。

Maven失败前刷新了`172`份Surefire reports、`1133` tests、`0 failures / 0 errors / 0 skipped`。该数据仅说明前置普通测试通过；由于`dh-app`失败且root module skipped，不能写成19/19 full regression PASS，quality scenario也未执行。

## 6. Artifact完整性

```text
capacity-acceptance-summary.json: MISSING
threshold-comparison.json: MISSING
sha256-manifest.txt: MISSING
secret-scan.json: MISSING
harness-exit-code.txt: MISSING
manifest mismatch: NOT_EVALUABLE
secret findings: NOT_EVALUABLE
teardown verdict: NOT_APPLICABLE / REGISTERED RESOURCES NEVER CREATED
```

Artifact缺失来自PowerShell preflight未启动，分类为harness runtime blocker，不是capacity threshold failure。

## 7. Boundary confirmation

- 未修改NQ、Java生产代码、Java测试、POM、harness脚本、criteria、application配置、migration/callback或API/contracts。
- 未访问外部HTTP，未接真实Provider、NQ、Agent或LangGraph。
- 未开启Paper或LIVE，未进入Stage-QDR-7 B3 implementation。
- 未push、未创建tag、未stage或commit `target/**`。

## 8. Readiness decision

```text
ALLOW_STAGE_QDR_7_B3_ENTRY: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一任务：

```text
DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-RUNTIME-BLOCKER
```

该任务只能修复并复核formal profile的PowerShell executable runtime binding；完成后仍需另起formal acceptance retry，不能直接声明capacity PASS或进入B3。
