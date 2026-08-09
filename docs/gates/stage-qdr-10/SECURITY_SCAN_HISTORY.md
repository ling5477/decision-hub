# Security Scan History

## Historical context

- Original final-close scan：`69bf31bc-2e61-4853-b343-902b28c28d91 / 2 Low-P3 / BLOCKED`。
- Intermediate working-tree evidence：`87304e3_worktree_20260809T111514 / HISTORICAL ONLY`；最终 committed-range scan 说明其只匹配 28 个文件中的 18 个，不能作为最终 authority。

## Scan A — failed finalization

```text
scan: e17f3cfd-852f-4eb2-b5cf-b815dd749515
result: FINALIZATION_FAILED
reason: scan.target.snapshotDigest missing
authority: NO
reused: NO
classification: HISTORICAL_FINALIZATION_FAILURE
```

该 scan 的 progress 显示 28/28 review items closed，但 completion 因 snapshotDigest 为空而失败，未生成 completed sealed artifacts，因此不得复用。

## Scan B — final authority

```text
scan: 7a89a6be-98aa-41b8-bcc2-ab0c036f1682
result: PASS / SEALED
coverage: 28/28
completion receipts: 28/28
deferred: 0
reportable findings: 0
active P0/P1: 0/0
snapshot: codex-security-snapshot/v1:sha256:1d74df0bc5da366ec7aad16a4841552de3d91d1cb5319d4e849096130ccb54eb
candidate tree: aee96dcbba36020c8c92ea48f455d2be90216a7c
authority: YES
```

`security/artifacts/02_discovery/work_ledger.jsonl` 包含 28 个唯一 path、28 个 `completed` receipts；canonical manifest/findings/coverage/report 与支持性 artifacts 均已归档。
