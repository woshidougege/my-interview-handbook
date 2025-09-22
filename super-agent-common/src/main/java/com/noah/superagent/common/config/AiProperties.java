package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI服务配置属性
 * 对应 application-ai.yml 中的 super-agent.ai 配置
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.ai")
public class AiProperties {

    /**
     * 阿里云百炼大模型配置
     */
    private AlibabaDashscopeConfig alibabaDashscope = new AlibabaDashscopeConfig();

    @Data
    public static class AlibabaDashscopeConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 基础URL
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";

        /**
         * 标题生成配置
         */
        private TitleGenerationConfig titleGeneration = new TitleGenerationConfig();

        /**
         * HTTP客户端配置
         */
        private HttpConfig http = new HttpConfig();

        /**
         * 重试配置
         */
        private RetryConfig retry = new RetryConfig();
    }

    @Data
    public static class TitleGenerationConfig {
        /**
         * 使用的模型
         */
        private String model = "qwen-plus";

        /**
         * 最大token数
         */
        private Integer maxTokens = 50;

        /**
         * 温度参数
         */
        private Double temperature = 0.7;

        /**
         * 超时时间（秒）
         */
        private Integer timeoutSeconds = 10;

        /**
         * 标题生成提示词模板（支持{question}占位符）
         * 默认值在YAML配置文件中定义
         */
        private String promptTemplate;
    }

    @Data
    public static class HttpConfig {
        /**
         * 连接超时时间（秒）
         */
        private Integer connectTimeoutSeconds = 10;

        /**
         * 读取超时时间（秒）
         */
        private Integer readTimeoutSeconds = 30;

        /**
         * 写入超时时间（秒）
         */
        private Integer writeTimeoutSeconds = 30;
    }

    @Data
    public static class RetryConfig {
        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 3;

        /**
         * 重试延迟（秒）
         */
        private Integer delaySeconds = 2;
    }
}
