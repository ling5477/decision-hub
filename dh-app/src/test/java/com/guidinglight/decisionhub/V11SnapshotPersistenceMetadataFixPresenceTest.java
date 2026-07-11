package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** V11 snapshot persistence metadata-only forward migration 文件边界回归测试。 */
class V11SnapshotPersistenceMetadataFixPresenceTest {

  private static String body;
  private static String lowerBody;

  @BeforeAll
  static void load() throws IOException {
    final Path migration =
        Path.of(
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V11__qdr6_snapshot_persistence_metadata_fix.sql")
            .toAbsolutePath()
            .normalize();
    assertTrue(Files.exists(migration), "V11 migration file must exist: " + migration);
    body = Files.readString(migration, StandardCharsets.UTF_8);
    lowerBody = body.toLowerCase(Locale.ROOT);
  }

  @Test
  void commentsEveryV10SnapshotIndexAndConstraintFamily() {
    assertAll(
        () -> assertTrue(body.contains("comment on constraint ux_dh_decision_request_tenant_decision")),
        () -> assertTrue(body.contains("comment on constraint ux_qdr_canonical_snapshot_tenant_id")),
        () -> assertTrue(body.contains("comment on constraint fk_qdr_canonical_snapshot_v9_replay_case")),
        () -> assertTrue(body.contains("comment on constraint chk_qdr_canonical_snapshot_hashes")),
        () -> assertTrue(body.contains("comment on index idx_qdr_canonical_snapshot_tenant_run")),
        () -> assertTrue(body.contains("comment on index idx_qdr_canonical_snapshot_tenant_replay_case")));
  }

  @Test
  void remainsMetadataOnlyWithoutSchemaSemantics() {
    assertAll(
        () -> assertFalse(lowerBody.contains("create table")),
        () -> assertFalse(lowerBody.contains("alter table")),
        () -> assertFalse(lowerBody.contains("create index")),
        () -> assertFalse(lowerBody.contains("drop ")),
        () -> assertFalse(lowerBody.contains("update ")),
        () -> assertFalse(lowerBody.contains("insert ")),
        () -> assertFalse(lowerBody.contains("delete ")),
        () -> assertFalse(lowerBody.contains("create trigger")),
        () -> assertFalse(lowerBody.contains("create function")));
  }
}
