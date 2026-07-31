# DH Stage-QDR-9 B4 Producer Environment Upstream Implementation Work Order

## Upstream Environment Contract Remote Containment — 2026-07-30

本工单冻结的设计与 `48 / 48` scope 链继续保留，但其实现授权已经消耗且实现已被 containment。当前状态覆盖下方历史执行状态：

~~~text
design/scope:
FROZEN / PUBLISHED / 48 OF 48

published implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0

published exact-SHA CI:
30283326199 / PASS / REGRESSION PASS ONLY

pre-publication milestone review:
NOT COMPLETED

retrospective review:
BLOCKED

technical P1:
1 OPEN / persistent identity and QDR7 rate-audit environment integrity

technical P2:
1 OPEN / replay key excludes environment

governance P1:
PUBLISHED BEFORE MILESTONE REVIEW

containment:
OPTION 1A / ORDINARY REVERT OF IMPLEMENTATION ONLY

implementation revert:
df921f275c61d67cebbb95c0924391866a6d09dc

current implementation code:
NOT PRESENT IN CURRENT TREE

FeedbackExecutionScope / signed environment contract:
DESIGNED / NOT IMPLEMENTED

history rewrite:
NONE
~~~

本 containment 不把历史写成“implementation 从未发布”，也不把 `30283326199` 的绿灯解释为安全审查通过。六条设计/scope 提交保持 ancestor；production Java、tests、POM、config 与 workflow 已恢复为 B3 safe baseline。P1/P2 未修复。

本工单不得直接恢复 implementation。后续唯一允许入口是 containment 发布及最终 authority exact-SHA CI 通过后的独立 scope-design 任务：

~~~text
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-SCOPE-DESIGN
~~~

该入口不授权 P1/P2 implementation、V16、registry、retention、B4 milestone review retry、B4 publication、B5、endpoint、scheduler 或 automatic learning。

## Upstream Persistent Identity and Replay Namespace Blocker — 2026-07-30

后续 scope-design 已完成代码现实审计并冻结 canonical contract，但未实施 technical P1/P2：

~~~text
scope-design baseline:
b0ff11e4057077ad7e0fe91d691116f069dc744e

published and reverted implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0

current safe tree:
B3 SAFE BASELINE

persistent identity:
QDR9-RATE-IDENTITY-2 / QDR9-IDEMPOTENCY-IDENTITY-2 / QDR9-RECOVERY-IDENTITY-2
DESIGN FROZEN / NOT IMPLEMENTED

rate audit environment:
command.executionScope().environment()
DESIGN FROZEN / NOT IMPLEMENTED

replay namespace:
QDR9-DRYRUN-REPLAY-2 / QDR9-LP1
DESIGN FROZEN / NOT IMPLEMENTED

legacy persistent identity:
OPTION B / BLOCKED BY UNBOUNDED EXPIRED TOMBSTONES

legacy replay:
OPTION B / BLOCKED BY UNBOUNDED CONFIGURED TTL AND CURRENT PORT

audit environment storage:
FORWARD MIGRATION REQUIRED

effective scope invariants:
54 / 54 PASS

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

现有 persistent 表的 `environment` 来自部署 guard property，不是 signed request environment；现有
audit table 无结构化 environment column；现有 replay key 不含 version/environment，且 legacy TTL
无全局 hard ceiling。所以下一步必须先执行
`DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN`，不得直接恢复或
cherry-pick `549ed5a…`。

本 blocker 保留既有 48/48 历史 scope，并以六个 exact subset invariant 扩展为 54/54。它不创建
migration、V16、registry 或 retention，不启动 B4 review retry/publication/B5。

## 1. 工单状态与边界

~~~text
task: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION
status: FROZEN / SCOPE-RETRY ACCEPTED / IMPLEMENTATION RETRY NEXT TASK ONLY
authority design: DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_AUTHORITY_DECISION.md
AUDIT authority: OPTION B / NEW EXPLICIT SIGNED CONTRACT REQUIRED
REPLAY/EVALUATION: DORMANT / NO PRODUCTION ENTRY
V16 / retention / API expansion / scheduler / automatic learning: NOT AUTHORIZED
~~~

