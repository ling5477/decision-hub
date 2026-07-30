# DH Stage-QDR-9 B4 Identity and Replay Namespace Scope Erratum

## 1. Scope decision

~~~text
baseline:
b0ff11e4057077ad7e0fe91d691116f069dc744e

prior effective upstream scope invariants:
48 / 48 PASS

new exact scope-set invariants:
6 / 6 PASS

effective scope invariants:
54 / 54 PASS

technical implementation:
NOT AUTHORIZED

migration path:
UNRESOLVED / SEPARATE SCOPE REQUIRED
~~~

`54 / 54` 表示保留此前 48 项 scope invariant，再加入本文件六个 exact subset invariant；不是把历史
48-file scope 改写为 54 files。以下路径是 future allowlist，只有独立 blocker 与 migration scope
accepted 后才能写。

## 2. B4_UPSTREAM_PERSISTENT_IDENTITY_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/PersistentGuardIdentity.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/CanonicalPersistentIdentityCodec.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyAdmissionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/IdempotencyTransitionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/GuardCleanupCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunGuardProperties.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprint.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcRateLimitAdmissionAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcIdempotencyGuardAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcGuardCleanupAdapter.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
~~~

只允许 versioned verified-environment identity、legacy Option B 与必要 adapter/wiring 传播。migration 不在
该集合中；任何 schema 变更必须使用单独、已定版本的 migration scope。

## 3. B4_UPSTREAM_RATE_AUDIT_ENVIRONMENT_SCOPE

~~~text
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/RateLimiter.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/InMemoryDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
~~~

只允许同一 verified scope 驱动 rate identity 与 `QDR7_RATE_LIMIT_ADMISSION`；禁止 profile/default/JSON。

## 4. B4_UPSTREAM_REPLAY_NAMESPACE_SCOPE

~~~text
dh-security/pom.xml
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NonceReplayGuard.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/ReplayAdmissionCommand.java (NEW)
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/ReplayAdmissionResult.java (NEW)
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/CanonicalReplayNamespace.java (NEW)
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/InMemoryNonceReplayGuard.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuard.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/SecurityWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
~~~

`dh-security/pom.xml` 仅沿用此前已冻结的 `dh-security -> dh-domain:${project.version}` 与
`FeedbackEnvironment` import boundary，不授权其他 POM。

## 5. B4_UPSTREAM_IDENTITY_REPLAY_COMPATIBILITY_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcRateLimitAdmissionAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcIdempotencyGuardAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuard.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/InMemoryNonceReplayGuard.java
~~~

只允许删除 historical environmentless audit overload、实现 legacy Option B、保持旧 read/upgrade
兼容。不得把 legacy environment 推断为 DEV/TEST。

## 6. B4_UPSTREAM_IDENTITY_REPLAY_TEST_SCOPE

~~~text
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/BoundedInMemoryNonceReplayGuardTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuardTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuardPersistenceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprintTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/SecurityWiringConfigTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
~~~

仅允许新增冻结的 isolation、legacy、audit、replay、并发与 architecture assertions；不得降低既有
PostgreSQL/Testcontainers、recovery、repeatability 或 capacity 断言。

## 7. B4_UPSTREAM_AUDIT_ENVIRONMENT_STORAGE_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/InMemoryDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
~~~

未来 migration 必须在下一任务确定版本后加入单独
`B4_UPSTREAM_AUDIT_ENVIRONMENT_MIGRATION_SCOPE`；本轮不写伪造的 `V16` 或未定路径。

## 8. Invariants

~~~text
B4_UPSTREAM_PERSISTENT_IDENTITY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_RATE_AUDIT_ENVIRONMENT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_REPLAY_NAMESPACE_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_IDENTITY_REPLAY_COMPATIBILITY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_IDENTITY_REPLAY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_AUDIT_ENVIRONMENT_STORAGE_SCOPE ⊆ WRITE_ALLOWLIST: PASS

PRIOR EFFECTIVE UPSTREAM SCOPE INVARIANTS:
48 / 48 PASS

EFFECTIVE UPSTREAM SCOPE INVARIANTS:
54 / 54 PASS

TASK_SCOPE_DESIGN_INVALID:
NO

MIGRATION_SCOPE:
BLOCKED / VERSION AND PATH UNRESOLVED

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

## 9. Forbidden scope

本 erratum 不授权修改 Java、tests、POM、V1–V15、任何 V16、config、workflow、API surface、
Controller endpoint、registry、retention、scheduler、automatic learning、B4 review/publication 或 B5。
也不授权真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。
