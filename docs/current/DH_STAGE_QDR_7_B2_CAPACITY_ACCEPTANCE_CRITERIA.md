# DH Stage-QDR-7 B2 Capacity Acceptance Criteria

> task: `DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`
> date: `2026-07-15`
> authority status: `FROZEN / ACCEPTED`
> B2 status: `CLOSED / ACCEPTED`
> post-B2 capacity acceptance: `BLOCKED / PENDING HARNESS AND EXECUTION`
> Stage-QDR-7 B3: `NOT_ALLOWED`

## Current freeze decision

```text
CAPACITY_ACCEPTANCE_CRITERIA_FREEZE: DONE / ACCEPTED
EVIDENCE_COMPATIBILITY: PASS
PROJECT_ACCEPTANCE_BASELINE: FROZEN
SCENARIO_MATRIX: PASS / FROZEN
NUMERIC_THRESHOLDS: FROZEN
THRESHOLD_JUSTIFICATION: PASS
ENVIRONMENT_BASELINE: FROZEN
HARNESS_CONTRACT: FROZEN / NOT_IMPLEMENTED
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 0 CONFLICTS
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED / PENDING HARNESS AND EXECUTION
ALLOW_CAPACITY_HARNESS_WORK_ORDER: YES / NEXT_TASK_ONLY
ALLOW_CAPACITY_HARNESS_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_ACCEPTANCE_EXECUTION_NOW: NO
ALLOW_STAGE_QDR_7_B3_ENTRY: NO
```

本标准只用于Stage-QDR-7 B2本地工程验收。它不是production SLO、production SLA、生产容量认证或部署规格，也不授权formal harness实现、容量执行、B3、外部HTTP、Provider、NQ、Agent/LangGraph、Paper或LIVE。

## 1. Scope governance

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

本任务只冻结文档合同。Java生产代码、Java测试、application配置、POM、migration、callback、API/Controller/DTO/OpenAPI/contracts、NQ与`target`原始证据均未修改；未执行新capacity matrix或故障注入。

## 2. Evidence validity ledger

| Evidence | Commit compatibility | Run ID | Status / class | Purpose | Include in freeze |
|---|---|---|---|---|---|
| 首次threshold run | `962b348`祖先代码，但repeatability已被后续修复取代 | `20260713T213431` | `HISTORICAL_FAILED_EVIDENCE` | 保留首个2xx、第二请求500与0轮matrix历史 | NO |
| retry-2完整目录 | `e2cb1ff`及当时受控dirty set，后续由`9212047`收口 | `20260714T012507` | `HISTORICAL_FAILED_EVIDENCE` | 保留cold-start、cleanup、recovery和Surefire sampler失败 | NO；不得用于阈值 |
| scenario evidence有效slice | `e2cb1ff`加受控修复集合，内容已进入`9212047` | `20260714T210052` | `VALID_CURRENT_EVIDENCE` | actual wiring、15轮rate、quota、isolation、nonce raw、idempotency、tenant cleanup、outage前pressure slice、资源 | YES |
| scenario evidence失败slice | 同上 | `20260714T210052` | `SUPERSEDED_PROBE_RESULT` | 旧same-pool recovery、初始HTTP parser、cleanup参数顺序失败、negative nonce summary parser | NO |
| same-pool recovery | 最终代码与测试内容进入`9212047`；当前HEAD无后续技术diff | `20260714T154500Z` | `VALID_CURRENT_EVIDENCE` | 固定endpoint recovery、restart、1114回归、manifest、secret scan | YES |
| correctness suites | current code/Testcontainers | 多轮 | `CORRECTNESS_ONLY_EVIDENCE` | quota、nonce、idempotency、cleanup、rollback、commit-unknown | 只用于正确性零容忍与精确计数 |

