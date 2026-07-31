# DH Stage-QDR-9 B4 Producer Environment Upstream Authority Decision

## 1. 决策状态

~~~text
task: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
classification: DOCUMENTATION / SECURITY_ARCHITECTURE / TRUSTED_ENVIRONMENT_AUTHORITY
design baseline: 9e7dfdc557b4a8d5c0768bdd3414f775971c0d73
parent: 2cc75dcc8c2228af43e79d7163d7489bf67ebc97
origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3
code / test / migration / configuration change: NONE
AUDIT_ENVIRONMENT_AUTHORITY: NEW EXPLICIT SIGNED CONTRACT REQUIRED
AUDIT_AUTHORITY_OPTION: B
REPLAY_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
EVALUATION_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
FEEDBACK_EXECUTION_SCOPE: FROZEN / NOT IMPLEMENTED
V16: CANDIDATE / NOT CREATED
ALLOW_V16_IMPLEMENTATION: NO
~~~
本决策解决的是 environment authority 的设计选择，不把尚未实现的上游合同或 registry 写入描述为已可用。FeedbackEnvironment 是 tenant-scoped routing/security attribute，不是 Spring profile、部署配置或普通业务扩展字段。

## 1.1 Maven module dependency decision

~~~text
task: DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-MAVEN-DEPENDENCY-SCOPE-RETRY
canonical environment type: FeedbackEnvironment
type owner: dh-domain
authorized dependency: dh-security -> dh-domain / APPROVED
scope: compile
future POM file: dh-security/pom.xml ONLY
permitted import: FeedbackEnvironment ONLY
forbidden import: FeedbackExecutionScope and all other domain qdr.feedback types
~~~

Maven reactor 审计证明 `dh-domain` 仅依赖 `dh-common`，不直接或传递依赖 `dh-security`；`dh-security` 当前也只依赖 `dh-common`。因此该单向依赖不会形成 cycle，也不会把 Spring、JDBC、Web、provider 或 security implementation concern 反向带入 domain。root `dependencyManagement` 不管理 reactor module version，future dependency 必须沿用当前 `${project.version}` 约定。该决定只授权 future POM 的单一 dependency 条目，不代表其已添加，且不授权 HMAC binding、`FeedbackExecutionScope`、AUDIT propagation 或 V16 implementation。

## 2. AUDIT 现状事实矩阵

| 检查项 | DecisionDryRunController.decide | HumanApprovalPacketController.createApprovalPacket / submitApprovalDecision |
| --- | --- | --- |
| controller 与入口方法 | EXPLICIT | EXPLICIT |
| request / command contract | EXPLICIT | EXPLICIT |
| tenant 来源 | VERIFIED | VERIFIED |
| source identity | VERIFIED | NOT_AVAILABLE |
| HMAC verification point | VERIFIED | NOT_AVAILABLE |
| timestamp validation | VERIFIED | NOT_AVAILABLE |
| nonce replay protection | VERIFIED | NOT_AVAILABLE |
| source allowlist | VERIFIED | NOT_AVAILABLE |
| signed fields | VERIFIED | NOT_AVAILABLE |
| environment field | NOT_AVAILABLE | NOT_AVAILABLE |
| environment covered by signature | NOT_AVAILABLE | NOT_AVAILABLE |
| tenant/environment consistency check | NOT_AVAILABLE | NOT_AVAILABLE |
| FeedbackExecutionScope creation point | NOT_AVAILABLE | NOT_AVAILABLE |
| native-audit / registry transaction owner | NOT_AVAILABLE | NOT_AVAILABLE |

已核验的 dry-run 链为：

~~~text
DecisionDryRunController.decide
→ AuthenticatedRequest.requireTenantId
→ NqDhHeaderParser / NqDhHeaderValidator
→ HmacNqDryRunAuthenticator
→ persistent nonce guard 与 rate limiter
→ DecisionDryRunCommand
→ PersistentGuardedDecisionDryRunService / DefaultDecisionDryRunService
→ DefaultDecisionOrchestrator / DefaultQdrModelGatewayIntegrationService
→ DecisionPersistenceRecords.AuditEventRecord
→ DecisionAuditRepository.saveAuditEvent
→ JdbcDecisionAuditRepository.saveAuditEvent
~~~

Human Approval 的 controller 从 AuthenticatedRequest 获得 tenant，再构造 approval command 并由 HumanApprovalPacketCommandService 写入 AuditEventRecord。它没有 source、timestamp、nonce 或 HMAC environment authority。两条 controller 路径因此都不能在当前树中创建可信 FeedbackExecutionScope。

