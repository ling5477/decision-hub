package com.guidinglight.decisionhub.eval.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.facade.DecisionHubFacade;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateCommand;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateResult;
import com.guidinglight.decisionhub.usecase.facade.impl.DecisionHubFacadeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Golden 回归入口（绑定 Maven verify）。
 *
 * 约定：
 * - 用例目录：${golden.cases.dir}/run/*.json
 * - 每条用例包含：operation + command（推荐）或 input（兼容旧格式）+ expect.assertList
 * - 目前支持 operation = RUN_CREATE
 */
public final class GoldenCaseRunnerMain {

    private static final Logger log = LoggerFactory.getLogger(GoldenCaseRunnerMain.class);

    private GoldenCaseRunnerMain() {}

    public static void main(final String[] args) {
        try {
            run();
        } catch (final RuntimeException e) {
            // 兜底：避免静默失败
            log.error("[GOLDEN] runner crashed: {}", safeMsg(e), e);
            System.exit(2);
        }
    }

    private static void run() {
        final String repoRoot = System.getProperty("repo.root", ".");
        final String casesDir =
                System.getProperty("golden.cases.dir", Path.of(repoRoot, "golden_cases").toString());

        // 只扫描 run 目录，避免把 expected/ 等非用例 JSON 当成用例处理
        final Path runDir = Path.of(casesDir, "run");
        if (!Files.exists(runDir)) {
            log.error("[GOLDEN] run dir not found: {}", runDir.toAbsolutePath());
            System.exit(2);
            return;
        }

        final List<Path> caseFiles = listJsonFiles(runDir);
        if (caseFiles.isEmpty()) {
            log.error("[GOLDEN] no case files found under: {}", runDir.toAbsolutePath());
            System.exit(2);
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();
        final DecisionHubFacade facade = new DecisionHubFacadeImpl();

        int failedCases = 0;

        for (final Path f : caseFiles) {
            final JsonNode node;
            try {
                node = mapper.readTree(f.toFile());
            } catch (final Exception e) {
                log.error("[GOLDEN] failed to read json file={}, err={}", f, safeMsg(e), e);
                failedCases++;
                continue;
            }

            final String caseId = textOrNull(node, "caseId");
            final String title = textOrNull(node, "title");
            final String op = node.path("operation").asText("");

            // 用例级别的失败收集：保证 PASS/FAIL 打印准确
            final List<String> caseErrors = new ArrayList<>();

            if (!"RUN_CREATE".equals(op)) {
                caseErrors.add("unknown operation: '" + op + "'");
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            // command 优先，兼容旧 input
            JsonNode cmdNode = node.get("command");
            if (cmdNode == null || cmdNode.isNull()) {
                cmdNode = node.get("input");
            }
            if (cmdNode == null || cmdNode.isNull()) {
                caseErrors.add("missing 'command' (preferred) or 'input' (legacy) object");
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            final RunCreateCommand cmd;
            try {
                cmd = mapper.treeToValue(cmdNode, RunCreateCommand.class);
            } catch (final Exception e) {
                caseErrors.add(
                        "failed to parse command: " + e.getClass().getSimpleName() + ": " + safeMsg(e));
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            final RunCreateResult res;
            try {
                res = facade.createRun(cmd);
            } catch (final Exception e) {
                caseErrors.add(
                        "facade.createRun threw: " + e.getClass().getSimpleName() + ": " + safeMsg(e));
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            // asserts
            final JsonNode assertList = node.path("expect").path("assertList");
            if (!assertList.isArray()) {
                caseErrors.add("expect.assertList must be an array");
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            for (final JsonNode a : assertList) {
                final String path = a.path("path").asText("");
                final String aop = a.path("op").asText("");
                final JsonNode valNode = a.get("value"); // 允许不存在（如 NOT_EMPTY）
                final String expected =
                        (valNode == null || valNode.isNull()) ? null : valNode.asText();

                // 目前只实现最小断言集，后续可扩展为 JSONPath 引擎
                if ("$.runId".equals(path) && "NOT_EMPTY".equals(aop)) {
                    if (res.getRunId() == null || res.getRunId().isBlank()) {
                        caseErrors.add(
                                "assert failed: $.runId NOT_EMPTY, actual=" + String.valueOf(res.getRunId()));
                    }
                    continue;
                }

                if ("$.status".equals(path) && "EQ".equals(aop)) {
                    if (expected == null) {
                        caseErrors.add("assert invalid: $.status EQ requires 'value'");
                    } else if (res.getStatus() == null || !expected.equals(res.getStatus())) {
                        caseErrors.add(
                                "assert failed: $.status EQ "
                                        + expected
                                        + ", actual="
                                        + String.valueOf(res.getStatus()));
                    }
                    continue;
                }

                caseErrors.add("unsupported assert: path=" + path + ", op=" + aop);
            }

            printCaseResult(f, caseId, title, op, caseErrors);
            if (!caseErrors.isEmpty()) {
                failedCases++;
            }
        }

        if (failedCases > 0) {
            log.error("[GOLDEN] FAILED cases={}", failedCases);
            System.exit(2);
        } else {
            log.info("[GOLDEN] ALL PASS cases={}", caseFiles.size());
        }
    }

    private static List<Path> listJsonFiles(final Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (final Exception e) {
            // 此处属于环境/IO问题，视为用例加载失败
            log.error("[GOLDEN] list json files failed, dir={}, err={}", dir, safeMsg(e), e);
            return List.of();
        }
    }

    private static void printCaseResult(
            final Path file,
            final String caseId,
            final String title,
            final String op,
            final List<String> errors) {

        final String idPart = (caseId == null || caseId.isBlank()) ? "" : (" caseId=" + caseId);
        final String titlePart = (title == null || title.isBlank()) ? "" : (" title=" + title);

        if (errors == null || errors.isEmpty()) {
            log.info("[GOLDEN] PASS op={}{}{} file={}", op, idPart, titlePart, file);
            return;
        }

        log.error("[GOLDEN] FAIL op={}{}{} file={}", op, idPart, titlePart, file);
        for (final String e : errors) {
            log.error("         - {}", e);
        }
    }

    private static String textOrNull(final JsonNode node, final String field) {
        final JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        final String s = v.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String safeMsg(final Throwable t) {
        final String m = t.getMessage();
        return (m == null) ? "" : m;
    }
}
