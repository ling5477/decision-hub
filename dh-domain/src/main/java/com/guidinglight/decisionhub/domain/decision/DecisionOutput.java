package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * GateK Decision Pipeline structured output contract.
 *
 * <p>The output is a read-only recommendation. It must always carry the
 * mandatory forbidden-action set and must never be treated as a trading
 * instruction.
 */
public final class DecisionOutput {

  /** First frozen schema version for GateK K1. */
  public static final String DEFAULT_SCHEMA_VERSION = "1.0.0";

  private final String requestId;
  private final String traceId;
  private final String tenantId;
  private final DecisionType decisionType;
  private final DecisionAction action;
  private final DecisionStatus status;
  private final DecisionRiskLevel riskLevel;
  private final DecisionPolicyStatus policyStatus;
  private final ProviderSignalStatus providerStatus;
  private final Set<ForbiddenAction> forbiddenActions;
  private final List<String> reasonCodes;
  private final List<String> evidenceRefs;
  private final Instant createdAt;
  private final String schemaVersion;

  private DecisionOutput(
      final String requestId,
      final String traceId,
      final String tenantId,
      final DecisionType decisionType,
      final DecisionAction action,
      final DecisionStatus status,
      final DecisionRiskLevel riskLevel,
      final DecisionPolicyStatus policyStatus,
      final ProviderSignalStatus providerStatus,
      final Set<ForbiddenAction> forbiddenActions,
      final List<String> reasonCodes,
      final List<String> evidenceRefs,
      final Instant createdAt,
      final String schemaVersion) {
    this.requestId = requireText(requestId, "requestId");
    this.traceId = requireText(traceId, "traceId");
    this.tenantId = requireText(tenantId, "tenantId");
    this.decisionType = Objects.requireNonNull(decisionType, "decisionType");
    if (this.decisionType != DecisionType.READ_ONLY_RECOMMENDATION) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    this.action = Objects.requireNonNull(action, "action");
    this.status = Objects.requireNonNull(status, "status");
    this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
    this.policyStatus = Objects.requireNonNull(policyStatus, "policyStatus");
    this.providerStatus = Objects.requireNonNull(providerStatus, "providerStatus");
    this.forbiddenActions = Set.copyOf(Objects.requireNonNull(forbiddenActions, "forbiddenActions"));
    if (!this.forbiddenActions.containsAll(ForbiddenAction.mandatorySet())) {
      throw new IllegalArgumentException("forbiddenActions must contain the mandatory set");
    }
    if (isDirectionalBias(action)
        && (riskLevel == DecisionRiskLevel.HIGH || riskLevel == DecisionRiskLevel.BLOCKED)) {
      throw new IllegalArgumentException("high or blocked risk forbids directional bias");
    }
    if (isDirectionalBias(action) && policyStatus != DecisionPolicyStatus.ALLOWED) {
      throw new IllegalArgumentException("policy must allow directional bias");
    }
    this.reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    this.evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.schemaVersion = requireText(schemaVersion, "schemaVersion");
  }

