package com.scheduler.controller;

import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerAllocateResponse;
import com.scheduler.model.RunnerInfo;
import com.scheduler.runner.RunnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RunnerController 单元测试
 */
@WebFluxTest(RunnerController.class)
class RunnerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RunnerService runnerService;

    @Test
    void allocateRunner_returnsCreatedStatus() {
        RunnerAllocateResponse response = new RunnerAllocateResponse(
                "runner-abc12345",
                RunnerInfo.RunnerStatus.ALLOCATED,
                LocalDateTime.now(),
                "http://runner-service/runners/runner-abc12345/connect"
        );
        when(runnerService.allocateRunner(any(RunnerAllocateRequest.class)))
                .thenReturn(Mono.just(response));

        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");

        webTestClient.post()
                .uri("/api/v1/runners/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RunnerAllocateResponse.class)
                .value(r -> {
                    assert r.getRunnerId().equals("runner-abc12345");
                    assert r.getStatus() == RunnerInfo.RunnerStatus.ALLOCATED;
                    assert r.getConnectionUrl() != null;
                });
    }

    @Test
    void getRunnerStatus_withExistingRunner_returnsOk() {
        RunnerInfo runnerInfo = new RunnerInfo(
                "runner-abc12345",
                RunnerInfo.RunnerStatus.CONNECTED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        when(runnerService.getRunnerStatus(eq("runner-abc12345")))
                .thenReturn(Mono.just(runnerInfo));

        webTestClient.get()
                .uri("/api/v1/runners/runner-abc12345")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RunnerInfo.class)
                .value(r -> {
                    assert r.getRunnerId().equals("runner-abc12345");
                    assert r.getStatus() == RunnerInfo.RunnerStatus.CONNECTED;
                });
    }

    @Test
    void getRunnerStatus_withUnknownRunner_returns5xx() {
        when(runnerService.getRunnerStatus(eq("nonexistent")))
                .thenReturn(Mono.error(new RuntimeException("Runner not found: nonexistent")));

        webTestClient.get()
                .uri("/api/v1/runners/nonexistent")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void releaseRunner_returnsNoContent() {
        when(runnerService.releaseRunner(eq("runner-abc12345")))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/runners/runner-abc12345")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void allocateRunner_withWindowsRunner_returnsAllocatedRunner() {
        RunnerAllocateResponse response = new RunnerAllocateResponse(
                "runner-win12345",
                RunnerInfo.RunnerStatus.ALLOCATED,
                LocalDateTime.now(),
                "http://runner-service/runners/runner-win12345/connect"
        );
        when(runnerService.allocateRunner(any(RunnerAllocateRequest.class)))
                .thenReturn(Mono.just(response));

        RunnerAllocateRequest request = new RunnerAllocateRequest("windows-latest", "run-002");

        webTestClient.post()
                .uri("/api/v1/runners/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RunnerAllocateResponse.class)
                .value(r -> {
                    assert r.getRunnerId().equals("runner-win12345");
                    assert r.getStatus() == RunnerInfo.RunnerStatus.ALLOCATED;
                });
    }
}
