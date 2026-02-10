package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Runner申请响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunnerAllocateResponse {
    
    /**
     * Runner ID
     */
    private String runnerId;
    
    /**
     * Runner状态
     */
    private RunnerInfo.RunnerStatus status;
    
    /**
     * 分配时间
     */
    private LocalDateTime allocatedAt;
    
    /**
     * Runner连接URL（模拟）
     */
    private String connectionUrl;
}
