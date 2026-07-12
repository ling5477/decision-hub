# DH Stage-QDR-7 Implementation Work Order

> task: `DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER`  
> mode: `WORK_ORDER_ONLY`  
> stage: `Stage-QDR-7 / IMPLEMENTING / B2_IMPLEMENTED`
> mainline: `Limited Dry Run Runtime Readiness`  
> endpoint: `POST /api/ai/decision-dry-runs`（既有，不新增 endpoint）

## B2 schema errata review outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-REVIEW`已`PASS / TRANSACTIONAL_CALLBACK_SELECTED`。现有`beforeMigrate`因不与V13共用事务而拒绝；唯一允许路线是`beforeEachMigrate`，使临时DDL、bounded trim和V13共同rollback。V1–V14继续immutable，B2仍`BLOCKED`。

```text
ALLOW_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_3_NOW: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION
```

## B2 blocker-fix retry outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY`已`DONE / REVIEW_PENDING`。pre-V13 compatibility callback、V14 schema alignment、clock isolation、actual JDBC result reference、production Spring completion rollback、idempotency real commit-unknown与concurrent cleanup均已有PostgreSQL 17.10/Testcontainers证据。历史milestone review保持`BLOCKED`，不得把本结果写成B2 acceptance。

## B2 milestone review retry-2 outcome（2026-07-12）

独立review为`BLOCKED / P1_FIX_REQUIRED`。callback scope/lock、V14 no-truncation/retry、terminal DB-clock、actual service completion与cleanup CAS-miss证据不足，且三份入口factsources仍有旧current task。capacity acceptance与B3继续禁止。

```text
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_2: YES / NEXT_TASK_ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2
```

## B2 milestone review retry outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY`为`BLOCKED / P1_FIX_RETRY_REQUIRED`。EXPIRED tombstone、DB clock、cleanup与rollback方向已确认，但V12→V13合法历史行兼容、冻结`result_type`类型、idempotency真实JDBC commit-unknown、production Spring同DataSource transaction wiring、JVM clock offset、actual JDBC result mapping和idempotency concurrent cleanup证据仍未关闭。

```text
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY
```

## B2 blocker fix outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX`已`DONE / REVIEW_RETRY_READY`。V13、EXPIRED tombstone、database clock、bounded cleanup、lease owner/recovery、tenant-bound result和actual JDBC transaction/commit-unknown evidence均已验证；V12与外部合同未修改。Previous milestone review保持`BLOCKED`历史状态。

```text
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY: YES / NEXT_TASK_ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY
```

## B2 milestone review outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW`结论为`BLOCKED`。尽管PostgreSQL 17.10、owning 875 tests、full 1045 tests与quality全部通过，review发现terminal expiry不可达、cleanup cutoff未由DB time封闭、V12与冻结schema字段漂移、JVM/DB clock混用和关键actual-JDBC事务测试证据不足。

```text
B2_IMPLEMENTATION_STATUS: BLOCKED / FIX_REQUIRED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX
```

Blocker fix必须forward-only；不得修改已提交V12。若要改变冻结schema要求，必须先单独review schema/security errata。

## B2 implementation outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION`已`DONE / LOCAL_VALIDATED`。V12、PostgreSQL fixed-window、persistent idempotency state/CAS/lease/result reference、tenant-first ports/JDBC、required transaction orchestration、bounded cleanup和production no-fallback wiring已实现；PostgreSQL 17.10/Testcontainers、owning-module 875 tests、full Maven 1045 tests与quality均通过，0 skipped。

```text
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW
```

该结果不授权capacity benchmark、最终production defaults、B3、API/OpenAPI、external HTTP/provider/NQ/Agent/LangGraph或LIVE。Milestone review必须先复核migration compatibility、transaction concurrency、error taxonomy、cleanup、test evidence和boundary diff。

## B1 review outcome（2026-07-12）

`DH-STAGE-QDR-7-B1-CAPACITY-BLOCKER-RESOLUTION` 已冻结 source 合同、pre-B2 安全上限与 post-B2 容量验收顺序。B2 schema/security review 不依赖最终吞吐默认值；persistent guards 实现后才运行 actual-wiring 2xx harness 并冻结 measured defaults。`044afba`的source lowercase production drift已由独立review关闭，B1 runtime contract为`FROZEN`；下一步为`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW`。

