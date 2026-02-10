package com.scheduler.performance;

import com.scheduler.model.ActionRequest;
import com.scheduler.model.RunResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 性能测试套件
 * 测试系统在不同负载下的性能表现
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("性能测试套件")
class PerformanceTestSuite {

    @Autowired
    private WebTestClient webTestClient;

    private static final int WARMUP_ITERATIONS = 10;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * 性能测试结果记录
     */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("性能指标收集")
    class PerformanceMetrics {
        private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);

        public void recordResponseTime(long timeMs) {
            responseTimes.add(timeMs);
        }

        public void recordSuccess() {
            successCount.incrementAndGet();
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        public PerformanceReport generateReport(String testName) {
            if (responseTimes.isEmpty()) {
                return new PerformanceReport(testName, 0, 0, 0L, 0L, 0.0, 0L, 0L, 0L);
            }

            List<Long> sorted = new ArrayList<>(responseTimes);
            Collections.sort(sorted);

            long min = sorted.get(0);
            long max = sorted.get(sorted.size() - 1);
            double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
            long p50 = sorted.get(sorted.size() / 2);
            long p95 = sorted.size() > 1 ? sorted.get((int) (sorted.size() * 0.95)) : sorted.get(0);
            long p99 = sorted.size() > 1 ? sorted.get((int) (sorted.size() * 0.99)) : sorted.get(0);

            return new PerformanceReport(
                testName,
                successCount.get(),
                failureCount.get(),
                (long) min,
                (long) max,
                avg,
                p50,
                p95,
                p99
            );
        }

        public void reset() {
            responseTimes.clear();
            successCount.set(0);
            failureCount.set(0);
        }
    }

    private final PerformanceMetrics metrics = new PerformanceMetrics();