## 3. 现有 HMAC 与租户绑定审计

HmacNqDryRunAuthenticator 在签名校验后校验 source allowlist 与 tenant/source pair；时间戳窗口、nonce replay guard、payload-size gate 均已存在并 fail-closed。其 canonical material 的现有顺序为：

~~~text
method
path
source
tenantId
requestId
traceId
timestamp
nonce
schemaVersion
sha256(rawBody)
~~~

NqDryRunAuthRequest、NormalizedNqDhHeaders、NqDhHeaderNames、DecisionDryRunRequest 与 AuthContext 均无 FeedbackEnvironment。因此 environment 既不存在于显式 wire contract，也未被 HMAC 覆盖；现有 DecisionDryRunRuntimeProperties.devOrTestProfile 与 guard deployment environment 只能控制运行配置，**不得**被解释为 producer environment authority。

## 4. Authority Decision — Option B

唯一选项为：

~~~text
AUDIT_ENVIRONMENT_AUTHORITY: NEW EXPLICIT SIGNED CONTRACT REQUIRED
AUDIT_LIFECYCLE_PRODUCER: NOT INTEGRATED UNTIL U1 ACCEPTANCE
~~~

Option A 不成立，因为无任何现有 AUDIT request/context 同时具备 explicit environment、HMAC coverage、DEV/TEST restriction 与 tenant/environment joint authorization。Option C 不选作最终设计结论：现有 AUDIT root 可接受经扩展的可信入站合同；但在 U1 完成前它仍不得写 registry 或启动 retention lifecycle。

未来每个外部 AUDIT root 的 environment 必须在认证完成后、业务 use-case 前验证以下全部条件：

~~~text
source authentication completed
payload-size gate passed
timestamp valid
nonce valid
source allowlist passed
tenant validated
environment explicitly supplied
environment included in canonical HMAC representation
environment = DEV or TEST
tenant/source/environment combination authorized
~~~

禁止以未签名 header、未签名 body extension、query parameter、Spring profile、server default、tenant mapping、controller constant、ThreadLocal、static global 或 repository reverse lookup 提供 environment。

未来 canonical representation 必须把 environment 作为独立、固定顺序字段，且 authorization key 至少绑定：

~~~text
tenantId + source + environment
~~~

篡改 environment 必须导致签名失败；缺失、PROD、LIVE、unknown 或未授权组合必须在 root 失败，且 native AUDIT 写入与 registry transition 均为零。

## 5. FeedbackExecutionScope 根合同

唯一根类型固定为：

~~~text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java
~~~

future immutable record 仅含 tenantId 与 FeedbackEnvironment environment，并要求 tenant non-blank、environment non-null、environment 仅 DEV/TEST。创建权仅属于已经验证 tenant/source/environment binding 的 root adapter 或受信内部调用方；repository、JDBC adapter、retention service、registry adapter、Spring configuration default 均不得创建或推断它。

AUDIT root adapter 的创建点固定为：

~~~text
authentication + signature + timestamp + nonce + source + tenant/environment validation
→ FeedbackExecutionScope
→ business use-case
~~~

## 6. Dormant REPLAY / EVALUATION 决策

QdrRegressionEvaluationService.evaluate(QdrRegressionEvaluationCommand) 当前只在测试中构造；未发现 API caller、Spring bean wiring、startup invocation 或 scheduler。因此：

~~~text
REPLAY_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
EVALUATION_PRODUCER_RUNTIME: DORMANT / NO PRODUCTION ENTRY
~~~

U2 仅冻结 future explicit root-command contract：caller 必须接收 FeedbackExecutionScope，无 environment default、无 profile inference、无 Spring 自动 wiring、无启动执行、无 scheduler、无外部 API。真实 caller 出现并通过独立 acceptance 前，不执行 native write + registry transition integration；其 registry row 缺失、unknown 或 active 均继续阻断 retention。

## 7. 事务与阶段顺序

AUDIT future design 固定为 verified scope、native audit persistence 与 FeedbackReferenceLivenessPort.activate(AUDIT, ...) 同一个 DH PlatformTransactionManager、Propagation.REQUIRED、REPEATABLE_READ 事务。native 或 registry 任一失败必须整体 rollback。当前 ApprovalWriteBoundary 未显式固定该 isolation，且 registry 尚不存在；这不是已实现事实。

REPLAY/EVALUATION 只有在真实可信 caller 出现后才适用同一事务规则；本任务不授权提前创建 runtime Bean。

