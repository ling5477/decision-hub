package com.guidinglight.decisionhub.usecase.qdr.report;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceFinding;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessAcceptanceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessFinding;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessSeverity;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportFindingView;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportView;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.DeterministicReplayResult;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayDifference;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayReproducibilityStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Stage-QDR-6 B4 evidence/replay report 的纯内存编排 service。
 *
 * <p>Service 只消费调用方已取得的 structured results，不读取 repository、不重新 assembly/replay、不持久化 report，
 * 也不依赖 HTTP、Provider、NQ、Agent、LangGraph、clock 或 random。任何 source/转换异常都返回脱敏的
 * {@link InternalAcceptanceStatus#FAILED} report。
 */
public final class DecisionEvidenceReplayReportService {

  /**
   * 聚合既有 structured results，并按冻结优先级计算 internal acceptance status。
   *
   * @param correlation 调用方期望的 tenant-bound 四键 identity。
   * @param evidence 已完成评估的 evidence aggregate。
   * @param replay 已执行完成的 deterministic replay result。
   * @param regression 已生成的 regression read model。
   * @param readiness 已完成的 provider readiness evaluation。
   * @param observability 已生成的 internal observability evidence。
   * @return 纯内存、不可授权外部行为的 internal report。
   */
  public DecisionEvidenceReplayInternalReport generate(
      final DecisionEvidenceCorrelation correlation,
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability) {
    return generateFromSources(
        correlation,
        () -> evidence,
        () -> replay,
        () -> regression,
        () -> readiness,
        () -> observability);
  }

  DecisionEvidenceReplayInternalReport generateFromSources(
      final DecisionEvidenceCorrelation correlation,
      final Supplier<DecisionEvidenceAggregate> evidenceSource,
      final Supplier<DeterministicReplayResult> replaySource,
      final Supplier<RegressionReportView> regressionSource,
      final Supplier<ProviderReadinessEvaluationResult> readinessSource,
      final Supplier<ModelGatewayObservabilityReport> observabilitySource) {
    final DecisionEvidenceCorrelation checkedCorrelation =
        Objects.requireNonNull(correlation, "correlation");
    try {
      return assemble(
          checkedCorrelation,
          Objects.requireNonNull(evidenceSource, "evidenceSource").get(),
          Objects.requireNonNull(replaySource, "replaySource").get(),
          Objects.requireNonNull(regressionSource, "regressionSource").get(),
          Objects.requireNonNull(readinessSource, "readinessSource").get(),
          Objects.requireNonNull(observabilitySource, "observabilitySource").get());
    } catch (final RuntimeException error) {
      return failedReport(checkedCorrelation);
    }
  }

  private DecisionEvidenceReplayInternalReport assemble(
      final DecisionEvidenceCorrelation correlation,
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability) {
    final List<InternalAcceptanceFinding> findings = new ArrayList<>();
    final boolean missingInput =
        evidence == null || replay == null || regression == null || readiness == null || observability == null;
    if (missingInput) {
      findings.add(
          finding(
              InternalAcceptanceFinding.Source.INPUT_BOUNDARY,
              InternalAcceptanceFinding.Severity.BLOCKER,
              "MISSING_REQUIRED_REPORT_INPUT",
              null,
              "required structured report input is missing"));
    }

    final boolean identityMismatch =
        !identityMatches(correlation, evidence, replay, regression, readiness, observability);
    if (identityMismatch) {
      findings.add(
          finding(
              InternalAcceptanceFinding.Source.INPUT_BOUNDARY,
              InternalAcceptanceFinding.Severity.ERROR,
              "REPORT_IDENTITY_MISMATCH",
              null,
              "tenant or correlation identity does not match"));
    }

    if (evidence != null) {
      findings.addAll(evidenceFindings(evidence));
    }
    if (replay != null) {
      findings.addAll(replayFindings(replay));
    }
    if (regression != null) {
      findings.addAll(regressionFindings(regression));
    }
    if (readiness != null) {
      findings.addAll(readinessFindings(readiness));
    }
    if (observability != null
        && observability.acceptanceStatus() != ProviderReadinessAcceptanceStatus.PASS) {
      findings.add(
          finding(
              InternalAcceptanceFinding.Source.OBSERVABILITY,
              severity(observability.acceptanceStatus()),
              "OBSERVABILITY_ACCEPTANCE_" + observability.acceptanceStatus().name(),
              observability.providerRef(),
              "observability acceptance evidence did not pass"));
    }

    final InternalAcceptanceStatus status =
        status(evidence, replay, regression, readiness, observability, missingInput, identityMismatch);
    return report(correlation, evidence, replay, regression, readiness, observability, status, findings);
  }

  private static InternalAcceptanceStatus status(
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability,
      final boolean missingInput,
      final boolean identityMismatch) {
    if (identityMismatch) {
      return InternalAcceptanceStatus.INVALID;
    }
    if (missingInput) {
      return InternalAcceptanceStatus.INCOMPLETE;
    }
    if (replay.status() == ReplayReproducibilityStatus.EXECUTION_FAILED) {
      return InternalAcceptanceStatus.FAILED;
    }
    if (evidence.status() == DecisionEvidenceStatus.INVALID
        || replay.status() == ReplayReproducibilityStatus.INVALID_INPUT) {
      return InternalAcceptanceStatus.INVALID;
    }
    if (replay.status() == ReplayReproducibilityStatus.UNSUPPORTED_VERSION) {
      return InternalAcceptanceStatus.UNSUPPORTED;
    }
    if (evidence.status() == DecisionEvidenceStatus.INCOMPLETE
        || !evidence.missingMandatoryEvidence().isEmpty()
        || replay.status() == ReplayReproducibilityStatus.INCOMPLETE
        || regression.verdict() == RegressionVerdict.Status.SKIPPED
        || readiness.decision() == ProviderReadinessDecision.SKIPPED
        || readiness.readinessSignal().status() == ProviderReadinessStatus.SKIPPED
        || observability.acceptanceStatus() == ProviderReadinessAcceptanceStatus.SKIPPED) {
      return InternalAcceptanceStatus.INCOMPLETE;
    }
    if (replay.status() == ReplayReproducibilityStatus.DIFFERENT
        || regression.verdict() != RegressionVerdict.Status.PASS
        || readiness.decision() != ProviderReadinessDecision.READY
        || readiness.readinessSignal().status() != ProviderReadinessStatus.READY
        || observability.acceptanceStatus() != ProviderReadinessAcceptanceStatus.PASS) {
      return InternalAcceptanceStatus.REJECTED;
    }
    return InternalAcceptanceStatus.ACCEPTED;
  }

  private static boolean identityMatches(
      final DecisionEvidenceCorrelation expected,
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability) {
    if (evidence == null || replay == null || regression == null || readiness == null || observability == null) {
      return true;
    }
    return expected.matches(evidence.correlation())
        && expected.tenantId().equals(replay.tenantId())
        && expected.traceId().equals(replay.traceId())
        && expected.requestId().equals(replay.requestId())
        && expected.decisionId().equals(replay.decisionId())
        && expected.tenantId().equals(regression.tenantId())
        && expected.traceId().equals(regression.traceId())
        && expected.requestId().equals(regression.sourceRequestId())
        && expected.decisionId().equals(regression.sourceDecisionId())
        && expected.traceId().equals(readiness.traceId())
        && expected.requestId().equals(readiness.sourceRequestId())
        && expected.tenantId().equals(observability.tenantId())
        && expected.traceId().equals(observability.traceId())
        && expected.requestId().equals(observability.sourceRequestId())
        && readiness.providerRef().equals(observability.providerRef())
        && readiness.decision().equals(observability.providerReadiness().readinessDecision())
        && readiness.readinessSignal().status().equals(observability.providerReadiness().readinessStatus());
  }

  private static DecisionEvidenceReplayInternalReport report(
      final DecisionEvidenceCorrelation correlation,
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability,
      final InternalAcceptanceStatus status,
      final List<InternalAcceptanceFinding> findings) {
    return new DecisionEvidenceReplayInternalReport(
        correlation,
        evidence == null ? null : evidence.status(),
        evidence == null ? List.of() : evidence.evidenceRefs().stream().map(ref -> ref.identity()).toList(),
        evidence == null ? List.of() : evidence.missingMandatoryEvidence(),
        replay == null ? null : replay.status(),
        replay == null ? List.of() : replay.differences(),
        replay == null ? null : replay.failureCode(),
        regression == null ? null : regression.verdict(),
        regression == null ? null : regression.severity(),
        readiness == null ? null : readiness.decision(),
        readiness == null ? null : readiness.readinessSignal().status(),
        observability == null ? null : observability.acceptanceStatus(),
        observabilityRefs(observability),
        status,
        findings,
        DecisionEvidenceReplayInternalReport.INTERNAL_ACCEPTANCE_ONLY);
  }

  private static DecisionEvidenceReplayInternalReport failedReport(
      final DecisionEvidenceCorrelation correlation) {
    return new DecisionEvidenceReplayInternalReport(
        correlation,
        null,
        List.of(),
        List.of(),
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        InternalAcceptanceStatus.FAILED,
        List.of(
            finding(
                InternalAcceptanceFinding.Source.INPUT_BOUNDARY,
                InternalAcceptanceFinding.Severity.BLOCKER,
                "REPORT_SOURCE_FAILED",
                null,
                "structured report source or conversion failed")),
        DecisionEvidenceReplayInternalReport.INTERNAL_ACCEPTANCE_ONLY);
  }

  private static List<InternalAcceptanceFinding> evidenceFindings(
      final DecisionEvidenceAggregate aggregate) {
    final List<InternalAcceptanceFinding> mapped = new ArrayList<>();
    for (DecisionEvidenceFinding source : aggregate.findings()) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.EVIDENCE,
              severity(source.severity()),
              source.code(),
              source.safeRef(),
              source.message()));
    }
    for (var missing : aggregate.missingMandatoryEvidence()) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.EVIDENCE,
              InternalAcceptanceFinding.Severity.BLOCKER,
              "MISSING_MANDATORY_EVIDENCE",
              missing.name(),
              "mandatory evidence is missing"));
    }
    return mapped;
  }

  private static List<InternalAcceptanceFinding> replayFindings(
      final DeterministicReplayResult replay) {
    final List<InternalAcceptanceFinding> mapped = new ArrayList<>();
    for (ReplayDifference difference : replay.differences()) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.REPLAY,
              InternalAcceptanceFinding.Severity.ERROR,
              difference.type().name(),
              difference.path(),
              difference.sanitizedMessage()));
    }
    if (replay.failureCode() != null) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.REPLAY,
              InternalAcceptanceFinding.Severity.BLOCKER,
              replay.failureCode().name(),
              replay.snapshotId(),
              "deterministic replay failed closed"));
    }
    return mapped;
  }

  private static List<InternalAcceptanceFinding> regressionFindings(
      final RegressionReportView regression) {
    final List<InternalAcceptanceFinding> mapped = new ArrayList<>();
    for (RegressionReportFindingView source : regression.findings()) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.REGRESSION,
              severity(source.severity()),
              source.findingCode(),
              source.evidenceRef(),
              source.findingMessage()));
    }
    if (regression.verdict() != RegressionVerdict.Status.PASS) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.REGRESSION,
              InternalAcceptanceFinding.Severity.ERROR,
              "REGRESSION_" + regression.verdict().name(),
              regression.verdictId(),
              "regression verdict did not pass"));
    }
    return mapped;
  }

  private static List<InternalAcceptanceFinding> readinessFindings(
      final ProviderReadinessEvaluationResult readiness) {
    final List<InternalAcceptanceFinding> mapped = new ArrayList<>();
    for (ProviderReadinessFinding source : readiness.findings()) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.PROVIDER_READINESS,
              severity(source.severity()),
              source.code(),
              source.summaryRef(),
              "provider readiness finding"));
    }
    if (readiness.decision() != ProviderReadinessDecision.READY) {
      mapped.add(
          finding(
              InternalAcceptanceFinding.Source.PROVIDER_READINESS,
              InternalAcceptanceFinding.Severity.ERROR,
              "PROVIDER_READINESS_" + readiness.decision().name(),
              readiness.providerRef(),
              "provider readiness evidence did not pass"));
    }
    return mapped;
  }

  private static List<String> observabilityRefs(
      final ModelGatewayObservabilityReport observability) {
    if (observability == null) {
      return List.of();
    }
    return List.of(
        "provider:" + observability.providerRef(),
        "gateway:" + observability.modelGatewayVersionRef(),
        "summary-hash:" + observability.providerSummaryHash(),
        "source:" + observability.sourceRef());
  }

  private static InternalAcceptanceFinding finding(
      final InternalAcceptanceFinding.Source source,
      final InternalAcceptanceFinding.Severity severity,
      final String code,
      final String safeRef,
      final String message) {
    return new InternalAcceptanceFinding(source, severity, code, safeRef, message);
  }

  private static InternalAcceptanceFinding.Severity severity(
      final DecisionEvidenceFinding.Severity severity) {
    return switch (severity) {
      case INFO -> InternalAcceptanceFinding.Severity.INFO;
      case WARN -> InternalAcceptanceFinding.Severity.WARN;
      case ERROR -> InternalAcceptanceFinding.Severity.ERROR;
      case BLOCKER -> InternalAcceptanceFinding.Severity.BLOCKER;
    };
  }

  private static InternalAcceptanceFinding.Severity severity(
      final com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity severity) {
    return switch (severity) {
      case INFO -> InternalAcceptanceFinding.Severity.INFO;
      case WARN -> InternalAcceptanceFinding.Severity.WARN;
      case ERROR -> InternalAcceptanceFinding.Severity.ERROR;
      case BLOCKER -> InternalAcceptanceFinding.Severity.BLOCKER;
    };
  }

  private static InternalAcceptanceFinding.Severity severity(
      final ProviderReadinessSeverity severity) {
    return switch (severity) {
      case INFO -> InternalAcceptanceFinding.Severity.INFO;
      case WARNING -> InternalAcceptanceFinding.Severity.WARN;
      case ERROR -> InternalAcceptanceFinding.Severity.ERROR;
      case BLOCKING -> InternalAcceptanceFinding.Severity.BLOCKER;
    };
  }

  private static InternalAcceptanceFinding.Severity severity(
      final ProviderReadinessAcceptanceStatus status) {
    return switch (status) {
      case PASS -> InternalAcceptanceFinding.Severity.INFO;
      case WARN -> InternalAcceptanceFinding.Severity.WARN;
      case FAIL -> InternalAcceptanceFinding.Severity.ERROR;
      case SKIPPED -> InternalAcceptanceFinding.Severity.BLOCKER;
    };
  }
}
