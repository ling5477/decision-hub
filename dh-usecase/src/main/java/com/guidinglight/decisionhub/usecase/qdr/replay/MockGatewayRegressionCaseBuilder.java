package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationPolicy;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * B3 mock gateway regression case builder。
 *
 * <p>Builder 把 existing dry-run / QDR decision artifact 和 mock gateway safe summary/ref
 * 转成 B2 repository 可保存的 replay case、evaluation case、expected/actual summary refs 与
 * verdict/finding commands。它只做本地 deterministic ID/hash/ref 计算，不调用 provider、HTTP、
 * Agent、LangGraph、NQ，也不生成 trading signal。
 */
public final class MockGatewayRegressionCaseBuilder {

    private static final String CASE_PREFIX = "qdr-b3-case:";
    private static final String EVALUATION_PREFIX = "qdr-b3-evaluation:";
    private static final String VERDICT_SUFFIX = "-verdict";

    /**
     * 构建 B3 regression case。
     *
     * @param command regression evaluation command。
     * @param verdict comparator 结果。
     * @return deterministic built case。
     */
    public BuiltRegressionCase build(
            final QdrRegressionEvaluationCommand command,
            final RegressionVerdict verdict) {
        final QdrRegressionEvaluationCommand checked = Objects.requireNonNull(command, "command");
        final RegressionVerdict checkedVerdict = Objects.requireNonNull(verdict, "verdict");
        final String identityHash = hash(
                checked.tenantId(),
                checked.traceId(),
                checked.requestId(),
                checked.decisionId(),
                checked.gatewayResult().gatewayCallRef());
        final String caseId = CASE_PREFIX + identityHash.substring(0, 24);
        final String evaluationId = EVALUATION_PREFIX + identityHash.substring(0, 24);
        final String verdictId = evaluationId + VERDICT_SUFFIX;
        final ReplayInputRef inputRef = new ReplayInputRef(
                "QDR_DECISION_ARTIFACT_REF",
                "decision-artifact:" + checked.decisionId(),
                hash(checked.tenantId(), checked.requestId(), checked.decisionId(), checked.traceId()));
        final ReplayOutputRef outputRef = new ReplayOutputRef(
                "MOCK_GATEWAY_SUMMARY_REF",
                checked.gatewayResult().gatewayCallRef(),
                checked.providerSummaryHash());
        final String expectedSummaryHash = summaryHash(checked.expectedSummary());
        final String actualSummaryHash = summaryHash(checked.actualSummary());
        final ReplayCase replayCase = new ReplayCase(
                checked.tenantId(),
                caseId,
                checked.decisionId(),
                checked.sourceRequestId(),
                checked.traceId(),
                inputRef,
                checked.expectedSummary(),
                toEvaluationPolicy(checked.policy()),
                checked.createdAt());
        final EvaluationCase evaluationCase = new EvaluationCase(
                checked.tenantId(),
                evaluationId,
                caseId,
                checked.policy().policyVersion(),
                checked.gatewayResult().modelVersionId(),
                checked.modelGatewayVersionRef(),
                checked.expectedSummary(),
                checked.actualSummary(),
                checkedVerdict,
                outputRef);
        return new BuiltRegressionCase(
                replayCase,
                evaluationCase,
                inputRef,
                outputRef,
                deterministicUuid(checked, "replay-case"),
                deterministicUuid(checked, "evaluation-case"),
                deterministicUuid(checked, "input-ref"),
                deterministicUuid(checked, "output-ref"),
                deterministicUuid(checked, "expected-summary"),
                deterministicUuid(checked, "actual-summary"),
                deterministicUuid(checked, "verdict"),
                caseId,
                evaluationId,
                verdictId,
                expectedSummaryHash,
                actualSummaryHash,
                hash(caseId, expectedSummaryHash, checked.policy().policyVersion()),
                hash(evaluationId, expectedSummaryHash, actualSummaryHash, checked.providerSummaryHash()),
                checked.createdAt());
    }

    private static EvaluationPolicy toEvaluationPolicy(final RegressionBaselinePolicy policy) {
        return new EvaluationPolicy(
                policy.policyVersion(),
                policy.confidenceTolerance(),
                0,
                EvaluationPolicy.RequiredEvidenceMode.STRICT,
                false);
    }

