package com.scheduler.controller;

import com.scheduler.model.WorkflowRunStatus;
import com.scheduler.model.WorkflowRunsResponse;
import com.scheduler.service.GitHubActionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GitHub Actions 状态查询控制器
 * 提供实时获取 GitHub Actions 工作流运行状态的接口
 */
@RestController
@RequestMapping("/api/v1/github-actions")
@RequiredArgsConstructor
public class GitHubActionsController {

    private final GitHubActionsService gitHubActionsService;

    /**
     * 获取工作流运行列表
     * GET /api/v1/github-actions/{owner}/{repo}/runs
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param perPage 每页数量（可选，默认 30）
     * @return 工作流运行列表
     */
    @GetMapping("/{owner}/{repo}/runs")
    public Mono<WorkflowRunsResponse> getWorkflowRuns(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false, defaultValue = "30") Integer perPage) {
        return gitHubActionsService.getWorkflowRuns(owner, repo, perPage);
    }

    /**
     * 获取指定工作流运行的状态
     * GET /api/v1/github-actions/{owner}/{repo}/runs/{runId}
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 工作流运行 ID
     * @return 工作流运行状态
     */
    @GetMapping("/{owner}/{repo}/runs/{runId}")
    public Mono<WorkflowRunStatus> getWorkflowRunStatus(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long runId) {
        return gitHubActionsService.getWorkflowRunStatus(owner, repo, runId);
    }

    /**
     * 实时监控工作流运行状态（Server-Sent Events）
     * GET /api/v1/github-actions/{owner}/{repo}/runs/{runId}/watch
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 工作流运行 ID
     * @param interval 轮询间隔（秒，可选，默认 5）
     * @return 工作流运行状态流（SSE）
     */
    @GetMapping(value = "/{owner}/{repo}/runs/{runId}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkflowRunStatus> watchWorkflowRunStatus(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long runId,
            @RequestParam(required = false, defaultValue = "5") Long interval) {
        return gitHubActionsService.watchWorkflowRunStatus(owner, repo, runId, interval);
    }
}