完整15轮`rate-round-summary.json`是唯一rate性能来源；单次protected 2xx、拒绝路径latency、correctness fixture和失败probe均不参与吞吐或延迟阈值。Recovery只使用`20260714T154500Z`。Manifest mismatch与secret finding均为0。

## 3. Derivation policy

- 吞吐下限：`floor(observed minimum × 0.80, 0.1 req/s)`，保留20%下降余量并向下取0.1。
- rate延迟上限：`ceil(observed maximum × 1.25, 10 ms)`，增加25%余量并向上取10 ms。
- cleanup时限：`ceil(observed maximum × 1.50, 50 ms)`；每个规模仅1个有效样本，因此使用50%余量。
- recovery时限：`ceil(observed maximum × 1.50, 100 ms)`；3轮样本，使用50%余量覆盖localhost restart抖动。
- pressure资源上限：`ceil(observed maximum × 1.25)`；内存类另按表中粒度向上取整。
- 所有正确性、跨scope污染、partial commit、unexpected 5xx、manifest mismatch、secret finding与测试失败/错误/跳过阈值固定为0。

没有删除数值不利的有效样本。排除只允许`INVALID_PROBE`、`INVALID_ENVIRONMENT`、`SUPERSEDED_CODE`或`MEASUREMENT_CORRUPTION`，并必须在evidence ledger中登记。

## 4. Environment acceptance baseline

| Item | Source / samples | Observed min / median / max | Frozen requirement | Derivation / margin | Limitation |
|---|---|---|---|---|---|
| OS / architecture | scenario environment，n=1 | Windows 11 `10.0.26200` / `amd64` | Windows 11 x64 | exact family | 未验证Linux/macOS |
| logical CPUs | scenario environment，n=1 | 32 / 32 / 32 | minimum 16 | observed × 0.50 | 不代表CPU型号等价 |
| host free memory before run | scenario environment + full regression，n=2 | 17.90 / 18.42 / 18.93 GiB | minimum 16 GiB | 向下取整并保留至少1.90 GiB | 只约束本地验收启动时点 |
| free-memory headroom during full regression | current resource baseline，n=188 | 17.52 / not aggregated / 18.93 GiB | minimum 13 GiB | observed min × 0.75，向下取1 GiB | sampler间隔2秒 |
| Docker allocation | scenario environment，n=1 | 23 / 23 / 23 GiB | minimum 16 GiB | observed向下保留约30% | cached `postgres:17` only |
| Java | environment，n=1 | `21.0.9` | Java 21 | major version exact | patch可更新但必须记录 |
| Maven | environment，n=1 | `3.9.12` | Maven 3.9.x | minor line exact | Maven wrapper不作为入口 |
| Docker | environment，n=1 | `29.6.1` | Docker Engine 29.x | major line exact | Docker Desktop localhost |
| PostgreSQL | environment，n=1 | `17.10` | cached `postgres:17` / PostgreSQL 17.x | major exact | 不允许外部数据库 |
| Testcontainers | POM/evidence，n=1 | `1.20.4` | `1.20.4` | exact | 版本变更需重新freeze |
| Hikari normal pool | rate/recovery，n=15+3 | max/min `10 / 2` | maximum pool 10 / minimum idle 2 | exact measured baseline | 不是production pool建议 |
| Hikari pressure fixture | contention slice，n=6 | max/min `4 / 2` | maximum pool 4 / minimum idle 2 | exact pressure baseline | 只用于pressure序列 |
| `MAVEN_OPTS` | environment，n=1 | unset | unset | exact | 不允许临时heap掩盖失败 |
| Surefire | POM/evidence，n=1 | 3.2.5；无显式fork/parallel | 3.2.5；repository default；parallel disabled | exact | 不推断framework default数值 |
| parallelism | frozen scenario | 1/2/4/8/16 | HTTP maximum 16；单一Maven/harness进程 | matrix最大点 | 禁止并行启动多个acceptance run |
| network | all valid runs | loopback only | `127.0.0.1` / localhost only | exact | 不代表生产网络 |