原 resource capacity evidence 确认 actual app-wired endpoint 无有效 2xx benchmark 样本、Docker/Testcontainers unavailable、server queue 与 future persistent guard 容量不可证。该证据保持有效，但已后置为 B2 capacity acceptance 输入；当前禁止在 persistent guards 实现前重跑同一 benchmark。

## B2 schema/security review outcome（2026-07-12）

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW`已`PASS / DESIGN_FROZEN`：选择PostgreSQL fixed-window counter，冻结V12候选schema、exact key、idempotency state/lease/result reference、atomic/CAS transaction、store failure、cleanup/retention、ports/JDBC与PostgreSQL test matrix。该PASS只开放独立`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION`；本review未创建migration、ports、Repository/JDBC或tests。

```text
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
ALLOW_REPOSITORY_JDBC_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```

## 1. 目标与停止规则

本工作单冻结 Stage-QDR-7 的实施批次、依赖、review 门槛、测试矩阵和回滚策略。本轮不修改生产代码、测试、migration、API、Controller、Repository/JDBC 或 runtime wiring，不启动 Stage-QDR-7 implementation。

```text
STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_7_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_7_B1_CONTRACT_FREEZE: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

任一批次发现必须新增或修改 endpoint、request/response envelope、Controller 或 OpenAPI 时，立即停止，进入独立 API/security review；不得在普通实现批次中顺带扩展。

## 2. 当前代码现实

### 2.1 已有基线

- 既有 endpoint 为 `POST /api/ai/decision-dry-runs`，返回 read-only success/error envelope。
- 入站已有认证上下文、canonical header/body binding、tenant/source allowlist、HMAC、timestamp、nonce replay、payload cap 和 context memory cap。
- nonce 在生产配置下使用 PostgreSQL/JDBC 原子登记，存储故障 fail-closed。
- endpoint 默认关闭，非 dev/test 另有 `production-enabled` 独立闸门；prod profile 当前强制 `enabled=false`、`production-enabled=false`、`kill-switch-enabled=true`。
- dry-run service 对 audit 写失败执行 fail-closed；当前链路不创建 NQ client，不发 outbound HTTP，不接真实 Provider、Agent、LangGraph 或 LIVE。
- 仓库已有 Flyway、Spring JDBC、PostgreSQL 17 Testcontainers 与事务型 persistence 测试基础。

### 2.2 已知缺口

- `InMemoryRateLimiter` 为 JVM-local 固定窗口，不提供多实例一致性。
- 通用 `IdempotencyFilter` 仅保存 `tenant + key` 的内存 TTL 占位；没有 canonical request hash、状态机或 result reference，重复请求只返回通用 conflict。
- kill switch 是启动时 properties snapshot，不提供动态跨实例传播。
- 没有 end-to-end deadline、bounded concurrency 或 queue/backpressure policy。
- `memory-cap-bytes <= 0` 与 `max-payload-bytes <= 0` 当前回退默认值，不符合本阶段要求的 invalid configuration fail-closed。
- 现有 redaction/audit guard 有实现基础，但缺少 protected-entry 最终验收证据。

## 3. 冻结的 guard 执行顺序

实现与验收必须证明以下逻辑顺序；具体 filter/service 类的放置由 B1 设计冻结，不能通过改变 API contract 绕行：

```text
1. trace/correlation 建立（不信任 caller 值作为授权依据）
2. endpoint feature flag + production gate + dynamic kill 快照
3. 认证上下文与 tenant 获取
4. raw payload cap + JSON/envelope 基础校验
5. canonical header/body binding
6. source + tenant/source allowlist
7. HMAC + timestamp
8. nonce replay 原子登记（same nonce always reject）
9. persistent multi-instance rate-limit token acquisition
10. canonical request hash + persistent idempotency state acquisition
11. context cap + forbidden material +业务只读 policy
12. deadline / bounded concurrency / backpressure admission
13. no-side-effect dry-run execution
14. success/failure audit + metrics + redacted response
15. idempotency terminal state/result reference 原子落定
```

