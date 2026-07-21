# Decision Hub Agent Guidelines

## Terminal current authority — 2026-07-21 exact-SHA CI accepted and Stage-QDR-8 plan frozen

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
B3 final documentation: PUBLISHED
CI-red remediation: CLOSED / ACCEPTED
Same-pool recovery fix commit: 8906389352d9d92099acdb857fce97aece3e6a20
Exact-SHA CI: PASSED / ACCEPTED / RUN 29823413542
Remote regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
Collector concurrency contract: PASS / 10 ROUNDS / 16 WRITERS / 1000 SAMPLES EACH / EXACT COUNT / 0 DUPLICATES / 0 CROSS-ROUND POLLUTION / 0 WORKER LEAKAGE
Remote quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Stage-QDR-8 plan: CLOSED / ACCEPTED
Stage-QDR-8 implementation work order: FROZEN / SCOPE CONTRACT COMPLETE
Stage-QDR-8 implementation: NOT_STARTED / NEXT
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_FOUNDATION
Planning baseline: 8906389352d9d92099acdb857fce97aece3e6a20
Planning docs commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
current task: DH-SAME-POOL-RECOVERY-FIX-EXACT-SHA-CI-AND-STAGE-QDR-8-PLAN-FREEZE
current task status: CLOSED / ACCEPTED
next action: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_EXACT_SHA_CI: NO / CONSUMED_ACCEPTED
ALLOW_STAGE_QDR_8_PLANNING: NO / CONSUMED_ACCEPTED
ALLOW_STAGE_QDR_8_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CURRENT_TASK_IS_PLANNING_ONLY
ALLOW_API_CHANGE_NOW / ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME_INTEGRATION: NO / NO / NO
ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Stage-QDR-8 只冻结结构化 feedback attribution 的领域与用例基础；默认不允许 persistence/API/runtime wiring，不自动更新模型、Prompt、策略、候选或历史状态，也不调用真实外部系统。B2 capacity gate 保持 deferred，Stage-QDR-7 B3 不重新打开。

本仓库是 Decision Hub。任何 Agent、Codex、人工改动都必须按本文件执行。

## 1. 项目定位

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易执行系统。

DH 负责：

```text
Agent 编排
候选方案生成
多路径探索
历史反馈强化
策略评分
冲突仲裁
报告生成
辅助决策
```

NQ 负责：

```text
交易核心
账户与资产
订单状态机
风控链路
正式回测
模拟盘/实盘执行
审计与复盘
```

## 2. 当前事实源

开工前必须优先读取：

```text
README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
```

涉及 DH/NQ 集成时，只能读取当前 factsources 明确索引且实际存在的集成文档；已归档或历史集成文档只能作为 background evidence，不得覆盖 `docs/current` 当前结论。

```text
docs/current
docs/gates/**
```

`docs/current` 是唯一当前事实源。

`docs/codex` 只保留历史计划与辅助执行区，不得覆盖 `docs/current` 的当前结论。

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

执行指导和入口文件必须与主权威一致，但不得覆盖`STATUS.md`与`WORK_ORDER.md`。

### 2.1 任务scope设计与review收口（强制）

所有任务在实施前必须满足：

```text
VALIDATION_SCOPE ⊆ READ_SCOPE
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST
```

任一包含关系不成立时，不得开始实施，必须输出：

```text
TASK_SCOPE_DESIGN_INVALID
```

Review收口规则固定为：

1. 技术验收已通过且唯一阻断为current docs漂移时，必须在同一任务内修复文档并完成最终验收。
2. 不再创建`docs fix -> review -> docs fix -> review`循环。
3. `STATUS.md`和`WORK_ORDER.md`冲突可以阻断阶段close。
4. 其他入口文档漂移必须修复，但不得自动降级为migration、事务或安全实现失败。
5. 只有新的真实P0/P1代码、安全、tenant、事务、migration或API问题，才能阻断技术acceptance。
6. 要求`current conflict count = 0`的文件必须全部出现在当前任务`WRITE_ALLOWLIST`中。

