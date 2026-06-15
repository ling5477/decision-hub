# DH-NQ Header Alignment Plan

> 任务：DH-NQ-HEADER-ALIGNMENT-PLAN
> 类型：INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION
> 日期：2026-06-14
> 仓库：Decision Hub（DH）；配套 NQ 侧只读核查
> 状态：**planning-only**（本轮只读核查 + 输出方案；未改运行代码 / 测试；未启动 Integration-1）
> 配套：`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` / `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`

本文件**只做对齐方案设计**：canonical 化的实施分批进行（`DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-*`）。

> **实施进展**
> - PLAN：ACCEPTED（DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW，2026-06-14）。**已定策略：canonical-only（无兼容期、无 legacy/canonical 双接收）**；
>   Tenant/Request/Trace 保权威来源、header 仅一致性校验不覆盖；timestamp 格式另列 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（不在本对齐链内）。
> - **Batch 1：DONE / ACCEPTED（2026-06-14，BATCH-1-REVIEW 通过）** —— 内部结构 skeleton（集中 header 常量 `NqDhHeaderNames` + 归一化模型
>   `NormalizedNqDhHeaders` + `NqDhHeaderParser`（legacy-only 读取）+ `NqDhHeaderValidator` skeleton + `NqDhHeaderValidationResult`）；
>   controller 去 magic string、经 parser 读取，**对外行为不变**（仍 legacy `X-DH-NQ-*`）。canonical 读取与强制留待 Batch 2+。
> - **DOC-RECONCILE（2026-06-15）**：本文件 §4–§7 已统一为 canonical-only。初版 PLAN 早期的"短兼容期 / 双 header 接收 / 冲突 fail-closed / 移除 legacy 独立批次"等措辞为 PLAN-REVIEW 改判前的待决推荐，已作废，仅保留为历史脉络说明。
> - **Batch 2：DONE / ACCEPTED（2026-06-15，BATCH-2-REVIEW 通过）** —— 生产入站切到 **canonical-only**：`NqDhHeaderParser.parseCanonical` + controller 改用 canonical `X-NQ-DH-*`，**不再读取 legacy、不做双接收**；仅 legacy / 缺必需 canonical header 由现有 HMAC authenticator fail-closed 拒绝（Source→403 / Timestamp·Nonce·Signature→401），状态码语义不变；HMAC 仍 value-based。
> - **生产入站现为 canonical-only**：仅接受 canonical `X-NQ-DH-*`；legacy `X-DH-NQ-*` 不再被接受（等同缺失 -> fail-closed）。legacy 常量 / `parseLegacy` 仅作历史引用保留。
> - **Batch 3：DONE（2026-06-15，待 BATCH-3-REVIEW）** —— 正式接入 `NqDhHeaderValidator`：canonical `X-NQ-DH-Tenant-Id/Request-Id/Trace-Id` 与权威来源（tenant=认证上下文，requestId/traceId=body）binding 一致性校验；header 可选、若提供且不一致则 fail-closed（403 `HEADER_BINDING_MISMATCH`）；**header 绝不覆盖权威来源**。
> - **header alignment 整体仍 NOT COMPLETED**：canonical-only 读取（Batch 2）+ Tenant/Request/Trace binding 一致性（Batch 3）已落地；仅余 docs/fixtures 收口（Batch 4）；Integration-1 仍 NOT STARTED。下一步 Batch 4（先经 BATCH-3-REVIEW）。

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

### 4.1 总体方针：canonical-only（无兼容期）

**PLAN-REVIEW 已定：一次性切到 canonical-only，不保留 legacy 兼容期、不做 legacy/canonical 双接收。**

- DH NQ feedback 当前仍是 contract/test 保护下的入站骨架，**NQ 侧无真实 legacy 发送方**，因此切 canonical-only 不会硬断任何真实在用发送方；同时避免双 header 解析带来的混签 / downgrade 攻击面，实现也更简单。
- 对齐仅限**入站读取**；DH 自身不对外发 NQ-DH header（无出站方向需要对齐）。
- legacy 常量（`NqDhHeaderNames.LEGACY_*`）仅作为历史引用保留，不再继续扩散；Batch 2 切换后生产不再读取 legacy。

> 历史脉络：初版 PLAN 曾推荐"短兼容期 + 双 header 接收 + 冲突 fail-closed + 后续单独移除 legacy"。PLAN-REVIEW 已改判 canonical-only（无兼容期），该初版推荐作废；本节及 §4.2–§4.7、§5–§7 均按 canonical-only 重述。

### 4.2 是否需要兼容期 / 是否允许双 header 接收

