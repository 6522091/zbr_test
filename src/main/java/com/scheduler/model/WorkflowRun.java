package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GitHub Actions工作流运行信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun {

    /**
     * 运行ID
     */
    private Long id;

    /**
     * 运行编号
     */
    private Integer runNumber;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * 结论
     */
    private String conclusion;

    /**
     * 分支
     */
    private String headBranch;

    /**
     * 提交SHA
     */
    private String headSha;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 运行开始时间
     */
    private LocalDateTime runStartedAt;

    /**
     * HTML URL
     */
    private String htmlUrl;

    /**
     * 触发事件
     */
    private String event;

    /**
     * 触发者
     */
    private String actor;
}
