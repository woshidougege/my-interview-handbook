package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 语音识别配置属性 - 极简版
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
}