## 3. 标准工作流

DH 采用与 NQ 一致的阶段化流程：

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

禁止跳过 VERIFY 标记完成。

禁止把临时补丁说明写入根 README。

阶段推进后必须更新：

```text
docs/current/STATUS.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

## 4. 当前阶段

### Historical pre-publication authority — 2026-07-21 same-pool recovery test concurrency fix local accepted

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

### Historical terminal authority — 2026-07-20 Stage-QDR-7 B3 final close accepted

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

B3 仅关闭 dev/test limited runtime readiness milestone；B2 formal capacity 仍未证明，且不得重新打开、重试或转写为已接受。后续只允许冻结下一阶段 plan/work order，不得直接实施，不得接入真实 HTTP、Provider、NQ、Agent、LangGraph、Paper 或 LIVE。

### Historical B3 implementation local acceptance — consumed by final close

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2 implementation: CLOSED / ACCEPTED
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
Capacity acceptance criteria: FROZEN / ACCEPTED
Harness stabilization: CLOSED / ACCEPTED
Implementation baseline HEAD: 1eea7b2b0e1f160c4a1c90ac17911e74230eaedc
Remote baseline CI: PASS / RUN 29744016753 / TEST 1145 / 0 FAILURES / 0 ERRORS / 0 SKIPPED / QUALITY PASS
B3 local validation: PASS / TEST 1160 / 0 FAILURES / 0 ERRORS / 0 SKIPPED / QUALITY PASS / POSTGRESQL TESTCONTAINERS EXECUTED
Remote CI: PENDING NEW COMMIT
Final Retry-4 formal run: BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED / 20260719T082658Z
Final formal disposition: DEFERRED / KNOWN_LIMITATION
Final formal reason: POSTGRES IMAGE CHECK / EXPECTED CACHED postgres:17 / ACTUAL MISSING
Final formal Maven/internal exit: 1 / 10
Final formal preflight: BLOCKED / 25 OF 26 PASS / POSTGRES-IMAGE 1 BLOCKED
Final formal mandatory scenarios: STARTED 0 / COMPLETED 0 / PARTIAL 0 / NOT_STARTED 15
Final formal threshold comparisons: EXECUTED 0 / NOT_EVALUATED 94
Final formal regression: BLOCKED / NOT_EXECUTED
Final formal quality: BLOCKED / NOT_EXECUTED
Final formal artifacts: PASS / 27 FILES / 26 MANIFEST ENTRIES / 0 MISMATCH
Final formal secret scan: PASS / 25 FILES / 0 FINDINGS
Final formal teardown: PASS / CONTAINER REMOVED_OR_ABSENT / VOLUME REMOVED_OR_ABSENT / RESIDUAL NONE
Final formal capacity acceptance executed: false
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
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 14 OF 14 / 0 CONFLICTS
```

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
```

`AGENTS.md`是执行指导，不是primary current-state authority；权威当前状态仍以`STATUS.md`、`WORK_ORDER.md`和`FACTSOURCE_POLICY.md`指定的文件为准。

Integration-0 safety gate 已 `CLOSED / ACCEPTED`；P1-4 residual、header alignment、timestamp alignment、code reality audit blockers 均已关闭或修复。`DH-STAGE4-DECISION-PIPELINE-MVP-PLAN` 与 `DH-STAGE4-DECISION-PIPELINE-MVP-WO` 已 `ACCEPTED / CLOSED`；K1-K8 已 `CLOSED / ACCEPTED`，Decision Pipeline MVP 已以 DH Stage4 canonical 命名关闭。旧 `DH-GATEK-DECISION-PIPELINE-MVP` 与旧 `docs/gates/dh-gatek-decision-pipeline-mvp/` 只能作为 historical naming error；当前 canonical 目录为 `docs/gates/dh-stage4-decision-pipeline-mvp/`。旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` 只能作为 historical reference，当前标记为 `SUPERSEDED / REBASE_REQUIRED`；NQ 已进入 GateN，后续 Integration-1 必须基于 GateN rebase 重新规划。当前不允许启动 Integration-1 runtime、真实 NQ runtime、真实 Provider、真实 HTTP、LangGraph runtime、AI / Agent runtime 或 LIVE。

