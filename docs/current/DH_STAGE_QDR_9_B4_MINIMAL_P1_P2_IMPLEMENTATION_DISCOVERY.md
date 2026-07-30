# DH Stage-QDR-9 B4 Minimal P1/P2 Implementation Discovery

## 0. Discovery consumption result — 2026-07-31

~~~text
task:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-IMPLEMENTATION

complete discovery:
CONSUMED / PASS

minimal remediation path:
M1

technical P1 / technical P2:
IMPLEMENTED / LOCAL_ACCEPTED
IMPLEMENTED / LOCAL_ACCEPTED

current technical tree:
MINIMAL ENVIRONMENT-BOUND REMEDIATION IMPLEMENTED LOCALLY

actual scope:
20 PRODUCTION / 16 TEST / 0 MIGRATION / 5 AUTHORITY / 0 UNEXPECTED

legacy compatibility / migration:
NOT REQUIRED / OWNER-CONFIRMED NO REAL DATA
NOT REQUIRED

full regression / quality:
PASS / 19 OF 19 / 1243 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PASS / 19 OF 19 / CHECKSTYLE 0 / SPOTLESS PASS

remote publication:
PENDING MILESTONE REVIEW

production capacity:
NOT_PROVEN

next action:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-MILESTONE-REVIEW
~~~

四份 exact allowlist 保持冻结且未扩充；implementation 未创建 scope erratum、migration、
legacy compatibility/retirement/backfill、server deployment、push 或 tag。

## 1. Discovery decision

~~~text
task:
DH-STAGE-QDR-9-B4-OWNER-ATTESTED-M1-REBASELINE-AND-MINIMAL-IMPLEMENTATION-DISCOVERY

baseline:
229910e31b9b5cba5fe944e4f3a497df73843d20

owner attestation:
ACCEPTED / FIRST_PARTY_OPERATIONAL_ATTESTATION

selected path:
M1

complete discovery:
PASS

minimal implementation scope:
FROZEN

migration required:
NO

legacy retirement / backfill / registry / retention:
NOT REQUIRED / NOT REQUIRED / NOT REQUIRED / NOT REQUIRED
~~~

本文件记录一次性 production call graph、persistent identity、replay、audit、transaction、constructor、
PostgreSQL fixture、capacity、architecture 与 Maven dependency discovery。它不实施任何代码、测试、
POM 或 migration。

## 2. Current code reality

### 2.1 Security and root path

| Path | Current reality | Minimal change |
|---|---|---|
| `DecisionDryRunRequest` | 无 environment 字段 | 增加 canonical DEV/TEST wire environment |
| `AuthenticatedRequest` | 可读取完整 `AuthContext`，但只有 tenant 是 required helper | 增加 required auth context/environment fail-closed 读取 |
| `AuthContext` | 只有 user/tenant/roles | 增加 canonical `FeedbackEnvironment`，保留非 dry-run compatibility constructor |
| `StaticTokenVerifier` / `SecurityWiringConfig` | bearer identity 未绑定 environment | 从既有 guard environment 配置建立 trusted DEV/TEST auth context |
| `NqDryRunAuthRequest` | canonical material input 无 environment | 同时携带 body 与 authenticated environment |
| `HmacNqDryRunAuthenticator` | HMAC 与 allowlist 只绑定 tenant/source | 签名、authorization、replay key 绑定 environment |
| `DecisionDryRunController` | 先创建 command，再 auth；rate call 无 scope | auth 成功后取得唯一 verified scope，再绑定 rate/command |

Verified `FeedbackExecutionScope` 的唯一创建点必须位于
`HmacNqDryRunAuthenticator.authenticate` 的 signature、tenant/source/environment 检查之后。
body、Spring profile、guard config 或 header 不能单独创建 trusted scope。

### 2.2 Persistent rate

- `PersistentDecisionDryRunRateLimiter` 当前使用
  `DecisionDryRunGuardProperties.environment()`。
