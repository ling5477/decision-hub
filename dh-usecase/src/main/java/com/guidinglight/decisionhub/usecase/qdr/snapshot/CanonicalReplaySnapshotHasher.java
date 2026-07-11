package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Canonical replay snapshot 的 domain-separated SHA-256 计算器。
 *
 * <p>生产入口只接受完整 {@link ReplayInputSnapshot}，不存在“传入 source hash”捷径。domain separator
 * 包含 snapshot/canonicalization/executor/hash versions；tenant 与 correlation 位于 canonical snapshot bytes。
 */
public final class CanonicalReplaySnapshotHasher {

  private static final String DOMAIN_PREFIX = "DH-QDR6-CANONICAL-REPLAY-SNAPSHOT";
  private final Qdr6CanonicalJson canonicalJson;

  /** 创建使用指定 QDR6 canonicalizer 的 hash calculator。 */
  public CanonicalReplaySnapshotHasher(final Qdr6CanonicalJson canonicalJson) {
    this.canonicalJson = Objects.requireNonNull(canonicalJson, "canonicalJson");
  }

  /** 按 snapshot 自身冻结版本生成 canonical bytes 与 lowercase SHA-256。 */
  public CanonicalReplaySnapshotHash hash(final ReplayInputSnapshot snapshot) {
    final ReplayInputSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
    return hash(
        checked,
        new HashDomain(
            checked.snapshotSchemaVersion(),
            checked.canonicalizationVersion(),
            checked.replayAlgorithmVersion(),
            checked.hashAlgorithmVersion()));
  }

  CanonicalReplaySnapshotHash hash(
      final ReplayInputSnapshot snapshot, final HashDomain domain) {
    try {
      final byte[] canonicalBytes = canonicalJson.canonicalize(snapshot.canonicalValue());
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(domain.bytes());
      digest.update(canonicalBytes);
      return new CanonicalReplaySnapshotHash(
          canonicalBytes, HexFormat.of().formatHex(digest.digest()));
    } catch (final CanonicalReplaySnapshotAssemblyException error) {
      throw error;
    } catch (final NoSuchAlgorithmException | RuntimeException error) {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.HASH_FAILED,
          "canonical snapshot SHA-256 failed",
          error);
    }
  }

  /** Canonical hash domain 的四个 version labels；测试可用于证明 domain version 敏感性。 */
  record HashDomain(
      String snapshotSchemaVersion,
      String canonicalizationVersion,
      String replayExecutorVersion,
      String hashAlgorithmVersion) {

    HashDomain {
      snapshotSchemaVersion = require(snapshotSchemaVersion, "snapshotSchemaVersion");
      canonicalizationVersion = require(canonicalizationVersion, "canonicalizationVersion");
      replayExecutorVersion = require(replayExecutorVersion, "replayExecutorVersion");
      hashAlgorithmVersion = require(hashAlgorithmVersion, "hashAlgorithmVersion");
    }

    byte[] bytes() {
      final String value =
          DOMAIN_PREFIX
              + '\0'
              + "snapshot="
              + snapshotSchemaVersion
              + '\0'
              + "canonical="
              + canonicalizationVersion
              + '\0'
              + "executor="
              + replayExecutorVersion
              + '\0'
              + "hash="
              + hashAlgorithmVersion
              + '\0';
      return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String require(final String value, final String field) {
      final String checked = Objects.requireNonNull(value, field).trim();
      if (checked.isEmpty()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
      return checked;
    }
  }
}
