# DH Stage-QDR-9 B4 Reference-Liveness Forward-Migration Work Order

## 1. 任务状态与前置

```text
task: DH-STAGE-QDR-9-B4-REFERENCE-LIVENESS-FORWARD-MIGRATION-IMPLEMENTATION-RETRY
status: BLOCKED / PRODUCER_ENVIRONMENT_SOURCE_REQUIRED
baseline HEAD and origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3
current highest migration: V15
candidate migration: V16__qdr9_reference_liveness_state_model.sql
candidate state: NOT_CREATED
B4 milestone review retry: NOT_ALLOWED UNTIL THIS WORK ORDER PASSES
```

本工单实施统一 `qdr_reference_liveness` source-of-truth；它不修改 V1–V15，不重解释 `event_status` 或 `verdict`，不新增 API、scheduler、automatic learning 或任何外部 runtime。

## 1.1 Producer Environment Contract Blocker

~~~text
V16 implementation attempt: BLOCKED BEFORE CODE WRITE
blocker: REPLAY/EVALUATION/AUDIT lack explicit environment propagation
no default or inference used: YES
new cross-module scope required: YES
producer environment scope invariants: 31 / 31 PASS
AUDIT / REPLAY / EVALUATION contract: FROZEN / LOCAL_ACCEPTED
AUDIT / REPLAY / EVALUATION source: UNRESOLVED / FAIL_CLOSED
V16: NOT CREATED
ALLOW_V16_IMPLEMENTATION_RETRY: NO
next action: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER
~~~

既有 27-file scope 没有覆盖所有 producer 的 earliest caller、environment command/record 传播和 atomic coordinator。新增文件清单只在 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SCOPE_WORK_ORDER.md 中冻结；任何 source-blocker 后续发现的额外路径必须另行 scope retry，不能在 V16 implementation 中隐式扩张。

## 1.2 Producer Environment Source Blocker

后续全量 root caller 审计已选定唯一 `FeedbackExecutionScope` 类型、三类 root-inward 传播链与原子事务 owner，但没有找到可接受的实际 environment authority：

```text
AUDIT production roots:
DecisionDryRunController.decide
HumanApprovalPacketController.createApprovalPacket
HumanApprovalPacketController.submitApprovalDecision

AUDIT caller-supplied DEV/TEST:
ABSENT

REPLAY/EVALUATION production caller and Spring wiring:
ABSENT

guard property / Spring profile / default DEV:
FORBIDDEN SOURCE

source-resolution exact scope:
36 / 36 PASS

PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION:
BLOCKED

ALLOW_V16_IMPLEMENTATION_RETRY:
NO

next action:
DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
```

36/36 只证明 caller、传播、事务和测试文件已完整序列化；V16 仍不得创建。必须先由独立 upstream contract 任务证明 controller/internal production caller 能显式提供 canonical DEV/TEST，且不引入 profile/config/tenant/ID inference。

## 2. V16 设计冻结

V16 仅在下一独立 implementation 任务创建。DDL 必须创建 `qdr_reference_liveness`，包含：

```text
id uuid primary key
tenant_id varchar(128) not null
environment varchar(16) not null check (environment in ('DEV','TEST'))
reference_type varchar(16) not null check (reference_type in ('AUDIT','REPLAY','EVALUATION'))
canonical_target_key varchar(256) not null
lifecycle_state varchar(16) not null check (lifecycle_state in ('ACTIVE','SAFE_INACTIVE'))
native_status varchar(64) null
state_version bigint not null check (state_version >= 1)
state_changed_at timestamptz not null
created_at timestamptz not null default transaction_timestamp()
updated_at timestamptz not null default transaction_timestamp()
unique (tenant_id, environment, reference_type, canonical_target_key)
```

必须创建 full-scope lookup / lock index `(tenant_id, environment, reference_type, canonical_target_key)`，以及 mismatch-detection index `(tenant_id, reference_type, canonical_target_key)`。DDL 必须有表和关键列 comment。禁止 tenant-only unique、跨环境唯一、默认 `SAFE_INACTIVE`、破坏性删除或对 V1–V15 的改写。

Backfill 只能插入由新 explicit producer context 能够证明的 `ACTIVE` row；对历史 AUDIT、REPLAY、EVALUATION target 不得从 JSON、event result、verdict、存在性或 RELEASED snapshot 推断 environment/lifecycle。无法证明的历史数据保持无 registry row，因而 retention blocked。

## 3. 冻结的 exact WRITE_ALLOWLIST

