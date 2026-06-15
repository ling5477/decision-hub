# DH-NQ Timestamp Format Alignment Plan

> 任务：DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT
> 类型：INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION
> 日期：2026-06-15
> 仓库：Decision Hub（DH）；配套 NQ 侧只读核查（本会话 NQ 仓库不可达，见 §2.5）
> 状态：**PLAN ACCEPTED（PLAN-REVIEW，ACCEPTED WITH NQ-SCOPED FOLLOW-UP）；T1 docs 收口 DONE（2026-06-15，待 T1-REVIEW）**；timestamp alignment 整体 **NOT COMPLETED**（T2 INT0 / T4 NQ companion 待办）；未改运行代码 / 测试；Integration-1 NOT STARTED
> 前置：DH-NQ header alignment = **CLOSED**（canonical `X-NQ-DH-*`）。本方案处理 header alignment 收尾时显式另列的 timestamp 格式分歧。

本文件**只做对齐方案设计**：timestamp 格式 canonical 化的实施分批进行（`DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-*`），本轮不实施。

---

## 1. 目标与背景

`X-NQ-DH-Timestamp` 的线缆格式在 DH 内部存在**三方分歧**：生产代码按 RFC3339 解析（`Instant.parse`），Integration-0 安全策略文档写「冻结为 epoch 毫秒」，而 DH 侧 Integration-0 contract test fixture 用 epoch **秒**。三者互不一致，是 Integration-1 真实联调前必须收口的契约前置项（与 header 命名对齐解耦，单独成线）。

目标：选定单一 canonical timestamp 线缆格式，使生产代码、契约文档、契约测试三方一致，并为 NQ 侧 companion 提供口径。**本轮仅规划。**

---

## 2. 当前 timestamp map（只读核查结论，2026-06-15）

### 2.1 DH 生产代码

| 位置 | 行为 |
| --- | --- |
| `dh-security/.../HmacNqFeedbackAuthenticator.java` `parseTimestamp` | `Instant.parse(value)` → **RFC3339 / ISO-8601 instant**；无法解析（如纯数字 epoch）→ `null` → 视为 `TIMESTAMP_EXPIRED`（401） |
| 同类 `isExpired` | `timestamp` 早于 `now - maxClockSkew` 或晚于 `now + maxClockSkew` 即过期；`maxClockSkew` 默认 `Duration.ofMinutes(5)`（= **±300s**） |
| 同类 `verifySignature` | 验签前把 timestamp **归一化**为 `Instant.parse(header).toString()` 再纳入签名原材料 → 实际签名值为 `Instant.toString()` 规范形（UTC、`Z` 结尾） |
| `dh-security/.../NqFeedbackAuthRequest.java` | `timestampHeader` 为 `String`（原始 header 文本）；`now` 为 `Instant` |
| `dh-api/.../NqFeedbackController.java` | 仅把 `nqHeaders.timestamp()`（canonical `X-NQ-DH-Timestamp` 原值）透传给 authenticator，不解析、不改写 |

**结论：DH 生产入站当前只接受 RFC3339 instant 字符串，且签名绑定的是其归一化 `Instant.toString()` 形；纯 epoch（秒或毫秒）数字会被 `Instant.parse` 拒绝 → 401。**

### 2.2 DH 文档

| 文件 | timestamp 格式描述 |
| --- | --- |
| `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` §2 | `X-NQ-DH-Timestamp 请求时间戳（冻结为 **epoch 毫秒**）` ← 与生产代码冲突 |
| `DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` §header / §规则 | 仅列 header 名 + 「Timestamp **±300 秒**窗口」；**未**钉死线缆格式（格式中立） |
| `DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md` | 仅「超 ±300s 窗口拒绝」用例；未钉格式 |
| `NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md` | 「时钟偏移内，建议默认 300 秒」；未钉格式 |
| `STAGE2/STAGE3_*_CONTRACT_PLAN.md` | body 字段（`occurredAt/fetchedAt/generatedAt` 等）= ISO-8601；为 body 字段，非本 header，但体现 ISO-8601 取向 |

### 2.3 DH 测试（Integration-0 contract test，test-only）