NQ / DH 三轮只读审计（NQ 全仓 / DH 全仓 / NQ-DH 联合边界 + 汇总）已完成，结论同步在 `docs/current/STATUS.md` §1.1。当前口径固定为：

```text
NQ-DH:            not integrated；runtime connection none
Integration-0:    CLOSED / ACCEPTED as contract / mock / documentation work line, not runtime integration
Allowed work:     docs, contract freeze record, mock, stub, contract test, security policy,
                  Decision Pipeline MVP planning
Forbidden work:   real NQ connection, RealClient, real Provider, trading,
                  credential access, NQ DB access, LIVE
Security baseline: FULL
Fail-closed state: FULL
P1-4 residual:     CLOSED
Integration-1:     NOT STARTED
Runtime integration: NOT STARTED
AI / Agent runtime: NOT STARTED
LIVE:              DISABLED
```

不得把 Integration-0 写成真实集成；不得把 Integration-1 写成 started；不得把 NQ integration 写成 started；不得把 DH 写成 integrated；不得把 AI / Agent runtime 写成 started；不得把 LIVE 写成 enabled。

## 4.1 阶段命名治理（强制）

DH 与 NQ 的阶段命名体系必须分离：

```text
NQ 自身阶段: Gate 体系，例如 GateN
DH 自身阶段: Stage 体系，例如 DH-STAGE4-DECISION-PIPELINE-MVP
NQ-DH 集成任务: 可引用 NQ GateN rebase，但不得把 DH 自身阶段写成 GateK/GateL/GateN
```

旧 `DH-GATEK-DECISION-PIPELINE-MVP`、`DH GateK Decision Pipeline MVP` 与 `docs/gates/dh-gatek-decision-pipeline-mvp/` 属于历史错误命名，当前 canonical 名称为 `DH-STAGE4-DECISION-PIPELINE-MVP`、`DH Stage4 Decision Pipeline MVP` 与 `docs/gates/dh-stage4-decision-pipeline-mvp/`。Decision Pipeline MVP 已 `ACCEPTED / CLOSED` 的事实不变；旧 GateK 命名只能在历史记录、旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK`、或 naming replacement 说明中出现，并必须标注 `SUPERSEDED / NAMING_REPLACED` 或 `SUPERSEDED / REBASE_REQUIRED`。不得继续创建新的 `DH-GATEK-*` 任务、current stage 或冻结目录。

## 5. 硬边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
DH 不成为交易事实源
```

## 6. 允许改动范围

当前 `DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION` 只允许改文档、workflow guard 和本仓库 `.agents` skill policy：

```text
AGENTS.md
README.md
docs/current
docs/README.md
docs/codex/**
docs/gates/** 的最小 errata / index
scripts/verify.ps1
.agents/**
```

当前任务不允许改：

```text
NQ 仓库
Java 生产代码
Java 测试代码
Flyway migration
API / Controller
contracts
golden_cases
实盘执行链路 / 订单状态机 / 风控核心 / 正式回测核心
```

## 7. 构建与验证

最低验证：

