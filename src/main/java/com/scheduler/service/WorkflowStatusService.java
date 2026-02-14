package com.scheduler.service;

import com.scheduler.model.RunnerInfo;
import com.scheduler.model.WorkflowRunStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流运行状态服务
 * 负责维护、查询 GitHub Action 运行状态，并支持实时状态推送
 */
@Service
public class WorkflowStatusService {

    /**
     * 运行状态存储（模拟数据库）
     */
    private final Map<String, WorkflowRunStatus> runStatusMap = new ConcurrentHashMap<>();

    /**
     * 状态变更事件 Sink，用于实时推送（多播）
     */
    private final Sinks.Many<WorkflowRunStatus> statusSink =
            Sinks.many().multicast().onBackpressureBuffer();

    /**
     * 注册一次新的调度运行
     *
     * @param runId      运行ID
     * @param actionName Action名称
     * @return 初始状态
     */
    public WorkflowRunStatus registerRun(String runId, String actionName) {
        WorkflowRunStatus status = new WorkflowRunStatus(
                runId,
                actionName,
                WorkflowRunStatus.RunStatus.SCHEDULED,
                LocalDateTime.now(),
                null,
                null,
                new ArrayList<>(),
                "Scheduled"
        );
        runStatusMap.put(runId, status);
        publishStatus(status);
        return status;
    }

    /**
     * 更新运行状态
     *
     * @param runId   运行ID
     * @param status  新状态
     * @param runners Runner信息列表
     * @param message 消息
     */
    public void updateRunStatus(String runId,
                                WorkflowRunStatus.RunStatus status,
                                java.util.List<RunnerInfo> runners,
                                String message) {
        WorkflowRunStatus runStatus = runStatusMap.get(runId);
        if (runStatus == null) {
            return;
        }
        runStatus.setStatus(status);
        if (runners != null) {
            runStatus.setRunners(runners);
        }
        if (message != null) {
            runStatus.setMessage(message);
        }
        if (status == WorkflowRunStatus.RunStatus.RUNNING && runStatus.getStartedAt() == null) {
            runStatus.setStartedAt(LocalDateTime.now());
        }
        if (status == WorkflowRunStatus.RunStatus.SUCCESS
                || status == WorkflowRunStatus.RunStatus.FAILURE
                || status == WorkflowRunStatus.RunStatus.CANCELLED) {
            runStatus.setCompletedAt(LocalDateTime.now());
        }
        publishStatus(runStatus);
    }

    /**
     * 查询单次运行的当前状态
     *
     * @param runId 运行ID
     * @return 运行状态，不存在时返回 empty
     */
    public Mono<WorkflowRunStatus> getRunStatus(String runId) {
        WorkflowRunStatus status = runStatusMap.get(runId);
        if (status == null) {
            return Mono.empty();
        }
        return Mono.just(status);
    }

    /**
     * 查询所有调度记录
     *
     * @return 所有运行状态列表
     */
    public Flux<WorkflowRunStatus> getAllRunStatuses() {
        Collection<WorkflowRunStatus> all = runStatusMap.values();
        return Flux.fromIterable(all);
    }

    /**
     * 实时订阅某次运行的状态变更（SSE 流）
     * 先推送当前状态快照，再持续推送后续变更，直到运行结束
     *
     * @param runId 运行ID
     * @return 状态变更事件流
     */
    public Flux<WorkflowRunStatus> streamRunStatus(String runId) {
        // 当前快照（如果存在）
        Flux<WorkflowRunStatus> snapshot = getRunStatus(runId).flux();

        // 后续变更事件（过滤对应 runId）
        Flux<WorkflowRunStatus> updates = statusSink.asFlux()
                .filter(s -> runId.equals(s.getRunId()))
                // 当状态为终态时结束流
                .takeUntil(s -> s.getStatus() == WorkflowRunStatus.RunStatus.SUCCESS
                        || s.getStatus() == WorkflowRunStatus.RunStatus.FAILURE
                        || s.getStatus() == WorkflowRunStatus.RunStatus.CANCELLED)
                // 超时保护：30秒无更新则结束
                .timeout(Duration.ofSeconds(30), Flux.empty());

        return Flux.concat(snapshot, updates);
    }

    /**
     * 实时订阅所有运行的状态变更（SSE 流）
     *
     * @return 所有运行的状态变更事件流
     */
    public Flux<WorkflowRunStatus> streamAllRunStatuses() {
        return statusSink.asFlux()
                .timeout(Duration.ofSeconds(60), Flux.empty());
    }

    /**
     * 发布状态变更事件
     */
    private void publishStatus(WorkflowRunStatus status) {
        statusSink.tryEmitNext(status);
    }
}
