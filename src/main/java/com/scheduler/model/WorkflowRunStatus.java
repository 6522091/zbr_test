package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GitHub Actions 工作流运行状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunStatus {

    /**
     * 工作流运行 ID
     */
    private Long id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 运行编号
     */
    private Integer runNumber;

    /**
     * 状态: queued, in_progress, completed
     */
    private String status;

    /**
     * 结论: success, failure, cancelled, skipped, timed_out, action_required, neutral
     */
    private String conclusion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 触发事件
     */
    private String event;

    /**
     * 分支
     */
    private String headBranch;

    /**
     * 提交 SHA
     */
    private String headSha;

    /**
     * HTML URL
     */
    private String htmlUrl;
}
