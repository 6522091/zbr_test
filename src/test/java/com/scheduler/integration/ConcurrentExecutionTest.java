package com.scheduler.integration;

import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发执行测试
 * 测试虚拟线程在高并发场景下的表现
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ConcurrentExecutionTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testConcurrentActionExecution() {
        int concurrentRequests = 50;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 创建并发请求流
        Flux.range(1, concurrentRequests)
            .flatMap(i -> {
                ActionRequest request = createSimpleActionRequest("job-" + i);
                return Mono.fromCallable(() -> {
                    try {
                        webTestClient.post()
                            .uri("/api/v1/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isAccepted()
                            .expectBody(RunResponse.class)
                            .value(response -> {
                                if (response != null && response.getRunId() != null) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            });
                        return true;
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        return false;
                    }
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
            })
            .blockLast(Duration.ofMinutes(2));

        // 验证结果
        assertThat(successCount.get()).isGreaterThan(0);
        System.out.println("并发测试完成 - 成功: " + successCount.get() + ", 失败: " + failureCount.get());
    }

    @Test
    void testVirtualThreadPerformance() {
        long startTime = System.currentTimeMillis();
        int requestCount = 100;

        Flux.range(1, requestCount)
            .flatMap(i -> {
                ActionRequest request = createSimpleActionRequest("perf-job-" + i);
                return Mono.fromCallable(() -> {
                    try {
                        webTestClient.post()
                            .uri("/api/v1/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isAccepted();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
            })
            .blockLast(Duration.ofMinutes(5));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("虚拟线程性能测试:");
        System.out.println("请求数量: " + requestCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均耗时: " + (duration / requestCount) + "ms/request");

        // 验证性能（应该能在合理时间内完成）
        assertThat(duration).isLessThan(60000); // 应该在60秒内完成
    }

    private ActionRequest createSimpleActionRequest(String jobName) {
        ActionRequest request = new ActionRequest();
        request.setName("Concurrent Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Simple Step");
        step.setRun("echo 'Hello from " + jobName + "'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put(jobName, job);
        request.setJobs(jobs);
        
        return request;
    }
}
