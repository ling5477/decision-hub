# DH-NQ Timestamp Format Alignment

> 任务：DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT
> 类型：CONTRACT_FINALIZATION + SECURITY_REVIEW + DOCUMENTATION
> 日期：2026-06-15；最终收口：2026-06-28
> 仓库：Decision Hub（DH）；配套 NexusQuant（NQ）T4 companion
> 状态：**CLOSED / ACCEPTED**。T1 docs、T2 DH INT0、T3 DH production / INT0 UTC-Z-only hardening、T4 NQ companion 均已 ACCEPTED。
> 前置：DH-NQ header alignment = **CLOSED**；DH production 入站 canonical-only `X-NQ-DH-*`。

本文件记录 `X-NQ-DH-Timestamp` 线缆格式最终收口结论。timestamp alignment CLOSED 仅表示 timestamp contract alignment 完成；**不授权 Integration-1 runtime、不表示 DH integrated、不启用 LIVE、不允许真实 HTTP / RealClient / real provider**。

---

## 1. Final Canonical Contract

```text
Header: X-NQ-DH-Timestamp
Canonical format: RFC3339 / ISO-8601 UTC Z
Example: 2026-06-15T12:34:56Z
Replay window: ±300s
Rejected formats:
  - epoch seconds
  - epoch milliseconds
  - numeric timezone offset, for example 2026-06-15T20:34:56+08:00
```

不变量：

- HMAC signatureMaterial 仍为 value-based，不包含 header name。
- DH production 验签仍使用 `timestamp.toString()` 归一化 UTC `Z` 值。
- nonce / source / tenant / requestId / traceId / payload 语义不变。
- timestamp CLOSED 不改变 Integration-1、runtime integration、DH integrated、LIVE 状态。

---

## 2. Final State Map

### 2.1 DH Production

| 位置 | 当前行为 |
| --- | --- |
| `dh-security/.../HmacNqFeedbackAuthenticator.java` `parseTimestamp` | 先要求 `timestampHeader.endsWith("Z")`，再 `Instant.parse`；epoch seconds、epoch milliseconds、数字时区偏移均 fail-closed |
| `isExpired` | 保持 `maxClockSkew = Duration.ofMinutes(5)`，即 ±300s |
| `verifySignature` | 验签前把 timestamp 归一化为 `Instant.toString()` UTC `Z` 值；signatureMaterial 字段集合不变，header name 不入签 |

### 2.2 DH INT0

| 位置 | 当前行为 |
| --- | --- |
| `Int0RequestFactory.validHeaders` | 生成 RFC3339 / ISO-8601 UTC `Z` timestamp |
| `Int0ContractValidator` | 先要求 header value 非空且以 `Z` 结尾，再 `Instant.parse(...).getEpochSecond()`；非法格式返回 `TIMESTAMP_INVALID`（401） |
| `DhNqIntegration0SecurityContractTest` | INT0-T05 覆盖 UTC `Z` accept、epoch seconds reject、epoch milliseconds reject、数字时区偏移 reject、±300s out-of-window reject |

### 2.3 NQ INT0 / Docs

| 位置 | 当前行为 |
| --- | --- |
| NQ `NQ_DH_INTEGRATION0_*` docs | 均固定 canonical timestamp 为 RFC3339 / ISO-8601 UTC `Z`，拒绝 epoch seconds / epoch milliseconds / 数字时区偏移，窗口 ±300s |
| NQ INT0 test/support | T4 companion 已 ACCEPTED；NQ INT0 生成/校验 UTC `Z`，拒绝 epoch seconds、epoch milliseconds、数字时区偏移 |
| NQ production runtime timestamp handling | **NOT PRESENT / NOT STARTED**；NQ 侧本轮只完成 contract / INT0 companion，不代表 runtime integration |

---

## 3. Closed Findings

### P0

- 无。

### P1

- **P1-1 production vs SECURITY_POLICY timestamp format conflict：CLOSED**。DH docs 已从 epoch milliseconds 收口到 RFC3339 UTC `Z`。
- **P1-2 DH INT0 epoch seconds drift：CLOSED**。DH INT0 已使用 RFC3339 UTC `Z` 并拒绝 epoch seconds / milliseconds。
- **P1-3 NQ companion missing：CLOSED**。NQ T4 companion 已 ACCEPTED，NQ docs 与 INT0 test/support 已对齐到同一 canonical contract。

### P2

- **P2-1 epoch seconds / milliseconds ambiguity：CLOSED**。最终 contract 不接受 epoch 数字格式。
- **P2-2 numeric timezone offset ambiguity：CLOSED**。DH production、DH INT0、NQ INT0 均拒绝 `+08:00` 等数字时区偏移。

### P3

- 文档术语统一完成；history entries 如保留旧批次事实，必须按历史语境解读，不作为当前 next task。

---

## 4. Batch Decisions

| Batch | Scope | Final state |
| --- | --- | --- |
| T1 | DH docs timestamp contract alignment | ACCEPTED |
| T2 | DH INT0 timestamp fixture / validator alignment | ACCEPTED |
| T3 | DH production / DH INT0 UTC-Z-only hardening | ACCEPTED |
| T4 | NQ timestamp companion | ACCEPTED |
| Finalize | DH/NQ docs current-state close + cross-repo regression | CLOSED / ACCEPTED |

---

## 5. 10 问速答

```text
1. DH production timestamp format：RFC3339 / ISO-8601 UTC Z；parseTimestamp = endsWith("Z") + Instant.parse。
2. DH docs / contract / security policy 是否一致：一致，均为 RFC3339 UTC Z。
3. NQ docs / fixture / contract test 是否一致：一致，T4 companion 已 ACCEPTED。
4. 是否接受 epoch seconds：否。
5. 是否接受 epoch milliseconds：否。
6. 是否接受数字时区偏移：否，例如 +08:00 必须拒绝。
7. 是否改 HMAC signatureMaterial：否。
8. 是否影响 nonce replay / ±300s / requestId / traceId：否。
9. timestamp alignment overall：CLOSED / ACCEPTED。
10. 是否仍禁止 Integration-1 runtime：是。Integration-1 NOT STARTED；Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。
```

---

## 6. Boundary

本收口不修改 DH/NQ Java production code，不修改 DH/NQ test code，不新增 API，不新增 migration，不新增 RealClient，不新增 real provider，不真实 HTTP，不真实 DH/NQ 调用，不调用真实交易所，不读取凭证，不启动 Integration-1，不开启 LIVE，不处理 Maven wrapper / datasource 默认弱口令 / nonce-burn race。

Timestamp CLOSED 后的下一步只能是 `GateK-PLAN` 或 DH 代码真实审查；任何 Integration-1 runtime 必须另起独立 PLAN、安全审查与人工授权。
