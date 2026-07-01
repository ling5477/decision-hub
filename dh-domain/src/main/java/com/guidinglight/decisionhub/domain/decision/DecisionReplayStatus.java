package com.guidinglight.decisionhub.domain.decision;

/** K4 replay read model 查询状态；所有非 FOUND 状态都不得被解释为可重放成功。 */
public enum DecisionReplayStatus {
  /** 找到完整 replay view。 */
  FOUND,

  /** 当前 tenant / decisionId 下没有任何 K3 记录。 */
  NOT_FOUND,

  /** 查询结果归属与请求 tenant 不一致，必须 fail-closed 且不返回明细。 */
  TENANT_MISMATCH,

  /** 找到部分记录，但 request / context / trace / output / audit 关键数据不完整。 */
  INCOMPLETE,

  /** 已落库 JSON、枚举或时间字段无法安全解析，不能返回错误 replay。 */
  CORRUPTED,

  /** 查询输入非法或底层读取失败，必须阻断 replay。 */
  BLOCKED
}
