package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GitHub Action 工作流运行状态模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunStatus {

    /**
     * 运行ID
     */
    private String runId;

    /**
     * Action名称
     */
    private String actionName;

    /**
     * 运行状态
     */
    private RunStatus status;

    /**
     * 调度开始时间
     */
    private LocalDateTime scheduledAt;

    /**
     * 运行开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 运行结束时间
     */
    private LocalDateTime completedAt;

    /**
     * 各Job的Runner状态列表
     */
    private List<RunnerInfo> runners;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 运行状态枚举
     */
    public enum RunStatus {
        /** 已调度，等待执行 */
        SCHEDULED,
        /** 运行中 */
        RUNNING,
        /** 成功完成 */
        SUCCESS,
        /** 执行失败 */
        FAILURE,
        /** 已取消 */
        CANCELLED
    }
}
