# DH Stage-QDR-7 B2 Persistent Guards Blocker Fix

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX`
> classification: `CODE_CHANGE + P1_SECURITY_BLOCKER_FIX + FORWARD_ONLY_MIGRATION`
> status: `DONE / REVIEW_RETRY_READY`
> review history: previous milestone review remains `BLOCKED`
> next action: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY`

## 1. 修复结论

本轮以forward-only V13关闭P1-1至P1-5。V12保持不变；HTTP API、Controller/DTO、OpenAPI、HMAC、nonce与source合同均未修改。B2尚未被本实现自行标记为accepted，只允许进入独立milestone review retry。

```text
V13_FORWARD_FIX: PASS
V12_IMMUTABILITY: PASS
IDEMPOTENCY_EXPIRY_LIFECYCLE: PASS
REQUEST_ID_TOMBSTONE: PASS
DB_CLOCK_UNIFICATION: PASS
RATE_CLEANUP_SAFETY: PASS
IDEMPOTENCY_CLEANUP_SAFETY: PASS
LEASE_OWNER_RECOVERY: PASS
RESULT_REFERENCE_EVIDENCE: PASS
ADMISSION_AUDIT_ATOMICITY: PASS
COMPLETION_ATOMICITY: PASS
COMMIT_UNKNOWN_EVIDENCE: PASS
STORE_FAILURE_FAIL_CLOSED: PASS
PRODUCTION_IN_MEMORY_FALLBACK: ABSENT
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / ZERO_SKIPS
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY: YES
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Schema与生命周期

V13新增`lease_owner`、`result_type`、`failed_at`、`expired_at`，对V12历史状态执行显式回填后重建状态CHECK。`COMPLETED`固定`result_type=DH_DECISION_OUTPUT`；`FAILED`和`EXPIRED`不再复用`completed_at`。无法满足既有状态不变量的数据会使migration失败，不静默产生非法行。

`COMPLETED/FAILED -> EXPIRED`由internal bounded cleanup按expected state/version执行CAS。cleanup清除result/failure/lease细节但保留完整identity、request hash和`EXPIRED`状态；本阶段不物理删除idempotency identity。因此same hash固定返回`IDEMPOTENCY_EXPIRED / 409`，different hash固定返回`IDEMPOTENCY_CONFLICT / 409`。

## 3. Database clock与cleanup

Admission只接受严格校验的TTL/retention `Duration`，lease只接受`Duration`；created/updated/window/lease/expiry/retention/terminal时间均由`transaction_timestamp()`产生并由SQL返回。应用时钟只用于非guard审计记录，不参与持久化identity、lease、TTL、retention或cleanup资格。

Rate cleanup只删除`window_end < transaction_timestamp() - safety_grace`的bounded、indexed、`FOR UPDATE SKIP LOCKED`候选。Idempotency cleanup拒绝active lease，分别按DB-time `retention_until`或`expires_at`判定，并只转为tombstone。`safetyGrace`必须为正且不超过1天，batch为1..1000。

## 4. Lease、result与事务证据

`IN_PROGRESS`强制`lease_owner + lease_token + lease_expires_at`同时存在。Heartbeat校验owner/token/state/version；recovery仅在PostgreSQL认定lease过期后接管。Owner是进程内生成的低敏感随机实例标识，不包含凭证、IP秘密或用户数据，也不进入日志/response。

真实JDBC测试验证tenant-bound `dh_decision_output` FK、missing/wrong-tenant拒绝、duplicate missing/checksum mismatch映射、rate/idempotency admission与audit rollback、output+audit+COMPLETED成功提交和失败整体rollback。测试专用`DataSource/Connection`代理在delegate commit真实完成后抛出`SQLException`，验证返回`RATE_LIMIT_COMMIT_UNKNOWN`，随后通过exact DB reconcile确认已提交且不重新admit。

## 5. 验证

```text
targeted PostgreSQL/Flyway suite: 14 tests / 0 failures / 0 errors / 0 skipped
owning modules: BUILD SUCCESS
full Maven: 155 reports / 1054 tests / 0 failures / 0 errors / 0 skipped
quality: BUILD SUCCESS / Checkstyle 0 / Spotless PASS
PostgreSQL: 17.10 via Testcontainers
capacity benchmark: NOT RUN / NOT ALLOWED
```

## 6. 边界

未修改NQ、V1-V12、API/Controller/DTO/OpenAPI、HMAC/nonce/source；未新增第三张结果表；未接external HTTP、Provider、Agent、LangGraph；未触碰交易、Paper或LIVE；未运行capacity benchmark；未进入B3；未push或创建tag。
