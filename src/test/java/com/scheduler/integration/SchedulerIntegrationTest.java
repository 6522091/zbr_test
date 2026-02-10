package com.scheduler.integration;

import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 集成测试
 * 测试完整的Action调度流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SchedulerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCompleteActionFlow() {
        // 创建测试Action请求
        ActionRequest request = new ActionRequest();
        request.setName("Integration Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step1 = new ActionRequest.Step();
        step1.setName("Step 1");
        step1.setRun("echo 'Step 1 executed'");
        
        ActionRequest.Step step2 = new ActionRequest.Step();
        step2.setName("Step 2");
        step2.setRun("echo 'Step 2 executed'");
        
        job.setSteps(new ActionRequest.Step[]{step1, step2});
        jobs.put("test-job", job);
        request.setJobs(jobs);

        // 执行Action
        webTestClient.post()
            .uri("/api/v1/run")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(RunResponse.class)
            .value(response -> {
                assert response.getRunId() != null;
                assert response.getStatus() != null;
            });
    }

    @Test
    void testConcurrentActionExecution() {
        // 测试并发执行多个Action
        ActionRequest request = createSimpleActionRequest();
        
        // 并发发送多个请求
        for (int i = 0; i < 5; i++) {
            final int index = i;
            webTestClient.post()
                .uri("/api/v1/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted();
        }
    }

    private ActionRequest createSimpleActionRequest() {
        ActionRequest request = new ActionRequest();
        request.setName("Simple Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Simple Step");
        step.setRun("echo 'Hello'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put("simple-job", job);
        request.setJobs(jobs);
        
        return request;
    }
}
