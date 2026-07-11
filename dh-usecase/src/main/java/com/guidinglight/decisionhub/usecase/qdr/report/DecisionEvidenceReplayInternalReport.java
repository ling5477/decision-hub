package com.guidinglight.decisionhub.usecase.qdr.report;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessAcceptanceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayDifference;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayReproducibilityStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-6 B4 evidence/replay internal acceptance report。
 *
 * <p>Report 只整合既有 structured safe contracts，不保存 raw payload，不持久化，不触发 replay assembly、Provider、
 * HTTP、NQ、Agent、LangGraph 或交易行为。所有 refs/findings/differences 均稳定排序。
 */
public record DecisionEvidenceReplayInternalReport(
    DecisionEvidenceCorrelation correlation,
    DecisionEvidenceStatus evidenceStatus,
    List<String> evidenceRefs,
    List<DecisionEvidencePolicy.EvidenceType> missingMandatoryEvidence,
    ReplayReproducibilityStatus replayStatus,
    List<ReplayDifference> replayDifferences,
    ReplayFailureCode replayFailureCode,
    RegressionVerdict.Status regressionVerdict,
    RegressionSeverity regressionSeverity,
    ProviderReadinessDecision providerReadinessDecision,
    ProviderReadinessStatus providerReadinessStatus,
    ProviderReadinessAcceptanceStatus observabilityAcceptanceStatus,
    List<String> observabilityRefs,
    InternalAcceptanceStatus acceptanceStatus,
    List<InternalAcceptanceFinding> findings,
    String safetyDeclaration) {

  /** Report 强制携带的 non-authorization 声明。 */
  public static final String INTERNAL_ACCEPTANCE_ONLY =
      "internal evidence acceptance only; not provider authorization; not NQ integration permission; not trading or execution permission; not Paper or LIVE permission";

  /** 校验状态不变量、复制集合并形成稳定顺序。 */
  public DecisionEvidenceReplayInternalReport {
    correlation = Objects.requireNonNull(correlation, "correlation");
    evidenceRefs = safeSortedTexts(evidenceRefs, "evidenceRefs");
    missingMandatoryEvidence =
        Objects.requireNonNullElse(missingMandatoryEvidence, List.<DecisionEvidencePolicy.EvidenceType>of())
            .stream()
            .map(value -> Objects.requireNonNull(value, "missingMandatoryEvidence"))
            .distinct()
            .sorted()
            .toList();
    replayDifferences =
        Objects.requireNonNullElse(replayDifferences, List.<ReplayDifference>of()).stream()
            .map(value -> Objects.requireNonNull(value, "replayDifference"))
            .sorted(Comparator.comparing(ReplayDifference::type).thenComparing(ReplayDifference::path))
            .toList();
    observabilityRefs = safeSortedTexts(observabilityRefs, "observabilityRefs");
    acceptanceStatus = Objects.requireNonNull(acceptanceStatus, "acceptanceStatus");
    findings =
        Objects.requireNonNullElse(findings, List.<InternalAcceptanceFinding>of()).stream()
            .map(value -> Objects.requireNonNull(value, "finding"))
            .sorted(InternalAcceptanceFinding.STABLE_ORDER)
            .toList();
    if (!INTERNAL_ACCEPTANCE_ONLY.equals(safetyDeclaration)) {
      throw new IllegalArgumentException("safetyDeclaration must preserve the frozen boundary");
    }
    validateAcceptedState(
        acceptanceStatus,
        evidenceStatus,
        missingMandatoryEvidence,
        replayStatus,
        replayDifferences,
        replayFailureCode,
        regressionVerdict,
        providerReadinessDecision,
        providerReadinessStatus,
        observabilityAcceptanceStatus);
  }

  private static List<String> safeSortedTexts(final List<String> values, final String field) {
    return Objects.requireNonNullElse(values, List.<String>of()).stream()
        .map(value -> InternalAcceptanceFinding.requireSafeText(value, field))
        .distinct()
        .sorted()
        .toList();
  }

  private static void validateAcceptedState(
      final InternalAcceptanceStatus acceptanceStatus,
      final DecisionEvidenceStatus evidenceStatus,
      final List<DecisionEvidencePolicy.EvidenceType> missingMandatoryEvidence,
      final ReplayReproducibilityStatus replayStatus,
      final List<ReplayDifference> replayDifferences,
      final ReplayFailureCode replayFailureCode,
      final RegressionVerdict.Status regressionVerdict,
      final ProviderReadinessDecision readinessDecision,
      final ProviderReadinessStatus readinessStatus,
      final ProviderReadinessAcceptanceStatus observabilityStatus) {
    if (acceptanceStatus != InternalAcceptanceStatus.ACCEPTED) {
      return;
    }
    if (evidenceStatus != DecisionEvidenceStatus.COMPLETE
        || !missingMandatoryEvidence.isEmpty()
        || replayStatus != ReplayReproducibilityStatus.REPRODUCIBLE
        || !replayDifferences.isEmpty()
        || replayFailureCode != null
        || regressionVerdict != RegressionVerdict.Status.PASS
        || readinessDecision != ProviderReadinessDecision.READY
        || readinessStatus != ProviderReadinessStatus.READY
        || observabilityStatus != ProviderReadinessAcceptanceStatus.PASS) {
      throw new IllegalArgumentException("ACCEPTED requires every frozen evidence gate to pass");
    }
  }
}
