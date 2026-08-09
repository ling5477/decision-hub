# PostgreSQL Test Evidence

- Exact-SHA CI：`31297296670 / PASS`
- Testcontainers job：`93204385212 / success`
- PostgreSQL：`17.10`
- Flyway：`V1-V15`
- Full regression：`1326 / 0 failures / 0 errors / 0 skipped`
- Stage-QDR-10 targeted PostgreSQL acceptance：`7/7 / 0/0/0`
- Mandatory execution assertions：`JdbcNonceReplayGuardPersistenceTest` 与 `PostgresContainerSmokeTest` 均实际执行且 skipped `0`。
- Quality job：`93204385242 / success / Checkstyle 0 / Spotless PASS`

该证据属于 implementation exact-SHA `d275b9e...`。close 与 cleanup commit 仍必须分别通过自己的 exact-SHA CI。
