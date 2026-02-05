package com.guidinglight.decisionhub.usecase.run;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.ledger.LedgerEvent;
import com.guidinglight.decisionhub.ledger.LedgerEventType;
import com.guidinglight.decisionhub.providers.ModelOutput;
import com.guidinglight.decisionhub.providers.ModelProvider;
import com.guidinglight.decisionhub.usecase.gate.evaluator.CompletenessEvaluatorV1;
import com.guidinglight.decisionhub.usecase.gate.evaluator.ConstraintEvaluatorV1;
import com.guidinglight.decisionhub.usecase.gate.evaluator.ConsistencyEvaluatorV1;
import com.guidinglight.decisionhub.usecase.run.support.DecisionRecordV1Factory;

import java.time.Duration;
import java.util.*;

/**
 * v1 全接线版：
 * - decision_id = run_id（DecisionRecordV1Factory 已定死）
 * - 模型调用（ModelProvider）-> DecisionRecord.model_runs[].raw_output/usage/tool_traces/prompt_snapshot
 * - Evaluators（constraint/consistency/completeness）-> eval_results[]
 * - v1 决策：gate + quality（rule_based pick_best_quality）-> final_decision
 *
 * 说明：
 * - 这里的 provider 调用是串行的（最小可用）。你后续可在 executor 层并行化。
 */
public class RunService {

  private final RunRepository runRepository;
  private final EventStore eventStore;
  private final List<ModelProvider> providers;

  private final ConstraintEvaluatorV1 constraint = new ConstraintEvaluatorV1();
  private final ConsistencyEvaluatorV1 consistency = new ConsistencyEvaluatorV1();
  private final CompletenessEvaluatorV1 completeness = new CompletenessEvaluatorV1();

  public RunService(RunRepository runRepository, EventStore eventStore, List<ModelProvider> providers) {
    this.runRepository = runRepository;
    this.eventStore = eventStore;
    this.providers = providers == null ? List.of() : providers;
  }

  public Run create(String tenantId, String question, Map<String, Object> configSnapshot) {
    Run run = new Run(tenantId, question, configSnapshot, TimeProvider.now());

    // v1：初始化 DecisionRecord（全量档案）
    run.setDecisionRecord(DecisionRecordV1Factory.init(run), TimeProvider.now());
    runRepository.save(run);

    eventStore.append(new LedgerEvent(
        IdGenerator.newId(),
        run.getRunId(),
        run.getTenantId(),
        LedgerEventType.RUN_CREATED,
        TimeProvider.now(),
        Map.of("question", question)
    ));
    return run;
  }

