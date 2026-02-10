package com.scheduler.controller;

import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import com.scheduler.service.ActionSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 主调度控制器
 * 提供/run接口用于接收和调度GitHub Action
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchedulerController {
    
    private final ActionSchedulerService actionSchedulerService;
    
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
}
