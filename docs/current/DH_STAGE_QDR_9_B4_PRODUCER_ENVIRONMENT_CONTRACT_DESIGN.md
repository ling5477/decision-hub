# DH Stage-QDR-9 B4 Producer Environment Contract Design

## 1. 冻结结论

本文件在 7624bccba9b865d4b687057f41b96799cb9ba8e3 containment baseline 上完成 AUDIT、REPLAY、EVALUATION producer 的 environment 调用链审计与合同设计。仅冻结未来实现的输入、传播、事务和测试合同；不创建 V16，不改 Java、测试、migration、API、scheduler 或 retention。

~~~text
CONTAINMENT: CLOSED / ACCEPTED / PUBLISHED
CONTAINMENT_EXACT_SHA_CI: 30203970694 / PASS
B4_RETENTION_IMPLEMENTATION: ABSENT / REVERTED
REFERENCE_LIVENESS_STATE_MODEL: FROZEN / PUBLISHED
SELECTED_ARCHITECTURE: OPTION B / qdr_reference_liveness
V16: CANDIDATE / NOT CREATED
V16_IMPLEMENTATION: BLOCKED BEFORE CODE WRITE
BLOCKER: STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_CONTRACT_BLOCKER
AUDIT_ENVIRONMENT_CONTRACT: FROZEN / LOCAL_ACCEPTED
REPLAY_ENVIRONMENT_CONTRACT: FROZEN / LOCAL_ACCEPTED
EVALUATION_ENVIRONMENT_CONTRACT: FROZEN / LOCAL_ACCEPTED
ENVIRONMENT_SOURCES: UNRESOLVED / FAIL_CLOSED
ALLOW_V16_IMPLEMENTATION_RETRY: NO
~~~

FeedbackEnvironment 是唯一 canonical 类型，只允许 DEV 与 TEST。所有 producer 入口、command、native record 和 registry transition 必须使用同一不可变值；null、unknown、PROD、LIVE、PAPER、LOCAL 以 ENVIRONMENT_REQUIRED 或等价结构化错误拒绝。

严禁把 tenant、target ID、trace ID、decision ID、reference snapshot、native status、Spring profile、数据库查询、ThreadLocal、静态变量或默认配置解释为 environment。SUCCESS/FAILED 和 PASS/FAIL/WARN/SKIPPED 不是 lifecycle。

## 2. 已审计的代码现实

| Producer | entry point / caller | command or context | native record / port / adapter | transaction | tenant | environment | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AUDIT | DefaultDecisionOrchestrator.decide(DecisionRequest) | domain/decision/DecisionRequest | AuditEventRecord -> DecisionAuditRepository -> JdbcDecisionAuditRepository | NOT_AVAILABLE | EXPLICIT | NOT_AVAILABLE | DecisionRequest 的既有持久化映射没有 environment，不能从 tenant 或 decision record 补猜。 |
| AUDIT | DefaultDecisionDryRunService；PersistentGuardedDecisionDryRunService | DecisionDryRunCommand | AuditEventRecord -> DecisionAuditRepository | 现有 guard transaction 不含 registry | EXPLICIT | NOT_AVAILABLE | command 无 environment；不得以 runtime profile 或 default guard config 替代。 |
| AUDIT | DefaultQdrModelGatewayIntegrationService.invoke | QdrModelGatewayIntegrationCommand，由 dry-run service 构造 | AuditEventRecord -> DecisionAuditRepository | NOT_AVAILABLE | EXPLICIT | NOT_AVAILABLE | 上游 dry-run 和 gateway command 都没有 environment。 |
| AUDIT | PersistentDecisionDryRunRateLimiter.check | PersistentGuardIdentity / DecisionDryRunGuardProperties | AuditEventRecord -> DecisionAuditRepository | GuardTransactionBoundary.required | EXPLICIT | NOT_AVAILABLE | identity 接受 dev/test/staging/prod，不是 FeedbackEnvironment 等价类型；配置默认或 profile 不可信。 |
| AUDIT | HumanApprovalPacketCommandService.create/submit | CreateApprovalPacketCommand、SubmitApprovalDecisionCommand、HumanApprovalPacket | AuditEventRecord -> DecisionAuditRepository | ApprovalWriteBoundary | EXPLICIT | NOT_AVAILABLE | command 与持久化 packet 都没有 environment；后续 submit 不能从 packet 推断。 |
| REPLAY | QdrRegressionEvaluationService.evaluate | QdrRegressionEvaluationCommand -> SaveReplayCaseCommand | ReplayCaseRecord -> ReplayCaseRepository -> JdbcReplayCaseRepository | NOT_AVAILABLE | EXPLICIT | NOT_AVAILABLE | command、save command、record、V9 adapter 无 environment；当前 main tree 无 production command constructor。 |
| EVALUATION | 同一 QdrRegressionEvaluationService | QdrRegressionEvaluationCommand -> SaveEvaluationCaseCommand | EvaluationCaseRecord -> EvaluationCaseRepository -> JdbcEvaluationCaseRepository | NOT_AVAILABLE | EXPLICIT | NOT_AVAILABLE | command、save command、record、V9 adapter 无 environment。 |