### B4_REFERENCE_LIVENESS_CONTRACT_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveReplayCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveEvaluationCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/snapshot/CanonicalReplaySnapshotWriteCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/snapshot/CanonicalReplaySnapshotRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/snapshot/CanonicalReplaySnapshotAssembler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceValidationPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessPort.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessRecords.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionService.java (NEW)
```

### B4_REFERENCE_LIVENESS_MIGRATION_SCOPE

```text
dh-app/src/main/resources/db/migration/V16__qdr9_reference_liveness_state_model.sql (NEW)
```

### B4_REFERENCE_LIVENESS_PRODUCER_SCOPE

```text
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcCanonicalReplaySnapshotRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcReferenceLivenessTransactionBoundary.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
```

Each producer must pass an explicit `FeedbackEnvironment` to the liveness writer; no caller may infer it from native IDs, tenant, JSON or existing status. The registry writer creates `ACTIVE` with the native target inside one DH PostgreSQL transaction. The transition service is internal-only and target-type checked; retention is never a writer.

### B4_REFERENCE_LIVENESS_RETENTION_FIX_SCOPE

```text
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceErrorCode.java
```

### B4_REFERENCE_LIVENESS_TEST_SCOPE

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecordsTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionServiceTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/snapshot/CanonicalReplaySnapshotRecordTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayEvaluationPersistenceRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcCanonicalReplaySnapshotRepositoryTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapterTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessFlywayPostgresTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
```

### B4_REFERENCE_LIVENESS_WIRING_SCOPE

```text
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

## 4. Scope invariants

```text
original: 8 / 8 PASS
B1: 9 / 9 PASS
B2: 13 / 13 PASS
B3: 17 / 17 PASS
B4: 21 / 21 PASS
B4_REFERENCE_LIVENESS_CONTRACT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_REFERENCE_LIVENESS_MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_REFERENCE_LIVENESS_PRODUCER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_REFERENCE_LIVENESS_RETENTION_FIX_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_REFERENCE_LIVENESS_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_REFERENCE_LIVENESS_WIRING_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TOTAL: 27 / 27 PASS
```

The future task must reject every file outside this serialization with `STAGE_QDR_9_B4_P1_FIX_SCOPE_EXPANSION_REQUIRED`; no new scope erratum is implied by this work order.

## 5. Implementation sequence and transaction contract

1. Run migration preflight: V15 must remain the highest existing migration, then add V16 once.
2. Create registry schema and conservative backfill; clean migration and V15-to-V16 upgrade must both pass.
3. Add typed liveness records/port and transaction boundary.
4. Modify AUDIT, REPLAY and EVALUATION producers so target insertion and registry `ACTIVE` creation are atomic on the same datasource.
5. Add explicit internal `ACTIVE ↔ SAFE_INACTIVE` transition with `FOR UPDATE`, version increment and environment/type/key validation.
6. Make B2 validation require a matching registry row for new references; legacy rows without one remain fail-closed.
7. Make retention revalidate native identity and registry lifecycle under `FOR SHARE` immediately before delete; only the explicit per-type safe allowlist permits deletion.

Producer updates and retention share the DH PostgreSQL transaction manager. Retention holds the aggregate candidate lock then registry `FOR SHARE`; a producer transition locks the same registry row `FOR UPDATE`. Timeout, query failure, missing row, mismatch, version miss or unknown state must produce no deletion and no success audit.

## 6. Frozen test matrix

| Area | Mandatory coverage |
| --- | --- |
| Migration | V1→V16 clean, V15→V16 upgrade, V1–V15 checksum preservation, once-only application, constraints, unique scope and indexes. |
| Backfill | deterministic explicit environment only; unresolved remains blocking; no row defaults to SAFE_INACTIVE; active legacy reference remains blocked. |
| Producers | AUDIT/REPLAY/EVALUATION create ACTIVE, close SAFE_INACTIVE, reactivate ACTIVE, `state_version` increment, tenant/environment preservation, unsupported transition rejection. |
| P1 | Per type: RELEASED+ACTIVE block; RELEASED+SAFE_INACTIVE eligible; environment mismatch, missing registry, unknown state, lookup failure and timeout block. |
| Concurrency | retention vs reactivation/release; two retention workers; lock behavior; no active aggregate delete; no partial aggregate delete; no false success audit. |
| Boundaries | retention default disabled; no API/scheduler/automatic learning/provider/NQ/Agent/LangGraph/trading side effects. |

## 7. Required acceptance and next gate

The future implementation must run targeted PostgreSQL 17.10 Testcontainers tests, `mvn -B -ntp -pl dh-usecase -am test`, `mvn -B -ntp -pl dh-infra,dh-app -am test`, `mvn -B -ntp test`, and `mvn -B -ntp -Pquality validate`. Only after all pass may the separate `DH-STAGE-QDR-9-B4-MILESTONE-REVIEW-RETRY` be authorized.

## 8. Upstream contract prerequisite

V16 仍为候选且未创建。前置条件已从泛化的 producer source resolution 收紧为已冻结的 U1/U2 authority contract：

~~~text
U1: AUDIT OPTION B signed tenant/source/environment authority + FeedbackExecutionScope + fail-closed propagation
U2: REPLAY/EVALUATION dormant explicit root-command contracts without production wiring
EFFECTIVE_UPSTREAM_SCOPE: 42 / 42 PASS
ALLOW_V16_IMPLEMENTATION: NO
~~~

只有 U1/U2 代码、security vectors、rollback tests 与 architecture guards 通过独立技术验收后，才可另行申请 V16 migration creation。当前不创建 V16、不实施 registry、不实施 retention。
