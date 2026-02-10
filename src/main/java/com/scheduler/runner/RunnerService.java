package com.scheduler.runner;

import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerAllocateResponse;
import com.scheduler.model.RunnerInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Runner管理服务
 * 模拟下游资源服务的Runner管理功能
 */
@Service
public class RunnerService {
    
    /**
     * Runner存储（模拟数据库）
     */
    private final Map<String, RunnerInfo> runners = new ConcurrentHashMap<>();
    
    /**
     * 申请Runner
     * 模拟向资源服务申请Runner的过程
     * 使用虚拟线程和响应式编程
     */
    public Mono<RunnerAllocateResponse> allocateRunner(RunnerAllocateRequest request) {
        return Mono.delay(Duration.ofMillis(100))
            .publishOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
            .map(delay -> {
                // 生成Runner ID
                String runnerId = "runner-" + UUID.randomUUID().toString().substring(0, 8);
                
                // 创建Runner信息
                RunnerInfo runnerInfo = new RunnerInfo(
                    runnerId,
                    RunnerInfo.RunnerStatus.ALLOCATED,
                    LocalDateTime.now(),
                    null,
                    null
                );
                
                // 存储Runner
                runners.put(runnerId, runnerInfo);
                
                // 返回响应
                return new RunnerAllocateResponse(
                    runnerId,
                    RunnerInfo.RunnerStatus.ALLOCATED,
                    LocalDateTime.now(),
                    "http://runner-service/runners/" + runnerId + "/connect"
                );
            });
    }
    
    /**
     * 查询Runner状态
     * 使用虚拟线程执行
     */
    public Mono<RunnerInfo> getRunnerStatus(String runnerId) {
        return Mono.fromCallable(() -> {
            RunnerInfo runner = runners.get(runnerId);
            if (runner == null) {
                throw new RuntimeException("Runner not found: " + runnerId);
            }
            return runner;
        }).subscribeOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()));
    }
    
    /**
     * 模拟Runner连接
     * 使用虚拟线程和响应式编程
     */
    public Mono<RunnerInfo> connectRunner(String runnerId) {
        return Mono.delay(Duration.ofMillis(200))
            .publishOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
            .map(delay -> {
                RunnerInfo runner = runners.get(runnerId);
                if (runner == null) {
                    throw new RuntimeException("Runner not found: " + runnerId);
                }
                
                // 更新Runner状态
                runner.setStatus(RunnerInfo.RunnerStatus.CONNECTED);
                runner.setConnectedAt(LocalDateTime.now());
                
                return runner;
            });
    }
    
    /**
     * 模拟Runner执行
     * 使用虚拟线程和响应式编程
     */
    public Mono<RunnerInfo> executeRunner(String runnerId) {
        return Mono.delay(Duration.ofMillis(500))
            .publishOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
            .map(delay -> {
                RunnerInfo runner = runners.get(runnerId);
                if (runner == null) {
                    throw new RuntimeException("Runner not found: " + runnerId);
                }
                
                // 更新Runner状态
                runner.setStatus(RunnerInfo.RunnerStatus.RUNNING);
                // 模拟执行完成后更新状态
                runner.setStatus(RunnerInfo.RunnerStatus.COMPLETED);
                runner.setCompletedAt(LocalDateTime.now());
                
                return runner;
            });
    }
    
    /**
     * 释放Runner
     */
    public Mono<Void> releaseRunner(String runnerId) {
        return Mono.fromRunnable(() -> {
            RunnerInfo runner = runners.get(runnerId);
            if (runner != null) {
                runner.setStatus(RunnerInfo.RunnerStatus.RELEASED);
                // 可以选择删除或保留历史记录
                // runners.remove(runnerId);
            }
        });
    }
}
