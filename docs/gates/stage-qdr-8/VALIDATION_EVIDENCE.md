# Stage-QDR-8 Validation Evidence

## Exact-SHA CI

```text
run: 29836489131
head SHA: 1279f1a0a246807e019bd2223c0f7254d50b74d5
test job: PASS
quality job: PASS
Reactor: 19 / 19 SUCCESS
tests: 1189
failures: 0
errors: 0
skipped: 0
PostgreSQL/Testcontainers: REAL EXECUTION
PostgreSQL: 17.10
mandatory skips: 0
ArchitectureTest: 40 / 40 PASS
Checkstyle: 0
Spotless: PASS
```

完整日志通过 `gh run view 29836489131 --log` 读取；模块汇总共 7 行，总计 1189 tests。19 个 Reactor module 均为 success。

## 本轮复用与本地验证

```text
Full regression: NOT_RERUN
Reused exact-SHA CI evidence: 1189 / 0 failures / 0 errors / 0 skipped
PostgreSQL/Testcontainers: NOT_RERUN
Reused exact-SHA CI evidence: PostgreSQL 17.10 / mandatory skips 0
Local final quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
```

## Scope validation

```text
changed files: 49
domain production: 14
use-case production: 13
test files: 6（含 ArchitectureTest）
current factsources: 16
unexpected files: 0
forbidden implementation path diff: 0
scope invariants: PASS / 3 OF 3
```
