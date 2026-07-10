package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stage-QDR-6 B1 的固定 evidence completeness 与 fail-closed policy。
 *
 * <p>该 policy 只校验已传入 contracts。它复用 {@link QdrPersistenceSafety} 和
 * {@link PromptModelSafetyRules} 的统一凭证/交易安全规则，并补足它们不负责的 raw material marker；
 * 不读取 Repository、不执行聚合、不调用 Provider、HTTP、NQ、Agent 或 LangGraph。
 */
public enum DecisionEvidencePolicy {
    /** 决策核心 profile。 */
    CORE_DECISION(
            EnumSet.of(
                    EvidenceType.REQUEST,
                    EvidenceType.RUN,
                    EvidenceType.CONTEXT_SNAPSHOT,
                    EvidenceType.DECISION_OUTPUT,
                    EvidenceType.AUDIT_EVENT,
                    EvidenceType.TRACE_STEP)),
    /** 确定性 replay 所需的安全 evidence profile。 */
    DETERMINISTIC_REPLAY(
            EnumSet.of(
                    EvidenceType.REQUEST,
                    EvidenceType.RUN,
                    EvidenceType.CONTEXT_SNAPSHOT,
                    EvidenceType.DECISION_OUTPUT,
                    EvidenceType.AUDIT_EVENT,
                    EvidenceType.TRACE_STEP,
                    EvidenceType.QDR_REPLAY_CASE,
                    EvidenceType.QDR_EVALUATION_CASE)),
    /** 内部 acceptance 所需的完整 evidence profile。 */
    INTERNAL_ACCEPTANCE(
            EnumSet.of(
                    EvidenceType.REQUEST,
                    EvidenceType.RUN,
                    EvidenceType.CONTEXT_SNAPSHOT,
                    EvidenceType.DECISION_OUTPUT,
                    EvidenceType.AUDIT_EVENT,
                    EvidenceType.TRACE_STEP,
                    EvidenceType.QDR_REPLAY_CASE,
                    EvidenceType.QDR_EVALUATION_CASE,
                    EvidenceType.REGRESSION_VERDICT,
                    EvidenceType.PROVIDER_HEALTH,
                    EvidenceType.PROVIDER_READINESS,
                    EvidenceType.OBSERVABILITY_REPORT,
                    EvidenceType.INTERNAL_ACCEPTANCE_RESULT));

    private static final Set<String> RAW_MATERIAL_MARKERS =
            Set.of("rawprompt", "rawproviderresponse", "providerraw", "prompttext");

    private final Set<EvidenceType> mandatoryEvidenceTypes;

    DecisionEvidencePolicy(final Set<EvidenceType> mandatoryEvidenceTypes) {
        this.mandatoryEvidenceTypes = Set.copyOf(mandatoryEvidenceTypes);
    }

    /**
     * 返回不可被调用方降级的强制 evidence 类型集合。
     *
     * @return 当前 profile 的不可变强制类型集合。
     */
    public Set<EvidenceType> mandatoryEvidenceTypes() {
        return mandatoryEvidenceTypes;
    }

