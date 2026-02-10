package com.scheduler.action;

import com.scheduler.model.ActionRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Action解析器
 * 解析GitHub Action配置
 */
@Component
public class ActionParser {
    
    /**
     * 解析Action请求，提取需要申请的Runner信息
     */
    public List<RunnerRequirement> parseRunnerRequirements(ActionRequest request) {
        List<RunnerRequirement> requirements = new ArrayList<>();
        
        if (request.getJobs() != null) {
            for (Map.Entry<String, ActionRequest.Job> jobEntry : request.getJobs().entrySet()) {
                String jobName = jobEntry.getKey();
                ActionRequest.Job job = jobEntry.getValue();
                
                if (job.getRunsOn() != null) {
                    requirements.add(new RunnerRequirement(
                        jobName,
                        job.getRunsOn(),
                        job.getSteps() != null ? job.getSteps().length : 0
                    ));
                }
            }
        }
        
        return requirements;
    }
    
    /**
     * Runner需求模型
     */
    public static class RunnerRequirement {
        private final String jobName;
        private final String runsOn;
        private final int stepCount;
        
        public RunnerRequirement(String jobName, String runsOn, int stepCount) {
            this.jobName = jobName;
            this.runsOn = runsOn;
            this.stepCount = stepCount;
        }
        
        public String getJobName() {
            return jobName;
        }
        
        public String getRunsOn() {
            return runsOn;
        }
        
        public int getStepCount() {
            return stepCount;
        }
    }
}