  public Run start(String runId) {
    Run run = get(runId);

    if (run.getDecisionRecord() == null) {
      run.setDecisionRecord(DecisionRecordV1Factory.init(run), TimeProvider.now());
      runRepository.save(run);
    }

    run.start(TimeProvider.now());
    markDecisionStatus(run, "RUNNING");
    runRepository.save(run);

    eventStore.append(new LedgerEvent(
        IdGenerator.newId(),
        run.getRunId(),
        run.getTenantId(),
        LedgerEventType.RUN_STARTED,
        TimeProvider.now(),
        Map.of("runId", run.getRunId(), "providerCount", providers.size())
    ));

    Map<String, Object> dr = run.getDecisionRecord();

    try {
      List<String> modelRunIds = new ArrayList<>();

      // 1) 模型调用：provider -> model_runs
      for (ModelProvider p : providers) {
        String modelRunId = "mr_" + p.key() + "_" + IdGenerator.newId();
        modelRunIds.add(modelRunId);

        appendModelRun(dr, modelRunId, p.key(), "PENDING");

        eventStore.append(new LedgerEvent(
            IdGenerator.newId(),
            run.getRunId(),
            run.getTenantId(),
            LedgerEventType.STEP_STARTED,
            TimeProvider.now(),
            Map.of("kind", "model_call", "model", p.key(), "model_run_id", modelRunId)
        ));

        updateModelRunStatus(dr, modelRunId, "RUNNING");
        // prompt_snapshot：把 question/options 固化，便于回放
        writePromptSnapshot(dr, modelRunId, run.getQuestion(), run.getConfigSnapshot(), p);

        run.setDecisionRecord(dr, TimeProvider.now());
        runRepository.save(run);

        Duration timeout = resolveTimeout(run.getConfigSnapshot());

        long startNs = System.nanoTime();
        try {
          ModelOutput out = p.invoke(run.getQuestion(), safeMap(run.getConfigSnapshot()), timeout);
          long latencyMs = Math.max(0, (System.nanoTime() - startNs) / 1_000_000);

          finishModelRunSucceeded(dr, modelRunId, latencyMs, out);

          eventStore.append(new LedgerEvent(
              IdGenerator.newId(),
              run.getRunId(),
              run.getTenantId(),
              LedgerEventType.STEP_COMPLETED,
              TimeProvider.now(),
              Map.of("kind", "model_call", "model", p.key(), "model_run_id", modelRunId, "latency_ms", latencyMs)
          ));
        } catch (Exception ex) {
          finishModelRunFailed(dr, modelRunId, ex);

          eventStore.append(new LedgerEvent(
              IdGenerator.newId(),
              run.getRunId(),
              run.getTenantId(),
              LedgerEventType.STEP_COMPLETED,
              TimeProvider.now(),
              Map.of("kind", "model_call", "model", p.key(), "model_run_id", modelRunId, "error", ex.getMessage())
          ));
        }

        run.setDecisionRecord(dr, TimeProvider.now());
        runRepository.save(run);
      }

      // 2) Evaluators：每个成功的 model_run 执行（constraint/consistency/completeness）
      for (String mrId : modelRunIds) {
        if (!"SUCCEEDED".equals(getModelRunStatus(dr, mrId))) continue;

        var r1 = constraint.evaluate(dr, mrId);
        appendEvalResult(dr, "constraint", mrId, r1.score, r1.passed, r1.reason, r1.metrics);

        var r2 = consistency.evaluate(dr, mrId);
        appendEvalResult(dr, "consistency", mrId, r2.score, r2.passed, r2.reason, r2.metrics);

        var r3 = completeness.evaluate(dr, mrId);
        appendEvalResult(dr, "completeness", mrId, r3.score, r3.passed, r3.reason, r3.metrics);

        boolean gatePassed = r1.passed && r2.passed;
        eventStore.append(new LedgerEvent(
            IdGenerator.newId(),
            run.getRunId(),
            run.getTenantId(),
            LedgerEventType.GATE_EVALUATED,
            TimeProvider.now(),
            Map.of("model_run_id", mrId, "gate_passed", gatePassed)
        ));
      }

      run.setDecisionRecord(dr, TimeProvider.now());
      runRepository.save(run);

      // 3) v1 策略：gate + quality，选最优
      DecisionPick pick = pickBestByV1Rule(dr);
      if (pick == null) {
        markDecisionStatus(run, "FAILED");
        run.fail(TimeProvider.now());
        runRepository.save(run);

        eventStore.append(new LedgerEvent(
            IdGenerator.newId(),
            run.getRunId(),
            run.getTenantId(),
            LedgerEventType.RUN_FAILED,
            TimeProvider.now(),
            Map.of("reason", "no_candidates_after_gate")
        ));
        return run;
      }

      writeFinalDecision(dr, pick);
      markDecisionStatus(run, "DONE");

      run.succeed(TimeProvider.now());
      run.setDecisionRecord(dr, TimeProvider.now());
      runRepository.save(run);

      eventStore.append(new LedgerEvent(
          IdGenerator.newId(),
          run.getRunId(),
          run.getTenantId(),
          LedgerEventType.DECISION_FINALIZED,
          TimeProvider.now(),
          Map.of("selected_model_run_id", pick.modelRunId, "confidence", pick.confidence, "quality", pick.quality)
      ));

      return run;

    } catch (Exception ex) {
      markDecisionStatus(run, "FAILED");
      run.fail(TimeProvider.now());
      runRepository.save(run);

      eventStore.append(new LedgerEvent(
          IdGenerator.newId(),
          run.getRunId(),
          run.getTenantId(),
          LedgerEventType.RUN_FAILED,
          TimeProvider.now(),
          Map.of("error", ex.getMessage())
      ));
      throw ex;
    }
  }

