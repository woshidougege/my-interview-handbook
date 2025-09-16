package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 监控和告警配置属性
 * 对应 application-billing.yml 中的 monitoring 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    /**
     * 计费监控配置
     */
    private BillingMonitoringConfig billing = new BillingMonitoringConfig();

    @Data
    public static class BillingMonitoringConfig {
        /**
         * 计费异常告警配置
         */
        private AlertConfig alert = new AlertConfig();

        /**
         * 统计配置
         */
        private MetricsConfig metrics = new MetricsConfig();
    }

    @Data
    public static class AlertConfig {
        /**
         * 是否启用告警
         */
        private Boolean enabled = true;

        /**
         * 告警阈值配置
         */
        private ThresholdConfig threshold = new ThresholdConfig();
    }

    @Data
    public static class ThresholdConfig {
        /**
         * 计费失败率阈值
         */
        private Double billingFailureRate = 0.05;

        /**
         * 积分不足率阈值
         */
        private Double creditInsufficientRate = 0.1;
    }

    @Data
    public static class MetricsConfig {
        /**
         * 是否启用指标收集
         */
        private Boolean enabled = true;

        /**
         * 指标收集间隔（秒）
         */
        private Integer collectionInterval = 60;
    }
}