    @BeforeEach
    void setUp() {
        metrics.reset();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后输出性能报告
        PerformanceReport report = metrics.generateReport("Test");
        if (report.getTotalRequests() > 0) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println(report);
            System.out.println("=".repeat(80) + "\n");
        }
    }

    /**
     * 测试1: 吞吐量测试
     * 测试系统在固定时间内能处理多少请求
     */
    @Test
    @Order(1)
    @DisplayName("吞吐量测试 - 1000请求/60秒")
    void testThroughput() {
        int totalRequests = 1000;
        int durationSeconds = 60;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    if (System.currentTimeMillis() < endTime) {
                        long requestStart = System.currentTimeMillis();
                        ActionRequest request = createSimpleActionRequest("throughput-" + requestId);
                        
                        webTestClient.post()
                            .uri("/api/v1/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isAccepted();

                        long responseTime = System.currentTimeMillis() - requestStart;
                        metrics.recordResponseTime(responseTime);
                        metrics.recordSuccess();
                        completed.incrementAndGet();
                    }
                } catch (Exception e) {
                    metrics.recordFailure();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(durationSeconds + 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        long actualDuration = System.currentTimeMillis() - startTime;
        double throughput = (completed.get() * 1000.0) / actualDuration;

        System.out.println(String.format(
            "吞吐量测试结果: 完成 %d/%d 请求, 耗时 %d ms, 吞吐量: %.2f 请求/秒",
            completed.get(), totalRequests, actualDuration, throughput
        ));

        assertThat(throughput).isGreaterThan(10.0); // 至少10请求/秒
    }

    /**
     * 测试2: 响应时间测试
     * 测试系统在不同负载下的响应时间分布
     */
    @Test
    @Order(2)
    @DisplayName("响应时间测试 - P50/P95/P99")
    void testResponseTime() {
        int requests = 500;
        int concurrency = 20;

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long start = System.currentTimeMillis();
                    ActionRequest request = createSimpleActionRequest("response-time-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    long responseTime = System.currentTimeMillis() - start;
                    metrics.recordResponseTime(responseTime);
                    metrics.recordSuccess();
                } catch (Exception e) {
                    metrics.recordFailure();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        PerformanceReport report = metrics.generateReport("响应时间测试");
        assertThat(report.getP95()).isLessThan(5000); // P95响应时间应小于5秒
        assertThat(report.getP99()).isLessThan(10000); // P99响应时间应小于10秒
    }

    /**
     * 测试3: 高并发压力测试
     * 测试系统在极高并发下的表现
     */
    @Test
    @Order(3)
    @DisplayName("高并发压力测试 - 1000并发")
    void testHighConcurrency() {
        int concurrentRequests = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        AtomicInteger success = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ActionRequest request = createSimpleActionRequest("concurrent-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    success.incrementAndGet();
                    metrics.recordSuccess();
                } catch (Exception e) {
                    metrics.recordFailure();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(180, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println(String.format(
                "高并发测试: %d并发请求, 成功: %d, 失败: %d, 耗时: %d ms",
                concurrentRequests, success.get(), 
                concurrentRequests - success.get(), duration
            ));

            assertThat(completed).isTrue();
            // 至少80%的请求应该成功
            assertThat(success.get()).isGreaterThan((int) (concurrentRequests * 0.8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
    }

    /**
     * 测试4: 虚拟线程性能测试
     * 验证虚拟线程在高并发场景下的优势
     */
    @Test
    @Order(4)
    @DisplayName("虚拟线程性能测试 - 10000请求")
    void testVirtualThreadPerformance() {
        int totalRequests = 10000;
        int batchSize = 100;
        long startTime = System.currentTimeMillis();

        // 使用虚拟线程执行器
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger completed = new AtomicInteger(0);

        for (int batch = 0; batch < totalRequests / batchSize; batch++) {
            final int batchId = batch;
            virtualExecutor.submit(() -> {
                for (int i = 0; i < batchSize; i++) {
                    final int requestId = batchId * batchSize + i;
                    try {
                        long requestStart = System.currentTimeMillis();
                        ActionRequest request = createSimpleActionRequest("virtual-" + requestId);
                        
                        webTestClient.post()
                            .uri("/api/v1/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .exchange()
                            .expectStatus().isAccepted();

                        long responseTime = System.currentTimeMillis() - requestStart;
                        metrics.recordResponseTime(responseTime);
                        metrics.recordSuccess();
                        completed.incrementAndGet();
                    } catch (Exception e) {
                        metrics.recordFailure();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            boolean finished = latch.await(300, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            double throughput = (completed.get() * 1000.0) / duration;

            System.out.println(String.format(
                "虚拟线程性能测试: 完成 %d/%d 请求, 耗时 %d ms, 吞吐量: %.2f 请求/秒",
                completed.get(), totalRequests, duration, throughput
            ));

            assertThat(finished).isTrue();
            assertThat(throughput).isGreaterThan(50.0); // 虚拟线程应该能达到更高的吞吐量
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        virtualExecutor.shutdown();
    }

    /**
     * 测试5: 长时间稳定性测试
     * 测试系统在长时间运行下的稳定性
     */
    @Test
    @Order(5)
    @DisplayName("长时间稳定性测试 - 5分钟持续负载")
    @Timeout(400) // 6分40秒超时
    void testLongRunningStability() {
        int durationMinutes = 5;
        int requestsPerSecond = 10;
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // 每秒发送指定数量的请求
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() < endTime) {
                for (int i = 0; i < requestsPerSecond; i++) {
                    final int requestId = totalRequests.incrementAndGet();
                    scheduler.submit(() -> {
                        try {
                            ActionRequest request = createSimpleActionRequest("stability-" + requestId);
                            
                            webTestClient.post()
                                .uri("/api/v1/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isAccepted();

                            successCount.incrementAndGet();
                            metrics.recordSuccess();
                        } catch (Exception e) {
                            metrics.recordFailure();
                        }
                    });
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        // 等待测试完成
        try {
            Thread.sleep(durationMinutes * 60 * 1000L + 5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.shutdown();
        
        int expectedRequests = durationMinutes * 60 * requestsPerSecond;
        double successRate = (successCount.get() * 100.0) / totalRequests.get();

        System.out.println(String.format(
            "稳定性测试: 总请求 %d, 成功 %d, 成功率: %.2f%%",
            totalRequests.get(), successCount.get(), successRate
        ));

        assertThat(successRate).isGreaterThan(95.0); // 成功率应大于95%
    }

    /**
     * 测试6: 内存和资源使用测试
     * 测试系统在负载下的资源使用情况
     */
    @Test
    @Order(6)
    @DisplayName("资源使用测试")
    void testResourceUsage() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        int requests = 500;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ActionRequest request = createSimpleActionRequest("resource-" + requestId);
                    
                    webTestClient.post()
                        .uri("/api/v1/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isAccepted();

                    metrics.recordSuccess();
                } catch (Exception e) {
                    metrics.recordFailure();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        
        // 强制GC
        System.gc();
        Thread.yield();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        long maxMemory = runtime.maxMemory();

        System.out.println(String.format(
            "资源使用测试: 初始内存: %d MB, 最终内存: %d MB, 使用: %d MB, 最大内存: %d MB",
            initialMemory / 1024 / 1024,
            finalMemory / 1024 / 1024,
            memoryUsed / 1024 / 1024,
            maxMemory / 1024 / 1024
        ));

        // 内存使用不应超过最大内存的80%
        assertThat(finalMemory).isLessThan((long) (maxMemory * 0.8));
    }

    /**
     * 创建简单的Action请求
     */
    private ActionRequest createSimpleActionRequest(String jobName) {
        ActionRequest request = new ActionRequest();
        request.setName("Performance Test Action");
        
        Map<String, ActionRequest.Job> jobs = new HashMap<>();
        ActionRequest.Job job = new ActionRequest.Job();
        job.setRunsOn("ubuntu-latest");
        
        ActionRequest.Step step = new ActionRequest.Step();
        step.setName("Test Step");
        step.setRun("echo 'Performance test: " + jobName + "'");
        job.setSteps(new ActionRequest.Step[]{step});
        
        jobs.put(jobName, job);
        request.setJobs(jobs);
        
        return request;
    }

    /**
     * 性能测试报告
     */
    static class PerformanceReport {
        private final String testName;
        private final int successCount;
        private final int failureCount;
        private final long minResponseTime;
        private final long maxResponseTime;
        private final double avgResponseTime;
        private final long p50;
        private final long p95;
        private final long p99;

        public PerformanceReport(String testName, int successCount, int failureCount,
                                long minResponseTime, long maxResponseTime, double avgResponseTime,
                                long p50, long p95, long p99) {
            this.testName = testName;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.minResponseTime = minResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.avgResponseTime = avgResponseTime;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
        }

        public int getTotalRequests() {
            return successCount + failureCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public long getMinResponseTime() {
            return minResponseTime;
        }

        public long getMaxResponseTime() {
            return maxResponseTime;
        }

        public double getAvgResponseTime() {
            return avgResponseTime;
        }

        public long getP50() {
            return p50;
        }

        public long getP95() {
            return p95;
        }

        public long getP99() {
            return p99;
        }

        @Override
        public String toString() {
            return String.format(
                "性能测试报告: %s\n" +
                "  总请求数: %d\n" +
                "  成功: %d, 失败: %d\n" +
                "  响应时间 (ms):\n" +
                "    最小值: %d\n" +
                "    最大值: %d\n" +
                "    平均值: %.2f\n" +
                "    P50: %d\n" +
                "    P95: %d\n" +
                "    P99: %d",
                testName, getTotalRequests(), successCount, failureCount,
                minResponseTime, maxResponseTime, avgResponseTime, p50, p95, p99
            );
        }
    }
}
