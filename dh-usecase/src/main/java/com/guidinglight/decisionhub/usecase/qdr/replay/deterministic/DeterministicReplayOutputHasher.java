package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotVersionVector;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** QDR6 replay output 的独立 domain-separated QDR6-CJSON-1 + SHA-256 calculator。 */
final class DeterministicReplayOutputHasher {

  static final String OUTPUT_SCHEMA_VERSION = "QDR6-REPLAY-OUTPUT-1";
  private static final String DOMAIN_PREFIX = "DH-QDR6-DETERMINISTIC-REPLAY-OUTPUT";
  private final Qdr6CanonicalJson canonicalJson;

  DeterministicReplayOutputHasher(final Qdr6CanonicalJson canonicalJson) {
    this.canonicalJson = Objects.requireNonNull(canonicalJson, "canonicalJson");
  }

  OutputHash hash(
      final DeterministicReplayProjection projection,
      final CanonicalReplaySnapshotVersionVector versions) {
    try {
      final byte[] canonicalBytes = canonicalJson.canonicalize(projection.canonicalValue());
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(domainBytes(versions));
      digest.update(canonicalBytes);
      return new OutputHash(canonicalBytes, HexFormat.of().formatHex(digest.digest()));
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("replay output SHA-256 unavailable", error);
    }
  }

  private static byte[] domainBytes(final CanonicalReplaySnapshotVersionVector versions) {
    final String domain =
        DOMAIN_PREFIX
            + '\0'
            + "outputSchema="
            + OUTPUT_SCHEMA_VERSION
            + '\0'
            + "snapshotSchema="
            + versions.snapshotSchemaVersion()
            + '\0'
            + "canonical="
            + versions.canonicalizationVersion()
            + '\0'
            + "executor="
            + versions.replayExecutorVersion()
            + '\0'
            + "hash="
            + versions.hashAlgorithmVersion()
            + '\0';
    return domain.getBytes(StandardCharsets.UTF_8);
  }

  record OutputHash(byte[] canonicalBytes, String lowercaseHex) {
    OutputHash {
      canonicalBytes = Arrays.copyOf(canonicalBytes, canonicalBytes.length);
      lowercaseHex =
          com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard.requireSha256Hex(
              lowercaseHex, "replayOutputHash");
    }

    @Override
    public byte[] canonicalBytes() {
      return Arrays.copyOf(canonicalBytes, canonicalBytes.length);
    }
  }
}
