# Stage-QDR-9 CI Evidence Reference

| 用途 | Run | Head SHA | Jobs | 结论 | URL |
|---|---:|---|---|---|---|
| accepted technical baseline | 30633947829 | `c7f940c0c48900a0cfb7eac86aac745c8006629c` | Quality；build & test (Testcontainers / Docker) | PASS | https://github.com/ling5477/decision-hub/actions/runs/30633947829 |
| B5 plan publication | 30639680724 | `5be3943aa82e7408f080fe95732379b5e329deb0` | Quality；build & test (Testcontainers / Docker) | PASS | https://github.com/ling5477/decision-hub/actions/runs/30639680724 |
| B5 close commit | 由 close commit push 触发 | `THIS_CLOSE_COMMIT` | Quality；build & test (Testcontainers / Docker) | TAG 前必须 PASS | 发布后通过 `gh run list --commit <SHA>` 核验 |

Close run ID 在 close commit 产生后才存在，因此 tag 前通过远端 exact-SHA gate 核验，并在 post-tag cleanup
记录中写回实际 run/head/job/url；不得把 pending 或 no-run 写成 PASS。
