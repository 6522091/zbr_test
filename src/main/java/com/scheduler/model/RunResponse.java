package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Action执行响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunResponse {
    
    /**
     * 运行ID
     */
    private String runId;
    
    /**
     * 状态
     */
    private RunStatus status;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 分配的Runner信息
     */
    private List<RunnerInfo> runners;
    
    /**
     * 执行结果
     */
    private String message;
    
    /**
     * 运行状态枚举
     */
    public enum RunStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILURE,
        CANCELLED
    }
}
