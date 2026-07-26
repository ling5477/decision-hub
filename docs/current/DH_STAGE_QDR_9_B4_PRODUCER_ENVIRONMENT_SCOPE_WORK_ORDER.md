# DH Stage-QDR-9 B4 Producer Environment Scope Work Order

## 1. 任务状态

~~~text
task: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-CONTRACT-SCOPE-DESIGN
baseline HEAD / origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3
containment exact-SHA CI: 30203970694 / PASS
current highest migration: V15
V16: CANDIDATE / NOT CREATED
current task result: LOCAL_ACCEPTED / SOURCE_BLOCKER_FROZEN
V16 implementation: BLOCKED BEFORE CODE WRITE
next action: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER
~~~

本工单只把既有 27 项 reference-liveness scope 的遗漏 producer environment 调用链串行化为 future allowlist。它不把新 scope 倒写为原 27-file scope 已覆盖，也不授权代码、测试、migration、API、scheduler、retention、B4 review/publication 或 B5。

## 2. 新增精确 scope 集合

### B4_PRODUCER_ENVIRONMENT_CONTRACT_SCOPE

~~~text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackEnvironment.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionRequest.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/QdrModelGatewayIntegrationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveReplayCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveEvaluationCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/EvaluationCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/CreateApprovalPacketCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/SubmitApprovalDecisionCommand.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/approval/HumanApprovalPacket.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessRecords.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessPort.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionService.java (NEW)
~~~

### B4_PRODUCER_ENVIRONMENT_PROPAGATION_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
~~~

### B4_PRODUCER_ENVIRONMENT_TRANSACTION_SCOPE

~~~text
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapter.java (NEW)
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcReferenceLivenessTransactionBoundary.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/main/resources/db/migration/V16__qdr9_reference_liveness_state_model.sql (NEW)
~~~

### B4_PRODUCER_ENVIRONMENT_TEST_SCOPE

~~~text
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/DecisionRequestTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestratorTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilderTest.java (NEW)
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceLivenessTransitionServiceTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayEvaluationPersistenceRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceLivenessAdapterTest.java (NEW)
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapterTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessMigrationPresenceTest.java (NEW)
dh-app/src/test/java/com/guidinglight/decisionhub/V16Qdr9ReferenceLivenessFlywayPostgresTest.java (NEW)
~~~

每个 future implementation 文件必须同时属于完整 WRITE_ALLOWLIST；不在这些集合或既有 27-file scope 内的文件以 STAGE_QDR_9_B4_P1_FIX_SCOPE_EXPANSION_REQUIRED 拒绝。若 source-blocker 证明必须触及 Controller、security RateLimiter contract、V1-V15 或其他未列文件，不能静默扩张，必须另起 scope retry。

## 3. Scope invariants

~~~text
reference-liveness design invariants: 27 / 27 PASS
B4_PRODUCER_ENVIRONMENT_CONTRACT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_PROPAGATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_TRANSACTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
EFFECTIVE_SCOPE_INVARIANTS: 31 / 31 PASS
TASK_SCOPE_DESIGN_INVALID: NO
~~~

## 4. 实施前阻断与退出条件

V16 retry 仅在独立 source-blocker 任务为每个 production caller 证明唯一、显式、可信的 FeedbackEnvironment source，并把必要文件完整纳入下一份 allowlist 后重新评估。永久禁止默认 DEV、tenant/ID/profile 推断、缺失 environment 时只跳过 registry、用 audit result/verdict 当 lifecycle、异步补写 registry、API/scheduler/automatic learning 扩张。

本轮不授权 B4 milestone review retry、B4 publication、B5、API、scheduler、automatic learning、real HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。

## 5. Source-resolution scope erratum — 2026-07-26

原 31/31 仅覆盖 environment contract 与初始传播，不包含全部 production root callers，不能作为 implementation acceptance scope。后续审计已定位：

```text
AUDIT roots:
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketController.java

REPLAY/EVALUATION root boundary:
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java

REPLAY/EVALUATION production caller:
NOT PRESENT
```

新增五个 exact scope 集合的完整逐文件清单位于：

```text
docs/current/DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_IMPLEMENTATION_WORK_ORDER.md
```

scope invariant 更新为：

```text
B4_PRODUCER_ENVIRONMENT_ROOT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_REPLAY_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_EVALUATION_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_SOURCE_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
EFFECTIVE_SCOPE_INVARIANTS: 36 / 36 PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

该 scope 只冻结 future validation/write serialization，当前仍不授权代码、测试、migration、API/security contract 或 V16。由于 AUDIT 无可信 caller environment、REPLAY/EVALUATION 无 production caller，implementation retry 保持 blocked。