- `RateLimiter` 当前 API 没有 environment/scope 参数。
- V12 rate primary key 已有 environment，不需要 JDBC/DDL 变化。
- 最小实现通过 `RateLimiter` 的 scope-aware overload 把 verified scope 传入 persistent
  implementation；旧 NQ feedback in-memory path 保持不变。
- verified scope 与 guard configuration 不一致时返回 configuration-invalid，不允许 fallback。
- admission 与 rate audit 继续共用 `GuardTransactionBoundary.required`。

### 2.3 Idempotency, recovery and request fingerprint

- `PersistentGuardedDecisionDryRunService.identity` 当前使用 guard config environment。
- admit、transition、completed duplicate、expired lease recovery 都使用同一个
  `PersistentGuardIdentity`；只需替换其 environment authority，无需改 JDBC port。
- V12 idempotency unique key 已包含 environment。
- `DecisionDryRunRequestFingerprint` 当前不含 environment，存在跨环境同 hash 风险。
- `DecisionDryRunCommand` 必须携带 verified scope；fingerprint 与 identity 都从该 scope 取值。
- `DefaultDecisionDryRunService` 必须在进入 Decision Core / `DecisionOrchestrator` 前验证
  scope 非空且 tenant/source 与 command 完全一致。
- `DecisionOrchestrator`、`DefaultDecisionOrchestrator`、gateway command/service 与 generic
  audit constructors 不需要修改；它们位于 verified root 后，且不是当前 guard identity owner。

### 2.4 Replay namespace

- 当前 key：
  `tenant::source::path::nonce::requestId`。
- 最小目标：
  `environment::tenant::source::path::nonce::requestId`，由 verified scope 生成。
- `NonceReplayGuard` port 与 `JdbcNonceReplayGuard` 无需修改。
- V4 `replay_key varchar(512) primary key` 足以存储新 key；新 key 长度仍受已验证输入边界约束。
- TTL 仍为 `now + 2 * maxClockSkew`；现有 cleanup 行为保持，不新增 scheduler。

### 2.5 QDR7 rate audit

- 当前 QDR7 rate audit `event_json` 只含 endpoint/source/status。
- V5 `event_json jsonb not null` 已是 structured storage。
- 最小实现增加 `environment: DEV|TEST`，值只能来自 verified scope。
- 不增加 generic audit column，不修改 `AuditEventRecord`、`DecisionAuditRepository` 或
  `JdbcDecisionAuditRepository`。
- admission 和 audit 仍在同一 transaction 内；audit 失败必须 rollback admission。

## 3. Migration reality

~~~text
highest current migration:
V15

V16 / V17 / V18:
ABSENT / ABSENT / ABSENT

Can environment isolation use existing columns/keys?
YES

Is a forward migration required for structured rate-audit environment?
NO

Can DEV/TEST databases be rebuilt?
YES

Is legacy retirement migration required?
NO
~~~

结构化 audit 不等于必须新增 dedicated column。当前 P1 是 producer integrity finding，
`event_json` 的 named `environment` field 可以无损表达 verified enum，且当前没有按 environment
查询 rate audit 的 authorized read API。新增列、index、backfill 或 staged constraint 会扩大范围，
不属于关闭 P1/P2 的必要条件。

## 4. Constructor and caller discovery

已反向检查以下 constructor/caller 集合：

- `new AuthContext(...)`：production `StaticTokenVerifier` 与全部 API security fixture；
- `new NqDryRunAuthRequest(...)`：Controller、HMAC unit、capacity、actual wiring、same-pool recovery；
- `new HmacNqDryRunAuthenticator(...)`：Spring wiring、Controller MVC 与 HMAC unit；
- `new DecisionDryRunCommand(...)`：Controller、persistent wiring fixture、fingerprint/default/limited/
  persistent service tests；
- `new PersistentGuardIdentity(...)`：rate/idempotency production、cleanup、JDBC mapping 与
  PostgreSQL fixtures；
