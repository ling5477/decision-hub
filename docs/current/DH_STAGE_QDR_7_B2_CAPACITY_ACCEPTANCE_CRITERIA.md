# DH Stage-QDR-7 B2 Capacity Acceptance Criteria

> task: `DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE`
> date: `2026-07-13`
> authority status: `BLOCKED / CAPACITY_THRESHOLD_JUSTIFICATION_INSUFFICIENT`
> B2 status: `CLOSED / ACCEPTED`
> post-B2 capacity acceptance: `BLOCKED`
> Stage-QDR-7 B3: `NOT_ALLOWED`

## Current same-pool recovery disposition（2026-07-14）

`DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER`已证明旧probe因随机宿主端口漂移而无效；修正后的固定loopback endpoint在同一ApplicationContext、DataSource、Hikari pool、PostgreSQL container、mapped port、JDBC URL hash与persistent volume上完成3/3恢复。Outage 9个真实请求全部fail-closed，37条序列最大采样间隔1048 ms，恢复后8并发/100请求全部2xx；两类restart分别3/3。完整回归为1114/0/0/0，Surefire 380 rows/7 PIDs，质量门通过。

Mandatory candidate evidence现为`SUFFICIENT`，允许下一独立任务`DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`重新审查并选择是否冻结criteria。本文件在该retry完成前仍保持`BLOCKED / NOT_ACCEPTED / NOT_FROZEN`；本任务不直接冻结阈值、不实现formal harness，也不执行capacity acceptance。Post-B2 capacity acceptance与B3继续禁止。

## Previous threshold evidence retry-2 disposition（historical / consumed，2026-07-14）

`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2`已完成13/13 actual-wiring preflight与15/15 measured rate rounds，但mandatory evidence没有全部通过：cold-start quota三轮各有7个unexpected 503；production cleanup合同不含tenant scope；PostgreSQL临时不可用后同一Hikari pool未恢复且相关时间序列不完整；full regression资源采样未捕获Surefire进程。完整1101项Maven回归和质量门本体通过，不足以补齐这些缺口。

因此本文件当时继续`BLOCKED / NOT_ACCEPTED / NOT_FROZEN`。该轮观测值只能作为localhost calibration input，不得冻结为正式threshold；当时下一任务为`DH-STAGE-QDR-7-B2-CAPACITY-SCENARIO-EVIDENCE-BLOCKER`，现已被顶部current retry disposition取代。

## Previous calibration path blocker closure disposition（historical / consumed，2026-07-13）

`DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER`通过构造期一次性冻结mock baseline profile创建时间，关闭了同一Spring Context重复bootstrap不确定性；stable profile ID、provider/config字段、strict registry mismatch与overwrite prevention均未放宽。同一Context与packaged jar各自完成5次顺序和8次并发protected请求，均为13/13结构化2xx，0个5xx、`UNKNOWN_ERROR`或profile mismatch；PostgreSQL 17.10、完整1101项回归与质量门均通过。

该结果只允许进入`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2`重新采集证据，不把5+8 repeatability样本写成1/2/4/8/16 rate matrix或accepted threshold。Criteria freeze、formal harness、capacity acceptance与B3继续禁止。

## Previous threshold evidence retry disposition（historical / consumed，2026-07-13）

`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY`在exact-HEAD Boot jar上取得1个完整安全链protected `200`，并完成same-nonce race、canonical source fail-closed、Spring Context/PostgreSQL persistent-volume restart、成功full regression与资源采样。但同一Context第二个合法请求因mock provider profile bootstrap mismatch返回`500 / UNKNOWN_ERROR`，20次warm-up只完成1次，1/2/4/8/16 matrix没有有效measured round；cleanup缺并发writer/duration/timeline，PostgreSQL contention缺持续lock-wait/connection-acquire序列。

因此本文件继续`BLOCKED / NOT_ACCEPTED`，且当前不允许criteria freeze retry。完整证据见`DH_STAGE_QDR_7_B2_CAPACITY_THRESHOLD_EVIDENCE.md`；下一任务为`DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER`，不得把single 2xx、nonce rejection latency、correctness fixture或成功full-regression资源基线写成accepted threshold。

