package com.scheduler.service;

import com.scheduler.action.ActionParser;
import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerInfo;
import com.scheduler.runner.RunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Action调度服务
 * 负责解析Action配置并调度Runner执行
 */
@Service
@RequiredArgsConstructor
public class ActionSchedulerService {
    
    private final ActionParser actionParser;
    private final RunnerService runnerService;
    
    /**
     * 解析并调度Action
     */
    public Mono<RunResponse> parseAndSchedule(ActionRequest request) {
        String runId = "run-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime startTime = LocalDateTime.now();
        
        // 解析Runner需求
        List<ActionParser.RunnerRequirement> requirements = actionParser.parseRunnerRequirements(request);
        
        // 创建初始响应
        RunResponse response = new RunResponse();
        response.setRunId(runId);
        response.setStatus(RunResponse.RunStatus.PENDING);
        response.setStartTime(startTime);
        response.setRunners(new ArrayList<>());
        
        if (requirements.isEmpty()) {
            response.setStatus(RunResponse.RunStatus.SUCCESS);
            response.setEndTime(LocalDateTime.now());
            response.setMessage("No jobs to execute");
            return Mono.just(response);
        }
        
        // 申请所有Runner
        return Flux.fromIterable(requirements)
            .flatMap(requirement -> {
                RunnerAllocateRequest allocateRequest = new RunnerAllocateRequest(
                    requirement.getRunsOn(),
                    runId
                );
                return runnerService.allocateRunner(allocateRequest);
            })
            .collectList()
            .flatMap(allocateResponses -> {
                // 更新响应中的Runner信息
                List<RunnerInfo> runnerInfos = allocateResponses.stream()
                    .map(resp -> {
                        RunnerInfo info = new RunnerInfo();
                        info.setRunnerId(resp.getRunnerId());
                        info.setStatus(resp.getStatus());
                        info.setAllocatedAt(resp.getAllocatedAt());
                        return info;
                    })
                    .collect(Collectors.toList());
                
                response.setRunners(runnerInfos);
                response.setStatus(RunResponse.RunStatus.RUNNING);
                
                // 连接并执行所有Runner
                return Flux.fromIterable(allocateResponses)
                    .flatMap(allocateResponse -> 
                        runnerService.connectRunner(allocateResponse.getRunnerId())
                            .flatMap(runner -> runnerService.executeRunner(runner.getRunnerId()))
                    )
                    .collectList()
                    .map(completedRunners -> {
                        // 检查所有Runner是否成功完成
                        boolean allSuccess = completedRunners.stream()
                            .allMatch(r -> r.getStatus() == RunnerInfo.RunnerStatus.COMPLETED);
                        
                        response.setStatus(allSuccess ? 
                            RunResponse.RunStatus.SUCCESS : 
                            RunResponse.RunStatus.FAILURE);
                        response.setEndTime(LocalDateTime.now());
                        response.setMessage(allSuccess ? 
                            "All jobs completed successfully" : 
                            "Some jobs failed");
                        response.setRunners(completedRunners);
                        
                        return response;
                    });
            });
    }
}
