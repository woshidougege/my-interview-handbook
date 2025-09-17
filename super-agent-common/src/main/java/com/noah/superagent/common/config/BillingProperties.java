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
         * 模型描述信息
         */
        private String description;
        
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
        private Double functionVideoGeneration;
        private Double functionImageGeneration;
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
         * 积分类型配置
         */
        private Map<String, CreditTypeConfig> types = new HashMap<>();
    }

    @Data
    public static class CreditTypeConfig {
        /**
         * 积分类型名称
         */
        private String name;

        /**
         * 积分类型描述
         */
        private String description;

        /**
         * 有效期天数（0表示永久有效）
         */
        private Integer validityDays;

        /**
         * 扣费优先级（数字越小优先级越高）
         */
        private Integer deductionPriority;
    }
}
