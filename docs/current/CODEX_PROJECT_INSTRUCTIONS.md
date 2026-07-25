# Codex Project Instructions
## Terminal current authority — 2026-07-25 Stage-QDR-9 B2 transactional persistence local accepted

```text
Implementation baseline: 20fe2f54ebf043c8c80f841db856ae35bc4f61a
Branch: dev
B1 implementation commit: 80b21f31bdb5dc7a15f327aa9617b0640fd1f22b
B1 scope-repair/review commit: 64a0757175e81720b2092c0a67302d619eb49291
B1 publication: PASS / FAST_FORWARD
B1 exact-SHA CI: PASS / RUN 30104473274
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-9 plan: DONE / PUBLISHED
Stage-QDR-9 implementation work order: FROZEN / ACCEPTED
Stage-QDR-9 overall: IN_PROGRESS / B1 CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B1: CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B2: IMPLEMENTED / LOCAL_ACCEPTED
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE + HISTORICAL_EVIDENCE_READ_MODEL
Current highest migration: V15
V15: PUBLISHED / EXACT_SHA_CI_ACCEPTED
Initial milestone review: BLOCKED / TASK_SCOPE_DESIGN_INVALID
Scope erratum: DONE / 2 EXACT LEGACY TESTS ADDED
Effective scope invariants: PASS / 9 OF 9
Final milestone review: PASS
Domain persistence contracts: PASS
V15 schema / clean migration / V14-to-V15 upgrade / constraints: PASS / PASS / PASS / PASS
Legacy V12/V13 migration compatibility: PASS / EXPLICIT V14 TARGET
Remote regression: PASS / 19 OF 19 REACTOR / 1208 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
ArchitectureTest: PASS
Checkstyle: 0
Spotless: PASS
B2 JDBC repository: IMPLEMENTED / FOUR V15 RELATIONS / TENANT_AND_ENVIRONMENT_SCOPED
Transactional aggregate write: PASS / REQUIRED + REPEATABLE_READ / ATOMIC ROLLBACK
Database idempotency: PASS / UNIQUE_CONSTRAINT_DECISIVE
Same key / same hash: REUSED / COMPLETE_AGGREGATE
Same key / different hash: IDEMPOTENCY_CONFLICT
Duplicate-key concurrency: PASS / 16 WORKERS / ONE PHYSICAL AGGREGATE
Commit outcome unknown: FAIL_CLOSED / NO_RETRY / READ_ONLY_RECONCILIATION_ONLY
Reference validation: PASS / AUDIT_REPLAY_EVALUATION_TENANT_BOUND / EVIDENCE_BOUND
PostgreSQL restart persistence: PASS / RESTART + RECONNECT + SAME_KEY_REUSED
B2 local targeted tests: PASS / 12 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
B2 local module regression: PASS / DH-USECASE 9 OF 9 + DH-INFRA,DH-APP 15 OF 15 REACTOR
B2 local full regression: PASS / 19 OF 19 REACTOR / 1220 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
B2 PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
B2 architecture guards: PASS / ArchitectureTest + StageQdr9FeedbackArchitectureTest
B2 local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Remote exact-SHA CI: PENDING NEW B2 COMMIT
JDBC Repository implementation: IMPLEMENTED
Historical read model: NOT_STARTED
Retention: NOT_STARTED
API / Automatic learning: NOT_ALLOWED / NOT_ALLOWED
Terminal current factsources: 12
Scope invariants: PASS / 13 OF 13
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 B2 AUTHORITY HASH / 0 CONFLICTS
B2 milestone review: REQUIRED BEFORE B3
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
current task: DH-STAGE-QDR-9-B2-JDBC-PERSISTENCE-AND-TRANSACTIONAL-IDEMPOTENCY-IMPLEMENTATION
current task status: DONE / LOCAL_ACCEPTED
next action: DH-STAGE-QDR-9-B2-MILESTONE-REVIEW
ALLOW_B2_MILESTONE_REVIEW: YES
ALLOW_B3_IMPLEMENTATION_NOW: NO / B2 MILESTONE REVIEW REQUIRED
ALLOW_API_CHANGE_NOW: NO
ALLOW_AUTOMATIC_LEARNING: NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Codex 下一动作仅为 `DH-STAGE-QDR-9-B1-MILESTONE-REVIEW`。B1 已在 9/9 scope 内完成并通过本地技术验收；review 前不得开始 B2，不得实现 JDBC adapter、historical read model、retention、API 或 automatic learning。

## Previous terminal authority — 2026-07-23 Stage-QDR-7/QDR-8 archive source recovery and pruning CI fix

```text
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-7 archive: docs/gates/stage-qdr-7/
Stage-QDR-7 tag: dh-stage-qdr-7-close
Stage-QDR-7 tag target: 6acf9c332434cafb45495d58f063a0f3faeaf475
Stage-QDR-7 source documents: RECOVERED / 30 OF 30
Stage-QDR-7 source documents path: docs/gates/stage-qdr-7/source-documents/
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8 archive: docs/gates/stage-qdr-8/
Stage-QDR-8 tag: dh-stage-qdr-8-close
Stage-QDR-8 tag target: 7b6066d1062fe49d16d1f093c8b3170354854376
Stage-QDR-8 source documents: RECOVERED / 3 OF 3
Stage-QDR-8 source documents path: docs/gates/stage-qdr-8/source-documents/
Pruning commit: PUBLISHED / 0c0c60c531b6143ad50ba3c62a86d5af3d031aa8
Historical failed CI: 29930851492 / QDR7_CAPACITY_SOURCE_DOCUMENT_PRUNED
Capacity machine contract: MIGRATED_TO_STABLE_CONFIG_PATH
Capacity machine contract path: config/qdr7-capacity/qdr7-capacity-acceptance-criteria.md
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Local targeted tests: PASS / 9 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
Local ArchitectureTest: PASS / 40 OF 40
Local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Terminal current factsources: 12
Scope invariants: PASS / 7 OF 7
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 BLOCK HASH / 0 CONFLICTS
current task: DH-STAGE-QDR-7-QDR8-ARCHIVE-SOURCE-RECOVERY-AND-PRUNING-CI-FIX
current task status: DONE / LOCAL_ACCEPTED
next action after local acceptance: OBTAIN FIX COMMIT PUSH AUTHORIZATION AND RUN EXACT-SHA CI
Stage-QDR-9: NOT_STARTED
ALLOW_FIX_COMMIT_PUBLICATION: YES / SEPARATE_EXPLICIT_AUTHORIZATION_REQUIRED
ALLOW_STAGE_QDR_9_PLAN_NOW: NO / FIX_COMMIT_PUBLICATION_AND_EXACT_SHA_CI_REQUIRED
ALLOW_STAGE_QDR_9_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE / ALLOW_MIGRATION / ALLOW_REPOSITORY_EXPANSION: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本任务完成 post-tag archive source completion，并把 QDR-7 capacity machine contract 迁移到稳定 `config` 路径。Stage-QDR-7/8 的 current pruning、archive 和 annotated tag 状态保持不变；B2 capacity 继续为 `DEFERRED / KNOWN_LIMITATION`，production capacity 继续为 `NOT_PROVEN`，Stage-QDR-9 继续为 `NOT_STARTED`。