| 位置 | 行为 |
| --- | --- |
| `dh-domain/.../integration0/support/Int0Contract.java` | `TIMESTAMP_WINDOW_SECONDS = 300L` |
| `Int0RequestFactory.validHeaders` | `H_TIMESTAMP = Long.toString(nowEpochSeconds)` → **epoch 秒** |
| `Int0ContractValidator` | `Long.parseLong(H_TIMESTAMP)`；`Math.abs(now - ts) > 300` → `TIMESTAMP_OUT_OF_WINDOW`（401）；非数字 → `TIMESTAMP_INVALID`（401） |

**结论：DH INT0 contract test 用 epoch 秒（Long），与生产 RFC3339、与 SECURITY_POLICY epoch 毫秒均不一致。INT0 不经真实 controller，故三者解耦下各自测试都绿，掩盖了分歧。**

### 2.4 DH 三方分歧汇总

```text
DH 生产代码         RFC3339 / ISO-8601 instant（Instant.parse；签名绑定 Instant.toString() 规范形）
DH SECURITY_POLICY  epoch 毫秒（文本，与生产冲突）
DH INT0 fixture     epoch 秒（Long，与生产、与 SECURITY_POLICY 均不一致）
DH CONTRACT_FREEZE  仅 ±300s 窗口（格式中立）
```

### 2.5 NQ 侧（本会话不可达）

本会话 NQ 仓库（`E:\Project\nexus-quant`）**不可访问**（跨盘不可达）。无法直接读取 NQ `docs/current/NQ_DH_INTEGRATION0_*` 与 `backend/**/integration0/support/Int0Contract.java`。降级依据：DH 侧 `Int0Contract.java` 注释声明其口径「来源：DH 的 `CONTRACT_FREEZE / SECURITY_POLICY / CONTRACT_TEST_PLAN`」，且 header alignment 阶段已确认 NQ 无生产 header 处理代码、NQ INT0 fixture 仅为 canonical `X-NQ-DH-*`。**NQ 侧 timestamp 线缆格式需在 NQ-scoped 任务中跨仓核对**（见 §6、Finding P1-3）。可信度：中（基于 DH 镜像文档 + header alignment 阶段结论，未当面核对 NQ 仓库当前内容）。

---

## 3. Findings（P0/P1/P2/P3）

### P0
- 无。

### P1
- **P1-1 生产代码与 SECURITY_POLICY 冲突（Integration-1 阻断级契约缺陷）**：生产 `Instant.parse` 只接受 RFC3339，而 SECURITY_POLICY §2 把 `X-NQ-DH-Timestamp` 冻结为 **epoch 毫秒**。若未来真实 NQ 发送方按冻结文档发 epoch 毫秒，DH 生产会 `Instant.parse` 失败 → 401 `TIMESTAMP_EXPIRED`，**真实联调必然失败**。必须在 Integration-1 前对齐。
- **P1-2 INT0 contract test 与生产格式不一致**：INT0 用 epoch 秒，掩盖了 P1-1（INT0 不经真实 controller，故各自绿）。契约测试未真正覆盖「生产实际接受的 timestamp 格式」。
- **P1-3 NQ 侧未跨仓核对**：本会话 NQ 不可达，NQ 文档 / fixture 的 timestamp 线缆格式未当面确认，需 NQ-scoped 任务核对并对齐到同一 canonical 格式。

### P2
- **P2-1 epoch 秒/毫秒单位歧义**：epoch 数字格式天然有秒/毫秒混淆风险（INT0=秒、SECURITY_POLICY=毫秒已是实例）。数字格式还需额外文档约定单位与位数。
- **P2-2 RFC3339 子格式未钉死**：生产 `Instant.parse` 接受多种等价表示（带偏移、不同小数秒精度），但签名绑定的是归一化 `Instant.toString()`；若发送方 header 文本与 `Instant.parse(header).toString()` 不一致（如用 `+00:00` 而非 `Z`），会签名不匹配。canonical 需钉死「UTC、`Z` 结尾、`Instant.toString()` 规范形」。

### P3
- **P3-1 文档术语统一**：CONTRACT_FREEZE / TEST_PLAN / AUDIT_REPORT 只写 ±300s 未写格式，收口时应统一引用 canonical 格式定义，避免再次产生「窗口已定、格式未定」的歧义。

