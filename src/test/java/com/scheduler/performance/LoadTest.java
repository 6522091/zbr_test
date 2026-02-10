package com.scheduler.performance;

import com.scheduler.model.ActionRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 负载测试
 * 模拟不同负载场景下的系统表现
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("负载测试")
class LoadTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 轻负载测试 - 模拟正常使用场景
     */
    @Test
    @DisplayName("轻负载测试 - 10并发, 100请求")
    void testLightLoad() {
        runLoadTest(10, 100, "轻负载");
    }

    /**
     * 中等负载测试 - 模拟正常峰值
     */
    @Test
    @DisplayName("中等负载测试 - 50并发, 500请求")
    void testMediumLoad() {
        runLoadTest(50, 500, "中等负载");
    }

    /**
     * 重负载测试 - 模拟高负载场景
     */
    @Test
    @DisplayName("重负载测试 - 100并发, 1000请求")
    void testHeavyLoad() {
        runLoadTest(100, 1000, "重负载");
    }

    /**
     * 执行负载测试
     */
    private void runLoadTest(int concurrency, int totalRequests, String loadType) {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxResponseTime = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    ActionRequest request = createActionRequest("load-" + loadType + "-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    long responseTime = System.currentTimeMillis() - requestStart;
                    totalResponseTime.addAndGet(responseTime);
                    updateMinMax(responseTime, minResponseTime, maxResponseTime);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(300, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            double throughput = (successCount.get() * 1000.0) / duration;
            double avgResponseTime = successCount.get() > 0 ? 
                (double) totalResponseTime.get() / successCount.get() : 0;
            double successRate = (successCount.get() * 100.0) / totalRequests;

            System.out.println(String.format(
                "\n%s测试结果:\n" +
                "  并发数: %d\n" +
                "  总请求: %d\n" +
                "  成功: %d (%.2f%%)\n" +
                "  失败: %d\n" +
                "  总耗时: %d ms\n" +
                "  吞吐量: %.2f 请求/秒\n" +
                "  平均响应时间: %.2f ms\n" +
                "  最小响应时间: %d ms\n" +
                "  最大响应时间: %d ms\n",
                loadType, concurrency, totalRequests, successCount.get(), successRate,
                failureCount.get(), duration, throughput, avgResponseTime,
                minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get(),
                maxResponseTime.get()
            ));

            assertThat(completed).isTrue();
            assertThat(successRate).isGreaterThan(90.0); // 成功率应大于90%
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
    }

    private void updateMinMax(long value, AtomicLong min, AtomicLong max) {
        min.updateAndGet(current -> Math.min(current, value));
        max.updateAndGet(current -> Math.max(current, value));
    }

    private ActionRequest createActionRequest(String jobName) {
        ActionRequest request = new ActionRequest();
        request.setName("Load Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Load Test Step");
        step.setRun("echo 'Load test: " + jobName + "'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put(jobName, job);
        request.setJobs(jobs);
        
        return request;
    }
}
