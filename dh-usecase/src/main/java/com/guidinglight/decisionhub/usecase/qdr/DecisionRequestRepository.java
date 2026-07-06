package com.guidinglight.decisionhub.usecase.qdr;

import com.guidinglight.decisionhub.domain.qdr.DecisionRequest;
import java.util.Optional;

/**
 * `decision_request` 主线 repository port。
 *
 * <p>实现必须写入 `tenantId`、`traceId`、`requestId` 和脱敏输入 JSON；写失败必须向上抛出，不能 fail-open。
 */
public interface DecisionRequestRepository {

  /**
   * 保存主线请求。
   *
   * @param request 请求记录。
   */
  void save(DecisionRequest request);

  /**
   * 按租户和幂等 request key 查询主线请求。
   *
   * @param tenantId 租户 ID。
   * @param requestKey request key。
   * @return 命中的请求。
   */
  Optional<DecisionRequest> findByTenantIdAndRequestKey(String tenantId, String requestKey);
}
