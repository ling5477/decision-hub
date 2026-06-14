package com.guidinglight.decisionhub.security.nq;

/**
 * NQ feedback 跨系统 header 名称集中常量（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1）。
 *
 * <p>Why：header 名此前以 magic string 散落在 controller 中。集中到一处，避免拼写漂移，并为后续
 * canonical 化（Batch 2 起读取 {@code X-NQ-DH-*}）与 legacy 退场（Batch 6）提供单一事实源。
 *
 * <p>canonical 族 {@code X-NQ-DH-*} 以 Integration-0 frozen contract 为准（见
 * {@code DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md} §6）。legacy 族 {@code X-DH-NQ-*} 为 DH 历史实现命名，
 * Batch 1 仅作 parser 的读取来源以保持对外行为不变；canonical-only 切换后（Batch 2+）legacy 将被移除。
 *
 * <p>本类只持有字符串常量，不发起任何 IO、不持有凭证。
 */
public final class NqDhHeaderNames {

  private NqDhHeaderNames() {}

  /** canonical 来源系统标识 header。 */
  public static final String SOURCE = "X-NQ-DH-Source";

  /** canonical 租户标识 header（DH 入站权威 tenant 仍来自认证上下文，此 header 仅供后续一致性校验）。 */
  public static final String TENANT_ID = "X-NQ-DH-Tenant-Id";

  /** canonical 幂等键 header（DH 入站权威 requestId 仍来自 body，此 header 仅供后续一致性校验）。 */
  public static final String REQUEST_ID = "X-NQ-DH-Request-Id";

  /** canonical 端到端追踪键 header（DH 入站权威 traceId 仍来自 body，此 header 仅供后续一致性校验）。 */
  public static final String TRACE_ID = "X-NQ-DH-Trace-Id";

  /** canonical 请求时间戳 header（格式对齐另列 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT，不在本批）。 */
  public static final String TIMESTAMP = "X-NQ-DH-Timestamp";

  /** canonical 一次性随机值 header（防重放）。 */
  public static final String NONCE = "X-NQ-DH-Nonce";

  /** canonical HMAC-SHA256 签名 header。 */
  public static final String SIGNATURE = "X-NQ-DH-Signature";

  /** legacy 来源 header（历史 {@code X-DH-NQ-*}）；Batch 1 parser 读取来源，canonical-only 后移除。 */
  public static final String LEGACY_SOURCE = "X-DH-NQ-Source";

  /** legacy 租户 header（历史）；DH 当前不从 header 取 tenant，保留仅为完整映射。 */
  public static final String LEGACY_TENANT_ID = "X-DH-NQ-Tenant-Id";

  /** legacy 幂等键 header（历史）；DH 当前不从 header 取 requestId。 */
  public static final String LEGACY_REQUEST_ID = "X-DH-NQ-Request-Id";

  /** legacy 追踪键 header（历史）；DH 当前不从 header 取 traceId。 */
  public static final String LEGACY_TRACE_ID = "X-DH-NQ-Trace-Id";

  /** legacy 时间戳 header（历史）。 */
  public static final String LEGACY_TIMESTAMP = "X-DH-NQ-Timestamp";

  /** legacy nonce header（历史）。 */
  public static final String LEGACY_NONCE = "X-DH-NQ-Nonce";

  /** legacy 签名 header（历史）。 */
  public static final String LEGACY_SIGNATURE = "X-DH-NQ-Signature";
}