    private static String summaryHash(final ExpectedDecisionSummary summary) {
        return hash(
                summary.decisionType(),
                summary.actionLabel(),
                summary.confidenceBand(),
                summary.riskLevel().name(),
                String.join("|", summary.requiredEvidenceRefs()),
                String.join("|", summary.forbiddenActions()));
    }

    private static UUID deterministicUuid(
            final QdrRegressionEvaluationCommand command, final String role) {
        return UUID.nameUUIDFromBytes(
                ("qdr-b3|"
                        + command.tenantId()
                        + "|"
                        + command.requestId()
                        + "|"
                        + command.decisionId()
                        + "|"
                        + command.gatewayResult().gatewayCallRef()
                        + "|"
                        + role)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(final String... parts) {
        return PromptModelSafetyRules.sha256Hex(List.of(parts));
    }

    /**
     * Builder 产出的 replay/evaluation/verdict persistence material。
     *
     * <p>所有 ID/hash/ref 均 deterministic 且 tenant-bound。`to*Command` 方法只生成 B2
     * repository command，不直接写数据库，不执行 replay 或 provider 调用。
     *
     * @param replayCase          replay case。
     * @param evaluationCase      evaluation case。
     * @param inputRef            input ref。
     * @param outputRef           output ref。
     * @param replayCaseId        replay case 主键。
     * @param evaluationCaseId    evaluation case 主键。
     * @param inputRefId          input ref 主键。
     * @param outputRefId         output ref 主键。
     * @param expectedSummaryId   expected summary 主键。
     * @param actualSummaryId     actual summary 主键。
     * @param verdictRecordId     verdict 主键。
     * @param caseId              case ID。
     * @param evaluationId        evaluation ID。
     * @param verdictId           verdict ID。
     * @param expectedSummaryHash expected summary hash。
     * @param actualSummaryHash   actual summary hash。
     * @param caseChecksum        replay case checksum。
     * @param evaluationChecksum  evaluation checksum。
     * @param createdAt           创建时间。
     */
    public record BuiltRegressionCase(
            ReplayCase replayCase,
            EvaluationCase evaluationCase,
            ReplayInputRef inputRef,
            ReplayOutputRef outputRef,
            UUID replayCaseId,
            UUID evaluationCaseId,
            UUID inputRefId,
            UUID outputRefId,
            UUID expectedSummaryId,
            UUID actualSummaryId,
            UUID verdictRecordId,
            String caseId,
            String evaluationId,
            String verdictId,
            String expectedSummaryHash,
            String actualSummaryHash,
            String caseChecksum,
            String evaluationChecksum,
            Instant createdAt) {

        /**
         * 校验 built case 的安全对象。
         */
        public BuiltRegressionCase {
            replayCase = Objects.requireNonNull(replayCase, "replayCase");
            evaluationCase = Objects.requireNonNull(evaluationCase, "evaluationCase");
            inputRef = ReplayPersistenceGuard.requireInputRef(inputRef);
            outputRef = ReplayPersistenceGuard.requireOutputRef(outputRef);
            replayCaseId = ReplayPersistenceGuard.requireUuid(replayCaseId, "replayCaseId");
            evaluationCaseId =
                    ReplayPersistenceGuard.requireUuid(evaluationCaseId, "evaluationCaseId");
            inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
            outputRefId = ReplayPersistenceGuard.requireUuid(outputRefId, "outputRefId");
            expectedSummaryId =
                    ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
            actualSummaryId = ReplayPersistenceGuard.requireUuid(actualSummaryId, "actualSummaryId");
            verdictRecordId = ReplayPersistenceGuard.requireUuid(verdictRecordId, "verdictRecordId");
            caseId = ReplayPersistenceGuard.requireSafeText(caseId, "caseId");
            evaluationId = ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId");
            verdictId = ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId");
            expectedSummaryHash =
                    ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
            actualSummaryHash =
                    ReplayPersistenceGuard.requireSha256Hex(actualSummaryHash, "actualSummaryHash");
            caseChecksum = ReplayPersistenceGuard.requireSha256Hex(caseChecksum, "caseChecksum");
            evaluationChecksum =
                    ReplayPersistenceGuard.requireSha256Hex(evaluationChecksum, "evaluationChecksum");
            createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        }

        /**
         * @param requestId              requestId。
         * @param modelGatewayVersionRef model gateway version ref。
         * @return replay case save command。
         */
        public SaveReplayCaseCommand toReplayCommand(
                final String requestId, final String modelGatewayVersionRef) {
            return new SaveReplayCaseCommand(
                    replayCaseId,
                    inputRefId,
                    expectedSummaryId,
                    replayCase,
                    requestId,
                    modelGatewayVersionRef,
                    expectedSummaryHash,
                    caseChecksum,
                    createdAt);
        }

        /**
         * @param sourceDecisionId source decision ID。
         * @param sourceRequestId  source request ID。
         * @param traceId          traceId。
         * @param requestId        requestId。
         * @return evaluation case save command。
         */
        public SaveEvaluationCaseCommand toEvaluationCommand(
                final String sourceDecisionId,
                final String sourceRequestId,
                final String traceId,
                final String requestId) {
            return new SaveEvaluationCaseCommand(
                    evaluationCaseId,
                    inputRefId,
                    outputRefId,
                    expectedSummaryId,
                    actualSummaryId,
                    evaluationCase,
                    sourceDecisionId,
                    sourceRequestId,
                    traceId,
                    requestId,
                    inputRef,
                    expectedSummaryHash,
                    actualSummaryHash,
                    evaluationChecksum,
                    createdAt,
                    createdAt);
        }

        /**
         * @param command 原始 evaluation command。
         * @return verdict save command。
         */
        public SaveRegressionVerdictCommand toVerdictCommand(
                final QdrRegressionEvaluationCommand command) {
            final RegressionVerdict verdict = evaluationCase.verdict();
            return new SaveRegressionVerdictCommand(
                    verdictRecordId,
                    command.tenantId(),
                    caseId,
                    evaluationId,
                    verdictId,
                    command.decisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    command.policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    inputRefId,
                    outputRefId,
                    expectedSummaryId,
                    actualSummaryId,
                    expectedSummaryHash,
                    actualSummaryHash,
                    verdict,
                    aggregateSeverity(verdict),
                    firstFinding(verdict).map(RegressionFinding::code).orElse(null),
                    firstFinding(verdict).map(RegressionFinding::message).orElse(null),
                    createdAt,
                    createdAt);
        }

        /**
         * @param command 原始 evaluation command。
         * @return finding save commands。
         */
        public List<SaveRegressionFindingCommand> toFindingCommands(
                final QdrRegressionEvaluationCommand command) {
            final RegressionVerdict verdict = evaluationCase.verdict();
            return verdict.findings().stream()
                    .map(finding -> toFindingCommand(command, finding))
                    .toList();
        }

        private SaveRegressionFindingCommand toFindingCommand(
                final QdrRegressionEvaluationCommand command, final RegressionFinding finding) {
            return new SaveRegressionFindingCommand(
                    UUID.nameUUIDFromBytes(
                            ("qdr-b3|"
                                    + command.tenantId()
                                    + "|"
                                    + verdictId
                                    + "|"
                                    + finding.code()
                                    + "|"
                                    + finding.evidenceRef())
                                    .getBytes(StandardCharsets.UTF_8)),
                    command.tenantId(),
                    caseId,
                    evaluationId,
                    verdictId,
                    command.decisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    command.policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    inputRefId,
                    outputRefId,
                    expectedSummaryId,
                    actualSummaryId,
                    expectedSummaryHash,
                    actualSummaryHash,
                    evaluationCase.verdict().status(),
                    finding.severity(),
                    finding.code(),
                    finding.message(),
                    finding.evidenceRef(),
                    createdAt,
                    createdAt);
        }

        private static RegressionSeverity aggregateSeverity(final RegressionVerdict verdict) {
            if (verdict.findings().isEmpty()) {
                return verdict.status() == RegressionVerdict.Status.PASS
                        ? RegressionSeverity.INFO
                        : RegressionSeverity.WARN;
            }
            return verdict.findings().stream()
                    .map(RegressionFinding::severity)
                    .max(Comparator.comparingInt(Enum::ordinal))
                    .orElse(RegressionSeverity.INFO);
        }

        private static java.util.Optional<RegressionFinding> firstFinding(
                final RegressionVerdict verdict) {
            return verdict.findings().stream().findFirst();
        }
    }
}
