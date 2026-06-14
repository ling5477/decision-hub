# DH-NQ Header Alignment Plan

> 任务：DH-NQ-HEADER-ALIGNMENT-PLAN
> 类型：INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION
> 日期：2026-06-14
> 仓库：Decision Hub（DH）；配套 NQ 侧只读核查
> 状态：**planning-only**（本轮只读核查 + 输出方案；未改运行代码 / 测试；未启动 Integration-1）
> 配套：`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` / `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`

本文件**只做对齐方案设计**：canonical 化的实施分批进行（`DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-*`）。

> **实施进展**
> - PLAN：ACCEPTED（DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW，2026-06-14）。已定策略：canonical-only（无兼容期）；
>   Tenant/Request/Trace 保权威来源、header 仅一致性校验不覆盖；timestamp 格式另列 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT。
> - **Batch 1：DONE（2026-06-14）** —— 内部结构 skeleton（集中 header 常量 `NqDhHeaderNames` + 归一化模型
>   `NormalizedNqDhHeaders` + `NqDhHeaderParser`（legacy-only 读取）+ `NqDhHeaderValidator` skeleton + `NqDhHeaderValidationResult`）；
>   controller 去 magic string、经 parser 读取，**对外行为不变**（仍 legacy `X-DH-NQ-*`）。canonical 读取与强制留待 Batch 2+。
> - **header alignment 整体仍 NOT COMPLETED**：canonical-only 尚未切换；下一步 Batch 2（canonical 读取）。

---

## 1. 目标与背景

把 DH NQ feedback 入站链路的历史 header 族 `X-DH-NQ-*` 对齐到 Integration-0 冻结的 canonical 族 `X-NQ-DH-*`，使 DH 运行时与冻结契约一致，为 Integration-1 扫清 header 命名前置项。本轮仅规划。

canonical 族（以 Integration-0 frozen contract 为准）：

```text
X-NQ-DH-Source
X-NQ-DH-Tenant-Id
X-NQ-DH-Request-Id
X-NQ-DH-Trace-Id
X-NQ-DH-Timestamp
X-NQ-DH-Nonce
X-NQ-DH-Signature
Content-Type: application/json
```

legacy 族（DH 运行时现状）：`X-DH-NQ-Source / Tenant-Id / Request-Id / Trace-Id / Timestamp / Nonce / Signature`。

---

## 2. 当前 header map（只读核查结论，2026-06-14）

### 2.1 NQ 仓库（`E:\Project\nexus-quant`）

| 位置 | header | 说明 |
| --- | --- | --- |
| `backend/nq-app/src/test/.../integration0/support/Int0Contract.java` | canonical `X-NQ-DH-*`（7） | 仅测试 fixture |
| `docs/current/NQ_DH_INTEGRATION0_SECURITY_POLICY.md` / `..._CONTRACT_FREEZE.md` / `..._CONTRACT_TEST_PLAN.md` / `STATUS.md` / `WORKLOG.md` | canonical `X-NQ-DH-*` 定义 + `X-DH-NQ-*` 对齐说明 | 文档 |
| NQ 生产代码（`backend/*/src/main`） | **无** | **NQ 侧当前无 DH 入站端点 / 无 header 处理生产代码** |

结论：NQ 侧 canonical 只在 fixture/docs；**无生产 header 处理**，无 legacy 生产用法；NQ-DH not integrated 成立。

### 2.2 DH 仓库（`E:\Project\decision-hub`）