- **不需要兼容期，不允许双 header 接收**：canonical-only 切换后，入站只接受 canonical `X-NQ-DH-*`，不再解析 legacy `X-DH-NQ-*`。
- 因此不存在 legacy 命中审计码、不存在双 header 冲突分支、也不存在"独立移除 legacy 批次"——切换即完成对齐。

### 4.3 Header 接收策略（canonical-only，fail-closed）

| 情形 | 处理 |
| --- | --- |
| 提供 canonical（`X-NQ-DH-*`） | **目标状态**，正常处理 |
| 缺必需 canonical header | **拒绝**，记 `MISSING_CANONICAL_HEADER`；沿用现有缺失语义（Source/Signature 缺失 -> 现有 401/403 路径） |
| 仅提供 legacy（`X-DH-NQ-*`） | canonical-only 下**等同于缺失 canonical**：拒绝，记 `MISSING_CANONICAL_HEADER`（不再解析 legacy，无 downgrade 通道） |

原则：只认 canonical，缺失一律 fail-closed；不解析 legacy 即无双 header 冲突、无 downgrade / 混签面。`LEGACY_HEADER_USED` / `HEADER_CONFLICT` 在 canonical-only 下不适用，不引入。

### 4.4 Signature canonicalization（签名策略）

只读核查结论：`HmacNqFeedbackAuthenticator.signatureMaterial()` 的签名原材料是 **value-based，不含 header name**：

```text
signatureMaterial = join("\n",
  normalize(sourceSystem), timestampValue, nonceValue,
  eventId, requestId, traceId, payloadJson)
```

由此：

- **签名不随 header 改名漂移**（签的是值，不是 header 名）。这是本次对齐安全性的关键支点。
- **防混签**：先把 canonical header 解析进**单一 normalized 逻辑模型**（每个逻辑字段唯一值），**再**基于该 normalized 值构建签名原材料并验签。canonical-only 下只有单一 header 族，不存在跨族混取。
- 需要 **normalized header model**：`parse(headers) -> NormalizedNqDhHeaders`（含 source/tenant/requestId/traceId/timestamp/nonce/signature 的解析值 + family；canonical-only 下 family=CANONICAL）。
- 明确签名 payload（保持现状字段集）：source、timestamp、nonce、eventId、requestId、traceId、body/payload。**不引入 header name 进签名**（保持 value-based，改名不漂移）。
- **不记录签名原材料 / secret 到日志**（沿用现状）。

### 4.5 Tenant-Id / Request-Id / Trace-Id 处理（P1-2 的方案建议）

推荐：**保持现有来源为权威**（tenant=auth-context，requestId/traceId=body），canonical header 仅作"可选附加"，若提供则与权威来源做一致性校验：

```text
X-NQ-DH-Tenant-Id  若提供 -> 必须等于 authenticated tenant，否则 fail-closed（沿用 tenant binding 403 语义）
X-NQ-DH-Request-Id 若提供 -> 必须等于 body.requestId，否则 fail-closed
X-NQ-DH-Trace-Id   若提供 -> 必须等于 body.traceId，否则 fail-closed（或以 header 为 trace 传播源，IMPL-REVIEW 定）
```

理由：不弱化"tenant 来自已认证主体"的安全基线；不把 body 既有字段降级为可被 header 覆盖。该一致性校验于 **Batch 3** 落地（不一致 -> fail-closed / `HEADER_BINDING_MISMATCH`）。

### 4.6 Audit and logs

审计码（canonical-only；只记类型 + 安全字段，不记 raw signature/secret/full body/credential）：

```text
MISSING_CANONICAL_HEADER  缺必需 canonical header（含"仅提供 legacy"）-> 拒绝（Batch 2）
HEADER_BINDING_MISMATCH   canonical Tenant/Request/Trace 与权威来源（auth-context/body）不一致 -> fail-closed（Batch 3）
SIGNATURE_MISMATCH        验签失败（沿用现有 BAD_SIGNATURE，不新增泄露）
```

canonical-only 下不引入 `LEGACY_HEADER_USED` / `HEADER_CONFLICT`（无 legacy 解析、无双 header 冲突）。上述 `MISSING_CANONICAL_HEADER` / `HEADER_BINDING_MISMATCH` 已与 Batch 1 skeleton 的 `NqDhHeaderValidationResult` 审计码常量一致。审计字段：source、tenantId、route、traceId、requestId、family（CANONICAL）、auditCode、错误类型。**禁止**：raw signature、raw secret、full body、payload 明文、credential。

### 4.7 如何避免 downgrade / spoofing

