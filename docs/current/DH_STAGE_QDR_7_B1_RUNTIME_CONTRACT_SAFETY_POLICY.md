# DH Stage-QDR-7 B1 Runtime Contract / Safety Policy Review

> task: `DH-STAGE-QDR-7-B1-RUNTIME-CONTRACT-SAFETY-POLICY`  
> mode: `REVIEW_ONLY`  
> endpoint: `POST /api/ai/decision-dry-runs`  
> implementation: `NOT_STARTED`  
> verdict: `FROZEN / SOURCE_FIX_REVIEW_PASS`
> capacity sequence: `FROZEN / POST_B2_MEASUREMENT_REQUIRED`

## B2 blocker fix consumption（2026-07-12）

B1冻结的database-time、lease、TTL、cleanup、retention与fail-closed要求已由B2 blocker fix消费并形成V13与真实PostgreSQL事务证据。该结论只允许milestone review retry；post-B2 capacity measurement仍未授权，本文原始historical blocker段落不覆盖当前`STATUS.md`。

## B2 schema/security review consumption（2026-07-12）

本B1合同已由`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW`消费；B2 review结论为`PASS / DESIGN_FROZEN`，下一步为独立implementation。本文后部保留的原始`BLOCKED / SOURCE_CODE_FIX_REQUIRED`与capacity blocker段落是review当时的historical checkpoint，不再覆盖本段、`STATUS.md`或B2 review文档。

```text
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: FROZEN
STAGE_QDR_7_B2_PERSISTENT_GUARDS_SCHEMA_SECURITY_REVIEW: PASS
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
ALLOW_REPOSITORY_JDBC_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```

## Capacity blocker resolution outcome（2026-07-12）

`DH-STAGE-QDR-7-B1-CAPACITY-BLOCKER-RESOLUTION` 已将容量门禁拆为 pre-B2 safety contract 与 post-B2 measured defaults。Payload `65536 bytes`、context `32768 bytes`、所有配置字段类型/单位、合法范围、绝对 hard ceiling、跨字段关系、invalid-config startup failure、bounded-only 与 no-in-memory-fallback 已冻结；deadline/concurrency/queue/rate/lease/TTL/cleanup/retention 的运行默认值全部后置到 B2 persistent guards + actual-wiring 2xx harness 之后。

Source contract 冻结为 exact case-sensitive `NQ_DRYRUN`、request 不 trim/不改写、config 仅 trim 且保留 case、signature 使用原始 wire value。`DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX` 已移除 `DecisionDryRunRuntimeProperties` 的 request source lowercase并增加fail-closed配置校验；独立review结论为 `CLOSED / PASS`。本轮仍不重跑 capacity benchmark。

## Resource capacity evidence outcome（2026-07-12）

`DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER` 已完成环境与 harness 核验，但 actual app-wired endpoint 无 2xx successful sample，Docker/Testcontainers 为 21 skipped，Tomcat queue 与 future persistent guard evidence 缺失。证据结论仍为 `RESOURCE_CAPACITY_EVIDENCE_INSUFFICIENT`，但其 gate 位置已纠正为 post-B2 capacity acceptance；不再要求在 persistent guards 实现前测量其 contention、lease、cleanup 或吞吐。

## 1. Review 结论

本 review 冻结 Stage-QDR-7 limited dry-run entry 的目标安全合同，但不确认当前实现已满足全部后续guard要求。Guard目标顺序、truth table、key domain、duplicate semantics、idempotency状态机、error taxonomy、audit/redaction、pre-B2安全上限和容量门禁顺序已形成唯一语义。Source production-code drift已由独立review关闭；最终运行默认值属于post-B2 acceptance，不是B1冻结条件。