| 位置 | header | 说明 |
| --- | --- | --- |
| `dh-api/.../api/feedback/NqFeedbackController.java`（生产） | **legacy `X-DH-NQ-*`**：Source / Timestamp / Nonce / Signature（4） | 仅消费这 4 个 header |
| `dh-api/.../feedback/NqFeedbackControllerWebMvcTest.java` / `NqFeedbackRateLimitWebMvcTest.java`（测试） | legacy `X-DH-NQ-*`（4） | WebMvc 测试 |
| `dh-domain/.../integration0/support/Int0Contract.java`（测试 fixture） | **canonical `X-NQ-DH-*`（7）** | INT0 契约抽象，不经真实 controller |
| `dh-security/.../HmacNqFeedbackAuthenticator.java`（生产） | **无 header name 常量**（按 value 签名；header 由 controller 解析后传入 `NqFeedbackAuthRequest`） | 见 §5 |
| `docs/current/DH_NQ_INTEGRATION0_*` / `STATUS.md` / `ROADMAP.md` / `README.md` | canonical 定义 + legacy 对齐说明 | 文档 |

### 2.3 关键结构差异（不止于改名）

DH 运行时**只用 4 个 header**（Source / Timestamp / Nonce / Signature），其余 3 个 canonical header 的等价信息来自非 header 通道：

```text
Tenant-Id   -> 来自 API 认证上下文（DhApiAuthenticationFilter -> AuthenticatedRequest.requireTenantId），非 NQ header
Request-Id  -> 来自 envelope body（req.getRequestId()）
Trace-Id    -> 来自 envelope body（req.getTraceId()）
```

因此"对齐"不是纯 7→7 的 header 改名：

- Source / Timestamp / Nonce / Signature：**1:1 改名**（legacy header -> canonical header）。
- Tenant-Id / Request-Id / Trace-Id：契约期望 header，DH 现状来自 auth-context / body。需在 IMPL 阶段决策：是否额外接收 canonical header 并与现有来源做一致性校验（见 §4.5、Finding P1-2）。本轮不冻结该决策，只标注。

---

## 3. Findings（P0/P1/P2/P3）

### P0
- 无。

### P1
- **P1-1 运行时与冻结契约 header 命名不一致**：DH 生产 controller 用 legacy `X-DH-NQ-*`，Integration-0 冻结 canonical 为 `X-NQ-DH-*`。这是 Integration-1 的已登记前置项（`CONTRACT_FREEZE.md` §6 / `ACCEPTANCE_REPORT.md`），尚未对齐。
- **P1-2 契约 7 header 与实现 4 header 的结构差异**：Tenant-Id / Request-Id / Trace-Id 在 DH 现状来自 auth-context / body，而非 header。对齐方案必须显式决策这 3 项的处理，避免"声称对齐 7 header 实则只改了 4 个"。

### P2
- **P2-1 DH 内部命名分裂**：生产代码 legacy、INT0 fixture canonical。二者解耦（INT0 不经真实 controller）所以都绿，但对读者有误导，且掩盖了 P1-1。canonical 化后应消除分裂。
- **P2-2 source 双绑定语义需在解析层保留**：authenticator 校验 `body.sourceSystem == headerSource` 且 `headerSource ∈ allowlist`。改名后 header parser 必须保持"header source 与 body source 一致"校验，不能因引入兼容层而弱化。

### P3
- **P3-1 header 常量散落**：legacy header name 目前硬编码在 `NqFeedbackController` 的 4 个常量里，无集中 header 契约类。canonical 化前应先抽出集中常量 / parser（Batch 1），避免散改遗漏。
- **P3-2 docs/fixtures 术语**：多处 docs 同时出现 legacy/canonical（多为对齐说明），canonical 化收尾时需统一表述，避免历史名被当成当前现状。

---

## 4. Recommended alignment strategy

### 4.1 总体方针：canonical 优先 + 有限兼容期

推荐**带兼容期的渐进对齐**，而非一刀切 canonical-only：

- DH NQ feedback 当前仍是 contract/test 保护下的入站骨架（NQ 侧无真实发送方），但保留兼容期可让未来真实 NQ 发送方在切换期内不被硬断；兼容期短、以 canonical 优先、并以 fail-closed 收口冲突。
- 兼容仅限**入站读取**；DH 自身不对外发 NQ-DH header（无出站方向需要对齐）。

