# Security Review

## Verdict

`PASS / SEALED / EXACT_COMMITTED_TREE`

## Authoritative scan

- Scan：`7a89a6be-98aa-41b8-bcc2-ab0c036f1682`
- Base / head：`87304e334787d778b10ebad1b1b7f17057f47322..d275b9e30bb381bea8467286786add2c5b43e119`
- Candidate tree：`aee96dcbba36020c8c92ea48f455d2be90216a7c`
- Snapshot：`codex-security-snapshot/v1:sha256:1d74df0bc5da366ec7aad16a4841552de3d91d1cb5319d4e849096130ccb54eb`
- Coverage / receipts：complete / `28/28`
- Deferred：`0`
- Reportable findings：`0`
- Active P0/P1：`0/0`
- Seal：complete
- Manifest artifact hash failures：`0`

Manifest 中 `findings.json` 与 `coverage.json` 的 SHA-256 已重新计算并精确匹配。当前 `HEAD^{tree}` 与 scan report 记录的 candidate tree 相同，因此未重跑 Codex Security。

旧失败 scan 不是 authority；详见 [SECURITY_SCAN_HISTORY.md](SECURITY_SCAN_HISTORY.md)。完整 sealed bundle 复制在 `security/`。