~~~text
U1: explicit signed AUDIT environment authority + FeedbackExecutionScope + fail-closed propagation
U2: dormant REPLAY/EVALUATION explicit root command contracts; no production wiring
V16: only after U1/U2 code and tests are accepted
Producer integration: only a real, trusted caller; absent caller remains dormant
Retention: missing / unknown / active registry state blocks deletion
~~~

## 8. 冻结测试矩阵

1. canonical HMAC 包含 environment；environment tamper invalidates signature；unsigned/missing environment rejected。
2. DEV/TEST accepted；PROD/LIVE/unknown rejected；tenant/source/environment mismatch rejected；nonce/timestamp protection remains active。
3. verified request creates FeedbackExecutionScope；unverified request creates no scope；无 default DEV、profile inference 或 ThreadLocal fallback。
4. AUDIT 中 verified environment = scope = AuditEventRecord propagation = registry environment；validation failure writes neither；native/registry failure mutually roll back。
5. REPLAY/EVALUATION 无 production wiring、无 startup invocation、无 controller；missing registry remains retention-blocking。
6. Architecture guards forbid raw unsigned environment header、profile lookup、automatic producer、scheduler、V16 before U1/U2 acceptance。

## 9. Readiness

## 9.1 Scope-retry erratum

首次 implementation preflight 证明原 trusted-upstream `42 / 42 PASS` scope 未覆盖 `DecisionDryRunErrorCode`、`DecisionOrchestrator`、`DecisionDryRunRequestFingerprint` 与三个既有 PostgreSQL compatibility tests。42-file 集合仍是 authority design 的原始审计事实，但对 implementation acceptance 固定为：

~~~text
ORIGINAL TRUSTED-UPSTREAM SCOPE: 42 / 42 PASS
IMPLEMENTATION PRECHECK: BLOCKED BEFORE CODE WRITE
SCOPE ERRATUM: DONE / 6 EXACT FILES ADDED
CORRECTED EFFECTIVE UPSTREAM SCOPE: 46 / 46 PASS
~~~

erratum 只补齐 future implementation 的 write/test scope；它不实施 signed environment、`FeedbackExecutionScope`、AUDIT propagation、V16、registry 或 retention。缺失 environment 仍必须 root fail-closed，且旧无 environment fixture 不得通过默认 `DEV` 保持兼容。

~~~text
UPSTREAM_ENVIRONMENT_AUTHORITY_DESIGN: DONE
AUDIT_AUTHORITY_OPTION: B
AUDIT_ENVIRONMENT_AUTHORITY: FROZEN / NEW EXPLICIT SIGNED CONTRACT REQUIRED
HMAC_ENVIRONMENT_BINDING: FROZEN / IMPLEMENTATION REQUIRED
FEEDBACK_EXECUTION_SCOPE: FROZEN / NOT IMPLEMENTED
REPLAY_RUNTIME: DORMANT
EVALUATION_RUNTIME: DORMANT
NO_DEFAULT_ENVIRONMENT: PASS
MISSING_ENVIRONMENT_FAIL_CLOSED: PASS
ALLOW_UPSTREAM_CONTRACT_IMPLEMENTATION_RETRY: YES / NEXT TASK ONLY
ALLOW_V16_IMPLEMENTATION: NO
ALLOW_B4_MILESTONE_REVIEW_RETRY: NO
ALLOW_B4_PUBLICATION: NO
ALLOW_B5_IMPLEMENTATION: NO
~~~

## 9.2 Persistent guard compatibility scope erratum

`PersistentGuardProductionWiringPostgresTest` 在本次 implementation continuation 的新鲜全量回归中是唯一失败类：三个 legacy request/command 没有 signed `FeedbackEnvironment` 与 verified `FeedbackExecutionScope`，因此正确触发 root `403` fail-closed。这个结果证明 security contract 生效，不得通过 default DEV、profile inference、HMAC bypass、mock authenticator 或放宽 production guard 恢复旧 fixture。

~~~text
B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE:
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
ORIGINAL EFFECTIVE SCOPE: 47 / 47 PASS
CORRECTED EFFECTIVE SCOPE: 48 / 48 PASS
PERSISTENT_GUARD_FIXTURE: SCOPE AUTHORIZED / TECHNICAL FIX PENDING
~~~

该 one-file PostgreSQL scope 只允许按显式 `DEV` 或 `TEST` 重建 signed fixture，并保留 persistent wiring 的 success、infrastructure failure、nonce/retry/recovery 与 Testcontainers 验证。它不授予 Human Approval lifecycle authority；REPLAY/EVALUATION 保持 dormant，V16、registry、retention 与 runtime expansion 均未获授权。