  public Run get(String runId) {
    return runRepository.findById(runId).orElseThrow();
  }

  // ------------------------- helpers -------------------------

  private static Map<String, Object> safeMap(Map<String, Object> m) {
    return m == null ? Map.of() : m;
  }

  private static Duration resolveTimeout(Map<String, Object> options) {
    if (options == null) return Duration.ofSeconds(60);
    Object t = options.get("timeout_ms");
    if (t instanceof Number) return Duration.ofMillis(((Number) t).longValue());
    return Duration.ofSeconds(60);
  }

  @SuppressWarnings("unchecked")
  private void markDecisionStatus(Run run, String status) {
    Map<String, Object> dr = run.getDecisionRecord();
    if (dr == null) return;
    dr.put("status", status);
    run.setDecisionRecord(dr, TimeProvider.now());
  }

  @SuppressWarnings("unchecked")
  private void appendModelRun(Map<String, Object> dr, String runId, String modelName, String status) {
    List<Map<String, Object>> runs = (List<Map<String, Object>>) dr.get("model_runs");
    if (runs == null) {
      runs = new ArrayList<>();
      dr.put("model_runs", runs);
    }

    Map<String, Object> item = new LinkedHashMap<>();
    item.put("run_id", runId);
    item.put("model_name", modelName);
    item.put("provider", "internal");
    item.put("status", status);
    item.put("started_at", TimeProvider.now().toString());
    item.put("finished_at", TimeProvider.now().toString());
    item.put("latency_ms", 0);

    Map<String, Object> prompt = new LinkedHashMap<>();
    prompt.put("system", "");
    prompt.put("developer", "");
    prompt.put("user", "");
    prompt.put("tools", List.of());
    prompt.put("temperature", 1.0);
    prompt.put("top_p", 1.0);
    item.put("prompt_snapshot", prompt);

    item.put("raw_output", new LinkedHashMap<>(Map.of("text", "")));
    item.put("usage", new LinkedHashMap<>());
    item.put("tool_traces", new ArrayList<>());
    item.put("errors", new ArrayList<>());

    runs.add(item);
  }

  @SuppressWarnings("unchecked")
  private void writePromptSnapshot(Map<String, Object> dr, String modelRunId, String userPrompt,
                                  Map<String, Object> options, ModelProvider provider) {
    Map<String, Object> r = findModelRun(dr, modelRunId);
    if (r == null) return;

    Map<String, Object> prompt = (Map<String, Object>) r.get("prompt_snapshot");
    if (prompt == null) prompt = new LinkedHashMap<>();

    // v1：system/developer 留空；你后续可接入你自己的 prompt builder
    prompt.put("system", "");
    prompt.put("developer", "");
    prompt.put("user", userPrompt == null ? "" : userPrompt);

    // 把“是否支持 tools / schema / maxTokensHint”固化到 tools 字段，便于复盘
    List<String> tools = new ArrayList<>();
    ModelProvider.Capabilities cap = provider.capabilities();
    if (cap != null) {
      if (cap.supportsTools()) tools.add("supportsTools=true");
      if (cap.supportsJsonSchema()) tools.add("supportsJsonSchema=true");
      tools.add("maxTokensHint=" + cap.maxTokensHint());
    }
    prompt.put("tools", tools);

    // 常用推理参数（若 options 里有就固化）
    Object temperature = options == null ? null : options.get("temperature");
    if (temperature instanceof Number) prompt.put("temperature", ((Number) temperature).doubleValue());

    Object topP = options == null ? null : options.get("top_p");
    if (topP instanceof Number) prompt.put("top_p", ((Number) topP).doubleValue());

    r.put("prompt_snapshot", prompt);
  }