---

## 4. Recommended decision（canonical timestamp 格式）

**canonical = RFC3339 / ISO-8601 UTC instant，`Instant.toString()` 规范形（UTC、`Z` 结尾），例：`2026-06-15T12:34:56Z`。**

- 单位：无（不是 epoch 数字，规避秒/毫秒歧义）。
- 时区：仅 UTC，`Z` 结尾；不接受带数字偏移（`+08:00` 等）的写法（避免签名归一化漂移，见 P2-2）。
- 小数秒：可选；推荐秒级精度。验签以 `Instant.parse(header).toString()` 归一化形为准（与现状一致）。
- 窗口：保持 **±300s**（`maxClockSkew` 默认 5 分钟），语义不变。

### 4.1 选择依据
- **不改现有生产实现**：DH 生产已用 `Instant.parse` + 归一化 `Instant.toString()`。选 RFC3339 = 生产**零代码行为变更**，对齐工作收敛为「改文档 + 改 INT0 测试格式」。
- **跨语言兼容**：Java（`Instant.parse`）、Python（`datetime.fromisoformat` / `dateutil`）、JS（`Date`）均稳定解析 RFC3339 UTC。
- **可读性 / 审计**：日志、审计、人工排查直读时间，优于裸 epoch 数字。
- **签名稳定性**：HMAC 仍 **value-based**，不引入 header name；签名绑定归一化 instant 串，跨族混签面为零（header alignment 已 canonical-only）。
- **规避单位歧义**：直接消除当前 epoch 秒（INT0）vs epoch 毫秒（SECURITY_POLICY）的混淆根因。

### 4.2 若改为 epoch milliseconds（不推荐）的代价
需修改生产 `parseTimestamp`（从 `Instant.parse` 改为 `Long.parse` + 单位判定）+ 改签名归一化逻辑 + 全量 WebMvc 测试 + 明确「毫秒」单位与位数校验防秒/毫秒混淆 + 牺牲日志可读性。收益仅「数字略省字节」，不足以抵消改动生产 + 引入单位歧义的风险。**故不选 epoch。**

---

## 5. 不变量确认（本方案不触碰）

- **HMAC signatureMaterial：不改**。仍 `join("\n", source, timestamp, nonce, eventId, requestId, traceId, payload)`，value-based，不含 header name。canonical=RFC3339 即维持现状。
- **nonce replay / ±300s 窗口 / requestId / traceId：不受影响**。格式选择只影响 timestamp 解析与窗口比较的输入表示；replay key（`source+nonce+requestId`）、binding（tenant/request/trace）、payload 64KiB、rate limit key（source+tenant+route）全部不变。
- **canonical-only header / 401·403·409·413·429·202 语义：不变**。

---

## 6. Implementation batches（后续实施，本轮不实施）

> 每批均：不接真实 NQ / 不真实 HTTP / 不新增 RealClient / 不新增真实 Provider / 不启动 Integration-1 / 不开 LIVE。验收均含 `mvn test` + `mvn -Pquality validate` BUILD SUCCESS + INT0-T01..T15 16/16 未破坏。

### Batch T1 — docs 收口（doc-only）（**DONE，2026-06-15，待 T1-REVIEW**）
- 已做：`SECURITY_POLICY` §2 header 表 +「Timestamp & Replay」§ 把「epoch 毫秒」→ RFC3339 / ISO-8601 UTC `Z`（例 `2026-06-15T12:34:56Z`），并加现状/不变量诚实声明（生产 `Instant.parse`→`Instant.toString()` 归一化、HMAC value-based、header name 不入签、严格 UTC-Z 强制=可选 T3、INT0 epoch 秒待 T2、NQ 侧待 T4 且为 Integration-1 前置阻断）；`CONTRACT_FREEZE` §规则、`CONTRACT_TEST_PLAN`（T5 / INT0-T05）钉死 canonical 格式（窗口仍 ±300s）。
- 未改 Java / 测试 / NQ。验收（实跑）：`mvn test` + `mvn -Pquality validate` BUILD SUCCESS（回归，无代码改动）；docs 一致性核查通过。

