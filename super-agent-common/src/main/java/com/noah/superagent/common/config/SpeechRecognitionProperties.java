package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 语音识别配置属性 - 支持文本纠错
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.ai.alibaba-dashscope.speech-recognition")
public class SpeechRecognitionProperties {

    /**
     * FunASR本地服务配置
     */
    private FunAsrConfig funasr = new FunAsrConfig();
    
    /**
     * 文本纠错配置
     */
    private TextCorrectionConfig textCorrection = new TextCorrectionConfig();

    /**
     * FunASR配置类
     */
    @Data
    public static class FunAsrConfig {
        /**
         * FunASR服务URL
         */
        private String serverUrl = "http://localhost:5000";

        /**
         * 超时时间（秒）
         */
        private Integer timeout = 120;
    }
    
    /**
     * 文本纠错配置类
     */
    @Data
    public static class TextCorrectionConfig {
        /**
         * 是否启用文本纠错
         */
        private Boolean enabled = true;
        
        /**
         * 音频最小时长（秒），超过此时长才触发纠错
         */
        private Integer minAudioDuration = 300; // 5分钟
        
        /**
         * 文本最小长度（字符），超过此长度才触发纠错
         */
        private Integer minTextLength = 500; // 500字符
        
        /**
         * 使用的纠错模型
         */
        private String model = "qwen-plus";
        
        /**
         * 最大token数
         */
        private Integer maxTokens = 4000;
        
        /**
         * 温度参数（较低温度保证准确性）
         */
        private Double temperature = 0.3;
        
        /**
         * 纠错提示词模板
         */
        private String systemPrompt;
    }
}
