# DH Stage-QDR-9 B4 Audit Environment Scope Erratum

## 1. Scope decision

~~~text
baseline:
9249bf78a2eaace4c59aedff788e37e083b72b3b

prior effective scope invariants:
54 / 54 PASS

new exact scope-set invariants:
6 / 6 PASS

effective scope invariants:
60 / 60 PASS

selected storage:
OPTION A

selected backfill:
B1

selected migration sequence:
S2

candidate migration:
dh-app/src/main/resources/db/migration/V17__qdr9_audit_environment_storage.sql

technical implementation:
NOT AUTHORIZED
~~~

`60 / 60` 表示保留此前 54 项 invariant，并新增六个 exact subset invariant；它不是 60 个文件。
以下是 future allowlist，只有 V16 predecessor、producer cutover、legacy blocker 和独立 implementation
authorization 全部满足后才能写。本任务未创建任何列出的 NEW 文件。

## 2. B4_AUDIT_ENVIRONMENT_SCHEMA_SCOPE

~~~text
dh-app/src/main/resources/db/migration/V17__qdr9_audit_environment_storage.sql (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentFlywayPostgresTest.java (NEW)
~~~

仅允许 Option A column、两个 exact CHECK、四个 exact non-unique indexes、comments 与 forward-only
V16→V17 upgrade。不得修改 V1–V16。

## 3. B4_AUDIT_ENVIRONMENT_BACKFILL_SCOPE

~~~text
dh-app/src/main/resources/db/migration/V17__qdr9_audit_environment_storage.sql (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentFlywayPostgresTest.java (NEW)
~~~

只允许零行 B1 backfill、historical null preservation 与 no-default/no-inference assertions。不得写历史
`DEV`/`TEST`。

## 4. B4_AUDIT_ENVIRONMENT_WRITE_PATH_SCOPE

### Canonical contract and adapters

~~~text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java (NEW / PREVIOUSLY FROZEN)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/InMemoryDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
~~~

### Producer propagation

~~~text
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/RateLimiter.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
~~~

只允许把同一 verified `FeedbackExecutionScope.environment()` 传播到 canonical `AuditEventRecord` 和 JDBC
column，并删除全部 environmentless invocation。Human Approval 等 producer 若仍无法取得 verified scope，
必须保持 blocked；不授权新增 legacy environmentless record。

## 5. B4_AUDIT_ENVIRONMENT_READ_PATH_SCOPE

~~~text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionReplayAuditEventView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditEventQuery.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditEventView.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditEventQueryPort.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditEventQueryAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionReplayQueryRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapter.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
~~~

新 port 仅为 internal-only tenant/environment-bound audit query。现有 replay adapter 只允许明确 legacy
`environment IS NULL` compatibility projection；不得给 null 补 DEV/TEST。

## 6. B4_AUDIT_ENVIRONMENT_TEST_SCOPE

~~~text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestratorPersistenceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestratorProviderGuardTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditEventQueryAdapterTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionReplayQueryRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapterTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentFlywayPostgresTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
~~~

仅允许新增冻结的 migration、backfill、DEV/TEST write、transaction rollback、read isolation、concurrency
与 architecture assertions；不得降低既有 PostgreSQL/Testcontainers、recovery、repeatability 或 capacity
断言。

## 7. B4_AUDIT_ENVIRONMENT_MIGRATION_SEQUENCE_SCOPE

~~~text
docs/current/DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_FORWARD_MIGRATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_STORAGE_DESIGN.md
docs/current/DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_FORWARD_MIGRATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_SCOPE_ERRATUM.md
dh-app/src/main/resources/db/migration/V17__qdr9_audit_environment_storage.sql (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V17Qdr9AuditEnvironmentFlywayPostgresTest.java (NEW)
~~~

本集合只允许保持 `V16 registry -> V17 audit environment` 的 S2 顺序及其验证。V16 path 由既有
reference-liveness scope 独占，本集合不允许修改或创建 V16。

## 8. Invariants

~~~text
B4_AUDIT_ENVIRONMENT_SCHEMA_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_BACKFILL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_WRITE_PATH_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_READ_PATH_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_MIGRATION_SEQUENCE_SCOPE ⊆ WRITE_ALLOWLIST: PASS

PRIOR EFFECTIVE SCOPE INVARIANTS:
54 / 54 PASS

EFFECTIVE SCOPE INVARIANTS:
60 / 60 PASS

TASK_SCOPE_DESIGN_INVALID:
NO

ALLOW_AUDIT_ENVIRONMENT_MIGRATION_IMPLEMENTATION:
NO

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

## 9. Forbidden scope

本 erratum 不授权修改 Java、tests、POM、V1–V15、candidate V16/V17、config、workflow、API surface、
Controller endpoint、registry、retention、scheduler、automatic learning、B4 review/publication 或 B5。
也不授权真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。
