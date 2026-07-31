# DH Stage-QDR-9 B4 Producer Environment Source Resolution

## 1. 决策状态

本文件记录 `DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER` 的代码现实审计与安全架构结论。审计基线为本地 `2cc75dcc8c2228af43e79d7163d7489bf67ebc97`；远端 `origin/dev` 与 advertised SHA 均为 `7624bccba9b865d4b687057f41b96799cb9ba8e3`。

```text
TASK_CLASSIFICATION: DOCUMENTATION / SECURITY_ARCHITECTURE_DECISION
CODE / TEST / MIGRATION CHANGE: NONE
V16: CANDIDATE / NOT_CREATED
ROOT_EXECUTION_SCOPE_TYPE: FeedbackExecutionScope
ROOT_EXECUTION_SCOPE_PATH: dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java
ROOT_EXECUTION_SCOPE_CONTRACT: FROZEN
ROOT_SOURCE_STRATEGY: SEPARATE PRODUCTION ROOTS / ONE SHARED STRICT TYPE
AUDIT_ENVIRONMENT_SOURCE: UNRESOLVED
REPLAY_ENVIRONMENT_SOURCE: UNRESOLVED
EVALUATION_ENVIRONMENT_SOURCE: UNRESOLVED
PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION: BLOCKED
BLOCKER: PRODUCER_ENVIRONMENT_SOURCE_UNRESOLVED
ALLOW_V16_IMPLEMENTATION_RETRY: NO
```

`FeedbackExecutionScope` 是唯一选定名称，不保留 `QdrExecutionScope` 或 `DecisionExecutionScope` 候选。future record 只包含 `tenantId` 与 `FeedbackEnvironment environment`；构造期要求 tenant non-blank、environment non-null，且 canonical enum 只允许 DEV/TEST。禁止 nullable、`Optional`、default DEV、builder default、宽松字符串解析、Spring profile、application property、tenant/ID 推断、ThreadLocal、static global 或 repository reverse lookup。

## 2. Production root caller 审计

### 2.1 AUDIT

| Production root | 证据路径 | 当前可用环境 | 结论 |
| --- | --- | --- | --- |
| `DecisionDryRunController.decide` | `dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java` | tenant、trace、signed dry-run headers；无 environment | 不能构造可信 `FeedbackExecutionScope`。 |
| `HumanApprovalPacketController.createApprovalPacket` | `dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketController.java` | authenticated tenant/requester/trace；无 environment | 不能构造可信 `FeedbackExecutionScope`。 |
| `HumanApprovalPacketController.submitApprovalDecision` | 同上 | authenticated tenant/requester/trace；无 environment | 不能从 packet、tenant 或 status 推断。 |

`DecisionDryRunController` 是 `DecisionDryRunCommand` 的唯一 production constructor。它同时调用 persistent rate limiter，再进入 `PersistentGuardedDecisionDryRunService`、`DefaultDecisionDryRunService`、`DefaultDecisionOrchestrator` 与 `DefaultQdrModelGatewayIntegrationService`。这些节点共有七个 production `AuditEventRecord` 构造位置，但都没有 environment。

`DecisionDryRunGuardProperties.environment` 与 `PersistentGuardIdentity.environment` 来自 guard deployment configuration，并允许 `dev/test/staging/prod`；它们不是 caller-supplied `FeedbackEnvironment`，不得转换为 registry source。

`HumanApprovalPacketController` 是 create/submit command 的唯一 production constructor。`AuthContext`、`AuthenticatedRequest` 与 `DhApiAuthenticationFilter` 当前只建立 user/tenant/roles，不建立 environment authority。

### 2.2 REPLAY 与 EVALUATION

`QdrRegressionEvaluationCommand` 的 production constructor 搜索结果为零；`QdrRegressionEvaluationService` 没有 Spring bean wiring，也没有 production caller。当前唯一 constructor 位于 `QdrRegressionEvaluationServiceTest`。

因此：

```text
REPLAY_ROOT_USE_CASE_BOUNDARY:
QdrRegressionEvaluationService.evaluate(QdrRegressionEvaluationCommand)

EVALUATION_ROOT_USE_CASE_BOUNDARY:
QdrRegressionEvaluationService.evaluate(QdrRegressionEvaluationCommand)

ACTUAL_PRODUCTION_ROOT_CALLER:
NOT PRESENT
```

不能把“未来可能调用该 service”写成真实 source，也不能为了统一创建 API、scheduler 或伪造共同入口。

## 3. 根作用域所有权与失败合同

采用独立根调用链方案；AUDIT、REPLAY、EVALUATION 共享同一个严格 `FeedbackExecutionScope` 类型，但不伪造共同 root。

