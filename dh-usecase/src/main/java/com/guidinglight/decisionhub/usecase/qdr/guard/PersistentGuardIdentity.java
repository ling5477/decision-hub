package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.util.Objects;

/**
 * Stage-QDR-7 persistent guard 的完整隔离身份。
 *
 * <p>所有production查询必须携带全部字段；本类型不提供tenantless、latest、default或fallback身份。
 *
 * @param environment 部署环境，只允许dev/test/staging/prod。
 * @param endpoint canonical endpoint。
 * @param source canonical wire source。
 * @param tenantId 已认证tenant，禁止outer whitespace。
 */
public record PersistentGuardIdentity(
    String environment, String endpoint, String source, String tenantId) {

  /** Canonical protected endpoint。 */
  public static final String DECISION_DRY_RUN_ENDPOINT = "/api/ai/decision-dry-runs";

  /** Canonical protected source。 */
  public static final String NQ_DRYRUN_SOURCE = "NQ_DRYRUN";

  /** 校验完整identity，非法配置或不可信输入必须fail-closed。 */
  public PersistentGuardIdentity {
    environment = requireExact(environment, "environment");
    if (!java.util.Set.of("dev", "test", "staging", "prod").contains(environment)) {
      throw new IllegalArgumentException("unsupported guard environment");
    }
    endpoint = requireExact(endpoint, "endpoint");
    if (!DECISION_DRY_RUN_ENDPOINT.equals(endpoint)) {
      throw new IllegalArgumentException("unsupported guard endpoint");
    }
    source = requireExact(source, "source");
    if (!NQ_DRYRUN_SOURCE.equals(source)) {
      throw new IllegalArgumentException("unsupported guard source");
    }
    tenantId = requireExact(tenantId, "tenantId");
  }

  private static String requireExact(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field);
    if (checked.isBlank() || !checked.equals(checked.trim())) {
      throw new IllegalArgumentException(field + " must be non-blank without outer whitespace");
    }
    return checked;
  }
}
