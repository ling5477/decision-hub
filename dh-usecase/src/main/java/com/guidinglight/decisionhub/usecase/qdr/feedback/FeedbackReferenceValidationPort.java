package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;

/**
 * Audit/replay/evaluation/evidence 安全引用确认端口。
 *
 * <p>实现必须 tenant-bound；既有目标表缺少 environment 时仍需保留并校验当前 environment，无法证明即
 * fail-closed。端口不得调用 HTTP、Provider 或 NQ。
 */
public interface FeedbackReferenceValidationPort {

  /**
   * 在写入前确认引用与 observation scope 完全一致。
   *
   * @throws FeedbackPersistenceException 引用不存在或 scope 不匹配时使用 {@code REFERENCE_INVALID}
   */
  void confirm(
      AttributionReferenceRecord reference, OutcomeObservationRecord observation);
}
