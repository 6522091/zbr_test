package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Runner信息模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunnerInfo {
    
    /**
     * Runner ID
     */
    private String runnerId;
    
    /**
     * Runner状态
     */
    private RunnerStatus status;
    
    /**
     * 分配时间
     */
    private LocalDateTime allocatedAt;
    
    /**
     * 连接时间
     */
    private LocalDateTime connectedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    /**
     * Runner状态枚举
     */
    public enum RunnerStatus {
        PENDING,
        ALLOCATED,
        CONNECTED,
        RUNNING,
        COMPLETED,
        FAILED,
        RELEASED
    }
}
