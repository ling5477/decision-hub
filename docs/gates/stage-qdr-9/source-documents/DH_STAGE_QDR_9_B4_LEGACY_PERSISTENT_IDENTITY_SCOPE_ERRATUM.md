# DH Stage-QDR-9 B4 Legacy Persistent Identity Scope Erratum

## 1. Scope decision

~~~text
baseline:
aab84e896595bbd8b3f5e99e8b2880ca28f8e7a4

prior effective scope invariants:
60 / 60 PASS

new exact scope-set invariants:
6 / 6 PASS

effective scope invariants:
66 / 66 PASS

selected legacy option:
L2

selected tombstone retirement:
T1

migration required:
YES

migration ordering:
V16 -> V17 -> V18 CANDIDATE

migration artifact path:
NOT AVAILABLE / SEQUENCING BLOCKED

technical implementation:
NOT AUTHORIZED
~~~

`66 / 66` 表示保留此前 60 项 invariant，并新增以下六个 exact subset invariant；不是 66 个文件。
本文件冻结 future allowlist 边界，但当前 lifetime 未闭合且 migration artifact 未加入 allowlist，所以
不能据此写任何 Java、test、POM 或 migration。

## 2. B4_LEGACY_PERSISTENT_IDENTITY_CONTRACT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/PersistentGuardIdentity.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/PersistentGuardHardCeilings.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyAdmissionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyAdmissionResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyAdmissionStatus.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyState.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyRecordView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyPersistentIdentityClassification.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyPersistentIdentityRetirementPolicy.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunGuardProperties.java
~~~

只允许 L2 classification、three-cutoff typed proof、unknown block 与 existing hard-ceiling 复用。不得把
request fingerprint、nonce/replay identity 或 deployment environment 当作 trusted namespace。

## 3. B4_LEGACY_PERSISTENT_IDENTITY_RETIREMENT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/GuardCleanupCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/GuardCleanupPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyIdentityRetirementCandidate.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyIdentityRetirementResult.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcGuardCleanupAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcRateLimitAdmissionAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcIdempotencyGuardAdapter.java
~~~

只允许 T1 candidate lock、final recheck、exact delete、typed outcome 和 atomic legacy-check + v2
admission。现有 count-only cleanup contract 必须收窄为可证明的一行 audit 对一行 retirement，不能新增
任意 CRUD/delete。

## 4. B4_LEGACY_PERSISTENT_IDENTITY_CLEANUP_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyPersistentIdentityRetirementService.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/GuardTransactionBoundary.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcGuardCleanupAdapter.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
~~~

只允许 internal-only、default-disabled、one-tenant/one-family、bounded single-batch service 与 required
transaction wiring。不得新增 scheduler、startup invocation、Controller、endpoint、automatic retry 或
unbounded loop。

## 5. B4_LEGACY_PERSISTENT_IDENTITY_AUDIT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyIdentityRetirementAuditRecord.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyIdentityRetirementAuditPort.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcLegacyIdentityRetirementAuditAdapter.java (NEW)
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
~~~

只允许独立 structured immutable audit storage 的 typed contract/adapter/wiring。Schema object 冻结为
`dh_qdr9_legacy_identity_retirement_audit`，但 migration artifact 不在当前 allowlist。不得复用
`dh_decision_audit_event` 伪造 decision/environment，不得把自由文本日志作为唯一证据。

## 6. B4_LEGACY_PERSISTENT_IDENTITY_TEST_SCOPE

~~~text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyPersistentIdentityRetirementPolicyTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/guard/LegacyPersistentIdentityRetirementServiceTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcLegacyIdentityRetirementAuditAdapterTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
~~~

只允许 frozen rate/idempotency/recovery/unknown/cleanup/concurrency/rollback/audit/DEV-TEST isolation
matrix；不得降低 existing PostgreSQL/Testcontainers、repeatability、same-pool recovery 或 capacity
assertions。Migration presence/upgrade tests 必须等 sequencing task 给出 exact V18 path 后另行加入。

## 7. B4_LEGACY_PERSISTENT_IDENTITY_MIGRATION_SEQUENCE_SCOPE

~~~text
docs/current/DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_FORWARD_MIGRATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_STORAGE_DESIGN.md
docs/current/DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_FORWARD_MIGRATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_DESIGN.md
docs/current/DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_SCOPE_ERRATUM.md
~~~

该集合只允许保持 `V16 registry -> V17 audit environment -> V18 legacy identity candidate` 的顺序、
blocker 与 schema semantic reservation。禁止在 exact artifact path、clean/upgrade tests 和独立
sequencing acceptance 之前创建或命名 V18 migration。

Exact schema semantic reservation：

~~~text
versioned rate identity namespace
versioned idempotency/recovery identity namespace
verified DEV/TEST structured environment
legacy environment remains unknown / no backfill
dh_qdr9_legacy_identity_retirement_audit
bounded retirement indexes/checks/comments
V1-V17 checksum preservation
~~~

## 8. Invariants

~~~text
B4_LEGACY_PERSISTENT_IDENTITY_CONTRACT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_LEGACY_PERSISTENT_IDENTITY_RETIREMENT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_LEGACY_PERSISTENT_IDENTITY_CLEANUP_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_LEGACY_PERSISTENT_IDENTITY_AUDIT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_LEGACY_PERSISTENT_IDENTITY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_LEGACY_PERSISTENT_IDENTITY_MIGRATION_SEQUENCE_SCOPE ⊆ WRITE_ALLOWLIST: PASS

PRIOR EFFECTIVE SCOPE INVARIANTS:
60 / 60 PASS

EFFECTIVE SCOPE INVARIANTS:
66 / 66 PASS

TASK_SCOPE_DESIGN_INVALID:
NO

LIFETIME_SCOPE:
BLOCKED / THREE HARD CEILINGS UNRESOLVED

MIGRATION_ARTIFACT_SCOPE:
BLOCKED / VERSIONED PATH NOT AUTHORIZED

ALLOW_LEGACY_IDENTITY_IMPLEMENTATION:
NO

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

## 9. Forbidden scope

本 erratum 不授权修改 Java、tests、POM、V1–V15、candidate V16/V17/V18、config、workflow、API
surface、Controller、registry、retention、scheduler、automatic learning、B4 review/publication 或 B5。
也不授权真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。Legacy replay namespace 继续由独立 blocker
处理，不得夹带到 persistent retirement implementation。
