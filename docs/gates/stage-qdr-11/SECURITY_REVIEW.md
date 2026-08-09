# Stage-QDR-11 Security Review
## Sealed scan
- Scan ID：`c911d3a1-e60a-4284-a18f-ebbe6db52018`
- Status：`complete`
- Coverage：`complete`
- Reportable findings：`0`
- Active P0/P1：`0/0`
- Unauthorized bypass：`0`
- Snapshot：`codex-security-snapshot/v1:sha256:3c499b40232176c7a74b40ea0b0c7a0f6ebbe89283eac6fe548aca093c30eb3f`
## Committed-tree binding
Scan 在 baseline `9102c2f` 的 working tree 上完成。按 Codex Security v1 snapshot 算法重建：
- tracked modifications：15。
- untracked additions：4。
- 重建 digest：`3c499b40232176c7a74b40ea0b0c7a0f6ebbe89283eac6fe548aca093c30eb3f`。
- committed tree：`9b50fc7f51ebad3257aa77d1c2874d71385c81e9`。
- 9 个 technical full-file SHA-256：全部匹配。
- 4 个 sealed canonical artifact hashes：全部匹配。
结论：scan candidate tree 与 implementation committed tree 内容精确一致，无需重复扫描。
## Receipts 与限制
- Technical receipts：9/9。
- Governance receipts：10/10。
- Internal facade 无 public/scheduled caller。
- Formal/production capacity 未评估。
- CodeRabbit：`NOT_EXECUTED / NETWORK_BLOCKED`，未伪造 PASS。
- Token usage：total 95486；input 5082911；cached input 5001216；output 13791；reasoning output 3179。
