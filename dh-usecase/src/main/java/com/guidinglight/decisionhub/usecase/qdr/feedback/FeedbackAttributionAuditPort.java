package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackAuditReference;

/**
 * 结构化归因审计端口。
 *
 * <p>实现必须在成功返回前持久获得 audit/replay 安全引用；失败必须抛出 {@link FeedbackAttributionAuditException}。
 */
public interface FeedbackAttributionAuditPort {

  /** 写入一次安全审计记录并返回与记录 scope 完全一致的不可变引用。 */
  FeedbackAuditReference write(FeedbackAttributionAuditRecord record);
}