```bash
mvn test
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

验证结果必须写入：

```text
docs/current/TESTING.md
```

实现记录必须写入：

```text
docs/current/WORKLOG.md
```

## 8. 代码与命名规范

```text
Java 21
Spring Boot 3.5.x
包名前缀 com.guidinglight.decisionhub
测试类以 *Test 结尾
Flyway 迁移命名 V{版本}__{描述}.sql
```

public/protected 的类、接口、枚举、字段、方法必须有清晰注释。

关键 private 方法如果承载业务规则，也必须注释。

变量、方法、类、枚举命名必须贴近业务原意、可读、可搜索。

## 8.1 语言治理

DH 项目文档正文默认中文为主，代码注释 / Javadoc 原则上也使用中文。类名、方法名、包名、字段名、enum 值、JSON Schema 字段、OpenAPI 字段、HTTP header、状态枚举、命令和外部技术名保留英文原样。

文档任务必须使用 `dh-docs-writer`，并遵守其语言规则：固定输出字段可以保留英文，但字段内容必须中文为主；不得新增整段英文说明；不得把中文业务概念翻译成不稳定英文术语后反复使用；从 NQ skill 或历史文档同步规则时，必须改写为 DH 中文主语言风格。

## 9. Agent 输出要求

```text
所有 Agent 输出必须结构化
所有关键对象必须有 traceId
所有 NQ feedback 必须保存原始 payload
最终策略建议必须经过 JudgeDecision
```

禁止单个 Agent 直接输出最终交易决策。

## 10. 安全与配置

禁止提交密钥。

敏感值必须通过环境变量或安全配置注入。

所有外部输入必须校验，避免 SQL、路径、命令注入。

## 11. Codex workflow routing（强制）

所有 Codex / Agent 任务必须先执行 `nq-dh-workflow-router` 前置分类，再决定插件、skill、文件范围和验收命令。

开工前必须明确：

```text
repository
module
target files
excluded files
expected output
```

默认不得扫描：

```text
node_modules
target
build
dist
.git
logs
test-results
secrets
credentials
```

禁止读取、复制、提交或输出：

```text
token
cookie
API key
API secret
exchange secret
production .env
private key
mnemonic
keystore password
2FA backup code
```

文档事实源规则：

```text
不把 archived / historical / superseded 文档当作当前事实源。
除非用户明确要求历史对照，否则当前状态以 STATUS.md、AGENTS.md、
CODEX_PROJECT_INSTRUCTIONS.md、WORK_ORDER.md 的当前段落为准。
历史 Stage 文档只能作为背景，不得自动转化为当前 next task。
```

标准输出格式固定为：

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Findings:
Validation:
Risks:
Next concrete action:
```

禁止把 `Summary` 作为必填输出字段；结论必须落在 `Findings` 或 `Next concrete action`。

### 11.1 任务分类与插件路由

```text
DOCUMENTATION        GitHub + Documents + Notion
CODE_ANALYSIS        GitHub
CODE_CHANGE          GitHub + CodeRabbit
SECURITY_AUDIT       GitHub + Codex Security + CodeRabbit
AGENT_API            GitHub + OpenAI Developers + Codex Security
NQ_INTEGRATION_PLAN  GitHub + Documents + Codex Security
PRODUCT_DESIGN       Figma + Product Design（仅 UI / 流程图任务）
PRESENTATION         Presentations + Documents + Canva
```

插件路由只决定工作方式，不自动授权外部连接、真实 provider、NQ RealClient、数据库访问或交易能力。任何插件、skill、MCP 都不能绕过本文件的 Gate / Freeze / 安全边界。

### 11.2 DH/NQ 安全边界

```text
DH 是多 Agent 决策系统，不是交易执行系统。
DH 当前只允许研究、分析、候选信号、风险解释、审计记录。
DH 不允许下单、撤单、修改策略状态、启动 Paper Run、访问交易所密钥、直接读写 NQ DB。
DH 到 NQ 的任何未来 runtime 接入都必须在 Integration-0 CLOSED / ACCEPTED 之后另起 GateN rebase planning。
Integration-0 已 CLOSED / ACCEPTED，但仍只是 contract / mock / documentation work line，不允许真实业务打通。
NQ integration not started.
Integration-1 NOT STARTED.
Runtime integration NOT STARTED.
DH integrated NO.
AI / Agent runtime NOT STARTED.
RealClient forbidden.
real provider forbidden.
LIVE DISABLED.
NQ mutation forbidden.
```

### 11.3 当前 Codex workflow 文档入口

```text
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```


## 12. Claude 执行纪律

- 默认使用简体中文说明计划、过程和结论。
- 先读 `AGENTS.md`、`CLAUDE.md`、`README.md`、`docs/current/*`，再读目标代码或文档。
- 默认最小变更，避免无关重构。
- 不回退用户已有改动。
- 能用工具验证的结论必须用工具验证。
- 每次交付必须说明修改文件、验证结果、剩余风险和是否触达禁止范围。