规则：认证失败、nonce replay、限流或 kill 拒绝不得被 idempotency 结果复用覆盖；idempotency 不能使旧签名、过期 timestamp 或重复 nonce 重新获得执行资格。任何 guard store 状态未知、读取失败、写入失败或顺序无法证明时 fail-closed。

## 4. B1 — Runtime Contract / Safety Policy Freeze

### 4.1 边界

B1 只冻结合同与安全策略文档，不修改 Controller、OpenAPI、生产代码、测试、migration 或 runtime wiring。

### 4.2 交付物

- 冻结既有 request/response envelope；不新增 endpoint，不增加 wire 字段。
- 冻结 canonical error taxonomy，至少区分：authentication/policy、timestamp、nonce replay、rate limited、idempotency conflict/in-progress/unknown、deadline、backpressure、invalid configuration、guard storage unavailable、audit failure、unknown failure。
- 冻结 feature flag / production gate / dynamic kill truth table。
- 冻结 payload/context hard limit，以及 deadline、concurrency、queue、rate、lease、TTL、cleanup、retention 的配置类型、单位、合法范围、绝对 hard ceiling、默认关闭策略和 failure mapping；这些 pre-B2 ceiling 不是生产默认值。最终运行默认值由 B2 implementation 后的 capacity acceptance 基于测量冻结。
- 冻结 rate-limit、nonce、requestId/idempotency key、request hash 的 domain separation。
- 冻结 duplicate semantics、guard 顺序、audit/redaction 字段 allowlist 和 metrics label cardinality。
- 给 B2 schema/security milestone review 提供状态机、唯一键、索引、retention、事务和并发不变量。

### 4.3 Identity 与 domain separation

```text
nonce replay domain:
  stage-qdr-7:nonce:v1 + endpoint + source + tenantId + nonce + requestId

rate-limit domain:
  stage-qdr-7:rate-limit:v1 + environment + endpoint + source + tenantId

idempotency identity domain:
  stage-qdr-7:idempotency:v1 + environment + endpoint + source + tenantId + requestId

canonical request hash domain:
  stage-qdr-7:request-hash:v1 + canonical request bytes
```

- `requestId` 是该 endpoint 的 canonical idempotency identity；若仍接受 `Idempotency-Key` header，B1 必须冻结它与 `requestId` 的严格相等/binding 规则，不能形成第二套未绑定 identity。
- canonical request bytes 必须排除会自然变化且不影响业务请求身份的 transport metadata，但纳入 tenant、source、schemaVersion、dryRun、decisionContext 与 forbiddenCapabilities；具体 canonicalization/version 必须在 B1 冻结并有 golden vectors。
- 禁止复用 `dh_nq_replay_nonce` 或其 port/JDBC 作为 idempotency store。nonce 是单次认证防重放，idempotency 是持久业务请求状态，两者生命周期、duplicate 语义和结果引用不同。

### 4.4 Duplicate request semantics

```text
same nonce:
  always reject

same requestId + same canonical request:
  IN_PROGRESS -> 返回冻结的 duplicate-in-progress 结果；不得二次执行
  COMPLETED   -> 返回/复用冻结的 idempotent result reference；不得二次执行
  FAILED      -> 返回冻结的失败结果；是否允许新 requestId 重试由 B1 明确，原 requestId 不隐式重跑
  EXPIRED     -> 按冻结 retention 规则拒绝或作为新 admission；必须有单一语义，不得实例间分歧

same requestId + different request hash:
  conflict / fail-closed

unknown or incomplete idempotency state:
  fail-closed
```

首次请求若已通过 nonce 认证并创建 `IN_PROGRESS`，客户端用相同 `requestId` 重试必须使用新 nonce 与新有效 timestamp/HMAC；新认证通过后才能读取既有 idempotency state。任何重复请求都不得再次执行 side effect；本 endpoint 本身必须保持 no-side-effect。

### 4.5 Feature / production / kill truth table

