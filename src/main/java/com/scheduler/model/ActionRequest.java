package com.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * GitHub Action请求模型
 * 接收GitHub Action的JSON配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {
    
    /**
     * Action名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * Action的jobs配置
     */
    @JsonProperty("jobs")
    private Map<String, Job> jobs;
    
    /**
     * 上下文信息
     */
    @JsonProperty("context")
    private Map<String, Object> context;
    
    /**
     * Job模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job {
        /**
         * Job运行环境
         */
        @JsonProperty("runs-on")
        private String runsOn;
        
        /**
         * Job的步骤
         */
        @JsonProperty("steps")
        private Step[] steps;
    }
    
    /**
     * Step模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        /**
         * 步骤名称
         */
        @JsonProperty("name")
        private String name;
        
        /**
         * 使用的Action
         */
        @JsonProperty("uses")
        private String uses;
        
        /**
         * 步骤参数
         */
        @JsonProperty("with")
        private Map<String, String> with;
        
        /**
         * 运行的命令
         */
        @JsonProperty("run")
        private String run;
    }
}
