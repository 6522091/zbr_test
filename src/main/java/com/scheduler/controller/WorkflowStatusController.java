package com.scheduler.controller;

import com.scheduler.model.WorkflowRunStatus;
import com.scheduler.service.WorkflowStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GitHub Action 运行调度状态控制器
 * 提供运行状态查询及实时 SSE 流式推送接口
 */
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class WorkflowStatusController {

    private final WorkflowStatusService workflowStatusService;

    /**
     * 查询所有 GitHub Action 运行调度记录
     * GET /api/v1/workflow/runs
     *
     * @return 所有运行状态列表
     */
    @GetMapping("/runs")
    public Flux<WorkflowRunStatus> listAllRuns() {
        return workflowStatusService.getAllRunStatuses();
    }

    /**
     * 查询单次 GitHub Action 运行的当前状态
     * GET /api/v1/workflow/runs/{runId}/status
     *
     * @param runId 运行ID（由 POST /api/v1/run 返回）
     * @return 运行状态
     */
    @GetMapping("/runs/{runId}/status")
    public Mono<WorkflowRunStatus> getRunStatus(@PathVariable String runId) {
        return workflowStatusService.getRunStatus(runId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Run not found: " + runId)));
    }

    /**
     * 实时订阅单次 GitHub Action 运行的状态变更（Server-Sent Events）
     * GET /api/v1/workflow/runs/{runId}/status/stream
     *
     * <p>客户端建立 SSE 连接后将立即收到当前状态快照，
     * 后续每次状态变更都会实时推送，直至运行结束（SUCCESS/FAILURE/CANCELLED）
     * 或超过 30 秒无更新为止。</p>
     *
     * @param runId 运行ID
     * @return SSE 状态事件流
     */
    @GetMapping(value = "/runs/{runId}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkflowRunStatus> streamRunStatus(@PathVariable String runId) {
        return workflowStatusService.streamRunStatus(runId);
    }

    /**
     * 实时订阅所有 GitHub Action 运行的状态变更（Server-Sent Events）
     * GET /api/v1/workflow/runs/stream
     *
     * <p>全局状态变更监听，每当任意 Action 运行状态发生变化时推送事件。
     * 连接默认保持 60 秒。</p>
     *
     * @return SSE 全局状态事件流
     */
    @GetMapping(value = "/runs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WorkflowRunStatus> streamAllRunStatuses() {
        return workflowStatusService.streamAllRunStatuses();
    }
}