| runtime enabled | production gate | environment | dynamic kill | 结果 |
|---|---|---|---|---|
| false | 任意 | 任意 | 任意 | 拒绝 |
| true | false | non-dev/test | false | 拒绝 |
| true | true | non-dev/test | false | 仅表示可继续检查；仍需所有 guard 与独立生产授权 |
| true | 任意 | dev/test | false | 仅表示可继续检查；仍需所有 guard |
| 任意 | 任意 | 任意 | true/unknown/stale beyond budget | 立即拒绝 |

Dynamic kill 必须由多实例一致、可观测、带版本/更新时间的共享事实提供；读取失败、状态未知或超过 B1 冻结的传播/陈旧预算时拒绝。它不替代 runtime feature flag 和 production gate。

### 4.6 B1 review 与完成条件

B1 必须进行一次安全语义合同 review。只有 duplicate/error/hash/domain/truth-table/budget/audit 合同无歧义且未要求 API 改动，才允许单独开启 B2 schema/security milestone review；B1 完成不自动授权 B2 implementation。

## 5. B2 — Persistent Multi-instance Guards

### 5.1 边界与前置 review

B2 规划新增 PostgreSQL persistent rate limit、persistent idempotency state machine、production port/JDBC 和 migration。实施前必须完成一次统一的 schema/security milestone review，覆盖 DDL、唯一键、索引、锁/隔离、事务、tenant/environment isolation、retention、cleanup、redaction、storage failure 和 rollback。

```text
B2_SCHEMA_SECURITY_REVIEW_REQUIRED: YES
ALLOW_B2_MIGRATION_IMPLEMENTATION_BEFORE_REVIEW: NO
ALLOW_B2_PRODUCTION_PORT_JDBC_BEFORE_REVIEW: NO
```

### 5.2 Persistent rate limit

- PostgreSQL 原子 token/window acquisition，key 至少包含 environment、endpoint、source、tenant；禁止 JVM-local 作为生产 fallback。
- 并发实例必须在同一窗口上得到一致计数，达到阈值时统一拒绝。
- 明确 fixed/sliding/token bucket 算法、数据库时钟、retry-after coarse semantics、hot-key contention 和 transaction boundary。
- 配置非法、表/连接不可用、超时、锁冲突无法安全分类时 fail-closed。
- cleanup 必须 tenant/environment-safe、可重入、可批量、有界，不能在请求热路径执行无界全表扫描。

### 5.3 Persistent idempotency state machine

冻结状态：

```text
IN_PROGRESS -> COMPLETED
IN_PROGRESS -> FAILED
IN_PROGRESS -> EXPIRED（仅由冻结的 lease/retention 规则驱动）
COMPLETED / FAILED: terminal，禁止覆盖
unknown / partial / illegal transition: fail-closed
```

最小持久字段：environment、endpoint、tenantId、source、requestId、canonicalizationVersion、requestHash、state、resultReference 或 frozen failure classification、lease/expiry、created/updated/completed timestamps、version/locking metadata。禁止保存 raw body、raw prompt、raw Provider response、signature、nonce、credential 或可执行交易材料。

- admission、state transition、result reference 必须使用原子 SQL/事务与明确并发控制。
- transaction rollback 后不得留下可误判为 completed 的记录；无法判定是否提交时按 unknown fail-closed。
- result reference 必须 tenant-bound、不可变、可验证；引用缺失、跨 tenant、hash mismatch 或目标不存在时 fail-closed。
- `FAILED` 只保存冻结的安全失败分类与安全引用，不保存原始异常/请求。
- retention/expiry/cleanup 不得让 still-running 请求被第二实例重复执行。

### 5.4 B2 milestone review 与 capacity acceptance

Migration + production port/JDBC 作为一个 milestone 统一 review，不为每个小类重复创建 standalone review。Schema/security review 先审查字段类型、合法范围、absolute ceiling、唯一键、索引、锁/隔离、事务、tenant/environment isolation、retention/cleanup 安全与 storage failure；它不依赖最终吞吐调优值。当前须先关闭 source normalization production-code blocker，且 review 本身不授权 implementation。

