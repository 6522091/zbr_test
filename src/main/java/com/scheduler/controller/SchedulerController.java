package com.scheduler.controller;

import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import com.scheduler.service.ActionSchedulerService;
import com.scheduler.service.RunStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 主调度控制器
 * 提供/run接口用于接收和调度GitHub Action
 * 提供/status接口用于查询GitHub Action运行状态
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchedulerController {

    private final ActionSchedulerService actionSchedulerService;
    private final RunStatusService runStatusService;
    
    /**
     * 运行Action
     * POST /api/v1/run
     *
     * @param request GitHub Action配置
     * @return 调度结果
     */
    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<RunResponse> runAction(@RequestBody ActionRequest request) {
        return actionSchedulerService.parseAndSchedule(request);
    }

    /**
     * 查询指定Run的状态
     * GET /api/v1/status/{runId}
     *
     * @param runId 运行ID
     * @return Run状态信息
     */
    @GetMapping("/status/{runId}")
    public Mono<RunResponse> getRunStatus(@PathVariable String runId) {
        return runStatusService.getRunStatus(runId);
    }

    /**
     * 查询所有Run的状态
     * GET /api/v1/status
     *
     * @return 所有Run状态信息
     */
    @GetMapping("/status")
    public Mono<Map<String, RunResponse>> getAllRunStatuses() {
        return Mono.just(runStatusService.getAllRunStatuses());
    }
}
