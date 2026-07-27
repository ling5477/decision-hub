# DH Stage-QDR-9 B4 Persistent Guard Fixture Scope Erratum

## 1. 结论

```text
task: DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-SCOPE-BLOCKER-RETRY-2
classification: DOCUMENTATION_PREWRITE / SCOPE_CONTRACT_ERRATUM / PERSISTENT_GUARD_POSTGRESQL_FIXTURE_SCOPE
starting HEAD: 675430a8a8e6cceaab75bb72c1fc1bf64af2da46
parent: 1971f3dc29fb690dcbd64cb7b0c62c797d81cbb1
origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3 / FRESHLY VERIFIED
upstream implementation retry: IMPLEMENTED IN WORKTREE
full-regression blocker: PersistentGuardProductionWiringPostgresTest
fresh full regression: 1238 tests / 3 failures / 0 errors / 0 skipped
technical changes during scope prewrite: 0
V16 / registry / retention: NOT CREATED / NOT IMPLEMENTED / NOT PRESENT
```

本 erratum 不改写此前 `42 / 42`、`46 / 46` 与 `47 / 47` scope 的历史事实。首次 implementation continuation 的新鲜全量回归表明，唯一失败类是 `PersistentGuardProductionWiringPostgresTest`：三个场景仍以旧构造器生成无 `FeedbackEnvironment` 与无 verified `FeedbackExecutionScope` 的请求。生产 root 正确返回 `403` 并 fail-closed；该行为不得为兼容旧 fixture 而降低。

## 2. 精确兼容性范围

### B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE

```text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
```

此范围只允许升级该既有 PostgreSQL/Testcontainers fixture：每个进入 persistent guard 的业务请求必须在调用点或严格必填的 fixture 参数中显式传入 `FeedbackEnvironment.DEV` 或 `FeedbackEnvironment.TEST`；该 environment 必须包含于 canonical HMAC，认证后必须创建 `new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.DEV)`（或等价显式 TEST scope）。

禁止默认 DEV helper、nullable environment、profile/tenant 推断、HMAC bypass、mock authenticator、绕过 persistent guard、将原成功或基础设施失败断言改成统一 `403`、删除 nonce/retry/recovery 断言，或关闭 PostgreSQL/Testcontainers。fixture 的目的仍是验证真实 production wiring、正常 persistent guard/nonce 行为，以及数据库 guard/nonce store 不可用时的冻结 `503` 或实际权威基础设施错误语义。

## 3. Scope 历史与不变量

```text
42 / 42: initial trusted-upstream scope
46 / 46: production / orchestrator / fingerprint / three PostgreSQL compatibility-test correction
47 / 47: dh-security -> dh-domain Maven dependency scope
48 / 48: persistent guard PostgreSQL fixture scope

B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
EFFECTIVE_UPSTREAM_SCOPE_INVARIANTS: 48 / 48 PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

Part A 只增加本 erratum、更新现有 scope/work-order 文档与 12 个 terminal factsources；没有修改 Java、test、POM、migration、configuration、API、V16、registry 或 retention。Part B 仅在本 scope erratum 形成独立本地提交后，才能修改上述一个 fixture。

## 4. 后续限制

```text
Persistent guard fixture: SCOPE AUTHORIZED / TECHNICAL FIX PENDING
Upstream contract: IMPLEMENTED IN WORKTREE / VALIDATION BLOCKED
B4 milestone review / publication / B5: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
V16 / registry / retention / new endpoint / scheduler / automatic learning: NOT AUTHORIZED
real HTTP / provider / NQ / Agent / LangGraph / Paper / LIVE: NOT AUTHORIZED
```
