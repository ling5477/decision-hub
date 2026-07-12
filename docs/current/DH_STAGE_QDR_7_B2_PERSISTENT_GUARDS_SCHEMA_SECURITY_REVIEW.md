# DH Stage-QDR-7 B2 Persistent Guards Schema / Security Review

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW`
> mode: `REVIEW_ONLY + SCHEMA_DESIGN_REVIEW + SECURITY_BOUNDARY_REVIEW`
> endpoint: `POST /api/ai/decision-dry-runs`（既有，不修改）
> implementation: `NOT_STARTED`
> verdict: `PASS / DESIGN_FROZEN / IMPLEMENTATION_SEPARATELY_GATED`

## 1. Review scope 与事实结论

本 review 只冻结 PostgreSQL persistent rate limit、persistent idempotency、事务并发、cleanup/retention、production port/JDBC 和后续测试/容量准入，不创建 migration、Java interface、Repository、JDBC、测试或 API 变更。

```text
repository: decision-hub
branch: dev
review HEAD: e3b60e4
review latest commit: e3b60e4 docs(qdr): accept stage-qdr-7 source normalization fix
review worktree before edits: CLEAN
review staged before edits: EMPTY
origin/dev...HEAD at review start: 0 behind / 0 ahead
Stage-QDR-7 B1: FROZEN
source normalization fix: PASS / CLOSED
canonical source: NQ_DRYRUN
current rate limit: JVM-local fixed window
current idempotency: generic in-memory key-only store
persistent guards: NOT_IMPLEMENTED
current Flyway max: V11
candidate next migration: V12 / NOT_CREATED
B2 implementation before this review: NOT_ALLOWED
post-B2 capacity acceptance: REQUIRED
```

任务输入称本地分支领先`origin/dev`，但开工时`git rev-list --left-right --count origin/dev...HEAD`为`0 0`；以live Git结果为准。本轮不push。

## 2. Migration baseline review

Flyway序列为`V1__init.sql`至`V11__qdr6_snapshot_persistence_metadata_fix.sql`。命名使用`V{整数}__{snake_case_description}.sql`；现有迁移以PostgreSQL `timestamptz`、显式`CHECK`、tenant-bound unique/FK、query index和`COMMENT`为主。V4已有`dh_nq_replay_nonce`，V5已有decision audit/output，V6已有QDR Decision Core，V8/V9/V10/V11覆盖model gateway、replay/evaluation和canonical snapshot。

`dh_nq_replay_nonce`不能复用：其primary key是组合后的`replay_key`，没有B2要求的`environment + endpoint + source + tenant`独立列，也没有rate counter或idempotency state/lease/result字段。V5/V6/V8/V9/V10表是业务/audit/snapshot结构，也不应被改造成通用guard表。复用会产生跨tenant可证性、retention耦合和误清理风险。

后续migration规则冻结为：

- 只能新增`V12__qdr7_persistent_guards.sql`；实现前再次读取migration目录，若最大版本变化则改为当时max+1，禁止硬编码冲突版本。
- additive only；不修改V1-V11，不drop/rename，不重写历史数据，不默认放行。
- 新表、约束、索引、COMMENT必须能在已有V1-V11真实数据库上原位升级；不要求危险全表回填。
- 可给`dh_decision_output`增加tenant-bound exact reference所需的唯一约束，但不得改变其业务字段语义；实现前必须用真实PostgreSQL验证既有数据无冲突。
- rollback只允许应用回滚并保留新表/数据；Flyway migration本身不提供destructive down migration。需要撤销schema时另起review和新版本migration。

## 3. Selected rate-limit algorithm

选择`fixed-window counter`，不选择sliding-window或token bucket。

| 维度 | fixed-window counter | sliding-window | token bucket |
|---|---|---|---|
| PostgreSQL原子性 | 单bucket行条件upsert，语义直接 | 多行log或多bucket聚合，事务更重 | 需原子计算refill、余额和时间 |
| 多实例一致性 | 唯一key+row lock提供全局winner | 可一致，但写放大明显 | 可一致，但clock/refill公式复杂 |
| 热点竞争 | 同key单行热点，必须capacity测量 | 分散写但聚合/清理更贵 | 同key热点 |
| cleanup | 过期bucket批删 | log清理成本最高 | 通常只保留当前行 |
| 审计解释 | window/count/limit直观 | 精确但记录量大 | token余额与refill较难解释 |
| clock rollback | DB对齐window可封闭 | 事件时间处理复杂 | rollback会影响refill安全 |

该选择不是沿用JVM实现，而是以PostgreSQL一致性、fail-closed、可解释性和可测量性为依据。固定窗口边界突发和热点行竞争保留为post-B2 capacity acceptance的硬验收项。

## 4. Rate-limit identity 与候选表

唯一domain冻结为：

```text
environment + endpoint + source + tenant_id + window_start
```

- `environment`来自启动时必填、受allowlist校验的deployment identity，只允许`dev/test/staging/prod`；不从请求header、active profile集合或fallback推导。
- `endpoint`固定canonical值`/api/ai/decision-dry-runs`，不使用raw URL、query string、alias或模糊route。
- `source`必须逐字符等于`NQ_DRYRUN`，case-sensitive，不trim、不case-fold、不alias。
- `tenant_id`来自认证后的canonical tenant；必须非空、长度不超过128、`btrim(tenant_id)=tenant_id`，不lowercase、不使用default tenant。
- `window_start`由PostgreSQL UTC epoch边界计算：`to_timestamp(floor(extract(epoch from transaction_timestamp()) / window_seconds) * window_seconds)`；`window_end=window_start+window_seconds`。
- admission只使用数据库事务时钟。应用时钟只可用于trace，不参与bucket identity；JVM clock rollback不改变bucket，DB时钟倒退或无法证明时拒绝并报store/config错误。
- identity字段明文保存以支持审计与tenant-first查询；不拼接或hash整个key。request/raw body等材料不进入该表。

候选表`dh_qdr7_rate_limit_bucket`：

| Column | Type | Constraint / semantics |
|---|---|---|
| `environment` | `varchar(16)` | not null；allowlist CHECK |
| `endpoint` | `varchar(128)` | not null；固定canonical endpoint CHECK |
| `source` | `varchar(64)` | not null；固定`NQ_DRYRUN` CHECK |
| `tenant_id` | `varchar(128)` | not null；非空、无outer whitespace |
| `window_start` | `timestamptz` | not null；UTC instant identity |
| `window_end` | `timestamptz` | not null；必须大于start |
| `window_seconds` | `integer` | not null；正数hard ceiling由B1配置合同控制 |
| `limit_value` | `integer` | not null；正数；bucket内不可改变 |
| `request_count` | `bigint` | not null；`1..limit_value`；防溢出 |
| `created_at` | `timestamptz` | not null；DB transaction time |
| `updated_at` | `timestamptz` | not null；每次accepted更新 |

Primary key同时是唯一约束：`(environment, endpoint, source, tenant_id, window_start)`。CHECK必须覆盖environment、canonical endpoint/source、trimmed tenant、positive window/limit、`window_end > window_start`、`request_count between 1 and limit_value`，并验证`window_end=window_start+window_seconds*interval '1 second'`。

索引：primary key支持exact admission；cleanup使用`(window_end, environment, endpoint, source, tenant_id)`。禁止tenantless业务查询；cleanup是唯一允许的跨tenant扫描，必须只投影primary key、受batch上限和expiry predicate约束，并输出聚合审计，不返回业务数据。

## 5. Rate-limit atomic admission

`RateLimitAdmissionPort.tryAcquire(tenant-first exact key, configured window/limit)`在一个本地PostgreSQL事务内完成：

1. 从DB transaction time计算window。
2. `INSERT ... ON CONFLICT ... DO UPDATE SET request_count=request_count+1`，更新条件必须同时满足stored window/limit与配置一致且`request_count < limit_value`，并`RETURNING`安全元数据。
3. 无返回行时，在同一事务exact select：count已达limit返回`RATE_LIMITED`；配置/row不一致返回`GUARD_CONFIGURATION_INVALID`；0或>1行返回store invalid并拒绝。
4. 写入对应脱敏guard audit；audit失败回滚本事务并拒绝。

并发winner为按PostgreSQL唯一键和row lock实际提交的前`limit_value`次；第`limit_value`次包含在允许额度内，之后拒绝，即条件是pre-count `< limit_value`。禁止先select后update、禁止read-modify-write跨事务、禁止返回成功后补写counter。

Isolation使用`READ COMMITTED`即可，因为唯一键冲突和条件update在单行上串行化；不得使用全表/tenant advisory lock。只允许对SQLSTATE `40P01` deadlock或`40001` serialization failure进行最多2次重试（初次+2次，总3次），总retry budget不超过100ms且不得超过剩余end-to-end deadline；使用bounded jitter。connection loss、timeout或任何可能已commit的异常不得重试，映射`RATE_LIMIT_COMMIT_UNKNOWN`。storage不可用映射`RATE_LIMIT_STORE_UNAVAILABLE`。所有不确定结果拒绝且不fallback到in-memory。

窗口切换按DB计算的新primary key自然竞争；旧bucket不再接受新窗口请求。Cleanup只能删除`window_end < database_now - safety_grace`的bucket；不得删除当前或未来window。cleanup与active admission竞争时以row lock/expiry predicate为准，锁不到的行跳过或留待下批。

## 6. Idempotency identity 与 fingerprint

Identity冻结为：

```text
environment + endpoint + source + tenant_id + request_id
```

Fingerprint冻结为：

```text
SHA-256("QDR7-DRYRUN-CJSON-1\n" + canonicalRequestBytes)
```

`canonicalRequestBytes`是UTF-8、无BOM的RFC 8785/JCS等价canonical JSON bytes；实现必须提供仓库内确定性canonicalizer与golden vectors，不得依赖Map迭代顺序。

纳入hash：canonical `tenantId`、canonical `source=NQ_DRYRUN`、`schemaVersion`、`dryRun`、`forbiddenCapabilities`（先按exact token去重，再按Unicode code point稳定升序）、完整安全`decisionContext` canonical JSON。`decisionContext`的object key排序、number/string/array/null语义必须稳定；业务数组保持顺序，只有`forbiddenCapabilities`按上述规则集合化。

不纳入hash：`requestId`（已在identity）、`traceId`（每次重试允许新trace）、`timestamp`、`nonce`、signature、authorization/header transport metadata。不得保存canonical bytes、raw body、signature、nonce、credential；只保存64位lowercase hex SHA-256。source和schemaVersion明确纳入hash，防止同identity下合同漂移。

## 7. Idempotency候选表

候选表`dh_qdr7_idempotency_guard`：

| Column | Type | Constraint / semantics |
|---|---|---|
| `guard_id` | `uuid` | primary key；server-generated stable id |
| `environment` | `varchar(16)` | not null；allowlist CHECK |
| `endpoint` | `varchar(128)` | not null；canonical endpoint CHECK |
| `source` | `varchar(64)` | not null；`NQ_DRYRUN` CHECK |
| `tenant_id` | `varchar(128)` | not null；canonical tenant |
| `request_id` | `varchar(128)` | not null；非空、无outer whitespace |
| `request_hash` | `char(64)` | not null；lowercase SHA-256 CHECK |
| `hash_version` | `varchar(32)` | 固定`QDR7-DRYRUN-CJSON-1` |
| `state` | `varchar(16)` | `RECEIVED/IN_PROGRESS/COMPLETED/FAILED/EXPIRED` CHECK |
| `version` | `bigint` | not null default 0；非负CAS version |
| `lease_owner` | `varchar(128)` | IN_PROGRESS必填；不得含host secret |
| `lease_token` | `uuid` | IN_PROGRESS必填；不写日志/响应 |
| `lease_expires_at` | `timestamptz` | IN_PROGRESS必填且晚于updated_at |
| `result_type` | `varchar(32)` | COMPLETED固定`DH_DECISION_OUTPUT` |
| `result_id` | `varchar(128)` | COMPLETED必填；指向existing safe output |
| `result_checksum` | `char(64)` | COMPLETED必填；safe response projection hash |
| `error_code` | `varchar(128)` | FAILED必填；stable structured code |
| `created_at` | `timestamptz` | not null；DB time |
| `updated_at` | `timestamptz` | not null；DB time |
| `completed_at` | `timestamptz` | 仅COMPLETED |
| `failed_at` | `timestamptz` | 仅FAILED |
| `expires_at` | `timestamptz` | terminal duplicate语义过期点；数值后置capacity acceptance |
| `retention_until` | `timestamptz` | tombstone最早物理清理点；必须>=expires_at |

Unique constraint：`(environment, endpoint, source, tenant_id, request_id)`；另加`(tenant_id, guard_id)`供tenant-bound reference。CHECK必须冻结identity trim、hash格式、state与lease/result/error/timestamp的交叉一致性、`version>=0`、`retention_until>=expires_at`。禁止JSON/raw payload列。

Exact query index由unique constraint提供；cleanup partial indexes：terminal/expired候选`(retention_until, environment, endpoint, source, tenant_id)` where state in terminal states，以及crash recovery`(lease_expires_at, environment, endpoint, source, tenant_id)` where state=`IN_PROGRESS`。禁止`latest`、list-all、requestId-only、tenantless lookup。

## 8. Idempotency state machine 与并发

初始admission使用exact identity的原子insert，state=`RECEIVED`、version=0、hash固定。唯一冲突后只exact读取同identity；hash不等立即`IDEMPOTENCY_CONFLICT`，不得覆盖或重新占用。

合法转换：

```text
RECEIVED -> IN_PROGRESS
IN_PROGRESS -> IN_PROGRESS   (heartbeat或expired-lease recovery，CAS)
IN_PROGRESS -> COMPLETED
IN_PROGRESS -> FAILED
RECEIVED -> EXPIRED          (仅从未开始且已达到expiry policy)
IN_PROGRESS -> EXPIRED       (仅内部recovery判定不可安全继续，且无active lease)
COMPLETED -> EXPIRED         (retention policy标记，不删除结果)
FAILED -> EXPIRED            (retention policy标记)
EXPIRED -> no transition
```

所有update必须带完整identity、expected state、expected version；IN_PROGRESS还必须带lease token。成功必须影响恰好1行并`version=version+1`；0行视为并发丢失后exact reread，>1行或unknown/corrupt state为`IDEMPOTENCY_STATE_INVALID`。COMPLETED/FAILED的业务结果不可覆盖；EXPIRED是tombstone，不得被相同requestId复用。

`RECEIVED -> IN_PROGRESS`生成不可预测lease token和bounded owner id。执行可能超过lease时需要heartbeat；heartbeat只能延长同token lease且不能越过配置hard ceiling。客户端duplicate不能抢expired lease；只有内部recovery worker可在确认旧lease过期后CAS接管同一hash工作。recovery必须复用原identity/hash和持久化进度，不得创建第二业务执行。无法证明执行是否已产生side effect/result时写`FAILED`或保持fail-closed待人工恢复，不自动重跑。

lease/TTL/retention/batch的最终生产默认值不在本review编造；类型、单位、正值、bounded hard ceiling、`lease < endToEndDeadline < terminal expiry <= retention_until`等结构关系沿用B1并由post-B2 capacity acceptance选值。

## 9. Duplicate semantics

| Existing state | same hash | different hash |
|---|---|---|
| `RECEIVED` | `IDEMPOTENCY_IN_PROGRESS / 409` | `IDEMPOTENCY_CONFLICT / 409` |
| `IN_PROGRESS` | `IDEMPOTENCY_IN_PROGRESS / 409` | `IDEMPOTENCY_CONFLICT / 409` |
| `COMPLETED` | exact tenant-bound读取并返回immutable safe result reference | `IDEMPOTENCY_CONFLICT / 409` |
| `FAILED` | 返回冻结的stable error，不自动重执行 | `IDEMPOTENCY_CONFLICT / 409` |
| `EXPIRED` | `IDEMPOTENCY_EXPIRED / 409`，必须新requestId | `IDEMPOTENCY_CONFLICT / 409` |

unknown/corrupt/store failure一律fail-closed。任何duplicate仍在HMAC/timestamp/nonce之后，不能用idempotency result绕过认证；nonce replay先拒绝。合法new-nonce retry消耗rate admission，再读取idempotency。

## 10. Result reference安全模型

COMPLETED引用现有`dh_decision_output`，不新增业务结果表。Reference冻结为：`result_type=DH_DECISION_OUTPUT + tenant_id + decision_id(result_id) + result_checksum`。后续V12需增加`unique(tenant_id, decision_id)`以支持exact tenant-bound reference；JDBC只能用tenant-first exact query。`result_checksum`对返回给duplicate的immutable safe projection计算SHA-256，读取时必须重算匹配，否则`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`。

safe projection只允许stable result ID、status、action/risk/policy等既有结构化只读字段、safe checksum、created/completed timestamp、schema version和safe audit/replay refs。禁止raw prompt、raw provider response、credential、authorization header、signature、nonce、raw body、账户/订单/数量/价格/杠杆及交易/执行指令。

`dh_decision_output`当前JDBC是insert-only；B2 implementation必须保持completed target不可update/delete，且不能通过通用CRUD暴露。如实现证明现有结构无法重建同一safe response，必须停止并进入`DH-STAGE-QDR-7-B2-BOUNDARY-EXPANSION-REVIEW`；本review不授权新增结果表。

## 11. Production ports 与Repository/JDBC边界

采用四个最小ports，不采用建议中的六个碎片ports，也不合并成通用CRUD：

```text
RateLimitAdmissionPort.tryAcquire(tenantId, environment, endpoint, source, policy)
IdempotencyGuardPort.admitExact(...)
IdempotencyGuardPort.transitionExact(... expectedState/version/leaseToken ...)
IdempotencyGuardPort.findExact(tenantId, environment, endpoint, source, requestId)
GuardCleanupPort.cleanupExpired(GuardCleanupCommand boundedCommand)
```

其中`IdempotencyGuardPort`合并admission/transition/query以维持单一状态机不变量，但方法仍是能力型、不可暴露save/update/delete任意状态。所有业务方法tenant-first；禁止latest、list-all、tenantless scan、任意status update、delete active record。

Ports位于`dh-usecase/.../decision/dryrun/guard`，仅暴露domain-safe command/result；transaction orchestration归属usecase service。JDBC adapters位于`dh-infra/.../jdbc/qdr/guard`，只依赖DH datasource/JdbcTemplate，不访问NQ DB。Spring wiring位于`dh-app`；production-like profile必须显式启用persistent mode且缺bean/config时启动失败，禁止fallback到`InMemoryRateLimiter`或`InMemoryIdempotencyStore`。测试替身只能在test source或显式test configuration，不得成为production component/profile候选。

## 12. Transaction boundaries

- Rate admission+guard audit：单事务；audit失败则counter事务回滚并拒绝。
- Idempotency admission+guard audit：单事务；insert/exact conflict判定与audit同事务。
- State transition：每次条件update单事务；heartbeat/recovery同样CAS。
- Final success：existing safe `dh_decision_output` insert、success audit、result checksum/reference和`IN_PROGRESS -> COMPLETED`必须由同一usecase-owned transaction coordinator提交；禁止先返回成功后写guard。
- Final failure：stable failure audit与`IN_PROGRESS -> FAILED`同事务。若source/business失败发生在admission后，必须冻结FAILED；不能删除记录让相同requestId重跑。
- audit写失败：对应事务rollback并返回fail-closed；不得保留可对外复用的success result。
- rollback/deadlock/serialization遵守第5节bounded retry；任何commit outcome unknown不得自动retry业务执行，映射对应`*_COMMIT_UNKNOWN`。

Rate admission与idempotency admission各自是短事务，不跨整个业务执行持锁。最终COMPLETED事务只承载final safe result/audit/state切换，不持有外部IO；Stage-QDR-7继续禁止external HTTP/provider/NQ/Agent/LangGraph。

## 13. Cleanup 与 retention

- 时间统一使用PostgreSQL UTC `timestamptz`和DB transaction time。
- Active lease永不清理；`RECEIVED`或`IN_PROGRESS`只能由recovery/state transition处理，cleanup不得物理删除。
- terminal row只有达到`retention_until < database_now`后可标记/删除；COMPLETED reference在其duplicate保证期内必须可读。
- Rate bucket只有`window_end < database_now - safety_grace`才可清理；当前/future window禁止删除。
- cleanup按索引、primary-key seek分页和显式batch上限执行；禁止offset深分页、全表锁、无界DELETE。
- 允许`FOR UPDATE SKIP LOCKED`选择已过期候选，因为cleanup是best-effort且遗漏可下批处理；不得用于admission或exact业务query。
- 每batch独立短事务；失败bounded retry，commit unknown不立即重复同batch，依靠幂等expiry predicate后续重扫。
- cleanup audit只记录guard type、environment、batch id、candidate/deleted/skipped/error counts与时间范围，不记录requestId/hash/result/raw material。
- batch size、interval、TTL、lease、retention与safety grace最终值全部留给post-B2 capacity acceptance；实现前只允许有界可配置、非法配置启动失败。

## 14. Error taxonomy / HTTP / audit / retry

| Error | HTTP | Retry | Safe ref | Nonce | Idempotency mutation |
|---|---:|---|---|---|---|
| `RATE_LIMITED` | 429 | window后可用新nonce | no | consumed | none |
| `RATE_LIMIT_STORE_UNAVAILABLE` | 503 | store恢复后新nonce | no | consumed | none |
| `RATE_LIMIT_COMMIT_UNKNOWN` | 503 | 不自动retry；新请求先重新认证/查状态 | no | consumed | none |
| `IDEMPOTENCY_IN_PROGRESS` | 409 | later with new nonce | no | consumed | no transition |
| `IDEMPOTENCY_CONFLICT` | 409 | no；必须新requestId | no | consumed | none |
| `IDEMPOTENCY_STORE_UNAVAILABLE` | 503 | store恢复后新nonce | no | consumed | unknown/none |
| `IDEMPOTENCY_COMMIT_UNKNOWN` | 503 | 不自动执行；先exact reconcile | no | consumed | unknown |
| `IDEMPOTENCY_STATE_INVALID` | 503 | no automatic retry | no | consumed | none |
| `IDEMPOTENCY_RESULT_UNAVAILABLE` | 503 | repair/reconcile后 | no | consumed | terminal不变 |
| `GUARD_CONFIGURATION_INVALID` | startup failure；runtime 503 | no | no | runtime可能已consumed | none |

每项使用独立audit event type，不得把store failure伪装成`RATE_LIMITED`、nonce replay或普通conflict。`COMPLETED` duplicate校验通过时才可返回safe reference；所有失败audit均脱敏。Rate/idempotency发生在nonce之后，所以表中错误的请求均已消费nonce；启动时配置错误不接收请求。

## 15. PostgreSQL/Testcontainers implementation test matrix

后续implementation必须新增真实PostgreSQL 17/Testcontainers证据，Docker/Testcontainers skipped必须为0：

1. clean V1->V12和existing V1->V11 data upgrade；历史checksum不变。
2. exact key约束、canonical source/endpoint/environment/tenant CHECK、跨tenant/source/environment隔离。
3. N并发rate admission只有前limit个winner，边界等号、window切换和clock source。
4. deadlock/serialization bounded retry、connection loss和commit unknown fail-closed；无in-memory fallback。
5. idempotency atomic insert、same/different hash、全部state duplicate语义。
6. expected state/version/token CAS、illegal transition、0/>1 affected row、terminal immutability。
7. lease heartbeat、expired-lease internal recovery、crash before/afterresult、commit unknown reconcile。
8. final safe result+audit+COMPLETED事务rollback；audit失败不留下可复用success。
9. result exact tenant lookup、checksum mismatch/missing target返回RESULT_UNAVAILABLE。
10. cleanup batch/seek/`SKIP LOCKED`、active lease不清理、concurrent admission/transition不误删、commit unknown可重扫。
11. raw material denylist、no request bytes/signature/nonce/credential persistence。
12. architecture guards：ports/usecase/infra/app分层，production profile无test double，no API/Controller/OpenAPI/external IO/NQ/trading mutation。

## 16. Post-B2 capacity acceptance gate

Schema/事务实现通过不等于runtime readiness。Implementation后必须另起capacity acceptance，使用actual app-wired 2xx mock-only harness、isolated PostgreSQL、至少两实例/等价多实例并发、Docker/Testcontainers 0 skipped，测量rate热点竞争、lock wait/deadlock/serialization、Hikari active/pending/acquire、idempotency lease/heartbeat/recovery、cleanup吞吐与storage growth、window boundary burst、p50/p95/p99和first saturation。

只有该证据才能选择window/quota、lease、TTL、retention、cleanup batch/interval、deadline/concurrency/queue最终默认值。未通过时：`ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO`且protected-entry acceptance继续BLOCKED。

## 17. Security invariants 与architecture guards

```text
all persistent keys include environment + endpoint + source + tenant
no tenantless lookup
no latest/default/fallback query
no in-memory fallback on PostgreSQL failure
no raw material persistence
no state overwrite without expected state/version
no completed-result mutation
no active-lease cleanup
no external IO
no NQ mutation
no trading authorization
```

Implementation architecture tests必须禁止guard ports依赖`dh-api`/`dh-infra`，禁止JDBC adapter出现在API层，禁止production wiring注入in-memory guard，禁止guard包依赖HTTP client/provider/NQ/Agent/LangGraph/trading包。

## 18. Readiness decision

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_SCHEMA_SECURITY_REVIEW: PASS
RATE_LIMIT_SCHEMA: FROZEN
IDEMPOTENCY_SCHEMA: FROZEN
KEY_DOMAIN_ISOLATION: PASS
STATE_MACHINE: FROZEN
TRANSACTION_CONCURRENCY: FROZEN
STORE_FAILURE_SEMANTICS: FROZEN
CLEANUP_RETENTION_STRUCTURE: FROZEN
PORT_JDBC_BOUNDARY: FROZEN
MIGRATION_STRATEGY: FROZEN
POST_B2_CAPACITY_GATE: REQUIRED

ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
ALLOW_REPOSITORY_JDBC_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_OPENAPI_CHANGE_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO

next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```

`YES / NEXT_TASK_ONLY`只表示本review已满足另起implementation任务的设计前置，不授权在本轮创建V12、ports、Repository/JDBC、tests或wiring。若implementation发现必须修改API/Controller/OpenAPI/HMAC/nonce/source合同、增加业务结果表或跨仓NQ变化，立即进入`DH-STAGE-QDR-7-B2-BOUNDARY-EXPANSION-REVIEW`。