  @SuppressWarnings("unchecked")
  private void updateModelRunStatus(Map<String, Object> dr, String runId, String status) {
    Map<String, Object> r = findModelRun(dr, runId);
    if (r == null) return;
    r.put("status", status);
    if ("RUNNING".equals(status)) {
      r.put("started_at", TimeProvider.now().toString());
    }
    r.put("finished_at", TimeProvider.now().toString());
  }

  @SuppressWarnings("unchecked")
  private void finishModelRunSucceeded(Map<String, Object> dr, String runId, long latencyMs, ModelOutput out) {
    Map<String, Object> r = findModelRun(dr, runId);
    if (r == null) return;

    r.put("status", "SUCCEEDED");
    r.put("finished_at", TimeProvider.now().toString());
    r.put("latency_ms", latencyMs);

    // raw_output.text
    Map<String, Object> raw = (Map<String, Object>) r.get("raw_output");
    if (raw == null) raw = new LinkedHashMap<>();
    raw.put("text", out == null ? "" : (out.text() == null ? "" : out.text()));
    // 保留 providerKey/meta 供复盘（不破坏 schema：raw_output 允许扩展）
    if (out != null) {
      raw.put("providerKey", out.providerKey());
      raw.put("meta", out.meta() == null ? Map.of() : out.meta());
    }
    r.put("raw_output", raw);

    // usage：从 meta 提取（若存在）
    Map<String, Object> usage = new LinkedHashMap<>();
    if (out != null && out.meta() != null) {
      putIfNumber(usage, "input_tokens", out.meta().get("input_tokens"));
      putIfNumber(usage, "output_tokens", out.meta().get("output_tokens"));
      putIfNumber(usage, "total_tokens", out.meta().get("total_tokens"));
      putIfNumber(usage, "cost_usd", out.meta().get("cost_usd"));
    }
    r.put("usage", usage);

    // tool_traces：从 meta 提取（若存在）
    List<Map<String, Object>> traces = new ArrayList<>();
    if (out != null && out.meta() != null) {
      Object t = out.meta().get("tool_traces");
      if (t instanceof List) {
        for (Object o : (List<?>) t) {
          if (o instanceof Map) traces.add(new LinkedHashMap<>((Map<String, Object>) o));
          else traces.add(Map.of("value", String.valueOf(o)));
        }
      }
      // 兼容：meta.tools_used = ["web.run","mcp","skills"]
      Object toolsUsed = out.meta().get("tools_used");
      if (toolsUsed instanceof List) {
        for (Object o : (List<?>) toolsUsed) {
          traces.add(Map.of("tool", String.valueOf(o)));
        }
      }
    }
    r.put("tool_traces", traces);
  }

  private static void putIfNumber(Map<String, Object> dst, String key, Object v) {
    if (v instanceof Number) dst.put(key, v);
    else if (v != null) {
      try { dst.put(key, Double.parseDouble(String.valueOf(v))); } catch (Exception ignore) {}
    }
  }

  @SuppressWarnings("unchecked")
  private void finishModelRunFailed(Map<String, Object> dr, String runId, Exception ex) {
    Map<String, Object> r = findModelRun(dr, runId);
    if (r == null) return;
    r.put("status", "FAILED");
    r.put("finished_at", TimeProvider.now().toString());

    List<String> errors = (List<String>) r.get("errors");
    if (errors == null) errors = new ArrayList<>();
    errors.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
    r.put("errors", errors);
  }

