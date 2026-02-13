package com.scheduler.action;

import com.scheduler.model.ActionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ActionParser 单元测试
 */
class ActionParserTest {

    private ActionParser actionParser;

    @BeforeEach
    void setUp() {
        actionParser = new ActionParser();
    }

    @Test
    void parseRunnerRequirements_withSingleJob_returnsOneRequirement() {
        ActionRequest request = new ActionRequest();
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        ActionRequest.Step step1 = new ActionRequest.Step();
        step1.setName("step1");
        job.setSteps(new ActionRequest.Step[]{step1});
        jobs.put("build", job);
        request.setJobs(jobs);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobName()).isEqualTo("build");
        assertThat(result.get(0).getRunsOn()).isEqualTo("ubuntu-latest");
        assertThat(result.get(0).getStepCount()).isEqualTo(1);
    }

    @Test
    void parseRunnerRequirements_withMultipleJobs_returnsAllRequirements() {
        ActionRequest request = new ActionRequest();
        Map<String, ActionRequest.Job> jobs = new HashMap<>();

        ActionRequest.Job buildJob = new ActionRequest.Job();
        buildJob.setRunsOn("ubuntu-latest");
        ActionRequest.Step s1 = new ActionRequest.Step();
        s1.setName("checkout");
        ActionRequest.Step s2 = new ActionRequest.Step();
        s2.setName("build");
        buildJob.setSteps(new ActionRequest.Step[]{s1, s2});
        jobs.put("build", buildJob);

        ActionRequest.Job testJob = new ActionRequest.Job();
        testJob.setRunsOn("windows-latest");
        ActionRequest.Step s3 = new ActionRequest.Step();
        s3.setName("test");
        testJob.setSteps(new ActionRequest.Step[]{s3});
        jobs.put("test", testJob);

        request.setJobs(jobs);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ActionParser.RunnerRequirement::getRunsOn)
                .containsExactlyInAnyOrder("ubuntu-latest", "windows-latest");
    }

    @Test
    void parseRunnerRequirements_withNullJobs_returnsEmptyList() {
        ActionRequest request = new ActionRequest();
        request.setJobs(null);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).isEmpty();
    }

    @Test
    void parseRunnerRequirements_withEmptyJobs_returnsEmptyList() {
        ActionRequest request = new ActionRequest();
        request.setJobs(new HashMap<>());

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).isEmpty();
    }

    @Test
    void parseRunnerRequirements_withJobMissingRunsOn_skipsJob() {
        ActionRequest request = new ActionRequest();
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn(null);
        jobs.put("no-runner-job", job);
        request.setJobs(jobs);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).isEmpty();
    }

    @Test
    void parseRunnerRequirements_withJobHavingNullSteps_returnsStepCountZero() {
        ActionRequest request = new ActionRequest();
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        job.setSteps(null);
        jobs.put("empty-steps-job", job);
        request.setJobs(jobs);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepCount()).isEqualTo(0);
    }

    @Test
    void parseRunnerRequirements_withJobHavingEmptySteps_returnsStepCountZero() {
        ActionRequest request = new ActionRequest();
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        job.setSteps(new ActionRequest.Step[]{});
        jobs.put("empty-steps-job", job);
        request.setJobs(jobs);

        List<ActionParser.RunnerRequirement> result = actionParser.parseRunnerRequirements(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepCount()).isEqualTo(0);
    }

    @Test
    void runnerRequirement_gettersReturnCorrectValues() {
        ActionParser.RunnerRequirement req = new ActionParser.RunnerRequirement("my-job", "ubuntu-22.04", 5);

        assertThat(req.getJobName()).isEqualTo("my-job");
        assertThat(req.getRunsOn()).isEqualTo("ubuntu-22.04");
        assertThat(req.getStepCount()).isEqualTo(5);
    }
}