任一执行前条件不满足时必须返回非0并报告`ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED`，不得先执行再把OOM归类为容量失败。

## 5. Rate performance criteria

固定protocol：concurrency `1 / 2 / 4 / 8 / 16`；每轮warm-up 20、measured 100；每点3轮，共15轮；percentile使用nearest-rank。每轮必须100个structured 2xx、unexpected rejection=0、unexpected 5xx=0。

| C | n | throughput min/med/max req/s | Min throughput | p50 min/med/max ms | Max p50 | p95 min/med/max ms | Max p95 | p99 min/med/max ms | Max p99 | max latency min/med/max ms | Max latency |
|---:|---:|---|---:|---|---:|---|---:|---|---:|---|---:|
| 1 | 3 | 6.262 / 6.438 / 7.228 | 5.0 | 122.644 / 150.511 / 152.218 | 200 | 214.588 / 217.324 / 259.279 | 330 | 259.234 / 274.460 / 290.978 | 370 | 292.269 / 308.853 / 321.674 | 410 |
| 2 | 3 | 12.655 / 14.327 / 16.534 | 10.1 | 120.210 / 136.703 / 151.768 | 190 | 156.891 / 198.878 / 230.576 | 290 | 183.209 / 277.245 / 323.218 | 410 | 185.794 / 290.312 / 324.082 | 410 |
| 4 | 3 | 24.891 / 26.568 / 26.665 | 19.9 | 138.489 / 147.953 / 151.730 | 190 | 197.313 / 198.746 / 276.456 | 350 | 212.560 / 229.217 / 293.181 | 370 | 215.020 / 259.791 / 308.378 | 390 |
| 8 | 3 | 47.323 / 48.834 / 56.396 | 37.8 | 136.994 / 151.722 / 152.172 | 200 | 168.524 / 222.430 / 262.149 | 330 | 176.538 / 282.643 / 293.091 | 370 | 177.915 / 284.652 / 322.084 | 410 |
| 16 | 3 | 48.406 / 57.779 / 61.165 | 38.7 | 244.786 / 245.282 / 290.880 | 370 | 307.249 / 339.189 / 564.079 | 710 | 312.776 / 366.178 / 611.833 | 770 | 326.172 / 368.950 / 613.001 | 770 |

Source为`20260714T210052/rate-round-summary.json`。每个阈值都使用本节统一公式；localhost、单机、deterministic mock gateway与单一PostgreSQL容器是已知限制。

## 6. Correctness and isolation criteria

### 6.1 Cold-start quota

`windowSeconds=3600`、quota 10、concurrency 8、attempts 40、rounds 3；禁止warm-up或预建bucket。每轮必须accepted=10、`429 / RATE_LIMITED`=30、database winners=10、PromptVersion canonical rows=1、unexpected 4xx/5xx=0、transport errors=0、oversell=0。Source为`quota-atomicity-retry.csv`，n=3，各计数min/median/max均为冻结精确值，安全余量为0。

### 6.2 Tenant/environment/source

至少两个tenant/environment scope；A达到quota后B仍接受，cross-tenant rows=0、cross-environment rows=0。`CROSS_SOURCE_CAPACITY: NOT_APPLICABLE_UNDER_SINGLE_CANONICAL_SOURCE`；非canonical source必须`403 / SOURCE_DENIED`。不得伪造第二合法source。

### 6.3 Nonce race

threads=8、attempts=24、rounds=3；每轮accepted=1、`409 / NONCE_REPLAY`=23、database winner rows=1、unexpected errors=0。有效HTTP source为修复parser后的`nonce-race.csv`，n=3；negative `nonce-race-summary.json`属于`MEASUREMENT_CORRUPTION / SUPERSEDED_PROBE_RESULT`，不参与判定。数据库唯一winner同时由current PostgreSQL correctness suite约束。

