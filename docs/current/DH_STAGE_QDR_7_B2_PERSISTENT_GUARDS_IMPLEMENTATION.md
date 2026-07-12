# DH Stage-QDR-7 B2 Persistent Guards Implementation

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION`
> classification: `CODE_CHANGE + ADDITIVE_MIGRATION + PERSISTENT_GUARDS`
> status: `DONE / LOCAL_VALIDATED`
> endpoint: `POST /api/ai/decision-dry-runs`（既有合同未修改）
> next action: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW`

## 1. 实施结论

本批次按已冻结的 schema/security review 实现 PostgreSQL multi-instance persistent guards。新增 additive V12、fixed-window atomic admission、persistent idempotency state/CAS/lease、bounded cleanup、tenant-first capability ports、JDBC adapters、usecase transaction orchestration和production wiring。PostgreSQL/store/commit不确定均fail-closed，未提供JVM-local或generic in-memory fallback。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_IMPLEMENTATION: DONE / LOCAL_VALIDATED
V12_MIGRATION: PASS
RATE_LIMIT_PERSISTENCE: PASS
IDEMPOTENCY_PERSISTENCE: PASS
KEY_DOMAIN_ISOLATION: PASS
TRANSACTION_ATOMICITY: PASS
STATE_MACHINE_CAS: PASS
LEASE_CRASH_RECOVERY: PASS
RESULT_REFERENCE_SAFETY: PASS
CLEANUP_SAFETY: PASS
STORE_FAILURE_FAIL_CLOSED: PASS
PRODUCTION_IN_MEMORY_FALLBACK: ABSENT
API_CONTRACT_UNCHANGED: YES
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / ZERO_SKIPS
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED / NOT_RUN
```

## 2. Migration 与 schema

- 新增`V12__qdr7_persistent_runtime_guards.sql`；V1-V11未修改。
- `dh_qdr7_rate_limit_bucket`使用`environment + endpoint + source + tenant_id + window_start`组合主键；DB transaction time生成UTC fixed window；counter、limit、window和canonical identity均有CHECK。
- `dh_qdr7_idempotency_guard`使用`environment + endpoint + source + tenant_id + request_id`唯一身份；保存lowercase SHA-256、state/version、opaque lease、safe result reference、stable error和expiry/retention timestamps。
- `dh_decision_output(tenant_id, decision_id)`增加tenant-bound unique constraint，供COMPLETED外键精确引用；未新增第三张结果表。
- cleanup索引支持expired bucket、terminal retention和expired lease；无raw body、canonical request、signature、nonce、credential、prompt、provider raw response或交易材料列。

## 3. Runtime 与事务语义

- Rate limit选择fixed-window counter；单事务conditional upsert产生精确winner，达到quota后返回429，store/commit/config分别返回独立fail-closed分类。
- 请求顺序保持feature/production gate、payload/header/HMAC/timestamp/nonce在前；认证成功后才消费persistent rate admission，再进入persistent idempotency。
- Dry-run route精确排除generic key-only`IdempotencyFilter`；NQ feedback继续使用原JVM-local limiter bean，dry-run通过qualified persistent limiter。
- Idempotency首次atomic insert为`RECEIVED`，随后expected state/version/token CAS进入`IN_PROGRESS`并完成`COMPLETED/FAILED/EXPIRED`合法边；heartbeat和expired-lease internal recovery只允许opaque token条件更新。
- 首次业务执行、existing safe output/audit与terminal transition由同一required transaction包围；audit/output/guard任一失败均rollback，不先返回成功。
- COMPLETED duplicate从tenant-bound replay read model重建immutable safe snapshot并校验checksum；FAILED不自动重执行；conflict/in-progress/expired保持409。
- cleanup使用bounded batch和`FOR UPDATE SKIP LOCKED`；active lease和未达到retention条件的记录不删除；本批次不创建生产调度频率。

## 4. Fingerprint 与数据最小化

Fingerprint为：

```text
SHA-256("QDR7-DRYRUN-CJSON-1\n" + canonicalRequestBytes)
```

纳入canonical tenant/source/schemaVersion/dryRun、stable decisionContext和sorted/deduplicated forbiddenCapabilities；排除requestId、traceId、transport timestamp、nonce、signature和headers。只持久化64位lowercase hash，不持久化canonical bytes或raw material。

## 5. 验证证据

| Check | Result |
|---|---|
| targeted store classification | 4 tests，0 failures/errors/skips |
| owning modules | `BUILD SUCCESS`；875 tests，0 failures/errors/skips |
| full Maven | `BUILD SUCCESS`；1045 tests，0 failures/errors/skips |
| PostgreSQL/Testcontainers | PostgreSQL 17.10真实运行；V1→V12、V11→V12、checksum、并发、CAS、lease、cleanup、rollback通过 |
| quality | `mvn -ntp -Pquality validate`通过；Checkstyle 0 violations；Spotless check通过 |

## 6. 保持边界

未修改API path/header/request/response envelope、DTO字段、OpenAPI、HMAC canonical material、nonce identity或V1-V11；未修改NQ；未接external HTTP、Provider、Agent、LangGraph；未触碰订单、账户、ledger、Paper或LIVE；未运行capacity benchmark；未选择最终生产吞吐默认值；未push/tag。

## 7. 后续门禁

本实现只允许进入独立milestone review。Post-B2 capacity acceptance仍必须在actual-wiring 2xx harness上测量rate hotspot、window boundary burst、lease/TTL、cleanup、connection pool、latency和growth，再选择最终默认值。B3、API/OpenAPI变化、真实HTTP/Provider/NQ/Agent/LangGraph/LIVE均未授权。
