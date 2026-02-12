package com.scheduler.service;

import com.scheduler.model.RunResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Run状态管理服务
 * 负责存储和查询GitHub Action运行状态
 */
@Service
public class RunStatusService {

    /**
     * Run状态存储（模拟数据库）
     * Key: runId
     * Value: RunResponse
     */
    private final Map<String, RunResponse> runStatuses = new ConcurrentHashMap<>();

    /**
     * 保存或更新Run状态
     *
     * @param runResponse Run响应对象
     * @return 保存的Run响应
     */
    public Mono<RunResponse> saveRunStatus(RunResponse runResponse) {
        return Mono.fromCallable(() -> {
            runStatuses.put(runResponse.getRunId(), runResponse);
            return runResponse;
        });
    }

    /**
     * 根据runId查询Run状态
     *
     * @param runId 运行ID
     * @return Run响应对象，如果不存在则返回空
     */
    public Mono<RunResponse> getRunStatus(String runId) {
        return Mono.fromCallable(() -> {
            RunResponse response = runStatuses.get(runId);
            if (response == null) {
                throw new RuntimeException("Run not found: " + runId);
            }
            return response;
        });
    }

    /**
     * 获取所有Run状态
     *
     * @return 所有Run状态的Map
     */
    public Map<String, RunResponse> getAllRunStatuses() {
        return new ConcurrentHashMap<>(runStatuses);
    }
}