> 若选择 canonical-only（不留兼容期）：优点是实现简单、无双 header 攻击面；风险是任何已按 legacy 实现的发送方会被立即拒绝。鉴于 NQ 侧尚无真实发送方，canonical-only 也可行——本方案把"是否保留兼容期"作为 PLAN-REVIEW 的待决选项，默认推荐"短兼容期 + 显式 deprecation + 后续单独移除"。

### 4.2 是否需要兼容期 / 是否允许双 header 接收

- **需要**（默认推荐）：兼容期内入站**同时接受** canonical 与 legacy，但 **canonical 优先**。
- 兼容期为**显式、有限、可观测**：每次命中 legacy 记 deprecation 审计码；兼容期移除是独立批次（Batch 6），本方案不移除。

### 4.3 Conflict policy（冲突策略）— fail-closed

| 情形 | 处理 |
| --- | --- |
| 只有 canonical | **目标状态**，正常处理 |
| 只有 legacy（兼容期内） | 接受，但记 `LEGACY_HEADER_USED` 审计 + 响应/日志标记 legacy |
| canonical + legacy 同名且**值相同** | 接受，记 `LEGACY_HEADER_USED`（deprecation warning），按 canonical 值处理 |
| canonical + legacy 同名且**值不同** | **fail-closed**：拒绝，记 `HEADER_CONFLICT`；状态码沿用现有认证错误模型（建议 400 BAD_REQUEST 或 401，IMPL-REVIEW 定）|
| 缺必需 header | **保持现有缺失语义**（如 Source/Signature 缺失 -> 现有 401/403 路径），记 `MISSING_CANONICAL_HEADER` |

原则：**绝不因兼容期放宽校验**；冲突一律 fail-closed，杜绝 downgrade。

### 4.4 Signature canonicalization（签名策略）

只读核查结论：`HmacNqFeedbackAuthenticator.signatureMaterial()` 的签名原材料是 **value-based，不含 header name**：

```text
signatureMaterial = join("\n",
  normalize(sourceSystem), timestampValue, nonceValue,
  eventId, requestId, traceId, payloadJson)
```

由此：

- **签名不随 header 改名漂移**（签的是值，不是 header 名）。这是本次对齐安全性的关键支点。
- **防混签**：必须先把 legacy/canonical 解析进**单一 normalized 逻辑模型**（每个逻辑字段唯一值，canonical 优先 + §4.3 冲突 fail-closed），**再**基于该 normalized 值构建签名原材料并验签。绝不允许"用 legacy 的 timestamp 值 + canonical 的 nonce 值"等跨族混取。
- 需要 **normalized header map**：`parse(headers) -> NqDhHeaderSet`（含 source/tenant/requestId/traceId/timestamp/nonce/signature 的解析值 + 命中来源标记 canonical|legacy）。
- 明确签名 payload（保持现状字段集）：source、timestamp、nonce、eventId、requestId、traceId、body/payload。**不引入 header name 进签名**（保持 value-based，避免兼容期签名分叉）。
- **不记录签名原材料 / secret 到日志**（沿用现状）。

### 4.5 Tenant-Id / Request-Id / Trace-Id 处理（P1-2 的方案建议）

推荐：**保持现有来源为权威**（tenant=auth-context，requestId/traceId=body），canonical header 仅作"可选附加"，若提供则与权威来源做一致性校验：

```text
X-NQ-DH-Tenant-Id  若提供 -> 必须等于 authenticated tenant，否则 fail-closed（沿用 tenant binding 403 语义）
X-NQ-DH-Request-Id 若提供 -> 必须等于 body.requestId，否则 fail-closed
X-NQ-DH-Trace-Id   若提供 -> 必须等于 body.traceId，否则 fail-closed（或以 header 为 trace 传播源，IMPL-REVIEW 定）
```

理由：不弱化"tenant 来自已认证主体"的安全基线；不把 body 既有字段降级为可被 header 覆盖。该决策在 PLAN-REVIEW 确认后于 Batch 2/3 落地。

### 4.6 Audit and logs

新增审计码（只记类型 + 安全字段，不记 raw signature/secret/full body/credential）：

