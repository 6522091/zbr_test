package com.scheduler.service;

import com.scheduler.action.ActionParser;
import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerAllocateResponse;
import com.scheduler.model.RunnerInfo;
import com.scheduler.runner.RunnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionSchedulerServiceTest {

    @Mock
    private ActionParser actionParser;

    @Mock
    private RunnerService runnerService;

    @InjectMocks
    private ActionSchedulerService actionSchedulerService;

    private ActionRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new ActionRequest();
        testRequest.setName("Test Action");

        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Test Step");
        step.setRun("echo hello");
        job.setSteps(new ActionRequest.Step[]{step});
        jobs.put("test-job", job);
        testRequest.setJobs(jobs);
    }

    @Test
    void parseAndSchedule_serviceIsInstantiated() {
        assertThat(actionSchedulerService).isNotNull();
    }

    @Test
    void parseAndSchedule_withNoJobs_returnsSuccess() {
        when(actionParser.parseRunnerRequirements(any(ActionRequest.class)))
                .thenReturn(Collections.emptyList());

        StepVerifier.create(actionSchedulerService.parseAndSchedule(testRequest))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(RunResponse.RunStatus.SUCCESS);
                    assertThat(response.getMessage()).isEqualTo("No jobs to execute");
                    assertThat(response.getRunId()).startsWith("run-");
                    assertThat(response.getStartTime()).isNotNull();
                    assertThat(response.getEndTime()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void parseAndSchedule_withSingleJob_returnsSuccess() {
        List<ActionParser.RunnerRequirement> requirements = List.of(
                new ActionParser.RunnerRequirement("test-job", "ubuntu-latest", 1)
        );
        when(actionParser.parseRunnerRequirements(any(ActionRequest.class)))
                .thenReturn(requirements);

        String runnerId = "runner-abc12345";
        RunnerAllocateResponse allocateResponse = new RunnerAllocateResponse(
                runnerId,
                RunnerInfo.RunnerStatus.ALLOCATED,
                LocalDateTime.now(),
                "http://runner-service/runners/" + runnerId + "/connect"
        );
        when(runnerService.allocateRunner(any(RunnerAllocateRequest.class)))
                .thenReturn(Mono.just(allocateResponse));

        RunnerInfo connectedRunner = new RunnerInfo(
                runnerId, RunnerInfo.RunnerStatus.CONNECTED, LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(runnerService.connectRunner(runnerId))
                .thenReturn(Mono.just(connectedRunner));

        RunnerInfo completedRunner = new RunnerInfo(
                runnerId, RunnerInfo.RunnerStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(runnerService.executeRunner(runnerId))
                .thenReturn(Mono.just(completedRunner));

        StepVerifier.create(actionSchedulerService.parseAndSchedule(testRequest))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(RunResponse.RunStatus.SUCCESS);
                    assertThat(response.getMessage()).isEqualTo("All jobs completed successfully");
                    assertThat(response.getRunId()).startsWith("run-");
                    assertThat(response.getRunners()).hasSize(1);
                    assertThat(response.getRunners().get(0).getStatus()).isEqualTo(RunnerInfo.RunnerStatus.COMPLETED);
                })
                .verifyComplete();
    }

    @Test
    void parseAndSchedule_runIdIsUniquePerInvocation() {
        when(actionParser.parseRunnerRequirements(any(ActionRequest.class)))
                .thenReturn(Collections.emptyList());

        RunResponse r1 = actionSchedulerService.parseAndSchedule(testRequest).block();
        RunResponse r2 = actionSchedulerService.parseAndSchedule(testRequest).block();

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.getRunId()).isNotEqualTo(r2.getRunId());
    }
}