本工单仅冻结 future implementation 的精确范围；它不授权当前任务写 Java、测试、migration、configuration 或 API。U1/U2 acceptance 是 V16 的前置条件，不是 V16 implementation authorization。

原 trusted-upstream `42 / 42 PASS` scope 在首次 implementation preflight 中以 `STAGE_QDR_9_B4_UPSTREAM_CONTRACT_SCOPE_BLOCKER` 确认缺少三个生产路径与三个 mandatory PostgreSQL compatibility tests。该 42-file 集合保留为历史审计事实；corrected `46 / 46 PASS` 在 Maven scope retry 前仍缺少必需的 `dh-security/pom.xml`，故二者均为 `SUPERSEDED FOR UPSTREAM IMPLEMENTATION ACCEPTANCE`。本工单的 effective scope 已扩展至 `47 / 47 PASS`。新鲜全量回归随后显示唯一 legacy persistent-guard PostgreSQL fixture 未在该范围；本 task 的 Part A 将它精确纳入 `48 / 48 PASS`，Part B 只能在本 scope erratum 独立提交后执行。

## 2. Future write allowlist

### B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE

~~~text
dh-security/pom.xml
~~~

本文件是唯一可修改的 future POM。只允许加入既有 reactor dependency `com.guidinglight:dh-domain:${project.version}`，默认 `compile` scope；目的仅为编译期引用 `FeedbackEnvironment`。`dh-domain` 不依赖 `dh-security`，两 module 的当前 production tree 不含 Spring/JDBC/Web runtime，故依赖方向已审核为无环。不得修改 root POM、`dh-domain/pom.xml`、`dependencyManagement`、repository、plugin、profile、Java 或 test plugin 配置。

`dh-security` 对 `dh-domain` 的 future import 仅允许 `FeedbackEnvironment`。`FeedbackExecutionScope` 继续只能在 verified root boundary 创建；不得在 authenticator 内创建、导入或以 `String`、`Map`、默认 `DEV` 取代。

### B4_ENVIRONMENT_AUTHORITY_CONTRACT_SCOPE

~~~text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackEnvironment.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java (NEW)
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeException.java (NEW)
~~~

### B4_AUDIT_UPSTREAM_BOUNDARY_SCOPE

~~~text
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketController.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/QdrModelGatewayIntegrationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/CreateApprovalPacketCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/SubmitApprovalDecisionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
~~~

### B4_AUDIT_SECURITY_BINDING_SCOPE

~~~text
dh-security/src/main/java/com/guidinglight/decisionhub/security/AuthContext.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/AuthenticatedRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderNames.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NormalizedNqDhHeaders.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderParser.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderValidator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderValidationResult.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacHumanApprovalEnvironmentAuthenticator.java (NEW)
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HumanApprovalEnvironmentAuthRequest.java (NEW)
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRuntimeProperties.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
~~~

HmacHumanApprovalEnvironmentAuthenticator 与其 request type 是 scope freeze 的新边界名称，不是本任务已经创建的实现；它们的唯一职责是使 Human Approval 的外部 environment 遵守与 dry-run 相同的 signed tenant/source/environment contract。不得改用 bearer tenant、profile 或 controller constant 推断 environment。

### B4_REPLAY_DORMANT_ENTRY_CONTRACT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveReplayCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
~~~

### B4_EVALUATION_DORMANT_ENTRY_CONTRACT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/SaveEvaluationCaseCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/EvaluationCaseRecord.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/EvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
~~~

两个 dormant scope 明确排除 dh-app production wiring、dh-api controller、scheduler、startup runner 与 registry transition。U2 只能让 future caller 显式传入 scope，不能制造 caller。

### B4_UPSTREAM_ENVIRONMENT_TEST_SCOPE

~~~text
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeTest.java (NEW)
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacHumanApprovalEnvironmentAuthenticatorTest.java (NEW)
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderNamesTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderParserTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderValidatorTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/HumanApprovalPacketControllerWebMvcTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
~~~

### B4_UPSTREAM_ERROR_CLASSIFICATION_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunErrorCode.java
~~~

未来仅允许增加冻结的结构化 environment 拒绝分类；不得删除既有错误码、改变无关 HTTP 映射、把环境错误返回为成功或扩大为自由文本错误。

### B4_UPSTREAM_ORCHESTRATOR_PROPAGATION_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestrator.java
~~~