### 6.4 Idempotency lifecycle

必须满足single winner、active lease not stolen、timeout/expiry transition follows contract、rollback leaves no success、commit-unknown not readmitted、terminal result stable；重复claim、错误接管、orphan、partial result、terminal overwrite与readmission容忍度均为0。Runtime lifecycle与Flyway callback必须分开报告。

## 7. Cleanup criteria

固定workers=2、concurrent writers=1、batch=10；scope为tenant/environment/endpoint/source。null、blank与`*` tenant必须fail-closed，不允许global maintenance语义。

| Initial scale | n | duration min/med/max ms | Max duration | Formula / margin | Required outcome |
|---:|---:|---|---:|---|---|
| 10 | 1 | 115 / 115 / 115 | 200 ms | max ×1.50，向上50 ms | expected delete 6；protected rows保留 |
| 100 | 1 | 110 / 110 / 110 | 200 ms | max ×1.50，向上50 ms | expected delete 64；protected rows保留 |
| 1000 | 1 | 718 / 718 / 718 | 1100 ms | max ×1.50，向上50 ms | expected delete 649；protected rows保留 |

Source为`cleanup-summary.json`。每个规模必须包含eligible、active、locked、not expired、other tenant、other environment与concurrent inserts。结束时未锁定eligible backlog=0；允许当时被锁定的1行暂留，但释放锁后必须在一个额外batch内收敛为0。duplicate、cross-tenant、cross-environment、active、locked与not-expired deletion阈值均为0。单scale只有1个有效样本，是本标准最主要的统计限制。

## 8. PostgreSQL same-pool recovery criteria

同一ApplicationContext、DataSource、Hikari pool、container endpoint、JDBC URL与persistent volume；rounds=3。

| Metric | n | Observed min/med/max ms | Frozen maximum | Formula / margin | Limitation |
|---|---:|---|---:|---|---|
| database ready | 3 | 487 / 509 / 512 | 800 ms | max ×1.50，向上100 ms | localhost container restart |
| Hikari recovery | 3 | 3720 / 3792 / 3830 | 5800 ms | max ×1.50，向上100 ms | 包含driver/pool重连抖动 |
| protected request recovery | 3 | 3792 / 3829 / 3864 | 5800 ms | max ×1.50，向上100 ms | 不是production SLA |

Outage false 2xx=0；每轮至少3个真实outage protected请求，3轮共9个；post-recovery concurrency=8、requests=100、expected 2xx=100、4xx=0、unexpected 5xx=0。已提交nonce/idempotency与PromptVersion canonical row必须保持，uncommitted transaction不得恢复为success。

## 9. Restart criteria

- Spring ApplicationContext restart：3/3 PASS。
- PostgreSQL same-container persistent-volume restart：3/3 PASS。

每轮必须验证committed nonce仍受保护、committed idempotency为terminal stable state、uncommitted transaction不是success、tenant isolation保持、PromptVersion canonical row保持、cleanup scope保持。

## 10. Hikari/PostgreSQL mandatory sequence

正式顺序固定为：normal load -> connection pressure -> lock contention -> statement timeout -> database outage -> database ready -> pool reconnect -> protected-request recovery -> post-recovery load。顺序缺失或时间序列断裂即FAIL。

| Metric | Source / n | Observed min/med/max | Frozen maximum | Formula / margin |
|---|---|---|---:|---|
| Hikari pending | valid pressure slice，n=6 | 0 / 9 / 13 | 17 | ceil(max ×1.25) |
| connection acquisition max | valid pressure slice，n=6 | 252.658 / 2012.633 / 3014.484 ms | 3800 ms | max ×1.25，向上100 ms |
| PostgreSQL waiting sessions | valid pressure slice，n=6 | 4 / 5 / 5 | 7 | ceil(max ×1.25) |
| PostgreSQL lock-wait sessions | valid pressure slice，n=6 | 0 / 3 / 3 | 4 | ceil(max ×1.25) |
| outage sampling gap | same-pool outage intervals，n=24 | 18.136 / 1027.963 / 1048.542 ms | 1400 ms | max ×1.25后向上100 ms |