DecisionPipelineWiringConfig 只装配 V9 replay/evaluation repository，没有 environment owner 或 transaction coordinator。JdbcDecisionAuditRepository、JdbcReplayCaseRepository 与 JdbcEvaluationCaseRepository 当前各自 JDBC 写入，不满足 native target 与 registry 同一事务的未来合同。

## 3. Environment 合同与传播

以下 future contract 的构造期必须校验 FeedbackEnvironment：required、non-null、immutable、仅 DEV/TEST。不得保留静默默认 DEV 的 constructor、builder default、nullable field 或 production fallback。

~~~text
QdrRegressionEvaluationCommand.environment
SaveReplayCaseCommand.environment
SaveEvaluationCaseCommand.environment
ReplayCaseRecord.environment
EvaluationCaseRecord.environment
DecisionPersistenceRecords.AuditEventRecord.environment
future producer lifecycle transition command.environment
~~~

AUDIT 的 earliest trusted caller 必须在实际入口显式携带该字段：DecisionRequest、DecisionDryRunCommand、QdrModelGatewayIntegrationCommand、CreateApprovalPacketCommand、SubmitApprovalDecisionCommand 与 rate-limit 内部调用合同。它们当前都不具备此字段，因此不是已解析来源。

唯一允许的转换是 future explicit SourceEnvironmentType -> FeedbackEnvironment，且只能逐值 DEV -> DEV、TEST -> TEST；null、unknown 和其他值拒绝。PersistentGuardIdentity 不能作为转换源，因为其支持 staging 与 prod。

| Target | future explicit source | propagation | canonical target key | ACTIVE owner | SAFE_INACTIVE / reactivation owner |
| --- | --- | --- | --- | --- | --- |
| AUDIT | 未解析：每个 audit caller 必须在入口取得 FeedbackEnvironment | entry -> audit caller -> AuditEventRecord -> audit persistence/liveness coordinator | audit:<auditEventId> | native audit insert coordinator | internal type-checked lifecycle command；不得由 event_status 触发 |
| REPLAY | 未解析：QdrRegressionEvaluationCommand.environment 及其 future production caller | command -> MockGatewayRegressionCaseBuilder -> save command -> record -> coordinator | replay-case:<replayCaseUuid>；replay:<caseId> 只是既有 alias | regression evaluation transaction coordinator | internal replay lifecycle command；snapshot existence 和 replay save 不得推断 |
| EVALUATION | 未解析：同一 QdrRegressionEvaluationCommand.environment | command -> builder -> save command -> record -> coordinator | evaluation:<evaluationCaseUuid> | regression evaluation transaction coordinator | internal evaluation lifecycle command；verdict 不得触发 |

registry key 保持 (tenant_id, environment, reference_type, canonical_target_key)。native 表不重解释 event_status 或 verdict；environment 是 V16 registry typed scope，native identity 仍需 tenant-bound 复核。

## 4. 原子事务合同

| Producer | native write | registry action | boundary / propagation / isolation | 失败行为 |
| --- | --- | --- | --- | --- |
| AUDIT | JdbcDecisionAuditRepository.saveAuditEvent | create ACTIVE 或校验过的 lifecycle transition | new audit+liveness coordinator；REQUIRED；REPEATABLE_READ；同一 DH PostgreSQL datasource | native 或 registry 任一失败、environment 缺失、state-version conflict 均整体 rollback。 |
| REPLAY | JdbcReplayCaseRepository.save | create ACTIVE 或校验过的 lifecycle transition | QdrRegressionEvaluationService future coordinator；REQUIRED；REPEATABLE_READ | replay/registry 任一失败整体 rollback；evaluation/verdict/finding 不得留下不一致成功状态。 |
| EVALUATION | JdbcEvaluationCaseRepository.save | create ACTIVE 或校验过的 lifecycle transition | 同一 regression evaluation coordinator；REQUIRED；REPEATABLE_READ | evaluation/registry 任一失败整体 rollback；verdict 不映射 SAFE_INACTIVE。 |

不得 afterCommit 补写、eventual consistency、best-effort registry update、异步重试或吞掉 registry 失败。transition 锁 registry row FOR UPDATE，校验 tenant/environment/type/key 与 state_version；reactivation 递增 version。retention 仅 FOR SHARE 读取，绝不写 registry。

## 5. 未解析来源的阻断

~~~text
DecisionRequest persisted read model          -> no environment
DecisionDryRunCommand                         -> no environment
QdrModelGatewayIntegrationCommand             -> no environment
QdrRegressionEvaluationCommand                -> no environment
Create/SubmitApprovalDecisionCommand          -> no environment
HumanApprovalPacket                            -> no environment
PersistentGuardIdentity / guard properties    -> broader dev/test/staging/prod configuration, not equivalent
~~~

