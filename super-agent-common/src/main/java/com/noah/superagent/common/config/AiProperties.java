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
         * 标题生成配置
         */
        private TitleGenerationConfig titleGeneration = new TitleGenerationConfig();
        
        /**
         * 语音识别配置
         */
        private SpeechRecognitionConfig speechRecognition = new SpeechRecognitionConfig();
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
         * 系统角色提示词 - 定义AI的角色、能力和规则
         * 默认值在YAML配置文件中定义
         */
        private String systemPrompt;
    }


    @Data
    public static class SpeechRecognitionConfig {
        /**
         * 使用的模型
         */
        private String model = "paraformer-realtime-v2";

        /**
         * 音频采样率
         */
        private Integer sampleRate = 16000;

        /**
         * 音频格式
         */
        private String audioFormat = "wav";

        /**
         * 语言提示
         */
        private String languageHints = "zh";

        /**
         * 是否启用标点符号预测
         */
        private Boolean enablePunctuation = true;

        /**
         * 是否启用逆文本正则化（ITN）
         */
        private Boolean enableItn = true;

        /**
         * 是否启用心跳保持长连接
         */
        private Boolean heartbeat = true;
    }
}
