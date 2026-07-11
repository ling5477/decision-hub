package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;

import java.util.Locale;
import java.util.Set;

/**
 * Canonical replay snapshot 的完整 immutable version vector。
 *
 * <p>该合同只校验调用方提供的版本与 checksum metadata，不生成 canonical bytes 或 hash。禁止使用
 * {@code latest/current/default} 代替 immutable version，避免 replay input 随当前配置漂移。
 *
 * @param snapshotSchemaVersion      snapshot schema version。
 * @param decisionSchemaVersion      V5 decision schema version。
 * @param contextSchemaVersion       context allowlist schema version。
 * @param policyVersion              decision policy version。
 * @param evaluationPolicyVersion    evaluation policy version。
 * @param promptVersionRef           immutable prompt version ref。
 * @param promptVersionChecksum      prompt version checksum。
 * @param modelVersionRef            immutable model version ref。
 * @param modelVersionChecksum       model version checksum。
 * @param modelGatewayVersionRef     model gateway semantic version ref。
 * @param canonicalizationVersion    canonicalization compatibility version。
 * @param replayExecutorVersion      replay executor compatibility version。
 * @param hashAlgorithmVersion       hash algorithm compatibility version。
 */
public record CanonicalReplaySnapshotVersionVector(
        String snapshotSchemaVersion,
        String decisionSchemaVersion,
        String contextSchemaVersion,
        String policyVersion,
        String evaluationPolicyVersion,
        String promptVersionRef,
        String promptVersionChecksum,
        String modelVersionRef,
        String modelVersionChecksum,
        String modelGatewayVersionRef,
        String canonicalizationVersion,
        String replayExecutorVersion,
        String hashAlgorithmVersion) {

    /** Frozen snapshot schema version。 */
    public static final String SNAPSHOT_SCHEMA_VERSION = "QDR6-REPLAY-INPUT-1";

    /** Frozen structured context schema version。 */
    public static final String CONTEXT_SCHEMA_VERSION = "QDR6-CONTEXT-1";

    /** Frozen future canonicalization compatibility version。 */
    public static final String CANONICALIZATION_VERSION = "QDR6-CJSON-1";

    /** Frozen future mock replay executor compatibility version。 */
    public static final String REPLAY_EXECUTOR_VERSION = "QDR6-MOCK-REPLAY-1";

    /** Frozen hash algorithm label；本批次不实现 hash 计算。 */
    public static final String HASH_ALGORITHM_VERSION = "SHA-256";

    private static final Set<String> MOVING_ALIASES = Set.of("latest", "current", "default");

    /** 校验完整 version vector、固定兼容性标签与 immutable checksum。 */
    public CanonicalReplaySnapshotVersionVector {
        snapshotSchemaVersion = requireFixed(
                snapshotSchemaVersion, SNAPSHOT_SCHEMA_VERSION, "snapshotSchemaVersion");
        decisionSchemaVersion = requireImmutableVersion(decisionSchemaVersion, "decisionSchemaVersion");
        contextSchemaVersion =
                requireFixed(contextSchemaVersion, CONTEXT_SCHEMA_VERSION, "contextSchemaVersion");
        policyVersion = requireImmutableVersion(policyVersion, "policyVersion");
        evaluationPolicyVersion =
                requireImmutableVersion(evaluationPolicyVersion, "evaluationPolicyVersion");
        promptVersionRef = requireImmutableVersion(promptVersionRef, "promptVersionRef");
        promptVersionChecksum =
                ReplayPersistenceGuard.requireSha256Hex(promptVersionChecksum, "promptVersionChecksum");
        modelVersionRef = requireImmutableVersion(modelVersionRef, "modelVersionRef");
        modelVersionChecksum =
                ReplayPersistenceGuard.requireSha256Hex(modelVersionChecksum, "modelVersionChecksum");
        modelGatewayVersionRef =
                requireImmutableVersion(modelGatewayVersionRef, "modelGatewayVersionRef");
        canonicalizationVersion = requireFixed(
                canonicalizationVersion, CANONICALIZATION_VERSION, "canonicalizationVersion");
        replayExecutorVersion =
                requireFixed(replayExecutorVersion, REPLAY_EXECUTOR_VERSION, "replayExecutorVersion");
        hashAlgorithmVersion =
                requireFixed(hashAlgorithmVersion, HASH_ALGORITHM_VERSION, "hashAlgorithmVersion");
    }

    private static String requireImmutableVersion(final String value, final String field) {
        final String checked = ReplayPersistenceGuard.requireSafeText(value, field);
        if (MOVING_ALIASES.contains(checked.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(field + " must be immutable and must not use a moving alias");
        }
        return checked;
    }

    private static String requireFixed(final String value, final String expected, final String field) {
        final String checked = ReplayPersistenceGuard.requireSafeText(value, field);
        if (!expected.equals(checked)) {
            throw new IllegalArgumentException(field + " must be " + expected);
        }
        return checked;
    }
}
