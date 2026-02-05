package com.guidinglight.decisionhub.eval.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.facade.DecisionHubFacade;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateCommand;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateResult;
import com.guidinglight.decisionhub.usecase.facade.impl.DecisionHubFacadeImpl;

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

    private GoldenCaseRunnerMain() {}

    public static void main(String[] args) throws Exception {
        String repoRoot = System.getProperty("repo.root", ".");
        String casesDir = System.getProperty("golden.cases.dir", Path.of(repoRoot, "golden_cases").toString());

        // 只扫描 run 目录，避免把 expected/ 等非用例 JSON 当成用例处理
        Path runDir = Path.of(casesDir, "run");
        if (!Files.exists(runDir)) {
            System.err.println("[GOLDEN] run dir not found: " + runDir.toAbsolutePath());
            System.exit(2);
            return;
        }

        List<Path> caseFiles = listJsonFiles(runDir);
        if (caseFiles.isEmpty()) {
            System.err.println("[GOLDEN] no case files found under: " + runDir.toAbsolutePath());
            System.exit(2);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        DecisionHubFacade facade = new DecisionHubFacadeImpl();

        int failedCases = 0;

        for (Path f : caseFiles) {
            JsonNode node = mapper.readTree(f.toFile());

            String caseId = textOrNull(node, "caseId");
            String title = textOrNull(node, "title");
            String op = node.path("operation").asText("");

            // 用例级别的失败收集：保证 PASS/FAIL 打印准确
            List<String> caseErrors = new ArrayList<>();

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

            RunCreateCommand cmd;
            try {
                cmd = mapper.treeToValue(cmdNode, RunCreateCommand.class);
            } catch (Exception e) {
                caseErrors.add("failed to parse command: " + e.getClass().getSimpleName() + ": " + safeMsg(e));
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            RunCreateResult res;
            try {
                res = facade.createRun(cmd);
            } catch (Exception e) {
                caseErrors.add("facade.createRun threw: " + e.getClass().getSimpleName() + ": " + safeMsg(e));
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            // asserts
            JsonNode assertList = node.path("expect").path("assertList");
            if (!assertList.isArray()) {
                caseErrors.add("expect.assertList must be an array");
                printCaseResult(f, caseId, title, op, caseErrors);
                failedCases++;
                continue;
            }

            for (JsonNode a : assertList) {
                String path = a.path("path").asText("");
                String aop = a.path("op").asText("");
                JsonNode valNode = a.get("value"); // 允许不存在（如 NOT_EMPTY）
                String expected = (valNode == null || valNode.isNull()) ? null : valNode.asText();

                // 目前只实现最小断言集，后续可扩展为 JSONPath 引擎
                if ("$.runId".equals(path) && "NOT_EMPTY".equals(aop)) {
                    if (res.getRunId() == null || res.getRunId().isBlank()) {
                        caseErrors.add("assert failed: $.runId NOT_EMPTY, actual=" + String.valueOf(res.getRunId()));
                    }
                    continue;
                }

                if ("$.status".equals(path) && "EQ".equals(aop)) {
                    if (expected == null) {
                        caseErrors.add("assert invalid: $.status EQ requires 'value'");
                    } else if (res.getStatus() == null || !expected.equals(res.getStatus())) {
                        caseErrors.add("assert failed: $.status EQ " + expected + ", actual=" + String.valueOf(res.getStatus()));
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
            System.err.println("[GOLDEN] FAILED cases=" + failedCases);
            System.exit(2);
        } else {
            System.out.println("[GOLDEN] ALL PASS cases=" + caseFiles.size());
        }
    }

    private static List<Path> listJsonFiles(Path dir) throws Exception {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    private static void printCaseResult(Path file, String caseId, String title, String op, List<String> errors) {
        String idPart = (caseId == null || caseId.isBlank()) ? "" : (" caseId=" + caseId);
        String titlePart = (title == null || title.isBlank()) ? "" : (" title=" + title);
        if (errors == null || errors.isEmpty()) {
            System.out.println("[GOLDEN] PASS op=" + op + idPart + titlePart + " file=" + file);
            return;
        }
        System.err.println("[GOLDEN] FAIL op=" + op + idPart + titlePart + " file=" + file);
        for (String e : errors) {
            System.err.println("         - " + e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        return (m == null) ? "" : m;
    }
}