因此本设计只冻结 required future contract，不能宣称任何 producer 已有可信 environment source。缺失 environment 时，入口拒绝 native operation，不写 native target、不写 registry transition；retention 对缺失或 other-environment registry row 继续阻断删除。

## 6. 冻结测试矩阵

1. 每类 producer：DEV/TEST 接受；null、unknown、PROD、LIVE 拒绝。
2. AUDIT、REPLAY、EVALUATION 分别断言 entry = command = native record = registry environment。
3. 同 tenant 在 DEV/TEST 独立；同 canonical key 的 registry rows 独立；environment 缺失拒绝。
4. 每类 producer：native failure rollback registry；registry failure rollback native；environment validation failure 两侧均不写。
5. SAFE_INACTIVE 仅由显式内部 lifecycle command 创建；同环境 reactivation 变 ACTIVE 且 version 递增；TEST 不影响 DEV。
6. retention P1：other-environment registry row block delete；matching SAFE_INACTIVE row 才可继续其他 eligibility 检查。
7. architecture：无 default DEV、ThreadLocal、profile inference、Controller/API、scheduler 或 automatic learning 扩张。

## 7. Readiness

~~~text
PRODUCER_ENVIRONMENT_CONTRACT_DESIGN: DONE / BLOCKED
AUDIT_ENVIRONMENT_SOURCE: FROZEN / UNRESOLVED
REPLAY_ENVIRONMENT_SOURCE: FROZEN / UNRESOLVED
EVALUATION_ENVIRONMENT_SOURCE: FROZEN / UNRESOLVED
ENVIRONMENT_TYPE_CONTRACT: PASS
NO_DEFAULT_ENVIRONMENT: PASS
PROPAGATION_PATHS: FROZEN / UNRESOLVED
PRODUCER_TRANSACTION_DESIGN: FROZEN
CROSS_MODULE_SCOPE: PASS
EFFECTIVE_SCOPE_INVARIANTS: PASS / 31 OF 31
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 B4 PRODUCER ENVIRONMENT AUTHORITY HASH / 0 CONFLICTS
ALLOW_V16_IMPLEMENTATION_RETRY: NO
NEXT_ACTION: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER
~~~

## 8. Producer source resolution erratum — 2026-07-26

后续 `DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER` 已补做 API root、authentication context、rate-limit bridge 与 regression service production wiring 审计。原 31/31 结论继续保留，但其语义严格限定为：

```text
CONTRACT AND INITIAL PROPAGATION SCOPE
SOURCE CALLERS UNRESOLVED
SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
```

唯一根类型选定为：

```text
ROOT_EXECUTION_SCOPE_TYPE: FeedbackExecutionScope
ROOT_EXECUTION_SCOPE_PATH:
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java
```

真实 caller 结论：

1. AUDIT 的 production roots 是 `DecisionDryRunController.decide` 与 `HumanApprovalPacketController.createApprovalPacket/submitApprovalDecision`；现有 auth/request contract 没有 environment。
2. `DecisionDryRunGuardProperties.environment` 是 deployment configuration 且支持 staging/prod，明确禁止作为 source。
3. REPLAY/EVALUATION 的 `QdrRegressionEvaluationService.evaluate` 没有 production constructor 或 Spring wiring；只有 test caller。

因此 root type、root owners、root-inward propagation、missing-environment 行为与事务 owner 已冻结，但三类实际 environment source 仍为 `UNRESOLVED`。五个 source-resolution exact scopes 已在新工单逐文件序列化，effective scope 为 36/36 PASS；scope 完整不等于 source authority 已存在。

```text
PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION: BLOCKED
AUDIT / REPLAY / EVALUATION SOURCE: UNRESOLVED / FAIL_CLOSED
ALLOW_V16_IMPLEMENTATION_RETRY: NO
NEXT_ACTION: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
```

## 9. Trusted upstream authority decision

本文件冻结的 FeedbackExecutionScope 类型、DEV/TEST 限制与 no-default 规则保持不变。新增代码现实审计确认 AUDIT 当前的 HMAC canonical payload 不含 environment，Human Approval 也没有 source/timestamp/nonce/HMAC contract。因此实施 authority 冻结为：

~~~text
AUDIT_AUTHORITY_OPTION: B
AUDIT_ENVIRONMENT_AUTHORITY: NEW EXPLICIT SIGNED CONTRACT REQUIRED
REPLAY_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
EVALUATION_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
MISSING_OR_UNVERIFIED_ENVIRONMENT: FAIL CLOSED
ENVIRONMENT_INFERENCE: PROHIBITED
~~~

scope 只能在 authentication、signature、timestamp、nonce、source、tenant/environment authorization 全部成功后，由 root adapter 创建。完整设计决策和 future cross-module contract 分别见 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_AUTHORITY_DECISION.md 与 DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_IMPLEMENTATION_WORK_ORDER.md。
