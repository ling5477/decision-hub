# Stage-QDR-9 Batch Summary

| Batch | 最终状态 | 核心范围 | 接受证据 |
|---|---|---|---|
| B1 | CLOSED / ACCEPTED / PUBLISHED | V15 schema、scope erratum、milestone review | 1208 tests；PostgreSQL 17.10 |
| B2 | CLOSED / ACCEPTED / PUBLISHED | JDBC persistence、事务与幂等 | CI 30187110533；1220 tests |
| B3 | CLOSED / ACCEPTED / PUBLISHED | internal-only/read-only historical evidence read model | CI 30190532421；1228 tests |
| B4 | CLOSED / ACCEPTED / PUBLISHED | owner-attested minimal environment remediation、authority rebaseline/final close | technical SHA `c7f940c0c48900a0cfb7eac86aac745c8006629c`；CI 30633947829 |
| B5 | FINAL CLOSE / GOVERNANCE ONLY | factsources、archive、manifest、CI、tag、post-tag cleanup | 技术实现 `NONE` |

Active P0/P1 为 `0/0`。B4 历史 containment、superseded designs 与 deferred capability 原件保留在
`source-documents/`，但不重新成为 active requirement 或 implementation authorization。