Review 通过并另行授权后，B2 implementation 必须配置驱动、严格范围校验，并实现 persistent guards；store failure 禁止回退 in-memory。随后才建立 actual Spring wiring、canonical source/HMAC、deterministic mock gateway、loopback、isolated PostgreSQL、Docker/Testcontainers 0 skipped 和 Tomcat/Hikari/JVM/HTTP metrics 完整的 2xx harness。B2 capacity acceptance 基于该证据冻结运行默认值；未通过前不得进入 B4。

## 6. B3 — Operational Safety / Resilience

### 6.1 本阶段实施范围

- end-to-end deadline：从受信 admission 起计时，覆盖 guard store、排队、执行、audit 与 idempotency finalization；超时拒绝/失败分类必须冻结。
- bounded concurrency：实例内执行槽有界；多实例总预算若无法全局证明，按 B1 容量模型保守限制。
- bounded queue/backpressure：队列容量与最大等待有界；队列满或预计超 deadline 时立即拒绝，不阻塞无界等待。
- dynamic kill：共享状态一致性、传播/陈旧预算、版本单调性与 fail-closed。
- environment isolation：所有 persistent key/state/config truth 明确隔离 dev/test/prod。
- audit/log redaction：字段 allowlist、异常分类、request hash 安全表示；禁止 raw body、HMAC/signature、nonce、credential、prompt/Provider raw material。
- failure audit 与 metrics：低基数 reason code，关键拒绝点可追踪；audit 写失败仍 fail-closed。
- invalid configuration fail-closed：payload/context/deadline/concurrency/queue/rate/idempotency/kill 配置非正、越界、缺失或互相矛盾时启动失败或入口拒绝，禁止静默 fallback。

### 6.2 明确后置

```text
outbound HTTP retry: DEFERRED / NO_REAL_TARGET
Provider retry: DEFERRED / NO_REAL_TARGET
Provider circuit breaker: DEFERRED / NO_REAL_TARGET
```

当前没有真实 outbound dependency。不得为了形式提前实现 retry/circuit breaker，也不得引入 HTTP client、Provider SDK 或真实 Provider。

### 6.3 Review 规则

B3 为普通实现批次，不单独重复创建 review；只有安全语义变化、P0/P1、API/Controller、migration 或 production persistence 边界扩张时停止并 review。

## 7. B4 — Protected Entry Readiness Acceptance

B4 只读验收既有 endpoint，不修改 API/Controller。B2 capacity acceptance 是硬前置；deadline/concurrency/queue/rate/lease/TTL/cleanup/retention measured defaults 未冻结时禁止进入 B4。验收必须逐项提供代码、配置和测试证据：

- default-disabled；production gate 独立关闭；dynamic kill 可立即拒绝且多实例一致。
- HMAC、timestamp、same nonce always reject、tenant/source enforced。
- persistent multi-instance rate limit 与 idempotency state machine 生效；guard store failure 不回退 in-memory。
- duplicate in-progress/completed/failed/expired 与 hash conflict 语义符合 B1。
- payload/context/deadline/concurrency/queue 非法配置 fail-closed。
- endpoint no-side-effect；无 order/risk/ledger/account mutation，无 Paper/LIVE trigger。
- 无 outbound HTTP、Provider/SDK、NQ runtime、Agent/LangGraph/Python runtime。
- audit/trace/redaction 完整；日志、metrics、error response 不泄露敏感材料。
- architecture guards、Stage-QDR-6 deterministic replay 回归与 full Maven/quality 通过。

若任何验收项要求修改 request/response、OpenAPI、Controller 或 authentication contract，B4 必须 `BLOCKED / API_SECURITY_REVIEW_REQUIRED`，进入独立 review，不得原地修复后自批。

## 8. B5 — Stage Final Close

固定顺序：

```text
B1-B4 完成且各自 commit
-> Stage final close review
-> close docs commit
-> self-contained archive packet
-> archive commit
-> clean worktree
-> annotated tag
-> push commit/tag
-> post-tag docs/current cleanup
```

Archive packet 至少包含 plan、implementation work order、B1–B4 摘要、schema/security review、protected-entry acceptance、validation evidence、final close、archive close、status snapshot、boundary、risk、tag state。未完成 archive-before-tag 时禁止 tag；本工作单本身不创建 tag 或 push。