## Stable machine contract and source archive governance

1. `docs/current` 只承担当前状态和入口权威，不承担长期机器合同。
2. `config`、`src/main`、`src/test`、CI、`scripts`、`deploy` 不得依赖可被 post-tag pruning 删除的 `docs/current` stage process document。
3. 长期机器可执行合同必须位于 `config/**`、`contracts/**` 或其他明确不可裁剪目录。
4. 阶段原始执行文档必须在 pruning 前完整复制到 `docs/gates/<stage>/source-documents/`。
5. Annotated tag 提供不可变恢复能力，但不能替代工作树中的 searchable archive source packet。
6. Post-tag pruning 前必须扫描 `config`、`src/main`、`src/test`、`.github`、`scripts`、`deploy` 对全部 pruning candidates 的引用。
7. 如果 pruning candidate 仍被机器依赖，必须先迁移依赖，再允许删除 current 文件。
8. Aggregate summary、final review 或 archive manifest 不能替代原始 plan、WO、retry、blocker 和 validation document。
## Historical pre-pruning authority — 2026-07-22 Stage-QDR-7/QDR-8 sequence repair local ready

```text
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
Stage-QDR-7 archive recovery: RETROSPECTIVE / GOVERNANCE_SEQUENCE_REPAIR
Stage-QDR-7 archive commit: 6acf9c332434cafb45495d58f063a0f3faeaf475
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
Structured feedback attribution: IMPLEMENTED / DETERMINISTIC / DECISION_BOUND / TENANT_BOUND / ENVIRONMENT_BOUND
Attribution safety: AUDITABLE / REPLAY_REFERENCE_SAFE / IDEMPOTENT / NO_SIDE_EFFECT
Persistence / API / runtime expansion: NONE
Implementation parent: 0fae8b3ee3da197c32ac8bc2d13ce9e3ba0e86a3
Implementation commit: 1279f1a0a246807e019bd2223c0f7254d50b74d5
Implementation publication: PASS
Implementation exact-SHA CI: 29836489131 / PASS
Remote regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
Remote ArchitectureTest: PASS / 40 OF 40
Remote quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
QDR-7 archive recovery quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
QDR-8 replay local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
QDR-8 replay commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
QDR-8 replay exact-SHA CI: PENDING_PUBLICATION
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
QDR-7 archive: COMPLETED / docs/gates/stage-qdr-7/
QDR-8 archive: COMPLETED / docs/gates/stage-qdr-8/
QDR-7 tag: NOT_CREATED / PENDING_NEW_HEAD_EXACT_SHA_CI
QDR-8 tag: NOT_CREATED / PENDING_QDR_7_TAG
current task: DH-STAGE-QDR-7-RETROSPECTIVE-ARCHIVE-TAG-AND-QDR8-SEQUENCE-REPAIR
current task status: LOCAL_SEQUENCE_REPAIRED / PENDING_PUBLICATION_CI_AND_TAGS
next action: FAST_FORWARD_PUBLISH; RUN_NEW_HEAD_EXACT_SHA_CI; CREATE_QDR7_THEN_QDR8_TAGS
ALLOW_CLOSE_COMMIT_PUBLICATION: YES / EXPLICITLY_AUTHORIZED
ALLOW_STAGE_QDR_7_TAG_AFTER_EXACT_SHA_CI: YES / EXPLICITLY_AUTHORIZED
ALLOW_STAGE_QDR_8_TAG_AFTER_QDR_7_TAG: YES / EXPLICITLY_AUTHORIZED
ALLOW_POST_TAG_CURRENT_PRUNING: NO / SEPARATE_TASK_ONLY
ALLOW_STAGE_QDR_9_PLAN_NOW: NO / TAG_CLOSE_AND_CURRENT_PRUNING_REQUIRED
ALLOW_API_CHANGE / ALLOW_MIGRATION / ALLOW_REPOSITORY_EXPANSION: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Stage-QDR-7 retrospective final close/archive 已由 `6acf9c332434cafb45495d58f063a0f3faeaf475` 恢复，未重写已发布历史，B2 capacity 继续 `DEFERRED / KNOWN_LIMITATION`；Stage-QDR-8 final close 内容已在其后重放为当前文档提交。两阶段 archive 均完成但 tag 尚未创建，必须等待新 QDR-8 HEAD fast-forward 发布与 exact-SHA CI 全绿后按 QDR-7 → QDR-8 顺序创建。结构化 feedback attribution 仍仅为 domain/usecase foundation，不形成在线学习闭环或真实外部副作用。

## Previous state — 2026-07-21 Stage-QDR-8 implementation local accepted

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
Stage-QDR-8 plan commit: PUBLISHED / 0fae8b3ee3da197c32ac8bc2d13ce9e3ba0e86a3
Stage-QDR-8 planning exact-SHA CI: PASSED / ACCEPTED / RUN 29830659396
Planning remote regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Stage-QDR-8 implementation: IMPLEMENTED / LOCAL_ACCEPTED
Structured feedback attribution: DETERMINISTIC / DECISION_BOUND / TENANT_BOUND / ENVIRONMENT_BOUND
Attribution safety: AUDITABLE / REPLAY_REFERENCE_SAFE / IDEMPOTENT / NO_SIDE_EFFECT
Persistence / API / runtime wiring: NOT_ADDED
Implementation baseline: 0fae8b3ee3da197c32ac8bc2d13ce9e3ba0e86a3
Implementation commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
Local regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
Local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Remote CI: PENDING NEW IMPLEMENTATION COMMIT
Stage-QDR-8 final close: PENDING EXACT_SHA CI
current task: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
current task status: DONE / LOCAL_ACCEPTED
next action: OBTAIN IMPLEMENTATION PUSH AUTHORIZATION; FAST-FORWARD PUSH; RUN EXACT-SHA TEST + QUALITY CI
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_STAGE_QDR_8_IMPLEMENTATION: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_EXACT_SHA_CI: YES / AFTER PUSH AUTHORIZATION
ALLOW_STAGE_QDR_8_FINAL_CLOSE: NO / EXACT_SHA_CI_REQUIRED
ALLOW_API_CHANGE / ALLOW_MIGRATION / ALLOW_REPOSITORY_EXPANSION: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Stage-QDR-8 implementation scope 已消费；不得继续扩大实现，下一独立任务仅允许获得 push 授权后验证实施提交 exact-SHA CI。

## Historical planning authority — 2026-07-21 exact-SHA CI accepted and Stage-QDR-8 plan frozen

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
Collector concurrency contract: PASS / 10 ROUNDS / 16 WRITERS / 1000 SAMPLES EACH / EXACT COUNT / 0 DUPLICATES
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
ALLOW_EXACT_SHA_CI: NO / CONSUMED_ACCEPTED
ALLOW_STAGE_QDR_8_PLANNING: NO / CONSUMED_ACCEPTED
ALLOW_STAGE_QDR_8_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CURRENT_TASK_IS_PLANNING_ONLY
ALLOW_API_CHANGE_NOW / ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME_INTEGRATION: NO / NO / NO
ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

下一任务只能按冻结 work order 实施 Stage-QDR-8 domain/usecase foundation；不得扩大到 API、migration、Repository、runtime wiring 或真实外部能力。

## Historical pre-publication authority — 2026-07-21 same-pool recovery test concurrency fix local accepted

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

## Historical terminal authority — 2026-07-20 Stage-QDR-7 B3 final close accepted

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

B3 final close 已消费；不得创建 B3.1/B3.2/B3.3 review、新 B3 scope、B2 task、Retry-5 或 capacity harness 修复任务。后续只允许 planning/work-order freeze，不授权真实 HTTP、Provider、NQ、Agent、LangGraph、Paper、LIVE 或下一阶段实现。

## Historical B3 implementation local acceptance — consumed by final close

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
Final formal preflight: BLOCKED / 25 OF 26 PASS
Final formal scenarios: STARTED 0 / COMPLETED 0 / NOT_STARTED 15
Final formal thresholds: EXECUTED 0 / NOT_EVALUATED 94
Final formal artifacts: PASS / 27 FILES / 26 MANIFEST ENTRIES / 0 MISMATCH / 0 SECRET FINDINGS / TEARDOWN PASS
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

B3 implementation 已按冻结任务 ID 完成并通过本地验收。不得重新打开 B2、重跑 formal、创建 Retry-5，也不得扩展 API、migration、Repository、真实 HTTP、Provider、NQ、Agent、LangGraph、Paper 或 LIVE。后续只允许取得 push 授权后推送 exact commit、运行 exact-SHA 远端 test + quality CI，再执行 B3 milestone final close。

## Historical pre-final authority — consumed by final Retry-4

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
ALLOW_CAPACITY_THRESHOLD_EVIDENCE_RETRY_2: NO / CONSUMED_BLOCKED
ALLOW_CAPACITY_HARNESS_WORK_ORDER: NO / CONSUMED_ACCEPTED
ALLOW_CAPACITY_HARNESS_IMPLEMENTATION_NOW: NO / CONSUMED_ACCEPTED
ALLOW_CAPACITY_ACCEPTANCE_EXECUTION_NOW: NO / EXACT-SHA CI THEN FINAL RETRY-4 FORMAL ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_RETRY_3: NO / CONSUMED_BLOCKED
ALLOW_CAPACITY_HARNESS_CI_VERIFICATION: NO / CONSUMED_ACCEPTED
ALLOW_FORMAL_RETRY_4: CONDITIONAL / EXACT-SHA CI PASS REQUIRED / FINAL ATTEMPT ONLY
ALLOW_CAPACITY_ENVIRONMENT_BLOCKER: NO / CONSUMED_ACCEPTED
ALLOW_CAPACITY_HARNESS_DEFECT_CORRECTION: NO / CONSUMED_ACCEPTED
ALLOW_EXACT_SHA_CI: YES / NEXT ACTION ONLY
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
CURRENT_TASK: DH-STAGE-QDR-7-B2-SUREFIRE-FORK-STARTUP-STABILIZATION
CURRENT_TASK_STATUS: CLOSED / ACCEPTED
NEXT_ACTION: EXACT-SHA REMOTE TEST + QUALITY CI; THEN FINAL ORIGINAL RETRY-4 FORMAL ACCEPTANCE
RETRY_5: NOT_ALLOWED
POST_FINAL_FORMAL_HARNESS_FIX_CHAIN: NOT_ALLOWED
```

