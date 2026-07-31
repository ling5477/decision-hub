# Stage-QDR-9 CI Evidence Reference

| 用途 | Run | Head SHA | Jobs | 结论 | URL |
|---|---:|---|---|---|---|
| accepted technical baseline | 30633947829 | `c7f940c0c48900a0cfb7eac86aac745c8006629c` | Quality；build & test (Testcontainers / Docker) | PASS | https://github.com/ling5477/decision-hub/actions/runs/30633947829 |
| B5 plan publication | 30639680724 | `5be3943aa82e7408f080fe95732379b5e329deb0` | Quality；build & test (Testcontainers / Docker) | PASS | https://github.com/ling5477/decision-hub/actions/runs/30639680724 |
| B5 close commit | 30640835327 | `88b1d6d8ea68c39eaa74486e5e0bcb6502e00036` | Quality；build & test (Testcontainers / Docker) | PASS | https://github.com/ling5477/decision-hub/actions/runs/30640835327 |

Close run 的 head SHA 与 close commit 精确一致，两个 job 均 success；该普通 CI 不是 formal capacity acceptance。
