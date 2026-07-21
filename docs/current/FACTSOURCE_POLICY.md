# Decision Hub Factsource Policy

## Terminal current authority — 2026-07-21 exact-SHA CI accepted and Stage-QDR-8 plan frozen

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
CI-red remediation: CLOSED / ACCEPTED
Same-pool recovery fix commit: 8906389352d9d92099acdb857fce97aece3e6a20
Exact-SHA CI: PASSED / ACCEPTED / RUN 29823413542
Remote regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
Remote quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Stage-QDR-8 plan: CLOSED / ACCEPTED
Stage-QDR-8 implementation work order: FROZEN / SCOPE CONTRACT COMPLETE
Stage-QDR-8 implementation: NOT_STARTED / NEXT
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_FOUNDATION
Planning docs commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
current task: DH-SAME-POOL-RECOVERY-FIX-EXACT-SHA-CI-AND-STAGE-QDR-8-PLAN-FREEZE
current task status: CLOSED / ACCEPTED
next action: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_STAGE_QDR_8_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CURRENT_TASK_IS_PLANNING_ONLY
ALLOW_API_CHANGE_NOW / ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME_INTEGRATION: NO / NO / NO
ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Stage-QDR-8 当前 factsources 为既有 14 个路径加 `DH_STAGE_QDR_8_PLAN.md` 与 `DH_STAGE_QDR_8_IMPLEMENTATION_WORK_ORDER.md`，共 16 个。新的计划与工单不得覆盖 `STATUS.md` / `WORK_ORDER.md` 主权威；implementation 尚未开始。

## 1. 目的

本文件定义Decision Hub current factsource权威层级、blocker规则和历史文档边界。目标是防止旧阶段文档、过期work order、历史review记录和阶段中间产物继续覆盖当前状态。

## 2. CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE

当前状态与下一任务的主权威只有：

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
```

以下入口和执行指导必须与主权威一致，但不得覆盖主权威：

```text
README.md
CLAUDE.md
AGENTS.md
docs/current/README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/TESTING.md
```

若入口或执行指导与主权威冲突，review/close可以阻断；修复时以`STATUS.md`和`WORK_ORDER.md`为准。

权威层级固定为：

```text
Primary current-state authority:
docs/current/STATUS.md
docs/current/WORK_ORDER.md

Policy authority:
docs/current/FACTSOURCE_POLICY.md

Execution guidance:
AGENTS.md
CLAUDE.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md