---

### 1) 工具分级与用途边界（强制按场景选工具）

#### A. 工程内操作（最高优先级）
**`idea-mcp`**
- 用于：项目结构、检索、阅读、编辑、重构、运行配置、问题检查。
- 约束：
  - 若工具支持 `projectPath`，必须显式传入。
  - 自动修改仅限白名单目录。
  - 编辑流程必须：读文件 → 改 → 格式化 → problems 检查（必要时）。

#### B. 仓库文件直接读写 / 兜底文件操作
**`filesystem`**
- 用于：当 `idea-mcp` 不可用，或需要直接处理普通文本/配置/脚本/说明文件时的兜底读写。
- 禁止：绕过 `idea-mcp` 直接对 Java/TS 核心业务代码做大规模盲改。
- 约束：仅作为工程内文件级兜底工具，不替代 `idea-mcp` 的符号级理解与重构能力。

#### C. 代码托管与变更协作（仅限 GitHub 场景）
**`github`**
- 用于：PR/Issue 读取、diff/变更审阅、提交记录查询、仓库信息读取。
- 禁止：未经明确指令自动创建/合并 PR、强推、删除分支、改仓库设置。
- 安全：Token 仅允许通过环境变量提供，例如 `GITHUB_MCP_PAT`。

#### D. 外部资料检索（只用于资料/对照，不直接改代码）
**`brave-search`**
- 用于：查外部信息、官方文档、第三方库用法、错误码、兼容性问题。
- 禁止：把搜索结果直接当项目事实；必须回到仓库或运行结果验证可落地性。
- 说明：需要 `BRAVE_API_KEY`。

#### E. 浏览器调试 / 前端运行态排查
**`chrome-devtools`**
- 用于：查看页面 DOM、网络请求、Console、Storage、路由跳转、前端运行时错误。
- 适用场景：
  - 页面白屏
  - 接口已发出但页面没渲染
  - 路由守卫异常
  - 表单交互异常
  - Ant Design 组件行为与预期不一致
- 禁止：执行敏感线上操作。

#### F. 数据库核对 / SQL 验证
**`postgres`**
- 用于：核对表结构、索引、约束、数据分布、执行 SQL 验证 migration/backfill/查询逻辑。
- 适用场景：
  - DDL 审查
  - migration 验证
  - 查询性能初查
  - 闭环联调时校验 DB 状态
- 禁止：未经明确授权直接修改生产数据。

#### G. 容器 / 本地基础设施联调
**`MCP_DOCKER`**
- 用于：查看容器状态、日志、网络、卷、镜像、Compose 相关运行信息。
- 适用场景：
  - 本地 PostgreSQL / 中间件 / 服务容器启动失败
  - 健康检查失败
  - 联调依赖未就绪
- 禁止：未经明确说明删除镜像、清卷、破坏性 prune。

#### H. CSS / 动画 / 视觉效果辅助
**`icss`**
- 用于：复杂 CSS 布局、渐变、遮罩、滤镜、动画、玻璃拟态、纯 CSS 特效实现参考。
- 定位：辅助 MCP，不是主实现工具。
- 禁止：代替业务 skill 负责页面开发主线。

---

### 1.1 `idea-mcp` 常用能力清单（按场景）
- 项目与结构：`get_project_modules`、`get_project_dependencies`、`get_repositories`、`list_directory_tree`
- 检索与阅读：`find_files_by_glob`、`find_files_by_name_keyword`、`search_in_files_by_text`、`search_in_files_by_regex`、`get_file_text_by_path`、`get_all_open_file_paths`
- 代码理解与质量：`get_symbol_info`、`get_file_problems`
- 编辑与重构：`create_new_file`、`replace_text_in_file`、`rename_refactoring`、`reformat_file`、`open_file_in_editor`
- 执行与联调：`get_run_configurations`、`execute_run_configuration`、`execute_terminal_command`

