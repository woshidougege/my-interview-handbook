package com.noah.superagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务配置属性
 * 用于配置提示和IDE支持
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alicloud.ai")
@EnableConfigurationProperties
public class AiProperties {

    /**
     * 是否启用AI服务
     */
    private Boolean enabled = true;

    /**
     * API配置
     */
    private ApiConfig api = new ApiConfig();

    /**
     * 模型配置
     */
    private ModelsConfig models = new ModelsConfig();

    /**
     * 功能配置
     */
    private FeaturesConfig features = new FeaturesConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 异步配置
     */
    private AiAsyncConfig async = new AiAsyncConfig();

    @Data
    public static class ApiConfig {
        private String apiKey;
        private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private Integer timeout = 30;
        private Integer retryCount = 3;
    }

    @Data
    public static class ModelsConfig {
        private TitleGenerationConfig titleGeneration = new TitleGenerationConfig();

        @Data
        public static class TitleGenerationConfig {
            private String model = "qwen-turbo";
            private ParametersConfig parameters = new ParametersConfig();

            @Data
            public static class ParametersConfig {
                private Double temperature = 0.5;
                private Double topP = 0.9;
                private Integer maxTokens = 50;
            }
        }
    }

    @Data
    public static class FeaturesConfig {
        private TitleGenerationConfig titleGeneration = new TitleGenerationConfig();

        @Data
        public static class TitleGenerationConfig {
            private Boolean enabled = true;
            private String promptTemplate = "请根据用户的问题生成一个简洁、准确的会话标题。标题应该：\n1. 不超过20个字符\n2. 准确概括问题的核心内容\n3. 简洁明了，易于理解\n4. 不包含特殊符号\n\n用户问题：{question}\n\n请直接返回标题，不要包含其他内容：";
            private String defaultTitle = "新对话";
            private Integer maxLength = 20;
        }
    }

    @Data
    public static class CacheConfig {
        private Boolean enabled = true;
        private TitleGenerationCache titleGeneration = new TitleGenerationCache();

        @Data
        public static class TitleGenerationCache {
            private Long ttl = 3600L;
            private Integer maxSize = 1000;
        }
    }

    @Data
    public static class RateLimitConfig {
        private Boolean enabled = true;
        private TitleGenerationRateLimit titleGeneration = new TitleGenerationRateLimit();

        @Data
        public static class TitleGenerationRateLimit {
            private Integer requestsPerMinute = 60;
            private Integer requestsPerUserPerMinute = 10;
        }
    }

    @Data
    public static class MonitoringConfig {
        private Boolean enabled = true;
        private MetricsConfig metrics = new MetricsConfig();

        @Data
        public static class MetricsConfig {
            private Boolean successRateEnabled = true;
            private Boolean responseTimeEnabled = true;
            private Boolean errorCountEnabled = true;
        }
    }

    @Data
    public static class AiAsyncConfig {
        private AiServiceConfig aiService = new AiServiceConfig();

        @Data
        public static class AiServiceConfig {
            private Integer corePoolSize = 2;
            private Integer maxPoolSize = 4;
            private Integer queueCapacity = 100;
            private Integer keepAliveSeconds = 60;
            private String threadNamePrefix = "ai-service-";
        }
    }
}
