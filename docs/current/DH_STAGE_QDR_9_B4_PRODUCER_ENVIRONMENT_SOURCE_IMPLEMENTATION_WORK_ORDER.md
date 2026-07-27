# DH Stage-QDR-9 B4 Producer Environment Source Implementation Work Order

## 1. 工单状态

```text
task: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-IMPLEMENTATION
status: BLOCKED / UPSTREAM ENVIRONMENT AUTHORITY REQUIRED
design baseline: 2cc75dcc8c2228af43e79d7163d7489bf67ebc97
origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3
root type: FeedbackExecutionScope
V16: CANDIDATE / NOT_CREATED
implementation authorization: NO
next action: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
```

本工单冻结 source-resolution 的 exact validation/write scope、事务与测试矩阵；它不授权本轮或下一轮直接改代码。只有独立 upstream contract 任务证明 AUDIT caller 能从明确、受校验、caller-supplied 的 DEV/TEST authority 构造 scope，并为 REPLAY/EVALUATION 提供真实 production caller，才可将本工单解锁。

## 2. B4_PRODUCER_ENVIRONMENT_ROOT_SOURCE_SCOPE

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackEnvironment.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java (NEW)
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeException.java (NEW)
dh-security/src/main/java/com/guidinglight/decisionhub/security/AuthContext.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/AuthenticatedRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketController.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/CreateApprovalPacketCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/SubmitApprovalDecisionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
```

这些文件完整覆盖当前 production root caller 与缺失 authority 的证据路径。它们不是 API/security 变更授权；upstream blocker 未关闭前不得修改。

## 3. B4_AUDIT_ENVIRONMENT_SOURCE_SCOPE

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionRequest.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/LimitedDryRunRuntimeService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/QdrModelGatewayIntegrationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/ApprovalWriteBoundary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessRecords.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessPort.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionService.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcReferenceLivenessTransactionBoundary.java (NEW)
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
```

## 4. B4_REPLAY_ENVIRONMENT_SOURCE_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveReplayCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessRecords.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessPort.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionService.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcReferenceLivenessTransactionBoundary.java (NEW)
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

## 5. B4_EVALUATION_ENVIRONMENT_SOURCE_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveEvaluationCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/EvaluationCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/EvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessRecords.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessPort.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionService.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcReferenceLivenessTransactionBoundary.java (NEW)
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

## 6. B4_PRODUCER_ENVIRONMENT_SOURCE_TEST_SCOPE

```text
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeTest.java (NEW)
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketControllerWebMvcTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilterEnvironmentTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestratorPersistenceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilderTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionServiceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayEvaluationPersistenceRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapterTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessFlywayPostgresTest.java (NEW)
```

## 7. Scope invariants

```text
reference-liveness design scope: 27 / 27 PASS
producer-environment initial scope: 31 / 31 PASS / SOURCE CALLERS UNRESOLVED
B4_PRODUCER_ENVIRONMENT_ROOT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_REPLAY_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_EVALUATION_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_SOURCE_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
EFFECTIVE_SCOPE_INVARIANTS: 36 / 36 PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

36/36 只代表当前 caller、传播、事务与测试路径已逐文件序列化；它不关闭 environment authority blocker，也不授权 API/security contract、V16 或 Java 写入。

## 8. 冻结实施顺序

1. 独立 upstream contract 任务选择并接受一个 caller-supplied DEV/TEST authority；禁止 profile/config/default/tenant/ID 推断。
2. 创建 `FeedbackExecutionScope` 与结构化 `ENVIRONMENT_REQUIRED` 失败。
3. 升级所有 root commands、records、builders 与 production callers；不保留无 environment constructor。
4. 由三个 JDBC native `save` 方法在 `JdbcReferenceLivenessTransactionBoundary` 内完成 native + ACTIVE registry 原子写入。
5. 使用同一 DH `PlatformTransactionManager`、REQUIRED、REPEATABLE_READ；移除 production direct/default transaction fallback。
6. 补齐 root/propagation/no-inference/rollback/caller completeness 测试。
7. 只有 source implementation 与 V16 targeted/full/quality 验证全部通过后，才允许独立 B4 milestone review retry。

## 9. 当前停止条件

```text
AUDIT upstream environment authority: ABSENT
REPLAY production root caller: ABSENT
EVALUATION production root caller: ABSENT
API/security source contract authorization: NO
ALLOW_SOURCE_IMPLEMENTATION: NO
ALLOW_V16_IMPLEMENTATION_RETRY: NO
ALLOW_B4_REVIEW/PUBLICATION/B5: NO / NO / NO
```

## 10. Upstream authority work-order handoff

本工单原有 36/36 source scope 已保留为 caller/path inventory；其 environment source unresolved 不能作为 V16 或 retention 实施授权。后续唯一可执行前置工单为：

~~~text
DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION
AUDIT authority: OPTION B / NEW EXPLICIT SIGNED CONTRACT REQUIRED
REPLAY/EVALUATION: DORMANT / NO PRODUCTION ENTRY
effective upstream scope: 42 / 42 PASS
V16 implementation: NOT ALLOWED
~~~

该工单的六个 exact future scopes、HMAC 文件覆盖和 test matrix 位于 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_IMPLEMENTATION_WORK_ORDER.md。它替代本工单作为 upstream contract implementation 的范围权威；本文件不授权增加 controller、scheduler、production caller 或 registry transition。