schema errata、persistent guards与B2 milestone保持`ACCEPTED`。本地Surefire fork startup stabilization已通过完整验收；下一步只允许新提交的exact-SHA远端test + quality CI，通过后执行最后一次原Retry-4 formal。不得创建Retry-5或后续harness微型修复链；当前不授权B3、API、外部HTTP/provider、NQ、Agent/LangGraph或LIVE。

> 项目: Decision Hub
> 必需前置 skill: `nq-dh-workflow-router`
> 必需文档 skill: `dh-docs-writer`
> 当前事实源: `docs/current`
> 当前工作区: 每轮用 `Get-Location` 确认；不得把本机盘符路径写成唯一事实

## 1. 当前状态与已关闭历史摘要

下方Stage-QDR-2至Stage-QDR-6条目均为historical/consumed摘要，不是当前任务或next action。Stage-QDR-7当前状态只以本节末尾的`CURRENT_TASK`/`NEXT_TASK`及文件顶部authority为准。

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
STAGE_QDR_4_B3_PLAN: DONE
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
stage-qdr-4 B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES / CONSUMED
stage-qdr-4 B4 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
stage-qdr-4 B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO / ALREADY_DONE
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES / CONSUMED
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B2_CI: FIXED / ARCHITECTURE_SOURCE_SCAN_FIXED
STAGE_QDR_5_B2_CI_BLOCKER_FIX: DONE
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: COMPLETE / B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE_COMMITTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS / 5961164
CURRENT_FACTSOURCE_CONFLICT: CLEARED
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
STAGE_QDR_7: IMPLEMENTING / B1_FROZEN / B2_CLOSED_ACCEPTED
STAGE_QDR_7_B1: FROZEN
STAGE_QDR_7_B2: CLOSED / ACCEPTED
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION: ACCEPTED
STAGE_QDR_7_B2_PERSISTENT_GUARDS_IMPLEMENTATION: ACCEPTED
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET_NOW: YES / CONSUMED
ALLOW_STAGE_QDR_6_TAG_CLOSE_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_7_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO / POST_B2_CAPACITY_ACCEPTANCE_IS_ACCEPTANCE_ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO / BLOCKED_BY_THRESHOLD_EVIDENCE_RETRY
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO / ALL_IN_ONE_FORBIDDEN
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
CURRENT_TASK: DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER
CURRENT_TASK_STATUS: CLOSED / ACCEPTED
NEXT_TASK: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY
MODE: CODE_AND_EVIDENCE_FIX + ACTUAL_WIRING_VALIDATION + LOCALHOST_PROTECTED_HTTP + REAL_POSTGRESQL + FULL_REGRESSION + CURRENT_FACTSOURCE_SYNC + NO_MIGRATION_CHANGE + NO_API + NO_PROVIDER + NO_AGENT + NO_LIVE
```

## 2. 前置分类规则

每个 Codex 任务必须先使用 `nq-dh-workflow-router` 分类，再决定 skill、scope、文件范围和验证命令。DH 文档治理、`docs/current`、archive、work order、acceptance、freeze、close review、WORKLOG、TESTING、STATUS、ROADMAP、API 与 DB_SCHEMA 同步必须使用 `dh-docs-writer`。

固定输出字段：

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

`Summary` 不是必填字段。

## 3. 当前事实源规则

`STATUS.md`与`WORK_ORDER.md`是当前状态和下一任务的主权威。README、CODEX、CLAUDE、AGENTS是入口或执行指导，不得覆盖这两个主权威。当前factsource集合包括：

```text
README.md
CLAUDE.md
AGENTS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
```

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

### 3.1 任务scope设计与review收口

所有任务在实施前必须满足：

```text
VALIDATION_SCOPE ⊆ READ_SCOPE
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST
```

任一包含关系不成立时不得开始实施，必须输出`TASK_SCOPE_DESIGN_INVALID`。

Review收口规则：

1. 技术验收已通过且唯一阻断为current docs漂移时，在同一任务内修复文档并完成最终验收。
2. 禁止继续创建`docs fix -> review -> docs fix -> review`循环。
3. `STATUS.md`和`WORK_ORDER.md`冲突可以阻断阶段close。
4. 其他入口漂移必须修复，但不得自动降级为migration、事务或安全实现失败。
5. 只有新的真实P0/P1代码、安全、tenant、事务、migration或API问题，才能阻断技术acceptance。
6. 要求`current conflict count = 0`的文件必须全部进入任务`WRITE_ALLOWLIST`。

Stage-QDR-4至Stage-QDR-6 archive以及已完成的Stage-QDR-7 plan/work order只作为historical/consumed evidence，不覆盖current factsources。未被当前authority显式列为current task的task-specific文档，一律按历史快照解释。

以下文件默认 supporting only，不作为 primary stage gate source：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/ARCHIVE_INDEX.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

归档历史默认不作为 blocker：

```text
docs/gates/**
docs/archive/** 仅当历史遗留目录存在时使用；QDR 当前归档标准不是 docs/archive
```

只有 `FACTSOURCE_POLICY.md` 定义的硬错误可让supporting docs升级为blocker。Stage-QDR-7 B1为`FROZEN`；B2、schema errata与persistent guards均已`ACCEPTED`。Calibration path blocker、PromptVersion atomic bootstrap、cold-start quota、tenant cleanup、same-pool recovery与Surefire sampling缺口已关闭；candidate threshold evidence为`CLOSED / SUFFICIENT`，criteria为`FROZEN / ACCEPTED`。Harness work order已`CLOSED / ACCEPTED`，下一任务只允许formal harness implementation；capacity acceptance、B3、API、Provider、外部HTTP、Agent、LangGraph、NQ runtime integration与LIVE仍未授权。

## 4. 安全边界

```text
DH 不直接下单
DH 不绕过 NQ 风控
DH 不修改 NQ 订单状态
DH 不读写 NQ DB
DH 不访问交易所密钥
DH 不启动 Paper Run
DH 不接真实 HTTP
DH 不接真实 provider
DH 不引入 Provider SDK
DH 不启动 Agent / LangGraph runtime
DH 不启用 LIVE
```

`LONG_BIAS / SHORT_BIAS` 只是 bias，不得映射成 `BUY / SELL`。`APPROVED` 不是 `BUY`，`REJECTED` 不是 `SELL`。Gateway result 只能作为只读 evidence / reasoning summary，不得触发交易、approval mutation、NQ mutation、risk mutation、ledger mutation、paper 或 live mutation。

## 5. 文档语言与路径规则

正文使用简体中文。类名、字段名、状态枚举、HTTP header、命令、路径和外部技术名保留英文原样。当前路径必须由每轮 `Get-Location` 确认，不得硬编码本机盘符路径。若历史文档出现旧路径，只能作为 historical record，不得覆盖当前工作区。

## 6. 验证纪律

docs-only 治理任务至少运行：

```powershell
git status --short
git diff --check
git diff --stat
```

本轮 QDR B1 / B2 / B3 边界治理还必须运行：

```powershell
git diff --name-only
git diff --cached --name-only
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍不可写成可用。Docker/Testcontainers skip 只能写成环境型 skip，不得写成 PASS。

## 7. Historical / Consumed entries（非当前）

以下内容是Stage-QDR-4至Stage-QDR-7早期阶段的historical/consumed快照。即使原始字段包含`next action`或`IMPLEMENTATION_NOT_STARTED`，也不得解释为当前任务；当前入口只看文件顶部authority、`STATUS.md`和`WORK_ORDER.md`。

```text
previous task: DH-STAGE-QDR-4-ARCHIVE-CONTENT-FIX / DONE
archive source: docs/gates/stage-qdr-4/
B4 plan source: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
B4 implementation WO source: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
previous B3 plan source: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
previous B3 work order source: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
B2 freeze/review: PASS
B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
B2 close review: PASS
B2 status: CLOSED / ACCEPTED
B3 plan: DONE / PLAN_ONLY
B3 implementation work order: DONE / WORK_ORDER_ONLY
B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
B3 close review: PASS
B3 status: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
B4 implementation work order: DONE / WORK_ORDER_ONLY
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO / ALREADY_DONE
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES / CONSUMED
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO / ALL_IN_ONE_FORBIDDEN
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
V9 migration: CREATED / V9__qdr_replay_evaluation_baseline.sql / POSTGRES_LOAD_VERIFIED
Repository / JDBC implementation: DONE / TENANT_BOUND
API / Controller: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
STAGE_QDR_6_IMPLEMENTATION: COMPLETE
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED / HISTORICAL_SNAPSHOT
STAGE_QDR_7_PLAN: DONE / PLAN_ONLY
ALLOW_STAGE_QDR_7_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
historical next action: DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER / CONSUMED
```
