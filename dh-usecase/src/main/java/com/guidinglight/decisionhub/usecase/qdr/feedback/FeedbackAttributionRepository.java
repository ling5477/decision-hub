package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionContributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackAttributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import java.util.List;
import java.util.Optional;

/**
 * Stage-QDR-9 feedback aggregate 的 usecase-owned persistence/query port。
 *
 * <p>B1 只冻结接口；JDBC adapter、duplicate-key reconciliation 与事务编排属于后续 B2。
 */
public interface FeedbackAttributionRepository {

  /** 按 tenant/environment/idempotency key 读取完整 aggregate。 */
  Optional<FeedbackPersistenceAggregate> findByIdempotencyKey(
      String tenantId, FeedbackEnvironment environment, String idempotencyKey);

  /** 按 tenant/environment/attribution identity 读取完整 aggregate。 */
  Optional<FeedbackPersistenceAggregate> findByAttributionId(
      String tenantId, FeedbackEnvironment environment, String attributionId);

  /** 插入 observation；必须在强制事务边界内调用。 */
  void insertObservation(OutcomeObservationRecord observation);

  /** 插入 attribution；必须在 observation 之后、同一事务内调用。 */
  void insertAttribution(FeedbackAttributionRecord attribution);

  /** 按 sortOrder 稳定插入全部 contributions。 */
  void insertContributions(List<AttributionContributionRecord> contributions);

  /** 在 reference confirmation 后插入全部 references。 */
  void insertReferences(List<AttributionReferenceRecord> references);
}