- canonical-only：不解析 legacy，攻击者无 legacy 通道可用于 downgrade / 覆盖 canonical。
- 切换不放宽任何校验（source allowlist / timestamp 窗口 / nonce replay / payload 64KiB / signature 全部保留）。
- 签名 value-based 且基于 normalized 值（family=CANONICAL），杜绝跨族混签绕过。
- Tenant-Id/Request-Id/Trace-Id 的 header 不得覆盖权威来源（§4.5 / Batch 3），防止 header 注入提权 / 串租户。
- 缺必需 canonical header 一律 fail-closed（`MISSING_CANONICAL_HEADER`），便于监控异常。

---

## 5. Implementation batches（canonical-only，后续实施批次）

> 每批均：不接真实 NQ / 不真实 HTTP / 不新增 RealClient / 不新增真实 Provider / 不启动 Integration-1 / 不开 LIVE。验收均含 `mvn test` + `mvn -Pquality validate` BUILD SUCCESS + INT0-T01..T15 16/16 未破坏。

### Batch 1 — 集中 header 常量 + parser + normalized model + validator skeleton（**DONE / ACCEPTED，2026-06-14**）
- 已做：`dh-security/.../security/nq/**` 新增 `NqDhHeaderNames`（canonical 7 + legacy 7 常量）/ `NormalizedNqDhHeaders`（归一化模型，toString 脱敏 signature）/ `NqDhHeaderParser`（仅 `parseLegacy`）/ `NqDhHeaderValidator`（pass-through skeleton）/ `NqDhHeaderValidationResult`（含 `MISSING_CANONICAL_HEADER` / `HEADER_BINDING_MISMATCH` 审计码）；`NqFeedbackController` 去 magic string、经 `parseLegacy` 读取。
- 不改对外行为：仍读 legacy `X-DH-NQ-*`，签名语义不变，validator 未接入 controller，未改 NQ。
- 验收：新增 常量/parser/validator 单测 7；既有 WebMvc 15 / rate limit 3 / authenticator 全绿；INT0 16/16 未破坏（BATCH-1-REVIEW 通过）。

### Batch 2 — canonical-only 读取（**DONE，2026-06-15，待 BATCH-2-REVIEW**）
- 已做：`NqDhHeaderParser.parseCanonical`（读 canonical 7 header，family=CANONICAL）；`NqFeedbackController` 从 `parseLegacy` 切到 `parseCanonical`，**不再读取 legacy、不做双接收**。`parseLegacy` 仅保留为历史引用 / 单测。
- 缺失语义：缺必需 canonical header（含仅提供 legacy）由现有 HMAC authenticator **fail-closed** 拒绝 —— canonical Source 缺失/不匹配 -> 403 `SOURCE_NOT_ALLOWED`；Timestamp -> 401 `TIMESTAMP_EXPIRED`；Nonce -> 401 `REPLAY_KEY_MISSING`；Signature -> 401 `BAD_SIGNATURE`。**状态码语义不变**；`NqDhHeaderValidator` 本批仍为未接入 skeleton（显式 `MISSING_CANONICAL_HEADER` / binding 强制随 Batch 3 一并接入）。
- 权威来源不变：tenant=认证上下文，requestId/traceId=body；canonical Tenant/Request/Trace 进入模型但不参与认证、不覆盖权威来源。HMAC signatureMaterial 仍 value-based（不含 header name）。
- 验收（实跑）：WebMvc 20（canonical 成功路径 + legacy-only 拒绝 + 缺 source/timestamp/nonce + 签名不泄露）、rate limit 3、parser 5；INT0 16/16 未回归；payload 64KiB / nonce replay / rate limit key 全绿。`mvn test` + `mvn -Pquality validate` BUILD SUCCESS（0 Checkstyle / spotless 通过）。

### Batch 3 — Tenant / Request / Trace binding 一致性校验（**DONE，2026-06-15，待 BATCH-3-REVIEW**）
- 已做：`NqDhHeaderValidator.validate(headers, authTenant, bodyRequestId, bodyTraceId)` 实现 binding 一致性（header 可选；若提供则必须等于权威来源），并由 `NqFeedbackController` 在 HMAC 认证成功后、入库前正式接入；任一不一致 -> **fail-closed** 403 / `HEADER_BINDING_MISMATCH`。**header 绝不覆盖权威来源**（tenant=认证上下文，requestId/traceId=body）。
- 安全：mismatch 响应仅含 error / errorCode(`HEADER_BINDING_MISMATCH`) / message / eventId / traceId / correlationId，不回显 header 原值 / signature / secret / full body；validator `reason` 仅含字段名不含具体值；审计日志仅记 auditCode + 安全字段。
- 保持：HMAC value-based、rate limit key=source+tenant+route、payload 64KiB、nonce replay；缺 source/timestamp/nonce/signature 仍由 authenticator fail-closed（403/401）。
- 验收（实跑）：WebMvc 25（binding 全一致 202 + tenant/request/trace mismatch 各 403 + mismatch 不泄露）、validator 单测 6、parser 5、rate limit 3；INT0 16/16 未回归；`mvn test` + `mvn -Pquality validate` BUILD SUCCESS。

