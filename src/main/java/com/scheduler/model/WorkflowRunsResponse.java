package com.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub Actions 工作流运行列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunsResponse {

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 工作流运行列表
     */
    private List<WorkflowRunStatus> workflowRuns;
}
