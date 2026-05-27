package com.guidinglight.decisionhub.api.research;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.error.BizException;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage1：ResearchRun REST 入口。
 *
 * <p>对应工单 4.6：/api/ai/research-runs 系列接口。
 */
@RestController
@RequestMapping("/api/ai/research-runs")
public final class ResearchRunController {

  private final ResearchRunCommandService commandService;
  private final ResearchRunQueryService queryService;

  /** 构造。 */
  public ResearchRunController(
      final ResearchRunCommandService commandService,
      final ResearchRunQueryService queryService) {
    this.commandService = commandService;
    this.queryService = queryService;
  }

  /** 创建新 ResearchRun。 */
  @PostMapping
  public ApiResponse<ResearchRunView> create(
      @Valid @RequestBody final CreateResearchRunRequest req,
      final HttpServletRequest httpRequest) {
    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final ResearchRun run =
        commandService.create(tenantId, req.getTopic(), req.getPayloadJson());
    return ApiResponse.ok(ResearchRunView.of(run), traceId(httpRequest));
  }

  /** 列出当前租户的全部 run。 */
  @GetMapping
  public ApiResponse<List<ResearchRunView>> list(final HttpServletRequest httpRequest) {
    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final List<ResearchRunView> views =
        queryService.listRuns(tenantId).stream().map(ResearchRunView::of).toList();
    return ApiResponse.ok(views, traceId(httpRequest));
  }

  /** 查询 run 详情。 */
  @GetMapping("/{runId}")
  public ApiResponse<ResearchRunView> detail(
      @PathVariable final String runId, final HttpServletRequest httpRequest) {
    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final ResearchRun run =
        queryService.findRun(runId).orElseThrow(() -> notFound(runId));
    AuthenticatedRequest.requireTenantMatch(tenantId, run.getTenantId());
    return ApiResponse.ok(ResearchRunView.of(run), traceId(httpRequest));
  }

  /** 启动 run。 */
  @PostMapping("/{runId}/start")
  public ApiResponse<ResearchRunView> start(
      @PathVariable final String runId, final HttpServletRequest httpRequest) {
    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final ResearchRun beforeStart =
        queryService.findRun(runId).orElseThrow(() -> notFound(runId));
    AuthenticatedRequest.requireTenantMatch(tenantId, beforeStart.getTenantId());
    final ResearchRun run = commandService.start(runId);
    return ApiResponse.ok(ResearchRunView.of(run), traceId(httpRequest));
  }

  /** 查询 run 的任务图。 */
  @GetMapping("/{runId}/tasks")
  public ApiResponse<AgentTask> tasks(
      @PathVariable final String runId, final HttpServletRequest httpRequest) {
    assertRunTenant(runId, httpRequest);
    final AgentTask task = queryService.findTask(runId).orElseThrow(() -> notFound(runId));
    return ApiResponse.ok(task, traceId(httpRequest));
  }

  /** 查询 run 下的候选列表。 */
  @GetMapping("/{runId}/candidates")
  public ApiResponse<List<StrategyCandidate>> candidates(
      @PathVariable final String runId, final HttpServletRequest httpRequest) {
    assertRunTenant(runId, httpRequest);
    return ApiResponse.ok(queryService.listCandidates(runId), traceId(httpRequest));
  }

  /** 查询 run 的 JudgeDecision。 */
  @GetMapping("/{runId}/judge-decision")
  public ApiResponse<JudgeDecision> judgeDecision(
      @PathVariable final String runId, final HttpServletRequest httpRequest) {
    assertRunTenant(runId, httpRequest);
    final JudgeDecision decision =
        queryService.findJudgeDecision(runId).orElseThrow(() -> notFound(runId));
    return ApiResponse.ok(decision, traceId(httpRequest));
  }

  private void assertRunTenant(final String runId, final HttpServletRequest httpRequest) {
    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final ResearchRun run =
        queryService.findRun(runId).orElseThrow(() -> notFound(runId));
    AuthenticatedRequest.requireTenantMatch(tenantId, run.getTenantId());
  }

  private static BizException notFound(final String runId) {
    return new BizException(
        CommonErrorCodes.NOT_FOUND, "research run not found: " + runId, null, null);
  }

  private static String traceId(final HttpServletRequest httpRequest) {
    final Object attr = httpRequest.getAttribute(TraceIdFilter.TRACE_HEADER);
    return attr == null ? null : attr.toString();
  }
}
