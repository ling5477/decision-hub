# DH Stage-QDR-9 B4 Owner-Attested M1 Rebaseline and Minimal Remediation Plan

## Current authority — B4 readiness authority rebaseline — 2026-07-31

~~~text
Task:
DH-STAGE-QDR-9-B4-READINESS-AUTHORITY-BLOCKER

Authority baseline:
064774df6eabb75678b7a46cd01b524e870d3ca0

Local documentation commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

Minimal P1/P2 remediation:
CLOSED / ACCEPTED / PUBLISHED

Active P0 / P1:
0 / 0

Implementation exact-SHA CI:
30625943817 / PASS

Authority exact-SHA CI:
30626830036 / PASS

Stage-QDR-9 B4:
REMEDIATION CLOSED / FINAL CLOSE REVIEW PENDING

Stage-QDR-9 B4 active scope:
- signed environment authority
- tenant/source/environment authorization
- rate/idempotency environment isolation
- replay namespace isolation
- recovery/fingerprint isolation
- QDR7 rate-audit environment
- root fail-closed
- milestone and publication evidence

B4 active scope result:
IMPLEMENTED / ACCEPTED / PUBLISHED

Reference-liveness:
DEFERRED / NOT IMPLEMENTED

Reference-liveness state model:
DEFERRED / NOT IMPLEMENTED / FUTURE INDEPENDENT CAPABILITY STAGE

Reference-liveness registry:
NOT IMPLEMENTED / NOT A CURRENT B4 CLOSE GATE

Retention eligibility/deletion/audit:
DEFERRED / NOT IMPLEMENTED / FUTURE INDEPENDENT CAPABILITY STAGE

Retention:
DEFERRED / NOT IMPLEMENTED

V16:
HISTORICAL CANDIDATE / NOT AUTHORIZED

Historical V17/V18 sequence:
SUPERSEDED / NOT ACTIVE

Historical 54/60/66 scopes:
HISTORICAL DESIGN EVIDENCE / NOT ACTIVE IMPLEMENTATION GATES

QDR-7 B2 formal capacity:
DEFERRED / KNOWN LIMITATION

Formal capacity:
DEFERRED / KNOWN LIMITATION

Required for B4 functional close:
NO

Required for production-ready declaration:
YES

Production capacity:
NOT_PROVEN

DH deployment / real persistent data:
NONE / NONE

Primary current status:
docs/current/STATUS.md

Current execution order:
docs/current/WORK_ORDER.md

Stage authority:
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md

Canonical remediation evidence:
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md

Historical designs:
reference-liveness / retention / V16 / identity-replay / audit-storage documents

READINESS_AUTHORITY_BLOCKER:
RESOLVED

CURRENT_AUTHORITY_CONSISTENCY:
PASS / 0 CONFLICTS

B4_FINAL_CLOSE_REVIEW_ELIGIBLE:
YES

Next action:
DH-STAGE-QDR-9-B4-FINAL-CLOSE-REVIEW

ALLOW_V16_IMPLEMENTATION / ALLOW_RETENTION_IMPLEMENTATION / ALLOW_B4_CLOSE_NOW / ALLOW_B5:
NO / NO / NO / NO
~~~

本次重基线不改变 M1 remediation 的已验证实现事实，只明确完整 B4 的剩余决策边界。原始
retention 目标因无部署、无真实持久数据且无立即 retention safety requirement，透明延期到未来
独立 capability stage；reference-liveness 和 registry 同步延期，不是本阶段 close gate。

本节之后的 M1/M4、V16/V17/V18、54/60/66 scope 与旧 next action 均作为历史设计或执行证据
保留，不得被静默恢复为 current authority。

## Current authority — Minimal P1/P2 publication and authority close — 2026-07-31

~~~text
Owner operational attestation:
ACCEPTED

Minimal remediation path:
M1

Minimal P1/P2 remediation:
CLOSED / ACCEPTED / PUBLISHED

Discovery commit:
7fb0907acef477c04431445faf40e16db8dff0fb

Implementation commit:
6baabe113a3efa447ddd8036bb1b6c086ce1bcff

Milestone review:
PASS / P0 0 / P1 0 / PUBLICATION-BLOCKING P2 0