unexpected HTTP 5xx=0、rollback anomalies=0、deadlocks=0、partial commits=0。Expected fail-closed 5xx during the explicit outage/pressure step must be classified separately and must never be counted as success; false 2xx remains0。Statement timeout必须产生受控rollback，不能成为unexpected error。

## 11. Full regression and resources

正式命令为`mvn -ntp test`；必须19/19 Reactor SUCCESS、0 failures/errors/skipped、真实PostgreSQL/Testcontainers执行、无native-memory OOM，并证明所有discovered Surefire forks被采样或明确no-fork。

| Metric | Source / samples | Observed maximum or minimum | Frozen threshold | Formula / margin |
|---|---|---|---|---|
| execution duration | current 1114 run，n=1 | max 509.631 s | max 660 s | max ×1.25，向上30 s |
| Maven working set | current resource baseline，n=186 | max 361,811,968 bytes | max 576 MiB | max ×1.50，向上64 MiB |
| Surefire individual working set | 1110+1114 runs，185+380 rows/12 PIDs | max 1,278,676,992 bytes | max 2 GiB | max ×1.50，向上256 MiB |
| Surefire aggregate working set | compatible sampled runs | max 1,278,676,992 bytes | max 2 GiB | max ×1.50，向上256 MiB |
| Docker aggregate memory | current complete Docker sampling，n=530 | max 117,549,563 bytes | max 192 MiB | max ×1.50，向上64 MiB |
| free-memory headroom | current host sampling，n=188 | min 18,808,967,168 bytes | min 13 GiB | min ×0.75，向下1 GiB |

资源阈值只用于确保本地验收可复现，不是生产机器规格。

## 12. Formal harness contract

本轮只冻结合同，不实现harness。

```text
Maven profile: qdr7-capacity-acceptance
Formal command: mvn -ntp -Pqdr7-capacity-acceptance -Dqdr7.runId=<UTC_yyyyMMddTHHmmssZ> -Dqdr7.seed=7 verify
Evidence root: target/qdr7-capacity-acceptance/<run-id>/
Primary summary: capacity-acceptance-summary.json
Seed: integer 7, fixed
Run ID: caller supplied, UTC format, unique and path-safe
```

Harness必须使用actual Spring wiring、localhost受保护HTTP、真实PostgreSQL/Testcontainers、canonical source/HMAC和deterministic mock gateway；覆盖全部mandatory scenario；输出raw CSV、JSON、p50/p95/p99、Hikari/PostgreSQL/JVM/Docker指标、threshold comparison、environment preflight、SHA-256 manifest与secret scan。

以下任一条件必须非0退出：environment不满足、scenario/metric缺失、correctness invariant失败、threshold失败、Testcontainers skipped、manifest mismatch、secret finding、外部HTTP尝试或未完成full regression。不得输出credential material，不得连接Provider、NQ、Agent/LangGraph、Paper或LIVE。

## 13. Verdict rules and next route

PASS要求environment、actual wiring、rate、quota、isolation/source、nonce、idempotency、cleanup、recovery、restart、Hikari/PostgreSQL sequence、full regression、quality、manifest、secret scan与current factsources全部PASS。标准已冻结但执行违反阈值为FAIL；标准、harness、环境或mandatory evidence不能形成判断为BLOCKED。BLOCKED不得写成FAIL，correctness PASS不得写成capacity PASS。

```text
Capacity acceptance criteria: FROZEN / ACCEPTED
Capacity harness: NOT_IMPLEMENTED / NEXT
Post-B2 capacity acceptance: BLOCKED / PENDING HARNESS AND EXECUTION
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER
```

---

## Historical preserved criteria record

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