  /** Default no-evidence output: fail closed to ABSTAIN. */
  public static DecisionOutput abstainForNoEvidence(
      final String requestId,
      final String traceId,
      final String tenantId,
      final Instant createdAt) {
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.ABSTAIN,
        DecisionStatus.ABSTAINED,
        DecisionRiskLevel.UNKNOWN,
        DecisionPolicyStatus.REVIEW_REQUIRED,
        ProviderSignalStatus.NOT_CALLED,
        ForbiddenAction.mandatorySet(),
        List.of("NO_EVIDENCE"),
        List.of(),
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  /** Provider failure output: fail closed to ABSTAIN. */
  public static DecisionOutput abstainForProviderFailure(
      final String requestId,
      final String traceId,
      final String tenantId,
      final ProviderSignalStatus providerStatus,
      final Instant createdAt) {
    return abstainForProviderFailure(
        requestId,
        traceId,
        tenantId,
        providerStatus,
        List.of("PROVIDER_UNAVAILABLE"),
        createdAt);
  }

  /**
   * provider guard 失败输出：保留具体失败原因码，同时统一 fail-closed 为 ABSTAIN。
   *
   * @param requestId 请求 ID
   * @param traceId trace ID
   * @param tenantId 租户 ID
   * @param providerStatus provider fail-closed 状态
   * @param reasonCodes 可审计原因码；不得包含原始 provider 响应或敏感信息
   * @param createdAt 输出创建时间
   * @return K1 合同格式的只读 fail-closed output
   */
  public static DecisionOutput abstainForProviderFailure(
      final String requestId,
      final String traceId,
      final String tenantId,
      final ProviderSignalStatus providerStatus,
      final List<String> reasonCodes,
      final Instant createdAt) {
    if (!new ProviderDecisionSignal(providerStatus, List.of()).requiresAbstain()) {
      throw new IllegalArgumentException("providerStatus must require abstain");
    }
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.ABSTAIN,
        DecisionStatus.ABSTAINED,
        DecisionRiskLevel.UNKNOWN,
        DecisionPolicyStatus.REVIEW_REQUIRED,
        providerStatus,
        ForbiddenAction.mandatorySet(),
        reasonCodes == null || reasonCodes.isEmpty()
            ? List.of("PROVIDER_UNAVAILABLE")
            : List.copyOf(reasonCodes),
        List.of(),
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  /** Policy denial output: fail closed to BLOCKED with no directional bias. */
  public static DecisionOutput blockedByPolicy(
      final String requestId,
      final String traceId,
      final String tenantId,
      final DecisionPolicyStatus policyStatus,
      final Instant createdAt) {
    if (policyStatus == DecisionPolicyStatus.ALLOWED) {
      throw new IllegalArgumentException("policyStatus must not be ALLOWED");
    }
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.ABSTAIN,
        DecisionStatus.BLOCKED,
        DecisionRiskLevel.BLOCKED,
        policyStatus,
        ProviderSignalStatus.NOT_CALLED,
        ForbiddenAction.mandatorySet(),
        List.of("POLICY_DENIED"),
        List.of(),
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  /** Directional analytical bias output; forbidden for high or blocked risk. */
  public static DecisionOutput directionalBias(
      final String requestId,
      final String traceId,
      final String tenantId,
      final DecisionAction action,
      final DecisionRiskLevel riskLevel,
      final List<String> reasonCodes,
      final List<String> evidenceRefs,
      final Instant createdAt) {
    if (!isDirectionalBias(action)) {
      throw new IllegalArgumentException("action must be LONG_BIAS or SHORT_BIAS");
    }
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        action,
        DecisionStatus.DIRECTIONAL_BIAS,
        riskLevel,
        DecisionPolicyStatus.ALLOWED,
        ProviderSignalStatus.MOCKED,
        ForbiddenAction.mandatorySet(),
        reasonCodes,
        evidenceRefs,
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  /** 生成安全的 observation-only 输出，不表达方向性偏好。 */
  public static DecisionOutput observation(
      final String requestId,
      final String traceId,
      final String tenantId,
      final DecisionAction action,
      final DecisionRiskLevel riskLevel,
      final List<String> reasonCodes,
      final List<String> evidenceRefs,
      final Instant createdAt) {
    if (action != DecisionAction.OBSERVE && action != DecisionAction.NO_TRADE) {
      throw new IllegalArgumentException("action must be OBSERVE or NO_TRADE");
    }
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        action,
        DecisionStatus.OBSERVATION_ONLY,
        riskLevel,
        DecisionPolicyStatus.ALLOWED,
        ProviderSignalStatus.MOCKED,
        ForbiddenAction.mandatorySet(),
        reasonCodes,
        evidenceRefs,
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  /** 风险或编排失败输出，统一 fail-closed 为 ABSTAIN。 */
  public static DecisionOutput abstainForRisk(
      final String requestId,
      final String traceId,
      final String tenantId,
      final DecisionRiskLevel riskLevel,
      final List<String> reasonCodes,
      final List<String> evidenceRefs,
      final Instant createdAt) {
    return new DecisionOutput(
        requestId,
        traceId,
        tenantId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.ABSTAIN,
        DecisionStatus.ABSTAINED,
        riskLevel,
        DecisionPolicyStatus.REVIEW_REQUIRED,
        ProviderSignalStatus.NOT_CALLED,
        ForbiddenAction.mandatorySet(),
        reasonCodes,
        evidenceRefs,
        createdAt,
        DEFAULT_SCHEMA_VERSION);
  }

  public String getRequestId() {
    return requestId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public DecisionType getDecisionType() {
    return decisionType;
  }

  public DecisionAction getAction() {
    return action;
  }

  public DecisionStatus getStatus() {
    return status;
  }

  public DecisionRiskLevel getRiskLevel() {
    return riskLevel;
  }

  public DecisionPolicyStatus getPolicyStatus() {
    return policyStatus;
  }

  public ProviderSignalStatus getProviderStatus() {
    return providerStatus;
  }

  public Set<ForbiddenAction> getForbiddenActions() {
    return forbiddenActions;
  }

  public List<String> getReasonCodes() {
    return reasonCodes;
  }

  public List<String> getEvidenceRefs() {
    return evidenceRefs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  private static boolean isDirectionalBias(final DecisionAction action) {
    return action == DecisionAction.LONG_BIAS || action == DecisionAction.SHORT_BIAS;
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}