Entry/index documents:
README.md
docs/current/README.md
```

## 2.1 TASK_SCOPE_DESIGN_VALIDATION

所有任务在实施前必须满足：

```text
VALIDATION_SCOPE ⊆ READ_SCOPE
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST
```

任一包含关系不成立时，不得开始实施，必须输出`TASK_SCOPE_DESIGN_INVALID`。

## 2.2 REVIEW_CLOSEOUT_RULE

1. 技术验收已通过且唯一阻断为current docs漂移时，必须在同一任务内修复文档并完成最终验收。
2. 禁止创建`docs fix -> review -> docs fix -> review`循环。
3. `STATUS.md`和`WORK_ORDER.md`冲突可以阻断阶段close。
4. 其他入口文档漂移必须修复，但不得自动降级为migration、事务或安全实现失败。
5. 只有新的真实P0/P1代码、安全、tenant、事务、migration或API问题，才能阻断技术acceptance。
6. 要求`current conflict count = 0`的文件必须全部位于当前任务`WRITE_ALLOWLIST`。

## 3. SUPPORTING_DOCS_NOT_BLOCKERS_BY_DEFAULT

以下文件默认不是 primary stage gate source，也不是 close review blocker：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/ARCHIVE_INDEX.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

这些文件可以提供背景、验证证据、变更记录、API / DB 摘要或历史复盘，但不得覆盖 `STATUS.md` 与 `WORK_ORDER.md` 的当前结论。其内部旧`next action`、旧`current task`或旧阶段状态一律按historical/consumed解释，除非主权威在当前块中显式引用。

## 3.1 ARCHIVED_DOCS_NOT_BLOCKERS

以下目录是 historical records，默认不作为 close review blocker：

```text
docs/gates/**
docs/archive/** 仅当历史遗留目录存在时使用；QDR 当前归档标准不是 docs/archive
```

`docs/gates/**` 是当前 QDR 阶段归档目录。归档文件保留历史内容和复盘价值，但不得覆盖 `STATUS.md`、`WORK_ORDER.md`、`CODEX_PROJECT_INSTRUCTIONS.md` 或 `TESTING.md` 的当前结论。

## 4. Supporting Docs 升级为 Blocker 的硬错误

只有出现以下硬错误时，supporting docs 才能升级为 blocker：

```text
real provider started
real HTTP started
Provider SDK introduced
LangGraph started
Agent runtime started
LIVE enabled
已关闭或历史阶段被重新写成当前implementation
stage-qdr-3 acceptance/final close 与 current factsources 冲突
gateway result can trade
raw prompt 或 raw provider response 被写成可保存
credential storage allowed
NQ mutation allowed
```

## 5. Historical pre-publication state — 2026-07-21 same-pool recovery test concurrency fix local accepted

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
B3 final documentation: PUBLISHED
Historical CI run 29757352202: FAILED / TEST_ONLY_UNSAFE_SHARED_COLLECTION / ConcurrentModificationException
Same-pool recovery test concurrency: FIXED / LOCAL_ACCEPTED
Collector concurrency contract: PASS / 10 ROUNDS / 16 WRITERS / 1000 SAMPLES EACH / EXACT COUNT / 0 DUPLICATES
PostgreSQL recovery stability: PASS / 5 OF 5 / 3 RESTARTS EACH / PERSISTENT STATE PRESERVED
Full regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Production Java / POM / workflow / API / migration / Repository / contracts diff: 0
current task: DH-CI-RED-SAME-POOL-RECOVERY-CONCURRENT-MODIFICATION-FIX
current task status: DONE / LOCAL_ACCEPTED
next action: OBTAIN PUSH AUTHORIZATION; FAST-FORWARD PUSH; RUN EXACT-SHA TEST + QUALITY CI
Remote CI: PENDING NEW COMMIT
Stage-QDR-8: NOT_PLANNED / BLOCKED UNTIL EXACT_SHA CI PASS
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 14 OF 14 / 0 CONFLICTS
ALLOW_B3_FINAL_CLOSE: NO / CONSUMED_ACCEPTED
ALLOW_EXACT_SHA_CI: YES / AFTER PUSH AUTHORIZATION
ALLOW_STAGE_QDR_8_PLANNING: NO / EXACT_SHA_CI_PASS_REQUIRED
ALLOW_STAGE_QDR_8_IMPLEMENTATION: NO
ALLOW_API_CHANGE_NOW / ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME_INTEGRATION: NO / NO / NO
ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本任务仅修复 test-only 日志采样集合的并发读取，不改变 B3 runtime 语义。B3 终局文档保持已发布；B2 formal capacity 继续 deferred，Stage-QDR-8 规划必须等待新提交 exact-SHA CI 全绿。

## 5.1 Historical terminal state — 2026-07-20 Stage-QDR-7 B3 final close accepted

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
Limited dry-run runtime readiness: ACCEPTED
B3.1 runtime boundary contracts: CLOSED / ACCEPTED
B3.2 protected mock-only runtime wiring: CLOSED / ACCEPTED
B3.3 runtime readiness and no-side-effect tests: CLOSED / ACCEPTED
Implementation commit: e42d430d6f8d18e32d8a9f02d2197aa68a595d63
Exact-SHA CI: PASSED / ACCEPTED / RUN 29750432646
Remote regression: PASS / 19 OF 19 REACTOR / 1160 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
Remote quality: PASS / CHECKSTYLE 0 / SPOTLESS PASS
Runtime availability: DEV_TEST_ONLY
Runtime default: DISABLED
Provider: DETERMINISTIC_MOCK_ONLY
Runtime controls: BOUNDED / FAIL_CLOSED / NO_SIDE_EFFECT
Kill switch: STARTUP_CONFIGURATION_SNAPSHOT / FAIL_CLOSED / NOT_MULTI_INSTANCE_DYNAMIC_SHARED
Retry: 0
External HTTP: 0
Real Provider: 0
NQ runtime: 0
Order/risk/ledger/Paper/LIVE mutations: 0
Audit/trace/snapshot/replay: PASS
current task: DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-FINAL-CLOSE
current task status: CLOSED / ACCEPTED
next action: DH-STAGE-QDR-7-NEXT-PHASE-PLAN-AND-WORK-ORDER-FREEZE
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 14 OF 14 / 0 CONFLICTS
ALLOW_EXACT_SHA_CI: NO / CONSUMED_ACCEPTED
ALLOW_B3_FINAL_CLOSE: NO / CONSUMED_ACCEPTED
ALLOW_NEXT_PHASE_PLANNING: YES
ALLOW_NEXT_PHASE_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
```

本节为唯一 active current state。B3 milestone 已关闭，但 B2 formal capacity 仍为 deferred；后续只允许 `DH-STAGE-QDR-7-NEXT-PHASE-PLAN-AND-WORK-ORDER-FREEZE`，不得直接实施或恢复任何已消费任务。

### Historical B3 implementation local acceptance — consumed by final close

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
Capacity acceptance criteria: FROZEN / ACCEPTED
Implementation baseline HEAD: 1eea7b2b0e1f160c4a1c90ac17911e74230eaedc
Remote baseline CI: PASS / RUN 29744016753 / TEST 1145 / QUALITY PASS
B3 local validation: PASS / TEST 1160 / 0 FAILURES / 0 ERRORS / 0 SKIPPED / QUALITY PASS / POSTGRESQL TESTCONTAINERS EXECUTED
Remote CI: PENDING NEW COMMIT
Final Retry-4 formal execution: BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED / 20260719T082658Z
Final Retry-4 terminal disposition: DEFERRED / KNOWN_LIMITATION
Final Retry-4 blocker: POSTGRES IMAGE CHECK / EXPECTED CACHED postgres:17 / ACTUAL MISSING
Final Retry-4 Maven/internal exit: 1 / 10
Final Retry-4 preflight: 25 OF 26 PASS
Final Retry-4 scenarios: STARTED 0 / COMPLETED 0 / NOT_STARTED 15
Final Retry-4 thresholds: EXECUTED 0 / NOT_EVALUATED 94
Final Retry-4 artifacts: PASS / 27 FILES / 26 MANIFEST ENTRIES / 0 MISMATCH / 0 SECRET FINDINGS / TEARDOWN PASS
Post-B2 capacity acceptance: DEFERRED / KNOWN_LIMITATION
Stage-QDR-7 B3: LIMITED DRY-RUN RUNTIME READINESS
Stage-QDR-7 B3 plan: CLOSED / ACCEPTED
Stage-QDR-7 B3 implementation work order: FROZEN / SCOPE CONTRACT COMPLETE
Stage-QDR-7 B3 implementation: IMPLEMENTED / LOCAL_ACCEPTED
Limited dry-run runtime: DEV_TEST_ONLY / DEFAULT_DISABLED / MOCK_PROVIDER_ONLY / BOUNDED / FAIL_CLOSED / NO_SIDE_EFFECT
current task: DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-IMPLEMENTATION
current task status: DONE / LOCAL_ACCEPTED
next action: OBTAIN PUSH AUTHORIZATION; PUSH EXACT COMMIT; RUN EXACT-SHA REMOTE TEST + QUALITY CI
Scope contracts: READ_SCOPE / WRITE_ALLOWLIST / VALIDATION_SCOPE / FIXABLE_BLOCKER_SCOPE / CURRENT_FACTSOURCE_SCAN_SCOPE = FROZEN
Scope invariants: PASS / 3 OF 3
ALLOW_FORMAL_RETRY_4: NO / CONSUMED_BLOCKED
RETRY_5: NOT_ALLOWED
POST_FINAL_FORMAL_HARNESS_FIX_CHAIN: NOT_ALLOWED
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_EXACT_SHA_CI: YES / AFTER PUSH AUTHORIZATION
ALLOW_B3_FINAL_CLOSE: NO / EXACT_SHA_CI_PENDING
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
real HTTP: NO
real provider: NO
NQ runtime integration: NO
Agent / LangGraph: NO
Paper: NO
LIVE: DISABLED
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 14 OF 14 / 0 CONFLICTS
```

本节以上述 terminal block 为唯一 active current state。最后一次 formal 不得重跑；旧 B2 与 pre-final 状态只保留为历史证据。B3 implementation 已在冻结 scope 内完成并本地接受，但不得描述为 production ready、Integration-1 runtime accepted 或 capacity accepted。B3 final close 只能在 exact commit 推送并通过 exact-SHA 远端 test + quality CI 后执行。

### Historical pre-final snapshot — consumed by final Retry-4

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED / ACCEPTED
Schema errata implementation: ACCEPTED
Persistent guards implementation: ACCEPTED
Guard hard-ceiling contract: CLOSED / ACCEPTED
Guard configuration bypass: CLOSED
Normative hard ceilings: rate window <= 3600s / rate quota <= 100000 / idempotency lease <= 900s
Capacity acceptance criteria: FROZEN / ACCEPTED
Capacity harness work order: CLOSED / ACCEPTED
Capacity harness implementation: CLOSED / ACCEPTED
Harness runtime binding: CLOSED / ACCEPTED
Harness Testcontainers lifecycle: CLOSED / ACCEPTED
Blocked artifact contract: CLOSED / ACCEPTED
Capacity harness runtime blocker-3: CLOSED / ACCEPTED
Harness tenant isolation: CLOSED / ACCEPTED / 3 OF 3 / QUERY_MISSING_TENANT_FILTER
Harness Context restart: CLOSED / ACCEPTED / 3 OF 3 / TEST_LIFECYCLE_FIXTURE_DEPENDENCY
Harness nonce driver: CLOSED / ACCEPTED / V4 REPLAY_KEY
Harness stabilization: CLOSED / ACCEPTED
Surefire fork startup stabilization: CLOSED / ACCEPTED
dh-domain consecutive regression: PASS / 3 OF 3 / 151 TESTS EACH / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Implementation validation: PASS / IMPLEMENTATION_VALIDATION_ONLY / 20260719T071924Z / MAVEN EXIT 0 / INTERNAL EXIT 0 / PREFLIGHT 26 OF 26 / 0 OF 15
Qualification: PASS / 15 OF 15 / NOT_FORMAL / QUALIFICATION_ONLY / 20260719T072450Z
Qualification thresholds: PASS / 94 OF 94 COMPARISONS
Qualification full regression: PASS / 19 OF 19 REACTOR SUCCESS / 1145 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Qualification quality/artifacts: PASS / CHECKSTYLE 0 / SPOTLESS PASS / 31 MANIFEST ENTRIES / 0 MISMATCH / 0 SECRET FINDINGS / TEARDOWN PASS
Qualification capacity acceptance executed: false
Qualification formal acceptance verdict: NOT_EVALUATED
Independent default regression: PASS / 19 OF 19 REACTOR SUCCESS / 1145 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Independent quality: PASS / CHECKSTYLE 0 / SPOTLESS PASS / 19 OF 19 REACTOR SUCCESS
Benign dumpstream exclusion: AUTHORIZED / 310 BYTES / BOOT MANIFEST-JAR + DIFFERENT ROOT ONLY / MAVEN AND SUREFIRE PASS / NOT STAGED
Historical remote CI baseline: PASS / RUN 29588823663 / HEAD f2f07ad3f14875165263e66428e3fe472bbe3cef
Historical remote CI: PASS / RUN 29646937611 / HEAD 20c665c7506c9f96da341944635918eec5275b7f / TEST + QUALITY
Remote CI: PENDING / EXACT-SHA STABILIZATION COMMIT REQUIRED
Formal profile: qdr7-capacity-acceptance
Historical formal run: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT / 20260715T140521Z
Latest binding validation: BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED / 20260715T145836Z / EXPECTED CONTRACT
Historical Retry-2 formal run: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT / 20260715T160710Z / INTERNAL EXIT 80
Historical Retry-3 formal run: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT / 20260716T144346Z / INTERNAL EXIT 20
Historical Retry-4 environment run: BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED / 20260717T151351Z / MAVEN EXIT 1 / INTERNAL EXIT 10
Capacity environment blocker: CLOSED / ACCEPTED / 20260718T062033Z / PREFLIGHT 26 OF 26 / MINIMUM MEMORY 32249491456 BYTES
Latest formal run: BLOCKED / ENVIRONMENT_SUREFIRE_FORK_STARTUP_BLOCKED / 20260719T035205Z / MAVEN EXIT 1 / INTERNAL EXIT NOT_EMITTED
Latest formal reason: SUREFIRE_FORKED_VM_STARTUP_TERMINATED / DH_DOMAIN / BEFORE_HARNESS
Latest formal preflight: NOT_EXECUTED / PRE-HARNESS SUREFIRE FORK STARTUP BLOCKER
Latest formal mandatory scenarios: BLOCKED / STARTED 0 / COMPLETED 0 / PARTIAL 0 / NOT_STARTED 15
Latest formal evidence: BLOCKED / RUN ROOT NOT CREATED / SUMMARY-LEDGER-COMPARISON-MANIFEST-SECRET-TEARDOWN NOT_GENERATED
Historical stabilized formal memory: 27181027328 BYTES / REQUIRED 17179869184 / HEADROOM 10001158144
Historical stabilized formal partial evidence: PASS / ACTUAL WIRING + RATE 15 OF 15 + QUOTA 3 OF 3 + ISOLATION 3 OF 3 + CANONICAL SOURCE + NONCE 3 OF 3
Historical stabilized formal blockers: IDEMPOTENCY_RESULT_FIXTURE_FK + RESTART_AGGREGATE_MISSING_PERSISTENT_VOLUME
Historical stabilized formal evidence ledger: BLOCKED / PARTIAL EXECUTION COLLAPSED TO 0 OF 15 + RESTART ROUND COUNT DRIFT
Historical stabilized formal artifacts: PASS / 27 FILES / 26 MANIFEST ENTRIES / 0 MISMATCH / 0 SECRET FINDINGS / TEARDOWN PASS
Historical stabilized Maven pre-integration regression: PASS / 172 REPORTS / 1141 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Latest implementation validation: PASS / IMPLEMENTATION_VALIDATION_ONLY / 20260719T071924Z / MAVEN EXIT 0 / INTERNAL EXIT 0 / 0 OF 15
Latest implementation preflight: PASS / 26 OF 26
Latest implementation probes: PASS / TENANT 3 OF 3 / CONTEXT RESTART 3 OF 3 / NONCE DRIVER PASS
Latest implementation artifacts: PASS / 29 FILES / 28 MANIFEST ENTRIES / 0 MISMATCH / 0 SECRET FINDINGS / TEARDOWN PASS
Criteria raw-byte alignment: PASS / SHA-256 d015a48e92be91b9f6b0a5f73c358405924af15044c809e48d3034ed57973cab / SEMANTIC 0 / THRESHOLD 0
Post-B2 capacity acceptance: BLOCKED / EXACT_SHA_REMOTE_CI_AND_FINAL_FORMAL_PENDING
Capacity calibration path blocker: CLOSED
Repeatable protected 2xx: PASS
PromptVersion atomic bootstrap: CLOSED / ACCEPTED
Cleanup tenant-scoped contract: CLOSED / ACCEPTED
Rate matrix: PASS / 15 OF 15 MEASURED ROUNDS
Quota atomicity: PASS / COLD_START 3 OF 3
Cleanup protected-row safety: PASS / TENANT_SCOPED
Cleanup capacity evidence: PASS / 10 + 100 + 1000
PostgreSQL same-pool recovery: CLOSED / ACCEPTED / 3 OF 3
PostgreSQL contention evidence: PASS / SAME_POOL_RECOVERY_AND_SERIES_COMPLETE
Restart reproducibility: PASS / SPRING_CONTEXT 3 OF 3 / POSTGRESQL_SAME_CONTAINER 3 OF 3
Capacity threshold evidence: CLOSED / SUFFICIENT
Candidate threshold evidence: SUFFICIENT
Allow capacity criteria freeze retry: NO / CONSUMED_ACCEPTED
Allow capacity harness work order: NO / CONSUMED_ACCEPTED
Allow capacity harness implementation: NO / CONSUMED_ACCEPTED
Allow capacity acceptance execution: NO / EXACT-SHA CI THEN FINAL RETRY-4 FORMAL ONLY
Allow post-B2 capacity acceptance retry-3: NO / CONSUMED_BLOCKED
Allow capacity harness CI verification: NO / CONSUMED_ACCEPTED
Allow formal retry-4: CONDITIONAL / EXACT-SHA CI PASS REQUIRED / FINAL ATTEMPT ONLY
Allow capacity environment blocker: NO / CONSUMED_ACCEPTED
Allow capacity harness defect correction: NO / CONSUMED_ACCEPTED
Formal-run full regression scenario: PENDING / FINAL RETRY-4
Previous full regression resource baseline: PASS / 380 SUREFIRE ROWS / 7 PIDS / HISTORICAL
Formal-run quality scenario: PENDING / FINAL RETRY-4
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-SUREFIRE-FORK-STARTUP-STABILIZATION
current task status: CLOSED / ACCEPTED
next action: EXACT-SHA REMOTE TEST + QUALITY CI; THEN FINAL ORIGINAL RETRY-4 FORMAL ACCEPTANCE
ALLOW_EXACT_SHA_CI: YES / NEXT ACTION ONLY
ALLOW_FORMAL_RETRY_4: CONDITIONAL / EXACT-SHA CI PASS REQUIRED / FINAL ATTEMPT ONLY
RETRY_5: NOT_ALLOWED
POST_FINAL_FORMAL_HARNESS_FIX_CHAIN: NOT_ALLOWED
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 5.2 Previous attempt / Superseded current state / Consumed evidence

以下状态只记录上一轮任务当时的真实结果，属于`Historical / Previous attempt / Superseded current state / Consumed evidence`，不得覆盖第5节active current state：

```text
previous current task: DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER
previous next task: DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2
previous full regression: PASS / 1101 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
previous same-pool attempt full regression: PASS / 1110 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
previous same-pool result: BLOCKED / RECOVERY_PROBE_INVALID
```

## 6. Archive Rule

`docs/gates/**` 是当前 QDR 阶段归档目录。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档标准；若历史遗留目录存在，只能作为 historical reference。

归档文件可以说明过去某一轮任务当时的状态，但不能作为当前事实源。若归档文档与 current factsources 冲突，以 `STATUS.md` 与 `WORK_ORDER.md` 为准。

## 7. Current Alignment Rule

`DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE`已在不修改callback、Java、测试、V1–V14、API、contracts、golden_cases或NQ的前提下完成8-file对齐，并保留初始`BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT`审计记录；其旧capacity acceptance路线已被后续任务消费，只作为historical record。

B2保持`CLOSED / ACCEPTED`，criteria保持`FROZEN / ACCEPTED`，全部formal blocked run继续作为历史证据保留。Surefire fork startup stabilization本地验收已通过；Post-B2 capacity acceptance仍为`BLOCKED / EXACT_SHA_REMOTE_CI_AND_FINAL_FORMAL_PENDING`。下一步只允许exact-SHA远端test + quality CI，然后执行最后一次原Retry-4 formal；Retry-5和后续harness微型修复链均不允许，当前B3继续`NOT_ALLOWED`。
