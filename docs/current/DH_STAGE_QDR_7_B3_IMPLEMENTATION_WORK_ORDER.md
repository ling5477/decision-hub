# DH Stage-QDR-7 B3 Limited Dry-Run Runtime Readiness Implementation Work Order

> work order: `DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-IMPLEMENTATION`
> status: `FROZEN / WORK_ORDER_ONLY`
> implementation: `NOT_STARTED / NEXT`
> plan: `DH_STAGE_QDR_7_B3_PLAN.md / CLOSED / ACCEPTED`
> B2 capacity gate: `DEFERRED / KNOWN_LIMITATION`

## 1. 目标与边界

在不改变现有 API/security identity、不新增 persistence、不连接任何真实外部依赖的前提下，为既有 `POST /api/ai/decision-dry-runs` 增加明确的 dev/test-only runtime policy、fail-closed kill decision、统一 bounded deadline/concurrency/queue/backpressure 与 no-side-effect readiness 证据。

```text
ALLOW_IMPLEMENTATION: YES / THIS_WORK_ORDER_ONLY
ALLOW_CONTROLLER_CHANGE: NO
ALLOW_API_OR_OPENAPI_CHANGE: NO
ALLOW_MIGRATION_CHANGE: NO
ALLOW_REPOSITORY_EXPANSION: NO
ALLOW_EXISTING_RUNTIME_WIRING_CHANGE: YES / BOUNDED
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER_OR_LIVE: NO
```

Capacity gate deferred 继续是 known limitation。实施与测试不得写成 formal capacity acceptance，不得开启生产流量，也不得生成 Retry-5、capacity blocker 或 harness 修复任务。

## 2. 实施前置条件

- branch 与 worktree 由实施任务重新核验；不得假定 planning commit 已 push。
- 本 plan/work order 已在 current factsources 中标记为 `CLOSED / ACCEPTED` 和 `FROZEN`。
- B2 保持 `CLOSED WITH CAPACITY GATE DEFERRED`。
- exact implementation task ID 必须与本文件标题一致。
- 当前 endpoint、Controller、OpenAPI、migration、Repository 与 HMAC/tenant/nonce/source 合同必须保持不变。
- 若实现前 factsources、远端分支或允许范围发生冲突，停止并重新规划；不得 pull/merge/rebase 掩盖分叉。

## 3. Future write allowlist

