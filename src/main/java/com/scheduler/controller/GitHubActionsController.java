package com.scheduler.controller;

import com.scheduler.model.WorkflowRun;
import com.scheduler.model.WorkflowRunsResponse;
import com.scheduler.service.GitHubActionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * GitHub Actions状态监控控制器
 * 提供实时获取GitHub Actions工作流运行状态的API
 */
@RestController
@RequestMapping("/api/v1/github-actions")
@RequiredArgsConstructor
public class GitHubActionsController {

    private final GitHubActionsService gitHubActionsService;

    /**
     * 获取仓库的工作流运行列表
     * GET /api/v1/github-actions/{owner}/{repo}/runs
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param perPage 每页数量（可选，默认30）
     * @param page 页码（可选，默认1）
     * @return 工作流运行列表
     */
    @GetMapping("/{owner}/{repo}/runs")
    public Mono<WorkflowRunsResponse> getWorkflowRuns(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false, defaultValue = "30") Integer perPage,
            @RequestParam(required = false, defaultValue = "1") Integer page) {
        return gitHubActionsService.getWorkflowRuns(owner, repo, perPage, page);
    }

    /**
     * 获取单个工作流运行的详细信息
     * GET /api/v1/github-actions/{owner}/{repo}/runs/{runId}
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 运行ID
     * @return 工作流运行详情
     */
    @GetMapping("/{owner}/{repo}/runs/{runId}")
    public Mono<WorkflowRun> getWorkflowRun(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long runId) {
        return gitHubActionsService.getWorkflowRun(owner, repo, runId);
    }

    /**
     * 获取特定分支的工作流运行列表
     * GET /api/v1/github-actions/{owner}/{repo}/runs/branch/{branch}
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param branch 分支名称
     * @param perPage 每页数量（可选，默认30）
     * @return 工作流运行列表
     */
    @GetMapping("/{owner}/{repo}/runs/branch/{branch}")
    public Mono<WorkflowRunsResponse> getWorkflowRunsByBranch(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String branch,
            @RequestParam(required = false, defaultValue = "30") Integer perPage) {
        return gitHubActionsService.getWorkflowRunsByBranch(owner, repo, branch, perPage);
    }

    /**
     * 获取特定状态的工作流运行列表
     * GET /api/v1/github-actions/{owner}/{repo}/runs/status/{status}
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param status 状态 (completed, in_progress, queued)
     * @param perPage 每页数量（可选，默认30）
     * @return 工作流运行列表
     */
    @GetMapping("/{owner}/{repo}/runs/status/{status}")
    public Mono<WorkflowRunsResponse> getWorkflowRunsByStatus(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String status,
            @RequestParam(required = false, defaultValue = "30") Integer perPage) {
        return gitHubActionsService.getWorkflowRunsByStatus(owner, repo, status, perPage);
    }
}
