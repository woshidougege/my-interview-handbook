package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 套餐配置类
 * 从配置文件读取套餐信息，替代数据库存储
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "super-agent.plans")
public class PlansConfig {

    /**
     * 年付优惠比例（如0.17表示17%）
     */
    private Double yearlyDiscountRate = 0.17;

    /**
     * 套餐列表
     */
    private List<PlanConfig> subscriptionPlans;

    /**
     * 单个套餐配置
     */
    @Data
    public static class PlanConfig {
        /**
         * 套餐ID
         */
        private String id;

        /**
         * 套餐名称
         */
        private String name;

        /**
         * 套餐代码
         */
        private String code;

        /**
         * 套餐描述
         */
        private String description;

        /**
         * 价格配置字段（-1表示自动计算，否则使用配置值）
         */
        private BigDecimal monthlyOriginalPrice;    // 月价原价（必填）
        private BigDecimal yearlyOriginalPrice;     // 年价原价（必填）
        private BigDecimal monthlyDiscountedPrice = BigDecimal.valueOf(-1);  // 月价优惠后（-1=自动17%优惠）
        private BigDecimal yearlyDiscountedPrice = BigDecimal.valueOf(-1);   // 年价优惠后（-1=自动17%优惠）
        private BigDecimal monthlySavings = BigDecimal.valueOf(-1);          // 月价优惠金额（-1=自动计算）
        private BigDecimal yearlySavings = BigDecimal.valueOf(-1);           // 年价优惠金额（-1=自动计算）

        /**
         * 积分数量（积分套餐专用）
         */
        private Long creditsAmount;

        /**
         * 有效期天数
         */
        private Integer validityDays;

        /**
         * 每日刷新积分数量
         */
        private Integer dailyRefreshCredits;

        /**
         * 排序值
         */
        private Integer sortOrder;

        /**
         * 是否推荐
         */
        private Boolean isRecommended = false;

        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 功能特性列表
         */
        private List<FeatureConfig> features;
    }

    /**
     * 功能特性配置
     */
    @Data
    public static class FeatureConfig {
        /**
         * 功能描述文本
         */
        private String text;

        /**
         * 是否高亮显示
         */
        private Boolean highlight = false;

    }
}
