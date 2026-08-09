# PostgreSQL Test Evidence
- Testcontainers image：`postgres:17`
- Runtime version：`PostgreSQL 17.10`
- Flyway：fresh database 完整应用 `V1-V15`
- Exact-SHA CI：run `31312337732`，job `93241931499 / SUCCESS`
- Mandatory Testcontainers skip assertion：`SUCCESS`
- Full reactor：`1338 tests / 0 failures / 0 errors / 0 skipped`
Stage-QDR-11 PostgreSQL 测试覆盖 persisted complete、partial/not-found、overflow/environment mismatch、cross-tenant 与 zero-write。测试中的 nested decision aggregate 使用 mock；production exact RUN comparison 与 RUN mismatch→INVALID 单测独立关闭该证明边界。
