package com.guidinglight.decisionhub.connector.nq;

import java.util.List;
import java.util.Map;

/**
 * Stage1：DH -> NQ 契约校验器接口。
 *
 * <p>对应工单 4.5：NqContractVerifier。Stage1 用规则验证请求结构是否满足 NQ 集成边界。
 */
public interface NqContractVerifier {

  /**
   * 校验请求是否合法。
   *
   * @param request 请求体。
   * @return 违规原因列表；空列表表示通过。
   */
  List<String> verify(Map<String, Object> request);
}