Implementation exact-SHA CI:
30625943817 / PASS

Signed environment:
PASS

Tenant/source/environment authorization:
PASS

Rate environment isolation:
PASS

Idempotency environment isolation:
PASS

Replay namespace isolation:
PASS

Recovery/fingerprint isolation:
PASS

QDR7 rate-audit environment:
PASS

Root fail-closed:
PASS

Legacy compatibility:
NOT REQUIRED / OWNER-CONFIRMED NO REAL DATA

Migration:
NOT REQUIRED

Implementation publication:
PASS / ORDINARY FAST-FORWARD / NO HISTORY REWRITE / NO FORCE PUSH

Server deployment:
NOT PERFORMED / NOT AUTHORIZED

Stage-QDR-9 B4 overall:
NOT CLOSED

V16:
NOT AUTHORIZED

B5:
NOT AUTHORIZED

Formal capacity gate:
NOT EXECUTED

Production capacity:
NOT_PROVEN

Next action:
DH-STAGE-QDR-9-B4-POST-REMEDIATION-READINESS-REASSESSMENT
~~~

本次 close 只结束 M1 minimal P1/P2 remediation milestone。它不改变 owner attestation 的
证据属性，不关闭完整 B4，也不授权 migration、legacy compatibility、registry、retention、
server deployment、V16、B5 或正式 capacity acceptance。

## 0. Minimal P1/P2 implementation result — 2026-07-31

~~~text
task:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-IMPLEMENTATION

MINIMAL_REMEDIATION_PATH:
M1

CURRENT_TECHNICAL_TREE:
MINIMAL ENVIRONMENT-BOUND REMEDIATION IMPLEMENTED LOCALLY

TECHNICAL_P1:
IMPLEMENTED / LOCAL_ACCEPTED

TECHNICAL_P2:
IMPLEMENTED / LOCAL_ACCEPTED

LEGACY_COMPATIBILITY:
NOT REQUIRED / OWNER-CONFIRMED NO REAL DATA

MIGRATION:
NOT REQUIRED

EXACT_SCOPE:
PASS / 20 PRODUCTION / 16 TEST / 0 MIGRATION / 5 AUTHORITY / 0 UNEXPECTED

POSTGRESQL_TESTCONTAINERS:
PASS / POSTGRESQL 17.10 / 0 MANDATORY SKIPS

FULL_REGRESSION:
PASS / 19 OF 19 / 1243 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED

QUALITY:
PASS / 19 OF 19 / CHECKSTYLE 0 / SPOTLESS PASS

REMOTE_PUBLICATION:
PENDING MILESTONE REVIEW

PRODUCTION_CAPACITY:
NOT_PROVEN

next action:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-MILESTONE-REVIEW
~~~

本结果不改变第 2 节 owner attestation 的证据属性，也不授权 migration、legacy
retirement/backfill、server deployment、push、publication 或 B5。

## 1. 决策状态

~~~text
task:
DH-STAGE-QDR-9-B4-OWNER-ATTESTED-M1-REBASELINE-AND-MINIMAL-IMPLEMENTATION-DISCOVERY

classification:
DOCUMENTATION / OWNER_OPERATIONAL_ATTESTATION / M1_REBASELINE / COMPLETE_DISCOVERY

repository:
E:/Project/decision-hub

branch:
dev

starting HEAD / origin/dev:
229910e31b9b5cba5fe944e4f3a497df73843d20
229910e31b9b5cba5fe944e4f3a497df73843d20

starting ahead / behind:
0 / 0

current technical tree:
B3 SAFE BASELINE

Stage-QDR-9 B4:
IMPLEMENTATION REVERTED / REVIEW BLOCKED

technical P1:
OPEN / persistent rate-idempotency identity and QDR7 rate-audit must bind verified environment

technical P2:
OPEN / replay nonce namespace must bind verified environment

owner operational attestation:
ACCEPTED / FIRST_PARTY_OPERATIONAL_ATTESTATION

ACTUAL_LEGACY_DATA:
OWNER_CONFIRMED ABSENT

MINIMAL_REMEDIATION_PATH:
M1

LEGACY RETIREMENT SUBSYSTEM:
NOT REQUIRED