  @SuppressWarnings("unchecked")
  private String getModelRunStatus(Map<String, Object> dr, String runId) {
    Map<String, Object> r = findModelRun(dr, runId);
    if (r == null) return "";
    return String.valueOf(r.get("status"));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> findModelRun(Map<String, Object> dr, String runId) {
    Object runsObj = dr.get("model_runs");
    if (!(runsObj instanceof List)) return null;
    for (Map<String, Object> r : (List<Map<String, Object>>) runsObj) {
      if (runId.equals(String.valueOf(r.get("run_id")))) return r;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private void appendEvalResult(Map<String, Object> dr, String evaluator, String modelRunId,
                               double score, boolean passed, String reason, Map<String, Object> metrics) {
    List<Map<String, Object>> evals = (List<Map<String, Object>>) dr.get("eval_results");
    if (evals == null) {
      evals = new ArrayList<>();
      dr.put("eval_results", evals);
    }

    Map<String, Object> item = new LinkedHashMap<>();
    item.put("evaluator", evaluator);
    item.put("target", Map.of("type", "model_run", "ref", modelRunId));
    item.put("score", score);
    item.put("passed", passed);
    item.put("reason", reason);
    item.put("metrics", metrics == null ? Map.of() : metrics);

    evals.add(item);
  }

  // ------------------------- v1 策略：gate + quality（rule_based）-------------------------

  private static final class DecisionPick {
    final String modelRunId;
    final double quality;
    final double confidence;

    private DecisionPick(String modelRunId, double quality, double confidence) {
      this.modelRunId = modelRunId;
      this.quality = quality;
      this.confidence = confidence;
    }
  }

  @SuppressWarnings("unchecked")
  private DecisionPick pickBestByV1Rule(Map<String, Object> dr) {
    List<Map<String, Object>> evals = (List<Map<String, Object>>) dr.getOrDefault("eval_results", List.of());

    Map<String, Map<String, Map<String, Object>>> byRun = new HashMap<>();
    for (Map<String, Object> e : evals) {
      Map<String, Object> tgt = (Map<String, Object>) e.get("target");
      if (tgt == null) continue;
      if (!"model_run".equals(String.valueOf(tgt.get("type")))) continue;
      String ref = String.valueOf(tgt.get("ref"));
      String name = String.valueOf(e.get("evaluator"));
      byRun.computeIfAbsent(ref, k -> new HashMap<>()).put(name, e);
    }

    double bestQ = -1;
    String bestRunId = null;

    for (String runId : byRun.keySet()) {
      Map<String, Map<String, Object>> m = byRun.get(runId);

      boolean gate = isPassed(m, "constraint") && isPassed(m, "consistency");
      if (!gate) continue;

      double c = scoreOf(m, "constraint");
      double s = scoreOf(m, "consistency");
      double p = scoreOf(m, "completeness");

      double q = 0.50 * p + 0.30 * s + 0.20 * c;

      if (q > bestQ) {
        bestQ = q;
        bestRunId = runId;
      }
    }

    if (bestRunId == null) return null;

    double conf = Math.max(0, Math.min(1, bestQ));
    return new DecisionPick(bestRunId, bestQ, conf);
  }

  private boolean isPassed(Map<String, Map<String, Object>> m, String name) {
    Map<String, Object> r = m.get(name);
    return r != null && Boolean.TRUE.equals(r.get("passed"));
  }

  private double scoreOf(Map<String, Map<String, Object>> m, String name) {
    Map<String, Object> r = m.get(name);
    if (r == null) return 0;
    Object v = r.get("score");
    if (v instanceof Number) return ((Number) v).doubleValue();
    try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) { return 0; }
  }

  @SuppressWarnings("unchecked")
  private void writeFinalDecision(Map<String, Object> dr, DecisionPick pick) {
    Map<String, Object> fd = (Map<String, Object>) dr.get("final_decision");
    if (fd == null) fd = new LinkedHashMap<>();

    fd.put("strategy", Map.of("type", "rule_based", "params", Map.of("rule", "pick_best_quality")));
    fd.put("selected", Map.of("type", "model_run", "ref", pick.modelRunId));
    fd.put("confidence", pick.confidence);

    Map<String, Object> mr = findModelRun(dr, pick.modelRunId);
    String text = "";
    if (mr != null) {
      Object rawObj = mr.get("raw_output");
      if (rawObj instanceof Map) {
        Object t = ((Map<?, ?>) rawObj).get("text");
        text = t == null ? "" : String.valueOf(t);
      }
    }
    fd.put("output", Map.of("text", text, "format", "markdown"));
    fd.put("warnings", List.of("v1_rule_based_pick_best_quality", "quality=" + pick.quality));

    dr.put("final_decision", fd);
  }
}