### 3.1 生产实现

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/**
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/main/resources/application.yml
dh-app/src/main/resources/application-dev.yml
dh-app/src/main/resources/application-test.yml
dh-app/src/main/resources/application-prod.yml
```

`decision/dryrun/**` 只允许 B3 runtime policy、bounded admission、failure classification 与 no-side-effect contract；不得扩展为 provider、NQ client、Agent 或交易执行能力。

### 3.2 测试

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/**
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/b3/**
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

Controller WebMvc 测试只能验证现有 wire contract，不授权修改 `dh-api/src/main/**`。`qdr7/capacity/**` 不在写范围。

### 3.3 文档

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/DH_STAGE_QDR_7_B3_PLAN.md
docs/current/DH_STAGE_QDR_7_B3_IMPLEMENTATION_WORK_ORDER.md
```

仅做与实施证据直接相关的最小同步；AGENTS/CLAUDE/FACTSOURCE_POLICY/CODEX_PROJECT_INSTRUCTIONS 只有真实 current conflict 且同一任务 write allowlist 明确包含时才能修改。

## 4. Future forbidden scope

```text
dh-api/src/main/**
dh-security/src/main/**
dh-infra/src/main/**
dh-domain/src/main/**
dh-connector/**
pom.xml
.github/workflows/**
scripts/qdr7-capacity/**
config/qdr7-capacity/**
dh-app/src/test/**/qdr7/capacity/**
dh-app/src/main/resources/db/migration/**
contracts/**
golden_cases/**
NQ repository
target/**
```

需要进入上述任一范围时停止本任务，并按 review 触发条件处理。

## 5. B3.1 — Runtime Boundary Contracts

### Step

建立 `LimitedDryRunRuntimePolicy`、`RuntimeFeatureFlag`、`RuntimeKillSwitch`、`RuntimeEnvironmentPolicy`、`RuntimeFailureClassification`、`NoSideEffectDecisionContract` 或等价职责对象，并冻结运行限制校验。

### Files

`dh-usecase/src/main/**/decision/dryrun/**` 及对应 unit tests。

### 必须实现

- feature disabled 永远拒绝；默认 false。
- 仅 dev/test environment 可进入；prod、unknown、blank、多 profile 冲突均拒绝。
- kill `DENY / UNKNOWN / STALE / READ_FAILED` 均拒绝；判定优先于执行。
- enabled 时 deadline、concurrency、queue 与 queue wait 必须显式、合法、有界。
- deadline `1..30000 ms`；concurrency `1..32`；queue `0..64`；queue wait `0..1000 ms` 且小于 deadline。
- retry count 固定 0；没有真实 outbound target。
- failure reason 为稳定低基数分类，不暴露配置值、内部异常、凭证或原始 payload。
- no-side-effect action allowlist 与 forbidden mutation/transport 能力必须显式。

### Success

Unit tests 覆盖全部组合和边界值；实现不依赖 Spring、HTTP、Repository 或数据库。

## 6. B3.2 — Protected Mock-Only Runtime Wiring

### Step

将 B3.1 合同接入现有 dry-run service/wiring，建立 bounded admission 与 deadline 传播，复用现有认证、persistent rate/idempotency、mock gateway、snapshot/audit/trace/replay。

### Files

允许的 `decision/dryrun/**`、`DecisionDryRunRuntimeWiringConfig.java` 和 application profile 配置。

### 必须实现

- runtime policy 在业务执行前生效；拒绝时不进入 orchestrator/mock provider。
- bounded executor/permit/queue 生命周期明确，线程数与队列有硬上限，应用关闭时资源释放。
- deadline 覆盖 B3 service admission、persistent idempotency、mock execution、audit/snapshot 与 finalization；不能无限等待。
- queue full、permit unavailable、deadline exhausted、kill denied/unknown、environment denied 均 fail-closed。
- current Controller、HMAC、tenant/source、nonce、rate 与 idempotency 顺序不变。
- provider 仍只允许 `MockModelProvider`；不加入 retry/circuit breaker。
- prod profile 必须 `enabled=false`、`production-enabled=false`、`kill-switch-enabled=true`；B3 不允许改变该事实。

### Stop

若无法在不修改 Controller/API/security identity 的前提下建立完整 deadline，则停止并输出 `B3_API_SECURITY_REVIEW_REQUIRED`；不得弱化 deadline 合同或原地扩展 Controller。

### Success

Wiring tests 证明默认关闭、dev/test 显式开启、prod/unknown 拒绝、kill 优先、资源有界、mock-only 且所有异常 fail-closed。

## 7. B3.3 — Runtime Readiness and No-Side-Effect Tests

### Step

形成不依赖 formal capacity 的 B3 readiness 测试证据。

### 必测矩阵

- flag 默认关闭、缺失关闭、显式 false 拒绝。
- production profile 与 unknown environment 拒绝。
- kill deny/unknown/stale/read failure 拒绝。
- invalid deadline/concurrency/queue/queue wait 配置拒绝。
- deadline、queue full、concurrency exhausted、取消和资源释放。
- HMAC、tenant、source、timestamp、nonce、payload cap 持续强制。
- persistent rate/idempotency、same nonce、duplicate request 与 tenant/environment isolation 不回归。
- provider bean 精确为 mock-only；外部 HTTP/NQ runtime 调用为 0；retry 为 0。
- action allowlist；`BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER` 拒绝。
- order/risk/ledger/account/Paper/LIVE mutation 为 0。
- success/reject/timeout/backpressure audit、trace、snapshot/result ref 完整且脱敏；audit failure fail-closed。
- ArchitectureTest 禁止 HTTP client、NQ client、Provider SDK、Agent/LangGraph 与交易依赖进入 B3 scope。
- PostgreSQL/Testcontainers 相关测试真实执行且 0 skipped；不得用 in-memory 替代生产 guard 路径。

### Success

目标测试、模块测试、full regression 与 quality 全部通过；无敏感输出、无残留线程/队列/容器、无禁止范围 diff。

## 8. B3.4 — Final Close

B3.1-B3.3 完成并各自形成可审查 commit 后，进入独立 final close：

```text
mvn -ntp test
mvn -ntp -Pquality validate
```

Final close 必须验证：

- full reactor success，tests 0 failure/error/skip。
- PostgreSQL/Testcontainers mandatory tests 真实执行。
- Checkstyle 0、Spotless PASS。
- security/no-side-effect scan 0 findings。
- endpoint 默认关闭、prod denied、kill fail-closed、mock-only、external HTTP/NQ call 0。
- current facts conflict 0。
- self-contained B3 milestone evidence packet 完整。

B3 使用 milestone close，不创建 Stage-QDR-7 tag。完整 Stage archive 和 annotated tag 仍属于后续 B5，必须遵循 archive-before-tag。

## 9. Review 触发条件

仅以下情况创建独立 review：

```text
migration 或表结构变化
新增或修改 API / Controller / OpenAPI
HMAC、tenant、nonce、timestamp、source 或认证顺序变化
真实 HTTP 或 Provider 提案
真实 NQ 调用提案
production persistence / Repository 扩张
P0 / P1 安全缺陷
B3 final close
```

B3.1-B3.3 的普通 policy、DTO/internal model、mock-only wiring 和测试连续推进，不为每批创建 standalone review。

## 10. 验证命令

实施任务按风险逐步执行，最终至少包含：

```powershell
mvn -ntp -pl dh-usecase -am test
mvn -ntp -pl dh-api -am test
mvn -ntp -pl dh-app -am test
mvn -ntp test
mvn -ntp -Pquality validate

git diff --check
git diff --name-only
git status --short
```

同时执行禁止依赖/文本扫描，确认 B3 变更没有引入 HTTP client、NQ runtime、Provider SDK、Agent、LangGraph、order/risk/ledger/Paper/LIVE mutation 或敏感日志。

## 11. 回滚与停止条件

运行时停止顺序：

```text
kill = deny
-> runtime enabled = false
-> production gate = false
-> stop accepting dry-run traffic
```

代码回滚按 B3 commit 逆序使用 `git revert`；禁止 `reset --hard`，不得删除或回写 B2 migration、persistent guard、nonce/idempotency 或 audit evidence。

以下任一条件阻断完成：

- API/Controller、migration、Repository 或 security identity 变化没有独立 review。
- prod 或 unknown environment 可进入。
- kill 未优先拒绝，或 unknown/read failure fail-open。
- executor/queue 无界、deadline 可缺失、存在无限重试或资源泄漏。
- 真实 HTTP、Provider、NQ、Agent、LangGraph、Paper、LIVE 或交易 mutation 被引入。
- PostgreSQL/Testcontainers 被跳过，或 full regression/quality 失败。
- current facts conflict 非 0。

## 12. 完成判定与下一步

本 work order 当前只完成冻结，未执行 implementation：

```text
STAGE_QDR_7_B3_PLAN: CLOSED / ACCEPTED
STAGE_QDR_7_B3_IMPLEMENTATION_WORK_ORDER: FROZEN
STAGE_QDR_7_B3_IMPLEMENTATION: NOT_STARTED / NEXT
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION: YES / NEXT_TASK_ONLY
NEXT_TASK: DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-IMPLEMENTATION
```