未来仅允许接收或传播 verified `FeedbackExecutionScope`，保证 tenant/environment 经过 orchestrator 到 AUDIT persistence 不漂移；不得在该层创建默认 `DEV`、从 profile 或 tenant 推断环境、改变决策业务语义或启动 dormant runtime。

### B4_UPSTREAM_FINGERPRINT_SCOPE

~~~text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprint.java
~~~

未来必须把 verified `FeedbackEnvironment` 放入确定性 canonical fingerprint。同 tenant/source/request、不同 environment 的 fingerprint 必须不同；同 environment 的 fingerprint 必须稳定。禁止 enum hashCode、对象 identity hash、非确定序列化、忽略 environment 或默认值。

### B4_UPSTREAM_COMPATIBILITY_TEST_SCOPE

~~~text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
~~~

未来仅允许升级既有 PostgreSQL/Testcontainers fixture：显式提供 `DEV` 或 `TEST` environment、重算 canonical HMAC、验证 fingerprint 分隔，并保留 recovery、repeatability 与 capacity 断言强度。旧无 environment 签名请求必须被拒绝；不得默认 `DEV`、跳过签名、禁用 environment 校验或降低 mandatory integration path。

### B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE

~~~text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
~~~

该精确 scope 只允许升级这个 existing PostgreSQL fixture 的请求构造：调用点或严格必填 helper 必须显式写出 `FeedbackEnvironment.DEV` 或 `FeedbackEnvironment.TEST`；environment 属于 canonical HMAC，verified root 必须交付 `FeedbackExecutionScope`。不得引入 default DEV、nullable environment、profile inference、mock/bypass authenticator，也不得把 production guard 的 `403` fail-closed 变成 fixture success。原 persistent wiring 成功、数据库基础设施失败、nonce/retry/recovery 与 Testcontainers 断言必须保留。

## 3. Required implementation sequence

~~~text
U1: implement the shared immutable scope, signed AUDIT contract, tenant/source/environment authorization,
    root construction, propagation, and fail-closed native/registry atomicity.
U2: add only explicit dormant REPLAY/EVALUATION command contracts and tests; no production wiring.
V16: permitted for planning/review only after U1 and U2 technical acceptance; migration creation needs separate authorization.
Producer integration: only a verified real caller can integrate; otherwise remain dormant.
Retention: reimplementation remains blocked by missing/unknown/active registry state.
~~~

Future transaction boundary is one DH PlatformTransactionManager, PROPAGATION_REQUIRED, ISOLATION_REPEATABLE_READ; native write and registry transition must roll back together. No asynchronous, after-commit or best-effort substitute is allowed.

## 4. Scope invariants and acceptance tests

~~~text
B4_ENVIRONMENT_AUTHORITY_CONTRACT_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_AUDIT_UPSTREAM_BOUNDARY_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_AUDIT_SECURITY_BINDING_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_REPLAY_DORMANT_ENTRY_CONTRACT_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_EVALUATION_DORMANT_ENTRY_CONTRACT_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_ENVIRONMENT_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_ERROR_CLASSIFICATION_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_ORCHESTRATOR_PROPAGATION_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_FINGERPRINT_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_COMPATIBILITY_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE ⊆ FUTURE_WRITE_ALLOWLIST: PASS
ORIGINAL_TRUSTED_UPSTREAM_SCOPE: 42 / 42 PASS / BLOCKED DURING IMPLEMENTATION / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
CORRECTED_UPSTREAM_SCOPE: 46 / 46 PASS / BLOCKED BY UNAUTHORIZED MAVEN DEPENDENCY FILE / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
EFFECTIVE_UPSTREAM_SCOPE_INVARIANTS: 48 / 48 PASS
TASK_SCOPE_DESIGN_INVALID: NO
~~~

Acceptance must cover canonical environment signing, tamper/missing rejection, DEV/TEST acceptance, PROD/LIVE/unknown rejection, tenant/source/environment mismatch, no scope on unverified input, full AUDIT propagation, environment-bound fingerprint stability/separation, upgraded PostgreSQL compatibility fixtures, two-way rollback, dormant producer absence and architecture guards. The implementation task must not add V16, retention, controller/scheduler entrypoints for REPLAY/EVALUATION, automatic learning, real HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE behavior.
