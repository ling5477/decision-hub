# Stage-QDR-6 Validation Evidence

## Final close retry evidence

以下命令在 `DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW-RETRY` 中真实执行，不是 archive/tag 任务伪造的本轮重跑：

| Command / evidence | Result |
| --- | --- |
| `mvn -ntp -pl dh-usecase,dh-infra -am test` | PASS / 0 failures / 0 errors / 0 skipped |
| `mvn -ntp -pl dh-app -am test` | PASS / dh-app 106 tests / 0 skipped |
| `mvn -ntp test` | PASS / 1017 tests / 0 failures / 0 errors / 0 skipped |
| `mvn -ntp -Pquality validate` | PASS / reactor 19 of 19 |
| Checkstyle | PASS / 0 violations |
| Spotless | PASS |
| ArchitectureTest | PASS / 39 tests / 0 skipped |
| PostgreSQL/Testcontainers | PASS / Docker Desktop / postgres:17 / PostgreSQL 17.10 |
| Flyway | PASS / V1-V11 validated and applied |
| V10 PostgreSQL suite | PASS / 16 tests / 0 skipped |

## Test groups

```text
B1 contracts: 20 tests
B2 aggregation: 17 tests
snapshot/canonicalization: 27 tests
deterministic replay: 15 tests
B4 internal report: 16 tests
full repository: 1017 tests / 0 skipped
```

## Archive/tag task evidence

```text
mvn -ntp -Pquality validate: PASS / reactor 19 of 19
Checkstyle: PASS / 0 violations
Spotless: PASS
full Maven tests: NOT_RERUN / final close retry evidence referenced
PostgreSQL/Testcontainers: NOT_RERUN / final close retry evidence referenced
V1-V11 worktree diff: EMPTY
forbidden-scope diff: EMPTY
```

Mockito/ByteBuddy dynamic agent future-JDK warning 与 `mvnw.cmd` wrapper risk 均为既有 non-blocking tooling risk。系统 Maven `mvn` 是本阶段有效验证入口。
