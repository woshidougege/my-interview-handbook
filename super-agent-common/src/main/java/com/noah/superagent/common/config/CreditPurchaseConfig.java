package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分购买配置类
 * 从配置文件读取积分购买相关配置
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "super-agent.credit-purchase")
public class CreditPurchaseConfig {

    /**
     * 积分有效期天数（0表示永久有效）
     */
    private Integer validityDays = 0;

    /**
     * 积分包配置列表
     */
    private List<CreditPackageConfig> packages;

    /**
     * 单个积分包配置
     */
    @Data
    public static class CreditPackageConfig {
        /**
         * 积分包ID
         */
        private String id;

        /**
         * 积分包名称
         */
        private String name;

        /**
         * 积分数量
         */
        private Long creditsAmount;

        /**
         * 价格
         */
        private BigDecimal price;

        /**
         * 是否推荐
         */
        private Boolean isRecommended = false;

        /**
         * 描述
         */
        private String description;

        /**
         * 功能特性列表
         */
        private List<String> features;
    }
}
