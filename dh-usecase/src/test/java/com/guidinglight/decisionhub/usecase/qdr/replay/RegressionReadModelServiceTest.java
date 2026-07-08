package com.guidinglight.decisionhub.usecase.qdr.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Stage-QDR-4 B4 regression read model service 回归测试。
 *
 * <p>测试只使用内存 B2 repository port，验证 internal report/read model；不新增 API/Controller，
 * 不启动数据库，不调用 provider/HTTP/NQ，不启动 Agent 或 LangGraph，也不生成 trading signal。
 */
final class RegressionReadModelServiceTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String CASE_ID = "qdr-b4-case-a";
    private static final String EVALUATION_ID = "qdr-b4-evaluation-a";
    private static final String VERDICT_ID = "qdr-b4-evaluation-a-verdict";
    private static final String TRACE_ID = "trace-a";
    private static final String SOURCE_REQUEST_ID = "source-request-a";
    private static final String SOURCE_DECISION_ID = "source-decision-a";
    private static final String REQUEST_ID = "request-a";
    private static final String POLICY_VERSION = "policy-v1";
    private static final String MODEL_GATEWAY_VERSION_REF = "mock-gateway:v1";
    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String HASH_C =
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");

    @Test
    void reportQueryByTenantAndCaseIdSucceeds() {
        final Fixture fixture = seededFixture();

        final List<RegressionReportView> reports =
                fixture.service.findReports(query(CASE_ID, null, null, null, null, null));

        assertEquals(1, reports.size());
        final RegressionReportView report = reports.getFirst();
        assertEquals(CASE_ID, report.caseId());
        assertEquals(EVALUATION_ID, report.evaluationId());
        assertEquals(VERDICT_ID, report.verdictId());
        assertEquals(RegressionVerdict.Status.PASS, report.verdict());
    }

    @Test
    void reportQueryByTenantAndEvaluationIdSucceeds() {
        final Fixture fixture = seededFixture();

        final List<RegressionReportView> reports =
                fixture.service.findReports(query(null, EVALUATION_ID, null, null, null, null));

        assertEquals(1, reports.size());
        assertEquals(EVALUATION_ID, reports.getFirst().evaluationId());
        assertEquals("READ_ONLY_RECOMMENDATION", reports.getFirst().decisionType());
    }

    @Test
    void reportQueryByTenantAndVerdictIdSucceedsWithFindings() {
        final Fixture fixture = new Fixture();
        fixture.seed(
                TENANT_A,
                CASE_ID,
                EVALUATION_ID,
                VERDICT_ID,
                List.of(finding(
                        TENANT_A,
                        CASE_ID,
                        EVALUATION_ID,
                        VERDICT_ID,
                        "MODEL_GATEWAY_VERSION_REF_MISMATCH")));

        final List<RegressionReportView> reports =
                fixture.service.findReports(query(null, null, VERDICT_ID, null, null, null));

        assertEquals(1, reports.size());
        assertEquals(1, reports.getFirst().findings().size());
        assertEquals(
                RegressionDriftSummary.DriftState.DRIFT,
                reports.getFirst().driftSummary().modelGatewayVersionRefDrift());
    }

    @Test
    void tenantlessQueryFailsClosedBeforeRepositoryCall() {
        final Fixture fixture = seededFixture();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        fixture.service.findReports(
                                new RegressionReportQuery(
                                        null,
                                        CASE_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        50,
                                        0)));
        assertEquals(0, fixture.replayRepository.findByCaseIdCalls);
    }

    @Test
    void uuidOnlyRepositoryQueryIsAbsentFromServicePath() {
        final Fixture fixture = seededFixture();

        fixture.service.findReports(query(null, null, VERDICT_ID, null, null, null));
        fixture.service.findReports(query(null, null, null, null, null, null));

        assertEquals(0, fixture.replayRepository.findByIdCalls);
        assertEquals(0, fixture.evaluationRepository.findByIdCalls);
        assertEquals(0, fixture.verdictRepository.findByIdCalls);
    }

    @Test
    void crossTenantReportReadReturnsEmpty() {
        final Fixture fixture = seededFixture();

        final List<RegressionReportView> reports =
                fixture.service.findReports(new RegressionReportQuery(
                        TENANT_B,
                        CASE_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        50,
                        0));

        assertTrue(reports.isEmpty());
    }

    @Test
    void listQueryIsPaginatedAndCanFilterByVerdictAndSeverity() {
        final Fixture fixture = new Fixture();
        fixture.seed(TENANT_A, "case-1", "evaluation-1", "verdict-1", List.of());
        fixture.seed(TENANT_A, "case-2", "evaluation-2", "verdict-2", List.of());
        fixture.seed(TENANT_A, "case-3", "evaluation-3", "verdict-3", List.of());

        final List<RegressionReportView> reports =
                fixture.service.findReports(new RegressionReportQuery(
                        TENANT_A,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        RegressionVerdict.Status.PASS,
                        RegressionSeverity.INFO,
                        null,
                        null,
                        2,
                        1));

        assertEquals(2, reports.size());
        assertEquals("verdict-2", reports.getFirst().verdictId());
        assertEquals("verdict-3", reports.get(1).verdictId());
    }

    @Test
    void caseIdQueryAppliesEvaluationPagination() {
        final Fixture fixture = new Fixture();
        fixture.seed(TENANT_A, CASE_ID, "evaluation-1", "verdict-1", List.of());
        fixture.seed(TENANT_A, CASE_ID, "evaluation-2", "verdict-2", List.of());
        fixture.seed(TENANT_A, CASE_ID, "evaluation-3", "verdict-3", List.of());

        final List<RegressionReportView> reports =
                fixture.service.findReports(new RegressionReportQuery(
                        TENANT_A,
                        CASE_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1,
                        1));

        assertEquals(1, reports.size());
        assertEquals("evaluation-2", reports.getFirst().evaluationId());
    }

    @Test
    void pageSizeOverOneHundredIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegressionReportQuery(
                        TENANT_A,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        101,
                        0));
    }

    @Test
    void reportViewDoesNotExposeRawPromptRawProviderResponseOrCredentialLikeFields() {
        final RegressionReportView report =
                seededFixture().service.findReports(query(CASE_ID, null, null, null, null, null)).getFirst();
        final String rendered = report.toString();

        assertFalse(rendered.contains("rawPrompt"));
        assertFalse(rendered.contains("rawProviderResponse"));
        assertFalse(rendered.contains("apiKey"));
        assertFalse(rendered.contains("apiSecret"));
        assertFalse(rendered.contains("credential"));
        assertEquals(HASH_A, report.providerSummaryHash());
        assertTrue(report.safeRefs().stream().allMatch(ref -> ref.contains(":")));
    }

    @Test
    void driftSummaryCapturesProviderModelPromptAndPolicyMismatch() {
        final Fixture fixture = new Fixture();
        fixture.seed(
                TENANT_A,
                CASE_ID,
                EVALUATION_ID,
                VERDICT_ID,
                List.of(
                        finding(TENANT_A, CASE_ID, EVALUATION_ID, VERDICT_ID, "PROVIDER_SUMMARY_HASH_MISMATCH"),
                        finding(TENANT_A, CASE_ID, EVALUATION_ID, VERDICT_ID, "MODEL_GATEWAY_VERSION_REF_MISMATCH"),
                        finding(TENANT_A, CASE_ID, EVALUATION_ID, VERDICT_ID, "PROMPT_VERSION_REF_MISMATCH"),
                        finding(TENANT_A, CASE_ID, EVALUATION_ID, VERDICT_ID, "POLICY_VERSION_MISMATCH")));

        final RegressionDriftSummary drift =
                fixture.service.findReports(query(CASE_ID, null, null, null, null, null))
                        .getFirst()
                        .driftSummary();

        assertEquals(RegressionDriftSummary.DriftState.DRIFT, drift.providerSummaryHashDrift());
        assertEquals(RegressionDriftSummary.DriftState.DRIFT, drift.modelGatewayVersionRefDrift());
        assertEquals(RegressionDriftSummary.DriftState.DRIFT, drift.promptVersionRefDrift());
        assertEquals(RegressionDriftSummary.DriftState.DRIFT, drift.policyVersionDrift());
    }

    @Test
    void executableTradingTermsAreNotExposedAsReportActionOrAllowedAction() {
        final RegressionReportView report =
                seededFixture().service.findReports(query(CASE_ID, null, null, null, null, null)).getFirst();
        final List<String> executableLabels =
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE");

        assertFalse(executableLabels.contains(report.actionLabel()));
        assertFalse(executableLabels.stream().anyMatch(term -> report.redactedSummary().contains(term)));
    }

    @Test
    void sourceAndCreatedFiltersRemainTenantBound() {
        final Fixture fixture = seededFixture();

        final List<RegressionReportView> byTrace =
                fixture.service.findReports(query(null, null, null, TRACE_ID, null, null));
        final List<RegressionReportView> bySourceRequest =
                fixture.service.findReports(query(null, null, null, null, SOURCE_REQUEST_ID, null));
        final List<RegressionReportView> bySourceDecision =
                fixture.service.findReports(query(null, null, null, null, null, SOURCE_DECISION_ID));
        final List<RegressionReportView> byCreated =
                fixture.service.findReports(new RegressionReportQuery(
                        TENANT_A,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        NOW.minusSeconds(1),
                        NOW.plusSeconds(1),
                        50,
                        0));

        assertEquals(1, byTrace.size());
        assertEquals(1, bySourceRequest.size());
        assertEquals(1, bySourceDecision.size());
        assertEquals(1, byCreated.size());
    }

    @Test
    void providerHttpAgentAndLangGraphClassesAreNotIntroduced() throws IOException {
        final Path root = projectRoot().resolve("dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay");
        final String forbiddenRuntimeClasses =
                "RealClient|HttpClient|WebClient|RestTemplate|OkHttp|LangGraph|AutoGen|CrewAI";

        try (Stream<Path> files = Files.walk(root)) {
            final List<Path> offenders = files
                    .filter(Files::isRegularFile)
                    .filter(path -> containsForbiddenRuntimeClass(path, forbiddenRuntimeClasses))
                    .toList();
            assertTrue(offenders.isEmpty(), offenders.toString());
        }
    }

    @Test
    void reportGenerationFailureFailsClosed() {
        final Fixture fixture = seededFixture();
        fixture.verdictRepository.failListByTenant = true;

        final ReplayPersistenceException error =
                assertThrows(
                        ReplayPersistenceException.class,
                        () -> fixture.service.findReports(query(null, null, null, null, null, null)));

        assertTrue(error.getMessage().contains("failed closed"));
    }

    private static Fixture seededFixture() {
        final Fixture fixture = new Fixture();
        fixture.seed(TENANT_A, CASE_ID, EVALUATION_ID, VERDICT_ID, List.of());
        return fixture;
    }

    private static RegressionReportQuery query(
            final String caseId,
            final String evaluationId,
            final String verdictId,
            final String traceId,
            final String sourceRequestId,
            final String sourceDecisionId) {
        return new RegressionReportQuery(
                TENANT_A,
                caseId,
                evaluationId,
                verdictId,
                traceId,
                sourceRequestId,
                sourceDecisionId,
                null,
                null,
                null,
                null,
                50,
                0);
    }

    private static RegressionFindingRecord finding(
            final String tenantId,
            final String caseId,
            final String evaluationId,
            final String verdictId,
            final String code) {
        return new RegressionFindingRecord(
                UUID.nameUUIDFromBytes((tenantId + "|" + verdictId + "|" + code).getBytes(StandardCharsets.UTF_8)),
                tenantId,
                caseId,
                evaluationId,
                verdictId,
                SOURCE_DECISION_ID,
                SOURCE_REQUEST_ID,
                TRACE_ID,
                REQUEST_ID,
                POLICY_VERSION,
                MODEL_GATEWAY_VERSION_REF,
                UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|input").getBytes(StandardCharsets.UTF_8)),
                UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|output").getBytes(StandardCharsets.UTF_8)),
                UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|expected").getBytes(StandardCharsets.UTF_8)),
                UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|actual").getBytes(StandardCharsets.UTF_8)),
                HASH_B,
                HASH_C,
                RegressionVerdict.Status.WARN,
                RegressionSeverity.WARN,
                code,
                code.toLowerCase().replace('_', ' ') + " recorded",
                "mock-gateway-ref:" + HASH_A.substring(0, 16),
                NOW,
                NOW);
    }

    private static ExpectedDecisionSummary summary() {
        return new ExpectedDecisionSummary(
                "READ_ONLY_RECOMMENDATION",
                "LONG_BIAS",
                "MEDIUM",
                RiskLevel.MEDIUM,
                List.of("gateway-call-ref", "decision-run-ref"),
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE"));
    }

    private static ReplayInputRef inputRef() {
        return new ReplayInputRef("QDR_DECISION_ARTIFACT_REF", "decision-artifact-a", HASH_B);
    }

    private static ReplayOutputRef outputRef() {
        return new ReplayOutputRef("MOCK_GATEWAY_SUMMARY_REF", "gateway-call-a", HASH_A);
    }

    private static Path projectRoot() {
        final Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if ("dh-usecase".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.getParent();
        }
        return workingDirectory;
    }

    private static boolean containsForbiddenRuntimeClass(
            final Path path, final String forbiddenRuntimeClasses) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("*"))
                    .filter(line -> !line.startsWith("//"))
                    .filter(line -> !line.startsWith("/*"))
                    .anyMatch(line -> line.matches(".*(" + forbiddenRuntimeClasses + ").*"));
        } catch (final IOException error) {
            throw new AssertionError("failed to inspect " + path, error);
        }
    }

    private static final class Fixture {
        private final InMemoryReplayCaseRepository replayRepository = new InMemoryReplayCaseRepository();
        private final InMemoryEvaluationCaseRepository evaluationRepository =
                new InMemoryEvaluationCaseRepository();
        private final InMemoryRegressionVerdictRepository verdictRepository =
                new InMemoryRegressionVerdictRepository();
        private final RegressionReadModelService service =
                new RegressionReadModelService(replayRepository, evaluationRepository, verdictRepository);

        private void seed(
                final String tenantId,
                final String caseId,
                final String evaluationId,
                final String verdictId,
                final List<RegressionFindingRecord> findingRecords) {
            final ReplayCaseRecord replayCase = replayCase(tenantId, caseId);
            final EvaluationCaseRecord evaluationCase = evaluationCase(tenantId, caseId, evaluationId, verdictId);
            final RegressionVerdictRecord verdictRecord = verdictRecord(tenantId, caseId, evaluationId, verdictId);
            replayRepository.records.put(key(tenantId, caseId), replayCase);
            evaluationRepository.records.put(key(tenantId, evaluationId), evaluationCase);
            verdictRepository.records.put(key(tenantId, verdictId), verdictRecord);
            verdictRepository.findings.addAll(findingRecords);
        }

        private static ReplayCaseRecord replayCase(final String tenantId, final String caseId) {
            return new ReplayCaseRecord(
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId).getBytes(StandardCharsets.UTF_8)),
                    tenantId,
                    caseId,
                    SOURCE_DECISION_ID,
                    SOURCE_REQUEST_ID,
                    TRACE_ID,
                    REQUEST_ID,
                    POLICY_VERSION,
                    MODEL_GATEWAY_VERSION_REF,
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|input").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|expected").getBytes(StandardCharsets.UTF_8)),
                    inputRef(),
                    summary(),
                    HASH_B,
                    HASH_C,
                    NOW,
                    NOW);
        }

        private static EvaluationCaseRecord evaluationCase(
                final String tenantId,
                final String caseId,
                final String evaluationId,
                final String verdictId) {
            return new EvaluationCaseRecord(
                    UUID.nameUUIDFromBytes((tenantId + "|" + evaluationId).getBytes(StandardCharsets.UTF_8)),
                    tenantId,
                    caseId,
                    evaluationId,
                    verdictId,
                    SOURCE_DECISION_ID,
                    SOURCE_REQUEST_ID,
                    TRACE_ID,
                    REQUEST_ID,
                    POLICY_VERSION,
                    "model:v1",
                    MODEL_GATEWAY_VERSION_REF,
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|input").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|output").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|expected").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|actual").getBytes(StandardCharsets.UTF_8)),
                    inputRef(),
                    outputRef(),
                    summary(),
                    summary(),
                    HASH_B,
                    HASH_C,
                    RegressionVerdict.Status.PASS,
                    RegressionSeverity.INFO,
                    null,
                    null,
                    HASH_A,
                    NOW,
                    NOW);
        }

        private static RegressionVerdictRecord verdictRecord(
                final String tenantId,
                final String caseId,
                final String evaluationId,
                final String verdictId) {
            return new RegressionVerdictRecord(
                    UUID.nameUUIDFromBytes((tenantId + "|" + verdictId).getBytes(StandardCharsets.UTF_8)),
                    tenantId,
                    caseId,
                    evaluationId,
                    verdictId,
                    SOURCE_DECISION_ID,
                    SOURCE_REQUEST_ID,
                    TRACE_ID,
                    REQUEST_ID,
                    POLICY_VERSION,
                    MODEL_GATEWAY_VERSION_REF,
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|input").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|output").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|expected").getBytes(StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes((tenantId + "|" + caseId + "|actual").getBytes(StandardCharsets.UTF_8)),
                    HASH_B,
                    HASH_C,
                    RegressionVerdict.Status.PASS,
                    RegressionSeverity.INFO,
                    null,
                    null,
                    null,
                    NOW,
                    NOW);
        }
    }

    private static final class InMemoryReplayCaseRepository implements ReplayCaseRepository {
        private final Map<String, ReplayCaseRecord> records = new LinkedHashMap<>();
        private int findByIdCalls;
        private int findByCaseIdCalls;

        @Override
        public ReplayCaseRecord save(final SaveReplayCaseCommand command) {
            throw new UnsupportedOperationException("read model test does not save replay case");
        }

        @Override
        public Optional<ReplayCaseRecord> findById(final String tenantId, final UUID id) {
            findByIdCalls++;
            return records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<ReplayCaseRecord> findByCaseId(final String tenantId, final String caseId) {
            findByCaseIdCalls++;
            return Optional.ofNullable(records.get(key(tenantId, caseId)));
        }

        @Override
        public List<ReplayCaseRecord> listByTraceId(
                final String tenantId, final String traceId, final int limit, final int offset) {
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.traceId().equals(traceId))
                    .toList(), limit, offset);
        }

        @Override
        public List<ReplayCaseRecord> listBySourceRequestId(
                final String tenantId,
                final String sourceRequestId,
                final int limit,
                final int offset) {
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.sourceRequestId().equals(sourceRequestId))
                    .toList(), limit, offset);
        }

        @Override
        public List<ReplayCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .toList(), limit, offset);
        }
    }

    private static final class InMemoryEvaluationCaseRepository implements EvaluationCaseRepository {
        private final Map<String, EvaluationCaseRecord> records = new LinkedHashMap<>();
        private int findByIdCalls;

        @Override
        public EvaluationCaseRecord save(final SaveEvaluationCaseCommand command) {
            throw new UnsupportedOperationException("read model test does not save evaluation case");
        }

        @Override
        public Optional<EvaluationCaseRecord> findById(final String tenantId, final UUID id) {
            findByIdCalls++;
            return records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<EvaluationCaseRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return Optional.ofNullable(records.get(key(tenantId, evaluationId)));
        }

        @Override
        public List<EvaluationCaseRecord> listByCaseId(
                final String tenantId, final String caseId, final int limit, final int offset) {
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.caseId().equals(caseId))
                    .toList(), limit, offset);
        }

        @Override
        public List<EvaluationCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .toList(), limit, offset);
        }
    }

    private static final class InMemoryRegressionVerdictRepository implements RegressionVerdictRepository {
        private final Map<String, RegressionVerdictRecord> records = new LinkedHashMap<>();
        private final List<RegressionFindingRecord> findings = new ArrayList<>();
        private boolean failListByTenant;
        private int findByIdCalls;

        @Override
        public RegressionVerdictRecord save(final SaveRegressionVerdictCommand command) {
            throw new UnsupportedOperationException("read model test does not save verdict");
        }

        @Override
        public List<RegressionFindingRecord> saveFindings(
                final String tenantId,
                final String verdictId,
                final List<SaveRegressionFindingCommand> commands) {
            throw new UnsupportedOperationException("read model test does not save findings");
        }

        @Override
        public Optional<RegressionVerdictRecord> findById(final String tenantId, final UUID id) {
            findByIdCalls++;
            return records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<RegressionVerdictRecord> findByVerdictId(
                final String tenantId, final String verdictId) {
            return Optional.ofNullable(records.get(key(tenantId, verdictId)));
        }

        @Override
        public Optional<RegressionVerdictRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.evaluationId().equals(evaluationId))
                    .findFirst();
        }

        @Override
        public List<RegressionFindingRecord> listFindingsByVerdictId(
                final String tenantId, final String verdictId, final int limit, final int offset) {
            return page(findings.stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.verdictId().equals(verdictId))
                    .toList(), limit, offset);
        }

        @Override
        public List<RegressionVerdictRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            if (failListByTenant) {
                throw new ReplayPersistenceException("synthetic list failure");
            }
            return page(records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .toList(), limit, offset);
        }
    }

    private static <T> List<T> page(final List<T> values, final int limit, final int offset) {
        return values.stream().skip(offset).limit(limit).toList();
    }

    private static String key(final String tenantId, final String id) {
        return tenantId + "|" + id;
    }
}
