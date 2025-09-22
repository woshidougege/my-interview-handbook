package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步任务配置属性
 * 对应 application-async.yml 中的 super-agent.async 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.async")
public class AsyncProperties {

    /**
     * 通用异步线程池配置
     */
    private ExecutorConfig general = new ExecutorConfig(2, 6, 200, 60, "async-general-", 20, true);

    /**
     * 支付异步线程池配置
     */
    private ExecutorConfig payment = new ExecutorConfig(1, 3, 100, 60, "async-payment-", 30, true);

    /**
     * 消息通知异步线程池配置
     */
    private ExecutorConfig notification = new ExecutorConfig(1, 2, 50, 60, "async-notification-", 15, true);

    /**
     * 数据同步异步线程池配置
     */
    private ExecutorConfig sync = new ExecutorConfig(1, 2, 30, 60, "async-sync-", 30, true);

    /**
     * 任务调度异步线程池配置
     */
    private ExecutorConfig scheduler = new ExecutorConfig(2, 4, 100, 120, "async-scheduler-", 60, true);

    /**
     * AI对话执行异步线程池配置
     */
    private ExecutorConfig aiChatExecution = new ExecutorConfig(10, 20, 500, 60, "ai-chat-execution-", 30, true);

    /**
     * 资源上报异步线程池配置
     */
    private ExecutorConfig resourceReport = new ExecutorConfig(2, 4, 200, 60, "resource-report-", 30, true);

    @Data
    public static class ExecutorConfig {
        /**
         * 核心线程数
         */
        private Integer corePoolSize;

        /**
         * 最大线程数
         */
        private Integer maxPoolSize;

        /**
         * 队列容量
         */
        private Integer queueCapacity;

        /**
         * 线程存活时间（秒）
         */
        private Integer keepAliveSeconds;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix;

        /**
         * 等待任务完成的超时时间（秒）
         */
        private Integer awaitTerminationSeconds;

        /**
         * 关闭时是否等待任务完成
         */
        private Boolean waitForTasksToCompleteOnShutdown;

        // 默认构造函数
        public ExecutorConfig() {
        }

        // 带参数的构造函数，便于设置默认值
        public ExecutorConfig(Integer corePoolSize, Integer maxPoolSize, Integer queueCapacity,
                             Integer keepAliveSeconds, String threadNamePrefix,
                             Integer awaitTerminationSeconds, Boolean waitForTasksToCompleteOnShutdown) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueCapacity = queueCapacity;
            this.keepAliveSeconds = keepAliveSeconds;
            this.threadNamePrefix = threadNamePrefix;
            this.awaitTerminationSeconds = awaitTerminationSeconds;
            this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
        }
    }
}