## Previous hard-ceiling disposition（historical / consumed，2026-07-13）

`DH-STAGE-QDR-7-B2-GUARD-CONFIGURATION-BYPASS-BLOCKER`已关闭B1 hard-ceiling实现分叉：`PersistentGuardHardCeilings`是properties与command共同引用的唯一数值权威，window `3600`、quota `100000`、lease `900`本身允许，max+1 fail-closed。定向24项、PostgreSQL 41项与完整1091项回归均为0 failures/errors/skipped。

该次关闭只移除criteria的hard-ceiling authority冲突，不提供actual-wiring容量样本；本文件继续`BLOCKED / NOT_ACCEPTED`。当时的下一任务`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY`现已执行并形成文件顶部的新disposition；本段不得覆盖当前next task。

> Historical / consumed：以下§1–§9保留criteria freeze任务当时的证据缺口、OOM样本与阈值判断，不得覆盖文件顶部的current disposition。第10节是current consolidated decision；其中repeatable 2xx、当前full regression与next task字段以本轮实测补正，仍不代表criteria已冻结。

## 1. Freeze decision

```text
CAPACITY_ACCEPTANCE_CRITERIA_FREEZE: BLOCKED
CAPACITY_THRESHOLD_JUSTIFICATION_INSUFFICIENT_BLOCKED
CAPACITY_CRITERIA_AUTHORITY: FAIL
PROJECT_ACCEPTANCE_BASELINE: BLOCKED
NUMERIC_THRESHOLDS: BLOCKED
HARNESS_CONTRACT: BLOCKED
ENVIRONMENT_PREFLIGHT: BLOCKED
FULL_REGRESSION_CONTRACT: PARTIAL / COMMAND_FROZEN / RESOURCE_BASELINE_BLOCKED
```

本文件是本轮唯一新增标准文件，但不是已接受的容量标准。它记录能够从current work order、生产代码、配置、B1证据和既有正确性测试直接追溯的数值，并明确列出无法形成确定阈值的阻断项。缺失的吞吐、延迟、cleanup时限、资源余量和压力轮次数值不得由hard ceiling、无有效2xx样本的旧尝试或单次正确性测试反推。

上一轮`DH_STAGE_QDR_7_B2_POST_IMPLEMENTATION_CAPACITY_ACCEPTANCE.md`的`BLOCKED`结论保持不变。本轮未执行容量测试、targeted PostgreSQL suite或full regression，也未实现harness。