### 2) 不可用降级条件（通用）
当首选工具出现以下任一情况，允许降级到下一优先级工具：
1. 不可访问 / 连接失败
2. 无权限 / 拒绝访问
3. 同一问题连续 2 次超时
4. 返回结果无法覆盖问题（范围不完整 / 关键文件不可读 / 结果与事实矛盾）

---

### 3) 降级顺序（按场景固定）

#### 工程内检索（定位文件 / 符号）
1) `idea-mcp`
2) `filesystem`
3) `rg`
4) `Select-String`
5) `findstr`

#### 前端运行态排查
1) `chrome-devtools`
2) `idea-mcp`
3) `filesystem`

#### 数据库结构 / 查询验证
1) `postgres`
2) `idea-mcp`
3) `filesystem`

#### 本地依赖 / 容器联调
1) `MCP_DOCKER`
2) `idea-mcp`
3) 终端命令

#### 外部资料核对
1) `brave-search`
2) 手动引用（必须带来源且标注可信度）

---

### 4) 降级披露要求（强制）
一旦发生降级，回复必须包含：
- 降级原因
- 使用的工具
- 检索范围
- 结果可信度（高 / 中 / 低 + 原因）

---

### 5) 启动与密钥规则（强制）
- 所有 API Key / Token 仅允许通过环境变量注入：
  - GitHub：`GITHUB_MCP_PAT`
  - Brave：`BRAVE_API_KEY`
  - 其他：按各 MCP server 文档约定
- 禁止把密钥写入仓库文件、Markdown、脚本、截图、日志输出。

---

### 6) 编辑与运行纪律（强制）
- 编辑：先读 → 再改 → 再格式化 → 必要时 problems 检查
- 运行：先列出 run configs → 再执行 → 汇报退出状态与关键输出

#### 编辑类任务（强制流程）
1. 先读取目标文件，确认上下文
2. 再执行修改 / 重构 / 新建文件
3. 修改后必须格式化
4. 最后确认无明显错误 / 警告激增
5. 回复中列出：修改文件清单 + 变更摘要

#### 运行 / 联调类任务（强制流程）
1. 先确认可用目标（run config / 容器 / DB / 页面）
2. 再执行联调
3. 回复中汇报：退出状态 + 关键输出摘要 + 关键异常

---


## 13 Agent Skills Routing（合并后 skills 规则）

### 13.1 Active skills（唯一默认启用集合）

当前 active skills 仅允许以下 10 个：

1. `nq-dh-workflow-router`
2. `dh-docs-writer`
3. `frontend-product-ui-design`
4. `ui-visual-system-polish`
5. `frontend-antd-page-builder`
6. `frontend-quality-regression`
7. `java-backend-maintenance`
8. `java-backend-regression-tests`
9. `db-schema-migration-review`
10. `python-ops-tooling`

使用原则：

- `nq-dh-workflow-router` 是所有任务的前置分类 skill；它只做分类、范围收口、插件建议和输出格式统一，不授权业务能力。
- `dh-docs-writer` 是 DH 文档治理主 skill；docs/current、Gate/Phase/Stage planning、work order、acceptance/freeze/close review、WORKLOG/TESTING/STATUS/ROADMAP/API 同步、DH/NQ 集成文档同步、Decision Pipeline MVP 文档规划和 docs/gates 归档任务必须使用它。
- 只选择与本轮任务直接相关的 skill，不要一次性激活所有 skills。
- 一个任务最多一个主 skill；其他 skill 只能作为补充，并说明为什么需要。
- 如果 skill 路由与当前 Gate 边界、安全边界、技术栈边界冲突，优先遵守 Gate / Freeze / Work Order / 安全 / 技术栈规则。
- 不得用 skill 名义绕过禁止项：不接 AI/DH、不接真实 provider、不接 NQ RealClient、不触碰 LIVE 交易、不新增未要求的 API / migration / 业务能力。

### 13.2 Optional skills（默认不启用）

- `.agents/optional-skills/` 下的 skill 不默认启用。
- `shadcn` / 其他非 Ant Design UI skill 只有在用户明确要求、或目标项目本身已使用对应框架时才允许使用。
- NexusQuant / Decision Hub 当前前端默认使用 React + TypeScript + Ant Design 企业后台栈，不得私自切换 UI 框架或引入新的 UI 体系。

