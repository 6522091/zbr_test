package com.scheduler.controller;

import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerAllocateResponse;
import com.scheduler.model.RunnerInfo;
import com.scheduler.runner.RunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Runner管理控制器
 * 提供Runner的申请、查询、释放接口
 */
@RestController
@RequestMapping("/api/v1/runners")
@RequiredArgsConstructor
public class RunnerController {
    
    private final RunnerService runnerService;
    
    /**
     * 申请Runner
     * POST /api/v1/runners/allocate
     */
    @PostMapping("/allocate")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RunnerAllocateResponse> allocateRunner(@RequestBody RunnerAllocateRequest request) {
        return runnerService.allocateRunner(request);
    }
    
    /**
     * 查询Runner状态
     * GET /api/v1/runners/{id}
     */
    @GetMapping("/test/{id}")
    public Mono<RunnerInfo> getRunnerStatus(@PathVariable String id) {
        return runnerService.getRunnerStatus(id);
    }
    
    /**
     * 释放Runner
     * DELETE /api/v1/runners/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> releaseRunner(@PathVariable String id) {
        return runnerService.releaseRunner(id);
    }
}