HISTORICAL BACKFILL:
NOT REQUIRED

ZERO-DOWNTIME MIGRATION:
NOT REQUIRED

V18:
NOT REQUIRED

REGISTRY REQUIRED FOR P1/P2:
NO

RETENTION REQUIRED FOR P1/P2:
NO

MIGRATION_REQUIRED:
NO

COMPLETE_DISCOVERY:
PASS

MINIMAL_IMPLEMENTATION_SCOPE:
FROZEN

local documentation commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

push / tag:
NO / NONE
~~~

本文件继续作为 Stage-QDR-9 B4 的唯一 canonical remediation plan。此前 M4
repository-only evidence audit 保留在本文件第 6 节及 Git 历史中；M4 的证据规则没有被删除或
描述为错误，只是其“缺少 owner/runtime evidence”的 blocker 已由 project owner 的 first-party
operational attestation 解除。

## 2. Owner operational attestation

### 2.1 Attestation

标记：

~~~text
FIRST_PARTY_OPERATIONAL_ATTESTATION
~~~

The DH project owner confirms that, as of 2026-07-30:

- DH has never been deployed to an actual server or runtime environment.
- DH has processed no real external traffic.
- No operational DH database or real persistent DH data exists.
- Existing database rows are limited to local development and automated tests.
- No legacy DH data must be retained.
- No zero-downtime migration is required for the current remediation.
- DEV/TEST databases may be rebuilt.
- The available 2-core/2-GB server is dedicated to the ongoing NQ
  168-hour acceptance and must not host DH.

### 2.2 证据属性

该声明是 project owner 对 DH operational reality 的第一方陈述，不能描述为：

- GitHub-verified deployment evidence；
- DBA inventory；
- production database evidence；
- repository 自动推导出的 deployment/data 结论。

本轮没有扩充 owner 声明，也没有删除任何受支持句子：

~~~text
unsupported assertions removed:
NONE
~~~

## 3. M1 rebaseline

Owner attestation 直接回答了原 M4 blocker 中 repository 无法证明的问题，因此当前分类改为：

~~~text
ACTUAL_DH_DEPLOYMENT:
ABSENT

REAL_EXTERNAL_TRAFFIC:
ABSENT

OPERATIONAL_DH_DATABASE:
ABSENT

REAL_PERSISTENT_DH_DATA:
ABSENT

RATE / IDEMPOTENCY / REPLAY / AUDIT LEGACY DATA:
OWNER_CONFIRMED ABSENT

DEV_TEST_ONLY_ROWS:
REBUILDABLE

DATA_RETENTION REQUIREMENT:
NONE

ZERO_DOWNTIME REQUIREMENT:
NONE

SELECTED MINIMAL REMEDIATION PATH:
M1
~~~

M1 只允许 clean-cutover 的最小 P1/P2 修复。它不授权生产部署、真实流量、NQ server 使用或
任何 legacy subsystem。

## 4. Primary delivery goal

只关闭以下已验证 finding：

1. P1：
   - `FeedbackEnvironment` / `FeedbackExecutionScope` 成为唯一 verified environment contract；
   - HMAC canonical material、bearer `AuthContext` 与 tenant/source/environment authorization
     绑定同一个 scope；
   - persistent rate identity 和 persistent idempotency/recovery identity 使用该 scope；
   - `QDR7_RATE_LIMIT_ADMISSION` 的 `event_json` 结构化记录该 verified environment。
2. P2：
   - replay/nonce key namespace 绑定同一个 verified environment。
3. 与上述修复直接相关：
   - request fingerprint 绑定 environment；
   - DEV/TEST same-environment duplicate 与 cross-environment isolation；
   - 缺失、未验证或不一致 environment 全部 fail-closed。

明确排除：

- legacy tombstone retirement；
- online dual-read/write、legacy blocking 与 historical backfill；
- V16/V17/V18 或任何新 migration；
- reference-liveness registry；
- retention、scheduler、cleanup subsystem；
- real NQ、real Provider、real HTTP；
- Agent/LangGraph、Paper、LIVE；
- server deployment；
- API endpoint 扩张、automatic learning 或 B5。

## 5. Complete implementation discovery result

详细代码证据和 exact-file allowlist 位于：

