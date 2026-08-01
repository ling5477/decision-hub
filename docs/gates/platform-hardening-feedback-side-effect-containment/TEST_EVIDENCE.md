# Test Evidence

## Local accepted regression

```text
Reactor: 19 / 19
Tests: 1252
Failures: 0
Errors: 0
Skipped: 0
PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10
Flyway: V1-V15
Checkstyle: 0
Spotless: PASS
```

## Implementation exact-SHA CI

- Run：[`30701063741`](https://github.com/ling5477/decision-hub/actions/runs/30701063741)
- Head SHA：`fddf3558255b5a4f8f6071da42363649942afe46`
- Quality job：`91372040214 / success`
- build & test (Testcontainers / Docker)：`91372040226 / success`
- Testcontainers mandatory execution assertion：`success`

该证据证明回归与 quality gate，不等于 formal capacity 或 production readiness。
