package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
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
     * 欠费配置
     */
    private OverdraftConfig overdraft = new OverdraftConfig();

    /**
     * 模型类默认积分消耗配置
     */
    private ModelDefaultsConfig modelDefaults = new ModelDefaultsConfig();

    /**
     * 功能类默认积分消耗配置
     */
    private FunctionDefaultsConfig functionDefaults = new FunctionDefaultsConfig();

    /**
     * 按模型特定配置
     */
    private Map<String, ModelConfig> models = new HashMap<>();

    /**
     * 订阅套餐配置
     */
    private SubscriptionConfig subscription = new SubscriptionConfig();

    /**
     * 积分系统配置
     */
    private CreditsConfig credits = new CreditsConfig();

    @Data
    public static class OverdraftConfig {
        /**
         * 是否允许欠费
         */
        private Boolean enabled = true;

        /**
         * 最大欠费额度（积分）
         */
        private BigDecimal maxAmount = BigDecimal.valueOf(300);

        /**
         * 欠费警告阈值（积分）
         */
        private BigDecimal warningThreshold = BigDecimal.valueOf(50);
    }

    @Data
    public static class ModelDefaultsConfig {
        /**
         * 文本生成默认积分消耗
         */
        private TextGenerationConfig textGeneration = new TextGenerationConfig();

        /**
         * 图片生成默认积分消耗（张/积分）
         */
        private Double imageGeneration = 25.0;

        /**
         * 视频生成默认积分消耗（秒/积分）
         */
        private Double videoGeneration = 24.0;
    }

    @Data
    public static class FunctionDefaultsConfig {
        // 功能类积分消耗（次/积分或页/积分）
        private Double browseruse = 0.1;
        private Double deepsearch = 0.1;
        private Double softwareOperation = 0.1;
        private Double pptGeneration = 0.1;        // 页/积分
        private Double meetingMinutes = 0.1;
        private Double documentWriting = 0.1;      // word文档
        private Double coding = 0.1;               // 编码+执行
        private Double functionVideoGeneration = 24.0; // 功能类文生视频（秒/积分）
        private Double functionImageGeneration = 25.0; // 功能类图片生成（张/积分）
        private Double translation = 0.1;
        private Double mindMap = 0.1;
        private Double databaseAnalysis = 0.1;
        private Double excelAnalysis = 0.1;
    }

    @Data
    public static class ModelConfig {
        /**
         * 继承的父模型配置
         */
        private String extendsModel;
        
        /**
         * 文本生成积分消耗配置
         */
        private TextGenerationConfig textGeneration;

        /**
         * 图片生成积分消耗（张/积分）
         */
        private Double imageGeneration;

        /**
         * 视频生成积分消耗（秒/积分）
         */
        private Double videoGeneration;

        // 功能类积分消耗覆盖
        private Double browseruse;
        private Double deepsearch;
        private Double softwareOperation;
        private Double pptGeneration;
        private Double meetingMinutes;
        private Double documentWriting;
        private Double coding;
        private Double translation;
        private Double mindMap;
        private Double databaseAnalysis;
        private Double excelAnalysis;
    }

    @Data
    public static class TextGenerationConfig {
        /**
         * 输入Token积分消耗（每千个Token）
         */
        private Double inputToken = 2.4;

        /**
         * 输出Token积分消耗（每千个Token）
         */
        private Double outputToken = 9.6;
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
