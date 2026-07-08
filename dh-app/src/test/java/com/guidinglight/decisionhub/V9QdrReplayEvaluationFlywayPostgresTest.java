package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V9 replay/evaluation baseline Flyway migration load test。
 *
 * <p>该测试只在本地 Docker 可用时启动临时 PostgreSQL；不访问生产数据库、不读取真实凭证、不调用 HTTP/provider/NQ。
 */
@Testcontainers(disabledWithoutDocker = true)
class V9QdrReplayEvaluationFlywayPostgresTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("decision_hub")
            .withUsername("decision_hub")
            .withPassword("decision_hub");

    @Test
    void flywayLoadsV9ReplayEvaluationBaselineOnPostgres() throws Exception {
        final Flyway flyway =
                Flyway.configure()
                        .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                        .locations("filesystem:src/main/resources/db/migration")
                        .load();

        flyway.migrate();

        try (Connection connection =
                        DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
                Statement statement = connection.createStatement();
                ResultSet rs =
                        statement.executeQuery(
                                "select table_name from information_schema.tables"
                                        + " where table_schema = 'public'"
                                        + " and table_name like 'qdr_%'")) {
            final Set<String> tables = new HashSet<>();
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
            assertThat(tables)
                    .contains(
                            "qdr_replay_case",
                            "qdr_evaluation_case",
                            "qdr_expected_decision_summary",
                            "qdr_regression_verdict",
                            "qdr_regression_finding",
                            "qdr_replay_input_ref",
                            "qdr_replay_output_ref");
        }
    }
}
