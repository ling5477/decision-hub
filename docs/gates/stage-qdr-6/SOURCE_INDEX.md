# Stage-QDR-6 Source Index

以下 source copies 是 archive-time frozen evidence。`docs/current` 原文件仍存在，等待独立 post-tag cleanup。

| Current source | Archive source | Role |
| --- | --- | --- |
| `docs/current/DH_STAGE_QDR_6_PLAN.md` | `SOURCE_DH_STAGE_QDR_6_PLAN.md` | plan |
| `docs/current/DH_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER.md` | `SOURCE_DH_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER.md` | implementation WO |
| `docs/current/DH_STAGE_QDR_6_B3_CANONICAL_SNAPSHOT_CONTRACT_REVIEW.md` | `SOURCE_DH_STAGE_QDR_6_B3_CANONICAL_SNAPSHOT_CONTRACT_REVIEW.md` | canonical snapshot contract |
| `docs/current/DH_STAGE_QDR_6_B3_SNAPSHOT_PERSISTENCE_GAP_REVIEW.md` | `SOURCE_DH_STAGE_QDR_6_B3_SNAPSHOT_PERSISTENCE_GAP_REVIEW.md` | persistence gap review |
| `docs/current/DH_STAGE_QDR_6_B3_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER.md` | `SOURCE_DH_STAGE_QDR_6_B3_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER.md` | persistence WO |
| `docs/current/DH_STAGE_QDR_6_B3_PERSISTENCE_MILESTONE_REVIEW.md` | `SOURCE_DH_STAGE_QDR_6_B3_PERSISTENCE_MILESTONE_REVIEW.md` | first persistence review |
| `docs/current/DH_STAGE_QDR_6_B3_PERSISTENCE_MILESTONE_REVIEW_RETRY.md` | `SOURCE_DH_STAGE_QDR_6_B3_PERSISTENCE_MILESTONE_REVIEW_RETRY.md` | persistence retry |
| `docs/current/DH_STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_BASELINE_GATE.md` | `SOURCE_DH_STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_BASELINE_GATE.md` | deterministic replay gate |
| `docs/current/DH_STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW.md` | `SOURCE_DH_STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW.md` | B3 close review |
| `docs/current/DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md` | `SOURCE_DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md` | first final close BLOCKED history |
| `docs/current/DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY.md` | `SOURCE_DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY.md` | final close retry PASS |

Canonical packet aliases：

```text
PLAN.md <- DH_STAGE_QDR_6_PLAN.md
IMPLEMENTATION_WORK_ORDER.md <- DH_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER.md
FINAL_CLOSE_REVIEW.md <- DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY.md
```

Final close blocker fix 没有独立 source 文件；其可审计证据由 `49fdf54`、`e09d5b4`、`5961164`、`DISCIPLINE_REPAIR.md` 以及 current `STATUS/WORKLOG/TESTING` 共同保存。