```text
AUDIT_ROOT_SCOPE_OWNERS:
DecisionDryRunController.decide
HumanApprovalPacketController.createApprovalPacket
HumanApprovalPacketController.submitApprovalDecision

REPLAY_ROOT_SCOPE_OWNER:
future production caller of QdrRegressionEvaluationService.evaluate / CURRENTLY ABSENT

EVALUATION_ROOT_SCOPE_OWNER:
future production caller of QdrRegressionEvaluationService.evaluate / CURRENTLY ABSENT
```

当前 controller/auth contract 没有 environment，且本任务不允许 API/security contract 变更，所以 AUDIT owner 不能创建该 scope。REPLAY/EVALUATION 没有 production owner。以上是 upstream contract blocker，不得以 profile、guard property 或默认 DEV 绕过。

future missing-environment 合同冻结如下：

```text
failure code: ENVIRONMENT_REQUIRED
failure point: earliest root use-case boundary
native AUDIT/REPLAY/EVALUATION write: 0
registry write/transition: 0
full payload logging: FORBIDDEN
catch-and-continue: FORBIDDEN
```

## 4. 传播链冻结

### 4.1 AUDIT

```text
DecisionDryRunController.decide
→ FeedbackExecutionScope
→ DecisionDryRunCommand
→ PersistentDecisionDryRunRateLimiter / PersistentGuardedDecisionDryRunService
→ DefaultDecisionDryRunService
→ domain.decision.DecisionRequest
→ DefaultDecisionOrchestrator
→ QdrModelGatewayIntegrationCommand
→ DefaultQdrModelGatewayIntegrationService
→ DecisionPersistenceRecords.AuditEventRecord
→ DecisionAuditRepository.saveAuditEvent
→ JdbcDecisionAuditRepository.saveAuditEvent
→ FeedbackReferenceLivenessPort.activate(AUDIT)
```

```text
HumanApprovalPacketController.createApprovalPacket / submitApprovalDecision
→ FeedbackExecutionScope
→ CreateApprovalPacketCommand / SubmitApprovalDecisionCommand
→ HumanApprovalPacketCommandService
→ DecisionPersistenceRecords.AuditEventRecord
→ DecisionAuditRepository.saveAuditEvent
→ JdbcDecisionAuditRepository.saveAuditEvent
→ FeedbackReferenceLivenessPort.activate(AUDIT)
```

`AuditEventRecord` future contract 必须携带同一不可变 scope；`event_status=SUCCESS/FAILED` 语义保持不变，不能映射 lifecycle。

### 4.2 REPLAY

```text
future production caller (CURRENTLY ABSENT)
→ FeedbackExecutionScope
→ QdrRegressionEvaluationCommand
→ QdrRegressionEvaluationService.evaluate
→ MockGatewayRegressionCaseBuilder
→ SaveReplayCaseCommand
→ ReplayCaseRecord
→ ReplayCaseRepository.save
→ JdbcReplayCaseRepository.save
→ FeedbackReferenceLivenessPort.activate(REPLAY)
```

canonical key 保持 `replay-case:<replayCaseUuid>`；snapshot existence 不是 lifecycle。

### 4.3 EVALUATION

```text
future production caller (CURRENTLY ABSENT)
→ FeedbackExecutionScope
→ QdrRegressionEvaluationCommand
→ QdrRegressionEvaluationService.evaluate
→ MockGatewayRegressionCaseBuilder
→ SaveEvaluationCaseCommand
→ EvaluationCaseRecord
→ EvaluationCaseRepository.save
→ JdbcEvaluationCaseRepository.save
→ FeedbackReferenceLivenessPort.activate(EVALUATION)
```

canonical key 保持 `evaluation:<evaluationCaseUuid>`；`PASS/FAIL/SKIPPED` verdict 不是 lifecycle。

## 5. Lifecycle 与事务所有者

| Producer | ACTIVE owner / native method | registry method | transaction owner | manager / propagation / isolation | rollback |
| --- | --- | --- | --- | --- | --- |
| AUDIT | `JdbcDecisionAuditRepository.saveAuditEvent` | `FeedbackReferenceLivenessPort.activate(AUDIT, scope, key)` | `JdbcReferenceLivenessTransactionBoundary.required` | 同一 DH `PlatformTransactionManager` / REQUIRED / REPEATABLE_READ | native、registry、version conflict 任一失败整体 rollback。 |
| REPLAY | `JdbcReplayCaseRepository.save` | `FeedbackReferenceLivenessPort.activate(REPLAY, scope, key)` | 同上 | 同上 | replay 与 registry 原子；任一失败整体 rollback。 |
| EVALUATION | `JdbcEvaluationCaseRepository.save` | `FeedbackReferenceLivenessPort.activate(EVALUATION, scope, key)` | 同上 | 同上 | evaluation 与 registry 原子；任一失败整体 rollback。 |

