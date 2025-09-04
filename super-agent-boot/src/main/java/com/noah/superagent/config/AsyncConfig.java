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
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.token-report.core-pool-size:4}")
    private int tokenReportCorePoolSize;
    
    @Value("${async.token-report.max-pool-size:8}")
    private int tokenReportMaxPoolSize;
    
    @Value("${async.token-report.queue-capacity:500}")
    private int tokenReportQueueCapacity;
    
    @Value("${async.token-report.keep-alive-seconds:60}")
    private int tokenReportKeepAliveSeconds;
    
    @Value("${async.token-report.thread-name-prefix:token-report-}")
    private String tokenReportThreadNamePrefix;

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
     * Token上报专用线程池
     */
    @Bean("tokenReportExecutor")
    public Executor tokenReportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(tokenReportCorePoolSize);
        executor.setMaxPoolSize(tokenReportMaxPoolSize);
        executor.setQueueCapacity(tokenReportQueueCapacity);
        executor.setThreadNamePrefix(tokenReportThreadNamePrefix);
        executor.setKeepAliveSeconds(tokenReportKeepAliveSeconds);
        
        // 拒绝策略：队列满时让调用线程执行（降级为同步）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("Token上报线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}, 空闲时间: {}s", 
            tokenReportCorePoolSize, tokenReportMaxPoolSize, tokenReportQueueCapacity, tokenReportKeepAliveSeconds);
        
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