`docs/current/DH_STAGE_QDR_9_B4_MINIMAL_P1_P2_IMPLEMENTATION_DISCOVERY.md`

### 5.1 Security/root path

- `DecisionDryRunController` 当前只从 `AuthenticatedRequest.requireTenantId` 取得 tenant，
  `AuthContext` 没有 environment。
- `DecisionDryRunRequest` 没有 environment。
- `NqDryRunAuthRequest` 与 `HmacNqDryRunAuthenticator.signatureMaterial` 没有 environment。
- 当前 replay key 为 tenant/source/path/nonce/requestId，未包含 environment。
- `FeedbackExecutionScope` 当前不存在。
- M1 implementation 必须在 HMAC signature、tenant/source allowlist 与 bearer environment
  一致性全部通过后创建 verified scope；任何更早创建的 body/config scope 都不是可信 authority。

### 5.2 Persistent identity and recovery

- V12 的 rate primary key 已包含
  `(environment, endpoint, source, tenant_id, window_start)`。
- V12 的 idempotency unique identity 已包含
  `(environment, endpoint, source, tenant_id, request_id)`。
- JDBC rate/idempotency repositories 已按完整 identity 查询，无需 schema 变更。
- 当前 `PersistentDecisionDryRunRateLimiter` 和
  `PersistentGuardedDecisionDryRunService` 都从
  `DecisionDryRunGuardProperties.environment()` 构造 identity；该配置不是 request verified
  authority。
- 当前 request fingerprint 包含 tenant/source/schema/context，但不含 environment。
- M1 implementation 必须使用 verified scope 的 environment 构造 rate、idempotency 与
  recovery identity，并让 guard 配置仅作为一致性 safety gate；scope/config 不一致时 fail-closed。

### 5.3 Replay

- `NonceReplayGuard` 是原子 `markIfAbsent(replayKey, expiresAt)` port。
- `JdbcNonceReplayGuard` 与 V4 表把 replay key 作为 opaque `varchar(512)` primary key；
  environment 可由 authenticator 编入 key，无需拆列或 migration。
- same environment 的相同 nonce/requestId 必须命中同一 key 并拒绝；
  DEV 与 TEST 必须形成不同 key，且两边各自保留 duplicate protection。
- 现有 TTL 与惰性 cleanup 不需要为 P2 修改；本轮不引入 scheduler 或新 cleanup subsystem。

### 5.4 Audit and transaction

- V5 `dh_decision_audit_event.event_json` 是结构化 `jsonb`，现有
  `AuditEventRecord` / `JdbcDecisionAuditRepository` 已能保存结构化 map。
- P1 只要求 QDR7 rate admission 记录 verified environment；在其 `event_json` 增加 canonical
  `environment` 字段即可，不需要新列。
- rate admission 与 QDR7 rate audit 当前已位于同一个
  `GuardTransactionBoundary.required` transaction；implementation 必须保持 admission/audit
  原子 rollback。
- generic audit rows、read model 与其他 constructor 不需要修改。

## 6. Historical repository-only evidence audit

### 6.1 保留结论

~~~text
Historical repository-only evidence audit:
M4 / insufficient without owner or runtime evidence

Superseded by:
owner operational attestation
~~~

### 6.2 Repository-only evidence

| 事实源 | 历史审查结论 | 当前解释 |
|---|---|---|
| Git history | `549ed5a` published，之后由 `df921f2` ordinary revert | 证明发布/回退，不证明 deployment |
| GitHub Actions | published implementation CI success | 只证明 build/test |
| GitHub Deployments / Environments | repository 渠道未发现记录 | 单独不足以证明从未部署 |
| `.github/workflows/ci.yml` | build/test/quality，无 deploy job | repository 内没有 deployment workflow |
| Compose / ops docs | local Postgres + Redis | local development evidence |
| Flyway V4/V5/V12/V15 | schema capability 存在 | 不证明 operational rows |
| production classes | persistent write path 存在 | 不证明 path 被真实调用 |
| Testcontainers fixtures | 测试会写相关 rows | test-only evidence |

原 M4 的方法论保持有效：repository-only evidence 不能单独断言真实 deployment/data
状态。本轮改变来自明确的 `FIRST_PARTY_OPERATIONAL_ATTESTATION`，不是对旧审查的否定。