### Batch 4 — docs / fixtures / WebMvc 测试切 canonical 收口
- 允许：DH WebMvc 测试 fixtures、docs（contract / test plan / README）统一到 canonical 表述；INT0 fixture 已是 canonical，核对一致。
- 禁止：改 NQ 生产；改签名 value 集；新增 API / migration。
- 验收：docs/fixtures 一致性核查；测试全绿。

### legacy 常量退场（可选后续小批，非阻塞）
- canonical-only 下 Batch 2 切换后生产已不读 legacy；`NqDhHeaderNames.LEGACY_*` 与 `parseLegacy` 仅作历史引用保留。
- 当确认无任何引用后，可单独小批移除 legacy 常量 / `parseLegacy` + 相关测试；不改对外行为。**不作为 header alignment 完成的前置条件。**

### NQ 侧（独立 NQ-scoped 任务）
- NQ 当前无生产 header 处理，仅 fixture/docs 已是 canonical。建议由 NQ 仓库自身任务补 `docs/current/NQ_DH_HEADER_ALIGNMENT_PLAN.md` companion；**本轮不跨仓写 NQ**（保持 DH/NQ 仓库边界，详见输出说明）。

---

## 6. Tests to add / update（规划，本轮不写）

```text
accepts_canonical_headers                                  Batch 2
rejects_missing_canonical_header                           Batch 2（缺必需 canonical -> MISSING_CANONICAL_HEADER）
rejects_legacy_only_headers                                Batch 2（仅 legacy 等同缺失 canonical -> 拒绝）
rejects_header_binding_mismatch                            Batch 3（Tenant/Request/Trace 不一致 -> HEADER_BINDING_MISMATCH）
does_not_log_signature_raw_material                        Batch 1+（贯穿；Batch 1 已固化 model toString 脱敏）
keeps_payload_64kib_gate                                   每批回归（NqFeedbackPayloadSizeGateTest）
keeps_nonce_replay_protection                              每批回归
keeps_rate_limit_key_source_tenant_route                   每批回归（限流前置不受 header 改名影响）
keeps_tenant_binding                                       Batch 3（§4.5）
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
7. 是否需要兼容期：否。PLAN-REVIEW 已定 canonical-only，无兼容期。
8. 是否允许双 header 接收：否。canonical-only 只接收 canonical。
9. 双 header 值冲突：不适用（不接收 legacy，无双 header 冲突）；缺 canonical -> MISSING_CANONICAL_HEADER。
10. 防 downgrade/spoofing：canonical-only（无 legacy 通道）+ 不放宽校验 + value-based 签名基于 normalized 值 + header 不覆盖权威 tenant/body。
11. 分批实施：Batch 1 常量/parser/skeleton（DONE）-> 2 canonical-only 读取 -> 3 Tenant/Request/Trace binding 一致性校验 -> 4 docs/fixtures 收口；legacy 常量退场为可选后续小批。
12. 测试：见 §6。
13. 是否允许进入 implementation：Batch 1 已 DONE / ACCEPTED；本轮 DOC-RECONCILE 后下一步为 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（canonical-only 读取）。
14. 是否仍禁止 Integration-1 runtime：是，Integration-1 NOT STARTED；header 对齐推进也不等于允许 runtime integration。
```

---

## 8. 边界与准入

边界确认（PLAN 轮，2026-06-14）：未修改 Java；未修改测试；未新增 API；未新增 migration；未做真实 HTTP；未做真实 NQ 调用；未新增 RealClient；未新增真实 Provider；未做真实交易所调用；未接 AI；未开启 LIVE；未启动 Integration-1；未读取或输出真实密钥。（Batch 1 IMPL 轮、DOC-RECONCILE 轮的边界分别见 WORKLOG / TESTING 对应条目；DOC-RECONCILE 轮仅改文档。）

Integration-1 decision：**Integration-1 仍 NOT STARTED**。Header alignment 推进也不代表允许 runtime integration；DH NOT INTEGRATED / Runtime integration NOT STARTED。**header alignment 整体 NOT COMPLETED**（Batch 1 DONE/ACCEPTED；Batch 2 DONE/ACCEPTED：生产入站已切 canonical-only `X-NQ-DH-*`、不再接受 legacy；Batch 3 DONE：Tenant/Request/Trace binding 一致性校验已接入（fail-closed `HEADER_BINDING_MISMATCH`，header 不覆盖权威来源）；仅余 docs/fixtures 收口（Batch 4）尚待）。

Next concrete action：`DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3-REVIEW`；通过后 `DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4`（docs/fixtures 收口）。
