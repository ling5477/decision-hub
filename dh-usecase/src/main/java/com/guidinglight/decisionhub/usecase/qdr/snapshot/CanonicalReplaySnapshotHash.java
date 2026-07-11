package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import java.util.Arrays;
import java.util.Objects;

/** 已完成 QDR6-CJSON-1 与 domain-separated SHA-256 的 immutable hash result。 */
public record CanonicalReplaySnapshotHash(byte[] canonicalBytes, String lowercaseHex) {

  /** 防御性复制 canonical bytes，并校验 lowercase SHA-256 hex。 */
  public CanonicalReplaySnapshotHash {
    canonicalBytes = Arrays.copyOf(Objects.requireNonNull(canonicalBytes, "canonicalBytes"), canonicalBytes.length);
    lowercaseHex =
        com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard.requireSha256Hex(
            lowercaseHex, "lowercaseHex");
  }

  /** 返回 canonical bytes 的防御性副本。 */
  @Override
  public byte[] canonicalBytes() {
    return Arrays.copyOf(canonicalBytes, canonicalBytes.length);
  }
}
