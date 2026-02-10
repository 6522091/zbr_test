package com.scheduler.performance;

import com.scheduler.model.ActionRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 压力测试
 * 测试系统在极限负载下的表现和恢复能力
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("压力测试")
class StressTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 突发流量测试
     * 模拟短时间内大量请求涌入
     */
    @Test
    @DisplayName("突发流量测试 - 500请求/秒")
    @Timeout(120)
    void testBurstTraffic() {
        int burstSize = 500;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(burstSize);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 在1秒内发送所有请求
        for (int i = 0; i < burstSize; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ActionRequest request = createActionRequest("burst-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 记录失败但不中断测试
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println(String.format(
                "突发流量测试: %d请求在1秒内发送, 成功: %d, 耗时: %d ms",
                burstSize, successCount.get(), duration
            ));

            assertThat(completed).isTrue();
            // 至少70%的请求应该成功处理
            assertThat(successCount.get()).isGreaterThan((int) (burstSize * 0.7));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
    }

    /**
     * 逐步增加负载测试
     * 模拟负载逐步增加的情况
     */
    @Test
    @DisplayName("逐步增加负载测试")
    @Timeout(300)
    void testGradualLoadIncrease() {
        int[] loadLevels = {10, 25, 50, 100, 200, 500};
        ExecutorService executor = Executors.newFixedThreadPool(500);
        
        for (int load : loadLevels) {
            System.out.println(String.format("\n测试负载级别: %d 并发", load));
            
            CountDownLatch latch = new CountDownLatch(load);
            AtomicInteger successCount = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < load; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        ActionRequest request = createActionRequest("gradual-" + load + "-" + requestId);
                        
                        webTestClient.post()
                            .uri("/api/v1/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isAccepted();

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 记录失败
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(60, TimeUnit.SECONDS);
                long duration = System.currentTimeMillis() - startTime;
                double successRate = (successCount.get() * 100.0) / load;

                System.out.println(String.format(
                    "  负载 %d: 成功 %d (%.2f%%), 耗时 %d ms",
                    load, successCount.get(), successRate, duration
                ));

                // 每个负载级别至少80%成功率
                assertThat(successRate).isGreaterThan(80.0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // 负载级别之间稍作休息
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdown();
    }

    /**
     * 系统恢复测试
     * 测试系统在过载后的恢复能力
     */
    @Test
    @DisplayName("系统恢复测试")
    @Timeout(180)
    void testSystemRecovery() {
        // 第一阶段：过载
        System.out.println("阶段1: 过载测试");
        int overloadRequests = 2000;
        ExecutorService overloadExecutor = Executors.newFixedThreadPool(200);
        CountDownLatch overloadLatch = new CountDownLatch(overloadRequests);
        AtomicInteger overloadSuccess = new AtomicInteger(0);

        for (int i = 0; i < overloadRequests; i++) {
            final int requestId = i;
            overloadExecutor.submit(() -> {
                try {
                    ActionRequest request = createActionRequest("overload-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    overloadSuccess.incrementAndGet();
                } catch (Exception e) {
                    // 忽略过载期间的失败
                } finally {
                    overloadLatch.countDown();
                }
            });
        }

        try {
            overloadLatch.await(120, TimeUnit.SECONDS);
            System.out.println(String.format("过载阶段完成: %d/%d 成功", 
                overloadSuccess.get(), overloadRequests));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        overloadExecutor.shutdown();

        // 第二阶段：恢复期等待
        System.out.println("\n阶段2: 恢复期等待 (10秒)");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 第三阶段：正常负载测试恢复能力
        System.out.println("\n阶段3: 恢复后正常负载测试");
        int recoveryRequests = 100;
        ExecutorService recoveryExecutor = Executors.newFixedThreadPool(20);
        CountDownLatch recoveryLatch = new CountDownLatch(recoveryRequests);
        AtomicInteger recoverySuccess = new AtomicInteger(0);

        for (int i = 0; i < recoveryRequests; i++) {
            final int requestId = i;
            recoveryExecutor.submit(() -> {
                try {
                    ActionRequest request = createActionRequest("recovery-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    recoverySuccess.incrementAndGet();
                } catch (Exception e) {
                    // 记录失败
                } finally {
                    recoveryLatch.countDown();
                }
            });
        }

        try {
            recoveryLatch.await(60, TimeUnit.SECONDS);
            double recoveryRate = (recoverySuccess.get() * 100.0) / recoveryRequests;
            
            System.out.println(String.format(
                "恢复测试完成: %d/%d 成功 (%.2f%%)",
                recoverySuccess.get(), recoveryRequests, recoveryRate
            ));

            // 恢复后应该能达到95%以上的成功率
            assertThat(recoveryRate).isGreaterThan(95.0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        recoveryExecutor.shutdown();
    }

    private ActionRequest createActionRequest(String jobName) {
        ActionRequest request = new ActionRequest();
        request.setName("Stress Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Stress Test Step");
        step.setRun("echo 'Stress test: " + jobName + "'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put(jobName, job);
        request.setJobs(jobs);
        
        return request;
    }
}