```text
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: FROZEN
GUARD_ORDER: FROZEN / CURRENT_IMPLEMENTATION_DRIFT_RECORDED
FEATURE_KILL_TRUTH_TABLE: FROZEN / IMPLEMENTATION_MISSING
KEY_DOMAIN_SEPARATION: FROZEN
DUPLICATE_REQUEST_SEMANTICS: FROZEN
IDEMPOTENCY_STATE_MACHINE: FROZEN
ERROR_TAXONOMY: FROZEN / IMPLEMENTATION_MAPPING_REQUIRED
PRE_B2_SAFETY_LIMITS: FROZEN
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED / NOT_STARTED
AUDIT_REDACTION_POLICY: FROZEN / ACCEPTANCE_EVIDENCE_PENDING
SOURCE_DRIFT_DISPOSITION: CLOSED
ALLOW_STAGE_QDR_7_B1_SOURCE_FIX_REVIEW: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
```

## 2. 现有代码与 15 步目标顺序映射

下表中的“目标顺序”是后续实现与 acceptance 的唯一权威。`Current drift` 不是允许的替代顺序，也不表示当前 endpoint 已通过 protected-entry acceptance。

| Step | Guard | 输入 | 成功条件 | 冻结错误 | 状态消耗 | Audit / fail-closed | 当前映射与差距 |
|---|---|---|---|---|---|---|---|
| 1 | Trace/correlation | `X-Trace-Id`、request | 生成或接受安全 trace reference，不作为授权依据 | `UNKNOWN_ERROR / 500` | 无 | 只记录安全 trace；异常拒绝 | `TraceIdFilter` 已有 |
| 2 | Runtime/production/dynamic kill | environment、feature、production gate、kill snapshot | 三个独立 gate 均明确允许，kill 新鲜且 inactive | `RUNTIME_DISABLED / 403`、`PRODUCTION_DISABLED / 403`、`KILL_* / 503` | 无 | 每次拒绝必须 audit；unknown/stale fail-closed | 当前 `runtimeEnabled()` 合并判断且位于 service，dynamic kill 不存在 |
| 3 | Authenticated tenant | 认证上下文 | tenant 存在且受信 | `TENANT_MISMATCH / 403` | 无 | 未认证拒绝，不创建业务状态 | `DhApiAuthenticationFilter` 已有 |
| 4 | Payload/envelope parse | raw bytes、Content-Length、JSON | 不超过 65536 bytes，JSON/envelope 可解析 | `PAYLOAD_TOO_LARGE / 413`、`POLICY_DENIED / 403` | 无 | 解析失败安全摘要；不记录 raw body | Controller 已有；非法配置仍 fallback |
| 5 | Header/body binding | tenant、requestId、traceId、timestamp、nonce | canonical header 与 body 精确绑定 | `TENANT_MISMATCH / 403`、`TIMESTAMP_INVALID / 401`、`SIGNATURE_INVALID / 401` | 无 | 拒绝 audit；不泄露 header 值 | Controller/header validator 已有 |
| 6 | Source + tenant/source allowlist | environment、tenant、source、endpoint | source 与 pair 均明确 allowlisted | `SOURCE_DENIED / 403` | 无 | missing/unknown 拒绝 | 当前分散在 authenticator 与 service，需收口 |
| 7 | HMAC + timestamp | method/path/source/tenant/requestId/traceId/timestamp/nonce/schema/body hash | signature 有效且 timestamp 合法、窗口内 | `SIGNATURE_INVALID / 401`、`TIMESTAMP_* / 401` | 无 | 验签异常拒绝；不记录签名材料 | authenticator 已有 |
| 8 | Nonce replay | environment、endpoint、source、tenant、nonce | 原子首次登记；same nonce 永远拒绝 | `NONCE_REPLAY / 409`、`NONCE_STORE_UNAVAILABLE / 503` | 成功时消费 nonce | store unknown/unavailable 拒绝；不 fallback | JDBC guard 已有，但 key 缺 environment；store failure 当前与 replay 混为 409 |
| 9 | Persistent rate admission | environment、endpoint、source、tenant | 多实例共享窗口原子取得 token | `RATE_LIMITED / 429`、`RATE_LIMIT_STORE_UNAVAILABLE / 503` | 每次合法认证后的 attempt 消耗 token | store failure 拒绝；不 fallback | 当前 JVM-local，且在 HMAC/nonce 前消费，顺序漂移 |
| 10 | Persistent idempotency admission | identity key、canonical hash | 原子创建/读取一致 state | `IDEMPOTENCY_* / 409或503` | 首次创建 `RECEIVED`；duplicate 不重复执行 | corrupt/unknown/store failure 拒绝 | 当前通用 filter 在 Controller 前消费 key-only 内存状态，顺序与语义均不符合 |
| 11 | Context/read-only policy | canonical request、decisionContext、forbiddenCapabilities | context 不超过 32768 bytes，字段完整且 no-side-effect | `MEMORY_LIMIT_EXCEEDED / 500`、`POLICY_DENIED / 403` | 无 | 拒绝 audit；不写 terminal success | service preflight 已有；非法配置仍 fallback |
| 12 | Deadline/concurrency/backpressure | admission time、deadline、slot、queue | deadline 有余量且取得有界执行槽 | `DEADLINE_EXCEEDED / 504`、`CONCURRENCY_REJECTED / 503`、`QUEUE_REJECTED / 503` | 只在成功 admission 后占 slot/queue | 不允许无界等待；释放失败仍拒绝 | 当前缺失 |
| 13 | No-side-effect execution | validated command、read-only ports | 只产生 existing dry-run snapshot/audit records，无外部 IO/交易 mutation | 已有 provider/policy taxonomy 或 `UNKNOWN_ERROR / 500` | idempotency 维持 `IN_PROGRESS` | 任一异常转安全失败 | service 已有 mock-only 主线；B4 尚未 acceptance |
| 14 | Audit/metrics/redaction | guard decisions、safe refs、result classification | audit 写成功，metrics 低基数且无敏感字段 | `AUDIT_FAILURE / 500` | 不改变业务 admission；失败阻断响应成功 | audit failure fail-closed | audit 基线已有；metrics/redaction acceptance 不完整 |
| 15 | Idempotency finalization | identity、hash、result/failure reference、version | 原子 `IN_PROGRESS -> COMPLETED/FAILED`，read-back 一致 | `IDEMPOTENCY_STORE_UNAVAILABLE / 503`、`UNKNOWN_ERROR / 500` | 写 terminal state | commit outcome unknown fail-closed | 当前缺失 |

