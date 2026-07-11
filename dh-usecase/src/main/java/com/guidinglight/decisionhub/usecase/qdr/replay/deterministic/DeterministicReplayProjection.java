package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.ReplayInputSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** QDR6-MOCK-REPLAY-1 的纯结构化、无 audit/DB-ID/环境字段 projection。 */
record DeterministicReplayProjection(
    Map<String, Object> context,
    String policyVersion,
    String promptVersion,
    String modelVersion,
    String gatewayVersion,
    Map<String, Object> expectedSummary,
    List<Map<String, Object>> evidenceRefs,
    String replayInputHash,
    String expectedSummaryHash,
    String providerSummaryHash) {

  DeterministicReplayProjection {
    context = Map.copyOf(Objects.requireNonNull(context, "context"));
    policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
    promptVersion = ReplayPersistenceGuard.requireSafeText(promptVersion, "promptVersion");
    modelVersion = ReplayPersistenceGuard.requireSafeText(modelVersion, "modelVersion");
    gatewayVersion = ReplayPersistenceGuard.requireSafeText(gatewayVersion, "gatewayVersion");
    expectedSummary = Map.copyOf(Objects.requireNonNull(expectedSummary, "expectedSummary"));
    evidenceRefs = List.copyOf(Objects.requireNonNull(evidenceRefs, "evidenceRefs"));
    if (evidenceRefs.isEmpty()) {
      throw new IllegalArgumentException("evidenceRefs must not be empty");
    }
    replayInputHash = ReplayPersistenceGuard.requireSha256Hex(replayInputHash, "replayInputHash");
    expectedSummaryHash =
        ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
    providerSummaryHash =
        ReplayPersistenceGuard.optionalSha256Hex(providerSummaryHash, "providerSummaryHash");
  }

  static DeterministicReplayProjection fromSnapshot(
      final ReplayInputSnapshot snapshot, final CanonicalReplaySnapshotIdentity identity) {
    final ReplayInputSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
    final CanonicalReplaySnapshotIdentity checkedIdentity =
        Objects.requireNonNull(identity, "identity");
    final Map<String, Object> canonical = checked.canonicalValue();
    final Map<String, Object> context =
        new LinkedHashMap<>(map(canonical.get("contextSnapshot"), "contextSnapshot"));
    context.put("replayCaseId", checkedIdentity.replayCaseId());
    if (checkedIdentity.evaluationCaseId() != null) {
      context.put("evaluationCaseId", checkedIdentity.evaluationCaseId());
    }
    if (checkedIdentity.regressionVerdictId() != null) {
      context.put("regressionVerdictId", checkedIdentity.regressionVerdictId());
    }
    return new DeterministicReplayProjection(
        context,
        checked.policyVersion(),
        checked.promptVersionRef(),
        checked.modelVersionRef(),
        checked.modelGatewayVersionRef(),
        map(canonical.get("expectedDecisionSummary"), "expectedDecisionSummary"),
        maps(canonical.get("evidenceRefs")),
        checked.replayInputHash(),
        checked.expectedSummaryHash(),
        checked.providerSummaryHash());
  }

  Map<String, Object> canonicalValue() {
    final Map<String, Object> value = new LinkedHashMap<>();
    value.put("context", context);
    value.put("policyVersion", policyVersion);
    value.put("promptVersion", promptVersion);
    value.put("modelVersion", modelVersion);
    value.put("gatewayVersion", gatewayVersion);
    value.put("expectedSummary", expectedSummary);
    value.put("evidenceRefs", evidenceRefs);
    value.put("replayInputHash", replayInputHash);
    value.put("expectedSummaryHash", expectedSummaryHash);
    if (providerSummaryHash != null) {
      value.put("providerSummaryHash", providerSummaryHash);
    }
    return Map.copyOf(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(final Object value, final String field) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException(field + " must be a structured object");
    }
    return (Map<String, Object>) map;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> maps(final Object value) {
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException("evidenceRefs must be a structured array");
    }
    return (List<Map<String, Object>>) list;
  }
}
