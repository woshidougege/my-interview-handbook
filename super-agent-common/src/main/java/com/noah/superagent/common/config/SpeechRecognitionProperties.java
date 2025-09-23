package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 语音识别配置属性
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.speech-recognition")
public class SpeechRecognitionProperties {

    /**
     * 模型配置
     */
    private String model = "gummy-realtime-v1";

    /**
     * 默认采样率
     */
    private Integer defaultSampleRate = 16000;

    /**
     * 支持的音频格式
     */
    private List<String> supportedFormats = List.of("pcm", "wav", "mp3", "opus", "speex", "aac", "amr");

    /**
     * 支持的源语言
     */
    private List<String> supportedSourceLanguages = List.of("auto", "zh", "en", "ja", "yue", "ko", "de", "fr", "ru", "it", "es");

    /**
     * 支持的翻译目标语言
     */
    private List<String> supportedTranslationLanguages = List.of("zh", "en", "ja", "ko");

    /**
     * 语言代码到显示名称的映射
     */
    private Map<String, String> languageNames = Map.of(
            "auto", "自动检测",
            "zh", "中文",
            "en", "英文", 
            "ja", "日语",
            "yue", "粤语",
            "ko", "韩语",
            "de", "德语",
            "fr", "法语",
            "ru", "俄语",
            "it", "意大利语"
    );

    /**
     * 音频格式到显示名称的映射
     */
    private Map<String, String> formatNames = Map.of(
            "pcm", "PCM",
            "wav", "WAV", 
            "mp3", "MP3",
            "opus", "Opus",
            "speex", "Speex",
            "aac", "AAC",
            "amr", "AMR"
    );

    /**
     * 音频格式说明
     */
    private Map<String, String> formatDescriptions = Map.of(
            "pcm", "未压缩的原始音频数据",
            "wav", "WAV格式，必须为PCM编码",
            "mp3", "MP3压缩音频格式",
            "opus", "Opus格式，必须使用Ogg封装",
            "speex", "Speex格式，必须使用Ogg封装",
            "aac", "AAC压缩音频格式",
            "amr", "AMR格式，仅支持AMR-NB类型"
    );

    /**
     * 默认配置
     */
    private DefaultConfig defaults = new DefaultConfig();

    /**
     * 默认配置类
     */
    @Data
    public static class DefaultConfig {
        /**
         * 是否启用转录功能
         */
        private Boolean transcriptionEnabled = true;

        /**
         * 是否启用翻译功能
         */
        private Boolean translationEnabled = false;

        /**
         * 默认源语言
         */
        private String sourceLanguage = "auto";

        /**
         * VAD断句静音时长阈值（毫秒）
         */
        private Integer maxEndSilence = 800;

        /**
         * 最小静音时长（毫秒）
         */
        private Integer minEndSilence = 200;

        /**
         * 最大静音时长（毫秒）
         */
        private Integer maxEndSilenceLimit = 6000;
    }

    /**
     * 高级配置
     */
    private AdvancedConfig advanced = new AdvancedConfig();

    /**
     * 参数限制配置
     */
    private LimitsConfig limits = new LimitsConfig();

    /**
     * 高级配置类
     */
    @Data
    public static class AdvancedConfig {
        /**
         * 是否启用热词功能
         */
        private Boolean hotWordEnabled = false;

        /**
         * 是否启用置信度返回
         */
        private Boolean confidenceEnabled = false;

        /**
         * 是否启用时间戳
         */
        private Boolean timestampEnabled = false;
    }

    /**
     * 参数限制配置类
     */
    @Data
    public static class LimitsConfig {
        /**
         * 最小采样率
         */
        private Integer minSampleRate = 16000;

        /**
         * 最大采样率
         */
        private Integer maxSampleRate = 48000;

        /**
         * 最大文件大小（MB）
         */
        private Integer maxFileSizeMB = 100;

        /**
         * 最大录音时长（秒）
         */
        private Integer maxRecordingDurationSeconds = 300;
    }
}