### 2.1 当前顺序 blocker

```text
CURRENT_FEATURE_GATE_ORDER: AFTER_CONTROLLER_RATE_AND_NONCE
CURRENT_RATE_LIMIT_ORDER: BEFORE_HMAC_TIMESTAMP_NONCE
CURRENT_IDEMPOTENCY_ORDER: FILTER_BEFORE_CONTROLLER_GUARDS
CURRENT_NONCE_STORE_FAILURE_MAPPING: INDISTINGUISHABLE_FROM_NONCE_REPLAY
```

后续实现必须调整到本文件的 15 步目标顺序。不得以“已有 guard”推断顺序合规，也不得在存储失败时回退到当前 in-memory 实现。

## 3. Runtime truth table

Feature、production gate、dynamic kill 是三个独立事实，不得互相代替。

| Feature | Production gate | Environment | Kill state | Guard stores | 结果 |
|---|---|---|---|---|---|
| disabled/unknown | 任意 | 任意 | 任意 | 任意 | `RUNTIME_DISABLED / 403` |
| enabled | disabled/unknown | non-dev/test | 任意 | 任意 | `PRODUCTION_DISABLED / 403` |
| enabled | allowed | 任意 | active | 任意 | `KILL_ACTIVE / 503` |
| enabled | allowed | 任意 | unknown/read failure | 任意 | `KILL_UNKNOWN / 503` |
| enabled | allowed | 任意 | stale beyond frozen budget | 任意 | `KILL_STALE / 503` |
| enabled | allowed | 任意 | inactive/fresh | unavailable/unknown | 对应 `*_STORE_UNAVAILABLE / 503` |
| enabled | allowed或dev/test不适用 | 合法 | inactive/fresh | available | 继续下一 guard，不代表最终成功 |

当前 startup-time boolean kill snapshot 无法表达 version、freshness、unknown 或跨实例传播，因此只属于 implementation gap。

## 4. Key domain separation

