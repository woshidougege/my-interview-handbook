package com.noah.superagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class AsyncConfig {

    // ========== 通用异步线程池配置 ==========
    @Value("${async.general.core-pool-size:2}")
    private Integer generalCorePoolSize;

    @Value("${async.general.max-pool-size:6}")
    private Integer generalMaxPoolSize;

    @Value("${async.general.queue-capacity:200}")
    private Integer generalQueueCapacity;

    @Value("${async.general.keep-alive-seconds:60}")
    private Integer generalKeepAliveSeconds;

    @Value("${async.general.thread-name-prefix:async-general-}")
    private String generalThreadNamePrefix;

    @Value("${async.general.await-termination-seconds:20}")
    private Integer generalAwaitTerminationSeconds;

    @Value("${async.general.wait-for-tasks-to-complete-on-shutdown:true}")
    private Boolean generalWaitForTasksToCompleteOnShutdown;

    // ========== 支付异步线程池配置 ==========
    @Value("${async.payment.core-pool-size:1}")
    private Integer paymentCorePoolSize;

    @Value("${async.payment.max-pool-size:3}")
    private Integer paymentMaxPoolSize;

    @Value("${async.payment.queue-capacity:100}")
    private Integer paymentQueueCapacity;

    @Value("${async.payment.keep-alive-seconds:60}")
    private Integer paymentKeepAliveSeconds;

    @Value("${async.payment.thread-name-prefix:async-payment-}")
    private String paymentThreadNamePrefix;

    @Value("${async.payment.await-termination-seconds:30}")
    private Integer paymentAwaitTerminationSeconds;

    @Value("${async.payment.wait-for-tasks-to-complete-on-shutdown:true}")
    private Boolean paymentWaitForTasksToCompleteOnShutdown;

    // ========== 消息通知异步线程池配置 ==========
    @Value("${async.notification.core-pool-size:1}")
    private Integer notificationCorePoolSize;

    @Value("${async.notification.max-pool-size:2}")
    private Integer notificationMaxPoolSize;

    @Value("${async.notification.queue-capacity:50}")
    private Integer notificationQueueCapacity;

    @Value("${async.notification.keep-alive-seconds:60}")
    private Integer notificationKeepAliveSeconds;

    @Value("${async.notification.thread-name-prefix:async-notification-}")
    private String notificationThreadNamePrefix;

    @Value("${async.notification.await-termination-seconds:15}")
    private Integer notificationAwaitTerminationSeconds;

    @Value("${async.notification.wait-for-tasks-to-complete-on-shutdown:true}")
    private Boolean notificationWaitForTasksToCompleteOnShutdown;

    // ========== 数据同步异步线程池配置 ==========
    @Value("${async.sync.core-pool-size:1}")
    private Integer syncCorePoolSize;

    @Value("${async.sync.max-pool-size:2}")
    private Integer syncMaxPoolSize;

    @Value("${async.sync.queue-capacity:30}")
    private Integer syncQueueCapacity;

    @Value("${async.sync.keep-alive-seconds:60}")
    private Integer syncKeepAliveSeconds;

    @Value("${async.sync.thread-name-prefix:async-sync-}")
    private String syncThreadNamePrefix;

    @Value("${async.sync.await-termination-seconds:30}")
    private Integer syncAwaitTerminationSeconds;

    @Value("${async.sync.wait-for-tasks-to-complete-on-shutdown:true}")
    private Boolean syncWaitForTasksToCompleteOnShutdown;

    // ========== 任务调度异步线程池配置 ==========
    @Value("${async.scheduler.core-pool-size:2}")
    private Integer schedulerCorePoolSize;

    @Value("${async.scheduler.max-pool-size:4}")
    private Integer schedulerMaxPoolSize;

    @Value("${async.scheduler.queue-capacity:100}")
    private Integer schedulerQueueCapacity;

    @Value("${async.scheduler.keep-alive-seconds:120}")
    private Integer schedulerKeepAliveSeconds;

    @Value("${async.scheduler.thread-name-prefix:async-scheduler-}")
    private String schedulerThreadNamePrefix;

    @Value("${async.scheduler.await-termination-seconds:60}")
    private Integer schedulerAwaitTerminationSeconds;

    @Value("${async.scheduler.wait-for-tasks-to-complete-on-shutdown:true}")
    private Boolean schedulerWaitForTasksToCompleteOnShutdown;

    // ========== AI服务异步线程池配置 ==========
    @Value("${alicloud.ai.async.ai-service.core-pool-size:2}")
    private Integer aiServiceCorePoolSize;

    @Value("${alicloud.ai.async.ai-service.max-pool-size:4}")
    private Integer aiServiceMaxPoolSize;

    @Value("${alicloud.ai.async.ai-service.queue-capacity:100}")
    private Integer aiServiceQueueCapacity;

    @Value("${alicloud.ai.async.ai-service.keep-alive-seconds:60}")
    private Integer aiServiceKeepAliveSeconds;

    @Value("${alicloud.ai.async.ai-service.thread-name-prefix:ai-service-}")
    private String aiServiceThreadNamePrefix;

    /**
     * 通用异步线程池
     * 用于处理一般的异步任务
     */
    @Bean("general-async-executor")
    public Executor generalAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(generalCorePoolSize);
        executor.setMaxPoolSize(generalMaxPoolSize);
        executor.setQueueCapacity(generalQueueCapacity);
        executor.setKeepAliveSeconds(generalKeepAliveSeconds);
        executor.setThreadNamePrefix(generalThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(generalWaitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(generalAwaitTerminationSeconds);
        
        executor.initialize();
        
        log.info("通用异步线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                generalCorePoolSize, generalMaxPoolSize, generalQueueCapacity);
        
        return executor;
    }

    /**
     * 支付异步线程池
     * 用于处理支付相关的异步任务
     */
    @Bean("payment-async-executor")
    public Executor paymentAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(paymentCorePoolSize);
        executor.setMaxPoolSize(paymentMaxPoolSize);
        executor.setQueueCapacity(paymentQueueCapacity);
        executor.setKeepAliveSeconds(paymentKeepAliveSeconds);
        executor.setThreadNamePrefix(paymentThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(paymentWaitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(paymentAwaitTerminationSeconds);
        
        executor.initialize();
        
        log.info("支付异步线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                paymentCorePoolSize, paymentMaxPoolSize, paymentQueueCapacity);
        
        return executor;
    }

    /**
     * 消息通知异步线程池
     * 用于处理消息通知相关的异步任务
     */
    @Bean("notification-async-executor")
    public Executor notificationAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(notificationCorePoolSize);
        executor.setMaxPoolSize(notificationMaxPoolSize);
        executor.setQueueCapacity(notificationQueueCapacity);
        executor.setKeepAliveSeconds(notificationKeepAliveSeconds);
        executor.setThreadNamePrefix(notificationThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(notificationWaitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(notificationAwaitTerminationSeconds);
        
        executor.initialize();
        
        log.info("消息通知异步线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                notificationCorePoolSize, notificationMaxPoolSize, notificationQueueCapacity);
        
        return executor;
    }

    /**
     * 数据同步异步线程池
     * 用于处理数据同步相关的异步任务
     */
    @Bean("sync-async-executor")
    public Executor syncAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(syncCorePoolSize);
        executor.setMaxPoolSize(syncMaxPoolSize);
        executor.setQueueCapacity(syncQueueCapacity);
        executor.setKeepAliveSeconds(syncKeepAliveSeconds);
        executor.setThreadNamePrefix(syncThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(syncWaitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(syncAwaitTerminationSeconds);
        
        executor.initialize();
        
        log.info("数据同步异步线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                syncCorePoolSize, syncMaxPoolSize, syncQueueCapacity);
        
        return executor;
    }

    /**
     * 任务调度异步线程池
     * 用于处理任务调度相关的异步任务
     */
    @Bean("scheduler-async-executor")
    public Executor schedulerAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(schedulerCorePoolSize);
        executor.setMaxPoolSize(schedulerMaxPoolSize);
        executor.setQueueCapacity(schedulerQueueCapacity);
        executor.setKeepAliveSeconds(schedulerKeepAliveSeconds);
        executor.setThreadNamePrefix(schedulerThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(schedulerWaitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(schedulerAwaitTerminationSeconds);
        
        executor.initialize();
        
        log.info("任务调度异步线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                schedulerCorePoolSize, schedulerMaxPoolSize, schedulerQueueCapacity);
        
        return executor;
    }

    /**
     * AI服务专用线程池
     * 用于处理AI相关的异步任务，如标题生成、对话等
     */
    @Bean("ai-service-executor")
    public Executor aiServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(aiServiceCorePoolSize);
        // 最大线程数
        executor.setMaxPoolSize(aiServiceMaxPoolSize);
        // 队列容量
        executor.setQueueCapacity(aiServiceQueueCapacity);
        // 线程存活时间
        executor.setKeepAliveSeconds(aiServiceKeepAliveSeconds);
        // 线程名前缀
        executor.setThreadNamePrefix(aiServiceThreadNamePrefix);
        
        // 拒绝策略：使用调用者线程运行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("AI服务线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                aiServiceCorePoolSize, aiServiceMaxPoolSize, aiServiceQueueCapacity);
        
        return executor;
    }
}