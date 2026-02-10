package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runner申请请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunnerAllocateRequest {
    
    /**
     * 运行环境要求（如：ubuntu-latest, windows-latest等）
     */
    private String runsOn;
    
    /**
     * 关联的Run ID
     */
    private String runId;
}
