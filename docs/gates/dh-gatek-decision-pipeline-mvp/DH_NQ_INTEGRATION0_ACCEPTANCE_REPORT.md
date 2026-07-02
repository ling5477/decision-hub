# DH-NQ Integration-0 Acceptance Report

> 任务：NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE
> 类型：DOCUMENTATION + ACCEPTANCE_REPORT
> 日期：2026-06-12
> 仓库视角：Decision Hub（DH，AI Agent 决策能力层 / 候选建议方）
> 对端：NexusQuant（NQ，交易事实源 / 主权执行方）
> 配套：`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` / `DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`

本文件与 NQ 仓库 `NQ_DH_INTEGRATION0_ACCEPTANCE_REPORT.md` 口径一致，DH 视角。

---

## 1. Acceptance decision

```text
Decision:             PASS
Integration-0 safety gate: CLOSED / ACCEPTED
Runtime integration:  NOT STARTED
Integration-1:        NOT STARTED
LIVE:                 DISABLED
AI:                   NOT STARTED
DH integration:       NOT INTEGRATED
```

Integration-0 是 contract / mock / documentation work line；本次验收只关闭 Integration-0 契约与 contract test 安全门，**不代表真实集成开始**。DH 当前仍无真实 NQ 调用、无真实 Provider、无交易能力、无 RealClient（含 RealNqBacktestClient）。

## 2. Completed work chain

```text
1. NQ 第一轮全仓只读审计              completed
2. DH 第二轮全仓只读审计              completed
3. NQ-DH 第三轮联合边界审计           completed
4. 三轮审计汇总                       completed
5. 事实源同步（DOC-SYNC-...-REGISTRATION） completed
6. Integration-0 契约冻结             completed
7. Integration-0 mock / contract test 设计   completed（15 项 × 16 字段矩阵）
8. Integration-0 contract test 代码实现       completed（NQ 16 + DH 16）
9. Integration-0 contract test implementation review / safety gate review  PASS
10. Integration-0 safety gate close / acceptance report  本文件
```

## 3. Contract scope accepted

以下 10 个契约已冻结并由 DH 侧 contract test 保护，状态：**contract-only / mock-only / test-protected / not runtime integration**。

```text
DH_TO_NQ：DHSignalCandidate / DHResearchReport / DHRiskReview / DHDecisionSummary
NQ_TO_DH：NQFeedbackEvent / NQPaperResultSummary / NQStrategyMetadata /
          NQBacktestSummary / NQErrorResponse / NQDhContractError
```

说明：DH 侧只用既有 Fake / Disabled client 与 test-only 内存校验器、脱敏 fixture 保护契约，**无真实 HTTP、无 RealClient、无真实 NQ**。

## 4. Test coverage accepted

| 用例 | 名称 | NQ | DH | negative path |
| --- | --- | --- | --- | --- |
| INT0-T01 | 禁止能力 | ✓ | ✓ | ✓ |
| INT0-T02 | 可开放能力 | ✓ | ✓ | n/a |
| INT0-T03 | header 缺失 | ✓ | ✓ | ✓（401/403/400） |
| INT0-T04 | HMAC 失败 | ✓ | ✓ | ✓ |
| INT0-T05 | timestamp 过期 | ✓ | ✓ | ✓（过去+未来） |
| INT0-T06 | nonce replay | ✓ | ✓ | ✓ |
| INT0-T07 | tenant mismatch | ✓ | ✓ | ✓ |
| INT0-T08 | payload > 64 KiB | ✓ | ✓ | ✓ |
| INT0-T09 | forbidden field | ✓ | ✓ | ✓ |
| INT0-T10 | raw prompt/context | ✓ | ✓ | ✓ |
| INT0-T11 | candidate schema | ✓ | ✓ | ✓ |
| INT0-T12 | feedback schema | ✓ | ✓ | ✓ |
| INT0-T13 | audit required | ✓ | ✓ | ✓ |
| INT0-T14 | no trading side-effect | ✓ | ✓ | n/a（副作用=0） |
| INT0-T15 | no credential access | ✓ | ✓ | n/a（凭证访问=0） |

