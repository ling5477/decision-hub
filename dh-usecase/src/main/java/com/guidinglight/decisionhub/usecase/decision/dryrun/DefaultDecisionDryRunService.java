package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * limited dry-run endpoint 的默认 usecase 实现。
 *
 * <p>流程为 feature/profile gate -> request policy -> source pair gate -> audit received ->
 * existing mock-only DecisionOrchestrator -> readonly response mapping。实现不接 NQ、不接真实 provider、不发 HTTP、
 * 不读写交易状态；audit 写失败会阻断成功响应并返回 fail-closed error envelope。
 */
public final class DefaultDecisionDryRunService implements DecisionDryRunService {

  private static final String ENDPOINT = "/api/ai/decision-dry-runs";

  private final DecisionOrchestrator orchestrator;
  private final DecisionAuditRepository auditRepository;
  private final DecisionDryRunRuntimeProperties properties;
  private final Clock clock;

  /**
   * 创建 dry-run service。
   *
   * @param orchestrator 既有 mock-only DecisionOrchestrator。
   * @param auditRepository 既有 audit / trace / replay persistence port。
   * @param properties runtime feature gate 配置。
   * @param clock 时间源。
   */
  public DefaultDecisionDryRunService(
      final DecisionOrchestrator orchestrator,
      final DecisionAuditRepository auditRepository,
      final DecisionDryRunRuntimeProperties properties,
      final Clock clock) {
    this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * 执行 limited dry-run；任一异常均转换为 fail-closed result。
   *
   * @param command dry-run 命令。
   * @return 成功 snapshot 或 error envelope result。
   */
  @Override
  public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
    try {
      final DecisionDryRunResult preflight = preflight(command);
      if (preflight != null) {
        return preflight;
      }
      final String receivedAuditRef =
          writeAudit(
              command,
              DecisionAuditEventType.DECISION_COMPLETED,
              DecisionAuditEventStatus.SUCCESS,
              null,
              "DRY_RUN_RECEIVED");
      final DecisionRequest request = toDecisionRequest(command);
      final DecisionOutput output = orchestrator.decide(request);
      final DecisionDryRunResult providerFailure = mapProviderFailure(command, output);
      if (providerFailure != null) {
        return providerFailure;
      }
      final DecisionDryRunSnapshot snapshot = toSnapshot(output, receivedAuditRef);
      return DecisionDryRunResult.success(snapshot);
    } catch (final RuntimeException error) {
      return rejectWithoutThrowing(
          command,
          500,
          DecisionDryRunErrorCode.UNKNOWN_ERROR,
          "dry-run request failed closed",
          DecisionAuditEventType.DECISION_FAILED);
    }
  }

  /**
   * 记录 controller/security 层拒绝，并统一返回 error envelope result。
   *
   * @param command 已解析出的命令；解析失败时可为 null。
   * @param status HTTP 状态建议。
   * @param errorCode canonical error code。
   * @param message 脱敏错误摘要。
   * @return fail-closed result。
   */
  @Override
  public DecisionDryRunResult reject(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message) {
    return rejectWithoutThrowing(command, status, errorCode, message, eventTypeFor(errorCode));
  }