## 2. Scope governance

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
```

本轮只允许修改附件列出的current文档。Java、测试、callback、V1–V14、API/OpenAPI/contracts、NQ、外部HTTP、Provider、Agent、LangGraph、Paper和LIVE全部禁止。

## 3. Authority order and conflict

标准来源顺序固定为：

1. current work order与policy；
2. 当前生产代码、配置和资源上限；
3. Stage-QDR-7 B1 capacity evidence；
4. 具有完整推导链的`PROJECT_ACCEPTANCE_BASELINE`。

### 3.1 Hard-ceiling authority reconciliation

| Item | B1 current contract | Current code validation | Decision |
|---|---:|---:|---|
| rate window | 1–3600 seconds | 1–3600 seconds / shared authority | `ALIGNED` |
| rate quota | 1–100000 per window/key | 1–100000 / shared authority | `ALIGNED` |
| idempotency lease | 1–900 seconds | 1–900 seconds / shared authority | `ALIGNED` |
| idempotency TTL | 1 ms–30 days | greater than 0 and at most 7 days | `CODE_STRICTER / COMPATIBLE_SUBSET` |
| retention | 1 ms–90 days | greater than 0 and at most 90 days | `ALIGNED_AT_UPPER_BOUND` |
| cleanup batch | 1–10000 rows | 1–1000 rows | `CODE_STRICTER / COMPATIBLE_SUBSET` |

`PersistentGuardHardCeilings`由`DecisionDryRunGuardProperties`与`RateLimitAdmissionCommand`共同引用，直接command构造不再接受更宽window/quota。Properties仍没有选择运行默认值，base/dev/test/prod配置也没有提供已启用persistent guard的rate/lease/TTL/retention数值，runtime保持disabled。Authority冲突已关闭，但这些ceiling仍不是容量阈值或运行默认值。

## 4. Traceable numeric evidence

| Evidence | Traceable numeric values | What it proves | What it does not prove |
|---|---|---|---|
| rate atomicity correctness test | 8 threads、40 attempts、quota 10、accepted 10、oversell 0 | 单轮测试负载下原子计数与tenant/environment隔离 | sustained throughput、latency、duration、repeatability |
| idempotency admission correctness test | 8 threads、24 attempts、admitted 1、duplicate winner 0 | 单轮同key CAS winner唯一 | nonce race、lease capacity、tail latency |
| cleanup correctness tests | 2 workers、batch 10；production-wiring fixture包含3 eligible、1 active、1 future和1 locked record | `SKIP LOCKED`、active/ineligible保护与bounded affected rows | concurrent writer pressure、backlog收敛时间、cleanup throughput |
| cleanup command validation | batch 1–1000；safety grace greater than 0 and at most 86400 seconds | 单事务安全上限 | 工程验收batch default与interval |
| migration callback timeout | lock timeout 5 seconds、statement timeout 60 seconds、repair ceiling 1000 rows | Flyway migration callback fail-closed边界 | runtime guard capacity或connection-pool threshold |
| B1 diagnostic matrix | concurrency 1/2/4/8/16；warm-up 20；measured attempts 100 | future harness的结构参考 | 0个有效2xx样本，全部latency/throughput结果被排除 |
| previous environment | host 15.70 GiB、Docker 8165437440 bytes、JVM estimated max heap约3.93 GiB | 两次native-memory OOM发生时的失败环境指纹 | 成功full regression所需最低内存 |

## 5. Mandatory scenario matrix disposition

### A. Persistent rate-limit atomicity

能够直接冻结的correctness seed为：

```text
environment: test
endpoint: /api/ai/decision-dry-runs
source: NQ_DRYRUN
tenant count: 2
thread count: 8
attempt count: 40
quota: 10
expected accepted: 10
oversell tolerance: 0
unexpected error tolerance: 0
```

不能冻结：warm-up、measured rounds、duration、throughput floor、p50/p95/p99/max latency ceiling。B1的20次warm-up和100次attempt结构没有有效2xx样本，不能转化为接受阈值。

### B. Tenant/source isolation

tenant隔离的correctness fixture可追溯到2个tenant；environment隔离可追溯到`test`与`dev`。production identity只允许一个canonical source `NQ_DRYRUN`，因此无法在不修改source合同的前提下构造两个合法source进行noisy-neighbor容量比较。

```text
tenant isolation error tolerance: 0
environment isolation error tolerance: 0
legal source count: 1
cross-source capacity comparison: BLOCKED / CONTRACT_HAS_ONE_CANONICAL_SOURCE
```

### C. Replay nonce concurrent race

JDBC实现使用`INSERT ... ON CONFLICT DO NOTHING`并在store failure时返回拒绝；现有测试只覆盖首次登记、实例重建后replay拒绝、scope隔离和过期清理。仓库没有same-nonce并发race样本，不能借用idempotency的8线程/24 attempts作为nonce阈值。

```text
expected accepted: 1
duplicate acceptance tolerance: 0
store-failure fail-open tolerance: 0
thread count: BLOCKED / NO_TRACEABLE_NONCE_RACE_SAMPLE
attempt count: BLOCKED / NO_TRACEABLE_NONCE_RACE_SAMPLE
measured rounds: BLOCKED / NO_TRACEABLE_NONCE_RACE_SAMPLE
```

### D. Persistent lifecycle and migration callback

Flyway `beforeEachMigrate` callback是migration compatibility callback，不是runtime claim队列。Runtime claim、lease、terminal transition、retry/recovery和commit-unknown由persistent idempotency state machine承担。两者必须在未来harness中分开报告，不得把Flyway 5秒/60秒timeout当作runtime lifecycle容量阈值。

能够冻结的正确性容忍度均为0：双claim、双terminal result、active lease错误接管、commit-unknown readmit、rollback残留。Claim并发量、lease pressure轮次、terminal throughput与latency没有有效测量依据。

### E. Cleanup under load

```text
maximum code-enforced batch: 1000 rows
existing correctness batch: 10 rows
existing cleanup workers: 2
protected active rows tolerance: 0
protected locked rows tolerance: 0
cross-scope deletion tolerance: 0
duplicate transition tolerance: 0
```

初始总记录数、concurrent writer数、工程验收batch、backlog threshold和cleanup duration threshold缺少成功负载证据。1000是代码hard ceiling，不是可直接采用的运行batch。

### F. PostgreSQL contention and fail-closed

必须保持0容忍：in-memory fallback、orphan、partial commit、unstructured success、commit-unknown readmit。现有migration lock/statement timeout只属于schema callback；runtime connection pool大小、acquire timeout、lock-wait ceiling和database-unavailable压力轮次没有current配置或测量依据。

### G. Restart persistence

未来验收必须分别输出：

1. application-object reconstruction；
2. Spring application context restart；
3. PostgreSQL container stop/start且保留同一data volume。

现有证据只覆盖第1项。第2项和第3项缺少可追溯的执行次数、恢复时限和结果阈值。

### H. Full Maven regression

命令和结果规则可以冻结：

```powershell
mvn -ntp test
```

- 禁止`-DskipTests`、`-DskipITs`或缩减reactor；
- reactor必须完整结束；
- Surefire fork异常、JVM crash或native-memory OOM统一为`FULL_REGRESSION_INCOMPLETE`；
- 所有Surefire结果必须0 failures、0 errors、0 skipped；
- targeted correctness suite不得替代full regression。

但最低host available memory、Docker内存、JVM heap/native上限、fork策略和最大并行度没有成功full regression样本支持，因此environment preflight尚不能冻结。

## 6. Harness contract disposition

以下结构要求已经有current依据，但整个合同因数值阈值未冻结而不能accepted：

- Maven profile候选入口必须是仓库内单一正式入口；
- 使用固定baseline SHA派生seed，禁止每次随机；
- `postgres:17`与Testcontainers 1.20.4；
- isolated PostgreSQL、真实Spring wiring、canonical source/HMAC、deterministic mock gateway、loopback；
- protected 2xx entry与direct persistent-store scenarios分开统计；
- 捕获Tomcat、Hikari、JVM、HTTP、database lock/timeout和row lifecycle指标；
- raw artifact使用machine-readable格式，summary使用Markdown；
- 缺失mandatory metric输出`NOT_CAPTURED`并使对应scenario失败；
- 任一correctness invariant、threshold comparison或environment preflight失败时进程返回非0；
- CI不得使用production配置、外部HTTP、Provider、NQ、Agent、LangGraph、Paper或LIVE。

正式profile名称、scenario selector、seed表示、warm-up、rounds、duration和threshold必须等待证据blocker关闭后一次性冻结，当前不得提前视为harness implementation授权。

## 7. Environment and OOM preflight disposition

已冻结错误分类：

```text
preflight resource requirement not met: ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED
native-memory OOM during execution: FULL_REGRESSION_INCOMPLETE
```

执行前必须采样host total/available memory、Docker memory/CPU、JVM flags、process RSS、page file、Docker daemon状态和磁盘空间；执行中必须周期采样JVM heap/non-heap、process RSS、Docker/PostgreSQL memory与Hikari usage；`hs_err_pid`、Surefire dumpstream和replay log必须保存到gitignored artifact目录并计算SHA-256。

当前失败样本只能证明约0.77–1.96 GiB available memory不足以完成上一轮full regression，不能证明任何更高数值必然足够。没有成功样本时设置最低host memory、Docker memory或native headroom都会成为未经验证的整数，因此environment preflight保持blocked。

## 8. Evidence separation

```text
correctness test evidence != capacity test evidence
capacity test evidence != full Maven regression evidence
targeted PostgreSQL suite != full Maven regression
quality validate != Maven test
hard ceiling != measured default
failed diagnostic matrix != accepted threshold
```

三类证据必须使用独立命令、独立artifact和独立verdict；任何一类通过都不能填补另一类缺失。

## 9. Blocking evidence required

下一任务必须先获取并审查：

1. 至少一组actual-wiring protected 2xx成功calibration数据；
2. rate、nonce、lifecycle和cleanup各自的可重复并发样本；
3. throughput与p50/p95/p99/max latency原始分布；
4. Hikari active/pending/acquire、JVM heap/non-heap/RSS、PostgreSQL lock/timeout与row growth数据；
5. cleanup backlog收敛曲线；
6. Spring context与PostgreSQL volume-preserving restart结果；
7. 至少一次资源充足环境中的完整`mvn -ntp test`成功样本；
8. B1 hard ceiling与当前代码校验范围的authority reconciliation：`CLOSED / ACCEPTED`；不得把该结论替代前7项容量证据。

在这些证据形成前，不允许进入capacity harness work order、harness implementation、capacity execution或Stage-QDR-7 B3。

## 10. Final decision

```text
CAPACITY_ACCEPTANCE_CRITERIA_FREEZE: BLOCKED
CAPACITY_CRITERIA_AUTHORITY: BLOCKED / NOT_FROZEN
PROJECT_ACCEPTANCE_BASELINE: BLOCKED
SCENARIO_MATRIX: FAIL / SAME_POOL_RECOVERY_INCOMPLETE
NUMERIC_THRESHOLDS: BLOCKED
THRESHOLD_JUSTIFICATION: FAIL
HARNESS_CONTRACT: BLOCKED
ENVIRONMENT_PREFLIGHT: PASS
FULL_REGRESSION_CONTRACT: PASS / MAVEN_AND_RESOURCE_BASELINE
CURRENT_FACTSOURCE_SYNC_REQUIRED: NO / CURRENT_ALIGNED
HARD_CEILING_AUTHORITY: NORMATIVE_SECURITY_LIMIT / IMPLEMENTATION_ALIGNED
GUARD_CONFIGURATION_BYPASS: CLOSED
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT / SCENARIO_EVIDENCE_BLOCKED
CAPACITY_CALIBRATION_PATH_BLOCKER: CLOSED
REPEATABLE_PROTECTED_2XX: PASS
ACTUAL_WIRING_PROTECTED_2XX: PASS / REPEATABLE_PATH_AVAILABLE
RATE_LIMIT_CALIBRATION: PASS / 15 OF 15 MEASURED ROUNDS
QUOTA_ATOMICITY: PASS / COLD_START 3 OF 3
CLEANUP_BACKLOG_EVIDENCE: PASS / TENANT_SCOPED 10 + 100 + 1000
POSTGRESQL_SAME_POOL_RECOVERY: BLOCKED / NO PROTECTED 2XX WITHIN 60 SECONDS
POSTGRESQL_CONTENTION_EVIDENCE: FAIL / SAME_POOL_RECOVERY_AND_SERIES_INCOMPLETE
RESTART_PERSISTENCE_EVIDENCE: NOT_COMPLETED / BLOCKED_BY_SAME_POOL_RECOVERY
FULL_REGRESSION_RESOURCE_BASELINE: PASS / 185 SUREFIRE ROWS / 5 PIDS
ALLOW_CAPACITY_THRESHOLD_EVIDENCE_RETRY_2: NO / CONSUMED_BLOCKED
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
next task: NOT_ASSIGNED / FORMAL_TASK_ID_REQUIRED
```
