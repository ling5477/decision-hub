package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionContribution;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackConfidence;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import java.util.List;
import java.util.Objects;

/**
 * Audit port 接收的安全、不可变结构化记录。
 *
 * @param command 已校验命令
 * @param canonicalHash 规范输入与幂等 hash
 * @param resultIdentity 确定性结果身份
 * @param status 归因状态
 * @param errorCode 稳定错误码
 * @param contributions 可解释贡献
 * @param confidence 整体置信度
 */
public record FeedbackAttributionAuditRecord(
    FeedbackAttributionCommand command,
    FeedbackCanonicalHash canonicalHash,
    String resultIdentity,
    FeedbackStatus status,
    FeedbackAttributionErrorCode errorCode,
    List<AttributionContribution> contributions,
    FeedbackConfidence confidence) {

  /** 构造期冻结贡献集合并拒绝缺失审计身份。 */
  public FeedbackAttributionAuditRecord {
    command = Objects.requireNonNull(command, "command");
    canonicalHash = Objects.requireNonNull(canonicalHash, "canonicalHash");
    resultIdentity = Objects.requireNonNull(resultIdentity, "resultIdentity");
    status = Objects.requireNonNull(status, "status");
    errorCode = Objects.requireNonNull(errorCode, "errorCode");
    contributions = List.copyOf(Objects.requireNonNull(contributions, "contributions"));
    confidence = Objects.requireNonNull(confidence, "confidence");
  }
}