所有 domain prefix 必须版本化，编码必须长度前缀或 canonical tuple，禁止用可歧义字符串拼接。

```text
nonce key:
  qdr7:nonce:v1(environment, endpoint, source, tenantId, nonce)

rate-limit key:
  qdr7:rate:v1(environment, endpoint, source, tenantId)

idempotency identity:
  qdr7:idempotency:v1(environment, endpoint, source, tenantId, requestId)

canonical request hash:
  SHA-256("QDR7-DRYRUN-CJSON-1\n" + canonicalRequestBytes)
```

`requestId` 只属于 idempotency identity，不放入 request hash。Nonce store、rate-limit store 与 idempotency store 必须是独立 schema/port/表语义；禁止复用 `dh_nq_replay_nonce` 作为 idempotency store。

## 5. Canonical request fingerprint

### 5.1 纳入字段

按以下固定顺序和 JSON canonicalization 生成 `QDR7-DRYRUN-CJSON-1`：

```text
tenantId
source
schemaVersion
dryRun
decisionContext（对象 key 递归字典序；数组顺序保留；数字/字符串按 JSON 类型保持）
forbiddenCapabilities（按去重后的字典序集合编码）
```

这些字段决定业务请求内容或安全边界，任何差异都必须产生不同 hash。

### 5.2 排除字段

```text
requestId（已在 identity key）
traceId（correlation only）
timestamp
nonce
HMAC signature
Authorization / Cookie / token / secret headers
Content-Length、连接信息、remote address
JSON whitespace、对象输入 key 顺序
任何 server-generated audit/result/received timestamp
```

排除 timestamp、nonce、signature 和 traceId 保证合法重试可使用新的认证材料而保持相同业务 hash。HMAC 仍覆盖现有签名材料；hash 排除不削弱每次重试的认证要求。

## 6. Nonce 与 idempotency 交互

```text
same nonce: always reject before idempotency lookup
same requestId retry: 必须使用新 nonce、新 timestamp、新 signature
nonce accepted: 只证明本次传输认证首次出现，不授予执行权
idempotency duplicate: 不回滚或复用已消费 nonce
nonce store unknown/unavailable: reject，禁止读取 idempotency result 绕过认证
```

Rate-limit token 在 nonce 成功后、idempotency lookup 前消费，因此合法 duplicate retry 计入限流；非法签名、过期 timestamp、nonce replay 不消耗 persistent rate/idempotency 状态。

## 7. Duplicate request semantics

选择并冻结 `IN_PROGRESS -> reject`，不引入新的异步查询 API。

| 条件 | 结果 | 是否执行 |
|---|---|---|
| same nonce | `NONCE_REPLAY / 409` | 否 |
| same requestId + same hash + `RECEIVED` | `IDEMPOTENCY_IN_PROGRESS / 409` | 否 |
| same requestId + same hash + `IN_PROGRESS` 且 lease active | `IDEMPOTENCY_IN_PROGRESS / 409` | 否 |
| same requestId + same hash + `COMPLETED` | 通过 immutable result reference 重建并返回原有 success envelope/status | 否 |
| same requestId + same hash + `FAILED` | 返回冻结 failure taxonomy、HTTP status 与安全 failure reference | 否 |
| same requestId + same hash + `EXPIRED` 且无 active lease | `IDEMPOTENCY_EXPIRED / 409`；重试必须使用新 requestId | 否 |
| same requestId + different hash | `IDEMPOTENCY_CONFLICT / 409` | 否 |
| unknown/corrupt/illegal state | `IDEMPOTENCY_STATE_INVALID / 503` | 否 |
| expired-with-active-lease | `IDEMPOTENCY_STATE_INVALID / 503` | 否 |
| store unavailable/commit unknown | `IDEMPOTENCY_STORE_UNAVAILABLE / 503` | 否 |

不得因 `FAILED`、`EXPIRED`、读取超时或 storage failure 静默重新执行。业务重试使用新 `requestId`，并重新经过全部 guard。

## 8. Persistent idempotency state machine

### 8.1 状态与合法转换

