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
 * 异步处理配置
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    // 资源上报线程池配置
    @Value("${async.resource-report.core-pool-size:4}")
    private int resourceReportCorePoolSize;
    
    @Value("${async.resource-report.max-pool-size:8}")
    private int resourceReportMaxPoolSize;
    
    @Value("${async.resource-report.queue-capacity:500}")
    private int resourceReportQueueCapacity;
    
    @Value("${async.resource-report.keep-alive-seconds:60}")
    private int resourceReportKeepAliveSeconds;
    
    @Value("${async.resource-report.thread-name-prefix:resource-report-}")
    private String resourceReportThreadNamePrefix;

    // 计费处理线程池配置
    @Value("${async.billing-process.core-pool-size:2}")
    private int billingProcessCorePoolSize;
    
    @Value("${async.billing-process.max-pool-size:4}")
    private int billingProcessMaxPoolSize;
    
    @Value("${async.billing-process.queue-capacity:200}")
    private int billingProcessQueueCapacity;
    
    @Value("${async.billing-process.keep-alive-seconds:60}")
    private int billingProcessKeepAliveSeconds;
    
    @Value("${async.billing-process.thread-name-prefix:billing-process-}")
    private String billingProcessThreadNamePrefix;

    // 通用异步线程池配置
    @Value("${async.general.core-pool-size:2}")
    private int generalCorePoolSize;
    
    @Value("${async.general.max-pool-size:6}")
    private int generalMaxPoolSize;
    
    @Value("${async.general.queue-capacity:200}")
    private int generalQueueCapacity;
    
    @Value("${async.general.keep-alive-seconds:60}")
    private int generalKeepAliveSeconds;
    
    @Value("${async.general.thread-name-prefix:async-}")
    private String generalThreadNamePrefix;

    /**
     * 资源上报专用线程池
     */
    @Bean("resourceReportExecutor")
    public Executor resourceReportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(resourceReportCorePoolSize);
        executor.setMaxPoolSize(resourceReportMaxPoolSize);
        executor.setQueueCapacity(resourceReportQueueCapacity);
        executor.setThreadNamePrefix(resourceReportThreadNamePrefix);
        executor.setKeepAliveSeconds(resourceReportKeepAliveSeconds);
        
        // 拒绝策略：队列满时让调用线程执行（降级为同步）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("资源上报线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}, 空闲时间: {}s", 
            resourceReportCorePoolSize, resourceReportMaxPoolSize, resourceReportQueueCapacity, resourceReportKeepAliveSeconds);
        
        return executor;
    }

    /**
     * 计费处理专用线程池
     */
    @Bean("billingProcessExecutor")
    public Executor billingProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(billingProcessCorePoolSize);
        executor.setMaxPoolSize(billingProcessMaxPoolSize);
        executor.setQueueCapacity(billingProcessQueueCapacity);
        executor.setThreadNamePrefix(billingProcessThreadNamePrefix);
        executor.setKeepAliveSeconds(billingProcessKeepAliveSeconds);
        
        // 拒绝策略：队列满时让调用线程执行（降级为同步）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("计费处理线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}, 空闲时间: {}s", 
            billingProcessCorePoolSize, billingProcessMaxPoolSize, billingProcessQueueCapacity, billingProcessKeepAliveSeconds);
        
        return executor;
    }
    
    /**
     * 通用异步处理线程池
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(generalCorePoolSize);
        executor.setMaxPoolSize(generalMaxPoolSize);
        executor.setQueueCapacity(generalQueueCapacity);
        executor.setThreadNamePrefix(generalThreadNamePrefix);
        executor.setKeepAliveSeconds(generalKeepAliveSeconds);
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("通用异步线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
            generalCorePoolSize, generalMaxPoolSize, generalQueueCapacity);
        
        return executor;
    }
}