## 7. Migration decision

~~~text
Can environment isolation be implemented using existing columns/keys?
YES

Is a single forward migration required for new structured audit environment?
NO

Can DEV/TEST databases be rebuilt instead of legacy backfill?
YES

Is any legacy retirement migration required?
NO
~~~

依据：

- rate/idempotency 的现有 V12 identity 已原生包含 environment；
- replay 的 V4 opaque primary key 可包含 environment-bound canonical key；
- QDR7 rate audit 的 V5 `event_json` 已是结构化 JSONB；
- owner 确认不存在需保留的 legacy DH data；
- P1/P2 不需要 dedicated audit environment column、backfill、registry 或 retention。

因此：

~~~text
MINIMAL_MIGRATION_ALLOWLIST:
EMPTY

V16 / V17 / V18:
NOT CREATED / NOT CREATED / NOT CREATED

ALLOW_V16_IMPLEMENTATION:
NO
~~~

## 8. Exact implementation scope

以下四个列表是完整 discovery 后的单一 active allowlist。历史 42/46/47/48/54/60/66
计数不继续累计，也不是 active implementation gate。

### 8.1 MINIMAL_PRODUCTION_WRITE_ALLOWLIST

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

### 8.2 MINIMAL_TEST_WRITE_ALLOWLIST

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

### 8.3 MINIMAL_MIGRATION_ALLOWLIST

~~~text
EMPTY
~~~

### 8.4 MINIMAL_CURRENT_AUTHORITY_ALLOWLIST

~~~text
docs/current/STATUS.md
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md
docs/current/DH_STAGE_QDR_9_B4_MINIMAL_P1_P2_IMPLEMENTATION_DISCOVERY.md
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
docs/current/WORKLOG.md
~~~

本 discovery task 只修改上述 authority 文件中的四个既有文件并创建一个 discovery 文件；后续
implementation 才可使用 production/test allowlist。

## 9. Implementation order

1. 建立 canonical `FeedbackEnvironment` / `FeedbackExecutionScope`，增加 `dh-security -> dh-domain`
   的唯一必要模块依赖。
2. 让 bearer `AuthContext`、body environment、HMAC canonical material 和 tenant/source/environment
   allowlist 在 authenticator root 汇合；验证通过后才创建 scope。
3. 将 scope 传给 rate limiter 与 `DecisionDryRunCommand`；缺失、mismatch、unsupported environment
   全部 fail-closed。
4. 用 scope environment 构造 rate、idempotency、recovery identity，并加入 request fingerprint。
5. 用 scope environment 构造 replay key；验证 same-environment replay rejection 与 DEV/TEST isolation。
6. 在既有 QDR7 rate audit `event_json` 写入 canonical environment，保持 admission/audit 同事务。
7. 运行 targeted、PostgreSQL/Testcontainers、capacity、architecture、module/full regression 与 quality；
   在 milestone review 前不得 publish。

## 10. Stop condition

Implementation 发现 allowlist 外必需文件时：

~~~text
STOP IMPLEMENTATION
RETURN TO COMPLETE DISCOVERY
~~~

不得创建 scope erratum，不得在实施中追加文件，也不得恢复历史累计 scope 计数。

## 11. Validation and readiness

~~~text
full regression:
NOT RERUN / DOCUMENTATION-ONLY DISCOVERY

quality:
PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS

Java / test / POM / migration diff:
MUST REMAIN 0

V16 / V17 / V18 files:
MUST REMAIN 0

OWNER_ATTESTATION:
ACCEPTED

ACTUAL_DH_DEPLOYMENT:
ABSENT

REAL_LEGACY_DATA:
ABSENT

MINIMAL_REMEDIATION_PATH:
M1

COMPLETE_DISCOVERY:
PASS

MINIMAL_IMPLEMENTATION_SCOPE:
FROZEN

MIGRATION_REQUIRED:
NO

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

## 12. Next concrete action

~~~text
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-IMPLEMENTATION
~~~

该任务只能按第 8 节 exact allowlist 和第 9 节顺序实施；不得部署 DH，不得占用 NQ
168-hour acceptance server，不得 push 或 publication-first。
