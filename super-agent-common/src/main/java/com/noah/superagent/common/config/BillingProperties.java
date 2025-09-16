package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 计费系统配置属性
 * 对应 application-billing.yml 中的 super-agent.billing 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.billing")
public class BillingProperties {

    /**
     * 是否启用计费
     */
    private Boolean enabled = true;

    /**
     * 模型计费价格配置
     */
    private PricingConfig pricing = new PricingConfig();

    /**
     * 功能计费价格配置
     */
    private FunctionsConfig functions = new FunctionsConfig();

    /**
     * 订阅套餐配置
     */
    private SubscriptionConfig subscription = new SubscriptionConfig();

    /**
     * 积分系统配置
     */
    private CreditsConfig credits = new CreditsConfig();

    @Data
    public static class PricingConfig {
        /**
         * 输入Token价格（每千个Token）
         */
        private Double inputToken = 0.0024;

        /**
         * 输出Token价格（每千个Token）
         */
        private Double outputToken = 0.0096;

        /**
         * 图片生成价格（元/张）
         */
        private Double imageGeneration = 0.25;

        /**
         * 视频生成价格（元/秒）
         */
        private Double videoGeneration = 0.24;
    }

    @Data
    public static class FunctionsConfig {
        // 搜索类功能
        private Double deepsearch = 0.001;
        private Double browseruse = 0.001;

        // 生成类功能
        private Double pptGeneration = 0.001;
        private Double meetingMinutes = 0.001;
        private Double documentWriting = 0.001;
        private Double coding = 0.001;
        private Double translation = 0.001;
        private Double mindMap = 0.001;

        // 分析类功能
        private Double databaseAnalysis = 0.001;
        private Double excelAnalysis = 0.001;
        private Double softwareOperation = 0.001;
    }

    @Data
    public static class SubscriptionConfig {
        /**
         * 按年订阅优惠比例
         */
        private Double yearlyDiscountRate = 0.17;

        /**
         * 套餐积分配置（key为套餐ID，value为积分数量）
         */
        private Map<Integer, Long> planCredits;
    }

    @Data
    public static class CreditsConfig {
        /**
         * 积分兑换比例
         */
        private Double exchangeRate = 0.01;

        /**
         * 免费积分配置
         */
        private FreeCreditsConfig freeCredits = new FreeCreditsConfig();

        /**
         * 活动积分配置
         */
        private ActivityCreditsConfig activityCredits = new ActivityCreditsConfig();

        /**
         * 积分扣费优先级
         */
        private DeductionPriorityConfig deductionPriority = new DeductionPriorityConfig();
    }

    @Data
    public static class FreeCreditsConfig {
        /**
         * 新用户赠送积分
         */
        private Integer newUserAmount = 1000;

        /**
         * 新用户积分有效期（天）
         */
        private Integer newUserValidity = 90;

        /**
         * 每日签到积分（登录时发放）
         */
        private Integer dailySignin = 300;

        /**
         * 每日积分有效期（天）
         */
        private Integer dailyValidity = 1;
    }

    @Data
    public static class ActivityCreditsConfig {
        /**
         * 分享奖励积分
         */
        private Integer shareReward = 500;

        /**
         * 分享积分有效期（天）
         */
        private Integer shareValidity = 30;
    }

    @Data
    public static class DeductionPriorityConfig {
        /**
         * 当日积分
         */
        private Integer daily = 1;

        /**
         * 活动积分
         */
        private Integer activity = 2;

        /**
         * 免费积分
         */
        private Integer free = 3;

        /**
         * 付费积分
         */
        private Integer paid = 4;
    }
}