### 13.3 前端任务路由

- 页面产品化、业务 UX、信息架构、核心状态模型、空态 / 错误态 / 禁用态 / 风险态、前端中文文案：使用 `frontend-product-ui-design`。
- 视觉层级、排版、色彩、专业金融后台质感、响应式、设计系统一致性、页面 polish：使用 `ui-visual-system-polish`。
- Ant Design 页面开发、组件组合、API 接入、类型定义、TanStack Query hooks、Axios client 接线、页面落地：使用 `frontend-antd-page-builder`。
- 前端 bug、路由 / 表单 / Ant Design 行为异常、E2E、Playwright、构建回归、UI 行为回归、提交前前端质量收口：使用 `frontend-quality-regression`。

做前端页面时，默认按以下顺序思考，但只激活本轮需要的 skill：

```text
frontend-product-ui-design
  -> frontend-antd-page-builder
  -> ui-visual-system-polish
  -> frontend-quality-regression
```

### 13.4 后端 / DB / Python 任务路由

- Java / Spring Boot / 模块边界 / Service 修复 / 异常链 / 事务 / 并发幂等 / 状态流转：使用 `java-backend-maintenance`。
- JUnit、golden cases、Controller / Service / Repository 集成回归、bug 修复后回归测试：使用 `java-backend-regression-tests`。
- Flyway / Liquibase migration、DDL、索引、约束、默认值、COMMENT、schema 审查、回填脚本审查：使用 `db-schema-migration-review`。
- Python 运维脚本、批处理、数据清洗、导入导出、迁移辅助、pytest、ruff、mypy：使用 `python-ops-tooling`。

做后端 DB 相关改动时，默认按以下顺序思考，但只激活本轮需要的 skill：

```text
db-schema-migration-review
  -> java-backend-maintenance
  -> java-backend-regression-tests
```

### 13.5 NexusQuant / Decision Hub 前端风格

- 默认是专业金融科技后台，不是营销页。
- 高信息密度但不拥挤，弱装饰、强层级。
- 强状态表达：运行、停止、失败、风控拒绝、恢复中、重试中、过期、未配置、无权限必须清晰可见。
- 强风控和异常可见性：不得为了页面好看隐藏风险、失败、拒绝、停用、审计和追踪信息。
- 使用 Ant Design 企业后台风格与既有组件模式。
- 禁止营销页式大标题、大渐变、大插画、无意义动效、过度动效和隐藏风险状态。

### 13.6 前端页面验收标准

新增或调整前端页面时，默认检查：

- 有明确业务目标说明。
- 有核心状态摘要。
- 有清晰筛选区、主数据区、详情区、操作区。
- loading / empty / error / disabled / risky operation 状态完整。
- 危险操作有二次确认。
- REAL / LIVE / 风控失败 / 恢复 / 重试 / 停止类操作必须有明确风险提示和影响范围说明。
- 服务端数据使用 TanStack Query；Zustand 只放 auth、account-context 等轻量全局状态。
- 不新增 API，不改后端契约，不新增 migration，除非用户明确要求。

### 13.7 MCP 辅助规则

以下 MCP 只作为辅助，不改变主 skill：

- 前端运行态问题：`chrome-devtools`
- 复杂 CSS / 动画参考：`icss`
- 查询 DB 结构 / 数据：`postgres`
- 本地依赖与容器联调：`MCP_DOCKER`
- 读写普通文件或兜底检索：`filesystem`

### 13.8 输出要求

完成后必须输出：

1. 主 skill 是什么，为什么命中；如未使用 skill，说明原因。
2. 辅助 skill / MCP 是什么，为什么需要；如未使用，说明未使用。
3. 新增文件。
4. 修改文件。
5. 验证步骤。
6. 风险与未覆盖项。
7. 若发现与现有规则冲突，必须说明冲突点，并以现有 Gate 边界、安全边界、技术栈边界优先。
