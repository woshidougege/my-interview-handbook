package com.noah.superagent.config;

import com.noah.superagent.common.config.AsyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 配置各种异步任务的线程池
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private final AsyncProperties asyncProperties;

    /**
     * 创建线程池执行器的通用方法
     */
    private ThreadPoolTaskExecutor createExecutor(AsyncProperties.ExecutorConfig config, String description) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(config.getWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());
        
        executor.initialize();
        
        log.info("{}已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                description, config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity());
        
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理一般的异步任务
     */
    @Bean("general-async-executor")
    public Executor generalAsyncExecutor() {
        return createExecutor(asyncProperties.getGeneral(), "通用异步线程池");
    }

    /**
     * 支付异步线程池
     * 用于处理支付相关的异步任务
     */
    @Bean("payment-async-executor")
    public Executor paymentAsyncExecutor() {
        return createExecutor(asyncProperties.getPayment(), "支付异步线程池");
    }

    /**
     * 消息通知异步线程池
     * 用于处理消息通知相关的异步任务
     */
    @Bean("notification-async-executor")
    public Executor notificationAsyncExecutor() {
        return createExecutor(asyncProperties.getNotification(), "消息通知异步线程池");
    }

    /**
     * 数据同步异步线程池
     * 用于处理数据同步相关的异步任务
     */
    @Bean("sync-async-executor")
    public Executor syncAsyncExecutor() {
        return createExecutor(asyncProperties.getSync(), "数据同步异步线程池");
    }

    /**
     * 任务调度异步线程池
     * 用于处理任务调度相关的异步任务
     */
    @Bean("scheduler-async-executor")
    public Executor schedulerAsyncExecutor() {
        return createExecutor(asyncProperties.getScheduler(), "任务调度异步线程池");
    }


    /**
     * 资源上报异步线程池
     * 用于处理资源使用量上报相关的异步任务
     */
    @Bean("resourceReportExecutor")
    public Executor resourceReportExecutor() {
        return createExecutor(asyncProperties.getResourceReport(), "资源上报异步线程池");
    }
}