`FeedbackReferenceLivenessTransitionService` 是三类 target 的唯一 `SAFE_INACTIVE` 与 reactivation owner；只能接收显式、type-checked、tenant/environment/key/version-bound internal command。当前没有任何自动 invocation 获得授权。audit result、snapshot existence、verdict、retention、afterCommit、async event、best effort 与 catch-and-log 均不是 owner。

现有 `ApprovalWriteBoundary` 与 `GuardTransactionBoundary` 在 future implementation 中必须使用同一个 DH transaction manager，并显式使用 REPEATABLE_READ；inner liveness boundary 以 REQUIRED 加入 outer transaction。当前 wiring 尚未满足此合同，但结构可闭合，不新增 transaction-atomicity blocker。

## 6. Exact scope erratum

既有 27/27 reference-liveness scope 保留。既有 31/31 只表示：

```text
CONTRACT AND INITIAL PROPAGATION SCOPE
SOURCE CALLERS UNRESOLVED
SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
```

本轮已定位所有当前 production callers，并把五个 source-resolution 集合逐文件冻结在 `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_IMPLEMENTATION_WORK_ORDER.md`。scope 包含关系为 36/36 PASS；这只证明 caller/scope serialization 完整，不把缺失的 upstream environment authority 伪装为已解析 source。

```text
B4_PRODUCER_ENVIRONMENT_ROOT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_AUDIT_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_REPLAY_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_EVALUATION_ENVIRONMENT_SOURCE_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_PRODUCER_ENVIRONMENT_SOURCE_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
EFFECTIVE_SCOPE_INVARIANTS: 36 / 36 PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

## 7. 测试矩阵

1. Root contract：DEV/TEST 接受；null、unknown、PROD、LIVE 拒绝；无 default constructor/builder value。
2. AUDIT：root scope = command/request = `AuditEventRecord` = registry。
3. REPLAY：root scope = evaluation command = save command/record = registry。
4. EVALUATION：root scope = evaluation command = save command/record = registry。
5. No inference：同 tenant、同 key 的 DEV/TEST 独立；tenant present + missing environment 与 dev profile + missing environment 均拒绝。
6. 每类 producer：environment validation failure 为 native 0 / registry 0；native failure rollback registry；registry failure rollback native。
7. Architecture guard：全部 production producer entrypoints 必须接收显式 scope；禁止旧 constructor、default DEV、ThreadLocal/profile lookup。
8. 边界：不新增 API、scheduler、automatic learning、HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。

## 8. Readiness

```text
PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION: BLOCKED
ROOT_EXECUTION_SCOPE: FROZEN
AUDIT_ENVIRONMENT_SOURCE: UNRESOLVED / UPSTREAM CALLER CONTRACT ABSENT
REPLAY_ENVIRONMENT_SOURCE: UNRESOLVED / PRODUCTION CALLER ABSENT
EVALUATION_ENVIRONMENT_SOURCE: UNRESOLVED / PRODUCTION CALLER ABSENT
NO_DEFAULT_ENVIRONMENT: PASS
MISSING_ENVIRONMENT_FAIL_CLOSED: PASS
PROPAGATION_PATHS: FROZEN FROM ROOT BOUNDARY INWARD
TRANSACTION_OWNERS: FROZEN
CROSS_MODULE_SCOPE: PASS
EFFECTIVE_SCOPE_INVARIANTS: PASS / 36 OF 36
ALLOW_V16_IMPLEMENTATION_RETRY: NO
ALLOW_B4_MILESTONE_REVIEW_RETRY: NO
ALLOW_B4_PUBLICATION: NO
ALLOW_B5_IMPLEMENTATION: NO
NEXT_ACTION: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
```

## 9. Upstream authority design supersession

DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER 已完成对本文件 blocker 的设计收口。旧状态中的 source unresolved 仍是当前代码事实；新结论是未来 authority 的实现方式已经唯一冻结，而不是声称现有 source 已被解析。

~~~text
AUDIT_ENVIRONMENT_AUTHORITY: OPTION B / NEW EXPLICIT SIGNED CONTRACT REQUIRED
REPLAY_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
EVALUATION_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
ROOT_EXECUTION_SCOPE: FeedbackExecutionScope / FROZEN / NOT IMPLEMENTED
EFFECTIVE_UPSTREAM_SCOPE: 42 / 42 PASS
V16: CANDIDATE / NOT CREATED
ALLOW_V16_IMPLEMENTATION: NO
NEXT_ACTION: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION
~~~

唯一当前 authority decision 见 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_AUTHORITY_DECISION.md；exact future write/test scope 见 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_IMPLEMENTATION_WORK_ORDER.md。不得从 guard profile、tenant、ID 或历史 audit record 反推 environment。