  private DecisionDryRunResult preflight(final DecisionDryRunCommand command) {
    if (command == null) {
      return rejectWithoutThrowing(
          null,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "missing dry-run request",
          DecisionAuditEventType.POLICY_DENIED);
    }
    if (!properties.runtimeEnabled()) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run endpoint disabled by feature gate",
          DecisionAuditEventType.POLICY_DENIED);
    }
    if (!requiredEnvelopePresent(command) || !command.dryRun()) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run request envelope is invalid",
          DecisionAuditEventType.POLICY_DENIED);
    }
    if (!properties.sourceAllowed(command.source())
        || !properties.tenantSourceAllowed(command.tenantId(), command.source())) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.SOURCE_DENIED,
          "dry-run source denied by tenant/source allowlist",
          DecisionAuditEventType.POLICY_DENIED);
    }
    if (command.forbiddenMaterialDetected()
        || !declaresMandatoryForbiddenCapabilities(command.forbiddenCapabilities())) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run request contains forbidden capability or execution material",
          DecisionAuditEventType.POLICY_DENIED);
    }
    if (command.context() == null || command.context().approxBytes() > properties.memoryCapBytes()) {
      return rejectWithoutThrowing(
          command,
          500,
          DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED,
          "dry-run context exceeds memory cap",
          DecisionAuditEventType.DECISION_FAILED);
    }
    if (!requiredContextPresent(command.context())) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run context is missing read-only subject fields",
          DecisionAuditEventType.POLICY_DENIED);
    }
    return null;
  }

  private DecisionDryRunResult mapProviderFailure(
      final DecisionDryRunCommand command, final DecisionOutput output) {
    if (output == null) {
      return rejectWithoutThrowing(
          command,
          500,
          DecisionDryRunErrorCode.UNKNOWN_ERROR,
          "dry-run orchestrator returned no output",
          DecisionAuditEventType.DECISION_FAILED);
    }
    if (output.getProviderStatus() == ProviderSignalStatus.DISABLED) {
      return rejectWithoutThrowing(
          command,
          503,
          DecisionDryRunErrorCode.PROVIDER_DISABLED,
          "provider disabled by guard",
          DecisionAuditEventType.PROVIDER_FAILED);
    }
    if (output.getProviderStatus() == ProviderSignalStatus.TIMEOUT) {
      return rejectWithoutThrowing(
          command,
          504,
          DecisionDryRunErrorCode.PROVIDER_TIMEOUT,
          "provider timed out by guard",
          DecisionAuditEventType.PROVIDER_FAILED);
    }
    if (output.getProviderStatus() == ProviderSignalStatus.BUDGET_EXCEEDED) {
      return rejectWithoutThrowing(
          command,
          429,
          DecisionDryRunErrorCode.BUDGET_EXCEEDED,
          "provider budget exceeded by guard",
          DecisionAuditEventType.PROVIDER_FAILED);
    }
    if (output.getPolicyStatus() == DecisionPolicyStatus.DENIED
        || output.getPolicyStatus() == DecisionPolicyStatus.INVALID
        || output.getPolicyStatus() == DecisionPolicyStatus.BLOCKED) {
      return rejectWithoutThrowing(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run policy denied",
          DecisionAuditEventType.POLICY_DENIED);
    }
    return null;
  }

  private DecisionRequest toDecisionRequest(final DecisionDryRunCommand command) {
    final DecisionDryRunContext context = command.context();
    return DecisionRequest.readOnlyRecommendation(
        command.requestId(),
        command.traceId(),
        command.tenantId(),
        command.source(),
        new DecisionSubject(
            context.symbol(),
            context.market(),
            context.timeframe(),
            context.strategyRef(),
            context.researchRef()),
        context.contextRef(),
        new DecisionContextSnapshot(
            context.snapshotId(),
            context.capturedAt() == null ? clock.instant() : context.capturedAt(),
            context.evidenceRefs()),
        clock.instant());
  }

  private DecisionDryRunSnapshot toSnapshot(
      final DecisionOutput output, final String receivedAuditRef) {
    final List<String> reasons = new ArrayList<>(output.getReasonCodes());
    final DecisionAction externalAction;
    if (output.getAction() == DecisionAction.ABSTAIN) {
      externalAction = DecisionAction.NO_TRADE;
      reasons.add("INTERNAL_ABSTAIN_MAPPED");
    } else {
      externalAction = output.getAction();
    }
    return new DecisionDryRunSnapshot(
        output.getRequestId(),
        true,
        externalAction.name(),
        confidenceFor(externalAction),
        output.getRiskLevel().name(),
        reasons,
        List.of(
            "request:" + output.getRequestId(),
            "trace:" + output.getTraceId(),
            "policy:" + output.getPolicyStatus().name(),
            "provider:" + output.getProviderStatus().name()),
        "replay:" + output.getRequestId(),
        receivedAuditRef,
        output.getSchemaVersion());
  }

  private DecisionDryRunResult rejectWithoutThrowing(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message,
      final DecisionAuditEventType eventType) {
    try {
      final String auditRef =
          writeAudit(
              command,
              eventType,
              DecisionAuditEventStatus.FAILED,
              errorCode.name(),
              "DRY_RUN_REJECTED");
      return DecisionDryRunResult.rejected(
          status, errorCode, message, requestId(command), traceId(command), auditRef);
    } catch (final RuntimeException auditFailure) {
      return DecisionDryRunResult.rejected(
          500,
          DecisionDryRunErrorCode.UNKNOWN_ERROR,
          "dry-run audit write failed closed",
          requestId(command),
          traceId(command),
          null);
    }
  }

  private String writeAudit(
      final DecisionDryRunCommand command,
      final DecisionAuditEventType eventType,
      final DecisionAuditEventStatus eventStatus,
      final String errorCode,
      final String marker) {
    final String requestId = requestId(command);
    final String auditId =
        requestId
            + "-dryrun-audit-"
            + marker.toLowerCase(Locale.ROOT)
            + "-"
            + UUID.randomUUID();
    auditRepository.saveAuditEvent(
        new DecisionPersistenceRecords.AuditEventRecord(
            auditId,
            requestId,
            tenantId(command),
            traceId(command),
            eventType,
            eventStatus,
            Map.of(
                "endpoint", ENDPOINT,
                "marker", marker,
                "requestId", requestId,
                "source", source(command),
                "schemaVersion", schemaVersion(command),
                "dryRun", command != null && command.dryRun()),
            errorCode,
            clock.instant()));
    return "audit:" + auditId;
  }

  private static boolean requiredEnvelopePresent(final DecisionDryRunCommand command) {
    return !isBlank(command.requestId())
        && !isBlank(command.traceId())
        && !isBlank(command.tenantId())
        && !isBlank(command.source())
        && !isBlank(command.timestamp())
        && !isBlank(command.nonce())
        && !isBlank(command.schemaVersion());
  }

  private static boolean requiredContextPresent(final DecisionDryRunContext context) {
    return !isBlank(context.symbol())
        && !isBlank(context.market())
        && !isBlank(context.timeframe())
        && !isBlank(context.snapshotId());
  }

  private static boolean declaresMandatoryForbiddenCapabilities(final Set<String> capabilities) {
    final Set<String> normalized =
        capabilities.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    return ForbiddenAction.mandatorySet().stream().map(Enum::name).allMatch(normalized::contains);
  }

  private static BigDecimal confidenceFor(final DecisionAction action) {
    return switch (action) {
      case LONG_BIAS, SHORT_BIAS -> new BigDecimal("0.650000");
      case OBSERVE, NO_TRADE -> new BigDecimal("0.500000");
      case ABSTAIN -> BigDecimal.ZERO;
    };
  }

  private static DecisionAuditEventType eventTypeFor(final DecisionDryRunErrorCode errorCode) {
    if (errorCode == DecisionDryRunErrorCode.PROVIDER_DISABLED
        || errorCode == DecisionDryRunErrorCode.PROVIDER_TIMEOUT
        || errorCode == DecisionDryRunErrorCode.BUDGET_EXCEEDED) {
      return DecisionAuditEventType.PROVIDER_FAILED;
    }
    if (errorCode == DecisionDryRunErrorCode.POLICY_DENIED
        || errorCode == DecisionDryRunErrorCode.SOURCE_DENIED
        || errorCode == DecisionDryRunErrorCode.TENANT_MISMATCH) {
      return DecisionAuditEventType.POLICY_DENIED;
    }
    return DecisionAuditEventType.DECISION_FAILED;
  }

  private static String requestId(final DecisionDryRunCommand command) {
    return isBlank(command == null ? null : command.requestId())
        ? "unknown-request"
        : command.requestId();
  }

  private static String traceId(final DecisionDryRunCommand command) {
    return isBlank(command == null ? null : command.traceId())
        ? "unknown-trace"
        : command.traceId();
  }

  private static String tenantId(final DecisionDryRunCommand command) {
    return isBlank(command == null ? null : command.tenantId())
        ? "unknown-tenant"
        : command.tenantId();
  }

  private static String source(final DecisionDryRunCommand command) {
    return isBlank(command == null ? null : command.source())
        ? "unknown-source"
        : command.source();
  }

  private static String schemaVersion(final DecisionDryRunCommand command) {
    return isBlank(command == null ? null : command.schemaVersion())
        ? "unknown-schema"
        : command.schemaVersion();
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