## 9. Review 策略

- B1：安全语义合同冻结，必须 review。
- B2：migration + production port/JDBC，统一 schema/security milestone review。
- B3：普通实现；仅安全语义、P0/P1 或受控边界变化时 review。
- B4：protected-entry acceptance review。
- B5：stage final close review。
- 普通小批次不得重复创建 standalone review；API/Controller 变化始终单独 review。

## 10. 测试矩阵

### 10.1 Contract / identity

- tenant/source/HMAC/timestamp/nonce 正反例。
- canonical request hash stability、canonicalization golden vectors 与 version mismatch。
- same requestId + same payload；same requestId + different payload。
- duplicate `IN_PROGRESS / COMPLETED / FAILED / EXPIRED / unknown`。
- nonce 与 idempotency domain separation：same nonce always reject；新 nonce 不触发二次执行。
- tenant/source/environment/endpoint cross-domain 不碰撞、不越权复用。

### 10.2 Persistent guards

- 两个应用实例共享 PostgreSQL 的 rate limit。
- concurrent token acquisition / threshold boundary / hot key。
- idempotency `IN_PROGRESS -> COMPLETED/FAILED/EXPIRED` 合法流转与非法流转拒绝。
- 相同 identity 并发首次请求只有一个取得执行权。
- transaction rollback、commit outcome unknown、result reference 缺失/冲突。
- cleanup/expiry/retention、running lease 与 cleanup race。
- PostgreSQL/Testcontainers 真库验证；storage unavailable/timeout/constraint error fail-closed。
- 验证 persistent guard 不回退到 in-memory；nonce 表不被 idempotency 复用。

### 10.3 Operational safety

- feature flag 默认关闭；production gate 独立关闭。
- dynamic kill 跨实例传播、stale/unknown/read failure 立即拒绝。
- end-to-end deadline 超限与 deadline 在排队/执行/audit/finalization 各阶段耗尽。
- bounded concurrency、queue full、backpressure、取消与资源释放。
- payload/context/deadline/concurrency/queue/rate/idempotency 配置非法拒绝。
- success/failure/rejection logging、audit、metrics 脱敏；高基数/敏感 label 拒绝。

### 10.4 Boundary regression

- no order/risk/ledger/account mutation；no Paper/LIVE trigger。
- no outbound HTTP/Provider/Provider SDK/NQ。
- no Agent/LangGraph/Python runtime。
- Stage-QDR-6 deterministic replay regression。
- ArchitectureTest/禁止依赖扫描。
- full Maven tests 与 `mvn -ntp -Pquality validate`。

## 11. Commit 与回滚

- B1、B2、B3、B4 独立 commit；文档、实现、测试在各批次内保持可审查，不混入无关格式化。
- 未部署 migration 可对 B2 commit 执行 `git revert <commit>`；禁止 destructive reset。
- 已应用 migration 只能 forward fix，不修改历史 migration，不依赖 down migration 擅自删表/列。
- 紧急回滚顺序：

```text
dynamic kill switch
-> runtime feature disabled
-> production gate disabled
-> stop accepting traffic
```

- Guard storage 不可用时必须拒绝请求，禁止回退到 JVM-local/in-memory。
- 回滚不能删除 still-required idempotency/nonce evidence；retention cleanup 只能按冻结策略执行。

## 12. 完成判定与下一步

本工作单通过条件：事实源与代码现实一致；B1–B5、identity/domain、duplicate semantics、review、tests、rollback 与 archive discipline 已冻结；本轮 diff 仅限允许文档；quality validation 有真实结果。

```text
STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: DONE
STAGE_QDR_7_B1_CAPACITY_BLOCKER_RESOLUTION: DONE
B1_SOURCE_NORMALIZATION_CONTRACT: PASS / CLOSED
STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: PASS / DESIGN_FROZEN
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO / REVIEW_TASK_BOUNDARY
ALLOW_REPOSITORY_JDBC_IMPLEMENTATION_NOW: NO / REVIEW_TASK_BOUNDARY
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```
