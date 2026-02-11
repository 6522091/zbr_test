package com.scheduler.controller;

import com.scheduler.model.WorkflowRunStatus;
import com.scheduler.model.WorkflowRunsResponse;
import com.scheduler.service.GitHubActionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * GitHubActionsController 单元测试
 */
@WebFluxTest(GitHubActionsController.class)
class GitHubActionsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GitHubActionsService gitHubActionsService;

    private WorkflowRunStatus mockWorkflowRunStatus;
    private WorkflowRunsResponse mockWorkflowRunsResponse;

    @BeforeEach
    void setUp() {
        mockWorkflowRunStatus = new WorkflowRunStatus();
        mockWorkflowRunStatus.setId(123456789L);
        mockWorkflowRunStatus.setName("CI");
        mockWorkflowRunStatus.setRunNumber(42);
        mockWorkflowRunStatus.setStatus("completed");
        mockWorkflowRunStatus.setConclusion("success");
        mockWorkflowRunStatus.setEvent("push");
        mockWorkflowRunStatus.setHeadBranch("main");
        mockWorkflowRunStatus.setHeadSha("abc123");
        mockWorkflowRunStatus.setHtmlUrl("https://github.com/owner/repo/actions/runs/123456789");
        mockWorkflowRunStatus.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        mockWorkflowRunStatus.setUpdatedAt(LocalDateTime.now());

        mockWorkflowRunsResponse = new WorkflowRunsResponse();
        mockWorkflowRunsResponse.setTotalCount(1);
        mockWorkflowRunsResponse.setWorkflowRuns(List.of(mockWorkflowRunStatus));
    }

    @Test
    void testGetWorkflowRuns() {
        when(gitHubActionsService.getWorkflowRuns(eq("owner"), eq("repo"), anyInt()))
                .thenReturn(Mono.just(mockWorkflowRunsResponse));

        webTestClient.get()
                .uri("/api/v1/github-actions/owner/repo/runs?perPage=30")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WorkflowRunsResponse.class)
                .value(response -> {
                    assert response.getTotalCount() == 1;
                    assert response.getWorkflowRuns().size() == 1;
                    assert response.getWorkflowRuns().get(0).getId().equals(123456789L);
                });
    }

    @Test
    void testGetWorkflowRunStatus() {
        when(gitHubActionsService.getWorkflowRunStatus(eq("owner"), eq("repo"), eq(123456789L)))
                .thenReturn(Mono.just(mockWorkflowRunStatus));

        webTestClient.get()
                .uri("/api/v1/github-actions/owner/repo/runs/123456789")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WorkflowRunStatus.class)
                .value(status -> {
                    assert status.getId().equals(123456789L);
                    assert status.getStatus().equals("completed");
                    assert status.getConclusion().equals("success");
                });
    }

    @Test
    void testWatchWorkflowRunStatus() {
        WorkflowRunStatus inProgressStatus = new WorkflowRunStatus();
        inProgressStatus.setId(123456789L);
        inProgressStatus.setStatus("in_progress");
        inProgressStatus.setConclusion(null);

        WorkflowRunStatus completedStatus = new WorkflowRunStatus();
        completedStatus.setId(123456789L);
        completedStatus.setStatus("completed");
        completedStatus.setConclusion("success");

        when(gitHubActionsService.watchWorkflowRunStatus(eq("owner"), eq("repo"), eq(123456789L), anyLong()))
                .thenReturn(Flux.just(inProgressStatus, completedStatus));

        webTestClient.get()
                .uri("/api/v1/github-actions/owner/repo/runs/123456789/watch?interval=1")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(WorkflowRunStatus.class)
                .hasSize(2)
                .value(statuses -> {
                    assert statuses.get(0).getStatus().equals("in_progress");
                    assert statuses.get(1).getStatus().equals("completed");
                });
    }
}