    /**
     * 对 query 与已传入 evidence refs 做纯 contract 评估。
     *
     * <p>任一 tenant/correlation 不一致或同 identity 的内容不一致都会生成 {@link DecisionEvidenceStatus#INVALID}；
     * 不会静默移除冲突 ref。缺少强制类型且不存在冲突时才返回 {@link DecisionEvidenceStatus#INCOMPLETE}。
     *
     * @param query 当前 tenant-bound 查询合同。
     * @param evidenceRefs 已由调用方提供的 typed safe refs。
     * @return 不访问外部资源的 aggregate contract。
     */
    public DecisionEvidenceAggregate evaluate(
            final DecisionEvidenceQuery query, final List<DecisionEvidenceRef> evidenceRefs) {
        final DecisionEvidenceQuery checkedQuery = Objects.requireNonNull(query, "query");
        if (checkedQuery.policy() != this) {
            throw new IllegalArgumentException("query policy must match evaluation policy");
        }

        final List<DecisionEvidenceRef> checkedRefs = List.copyOf(Objects.requireNonNullElse(evidenceRefs, List.of()));
        final List<DecisionEvidenceFinding> findings = new ArrayList<>();
        final Map<String, DecisionEvidenceRef> refsByIdentity = new LinkedHashMap<>();
        final EnumSet<EvidenceType> presentMandatoryTypes = EnumSet.noneOf(EvidenceType.class);
        boolean invalid = false;

        for (final DecisionEvidenceRef evidenceRef : checkedRefs) {
            if (!checkedQuery.correlation().matches(evidenceRef.correlation())) {
                invalid = true;
                findings.add(
                        finding(
                                "CORRELATION_MISMATCH",
                                DecisionEvidenceFinding.Severity.BLOCKER,
                                evidenceRef.evidenceType(),
                                evidenceRef.identity(),
                                "证据关联键与查询主关联键不一致"));
                if (!checkedQuery.tenantId().equals(evidenceRef.correlation().tenantId())) {
                    findings.add(
                            finding(
                                    "TENANT_MISMATCH",
                                    DecisionEvidenceFinding.Severity.BLOCKER,
                                    evidenceRef.evidenceType(),
                                    evidenceRef.identity(),
                                    "跨租户证据已被 fail-closed 拒绝"));
                }
                continue;
            }

            final DecisionEvidenceRef existing = refsByIdentity.putIfAbsent(evidenceRef.identity(), evidenceRef);
            if (existing != null && !existing.equals(evidenceRef)) {
                invalid = true;
                findings.add(
                        finding(
                                "EVIDENCE_REF_INVALID",
                                DecisionEvidenceFinding.Severity.BLOCKER,
                                evidenceRef.evidenceType(),
                                evidenceRef.identity(),
                                "同一 evidence ref 存在不一致内容"));
                continue;
            }
            if (mandatoryEvidenceTypes.contains(evidenceRef.evidenceType())) {
                if (!evidenceRef.mandatory()) {
                    invalid = true;
                    findings.add(
                            finding(
                                    "EVIDENCE_REF_INVALID",
                                    DecisionEvidenceFinding.Severity.ERROR,
                                    evidenceRef.evidenceType(),
                                    evidenceRef.identity(),
                                    "强制 evidence 类型不能被来源声明为可选"));
                } else {
                    presentMandatoryTypes.add(evidenceRef.evidenceType());
                }
            }
        }

        final EnumSet<EvidenceType> missingMandatoryTypes =
                EnumSet.copyOf(mandatoryEvidenceTypes);
        missingMandatoryTypes.removeAll(presentMandatoryTypes);
        for (final EvidenceType missingType : missingMandatoryTypes) {
            findings.add(
                    finding(
                            "MANDATORY_EVIDENCE_MISSING",
                            DecisionEvidenceFinding.Severity.ERROR,
                            missingType,
                            null,
                            "强制 evidence 类型缺失"));
        }

        final DecisionEvidenceStatus status =
                invalid
                        ? DecisionEvidenceStatus.INVALID
                        : missingMandatoryTypes.isEmpty()
                                ? DecisionEvidenceStatus.COMPLETE
                                : DecisionEvidenceStatus.INCOMPLETE;
        return new DecisionEvidenceAggregate(
                checkedQuery.correlation(),
                checkedRefs,
                status,
                findings,
                List.copyOf(missingMandatoryTypes));
    }

    /**
     * 对必填安全文本复用既有 QDR redaction 规则并拒绝 raw material marker。
     *
     * @param value 待校验值。
     * @param field 固定错误消息使用的字段名。
     * @return trim 后的安全文本。
     */
    static String requireSafeText(final String value, final String field) {
        final String checked = QdrPersistenceSafety.requireSafeText(value, field);
        final String normalized = normalize(checked);
        if (RAW_MATERIAL_MARKERS.stream().anyMatch(normalized::contains)) {
            throw new IllegalArgumentException(field + " rejected by raw material boundary");
        }
        return checked;
    }

    /**
     * 对可选安全文本进行校验。
     *
     * @param value 待校验值。
     * @param field 固定错误消息使用的字段名。
     * @return null 或 trim 后的安全文本。
     */
    static String optionalSafeText(final String value, final String field) {
        return value == null || value.isBlank() ? null : requireSafeText(value, field);
    }

    /**
     * 校验可选 SHA-256 hash。
     *
     * @param value 可选 hash。
     * @param field 固定错误消息使用的字段名。
     * @return null 或 lowercase SHA-256 hex。
     */
    static String optionalSha256Hex(final String value, final String field) {
        return value == null || value.isBlank() ? null : QdrPersistenceSafety.requireSha256Hex(value, field);
    }

    /** 校验 policy 不为空。 */
    static DecisionEvidencePolicy requirePolicy(final DecisionEvidencePolicy policy) {
        return Objects.requireNonNull(policy, "policy");
    }

    /** 校验 finding severity 不为空。 */
    static DecisionEvidenceFinding.Severity requireSeverity(
            final DecisionEvidenceFinding.Severity severity) {
        return Objects.requireNonNull(severity, "severity");
    }

    private static DecisionEvidenceFinding finding(
            final String code,
            final DecisionEvidenceFinding.Severity severity,
            final EvidenceType evidenceType,
            final String safeRef,
            final String message) {
        return new DecisionEvidenceFinding(code, severity, evidenceType, safeRef, message);
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    /** Stage-QDR-6 B1 支持的固定 evidence 类型。 */
    public enum EvidenceType {
        REQUEST,
        RUN,
        CONTEXT_SNAPSHOT,
        DECISION_OUTPUT,
        AUDIT_EVENT,
        TRACE_STEP,
        QDR_REPLAY_CASE,
        QDR_EVALUATION_CASE,
        REGRESSION_VERDICT,
        REGRESSION_FINDING,
        PROVIDER_HEALTH,
        PROVIDER_READINESS,
        OBSERVABILITY_REPORT,
        INTERNAL_ACCEPTANCE_RESULT
    }
}
