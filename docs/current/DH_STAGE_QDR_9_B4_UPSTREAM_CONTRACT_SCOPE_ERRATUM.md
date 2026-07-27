# DH Stage-QDR-9 B4 Upstream Contract Scope Erratum

## 1. 任务结论

```text
task: DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-SCOPE-RETRY
classification: DOCUMENTATION / SCOPE_CONTRACT_ERRATUM / CURRENT_FACTSOURCE_ALIGNMENT
baseline: 75c2449972c4b6f15689144478f31c0b7edf8126
technical changes: 0
blocker: STAGE_QDR_9_B4_UPSTREAM_CONTRACT_SCOPE_BLOCKER
original trusted-upstream scope: 42 / 42 PASS
original scope status: BLOCKED DURING IMPLEMENTATION
corrected upstream scope: 46 / 46 PASS
V16 / registry / retention: NOT CREATED / NOT STARTED / NOT PRESENT
upstream contract implementation: BLOCKED BEFORE CODE WRITE
```

本 erratum 不改写 42-file scope 的历史事实。原 `42 / 42 PASS` 已完整覆盖当时冻结的集合，但首次实施前代码审计证明其未覆盖全部必需的生产传播与兼容性测试路径。因此原 scope 对 upstream implementation acceptance 的适用性为：

```text
ORIGINAL TRUSTED-UPSTREAM SCOPE
BLOCKED DURING IMPLEMENTATION
SUPERSEDED FOR UPSTREAM IMPLEMENTATION ACCEPTANCE
```

本轮只补齐 scope 合同；未实施 `FeedbackExecutionScope`、signed environment、HMAC、registry、retention、V16 或任何运行时行为。

## 2. 阻断证据

首次 upstream implementation preflight 发现以下六个路径不在原 42-file `WRITE_ALLOWLIST` 中：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprint.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
```

它们分别承载结构化 environment 拒绝、verified scope 经过 orchestrator 的传播、environment-bound idempotency fingerprint，以及既有 PostgreSQL/Testcontainers 成功签名 fixture 的显式 environment 升级。缺失任何一项都会导致 implementation 要么无法给出冻结错误分类，要么丢失 environment，要么通过默认 `DEV` 维持旧测试，均不允许。

## 3. 新增精确 scope

### B4_UPSTREAM_ERROR_CLASSIFICATION_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunErrorCode.java
```

仅允许增加冻结的 `ENVIRONMENT_REQUIRED`、`ENVIRONMENT_INVALID`、`ENVIRONMENT_NOT_AUTHORIZED`、`TENANT_ENVIRONMENT_MISMATCH` 与 `SOURCE_ENVIRONMENT_NOT_AUTHORIZED`（或其更具体的等价分类）。不得删除既有错误码、把环境拒绝映射为成功，或扩大为自由文本错误。

### B4_UPSTREAM_ORCHESTRATOR_PROPAGATION_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestrator.java
```

仅允许接收或传播 verified `FeedbackExecutionScope`，并保持 tenant/environment 到 AUDIT persistence 的一致性。禁止默认 `DEV`、profile/tenant 推断、ThreadLocal、决策语义变更或 REPLAY/EVALUATION runtime wiring。

### B4_UPSTREAM_FINGERPRINT_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprint.java
```

canonical fingerprint 必须纳入 verified `FeedbackEnvironment`：同 payload、同 environment 的结果稳定；同 tenant/source/request、不同 environment 的结果不同。禁止 enum/object identity hash、非确定序列化或忽略 environment。

### B4_UPSTREAM_COMPATIBILITY_TEST_SCOPE

```text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
```

仅允许升级签名 fixture：显式传入 `DEV` 或 `TEST`、重新计算 canonical HMAC，并保留 recovery、repeatability、capacity 与 PostgreSQL/Testcontainers 的原有断言强度。旧的无 environment 签名请求必须被拒绝；禁止默认 `DEV`、跳过签名或降低断言。

## 4. 冻结传播与错误合同

未来实现必须遵循：

```text
verified inbound environment
→ verified AuthContext
→ FeedbackExecutionScope
→ DecisionOrchestrator
→ AUDIT command/context
→ audit persistence boundary
```

每层 environment 均为 non-null，且不得重新解析、重新推断或提供默认值。缺失 environment 返回 `ENVIRONMENT_REQUIRED`；不支持值返回 `ENVIRONMENT_INVALID`；有效 enum 但 tenant/source/environment 未授权返回 `ENVIRONMENT_NOT_AUTHORIZED` 或更具体冻结错误。HMAC 篡改仍可返回 `SIGNATURE_INVALID`，具体安全检查顺序必须由实现时的签名合同固定。

## 5. Scope 治理与后续

```text
Reference-liveness design: 27 / 27 PASS
Producer environment contract: 31 / 31 PASS
Producer environment source design: 36 / 36 PASS
Original trusted-upstream scope: 42 / 42 PASS / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
B4_UPSTREAM_ERROR_CLASSIFICATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_ORCHESTRATOR_PROPAGATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_FINGERPRINT_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B4_UPSTREAM_COMPATIBILITY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
EFFECTIVE_UPSTREAM_SCOPE_INVARIANTS: 46 / 46 PASS
TASK_SCOPE_DESIGN_INVALID: NO
ALLOW_UPSTREAM_CONTRACT_IMPLEMENTATION_RETRY: YES / NEXT TASK ONLY
ALLOW_V16_IMPLEMENTATION / B4 REVIEW RETRY / B4 PUBLICATION / B5: NO / NO / NO / NO
```

本 erratum 不构成技术 acceptance。下一步仅为 `DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION-RETRY`；其仍必须在实际代码、PostgreSQL/Testcontainers、全量回归与质量门全部通过后，才可单独评估后续阶段。

## 6. Maven module dependency scope retry

`DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-MAVEN-DEPENDENCY-SCOPE-RETRY` 在任何实现写入前发现，future `AuthContext` 与 `HmacNqDryRunAuthenticator` 需引用 canonical `FeedbackEnvironment`，而 `dh-security` 当前未依赖其 owner `dh-domain`。该 compile blocker 不是可通过 `String`、默认 `DEV`、security-local enum 或类型迁移绕过的问题。

实际 Maven reactor/DAG 审计确认 `dh-domain` 不直接或传递依赖 `dh-security`；`dh-domain` 与 `dh-security` 当前 production tree 均只到 `dh-common` 和 Jackson。故直接加入 `dh-security -> dh-domain` 无 Maven cycle，且不会将 Spring、JDBC、Web、provider 或 security implementation concern 引入 domain/security 边界。

~~~text
MAVEN_MODULE_DEPENDENCY_SCOPE: dh-security/pom.xml ONLY
AUTHORIZED FUTURE DEPENDENCY: com.guidinglight:dh-domain:${project.version}
MAVEN SCOPE: compile (default)
PERMITTED DOMAIN IMPORT: FeedbackEnvironment ONLY
ROOT dependencyManagement sufficient to omit reactor version: NO
POM / Java / test / migration change during this retry: 0 / 0 / 0 / 0
B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
EFFECTIVE_UPSTREAM_SCOPE_INVARIANTS: 47 / 47 PASS
~~~

原 corrected `46 / 46 PASS` 保留为精确历史 scope，但其 implementation acceptance 状态升级为 `BLOCKED BY UNAUTHORIZED MAVEN DEPENDENCY FILE / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE`。仅在已冻结的 `dh-security/pom.xml` 中加入上述 reactor dependency 后，才可进行下一任务；任何其他 POM 需求均必须以 `STAGE_QDR_9_B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE_BLOCKER` 停止。
