package com.scheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.WorkflowRun;
import com.scheduler.model.WorkflowRunsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Actions API服务
 * 用于实时获取GitHub Actions工作流运行状态
 */
@Service
@Slf4j
public class GitHubActionsService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public GitHubActionsService(
            @Value("${github.api.token:}") String githubToken,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        // 如果配置了token，添加认证头
        if (githubToken != null && !githubToken.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + githubToken);
        }

        this.webClient = builder.build();
    }

    /**
     * 获取仓库的工作流运行列表
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param perPage 每页数量
     * @param page 页码
     * @return 工作流运行响应
     */
    public Mono<WorkflowRunsResponse> getWorkflowRuns(String owner, String repo, Integer perPage, Integer page) {
        String url = String.format("/repos/%s/%s/actions/runs", owner, repo);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    if (perPage != null) {
                        uriBuilder.queryParam("per_page", perPage);
                    }
                    if (page != null) {
                        uriBuilder.queryParam("page", page);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseWorkflowRunsResponse)
                .doOnError(error -> log.error("Error fetching workflow runs: {}", error.getMessage()));
    }

    /**
     * 获取单个工作流运行的详细信息
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param runId 运行ID
     * @return 工作流运行详情
     */
    public Mono<WorkflowRun> getWorkflowRun(String owner, String repo, Long runId) {
        String url = String.format("/repos/%s/%s/actions/runs/%d", owner, repo, runId);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseWorkflowRun)
                .doOnError(error -> log.error("Error fetching workflow run {}: {}", runId, error.getMessage()));
    }

    /**
     * 获取特定分支的工作流运行列表
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param branch 分支名称
     * @param perPage 每页数量
     * @return 工作流运行响应
     */
    public Mono<WorkflowRunsResponse> getWorkflowRunsByBranch(String owner, String repo, String branch, Integer perPage) {
        String url = String.format("/repos/%s/%s/actions/runs", owner, repo);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url)
                            .queryParam("branch", branch);
                    if (perPage != null) {
                        uriBuilder.queryParam("per_page", perPage);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseWorkflowRunsResponse)
                .doOnError(error -> log.error("Error fetching workflow runs for branch {}: {}", branch, error.getMessage()));
    }

    /**
     * 获取特定状态的工作流运行列表
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param status 状态 (completed, in_progress, queued)
     * @param perPage 每页数量
     * @return 工作流运行响应
     */
    public Mono<WorkflowRunsResponse> getWorkflowRunsByStatus(String owner, String repo, String status, Integer perPage) {
        String url = String.format("/repos/%s/%s/actions/runs", owner, repo);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url)
                            .queryParam("status", status);
                    if (perPage != null) {
                        uriBuilder.queryParam("per_page", perPage);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseWorkflowRunsResponse)
                .doOnError(error -> log.error("Error fetching workflow runs with status {}: {}", status, error.getMessage()));
    }

    /**
     * 解析工作流运行列表响应
     */
    private WorkflowRunsResponse parseWorkflowRunsResponse(JsonNode jsonNode) {
        WorkflowRunsResponse response = new WorkflowRunsResponse();
        response.setTotalCount(jsonNode.get("total_count").asInt());

        List<WorkflowRun> workflowRuns = new ArrayList<>();
        JsonNode runsNode = jsonNode.get("workflow_runs");

        if (runsNode != null && runsNode.isArray()) {
            for (JsonNode runNode : runsNode) {
                workflowRuns.add(parseWorkflowRun(runNode));
            }
        }

        response.setWorkflowRuns(workflowRuns);
        return response;
    }

    /**
     * 解析单个工作流运行
     */
    private WorkflowRun parseWorkflowRun(JsonNode node) {
        WorkflowRun run = new WorkflowRun();
        run.setId(node.get("id").asLong());
        run.setRunNumber(node.get("run_number").asInt());
        run.setName(node.get("name").asText());
        run.setStatus(node.get("status").asText());

        JsonNode conclusionNode = node.get("conclusion");
        if (conclusionNode != null && !conclusionNode.isNull()) {
            run.setConclusion(conclusionNode.asText());
        }

        run.setHeadBranch(node.get("head_branch").asText());
        run.setHeadSha(node.get("head_sha").asText());
        run.setHtmlUrl(node.get("html_url").asText());
        run.setEvent(node.get("event").asText());

        JsonNode actorNode = node.get("actor");
        if (actorNode != null && actorNode.has("login")) {
            run.setActor(actorNode.get("login").asText());
        }

        // 解析时间
        if (node.has("created_at") && !node.get("created_at").isNull()) {
            run.setCreatedAt(LocalDateTime.parse(node.get("created_at").asText(), ISO_FORMATTER));
        }
        if (node.has("updated_at") && !node.get("updated_at").isNull()) {
            run.setUpdatedAt(LocalDateTime.parse(node.get("updated_at").asText(), ISO_FORMATTER));
        }
        if (node.has("run_started_at") && !node.get("run_started_at").isNull()) {
            run.setRunStartedAt(LocalDateTime.parse(node.get("run_started_at").asText(), ISO_FORMATTER));
        }

        return run;
    }
}
