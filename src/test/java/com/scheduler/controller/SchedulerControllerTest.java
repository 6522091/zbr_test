package com.scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import com.scheduler.service.ActionSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(SchedulerController.class)
class SchedulerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ActionSchedulerService actionSchedulerService;

    @Test
    void testRunAction() {
        // 准备测试数据
        ActionRequest request = createTestActionRequest();
        RunResponse response = new RunResponse();
        response.setRunId("run-12345678");
        response.setStatus(RunResponse.RunStatus.RUNNING);
        response.setStartTime(LocalDateTime.now());

        // Mock服务响应
        when(actionSchedulerService.parseAndSchedule(any(ActionRequest.class)))
            .thenReturn(Mono.just(response));

        // 执行测试
        webTestClient.post()
            .uri("/api/v1/run")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(RunResponse.class)
            .value(r -> {
                assert r.getRunId() != null;
                assert r.getStatus() == RunResponse.RunStatus.RUNNING;
            });
    }

    private ActionRequest createTestActionRequest() {
        ActionRequest request = new ActionRequest();
        request.setName("Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Test Step");
        step.setRun("echo 'Hello World'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put("test-job", job);
        request.setJobs(jobs);
        
        return request;
    }
}
