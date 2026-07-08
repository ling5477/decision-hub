package com.guidinglight.decisionhub.usecase.qdr.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionEvidenceRef;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Stage-qdr-4 B3 regression evaluation service 回归测试。
 *
 * <p>测试只使用内存 B2 repository port，验证 deterministic mock gateway regression flow；
 * 不启动数据库、不调用 provider/HTTP/NQ，不启动 Agent 或 LangGraph。
 */
final class QdrRegressionEvaluationServiceTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String OTHER_TENANT_ID = "tenant-b";
    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");

    @Test
    void mockGatewayOutputCreatesReplayEvaluationAndVerdictThroughRepositories() {
        final InMemoryReplayCaseRepository replayRepository = new InMemoryReplayCaseRepository();
        final InMemoryEvaluationCaseRepository evaluationRepository =
                new InMemoryEvaluationCaseRepository();
        final InMemoryRegressionVerdictRepository verdictRepository =
                new InMemoryRegressionVerdictRepository();
        final QdrRegressionEvaluationService service =
                service(replayRepository, evaluationRepository, verdictRepository);

        final QdrRegressionEvaluationResult result = service.evaluate(command(
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A));

        assertEquals(RegressionVerdict.Status.PASS, result.verdict().status());
        assertFalse(result.persistenceBlocked());
        assertNotNull(result.replayCaseRecord());
        assertNotNull(result.evaluationCaseRecord());
        assertNotNull(result.regressionVerdictRecord());
        assertEquals(1, replayRepository.saved.size());
        assertEquals(1, evaluationRepository.saved.size());
        assertEquals(1, verdictRepository.saved.size());
        assertEquals(result.replayCaseRecord().caseId(), result.evaluationCaseRecord().caseId());
        assertEquals(RegressionVerdict.Status.PASS, result.regressionVerdictRecord().verdict());
    }

    @Test
    void expectedSummaryCanCreateRegressionVerdictAndFinding() {
        final InMemoryReplayCaseRepository replayRepository = new InMemoryReplayCaseRepository();
        final InMemoryEvaluationCaseRepository evaluationRepository =
                new InMemoryEvaluationCaseRepository();
        final InMemoryRegressionVerdictRepository verdictRepository =
                new InMemoryRegressionVerdictRepository();
        final QdrRegressionEvaluationService service =
                service(replayRepository, evaluationRepository, verdictRepository);

        final QdrRegressionEvaluationResult result = service.evaluate(command(
                summary("LONG_BIAS", "LOW", RiskLevel.LOW, evidence(), forbidden()),
                summary("LONG_BIAS", "HIGH", RiskLevel.HIGH, evidence(), forbidden()),
                HASH_A));

        assertEquals(RegressionVerdict.Status.FAIL, result.verdict().status());
        assertFalse(result.findingRecords().isEmpty());
        assertFindingRecordCodes(
                result, "CONFIDENCE_DRIFT_BEYOND_TOLERANCE", "RISK_LEVEL_INCREASED");
    }

    @Test
    void repositorySaveFailureReturnsFailBlocked() {
        final InMemoryReplayCaseRepository replayRepository = new InMemoryReplayCaseRepository();
        replayRepository.failOnSave = true;
        final QdrRegressionEvaluationService service =
                service(
                        replayRepository,
                        new InMemoryEvaluationCaseRepository(),
                        new InMemoryRegressionVerdictRepository());

        final QdrRegressionEvaluationResult result = service.evaluate(command(
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A));

        assertTrue(result.persistenceBlocked());
        assertEquals(RegressionVerdict.Status.FAIL, result.verdict().status());
        assertTrue(result.verdict().findings().stream()
                .map(RegressionFinding::severity)
                .anyMatch(RegressionSeverity.BLOCKER::equals));
    }

    @Test
    void crossTenantRegressionReadReturnsEmpty() {
        final InMemoryRegressionVerdictRepository verdictRepository =
                new InMemoryRegressionVerdictRepository();
        final QdrRegressionEvaluationService service =
                service(
                        new InMemoryReplayCaseRepository(),
                        new InMemoryEvaluationCaseRepository(),
                        verdictRepository);

        final QdrRegressionEvaluationResult result = service.evaluate(command(
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A));

        assertTrue(verdictRepository
                .findByVerdictId(OTHER_TENANT_ID, result.regressionVerdictRecord().verdictId())
                .isEmpty());
    }

    @Test
    void rawPromptAndRawProviderResponseAreRejectedBeforePersistence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> commandWithGatewaySummary("rawPrompt should not pass"));
        assertThrows(
                IllegalArgumentException.class,
                () -> commandWithGatewaySummary("rawProviderResponse should not pass"));
    }

    @Test
    void credentialLikeEvidenceRefIsRejectedBeforePersistence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegressionEvidenceRef(
                        "MOCK_GATEWAY_SUMMARY",
                        "apiKey-ref",
                        HASH_A,
                        "ACTUAL_SUMMARY"));
    }

    @Test
    void credentialLikeJsonKeyFailsClosedBeforePersistence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReplayPersistenceGuard.rejectUnsafeJson(
                        Map.of("apiKey", "redacted-value"), "regression.case"));
    }

    @Test
    void providerHttpAgentAndLangGraphClassesAreNotIntroduced() throws IOException {
        final Path projectRoot = projectRoot();
        final List<Path> roots = List.of(
                projectRoot.resolve("dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay"),
                projectRoot.resolve("dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay"));
        final String forbiddenTerms =
                "RealClient|HttpClient|WebClient|RestTemplate|OkHttp|LangGraph|AutoGen|CrewAI";

        for (Path root : roots) {
            try (Stream<Path> files = Files.walk(root)) {
                final List<Path> offenders = files
                        .filter(Files::isRegularFile)
                        .filter(path -> containsForbiddenRuntimeTerm(path, forbiddenTerms))
                        .toList();
                assertTrue(offenders.isEmpty(), offenders.toString());
            }
        }
    }

    private static QdrRegressionEvaluationService service(
            final ReplayCaseRepository replayRepository,
            final EvaluationCaseRepository evaluationRepository,
            final RegressionVerdictRepository verdictRepository) {
        return new QdrRegressionEvaluationService(
                replayRepository,
                evaluationRepository,
                verdictRepository,
                new MockGatewayRegressionCaseBuilder(),
                new QdrRegressionComparator());
    }

    private static QdrRegressionEvaluationCommand command(
            final ExpectedDecisionSummary expected,
            final ExpectedDecisionSummary actual,
            final String providerSummaryHash) {
        return new QdrRegressionEvaluationCommand(
                TENANT_ID,
                "trace-a",
                "request-a",
                "decision-a",
                "source-request-a",
                "mock-gateway:v1",
                "prompt:v1",
                providerSummaryHash,
                gatewayResult("mock gateway redacted summary"),
                expected,
                actual,
                List.of(new RegressionEvidenceRef(
                        "MOCK_GATEWAY_SUMMARY",
                        "gateway-call-a",
                        providerSummaryHash,
                        "ACTUAL_SUMMARY")),
                policy(providerSummaryHash),
                NOW);
    }

    private static QdrRegressionEvaluationCommand commandWithGatewaySummary(final String summary) {
        return new QdrRegressionEvaluationCommand(
                TENANT_ID,
                "trace-a",
                "request-a",
                "decision-a",
                "source-request-a",
                "mock-gateway:v1",
                "prompt:v1",
                HASH_A,
                gatewayResult(summary),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                List.of(new RegressionEvidenceRef(
                        "MOCK_GATEWAY_SUMMARY",
                        "gateway-call-a",
                        HASH_A,
                        "ACTUAL_SUMMARY")),
                policy(HASH_A),
                NOW);
    }

    private static QdrModelGatewayIntegrationResult gatewayResult(final String redactedSummary) {
        return new QdrModelGatewayIntegrationResult(
                "prompt:v1",
                "model:v1",
                "provider:mock",
                "gateway-call-a",
                "ALLOWED",
                "PASSED",
                "input=1,rendered=1,output=1,estimated=1,memory=0",
                redactedSummary,
                "trace:model-gateway",
                "audit:model-gateway");
    }

    private static RegressionBaselinePolicy policy(final String providerSummaryHash) {
        return new RegressionBaselinePolicy(
                "policy-v1",
                new BigDecimal("0.10"),
                true,
                true,
                true,
                true,
                true,
                "mock-gateway:v1",
                "prompt:v1",
                providerSummaryHash);
    }

    private static ExpectedDecisionSummary summary(
            final String actionLabel,
            final String confidenceBand,
            final RiskLevel riskLevel,
            final List<String> evidenceRefs,
            final List<String> forbiddenActions) {
        return new ExpectedDecisionSummary(
                "READ_ONLY_RECOMMENDATION",
                actionLabel,
                confidenceBand,
                riskLevel,
                evidenceRefs,
                forbiddenActions);
    }

    private static List<String> evidence() {
        return List.of("gateway-call-ref", "decision-run-ref");
    }

    private static List<String> forbidden() {
        return List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE");
    }

    private static void assertFindingRecordCodes(
            final QdrRegressionEvaluationResult result, final String... codes) {
        final List<String> actualCodes = result.findingRecords().stream()
                .map(RegressionFindingRecord::findingCode)
                .toList();
        for (String code : codes) {
            assertTrue(actualCodes.contains(code), actualCodes.toString());
        }
    }

    private static Path projectRoot() {
        final Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if ("dh-usecase".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.getParent();
        }
        return workingDirectory;
    }

    private static boolean containsForbiddenRuntimeTerm(
            final Path path, final String forbiddenTerms) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("*"))
                    .filter(line -> !line.startsWith("//"))
                    .filter(line -> !line.startsWith("/*"))
                    .anyMatch(line -> line.matches(".*(" + forbiddenTerms + ").*"));
        } catch (final IOException error) {
            throw new AssertionError("failed to inspect " + path, error);
        }
    }

    private static final class InMemoryReplayCaseRepository implements ReplayCaseRepository {
        private final Map<String, ReplayCaseRecord> saved = new LinkedHashMap<>();
        private boolean failOnSave;

        @Override
        public ReplayCaseRecord save(final SaveReplayCaseCommand command) {
            if (failOnSave) {
                throw new ReplayPersistenceException("synthetic replay save failure");
            }
            final ReplayCaseRecord record = new ReplayCaseRecord(
                    command.replayCaseId(),
                    command.tenantId(),
                    command.caseId(),
                    command.replayCase().sourceDecisionId(),
                    command.replayCase().sourceRequestId(),
                    command.replayCase().traceId(),
                    command.requestId(),
                    command.replayCase().policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.expectedSummaryId(),
                    command.replayCase().inputRef(),
                    command.replayCase().expectedSummary(),
                    command.expectedSummaryHash(),
                    command.caseChecksum(),
                    command.replayCase().createdAt(),
                    command.updatedAt());
            saved.put(key(command.tenantId(), command.caseId()), record);
            return record;
        }

        @Override
        public Optional<ReplayCaseRecord> findById(final String tenantId, final UUID id) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId) && record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<ReplayCaseRecord> findByCaseId(final String tenantId, final String caseId) {
            return Optional.ofNullable(saved.get(key(tenantId, caseId)));
        }

        @Override
        public List<ReplayCaseRecord> listByTraceId(
                final String tenantId, final String traceId, final int limit, final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.traceId().equals(traceId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ReplayCaseRecord> listBySourceRequestId(
                final String tenantId,
                final String sourceRequestId,
                final int limit,
                final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.sourceRequestId().equals(sourceRequestId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ReplayCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryEvaluationCaseRepository implements EvaluationCaseRepository {
        private final Map<String, EvaluationCaseRecord> saved = new LinkedHashMap<>();

        @Override
        public EvaluationCaseRecord save(final SaveEvaluationCaseCommand command) {
            final RegressionVerdict verdict = command.evaluationCase().verdict();
            final EvaluationCaseRecord record = new EvaluationCaseRecord(
                    command.evaluationCaseId(),
                    command.tenantId(),
                    command.evaluationCase().caseId(),
                    command.evaluationId(),
                    command.evaluationId() + "-verdict",
                    command.sourceDecisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    command.evaluationCase().policyVersion(),
                    command.evaluationCase().modelVersionRef(),
                    command.evaluationCase().modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.outputRefId(),
                    command.expectedSummaryId(),
                    command.actualSummaryId(),
                    command.inputRef(),
                    command.evaluationCase().outputRef(),
                    command.evaluationCase().expectedSummary(),
                    command.evaluationCase().actualSummary(),
                    command.expectedSummaryHash(),
                    command.actualSummaryHash(),
                    verdict.status(),
                    aggregateSeverity(verdict),
                    firstFinding(verdict).map(RegressionFinding::code).orElse(null),
                    firstFinding(verdict).map(RegressionFinding::message).orElse(null),
                    command.evaluationChecksum(),
                    command.createdAt(),
                    command.updatedAt());
            saved.put(key(command.tenantId(), command.evaluationId()), record);
            return record;
        }

        @Override
        public Optional<EvaluationCaseRecord> findById(final String tenantId, final UUID id) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId) && record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<EvaluationCaseRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return Optional.ofNullable(saved.get(key(tenantId, evaluationId)));
        }

        @Override
        public List<EvaluationCaseRecord> listByCaseId(
                final String tenantId, final String caseId, final int limit, final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.caseId().equals(caseId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<EvaluationCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class InMemoryRegressionVerdictRepository
            implements RegressionVerdictRepository {
        private final Map<String, RegressionVerdictRecord> saved = new LinkedHashMap<>();
        private final List<RegressionFindingRecord> findings = new ArrayList<>();

        @Override
        public RegressionVerdictRecord save(final SaveRegressionVerdictCommand command) {
            final RegressionVerdictRecord record = new RegressionVerdictRecord(
                    command.verdictRecordId(),
                    command.tenantId(),
                    command.caseId(),
                    command.evaluationId(),
                    command.verdictId(),
                    command.sourceDecisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    command.policyVersion(),
                    command.modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.outputRefId(),
                    command.expectedSummaryId(),
                    command.actualSummaryId(),
                    command.expectedSummaryHash(),
                    command.actualSummaryHash(),
                    command.verdict().status(),
                    command.severity(),
                    command.findingCode(),
                    command.findingMessage(),
                    command.verdict().failureReason(),
                    command.createdAt(),
                    command.updatedAt());
            saved.put(key(command.tenantId(), command.verdictId()), record);
            return record;
        }

        @Override
        public List<RegressionFindingRecord> saveFindings(
                final String tenantId,
                final String verdictId,
                final List<SaveRegressionFindingCommand> commands) {
            final List<RegressionFindingRecord> records = commands.stream()
                    .map(command -> new RegressionFindingRecord(
                            command.findingId(),
                            command.tenantId(),
                            command.caseId(),
                            command.evaluationId(),
                            command.verdictId(),
                            command.sourceDecisionId(),
                            command.sourceRequestId(),
                            command.traceId(),
                            command.requestId(),
                            command.policyVersion(),
                            command.modelGatewayVersionRef(),
                            command.inputRefId(),
                            command.outputRefId(),
                            command.expectedSummaryId(),
                            command.actualSummaryId(),
                            command.expectedSummaryHash(),
                            command.actualSummaryHash(),
                            command.verdict(),
                            command.severity(),
                            command.findingCode(),
                            command.findingMessage(),
                            command.evidenceRef(),
                            command.createdAt(),
                            command.updatedAt()))
                    .toList();
            findings.addAll(records);
            return records;
        }

        @Override
        public Optional<RegressionVerdictRecord> findById(final String tenantId, final UUID id) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId) && record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<RegressionVerdictRecord> findByVerdictId(
                final String tenantId, final String verdictId) {
            return Optional.ofNullable(saved.get(key(tenantId, verdictId)));
        }

        @Override
        public Optional<RegressionVerdictRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.evaluationId().equals(evaluationId))
                    .findFirst();
        }

        @Override
        public List<RegressionFindingRecord> listFindingsByVerdictId(
                final String tenantId, final String verdictId, final int limit, final int offset) {
            return findings.stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.verdictId().equals(verdictId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<RegressionVerdictRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return saved.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
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

    private static Optional<RegressionFinding> firstFinding(final RegressionVerdict verdict) {
        return verdict.findings().stream().findFirst();
    }

    private static String key(final String tenantId, final String id) {
        return tenantId + "|" + id;
    }
}
