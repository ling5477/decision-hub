package com.guidinglight.decisionhub.qdr7;

import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Limited dry-run专用persistent rate limiter bridge。
 *
 * <p>它保留Controller既有RateLimiter构造合同，但将window identity和counter完全委托PostgreSQL port；audit与admission
 * 共用required transaction。任何commit/store/config不确定都返回独立fail-closed分类，不调用in-memory实现。
 */
public final class PersistentDecisionDryRunRateLimiter implements RateLimiter {

  /** Controller内部canonical route；不接受raw path或alias。 */
  public static final String ROUTE = "DECISION_DRY_RUN";

  private final RateLimitAdmissionPort admissionPort;
  private final GuardTransactionBoundary transactions;
  private final DecisionAuditRepository auditRepository;
  private final DecisionDryRunGuardProperties properties;
  private final Clock clock;

  /** 创建persistent rate bridge。 */
  public PersistentDecisionDryRunRateLimiter(
      final RateLimitAdmissionPort admissionPort,
      final GuardTransactionBoundary transactions,
      final DecisionAuditRepository auditRepository,
      final DecisionDryRunGuardProperties properties,
      final Clock clock) {
    this.admissionPort = Objects.requireNonNull(admissionPort, "admissionPort");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public RateLimitResult check(
      final String source, final String tenantId, final String route, final Instant ignoredNow) {
    return check(source, tenantId, route, ignoredNow, "unknown-request", "unknown-trace");
  }

  @Override
  public RateLimitResult check(
      final String source,
      final String tenantId,
      final String route,
      final Instant ignoredNow,
      final String requestId,
      final String traceId) {
    if (!properties.runtimeEnabled()) {
      return RateLimitResult.pass();
    }
    if (!ROUTE.equals(route)) {
      return RateLimitResult.configurationInvalid();
    }
    final PersistentGuardIdentity identity;
    try {
      identity =
          new PersistentGuardIdentity(
              properties.environment(),
              PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
              source,
              tenantId);
    } catch (final IllegalArgumentException error) {
      return RateLimitResult.configurationInvalid();
    }
    try {
      final RateLimitAdmissionResult result =
          transactions.required(
              () -> {
                final RateLimitAdmissionResult admission =
                    admissionPort.tryAcquire(
                        new RateLimitAdmissionCommand(
                            identity, properties.rateWindowSeconds(), properties.rateLimitValue()));
                if (admission.status()
                    != com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus
                        .STORE_UNAVAILABLE) {
                  writeAudit(requestId, traceId, identity, admission);
                }
                return admission;
              });
      return switch (result.status()) {
        case ACCEPTED -> RateLimitResult.pass();
        case RATE_LIMITED -> RateLimitResult.limited(retryAfter(result));
        case STORE_UNAVAILABLE -> RateLimitResult.storeUnavailable();
        case COMMIT_UNKNOWN -> RateLimitResult.commitUnknown();
        case CONFIGURATION_INVALID -> RateLimitResult.configurationInvalid();
      };
    } catch (final PersistentGuardStoreException error) {
      return RateLimitResult.storeUnavailable();
    } catch (final RuntimeException error) {
      return RateLimitResult.commitUnknown();
    }
  }

  private void writeAudit(
      final String requestId,
      final String traceId,
      final PersistentGuardIdentity identity,
      final RateLimitAdmissionResult result) {
    auditRepository.saveAuditEvent(
        new DecisionPersistenceRecords.AuditEventRecord(
            safe(requestId, "unknown-request") + "-qdr7-rate-" + UUID.randomUUID(),
            safe(requestId, "unknown-request"),
            identity.tenantId(),
            safe(traceId, "unknown-trace"),
            DecisionAuditEventType.QDR7_RATE_LIMIT_ADMISSION,
            result.status()
                    == com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus
                        .ACCEPTED
                ? DecisionAuditEventStatus.SUCCESS
                : DecisionAuditEventStatus.FAILED,
            Map.of(
                "endpoint",
                identity.endpoint(),
                "source",
                identity.source(),
                "status",
                result.status().name()),
            result.status()
                    == com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus
                        .ACCEPTED
                ? null
                : result.status().name(),
            clock.instant()));
  }

  private static Integer retryAfter(final RateLimitAdmissionResult result) {
    if (result.windowEnd() == null || result.databaseNow() == null) {
      return null;
    }
    return (int)
        Math.max(
            1L,
            result.windowEnd().getEpochSecond() - result.databaseNow().getEpochSecond());
  }

  private static String safe(final String value, final String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