- `new AuditEventRecord(...)`：QDR7 rate/idempotency、orchestrator、approval、gateway、JDBC tests；
- `new PersistentGuardedDecisionDryRunService(...)`：Spring wiring 与 usecase regression；
- `new DefaultDecisionDryRunService(...)` / `new DefaultDecisionOrchestrator(...)`：全部 production
  wiring 与 compatibility fixtures。

兼容策略：

- 非 dry-run 的旧 `AuthContext` constructor 保留；只有 dry-run root 强制 environment；
- generic `RateLimiter` old path 保留给 NQ feedback；persistent dry-run old path在 runtime enabled
  时不得使用 unverified environment；
- generic audit constructor 不变；
- implementation 不修改 `DecisionOrchestrator` 或 generic gateway contract。

## 5. PostgreSQL, capacity and architecture discovery

必须覆盖：

- V12 clean migration 与 rate/idempotency exact identity；
- production wiring 的 DEV/TEST rate/idempotency isolation；
- actual Spring wiring replay repeatability；
- same-pool recovery 与 cross-environment fingerprint/identity；
- JDBC replay persistence；
- QDR7 capacity fixture 的 signed environment 和 auth context；
- MVC root fail-closed、environment mismatch/unsupported environment；
- architecture guard：scope 为 domain-owned、security 只依赖 canonical domain type、无 real NQ/
  Provider/HTTP/Agent/LangGraph/server surface。

`DecisionContractGapGuardTest` 必须更新 limited-runtime source allowlist，因为新的 domain-owned
`FeedbackExecutionScope` 会合法包含 canonical `NQ_DRYRUN` source。

## 6. Maven dependency discovery

~~~text
new module dependency required:
dh-security -> dh-domain

purpose:
AuthContext, HMAC auth request/result and RateLimiter use the single canonical
FeedbackEnvironment / FeedbackExecutionScope types

other POM changes:
NONE
~~~

该依赖保持方向 `security -> domain`，不引入 Spring、JDBC、API、infra、NQ 或 provider 依赖。

## 7. Exact allowlists

### MINIMAL_PRODUCTION_WRITE_ALLOWLIST

~~~text
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/AuthenticatedRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/SecurityWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackEnvironment.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeException.java
dh-security/pom.xml
dh-security/src/main/java/com/guidinglight/decisionhub/security/AuthContext.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/StaticTokenVerifier.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthResult.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/RateLimiter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprint.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
~~~

### MINIMAL_TEST_WRITE_ALLOWLIST

~~~text
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScopeTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuardPersistenceTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRunRequestFingerprintTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/LimitedDryRunRuntimeServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunServiceTest.java
~~~

### MINIMAL_MIGRATION_ALLOWLIST

~~~text
EMPTY
~~~

### MINIMAL_CURRENT_AUTHORITY_ALLOWLIST

~~~text
docs/current/STATUS.md
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md
docs/current/DH_STAGE_QDR_9_B4_MINIMAL_P1_P2_IMPLEMENTATION_DISCOVERY.md
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
docs/current/WORKLOG.md
~~~

## 8. Stop rule and readiness

~~~text
ALLOW_MINIMAL_P1_P2_IMPLEMENTATION:
YES / NEXT TASK ONLY / EXACT ALLOWLIST ONLY

ALLOW_SERVER_DEPLOYMENT:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_B4_REVIEW_PUBLICATION:
NO

ALLOW_B5:
NO

PRODUCTION_CAPACITY:
NOT_PROVEN
~~~

Implementation 如发现 allowlist 外必需文件：

~~~text
STOP IMPLEMENTATION
RETURN TO COMPLETE DISCOVERY
~~~

不得新增 scope erratum，不得恢复 legacy retirement、backfill、registry、retention、scheduler、
real NQ、real Provider、real HTTP、Agent/LangGraph 或 server deployment。