```text
atomic insert -> RECEIVED
RECEIVED -> IN_PROGRESS       取得唯一执行 lease
RECEIVED -> EXPIRED           admission lease 从未取得且明确过期
IN_PROGRESS -> COMPLETED      immutable result reference 已持久化并可 read-back
IN_PROGRESS -> FAILED         frozen failure classification/reference 已持久化
IN_PROGRESS -> EXPIRED        lease 明确过期且 recovery 证明无 active owner
COMPLETED / FAILED / EXPIRED  terminal，不允许覆盖或回到 IN_PROGRESS
```

任何未列出的转换均为 `IDEMPOTENCY_STATE_INVALID / 503`。State transition 使用 compare-and-set/version 或等价单条条件更新；affected rows 不等于 1 时重新读取并按 duplicate semantics 分类，禁止盲重试执行。

### 8.2 Admission 原子性

- 唯一键为完整 idempotency identity。
- 首次原子 insert 同时保存 canonicalization version 与 request hash。
- 唯一键冲突后必须读取同一 tenant/environment/endpoint/source 下记录并 constant-time 比较 hash。
- 只有一个 caller 能原子取得 `RECEIVED -> IN_PROGRESS` lease；其他 caller 返回 canonical in-progress。

### 8.3 Result reference 与 failure reference

- Result reference 必须 tenant-bound、immutable、可 read-back，并能重建现有 response envelope；不得保存 raw response body 作为快捷 fallback。
- Failure 只保存 stable error code、HTTP status、安全 failure class 和 safe audit reference。
- Reference missing、cross-tenant、hash/version mismatch 或 read-back failure 均 fail-closed。

### 8.4 Lease、expiry、retry 与 crash recovery

- Lease 使用数据库时间与 owner token/version；不得依赖单实例 wall clock。
- Lease 时长数值尚未冻结，见 resource blocker。
- Crash 后只有 recovery worker/transaction 能在证明 lease 过期且无 active owner 后转 `EXPIRED`；request path 不抢占旧 lease。
- Same requestId 永不因 lease 过期自动重跑；重试使用新 requestId。
- Cleanup 只删除超过冻结 retention 且无 active lease 的 terminal rows；不得删除 `RECEIVED/IN_PROGRESS` active row。
- Cleanup 必须有界批处理、可重入、按 environment/tenant 安全隔离；batch/retention 数值尚未冻结。

### 8.5 Storage failure

读取、insert、transition、result write/read-back、cleanup classification 或 commit outcome unknown 时统一 fail-closed。禁止回退到 `InMemoryIdempotencyStore`，禁止把 storage failure 当作“未命中”。

## 9. Canonical error taxonomy

Error response 继续使用既有 envelope，不新增 endpoint 或字段。实现阶段新增/映射 stable code 时必须经过 security review；reason/message 只能是固定安全摘要。

