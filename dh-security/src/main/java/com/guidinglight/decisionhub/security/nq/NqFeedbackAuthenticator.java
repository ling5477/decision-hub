package com.guidinglight.decisionhub.security.nq;

/**
 * NQ feedback 来源认证端口。
 *
 * <p>认证必须发生在结构化入库前；失败时不得调用 feedback ingestion service。
 */
public interface NqFeedbackAuthenticator {

  /**
   * 校验 feedback 来源、签名、timestamp、nonce 和请求大小。
   *
   * @param request 从 HTTP header 与 envelope 关键字段构造的认证请求。
   * @return 认证结果；失败结果包含可映射的 HTTP 状态。
   */
  NqFeedbackAuthResult authenticate(NqFeedbackAuthRequest request);
}
