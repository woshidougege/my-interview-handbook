package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步处理配置属性
 * 对应 application-async.yml 中的 async 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

    /**
     * 通用异步线程池配置
     */
    private ThreadPoolConfig general = new ThreadPoolConfig();

    /**
     * 支付异步线程池配置
     */
    private ThreadPoolConfig payment = new ThreadPoolConfig();

    /**
     * 消息通知异步线程池配置
     */
    private ThreadPoolConfig notification = new ThreadPoolConfig();

    /**
     * 数据同步异步线程池配置
     */
    private ThreadPoolConfig sync = new ThreadPoolConfig();

    /**
     * 任务调度异步线程池配置
     */
    private ThreadPoolConfig scheduler = new ThreadPoolConfig();

    /**
     * AI服务异步线程池配置
     */
    private ThreadPoolConfig aiService = new ThreadPoolConfig();

    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private Integer corePoolSize = 2;

        /**
         * 最大线程数
         */
        private Integer maxPoolSize = 6;

        /**
         * 队列容量
         */
        private Integer queueCapacity = 200;

        /**
         * 线程保活时间（秒）
         */
        private Integer keepAliveSeconds = 60;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "async-";

        /**
         * 拒绝策略
         */
        private String rejectionPolicy = "CallerRunsPolicy";

        /**
         * 等待任务完成时间（秒）
         */
        private Integer awaitTerminationSeconds = 20;

        /**
         * 是否等待任务完成
         */
        private Boolean waitForTasksToCompleteOnShutdown = true;
    }
}