```text
LEGACY_HEADER_USED        命中 legacy 族（兼容期 deprecation 信号）
HEADER_CONFLICT           canonical 与 legacy 同名值冲突 -> fail-closed
MISSING_CANONICAL_HEADER  缺必需 header（兼容期内也按现有缺失语义）
SIGNATURE_MISMATCH        验签失败（沿用现有 BAD_SIGNATURE，不新增泄露）
```

审计字段：source、tenantId、route、traceId、requestId、命中族（canonical|legacy|both）、auditCode、错误类型。**禁止**：raw signature、raw secret、full body、payload 明文、credential。

### 4.7 如何避免 downgrade / spoofing

- canonical 优先 + 冲突 fail-closed（§4.3），攻击者无法用 legacy 值覆盖 canonical。
- 兼容期不放宽任何校验（source allowlist / timestamp 窗口 / nonce replay / payload 64KiB / signature 全部保留）。
- 签名 value-based 且基于 normalized 值，杜绝跨族混签绕过。
- Tenant-Id/Request-Id/Trace-Id 的 header 不得覆盖权威来源（§4.5），防止 header 注入提权 / 串租户。
- legacy 命中记审计，便于监控兼容期残留与异常。

---

## 5. Implementation batches（后续实施批次，本轮不实施）

> 每批均：不接真实 NQ / 不真实 HTTP / 不新增 RealClient / 不新增真实 Provider / 不启动 Integration-1 / 不开 LIVE。验收均含 `mvn test` + `mvn -Pquality validate` BUILD SUCCESS + INT0-T01..T15 16/16 未破坏。

### Batch 1 — 集中 header 常量 + parser + normalized model
- 允许：`dh-api/.../api/feedback/**`、`dh-security/.../security/nq/**`（新增 header 常量类 / `NqDhHeaderSet` + parser，纯结构，不改对外行为）、对应单测。
- 禁止：改 controller 对外 header 行为、改签名语义、改 NQ。
- 验收：新增 parser 单测；既有 WebMvc / authenticator 测试全绿（行为不变）。

### Batch 2 — 支持 canonical `X-NQ-DH-*`（读取）
- 允许：controller / parser 接受 canonical 4 header（Source/Timestamp/Nonce/Signature）+（按 §4.5）可选 canonical Tenant/Request/Trace 一致性校验；canonical 路径走 normalized 模型与现有 authenticator。
- 禁止：移除 legacy；改签名 value 集；改 NQ。
- 验收：`accepts_canonical_headers`；INT0 不回归。

### Batch 3 — 兼容 legacy（canonical 优先）
- 允许：parser 同时接受 legacy；canonical 优先；命中 legacy 记 `LEGACY_HEADER_USED`。
- 禁止：legacy 放宽任何校验；改 NQ。
- 验收：`accepts_legacy_headers_during_compat_period`、`canonical_takes_precedence_when_both_same`。

### Batch 4 — 冲突 fail-closed
- 允许：实现 §4.3 冲突矩阵（同名值不同 -> fail-closed，记 `HEADER_CONFLICT`），按现有错误模型映射状态码。
- 禁止：fail-open；混签绕过。
- 验收：`rejects_conflicting_canonical_and_legacy_headers`、`rejects_signature_mismatch_after_header_normalization`。

### Batch 5 — docs / fixtures 切 canonical
- 允许：DH WebMvc 测试 fixtures、docs（contract/test plan/README）统一到 canonical 表述；INT0 fixture 已是 canonical，核对一致。
- 禁止：删除 legacy 支持（仍在兼容期）；改 NQ 生产。
- 验收：docs/fixtures 一致性核查；测试全绿。

### Batch 6 — compat deprecation review 后移除 legacy（**不在本规划链内执行**）
- 前置：兼容期监控 `LEGACY_HEADER_USED` 归零 + 单独 deprecation review。
- 允许：移除 legacy 解析分支 + 相关测试调整。
- 禁止：在 review 通过前移除。