| Error code | HTTP | 语义 |
|---|---:|---|
| `RUNTIME_DISABLED` | 403 | feature disabled/unknown |
| `PRODUCTION_DISABLED` | 403 | non-dev/test production gate 未明确允许 |
| `KILL_ACTIVE` | 503 | dynamic kill active |
| `KILL_UNKNOWN` | 503 | kill 读取失败或未知 |
| `KILL_STALE` | 503 | kill snapshot 超 freshness budget |
| `INVALID_CONFIGURATION` | 503 | 配置缺失、零/负值、溢出、单位不支持或关系非法 |
| `TENANT_MISMATCH` | 403 | tenant 缺失或 binding mismatch |
| `SOURCE_DENIED` | 403 | source 或 tenant/source 未允许 |
| `SIGNATURE_INVALID` | 401 | HMAC 缺失或无效 |
| `TIMESTAMP_INVALID` | 401 | 格式/UTC-Z 非法 |
| `TIMESTAMP_OUT_OF_WINDOW` | 401 | 超出认证窗口 |
| `NONCE_REPLAY` | 409 | same nonce 已存在 |
| `NONCE_STORE_UNAVAILABLE` | 503 | nonce store 不可判定 |
| `PAYLOAD_TOO_LARGE` | 413 | raw payload 超过 65536 bytes |
| `MEMORY_LIMIT_EXCEEDED` | 500 | context 超过 32768 bytes；保持当前 HTTP mapping，API/security review 前不改 |
| `POLICY_DENIED` | 403 | envelope/read-only/forbidden material policy 拒绝 |
| `IDEMPOTENCY_CONFLICT` | 409 | same identity 不同 hash |
| `IDEMPOTENCY_IN_PROGRESS` | 409 | `RECEIVED/IN_PROGRESS` duplicate |
| `IDEMPOTENCY_EXPIRED` | 409 | terminal expired，须新 requestId |
| `IDEMPOTENCY_STATE_INVALID` | 503 | unknown/corrupt/illegal/active-lease conflict |
| `IDEMPOTENCY_STORE_UNAVAILABLE` | 503 | store/commit/reference 不可判定 |
| `RATE_LIMITED` | 429 | persistent quota exhausted |
| `RATE_LIMIT_STORE_UNAVAILABLE` | 503 | rate store 不可判定 |
| `DEADLINE_EXCEEDED` | 504 | end-to-end deadline 耗尽 |
| `CONCURRENCY_REJECTED` | 503 | 无可用 bounded execution slot |
| `QUEUE_REJECTED` | 503 | queue full 或等待将超过 deadline |
| `AUDIT_FAILURE` | 500 | success/failure audit 无法安全写入 |
| `PROVIDER_DISABLED` | 503 | 既有 mock gateway/provider gate disabled |
| `PROVIDER_TIMEOUT` | 504 | 既有 mock gateway timeout classification |
| `BUDGET_EXCEEDED` | 429 | 既有 provider/mock budget classification |
| `UNKNOWN_ERROR` | 500 | 未分类异常统一 fail-closed |

不得把 SQLState、表名、constraint、exception message、stack trace、URI、secret 或 raw input 写入 response。`NONCE_STORE_UNAVAILABLE` 必须与真实 replay 区分，这是当前代码的明确修复项。

## 10. Resource budgets 与 blocker

### 10.1 可冻结的兼容上限

| Budget | 冻结值 | 证据 | 合法配置 |
|---|---:|---|---|
| raw payload | 65536 bytes / 64 KiB | `application.yml` 与 authenticator 默认 | 必须为正整数 bytes；0、负数、溢出、非整数或不支持单位启动拒绝 |
| decision context | 32768 bytes / 32 KiB | `application.yml` 与 runtime properties 默认 | 必须为正整数 bytes；0、负数、溢出、非整数或不支持单位启动拒绝 |

以上是现有 endpoint 兼容上限，不证明更高容量可用。当前 `<=0` fallback 行为不符合冻结 policy，实施时必须改为 `INVALID_CONFIGURATION` fail-closed。

### 10.2 观察值但不可作为 B2/B3 容量合同

```text
current JVM-local rate window: 1 second
current JVM-local quota: 20 requests per source+tenant+route
current JVM-local max keys: 10000
generic IdempotencyFilter TTL: 10 minutes
```

这些数值属于共享 JVM-local/通用实现，没有 multi-instance endpoint-specific load、latency、contention、storage growth 或 recovery evidence，不能直接冻结为 persistent Stage-QDR-7 budget。

### 10.3 缺失的 capacity evidence

```text
end-to-end deadline: BLOCKED
maximum concurrency: BLOCKED
queue capacity / max wait: BLOCKED
persistent rate-limit window/quota: BLOCKED
idempotency RECEIVED/IN_PROGRESS lease: BLOCKED
idempotency terminal TTL/retention: BLOCKED
cleanup batch size/frequency: BLOCKED
dynamic kill freshness/propagation budget: BLOCKED
```

所需 evidence 至少包括 representative request payload/context 分布、mock-only endpoint latency percentiles、目标实例数、数据库 admission contention、可接受 reject rate、idempotency growth/day、crash/recovery time、cleanup throughput 与 kill propagation SLO。未提供这些证据前禁止凭空填数。

```text
B1_RESOURCE_CAPACITY_EVIDENCE_BLOCKED
RESOURCE_BUDGETS: BLOCKED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO
```