### Batch T2 — INT0 contract test 对齐（test-only）
- 把 `Int0RequestFactory` / `Int0ContractValidator` / `Int0Contract` 的 timestamp 从 epoch 秒改为 RFC3339 UTC 字符串解析（窗口仍 ±300s，保持 `TIMESTAMP_INVALID` / `TIMESTAMP_OUT_OF_WINDOW` 语义）；INT0 16/16 不回归。
- 禁止：改生产；改 NQ。验收：INT0 全绿。

### Batch T3 —（可选，gated）生产 parseTimestamp 收紧
- 仅当需要时：把 `parseTimestamp` 收紧为「要求 UTC `Z` 规范形、拒绝带数字偏移」，并加显式 WebMvc 测试固化 canonical timestamp 形（P2-2）。属生产行为微调，需单独 review/gate；**不在本规划默认范围**，列为可选。
- 禁止：改签名 value 集；改窗口语义。

### Batch T4 — NQ 侧 companion（独立 NQ-scoped 任务）
- 跨仓核对 NQ `NQ_DH_INTEGRATION0_*` / NQ INT0 fixture timestamp 线缆格式，并对齐到同一 canonical RFC3339；**本轮不跨仓写 NQ**（保持仓库边界）。解决 P1-3。

> 顺序建议：T1 → T2 →（NQ）T4 →（可选）T3。T1/T2 即可消除 DH 内三方分歧并使「契约测试覆盖生产实际格式」。

---

## 7. 10 问速答

```text
1. DH 代码 X-NQ-DH-Timestamp 解析格式：RFC3339 / ISO-8601（Instant.parse）；签名绑定归一化 Instant.toString()（UTC Z）。
2. DH 文档/contract/security policy 是否一致：不一致。SECURITY_POLICY §2=epoch 毫秒（与生产冲突）；CONTRACT_FREEZE/TEST_PLAN=仅 ±300s 未钉格式。
3. NQ 文档/fixture/contract test 是否一致：本会话 NQ 不可达，未当面核对（P1-3）；需 NQ-scoped 任务跨仓确认。
4. 选 RFC3339 还是 epoch ms：选 RFC3339 / ISO-8601 UTC。
5. 选择依据：生产零改动 + 跨语言（Java/Py/JS）稳定解析 + 可读审计 + value-based 签名不漂移 + 规避秒/毫秒歧义。
6. 是否需要兼容期：否。NQ 无真实发送方；属契约澄清 + 测试/文档对齐，非运行期双格式接收（不做 dual-format parsing）。
7. 是否改 HMAC signatureMaterial：否。维持 value-based、不含 header name；canonical=RFC3339 即维持现状。
8. 是否影响 nonce replay / ±300s / requestId / traceId：不影响。仅 timestamp 解析输入表示；窗口、replay、binding 全保持。
9. 后续分批：T1 docs 收口 -> T2 INT0 测试对齐 ->（NQ）T4 companion ->（可选 gated）T3 生产 parseTimestamp 收紧。
10. 是否仍禁止 Integration-1 runtime：是。Integration-1 NOT STARTED；timestamp 对齐通过也不放开 runtime。
```

---

## 8. 边界与准入

边界确认（本 PLAN 轮，2026-06-15）：未修改 NQ 仓库；未修改 Java 生产代码；未修改测试；未新增 API；未新增 migration；未做真实 HTTP；未做真实 NQ 调用；未新增 RealClient；未新增真实 Provider；未做真实交易所调用；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 Maven wrapper / datasource 弱口令 / nonce-burn race；未读取或输出真实密钥。本轮仅新增本规划文档 + 更新状态文档。

Integration-1 decision：**Integration-1 仍 NOT STARTED**。timestamp 对齐推进也不代表允许 runtime integration；DH NOT INTEGRATED / Runtime integration NOT STARTED / LIVE DISABLED。timestamp format alignment **整体 NOT COMPLETED**：PLAN ACCEPTED；**T1 docs 收口 DONE（2026-06-15）**；T2 INT0 对齐 / T4 NQ companion（Integration-1 前置阻断）/（可选）T3 生产收紧 待办。

Next concrete action：`DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1-REVIEW`；通过后 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2`（INT0 测试由 epoch 秒改 RFC3339）。
