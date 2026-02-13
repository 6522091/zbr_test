package com.scheduler.runner;

import com.scheduler.model.RunnerAllocateRequest;
import com.scheduler.model.RunnerAllocateResponse;
import com.scheduler.model.RunnerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RunnerService 单元测试
 */
class RunnerServiceTest {

    private RunnerService runnerService;

    @BeforeEach
    void setUp() {
        runnerService = new RunnerService();
    }

    @Test
    void allocateRunner_returnsAllocatedRunner() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");

        StepVerifier.create(runnerService.allocateRunner(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getRunnerId()).startsWith("runner-");
                    assertThat(response.getStatus()).isEqualTo(RunnerInfo.RunnerStatus.ALLOCATED);
                    assertThat(response.getAllocatedAt()).isNotNull();
                    assertThat(response.getConnectionUrl()).contains(response.getRunnerId());
                })
                .verifyComplete();
    }

    @Test
    void allocateRunner_generatesUniqueRunnerIds() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");

        RunnerAllocateResponse r1 = runnerService.allocateRunner(request).block();
        RunnerAllocateResponse r2 = runnerService.allocateRunner(request).block();

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.getRunnerId()).isNotEqualTo(r2.getRunnerId());
    }

    @Test
    void getRunnerStatus_afterAllocation_returnsRunner() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");
        RunnerAllocateResponse allocated = runnerService.allocateRunner(request).block();
        assertThat(allocated).isNotNull();
        String runnerId = allocated.getRunnerId();

        StepVerifier.create(runnerService.getRunnerStatus(runnerId))
                .assertNext(runner -> {
                    assertThat(runner.getRunnerId()).isEqualTo(runnerId);
                    assertThat(runner.getStatus()).isEqualTo(RunnerInfo.RunnerStatus.ALLOCATED);
                })
                .verifyComplete();
    }

    @Test
    void getRunnerStatus_withUnknownId_throwsError() {
        StepVerifier.create(runnerService.getRunnerStatus("nonexistent-runner"))
                .expectErrorMatches(e -> e instanceof RuntimeException
                        && e.getMessage().contains("Runner not found"))
                .verify();
    }

    @Test
    void connectRunner_updatesStatusToConnected() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");
        RunnerAllocateResponse allocated = runnerService.allocateRunner(request).block();
        assertThat(allocated).isNotNull();
        String runnerId = allocated.getRunnerId();

        StepVerifier.create(runnerService.connectRunner(runnerId))
                .assertNext(runner -> {
                    assertThat(runner.getStatus()).isEqualTo(RunnerInfo.RunnerStatus.CONNECTED);
                    assertThat(runner.getConnectedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void connectRunner_withUnknownId_throwsError() {
        StepVerifier.create(runnerService.connectRunner("nonexistent-runner"))
                .expectErrorMatches(e -> e instanceof RuntimeException
                        && e.getMessage().contains("Runner not found"))
                .verify();
    }

    @Test
    void executeRunner_updatesStatusToCompleted() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");
        RunnerAllocateResponse allocated = runnerService.allocateRunner(request).block();
        assertThat(allocated).isNotNull();
        String runnerId = allocated.getRunnerId();

        StepVerifier.create(runnerService.executeRunner(runnerId))
                .assertNext(runner -> {
                    assertThat(runner.getStatus()).isEqualTo(RunnerInfo.RunnerStatus.COMPLETED);
                    assertThat(runner.getCompletedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void executeRunner_withUnknownId_throwsError() {
        StepVerifier.create(runnerService.executeRunner("nonexistent-runner"))
                .expectErrorMatches(e -> e instanceof RuntimeException
                        && e.getMessage().contains("Runner not found"))
                .verify();
    }

    @Test
    void releaseRunner_updatesStatusToReleased() {
        RunnerAllocateRequest request = new RunnerAllocateRequest("ubuntu-latest", "run-001");
        RunnerAllocateResponse allocated = runnerService.allocateRunner(request).block();
        assertThat(allocated).isNotNull();
        String runnerId = allocated.getRunnerId();

        StepVerifier.create(runnerService.releaseRunner(runnerId))
                .verifyComplete();

        // 验证状态已更新为 RELEASED
        RunnerInfo runner = runnerService.getRunnerStatus(runnerId).block();
        assertThat(runner).isNotNull();
        assertThat(runner.getStatus()).isEqualTo(RunnerInfo.RunnerStatus.RELEASED);
    }

    @Test
    void releaseRunner_withUnknownId_completesWithoutError() {
        // 释放不存在的Runner不应抛出异常
        StepVerifier.create(runnerService.releaseRunner("nonexistent-runner"))
                .verifyComplete();
    }
}
