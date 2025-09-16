package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性
 * 对应 application-billing.yml 中的 cache 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * 计费缓存配置
     */
    private BillingCacheConfig billing = new BillingCacheConfig();

    @Data
    public static class BillingCacheConfig {
        /**
         * 价格缓存过期时间（秒）
         */
        private Integer pricingCacheTtl = 3600;

        /**
         * 用户积分缓存过期时间（秒）
         */
        private Integer userCreditsCacheTtl = 300;

        /**
         * 计费规则缓存过期时间（秒）
         */
        private Integer billingRulesCacheTtl = 1800;
    }
}
