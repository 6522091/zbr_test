package com.scheduler.controller;

import com.scheduler.model.WorkflowRun;
import com.scheduler.model.WorkflowRunsResponse;
import com.scheduler.service.GitHubActionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class GitHubActionsControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private GitHubActionsService gitHubActionsService;

    private GitHubActionsController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new GitHubActionsController(gitHubActionsService);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void testGetWorkflowRuns() {
        WorkflowRunsResponse response = new WorkflowRunsResponse();
        response.setTotalCount(1);

        WorkflowRun run = new WorkflowRun();
        run.setId(12345L);
        run.setRunNumber(1);
        run.setName("Test Workflow");
        run.setStatus("completed");
        run.setConclusion("success");
        run.setHeadBranch("main");
        run.setHeadSha("abc123");
        run.setCreatedAt(LocalDateTime.now());
        run.setHtmlUrl("https://github.com/test/repo/actions/runs/12345");
        run.setEvent("push");
        run.setActor("testuser");

        response.setWorkflowRuns(Arrays.asList(run));

        when(gitHubActionsService.getWorkflowRuns(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/github-actions/testowner/testrepo/runs?perPage=30&page=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(1)
                .jsonPath("$.workflowRuns[0].id").isEqualTo(12345)
                .jsonPath("$.workflowRuns[0].name").isEqualTo("Test Workflow")
                .jsonPath("$.workflowRuns[0].status").isEqualTo("completed");
    }

    @Test
    void testGetWorkflowRun() {
        WorkflowRun run = new WorkflowRun();
        run.setId(12345L);
        run.setRunNumber(1);
        run.setName("Test Workflow");
        run.setStatus("completed");
        run.setConclusion("success");
        run.setHeadBranch("main");
        run.setHeadSha("abc123");
        run.setCreatedAt(LocalDateTime.now());
        run.setHtmlUrl("https://github.com/test/repo/actions/runs/12345");
        run.setEvent("push");
        run.setActor("testuser");

        when(gitHubActionsService.getWorkflowRun(anyString(), anyString(), anyLong()))
                .thenReturn(Mono.just(run));

        webTestClient.get()
                .uri("/api/v1/github-actions/testowner/testrepo/runs/12345")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(12345)
                .jsonPath("$.name").isEqualTo("Test Workflow")
                .jsonPath("$.status").isEqualTo("completed");
    }

    @Test
    void testGetWorkflowRunsByBranch() {
        WorkflowRunsResponse response = new WorkflowRunsResponse();
        response.setTotalCount(1);

        WorkflowRun run = new WorkflowRun();
        run.setId(12345L);
        run.setHeadBranch("feature-branch");

        response.setWorkflowRuns(Arrays.asList(run));

        when(gitHubActionsService.getWorkflowRunsByBranch(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/github-actions/testowner/testrepo/runs/branch/feature-branch")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(1)
                .jsonPath("$.workflowRuns[0].headBranch").isEqualTo("feature-branch");
    }

    @Test
    void testGetWorkflowRunsByStatus() {
        WorkflowRunsResponse response = new WorkflowRunsResponse();
        response.setTotalCount(1);

        WorkflowRun run = new WorkflowRun();
        run.setId(12345L);
        run.setStatus("in_progress");

        response.setWorkflowRuns(Arrays.asList(run));

        when(gitHubActionsService.getWorkflowRunsByStatus(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/github-actions/testowner/testrepo/runs/status/in_progress")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(1)
                .jsonPath("$.workflowRuns[0].status").isEqualTo("in_progress");
    }
}
