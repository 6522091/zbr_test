package com.scheduler.service;

import com.scheduler.model.WorkflowRunStatus;
import com.scheduler.model.WorkflowRunsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * GitHub Actions API 服务
 * 用于实时获取工作流运行状态
 */
@Slf4j
@Service
public class GitHubActionsService {

    private final WebClient webClient;

    @Value("${github.api.token:}")
    private String githubToken;

    public GitHubActionsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * 获取指定仓库的工作流运行列表
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param perPage 每页数量，默认 30，最大 100
     * @return 工作流运行列表
     */
    public Mono<WorkflowRunsResponse> getWorkflowRuns(String owner, String repo, Integer perPage) {
        String uri = String.format("/repos/%s/%s/actions/runs?per_page=%d", owner, repo, perPage != null ? perPage : 30);

        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(uri);

        if (githubToken != null && !githubToken.isEmpty()) {
            request = request.header("Authorization", "Bearer " + githubToken);
        }

        return request.retrieve()
                .bodyToMono(Map.class)
                .map(this::convertToWorkflowRunsResponse)
                .doOnError(error -> log.error("Error fetching workflow runs: {}", error.getMessage()));
    }

    /**
     * 获取指定工作流运行的详细状态
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 工作流运行 ID
     * @return 工作流运行状态
     */
    public Mono<WorkflowRunStatus> getWorkflowRunStatus(String owner, String repo, Long runId) {
        String uri = String.format("/repos/%s/%s/actions/runs/%d", owner, repo, runId);

        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(uri);

        if (githubToken != null && !githubToken.isEmpty()) {
            request = request.header("Authorization", "Bearer " + githubToken);
        }

        return request.retrieve()
                .bodyToMono(Map.class)
                .map(this::convertToWorkflowRunStatus)
                .doOnError(error -> log.error("Error fetching workflow run status: {}", error.getMessage()));
    }

    /**
     * 实时轮询获取工作流运行状态（Server-Sent Events 流）
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 工作流运行 ID
     * @param intervalSeconds 轮询间隔（秒）
     * @return 工作流运行状态流
     */
    public Flux<WorkflowRunStatus> watchWorkflowRunStatus(String owner, String repo, Long runId, Long intervalSeconds) {
        long interval = intervalSeconds != null ? intervalSeconds : 5;

        return Flux.interval(java.time.Duration.ofSeconds(interval))
                .flatMap(tick -> getWorkflowRunStatus(owner, repo, runId))
                .distinctUntilChanged(WorkflowRunStatus::getStatus)
                .takeUntil(status -> "completed".equalsIgnoreCase(status.getStatus()));
    }

    /**
     * 转换 API 响应为 WorkflowRunsResponse
     */
    private WorkflowRunsResponse convertToWorkflowRunsResponse(Map<String, Object> response) {
        WorkflowRunsResponse result = new WorkflowRunsResponse();
        result.setTotalCount((Integer) response.get("total_count"));

        List<Map<String, Object>> workflowRuns = (List<Map<String, Object>>) response.get("workflow_runs");
        if (workflowRuns != null) {
            List<WorkflowRunStatus> statusList = workflowRuns.stream()
                    .map(this::convertToWorkflowRunStatus)
                    .toList();
            result.setWorkflowRuns(statusList);
        }

        return result;
    }

    /**
     * 转换 API 响应为 WorkflowRunStatus
     */
    private WorkflowRunStatus convertToWorkflowRunStatus(Map<String, Object> run) {
        WorkflowRunStatus status = new WorkflowRunStatus();

        status.setId(((Number) run.get("id")).longValue());
        status.setName((String) run.get("name"));
        status.setRunNumber((Integer) run.get("run_number"));
        status.setStatus((String) run.get("status"));
        status.setConclusion((String) run.get("conclusion"));
        status.setEvent((String) run.get("event"));
        status.setHeadBranch((String) run.get("head_branch"));
        status.setHeadSha((String) run.get("head_sha"));
        status.setHtmlUrl((String) run.get("html_url"));

        // 转换时间
        String createdAt = (String) run.get("created_at");
        if (createdAt != null) {
            status.setCreatedAt(ZonedDateTime.parse(createdAt).toLocalDateTime());
        }

        String updatedAt = (String) run.get("updated_at");
        if (updatedAt != null) {
            status.setUpdatedAt(ZonedDateTime.parse(updatedAt).toLocalDateTime());
        }

        return status;
    }
}