## 11. Audit / redaction policy

### 11.1 Allowlist-only audit fields

```text
environment
endpoint / HTTP method
tenantId / source
requestId / traceId
guardStep / guardDecision
stable errorCode / HTTP status
canonicalizationVersion
safe requestHash reference（截断或独立 safe ref，不记录 canonical bytes）
idempotency safe reference / state
rate-limit safe bucket reference / allowed-or-rejected / coarse retry class
deadline outcome / concurrency outcome / queue outcome
kill version / freshness class / active-or-inactive
redacted failure class
auditRef / resultRef（tenant-bound safe reference）
event timestamp
```

字段值必须有长度、字符集和低基数约束；tenant/request/result reference 可审计但不得作为无界 metrics label。

### 11.2 Redaction denylist

```text
raw body / canonical request bytes
signature / HMAC material
nonce
Authorization header
Cookie / token / secret
credential / API key / private key
raw prompt / rendered prompt
raw provider response
database URL/user/password
SQL / bind values / SQLState / constraint detail
stack trace / raw exception message
```

Audit 写失败必须返回 `AUDIT_FAILURE` 或安全 `UNKNOWN_ERROR` 并 fail-closed；不得因 audit store 不可用继续返回 success。Logging 与 metrics 使用同一 denylist。

## 12. Required implementation tests

1. 15 步 guard 顺序与每步状态消耗顺序。
2. feature/production/dynamic kill 独立 truth table。
3. kill active/unknown/stale/read failure fail-closed。
4. same nonce 始终先拒绝，且不读取 idempotency result 绕过认证。
5. same requestId/same hash 的 `RECEIVED/IN_PROGRESS/COMPLETED/FAILED/EXPIRED`。
6. same requestId/different hash conflict。
7. concurrent atomic insert 与唯一 `RECEIVED -> IN_PROGRESS` winner。
8. illegal transition、affected rows 0/>1、commit outcome unknown。
9. nonce/rate/idempotency domain separation 与 environment/tenant isolation。
10. payload/context 非法配置启动拒绝，不 fallback。
11. deadline/concurrency/queue 边界与资源释放。
12. nonce/rate/idempotency store failure 无 in-memory fallback。
13. audit failure fail-closed。
14. response/log/audit/metrics denylist 脱敏与低基数 label。
15. no order/risk/ledger/account mutation、no Paper/LIVE、no external HTTP/Provider/NQ architecture guards。
16. PostgreSQL/Testcontainers 多实例、lease、cleanup、rollback 与 crash recovery。

## 13. Implementation implications

- B1 resource capacity 不再作为 B2 schema/security review 的循环前置；当前须先关闭 source normalization production-code blocker。
- 后续实现必须重新排序当前 feature/rate/idempotency guards；不得把现有顺序视作 frozen contract。
- Persistent rate/idempotency 需要独立 schema/port/JDBC review；不得复用 nonce 表。
- Error taxonomy 实现需保持既有 response envelope；如必须改 wire shape、Controller 或 OpenAPI，停止并进入独立 API/security review。
- Dynamic kill、deadline、concurrency、queue 仍为未实现；本 review 不授权代码变更。

## 14. Readiness decision

```text
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: FROZEN
CONTRACT_VERDICT: SAFETY_AND_SEQUENCE_FROZEN / SOURCE_FIX_REVIEW_PASS
GUARD_ORDER: FROZEN
FEATURE_KILL_TRUTH_TABLE: FROZEN
KEY_DOMAIN_SEPARATION: FROZEN
DUPLICATE_REQUEST_SEMANTICS: FROZEN
IDEMPOTENCY_STATE_MACHINE: FROZEN
ERROR_TAXONOMY: FROZEN
PRE_B2_SAFETY_LIMITS: FROZEN
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED / NOT_STARTED
AUDIT_REDACTION_POLICY: FROZEN
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO / REVIEW_TASK_BOUNDARY
ALLOW_REPOSITORY_JDBC_IMPLEMENTATION_NOW: NO / REVIEW_TASK_BOUNDARY
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```