```text
NQ 侧 16 tests passed；DH 侧 16 tests passed。
两侧均覆盖 T01-T15。
negative path 已覆盖（DH 24 个 assertFalse；11 个拒绝码断言 401/403/409/413/400）。
audit event shape 已覆盖（T13 + T04 SIGNATURE_FAILED + T06 REPLAY_REJECTED）。
forbidden side-effect 已覆盖（T14 side-effect=0；含 realNqCall=0 + T15 credential=0）。
```

## 5. Validation evidence

DH：

```text
mvn test                      BUILD SUCCESS
dh-domain                     86 tests / 0 failures（含 Integration-0 16）
Integration-0                 16 tests passed（ContractValidation 6 + Security 8 + NoSideEffect 2）
ArchitectureTest              12 条全绿（integration0 测试包未引入 RealClient / providers / HTTP client）
PostgresContainerSmokeTest    Docker 不可用自动 skip（既有环境性 skip，非本轮引入，不阻塞）
git status --short            clean
git diff --check              clean
git show HEAD                 仅 src/test/** + docs/current/**（无 src/main / 无 pom）
```

NQ（引用，详见 NQ 仓库 acceptance report）：

```text
mvn -f backend/pom.xml test   BUILD SUCCESS
nq-app                        51 tests / 0 failures（含 Integration-0 16）
ArchUnit                      ModuleBoundary / PackageBoundary 全绿
git status / git diff         clean
```

## 6. Accepted boundaries

```text
无生产代码改动（无 src/main）
无 API 改动
无 migration 改动
无 RealClient（含 RealNqBacktestClient）
无真实 Provider
无真实 HTTP
无真实交易所调用
无真实 NQ 调用
无 AI runtime
无 LIVE
无凭证读取
无 NQ DB 读写
无交易副作用
```

## 7. Still forbidden after Integration-0 close

```text
真实联调 / Integration-1 直接启动
NQ RealClient / DH RealClient
真实 Provider / 真实 HTTP / 真实 NQ endpoint / 真实交易所调用
下单 / 撤单 / Paper Run 启停 / 策略状态修改 / 风控状态修改
读写 NQ DB / 读取凭证
LIVE / AI 自动交易
```

## 8. Integration-1 blockers

### DH P1-4 residual（不阻塞 Integration-0 close，阻塞 Integration-1）

```text
rate limit 缺失
memory cap 缺失（dh-memory 5 Store 与 Stage2/Stage3 InMemory 仓储无上限）
replay nonce persistence 缺失
```

要求：

```text
Integration-1 前必须修复上述三项。
修复后必须重跑 contract tests。
T06 必须以持久化 / 集中缓存 nonce 重跑（替换 test-only 内存 nonce store）。
必须新增 rate limit 429 测试。
必须新增 memory cap / bounded store 测试。
```

### Header alignment

```text
DH 已实现的 NQ feedback authenticator 使用 X-DH-NQ-* 命名族；
Integration-0 冻结 canonical X-NQ-DH-* 命名族。
两者必须在 Integration-1 前统一（或映射层显式转换）。
统一后需更新 contract fixtures，并重跑 NQ / DH contract tests。
```

### Real channel safety

```text
Integration-1 必须单独开工，先做设计审计。
必须 staging / paper-only；LIVE disabled。
必须无凭证落日志；必须 no trading side-effect。
必须通过安全审查。
```

## 9. Next allowed actions

只允许：

```text
1. Integration-0 acceptance commit
2. Integration-0 archive / tag / gate close docs
3. Integration-1 planning-only audit
4. DH P1-4 residual fix planning
5. NQ GateK-PLAN 文档规划（NQ 侧）
```

禁止：

```text
1. 直接 Integration-1 implementation
2. 真实只读通道
3. 真实 HTTP
4. RealClient
5. Provider
6. LIVE
7. AI 自动交易
```

## 10. Final decision

```text
Integration-0:        PASS / CLOSED / ACCEPTED
Integration-1:        NOT STARTED
Runtime integration:  NOT STARTED
LIVE:                 DISABLED
AI:                   NOT STARTED
DH integration:       NOT INTEGRATED
```
