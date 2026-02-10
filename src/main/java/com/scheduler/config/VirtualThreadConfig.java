package com.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * 虚拟线程配置类
 * 使用Java 21的虚拟线程特性来处理并发请求
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * 配置虚拟线程执行器
     * Java 21的虚拟线程可以创建数百万个线程，非常适合高并发场景
     */
    @Bean(name = "virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