### NQ 侧（独立 NQ-scoped 任务）
- NQ 当前无生产 header 处理，仅 fixture/docs 已是 canonical。建议由 NQ 仓库自身任务补 `docs/current/NQ_DH_HEADER_ALIGNMENT_PLAN.md` companion；**本轮不跨仓写 NQ**（保持 DH/NQ 仓库边界，详见输出说明）。

---

## 6. Tests to add / update（规划，本轮不写）

```text
accepts_canonical_headers                                  Batch 2
accepts_legacy_headers_during_compat_period                Batch 3
canonical_takes_precedence_when_both_same                  Batch 3
rejects_conflicting_canonical_and_legacy_headers           Batch 4
rejects_missing_required_header                            Batch 2/4（沿用现有缺失语义）
rejects_signature_mismatch_after_header_normalization      Batch 4
does_not_log_signature_raw_material                        Batch 1+（贯穿）
keeps_payload_64kib_gate                                   每批回归（NqFeedbackPayloadSizeGateTest）
keeps_nonce_replay_protection                              每批回归
keeps_rate_limit_key_source_tenant_route                   每批回归（限流前置不受 header 改名影响）
keeps_tenant_binding                                       Batch 2（§4.5）
keeps_INT0_T01_to_T15_green                                每批回归（DhNqIntegration0* 16/16）
no_real_http / no_real_nq_call / no_trading_side_effect    每批边界
no_credential_access                                       每批边界
```

---

## 7. 14 问速答

```text
1. NQ 是否存在 X-DH-NQ-*：仅 docs 对齐说明引用（6 处）；无生产代码用法。
2. NQ 是否存在 X-NQ-DH-*：是，canonical（INT0 fixture 7 + docs 定义）；无生产 header 处理代码。
3. DH 是否存在 X-DH-NQ-*：是，生产 controller + WebMvc 测试用 legacy（Source/Timestamp/Nonce/Signature 4）。
4. DH 是否存在 X-NQ-DH-*：是，仅 INT0 fixture（canonical 7）+ docs；生产代码未用。
5. 命名是否一致：不一致。DH 生产=legacy，DH INT0 fixture / 两仓 docs / NQ fixture=canonical（P1-1/P2-1）。
6. 哪些必须 canonical 化：DH 生产入站读取的 4 header（Source/Timestamp/Nonce/Signature）；Tenant/Request/Trace 按 §4.5 决策。
7. 是否需要兼容期：推荐需要（短、显式、可观测）；canonical-only 亦可，列为 PLAN-REVIEW 待决。
8. 是否允许双 header 接收：兼容期内允许，canonical 优先。
9. 双 header 值冲突：fail-closed 拒绝（HEADER_CONFLICT），按现有错误模型映射。
10. 防 downgrade/spoofing：canonical 优先 + 冲突 fail-closed + 不放宽校验 + value-based 签名基于 normalized 值 + header 不覆盖权威 tenant/body。
11. 分批实施：Batch 1 常量/parser -> 2 canonical -> 3 legacy 兼容 -> 4 冲突 fail-closed -> 5 docs/fixtures -> 6 移除（独立 review 后）。
12. 测试：见 §6。
13. 是否允许进入 implementation：本轮不进入；须先 DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW 通过。
14. 是否仍禁止 Integration-1 runtime：是，Integration-1 NOT STARTED；header 对齐通过也不等于允许 runtime integration。
```

---

## 8. 边界与准入

边界确认：未修改 Java；未修改测试；未新增 API；未新增 migration；未做真实 HTTP；未做真实 NQ 调用；未新增 RealClient；未新增真实 Provider；未做真实交易所调用；未接 AI；未开启 LIVE；未启动 Integration-1；未读取或输出真实密钥。

Integration-1 decision：**Integration-1 仍 NOT STARTED**。Header alignment plan 通过也不代表允许 runtime integration；DH NOT INTEGRATED / Runtime integration NOT STARTED / header alignment NOT STARTED（本轮仅 PLAN）。

Next concrete action：`DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW`；通过后 `DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1`（集中 header 常量 + parser + normalized model）。
